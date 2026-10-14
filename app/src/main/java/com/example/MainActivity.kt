package com.example

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.editor.LayoutEditorScreen
import com.example.ui.editor.LayoutEditorViewModel
import com.example.ui.gamepad.GamepadScreen
import com.example.ui.gamepad.GamepadViewModel
import com.example.ui.profiles.ProfileManagerDialog
import com.example.ui.settings.SettingsScreen
import com.example.ui.splash.SplashScreen
import com.example.ui.theme.SpadTheme
import com.example.ui.tv.TvConnectDialog

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        enableEdgeToEdge()

        // Keep screen on during controller gameplay
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Setup immersive sticky system bars
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            SpadAppRoot()
        }
    }
}

@Composable
fun SpadAppRoot() {
    val gamepadViewModel: GamepadViewModel = viewModel()
    val layoutEditorViewModel: LayoutEditorViewModel = viewModel()
    val prefs by gamepadViewModel.preferencesFlow.collectAsState()
    val activeProfile by gamepadViewModel.activeProfile.collectAsState()

    var showTvDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }

    val navController = rememberNavController()

    SpadTheme(themeMode = prefs.themeMode) {
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.fillMaxSize()
        ) {
            composable("splash") {
                SplashScreen(
                    onSplashFinished = {
                        navController.navigate("gamepad") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                )
            }

            composable("gamepad") {
                GamepadScreen(
                    viewModel = gamepadViewModel,
                    onNavigateToEditor = {
                        navController.navigate("editor")
                    },
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    },
                    onOpenTvDialog = {
                        showTvDialog = true
                    },
                    onOpenProfilesDialog = {
                        showProfileDialog = true
                    }
                )

                if (showTvDialog) {
                    TvConnectDialog(
                        viewModel = gamepadViewModel,
                        onDismiss = { showTvDialog = false }
                    )
                }

                if (showProfileDialog) {
                    ProfileManagerDialog(
                        profileRepository = gamepadViewModel.profileRepository,
                        activeProfileId = activeProfile.id,
                        onSelectProfile = { p ->
                            gamepadViewModel.selectProfile(p)
                            showProfileDialog = false
                        },
                        onDismiss = { showProfileDialog = false }
                    )
                }
            }

            composable("editor") {
                LayoutEditorScreen(
                    profileId = activeProfile.id,
                    viewModel = layoutEditorViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                        gamepadViewModel.selectProfile(layoutEditorViewModel.draftProfile.value)
                    }
                )
            }

            composable("settings") {
                SettingsScreen(
                    viewModel = gamepadViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onOpenTvDialog = {
                        showTvDialog = true
                    },
                    onOpenProfilesDialog = {
                        showProfileDialog = true
                    },
                    onNavigateToEditor = {
                        navController.navigate("editor")
                    }
                )

                if (showTvDialog) {
                    TvConnectDialog(
                        viewModel = gamepadViewModel,
                        onDismiss = { showTvDialog = false }
                    )
                }

                if (showProfileDialog) {
                    ProfileManagerDialog(
                        profileRepository = gamepadViewModel.profileRepository,
                        activeProfileId = activeProfile.id,
                        onSelectProfile = { p ->
                            gamepadViewModel.selectProfile(p)
                            showProfileDialog = false
                        },
                        onDismiss = { showProfileDialog = false }
                    )
                }
            }
        }
    }
}

package com.example.ui.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.DefaultProfiles
import com.example.core.model.RgbConfig
import com.example.core.model.RgbEffectMode
import com.example.ui.gamepad.GamepadViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: GamepadViewModel,
    onNavigateBack: () -> Unit,
    onOpenTvDialog: () -> Unit = {},
    onOpenProfilesDialog: () -> Unit = {},
    onNavigateToEditor: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs by viewModel.preferencesFlow.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    val tvState by viewModel.tvConnectionState.collectAsState()
    val latency by viewModel.latencyMs.collectAsState()

    var pcIp by remember(prefs.pcReceiverIp) { mutableStateOf(prefs.pcReceiverIp) }
    var pcPort by remember(prefs.pcReceiverPort) { mutableStateOf(prefs.pcReceiverPort.toString()) }
    var showResetDialog by remember { mutableStateOf(false) }

    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("S!pad Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("btn_settings_back")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF00F0FF))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF070A10))
            )
        },
        containerColor = Color(0xFF070A10),
        modifier = modifier.testTag("settings_screen")
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quick Access Cards
            item {
                SectionHeader("QUICK CONTROLLER ACTIONS", Icons.Default.SportsEsports)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // TV Connection Tile
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Samsung Smart TV", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    text = when (tvState) {
                                        is com.example.core.model.TvConnectionState.Connected -> "Connected • ${latency}ms latency"
                                        is com.example.core.model.TvConnectionState.Connecting -> "Connecting to TV..."
                                        is com.example.core.model.TvConnectionState.PairingPrompt -> "Waiting for approval on TV"
                                        else -> "Not connected"
                                    },
                                    color = if (tvState is com.example.core.model.TvConnectionState.Connected) Color(0xFF00FF66) else Color(0xFF94A3B8),
                                    fontSize = 12.sp
                                )
                            }
                            Button(
                                onClick = onOpenTvDialog,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF), contentColor = Color.Black),
                                modifier = Modifier.testTag("btn_settings_open_tv")
                            ) {
                                Text("Manage TV", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }

                        Divider(color = Color(0xFF1E293B))

                        // Profiles Tile
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Controller Profile", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Active: ${activeProfile.name}", color = Color(0xFF00F0FF), fontSize = 12.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (activeProfile.id != "builtin_xbox") {
                                    Button(
                                        onClick = {
                                            viewModel.switchToXboxController()
                                            Toast.makeText(context, "Switched to Xbox Controller", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF107C10), contentColor = Color.White),
                                        modifier = Modifier.testTag("btn_settings_quick_xbox")
                                    ) {
                                        Text("Xbox Mode", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                                OutlinedButton(
                                    onClick = onOpenProfilesDialog,
                                    modifier = Modifier.testTag("btn_settings_open_profiles")
                                ) {
                                    Text("Switch", color = Color.White, fontSize = 11.sp)
                                }
                                Button(
                                    onClick = onNavigateToEditor,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBD00FF), contentColor = Color.White),
                                    modifier = Modifier.testTag("btn_settings_open_editor")
                                ) {
                                    Text("Edit Layout", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
            // 1. Appearance Section
            item {
                SectionHeader("APPEARANCE & THEME", Icons.Default.Palette)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Theme Mode", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val modes = listOf("dark" to "Dark Mode", "light" to "Light Mode", "system" to "System")
                            for ((mode, label) in modes) {
                                val isSelected = prefs.themeMode == mode
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFF00F0FF) else Color(0xFF1E293B),
                                    modifier = Modifier
                                        .clickable {
                                            scope.launch { viewModel.preferencesRepository.setThemeMode(mode) }
                                        }
                                        .padding(4.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Divider(color = Color(0xFF1E293B))

                        // Background Theme Picker
                        Text("Background Art", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val themes = listOf(
                                "cyber_grid" to "Cyber Grid",
                                "neon_wave" to "Neon Wave",
                                "dark_minimal" to "Minimal",
                                "deep_void" to "Deep Void"
                            )
                            for ((themeId, label) in themes) {
                                val isSelected = activeProfile.backgroundTheme == themeId
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFFBD00FF) else Color(0xFF1E293B),
                                    modifier = Modifier.clickable {
                                        val updated = activeProfile.copy(backgroundTheme = themeId)
                                        viewModel.selectProfile(updated)
                                        scope.launch { viewModel.profileRepository.saveProfile(updated) }
                                    }
                                ) {
                                    Text(
                                        text = label,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        // Background Brightness
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Background Brightness", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text("${(activeProfile.backgroundBrightness * 100).toInt()}%", color = Color(0xFF00F0FF), fontSize = 12.sp)
                        }
                        Slider(
                            value = activeProfile.backgroundBrightness,
                            onValueChange = {
                                val updated = activeProfile.copy(backgroundBrightness = it)
                                viewModel.selectProfile(updated)
                                scope.launch { viewModel.profileRepository.saveProfile(updated) }
                            },
                            valueRange = 0.1f..1.0f,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF00F0FF), activeTrackColor = Color(0xFF00F0FF))
                        )
                    }
                }
            }

            // 2. RGB Lighting Engine
            item {
                SectionHeader("RGB LIGHTING ENGINE", Icons.Default.Palette)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Master RGB Switch", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Toggle dynamic animated illumination", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                            Switch(
                                checked = activeProfile.rgbConfig.isEnabled,
                                onCheckedChange = { enabled ->
                                    val updatedRgb = activeProfile.rgbConfig.copy(isEnabled = enabled)
                                    val updatedProfile = activeProfile.copy(rgbConfig = updatedRgb)
                                    viewModel.selectProfile(updatedProfile)
                                    scope.launch { viewModel.profileRepository.saveProfile(updatedProfile) }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00F0FF), checkedTrackColor = Color(0xFF003840))
                            )
                        }

                        if (activeProfile.rgbConfig.isEnabled) {
                            Text("Animation Preset", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val presets = listOf(
                                    RgbEffectMode.RAINBOW_CYCLE to "Rainbow",
                                    RgbEffectMode.BREATHING to "Breathe",
                                    RgbEffectMode.COLOR_PULSE to "Pulse",
                                    RgbEffectMode.NEON_WAVE to "Wave",
                                    RgbEffectMode.REACTIVE_PULSE to "Reactive"
                                )
                                for ((mode, label) in presets) {
                                    val isSelected = activeProfile.rgbConfig.mode == mode
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) Color(0xFF00F0FF) else Color(0xFF1E293B),
                                        modifier = Modifier.clickable {
                                            val updated = activeProfile.copy(rgbConfig = activeProfile.rgbConfig.copy(mode = mode))
                                            viewModel.selectProfile(updated)
                                            scope.launch { viewModel.profileRepository.saveProfile(updated) }
                                        }
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }

                            // Speed Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Animation Speed", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                Text("${"%.1f".format(activeProfile.rgbConfig.animationSpeed)}x", color = Color(0xFF00F0FF), fontSize = 12.sp)
                            }
                            Slider(
                                value = activeProfile.rgbConfig.animationSpeed,
                                onValueChange = {
                                    val updated = activeProfile.copy(rgbConfig = activeProfile.rgbConfig.copy(animationSpeed = it))
                                    viewModel.selectProfile(updated)
                                    scope.launch { viewModel.profileRepository.saveProfile(updated) }
                                },
                                valueRange = 0.2f..2.5f,
                                colors = SliderDefaults.colors(thumbColor = Color(0xFF00F0FF), activeTrackColor = Color(0xFF00F0FF))
                            )
                        }
                    }
                }
            }

            // 3. Controls & Haptics
            item {
                SectionHeader("CONTROLS & HAPTIC FEEDBACK", Icons.Default.SportsEsports)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Tactile Vibration", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Haptic pulses on button clicks and stick boundaries", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                            Switch(
                                checked = activeProfile.hapticConfig.isEnabled,
                                onCheckedChange = { enabled ->
                                    val updatedHaptic = activeProfile.hapticConfig.copy(isEnabled = enabled)
                                    val updated = activeProfile.copy(hapticConfig = updatedHaptic)
                                    viewModel.selectProfile(updated)
                                    scope.launch { viewModel.profileRepository.saveProfile(updated) }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00F0FF), checkedTrackColor = Color(0xFF003840))
                            )
                        }

                        // Stick Deadzone
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Stick Radial Deadzone", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text("${(activeProfile.leftStickConfig.deadzonePercent * 100).toInt()}%", color = Color(0xFF00F0FF), fontSize = 12.sp)
                        }
                        Slider(
                            value = activeProfile.leftStickConfig.deadzonePercent,
                            onValueChange = {
                                val updated = activeProfile.copy(
                                    leftStickConfig = activeProfile.leftStickConfig.copy(deadzonePercent = it),
                                    rightStickConfig = activeProfile.rightStickConfig.copy(deadzonePercent = it)
                                )
                                viewModel.selectProfile(updated)
                                scope.launch { viewModel.profileRepository.saveProfile(updated) }
                            },
                            valueRange = 0.0f..0.35f,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF00F0FF), activeTrackColor = Color(0xFF00F0FF))
                        )

                        // Stick Sensitivity
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Stick Sensitivity", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text("${"%.1f".format(activeProfile.leftStickConfig.sensitivity)}x", color = Color(0xFFBD00FF), fontSize = 12.sp)
                        }
                        Slider(
                            value = activeProfile.leftStickConfig.sensitivity,
                            onValueChange = {
                                val updated = activeProfile.copy(
                                    leftStickConfig = activeProfile.leftStickConfig.copy(sensitivity = it),
                                    rightStickConfig = activeProfile.rightStickConfig.copy(sensitivity = it)
                                )
                                viewModel.selectProfile(updated)
                                scope.launch { viewModel.profileRepository.saveProfile(updated) }
                            },
                            valueRange = 0.5f..2.5f,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFFBD00FF), activeTrackColor = Color(0xFFBD00FF))
                        )
                    }
                }
            }

            // 4. PC Receiver Target
            item {
                SectionHeader("OPTIONAL PC RECEIVER", Icons.Default.Tv)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Stream low-latency 24-byte UDP packets to your gaming PC running spad_pc_receiver.py for virtual Xbox 360 controller emulation.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = pcIp,
                                onValueChange = { pcIp = it },
                                label = { Text("PC IP Address", fontSize = 11.sp) },
                                modifier = Modifier.weight(2f),
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp)
                            )
                            OutlinedTextField(
                                value = pcPort,
                                onValueChange = { pcPort = it },
                                label = { Text("Port", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp)
                            )
                        }

                        Button(
                            onClick = {
                                val portInt = pcPort.toIntOrNull() ?: 5150
                                scope.launch {
                                    viewModel.preferencesRepository.setPcReceiver(pcIp.trim(), portInt)
                                    viewModel.pcTransport.configureTarget(pcIp.trim(), portInt)
                                    Toast.makeText(context, "Saved PC Receiver configuration", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF), contentColor = Color.Black),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Save PC Target", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // 5. Battery & Power Optimization
            item {
                SectionHeader("BATTERY & POWER OPTIMIZATION", Icons.Default.Security)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Eco Battery Saver Mode", color = Color(0xFF00FF66), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    "Stops ambient CPU animation loops when idle, optimizes network socket packet dispatching, and minimizes GPU recompositions to maximize phone battery life during gaming sessions.",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Switch(
                                checked = prefs.batterySaverMode,
                                onCheckedChange = { enabled ->
                                    scope.launch { viewModel.preferencesRepository.setBatterySaverMode(enabled) }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00FF66), checkedTrackColor = Color(0xFF004D20)),
                                modifier = Modifier.testTag("switch_battery_saver")
                            )
                        }
                    }
                }
            }

            // 6. Experimental Gaming Hub
            item {
                SectionHeader("EXPERIMENTAL SAMSUNG GAMING HUB", Icons.Default.Security)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131722)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Experimental Gaming Hub Flag", color = Color(0xFFFFD600), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Status: Disabled by default", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                            Switch(
                                checked = prefs.experimentalGamingHub,
                                onCheckedChange = { checked ->
                                    scope.launch { viewModel.preferencesRepository.setExperimentalGamingHub(checked) }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD600), checkedTrackColor = Color(0xFF4A3E00))
                            )
                        }

                        Text(
                            text = "TECHNICAL DISCLOSURE: Samsung Gaming Hub (Xbox Cloud Gaming, GeForce NOW) requires physical kernel-level Bluetooth HID or USB HID gamepads. Samsung does not provide a public local network socket that accepts arbitrary analog joystick HID events inside Gaming Hub. Phase 1 provides full Smart TV remote control; PC Receiver provides full PC Xbox gaming.",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // 6. Restore Factory Defaults
            item {
                SectionHeader("RESTORE FACTORY DEFAULTS", Icons.Default.Refresh)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1218)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(1.dp, Color(0xFF881337), RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Reset Everything to Default",
                                    color = Color(0xFFFF5252),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Revert all controller button positions, layouts, stick sensitivities, deadzones, RGB lighting, haptic feedback, and app preferences back to original factory defaults.",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        Button(
                            onClick = { showResetDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE11D48),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .align(Alignment.End)
                                .testTag("btn_reset_defaults")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Reset All to Default", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // 7. About & Credits
            item {
                SectionHeader("ABOUT & CREDITS", Icons.Default.Settings)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("S!pad", color = Color(0xFF00F0FF), fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text("Version 1.0.0", color = Color.White, fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Made by Shubham Kumar", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Made in India 🇮🇳", color = Color(0xFFFF9933), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Privacy: 100% on-device & local Wi-Fi. No accounts, no cloud database, no user tracking. Original design and codebase.",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(40.dp))
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = "Reset Everything to Default?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "This will reset all controller layouts (button positions, sizes, colors), stick deadzones, RGB lighting, tactile haptics, and app preferences back to original factory defaults.\n\nAre you sure you want to proceed?",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        viewModel.resetEverythingToDefault {
                            pcIp = "192.168.1.100"
                            pcPort = "5150"
                            Toast.makeText(context, "All settings and layouts restored to default", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE11D48),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("btn_confirm_reset_defaults")
                ) {
                    Text("Yes, Reset Everything", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showResetDialog = false }
                ) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(16.dp))
        Text(title, color = Color(0xFF00F0FF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

package com.example.ui.gamepad

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.data.bluetooth.BluetoothControllerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.model.ControlType
import com.example.core.model.GamepadButton
import com.example.core.model.TvConnectionState
import com.example.core.rgb.RgbColorEngine
import com.example.ui.gamepad.components.ActionButtonCluster
import com.example.ui.gamepad.components.AnalogStickComponent
import com.example.ui.gamepad.components.BackgroundRenderer
import com.example.ui.gamepad.components.BumperComponent
import com.example.ui.gamepad.components.DpadComponent
import com.example.ui.gamepad.components.StickClickButton
import com.example.ui.gamepad.components.SystemMenuButton
import com.example.ui.gamepad.components.TriggerComponent
import com.example.ui.gamepad.components.XboxGuideButton

@Composable
fun GamepadScreen(
    viewModel: GamepadViewModel,
    onNavigateToEditor: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onOpenTvDialog: () -> Unit,
    onOpenProfilesDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profile by viewModel.activeProfile.collectAsState()
    val tvState by viewModel.tvConnectionState.collectAsState()
    val latency by viewModel.latencyMs.collectAsState()
    val rgbProgress by viewModel.rgbProgress.collectAsState()
    val btState by viewModel.bluetoothState.collectAsState()

    val bluetoothPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            results[Manifest.permission.BLUETOOTH_CONNECT] == true &&
            results[Manifest.permission.BLUETOOTH_ADVERTISE] == true
        } else {
            true
        }
        if (granted) {
            viewModel.bluetoothGamepadManager.startBluetoothController()
        }
    }

    val enableBtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.bluetoothGamepadManager.startBluetoothController()
    }

    // Auto-request Bluetooth permissions & automatically turn on Bluetooth Gamepad mode
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasConnect = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            val hasAdvertise = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
            if (!hasConnect || !hasAdvertise) {
                permissionLauncher.launch(bluetoothPermissions)
            } else {
                viewModel.bluetoothGamepadManager.startBluetoothController()
            }
        } else {
            viewModel.bluetoothGamepadManager.startBluetoothController()
        }
    }

    // Auto-launch turn on Bluetooth prompt if device Bluetooth is switched off
    LaunchedEffect(btState) {
        if (btState is BluetoothControllerState.BluetoothDisabledOnDevice) {
            try {
                enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            } catch (_: Exception) {}
        }
    }

    val isXboxProfile = profile.id == "builtin_xbox" || profile.name.contains("Xbox", ignoreCase = true)

    var isSettingsMenuOpen by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    val globalRgbColor = if (profile.rgbConfig.isEnabled) {
        RgbColorEngine.getAnimatedColor(
            config = profile.rgbConfig,
            progress = rgbProgress
        )
    } else {
        if (isXboxProfile) Color(0xFF00FF66) else Color(0xFF00F0FF)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("gamepad_screen_root")
    ) {
        // 1. Immersive Gaming Background
        BackgroundRenderer(
            themeName = profile.backgroundTheme,
            brightness = profile.backgroundBrightness,
            opacity = profile.backgroundOpacity
        )

        // 2. Interactive Virtual Gamepad Surface (Canvas Constraints with Adaptive Scaling)
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val screenW = maxWidth.value
            val screenH = maxHeight.value
            val screenWpx = with(density) { maxWidth.toPx() }
            val screenHpx = with(density) { maxHeight.toPx() }

            // Adaptive scale factor: phones (typically height ~360-420dp) get 1.0f,
            // tablets (height ~600-800dp+) scale up gracefully to 1.15f-1.30f
            val scaleFactor = when {
                screenH >= 650f -> 1.30f
                screenH >= 500f -> 1.15f
                screenH <= 340f -> 0.88f
                else -> 1.0f
            }

            // Render all virtual controller components
            for (control in profile.controls) {
                if (!control.isVisible) continue

                val scaledSize = control.sizeDp * scaleFactor
                val isBumper = control.type == ControlType.L1_BUTTON || control.type == ControlType.R1_BUTTON
                val isTrigger = control.type == ControlType.L2_TRIGGER || control.type == ControlType.R2_TRIGGER

                val controlW = scaledSize
                val controlH = if (isBumper) scaledSize * 0.50f else if (isTrigger) scaledSize * 0.58f else scaledSize

                val controlX = (control.xPercent * screenW) - (controlW / 2f)
                val controlY = (control.yPercent * screenH) - (controlH / 2f)

                val controlRgb = if (profile.rgbConfig.isEnabled) {
                    RgbColorEngine.getAnimatedColor(
                        config = profile.rgbConfig,
                        progress = rgbProgress,
                        phaseOffset = control.xPercent
                    )
                } else {
                    RgbColorEngine.parseColorHex(control.colorHex)
                }

                Box(
                    modifier = Modifier
                        .offset { IntOffset(controlX.dp.roundToPx(), controlY.dp.roundToPx()) }
                        .rotate(control.rotationDeg)
                ) {
                    when (control.type) {
                        ControlType.LEFT_STICK -> {
                            AnalogStickComponent(
                                config = profile.leftStickConfig.copy(sizeDp = scaledSize),
                                accentColor = RgbColorEngine.parseColorHex(control.colorHex),
                                rgbGlowColor = controlRgb,
                                tag = "stick_left",
                                onPositionChanged = { rx, ry -> viewModel.onLeftStickMoved(rx, ry) },
                                onClickL3R3 = { viewModel.onButtonPressed(GamepadButton.L3, true); viewModel.onButtonPressed(GamepadButton.L3, false) }
                            )
                        }

                        ControlType.RIGHT_STICK -> {
                            AnalogStickComponent(
                                config = profile.rightStickConfig.copy(sizeDp = scaledSize),
                                accentColor = RgbColorEngine.parseColorHex(control.colorHex),
                                rgbGlowColor = controlRgb,
                                tag = "stick_right",
                                onPositionChanged = { rx, ry -> viewModel.onRightStickMoved(rx, ry) },
                                onClickL3R3 = { viewModel.onButtonPressed(GamepadButton.R3, true); viewModel.onButtonPressed(GamepadButton.R3, false) }
                            )
                        }

                        ControlType.DPAD -> {
                            DpadComponent(
                                sizeDp = scaledSize,
                                accentColor = RgbColorEngine.parseColorHex(control.colorHex),
                                rgbGlowColor = controlRgb,
                                onButtonStateChanged = { b, pressed -> viewModel.onButtonPressed(b, pressed) }
                            )
                        }

                        ControlType.ACTION_CLUSTER -> {
                            ActionButtonCluster(
                                sizeDp = scaledSize,
                                rgbGlowColor = controlRgb,
                                onButtonStateChanged = { b, pressed -> viewModel.onButtonPressed(b, pressed) }
                            )
                        }

                        ControlType.L1_BUTTON -> {
                            BumperComponent(
                                label = "LB",
                                button = GamepadButton.L1,
                                accentColor = RgbColorEngine.parseColorHex(control.colorHex),
                                rgbGlowColor = controlRgb,
                                sizeDp = scaledSize,
                                onStateChanged = { b, p -> viewModel.onButtonPressed(b, p) }
                            )
                        }

                        ControlType.R1_BUTTON -> {
                            BumperComponent(
                                label = "RB",
                                button = GamepadButton.R1,
                                accentColor = RgbColorEngine.parseColorHex(control.colorHex),
                                rgbGlowColor = controlRgb,
                                sizeDp = scaledSize,
                                onStateChanged = { b, p -> viewModel.onButtonPressed(b, p) }
                            )
                        }

                        ControlType.L2_TRIGGER -> {
                            TriggerComponent(
                                label = "LT",
                                accentColor = RgbColorEngine.parseColorHex(control.colorHex),
                                rgbGlowColor = controlRgb,
                                sizeDp = scaledSize,
                                onTriggerChanged = { v -> viewModel.onLeftTriggerChanged(v) },
                                onDigitalStateChanged = { pressed -> viewModel.onButtonPressed(GamepadButton.L2, pressed) }
                            )
                        }

                        ControlType.R2_TRIGGER -> {
                            TriggerComponent(
                                label = "RT",
                                accentColor = RgbColorEngine.parseColorHex(control.colorHex),
                                rgbGlowColor = controlRgb,
                                sizeDp = scaledSize,
                                onTriggerChanged = { v -> viewModel.onRightTriggerChanged(v) },
                                onDigitalStateChanged = { pressed -> viewModel.onButtonPressed(GamepadButton.R2, pressed) }
                            )
                        }

                        ControlType.SELECT_BUTTON -> {
                            SystemMenuButton(
                                label = "VIEW",
                                button = GamepadButton.SELECT,
                                accentColor = RgbColorEngine.parseColorHex(control.colorHex),
                                rgbGlowColor = controlRgb,
                                sizeDp = scaledSize,
                                onStateChanged = { b, p -> viewModel.onButtonPressed(b, p) }
                            )
                        }

                        ControlType.START_BUTTON -> {
                            SystemMenuButton(
                                label = "MENU",
                                button = GamepadButton.START,
                                accentColor = RgbColorEngine.parseColorHex(control.colorHex),
                                rgbGlowColor = controlRgb,
                                sizeDp = scaledSize,
                                onStateChanged = { b, p -> viewModel.onButtonPressed(b, p) }
                            )
                        }

                        ControlType.LS_BUTTON -> {
                            StickClickButton(
                                label = "LS",
                                button = GamepadButton.L3,
                                accentColor = RgbColorEngine.parseColorHex(control.colorHex),
                                rgbGlowColor = controlRgb,
                                onStateChanged = { b, p -> viewModel.onButtonPressed(b, p) }
                            )
                        }

                        ControlType.RS_BUTTON -> {
                            StickClickButton(
                                label = "RS",
                                button = GamepadButton.R3,
                                accentColor = RgbColorEngine.parseColorHex(control.colorHex),
                                rgbGlowColor = controlRgb,
                                onStateChanged = { b, p -> viewModel.onButtonPressed(b, p) }
                            )
                        }

                        ControlType.XBOX_GUIDE_BUTTON -> {
                            XboxGuideButton(
                                accentColor = RgbColorEngine.parseColorHex(control.colorHex),
                                rgbGlowColor = controlRgb,
                                sizeDp = scaledSize,
                                onStateChanged = { b, p -> viewModel.onButtonPressed(b, p) }
                            )
                        }

                        ControlType.SETTINGS_BUTTON -> {
                            CircleSettingsButton(
                                sizeDp = scaledSize.dp,
                                accentColor = RgbColorEngine.parseColorHex(control.colorHex),
                                rgbGlowColor = controlRgb,
                                isXbox = isXboxProfile,
                                isTvConnected = tvState is TvConnectionState.Connected,
                                onClick = { isSettingsMenuOpen = true }
                            )
                        }
                    }
                }
            }

            // 3. CIRCLE SETTINGS BUTTON (Fallback if profile has no SETTINGS_BUTTON control)
            // Note: Settings button is movable ONLY through the layout customization editor!
            // In Gamepad mode, it is a fixed circular button that opens the settings menu when tapped.
            val hasSettingsControl = profile.controls.any { it.type == ControlType.SETTINGS_BUTTON && it.isVisible }
            if (!hasSettingsControl) {
                val defSizeDp = 48.dp
                val defXDp = (0.50f * screenW).dp - (defSizeDp / 2)
                val defYDp = (0.48f * screenH).dp - (defSizeDp / 2)
                Box(
                    modifier = Modifier.offset {
                        IntOffset(defXDp.roundToPx(), defYDp.roundToPx())
                    }
                ) {
                    CircleSettingsButton(
                        sizeDp = defSizeDp,
                        accentColor = if (isXboxProfile) Color(0xFF107C10) else Color(0xFF00F0FF),
                        rgbGlowColor = if (isXboxProfile) Color(0xFF00FF66) else Color(0xFF00F0FF),
                        isXbox = isXboxProfile,
                        isTvConnected = tvState is TvConnectionState.Connected,
                        onClick = { isSettingsMenuOpen = true }
                    )
                }
            }
        }

        // 4. SCROLL-DOWN SETTINGS & CONTROLS MENU
        // Opens when clicking the movable settings button
        if (isSettingsMenuOpen) {
            Dialog(
                onDismissRequest = { isSettingsMenuOpen = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .fillMaxHeight(0.92f)
                        .border(1.5.dp, Color(0xFF00F0FF), RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xF8080E1C),
                    tonalElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp)
                    ) {
                        // Menu Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = Color(0xFF00F0FF),
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "SETTINGS & CONTROLS",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            IconButton(
                                onClick = { isSettingsMenuOpen = false },
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(Color(0xFF1E293B), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Menu",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Text(
                            text = "💡 Tip: Customize this circle Settings button's position and size anytime in Customize Buttons",
                            color = Color(0xFF64748B),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                        )

                        Divider(color = Color(0xFF1E293B), thickness = 1.dp)

                        // Scrollable Menu Content
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // SECTION 1: XBOX & CONTROLLER PROFILE
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.SportsEsports,
                                                contentDescription = null,
                                                tint = Color(0xFF00F0FF),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "CONTROLLER PROFILE",
                                                color = Color(0xFF00F0FF),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Surface(
                                            color = if (isXboxProfile) Color(0xFF107C10) else Color(0xFF1E293B),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = profile.name,
                                                color = if (isXboxProfile) Color(0xFF00FF66) else Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Switch to Xbox Button
                                        Button(
                                            onClick = {
                                                if (!isXboxProfile) {
                                                    viewModel.switchToXboxController()
                                                    Toast.makeText(context, "🎮 Switched to Xbox Controller", Toast.LENGTH_SHORT).show()
                                                }
                                                isSettingsMenuOpen = false
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isXboxProfile) Color(0xFF107C10) else Color(0xFF1E293B),
                                                contentColor = if (isXboxProfile) Color.White else Color(0xFF94A3B8)
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.SportsEsports, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(if (isXboxProfile) "Xbox Active" else "Switch to Xbox", fontSize = 12.sp)
                                        }

                                        // All Profiles Button
                                        OutlinedButton(
                                            onClick = {
                                                isSettingsMenuOpen = false
                                                onOpenProfilesDialog()
                                            },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00F0FF)),
                                            border = BorderStroke(1.dp, Color(0xFF00F0FF)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("All Profiles ▾", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }

                            // SECTION 2: TV CONNECTION (BLUETOOTH GAMEPAD & WI-FI)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.SportsEsports,
                                                contentDescription = null,
                                                tint = Color(0xFF00FF66),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "TV & XBOX CONNECTION",
                                                color = Color(0xFF00FF66),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Surface(
                                            color = when (btState) {
                                                is BluetoothControllerState.Connected -> Color(0xFF0F2615)
                                                is BluetoothControllerState.ReadyToPair -> Color(0xFF281F0B)
                                                else -> if (tvState is TvConnectionState.Connected) Color(0xFF0F2615) else Color(0xFF1E293B)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(
                                                1.dp,
                                                when (btState) {
                                                    is BluetoothControllerState.Connected -> Color(0xFF107C10)
                                                    is BluetoothControllerState.ReadyToPair -> Color(0xFFFFD600)
                                                    else -> if (tvState is TvConnectionState.Connected) Color(0xFF107C10) else Color(0xFF334155)
                                                }
                                            )
                                        ) {
                                            Text(
                                                text = when (val st = btState) {
                                                    is BluetoothControllerState.Connected -> "BT: Connected"
                                                    is BluetoothControllerState.ReadyToPair -> "BT: Broadcasting"
                                                    is BluetoothControllerState.PermissionRequired -> "BT: Permission Needed"
                                                    else -> if (tvState is TvConnectionState.Connected) "Wi-Fi: Connected (${latency}ms)" else "BT: Ready"
                                                },
                                                color = when (btState) {
                                                    is BluetoothControllerState.Connected -> Color(0xFF00FF66)
                                                    is BluetoothControllerState.ReadyToPair -> Color(0xFFFFD600)
                                                    is BluetoothControllerState.PermissionRequired -> Color(0xFFFF5252)
                                                    else -> if (tvState is TvConnectionState.Connected) Color(0xFF00FF66) else Color(0xFF94A3B8)
                                                },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                isSettingsMenuOpen = false
                                                onOpenTvDialog()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF107C10), contentColor = Color.White),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Tv, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Connect TV Hub", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        if (btState is BluetoothControllerState.Connected || btState is BluetoothControllerState.ReadyToPair) {
                                            OutlinedButton(
                                                onClick = { viewModel.bluetoothGamepadManager.stop() },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                                                border = BorderStroke(1.dp, Color(0xFFFF5252)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Stop BT", fontSize = 12.sp)
                                            }
                                        } else {
                                            OutlinedButton(
                                                onClick = {
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                        permissionLauncher.launch(bluetoothPermissions)
                                                    } else {
                                                        viewModel.bluetoothGamepadManager.startBluetoothController()
                                                    }
                                                },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00FF66)),
                                                border = BorderStroke(1.dp, Color(0xFF00FF66)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Start BT Gamepad", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            // SECTION 3: BUTTON CUSTOMIZATION & LAYOUT EDITOR
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = null,
                                                tint = Color(0xFFBD00FF),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "CUSTOMIZE BUTTONS",
                                                color = Color(0xFFBD00FF),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = "Drag, reposition, scale, rotate, and color every button on screen",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            isSettingsMenuOpen = false
                                            onNavigateToEditor()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBD00FF), contentColor = Color.White)
                                    ) {
                                        Text("Edit Layout", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // SECTION 4: FULL APP SETTINGS
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Tune,
                                                contentDescription = null,
                                                tint = Color(0xFFFFD600),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "FULL APPLICATION SETTINGS",
                                                color = Color(0xFFFFD600),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = "Stick deadzones, sensitivity curves, haptic vibration, RGB chroma effects, UDP binary stream",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            isSettingsMenuOpen = false
                                            onNavigateToSettings()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color.White),
                                        border = BorderStroke(1.dp, Color(0xFF334155))
                                    ) {
                                        Text("All Settings", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // SECTION 5: RESTORE FACTORY DEFAULTS
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1218)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFF881337), RoundedCornerShape(14.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = null,
                                                tint = Color(0xFFFF5252),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "RESTORE FACTORY DEFAULTS",
                                                color = Color(0xFFFF5252),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = "Reset all buttons, positions, RGB illumination, deadzones, and settings back to original factory defaults",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            isSettingsMenuOpen = false
                                            showResetConfirmDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48), contentColor = Color.White)
                                    ) {
                                        Text("Reset All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showResetConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showResetConfirmDialog = false },
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
                        text = "This will restore all controller button layouts, positions, stick deadzones, RGB lighting, haptics, and app settings to factory defaults.\n\nAre you sure you want to proceed?",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showResetConfirmDialog = false
                            viewModel.resetEverythingToDefault {
                                Toast.makeText(context, "All settings and layouts restored to default", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48), contentColor = Color.White)
                    ) {
                        Text("Yes, Reset Everything", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showResetConfirmDialog = false }
                    ) {
                        Text("Cancel", color = Color.White)
                    }
                },
                containerColor = Color(0xFF0F172A),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

/**
 * High-end circular Settings button styled like an authentic console hardware button.
 * Movable ONLY through the layout customization editor (LayoutEditorScreen) — during live
 * gameplay, tapping it reliably opens the scroll-down settings & controls menu without accidental dragging.
 */
@Composable
fun CircleSettingsButton(
    sizeDp: androidx.compose.ui.unit.Dp,
    accentColor: Color,
    rgbGlowColor: Color,
    isXbox: Boolean,
    isTvConnected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = Color(0xF2080E1C),
        shadowElevation = 10.dp,
        modifier = modifier
            .size(sizeDp)
            .border(
                width = 2.dp,
                brush = Brush.radialGradient(
                    colors = listOf(
                        rgbGlowColor,
                        if (isXbox) Color(0xFF107C10) else Color(0xFFBD00FF)
                    )
                ),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .testTag("btn_circle_settings")
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Open Settings & Controls Menu",
                tint = if (isXbox) Color(0xFF00FF66) else rgbGlowColor,
                modifier = Modifier.size((sizeDp.value * 0.48f).coerceAtLeast(18f).dp)
            )

            // TV status indicator dot
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(3.dp)
                    .size((sizeDp.value * 0.22f).coerceIn(7f, 12f).dp)
                    .background(
                        color = if (isTvConnected) Color(0xFF00FF66) else Color(0xFF64748B),
                        shape = CircleShape
                    )
                    .border(1.2.dp, Color(0xFF080E1C), CircleShape)
            )
        }
    }
}

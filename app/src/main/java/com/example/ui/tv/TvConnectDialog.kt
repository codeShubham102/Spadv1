package com.example.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.core.model.DiscoveredTv
import com.example.core.model.TvConnectionState
import com.example.data.bluetooth.BluetoothControllerState
import com.example.data.network.samsung.TvDiscoveryService
import com.example.ui.gamepad.GamepadViewModel
import kotlinx.coroutines.launch

@Composable
fun TvConnectDialog(
    viewModel: GamepadViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val discoveryService = remember { TvDiscoveryService(context) }
    val discoveredTvs by discoveryService.discoveredTvs.collectAsState()
    val isScanning by discoveryService.isScanning.collectAsState()
    val connectionState by viewModel.tvConnectionState.collectAsState()
    val latencyMs by viewModel.latencyMs.collectAsState()
    val prefs by viewModel.preferencesFlow.collectAsState()
    val btState by viewModel.bluetoothState.collectAsState()

    var manualIp by remember(prefs.lastTvIp) { mutableStateOf(prefs.lastTvIp) }
    var selectedTab by remember { mutableIntStateOf(1) } // Default to 1: Bluetooth Gamepad for Samsung TV & Xbox Cloud Gaming

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

    val requestBluetoothAndStart = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val connectGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            val advertiseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
            if (!connectGranted || !advertiseGranted) {
                permissionLauncher.launch(bluetoothPermissions)
            } else {
                viewModel.bluetoothGamepadManager.startBluetoothController()
            }
        } else {
            viewModel.bluetoothGamepadManager.startBluetoothController()
        }
    }

    // Auto-request Bluetooth permissions & start Bluetooth Gamepad controller immediately
    LaunchedEffect(selectedTab) {
        if (selectedTab == 1) {
            requestBluetoothAndStart()
        }
    }

    // Auto-launch Bluetooth turn-on prompt if Bluetooth is switched off on device
    LaunchedEffect(btState) {
        if (btState is BluetoothControllerState.BluetoothDisabledOnDevice) {
            try {
                enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(prefs.lastTvIp, prefs.lastTvName) {
        if (prefs.lastTvIp.isNotBlank()) {
            discoveryService.seedSavedTv(prefs.lastTvIp, prefs.lastTvName)
        }
        discoveryService.startDiscovery()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (selectedTab == 0) Icons.Default.Tv else Icons.Default.SportsEsports,
                        contentDescription = "Samsung Connection",
                        tint = if (selectedTab == 0) Color(0xFF00F0FF) else Color(0xFF00FF66)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Samsung TV Controller Hub",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Tab Switcher: Wi-Fi TV Remote vs Bluetooth Gamepad
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E293B))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selectedTab == 0) Color(0xFF00F0FF) else Color.Transparent)
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Wifi,
                                contentDescription = null,
                                tint = if (selectedTab == 0) Color.Black else Color(0xFF94A3B8),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Wi-Fi TV Remote",
                                color = if (selectedTab == 0) Color.Black else Color(0xFF94A3B8),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selectedTab == 1) Color(0xFF00FF66) else Color.Transparent)
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.SportsEsports,
                                contentDescription = null,
                                tint = if (selectedTab == 1) Color.Black else Color(0xFF94A3B8),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Xbox App Mode",
                                color = if (selectedTab == 1) Color.Black else Color(0xFF94A3B8),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                if (selectedTab == 0) {
                    // TAB 0: WI-FI SMART TV REMOTE
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("WI-FI TV STATUS", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                when (val st = connectionState) {
                                    is TvConnectionState.Connected -> {
                                        Text(
                                            text = "Connected (${st.deviceName})",
                                            color = Color(0xFF00FF66),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Text("Latency: ${latencyMs}ms", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    }
                                    is TvConnectionState.Connecting -> {
                                        Text("Connecting to ${st.deviceName}...", color = Color(0xFFFFD600), fontSize = 12.sp)
                                    }
                                    is TvConnectionState.PairingPrompt -> {
                                        Text("Look at TV screen to ALLOW...", color = Color(0xFFFF007F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("Use TV remote to click ALLOW", color = Color(0xFFCBD5E1), fontSize = 10.sp)
                                    }
                                    is TvConnectionState.Error -> {
                                        Text(st.message, color = Color(0xFFFF0055), fontSize = 11.sp, maxLines = 2)
                                    }
                                    else -> {
                                        Text("Disconnected", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                    }
                                }
                            }

                            if (connectionState is TvConnectionState.Connected) {
                                Button(
                                    onClick = { viewModel.disconnectTv() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0055)),
                                    modifier = Modifier.testTag("btn_tv_disconnect")
                                ) {
                                    Text("Disconnect", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // Discovered TVs Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DISCOVERED ON LOCAL WI-FI (ALL SUBNET IPs)",
                            color = Color(0xFF00F0FF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { scope.launch { discoveryService.startDiscovery() } },
                            enabled = !isScanning,
                            modifier = Modifier.size(24.dp)
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color(0xFF00F0FF))
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF00F0FF), modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    // TV List
                    if (discoveredTvs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(65.dp)
                                .background(Color(0xFF131D2E), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isScanning) "Searching for Samsung TVs on Wi-Fi..." else "No TVs found automatically. Enter IP below.",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(95.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(discoveredTvs, key = { it.id }) { tv ->
                                DiscoveredTvRow(
                                    tv = tv,
                                    onConnect = {
                                        viewModel.connectToTv(tv.ipAddress, tv.port, prefs.tvToken)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Manual IP Fallback
                    Text("MANUAL IP ADDRESS (FROM TV NETWORK SETTINGS)", color = Color(0xFF00F0FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualIp,
                            onValueChange = { manualIp = it },
                            placeholder = { Text("e.g. 192.168.1.150", color = Color.Gray, fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_manual_tv_ip"),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val ip = manualIp.trim()
                                if (ip.isNotBlank()) {
                                    discoveryService.addManualTv(ip)
                                    viewModel.connectToTv(ip, 8002, prefs.tvToken)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF), contentColor = Color.Black),
                            modifier = Modifier.testTag("btn_connect_manual_ip")
                        ) {
                            Text("Connect", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                } else {
                    // TAB 1: BLUETOOTH CONTROLLER MODE (XBOX APP ON SAMSUNG TV)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF131D24),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF107C10)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Bluetooth, contentDescription = null, tint = Color(0xFF00FF66))
                                    Spacer(Modifier.width(6.dp))
                                    Text("BLUETOOTH GAMEPAD STATUS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                when (btState) {
                                    is BluetoothControllerState.Connected -> {
                                        Text("CONNECTED", color = Color(0xFF00FF66), fontWeight = FontWeight.Black, fontSize = 11.sp)
                                    }
                                    is BluetoothControllerState.Connecting -> {
                                        Text("CONNECTING...", color = Color(0xFF00F0FF), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                    is BluetoothControllerState.ReadyToPair -> {
                                        Text("READY TO PAIR", color = Color(0xFFFFD600), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                    is BluetoothControllerState.PermissionRequired -> {
                                        Text("PERMISSION NEEDED", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                    is BluetoothControllerState.BluetoothDisabledOnDevice -> {
                                        Text("BT TURNED OFF", color = Color(0xFFFFB300), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                    else -> {
                                        Text("OFF", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    }
                                }
                            }

                            when (val st = btState) {
                                is BluetoothControllerState.Connected -> {
                                    Surface(
                                        color = Color(0xFF0B2414),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFF107C10)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                "Connected to ${st.deviceName} as Xbox Controller!",
                                                color = Color(0xFF00FF66),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                "Open Xbox App on your Samsung TV now — it will recognize this controller!",
                                                color = Color(0xFFCBD5E1),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                                is BluetoothControllerState.Connecting -> {
                                    Surface(
                                        color = Color(0xFF0B1E2D),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFF00F0FF)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            CircularProgressIndicator(color = Color(0xFF00F0FF), modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                            Text(
                                                "Connecting to ${st.deviceName}...",
                                                color = Color(0xFF00F0FF),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                                is BluetoothControllerState.ReadyToPair -> {
                                    Surface(
                                        color = Color(0xFF281F0B),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFFFD600)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                "Broadcasting as \"${st.localName}\"",
                                                color = Color(0xFFFFD600),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                "Pair from Samsung TV Bluetooth Device List, or tap a paired TV below to connect!",
                                                color = Color(0xFFCBD5E1),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                                is BluetoothControllerState.PermissionRequired -> {
                                    Surface(
                                        color = Color(0xFF2E1212),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFFF5252)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Bluetooth Permission Required", color = Color(0xFFFF8888), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                            Text(
                                                "Android requires Nearby Devices (Bluetooth) permission so Samsung TV can discover this phone as a game controller.",
                                                color = Color(0xFFCBD5E1),
                                                fontSize = 10.sp,
                                                lineHeight = 14.sp
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Button(
                                                    onClick = { permissionLauncher.launch(bluetoothPermissions) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252), contentColor = Color.White),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Grant Permission", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                                }
                                                OutlinedButton(
                                                    onClick = {
                                                        try {
                                                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                                data = Uri.fromParts("package", context.packageName, null)
                                                            }
                                                            context.startActivity(intent)
                                                        } catch (_: Exception) {}
                                                    },
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                                    border = BorderStroke(1.dp, Color(0xFF64748B))
                                                ) {
                                                    Text("Settings", fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                                is BluetoothControllerState.BluetoothDisabledOnDevice -> {
                                    Surface(
                                        color = Color(0xFF281F0B),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFFFB300)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.BluetoothDisabled, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Bluetooth Is Turned Off", color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                            Text(
                                                "Please turn on Bluetooth to connect your phone as an Xbox controller.",
                                                color = Color(0xFFCBD5E1),
                                                fontSize = 10.sp
                                            )
                                            Button(
                                                onClick = {
                                                    try {
                                                        enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                                                    } catch (_: Exception) {
                                                        try {
                                                            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                                                        } catch (_: Exception) {}
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300), contentColor = Color.Black),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Turn On Bluetooth", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                                is BluetoothControllerState.Unsupported -> {
                                    Surface(
                                        color = Color(0xFF281414),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFFF6666)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(st.reason, color = Color(0xFFFF8888), fontSize = 10.sp)
                                        }
                                    }
                                }
                                is BluetoothControllerState.Error -> {
                                    Surface(
                                        color = Color(0xFF281414),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFFF0055)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(st.message, color = Color(0xFFFF6688), fontSize = 10.sp)
                                        }
                                    }
                                }
                                else -> {
                                    Text(
                                        "Why Xbox App says \"Connect a controller\": Xbox Cloud Gaming on Samsung TV requires an authentic Bluetooth Gamepad device, not a Wi-Fi TV remote.",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                            }

                            Spacer(Modifier.height(2.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (btState is BluetoothControllerState.Connected || btState is BluetoothControllerState.ReadyToPair || btState is BluetoothControllerState.Connecting) {
                                    Button(
                                        onClick = { viewModel.bluetoothGamepadManager.stop() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155), contentColor = Color.White),
                                        modifier = Modifier.weight(1f).testTag("btn_stop_bluetooth_gamepad")
                                    ) {
                                        Text("Stop Bluetooth Mode", fontSize = 11.sp)
                                    }
                                } else {
                                    Button(
                                        onClick = { requestBluetoothAndStart() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF107C10), contentColor = Color.White),
                                        modifier = Modifier.weight(1f).testTag("btn_enable_bluetooth_gamepad")
                                    ) {
                                        Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Enable Bluetooth Gamepad", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    // AUTO-CONNECT TO PAIRED TV CARD
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF131D24),
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFFFFD600), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("AUTO-CONNECT TO PAIRED TV", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Switch(
                                    checked = prefs.btAutoConnect,
                                    onCheckedChange = { viewModel.setBtAutoConnect(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF00FF66),
                                        checkedTrackColor = Color(0xFF107C10)
                                    )
                                )
                            }

                            Text(
                                "When enabled, phone automatically connects to your paired Samsung TV as soon as you open the app.",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                lineHeight = 13.sp
                            )

                            val bondedDevices = remember(btState) { viewModel.bluetoothGamepadManager.getBondedDevices() }
                            if (bondedDevices.isNotEmpty()) {
                                Divider(color = Color(0xFF1E293B), thickness = 1.dp)
                                Text("PAIRED TVS & DEVICES:", color = Color(0xFFCBD5E1), fontSize = 10.sp, fontWeight = FontWeight.Bold)

                                for (dev in bondedDevices) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF0B141C),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = if (dev.isTvOrDisplay) Icons.Default.Tv else Icons.Default.Bluetooth,
                                                    contentDescription = null,
                                                    tint = if (dev.isTvOrDisplay) Color(0xFF00F0FF) else Color(0xFF94A3B8),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Column {
                                                    Text(
                                                        dev.name,
                                                        color = if (dev.isTvOrDisplay) Color(0xFF00FF66) else Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    )
                                                    Text(dev.address, color = Color(0xFF64748B), fontSize = 9.sp)
                                                }
                                            }

                                            Button(
                                                onClick = { viewModel.connectToBluetoothDevice(dev.address) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF107C10), contentColor = Color.White),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text("Connect ⚡", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    "No paired TVs found in Bluetooth yet. Once you pair with your Samsung TV once, it will appear here and auto-connect automatically!",
                                    color = Color(0xFF64748B),
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }

                    // Step by step Xbox App guide
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0B141C),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("HOW TO PLAY XBOX GAMES ON SAMSUNG TV:", color = Color(0xFF00FF66), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "1. Tap 'Enable Bluetooth Gamepad' above.\n" +
                                       "2. On your Samsung TV, go to:\n" +
                                       "   Settings → General → External Device Manager → Device Connect Manager → Bluetooth Device List\n" +
                                       "   (Or on 2022+ TVs: Gaming Hub → Controllers / Bluetooth Devices)\n" +
                                       "3. Select this phone (\"S!pad Gamepad\") and click 'Pair and Connect'.\n" +
                                       "4. Open Xbox App on your TV — it will detect the controller and let you play!",
                                color = Color(0xFFCBD5E1),
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
            ) {
                Text("Done", color = Color.White)
            }
        }
    )
}

@Composable
private fun DiscoveredTvRow(
    tv: DiscoveredTv,
    onConnect: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF162032),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(tv.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                Text("${tv.ipAddress}:${tv.port}", color = Color(0xFF00F0FF), fontSize = 10.sp)
            }
            Button(
                onClick = onConnect,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF), contentColor = Color.Black),
                modifier = Modifier.height(28.dp)
            ) {
                Text("Connect", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

package com.example.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.ControlType
import com.example.core.rgb.RgbColorEngine
import com.example.ui.gamepad.components.BackgroundRenderer

@Composable
fun LayoutEditorScreen(
    profileId: String,
    viewModel: LayoutEditorViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(profileId) {
        viewModel.loadProfile(profileId)
    }

    BackHandler {
        onNavigateBack()
    }

    val draft by viewModel.draftProfile.collectAsState()
    val selectedId by viewModel.selectedControlId.collectAsState()
    val selectedControl = draft.controls.find { it.id == selectedId }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("layout_editor_screen")
    ) {
        // Background
        BackgroundRenderer(
            themeName = draft.backgroundTheme,
            brightness = 0.5f,
            opacity = 0.5f
        )

        // Interactive Editable Canvas
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val screenW = maxWidth.value
            val screenH = maxHeight.value
            val screenWpx = with(density) { maxWidth.toPx() }
            val screenHpx = with(density) { maxHeight.toPx() }

            for (control in draft.controls) {
                val isSelected = control.id == selectedId
                val isBumper = control.type == ControlType.L1_BUTTON || control.type == ControlType.R1_BUTTON
                val isTrigger = control.type == ControlType.L2_TRIGGER || control.type == ControlType.R2_TRIGGER
                val isTriggerOrBumper = isTrigger || isBumper

                val controlW = control.sizeDp
                val controlH = if (isBumper) control.sizeDp * 0.50f else if (isTrigger) control.sizeDp * 0.58f else control.sizeDp

                val controlX = (control.xPercent * screenW) - (controlW / 2f)
                val controlY = (control.yPercent * screenH) - (controlH / 2f)

                Box(
                    modifier = Modifier
                        .offset { IntOffset(controlX.dp.roundToPx(), controlY.dp.roundToPx()) }
                        .width(controlW.dp)
                        .height(controlH.dp)
                        .rotate(control.rotationDeg)
                        .pointerInput(control.id, screenWpx, screenHpx) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                viewModel.selectControl(control.id)
                                val pointerId = down.id
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                    if (!change.pressed) break
                                    val delta = change.positionChange()
                                    if (delta.x != 0f || delta.y != 0f) {
                                        change.consume()
                                        if (screenWpx > 0f && screenHpx > 0f) {
                                            viewModel.moveControlByDelta(
                                                control.id,
                                                delta.x / screenWpx,
                                                delta.y / screenHpx
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        .testTag("editor_item_${control.id}")
                ) {
                    // Control Shape Preview Canvas
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val color = RgbColorEngine.parseColorHex(control.colorHex)
                        if (isTriggerOrBumper) {
                            val corner = CornerRadius(14f, 14f)
                            drawRoundRect(
                                color = color.copy(alpha = if (isSelected) 0.55f else 0.35f),
                                cornerRadius = corner
                            )
                            drawRoundRect(
                                color = if (isSelected) Color(0xFF00F0FF) else color,
                                cornerRadius = corner,
                                style = Stroke(width = if (isSelected) 4.0f else 2.0f)
                            )
                            if (isTrigger) {
                                val ribSpacing = size.width / 5f
                                for (i in 1..4) {
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.25f),
                                        start = Offset(i * ribSpacing, size.height * 0.28f),
                                        end = Offset(i * ribSpacing, size.height * 0.72f),
                                        strokeWidth = 2.5f
                                    )
                                }
                            }
                        } else {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val radius = size.width / 2f
                            drawCircle(
                                color = color.copy(alpha = if (isSelected) 0.50f else 0.35f),
                                radius = radius,
                                center = center
                            )
                            drawCircle(
                                color = if (isSelected) Color(0xFF00F0FF) else color,
                                radius = radius,
                                center = center,
                                style = Stroke(width = if (isSelected) 4.5f else 2.0f)
                            )
                        }
                    }

                    // Centered Label, Icon & Coordinates
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (control.type == ControlType.SETTINGS_BUTTON) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = if (isSelected) Color(0xFF00F0FF) else Color.White,
                                modifier = Modifier.size((control.sizeDp * 0.36f).dp.coerceIn(16.dp, 34.dp))
                            )
                        }
                        val labelText = when (control.type) {
                            ControlType.L2_TRIGGER -> "LT Trigger"
                            ControlType.R2_TRIGGER -> "RT Trigger"
                            ControlType.L1_BUTTON -> "LB Bumper"
                            ControlType.R1_BUTTON -> "RB Bumper"
                            ControlType.SETTINGS_BUTTON -> "Settings"
                            ControlType.SELECT_BUTTON -> "⧉ View"
                            ControlType.START_BUTTON -> "☰ Menu"
                            ControlType.XBOX_GUIDE_BUTTON -> "Xbox Guide"
                            ControlType.ACTION_CLUSTER -> "A B X Y"
                            ControlType.DPAD -> "D-Pad"
                            ControlType.LEFT_STICK -> "Left Stick"
                            ControlType.RIGHT_STICK -> "Right Stick"
                            else -> control.type.displayName
                        }
                        Text(
                            text = labelText,
                            color = Color.White,
                            fontSize = if (isTriggerOrBumper) (controlH * 0.25f).coerceIn(9f, 15f).sp else (control.sizeDp * 0.10f).coerceIn(9f, 15f).sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (isSelected) {
                            val dimLabel = if (isTriggerOrBumper) "${controlW.toInt()}×${controlH.toInt()}dp" else "${control.sizeDp.toInt()}dp"
                            Text(
                                text = "${(control.xPercent * 100).toInt()}%, ${(control.yPercent * 100).toInt()}% • $dimLabel",
                                color = Color(0xFF00F0FF),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Bounding Box on Selected Item
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(2.dp, Color(0xFF00F0FF), RoundedCornerShape(8.dp))
                        )
                    }
                }
            }
        }

        // Top Navigation Header & Quick Control Chips
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 10.dp, start = 12.dp, end = 12.dp)
        ) {
            // Main Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.92f),
                    modifier = Modifier.border(1.dp, Color(0xFF00F0FF), RoundedCornerShape(20.dp))
                ) {
                    Text(
                        text = "Editing: ${draft.name}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.resetToDefaults() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFD600)),
                        modifier = Modifier.testTag("btn_reset_layout")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reset", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { viewModel.saveChanges(onNavigateBack) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF), contentColor = Color.Black),
                        modifier = Modifier.testTag("btn_save_layout")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF0F172A), CircleShape)
                            .testTag("btn_cancel_layout")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Quick Control Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (ctrl in draft.controls) {
                    val isCurrent = ctrl.id == selectedId
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCurrent) Color(0xFF00F0FF) else Color(0xCC1E293B),
                        modifier = Modifier
                            .clickable { viewModel.selectControl(ctrl.id) }
                            .border(
                                width = 1.dp,
                                color = if (isCurrent) Color.White else Color(0xFF334155),
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        val chipLabel = when (ctrl.type) {
                            ControlType.L2_TRIGGER -> "⚡ LT Trigger"
                            ControlType.R2_TRIGGER -> "⚡ RT Trigger"
                            ControlType.L1_BUTTON -> "LB Bumper"
                            ControlType.R1_BUTTON -> "RB Bumper"
                            ControlType.SETTINGS_BUTTON -> "⚙️ Settings"
                            ControlType.XBOX_GUIDE_BUTTON -> "Xbox Guide"
                            else -> ctrl.type.displayName
                        }
                        Text(
                            text = chipLabel,
                            color = if (isCurrent) Color.Black else Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Bottom Property Inspector Bar
        if (selectedControl != null) {
            var isInspectorExpanded by remember { mutableStateOf(true) }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = Color(0xF2090D16),
                tonalElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Header Row: Title, Position Readout, and Minimize Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = selectedControl.type.displayName.uppercase(),
                                color = Color(0xFF00F0FF),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "X: ${(selectedControl.xPercent * 100).toInt()}%  Y: ${(selectedControl.yPercent * 100).toInt()}%  (Drag directly on screen)",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }

                        // Minimize / Expand Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { isInspectorExpanded = !isInspectorExpanded }
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isInspectorExpanded) "Minimize" else "Controls",
                                color = Color(0xFF00F0FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = if (isInspectorExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                contentDescription = "Toggle Inspector",
                                tint = Color(0xFF00F0FF),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (isInspectorExpanded) {
                        Spacer(Modifier.height(6.dp))

                        // Controls Row: [Nudge D-Pad] | [X & Y Sliders] | [Size Slider] | [Rotation Slider] | [Colors]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 1. Position Nudge D-Pad
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("NUDGE", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(2.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Button(
                                        onClick = { viewModel.nudgeControl(selectedControl.id, 0f, -0.015f) },
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.size(28.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                                    ) {
                                        Text("▲", color = Color.White, fontSize = 10.sp)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Button(
                                            onClick = { viewModel.nudgeControl(selectedControl.id, -0.015f, 0f) },
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.size(28.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                                        ) {
                                            Text("◀", color = Color.White, fontSize = 10.sp)
                                        }
                                        Button(
                                            onClick = { viewModel.nudgeControl(selectedControl.id, 0f, 0.015f) },
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.size(28.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                                        ) {
                                            Text("▼", color = Color.White, fontSize = 10.sp)
                                        }
                                        Button(
                                            onClick = { viewModel.nudgeControl(selectedControl.id, 0.015f, 0f) },
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.size(28.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                                        ) {
                                            Text("▶", color = Color.White, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }

                            // 2. Position X & Y Sliders
                            Column(modifier = Modifier.width(130.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Pos X", color = Color.White, fontSize = 10.sp)
                                    Text("${(selectedControl.xPercent * 100).toInt()}%", color = Color(0xFF00F0FF), fontSize = 10.sp)
                                }
                                Slider(
                                    value = selectedControl.xPercent,
                                    onValueChange = { viewModel.updateControlPosition(selectedControl.id, it, selectedControl.yPercent) },
                                    valueRange = 0.02f..0.98f,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFF00F0FF), activeTrackColor = Color(0xFF00F0FF)),
                                    modifier = Modifier.height(26.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Pos Y", color = Color.White, fontSize = 10.sp)
                                    Text("${(selectedControl.yPercent * 100).toInt()}%", color = Color(0xFF00F0FF), fontSize = 10.sp)
                                }
                                Slider(
                                    value = selectedControl.yPercent,
                                    onValueChange = { viewModel.updateControlPosition(selectedControl.id, selectedControl.xPercent, it) },
                                    valueRange = 0.04f..0.96f,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFF00F0FF), activeTrackColor = Color(0xFF00F0FF)),
                                    modifier = Modifier.height(26.dp)
                                )
                            }

                            // 3. Size Customization: Dimension readout, precision +/- stepper, smooth slider, and quick presets
                            Column(modifier = Modifier.width(180.dp)) {
                                val isBumper = selectedControl.type == ControlType.L1_BUTTON || selectedControl.type == ControlType.R1_BUTTON
                                val isTrigger = selectedControl.type == ControlType.L2_TRIGGER || selectedControl.type == ControlType.R2_TRIGGER

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Size", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    val dimText = if (isTrigger) {
                                        "${selectedControl.sizeDp.toInt()}w × ${(selectedControl.sizeDp * 0.58f).toInt()}h"
                                    } else if (isBumper) {
                                        "${selectedControl.sizeDp.toInt()}w × ${(selectedControl.sizeDp * 0.50f).toInt()}h"
                                    } else {
                                        "${selectedControl.sizeDp.toInt()} dp"
                                    }
                                    Text(dimText, color = Color(0xFF00F0FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF1E293B),
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clickable { viewModel.updateControlSize(selectedControl.id, selectedControl.sizeDp - 5f) }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("–", color = Color(0xFF00F0FF), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Slider(
                                        value = selectedControl.sizeDp,
                                        onValueChange = { viewModel.updateControlSize(selectedControl.id, it) },
                                        valueRange = 40f..260f,
                                        colors = SliderDefaults.colors(thumbColor = Color(0xFF00F0FF), activeTrackColor = Color(0xFF00F0FF)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(30.dp)
                                            .testTag("slider_control_size")
                                    )

                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF1E293B),
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clickable { viewModel.updateControlSize(selectedControl.id, selectedControl.sizeDp + 5f) }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("+", color = Color(0xFF00F0FF), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // Quick Presets row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf(
                                        "S" to 60f,
                                        "M" to 80f,
                                        "L" to 110f,
                                        "XL" to 140f
                                    ).forEach { (label, sz) ->
                                        val isCurrent = kotlin.math.abs(selectedControl.sizeDp - sz) < 6f
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isCurrent) Color(0xFF00F0FF) else Color(0xFF1E293B),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { viewModel.updateControlSize(selectedControl.id, sz) }
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (isCurrent) Color.Black else Color(0xFFCBD5E1),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // 4. Rotation Slider
                            Column(modifier = Modifier.width(130.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Rotation", color = Color.White, fontSize = 10.sp)
                                    Text("${selectedControl.rotationDeg.toInt()}°", color = Color(0xFFBD00FF), fontSize = 10.sp)
                                }
                                Slider(
                                    value = selectedControl.rotationDeg,
                                    onValueChange = { viewModel.updateControlRotation(selectedControl.id, it) },
                                    valueRange = -180f..180f,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFFBD00FF), activeTrackColor = Color(0xFFBD00FF)),
                                    modifier = Modifier.height(32.dp).testTag("slider_control_rotation")
                                )
                            }

                            // 5. Color Palette Choices
                            Column {
                                Text("COLOR", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val palette = listOf("#00F0FF", "#107C10", "#BD00FF", "#FF007F", "#FF6B00", "#FFE600", "#00FF66", "#FFFFFF")
                                    for (hex in palette) {
                                        val color = RgbColorEngine.parseColorHex(hex)
                                        val isChosen = selectedControl.colorHex.equals(hex, ignoreCase = true)
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(color, CircleShape)
                                                .border(
                                                    width = if (isChosen) 2.5.dp else 1.dp,
                                                    color = if (isChosen) Color.White else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                                .clickable { viewModel.updateControlColor(selectedControl.id, hex) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

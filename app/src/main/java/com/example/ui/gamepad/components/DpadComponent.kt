package com.example.ui.gamepad.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.model.GamepadButton

@Composable
fun DpadComponent(
    sizeDp: Float = 145f,
    accentColor: Color,
    rgbGlowColor: Color,
    onButtonStateChanged: (GamepadButton, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var pressedUp by remember { mutableStateOf(false) }
    var pressedDown by remember { mutableStateOf(false) }
    var pressedLeft by remember { mutableStateOf(false) }
    var pressedRight by remember { mutableStateOf(false) }

    fun updateStates(up: Boolean, down: Boolean, left: Boolean, right: Boolean) {
        if (pressedUp != up) {
            pressedUp = up
            onButtonStateChanged(GamepadButton.DPAD_UP, up)
        }
        if (pressedDown != down) {
            pressedDown = down
            onButtonStateChanged(GamepadButton.DPAD_DOWN, down)
        }
        if (pressedLeft != left) {
            pressedLeft = left
            onButtonStateChanged(GamepadButton.DPAD_LEFT, left)
        }
        if (pressedRight != right) {
            pressedRight = right
            onButtonStateChanged(GamepadButton.DPAD_RIGHT, right)
        }
    }

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .testTag("dpad_controller"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(sizeDp.dp)
                .pointerInput(Unit) {
                    val w = size.width
                    val h = size.height
                    val cx = w / 2f
                    val cy = h / 2f
                    val deadzone = w * 0.15f

                    detectTapGestures(
                        onPress = { offset ->
                            val dx = offset.x - cx
                            val dy = offset.y - cy
                            val dist = kotlin.math.sqrt(dx * dx + dy * dy)

                            if (dist > deadzone) {
                                val up = dy < -deadzone && kotlin.math.abs(dx) < kotlin.math.abs(dy) * 1.6f
                                val down = dy > deadzone && kotlin.math.abs(dx) < kotlin.math.abs(dy) * 1.6f
                                val left = dx < -deadzone && kotlin.math.abs(dy) < kotlin.math.abs(dx) * 1.6f
                                val right = dx > deadzone && kotlin.math.abs(dy) < kotlin.math.abs(dx) * 1.6f
                                updateStates(up, down, left, right)
                            }

                            tryAwaitRelease()
                            updateStates(false, false, false, false)
                        }
                    )
                }
                .pointerInput(Unit) {
                    val w = size.width
                    val h = size.height
                    val cx = w / 2f
                    val cy = h / 2f
                    val deadzone = w * 0.15f

                    detectDragGestures(
                        onDrag = { change, _ ->
                            change.consume()
                            val dx = change.position.x - cx
                            val dy = change.position.y - cy
                            val dist = kotlin.math.sqrt(dx * dx + dy * dy)

                            if (dist > deadzone) {
                                val up = dy < -deadzone && kotlin.math.abs(dx) < kotlin.math.abs(dy) * 1.6f
                                val down = dy > deadzone && kotlin.math.abs(dx) < kotlin.math.abs(dy) * 1.6f
                                val left = dx < -deadzone && kotlin.math.abs(dy) < kotlin.math.abs(dx) * 1.6f
                                val right = dx > deadzone && kotlin.math.abs(dy) < kotlin.math.abs(dx) * 1.6f
                                updateStates(up, down, left, right)
                            } else {
                                updateStates(false, false, false, false)
                            }
                        },
                        onDragEnd = { updateStates(false, false, false, false) },
                        onDragCancel = { updateStates(false, false, false, false) }
                    )
                }
        ) {
            val w = size.width
            val h = size.height
            val armWidth = w * 0.34f
            val armLength = (w - armWidth) / 2f
            val corner = CornerRadius(14f, 14f)

            // 1. Ambient Glow Under Cross
            val anyPressed = pressedUp || pressedDown || pressedLeft || pressedRight
            if (anyPressed) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(rgbGlowColor.copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(w / 2f, h / 2f),
                        radius = w * 0.60f
                    ),
                    radius = w * 0.60f,
                    center = Offset(w / 2f, h / 2f)
                )
            }

            // 2. Cross Beveled Background
            val baseCrossBrush = Brush.radialGradient(
                colors = listOf(Color(0xFF1E283D), Color(0xFF111826), Color(0xFF090D15)),
                center = Offset(w / 2f, h / 2f),
                radius = w / 2f
            )

            // Horizontal Arm
            drawRoundRect(
                brush = baseCrossBrush,
                topLeft = Offset(0f, (h - armWidth) / 2f),
                size = Size(w, armWidth),
                cornerRadius = corner
            )
            // Vertical Arm
            drawRoundRect(
                brush = baseCrossBrush,
                topLeft = Offset((w - armWidth) / 2f, 0f),
                size = Size(armWidth, h),
                cornerRadius = corner
            )

            // 3. Metallic Outer Border
            val crossBorderBrush = Brush.linearGradient(
                listOf(rgbGlowColor.copy(alpha = 0.7f), Color(0xFF334155), rgbGlowColor.copy(alpha = 0.4f)),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            drawRoundRect(
                brush = crossBorderBrush,
                topLeft = Offset(0f, (h - armWidth) / 2f),
                size = Size(w, armWidth),
                cornerRadius = corner,
                style = Stroke(width = 1.8f)
            )
            drawRoundRect(
                brush = crossBorderBrush,
                topLeft = Offset((w - armWidth) / 2f, 0f),
                size = Size(armWidth, h),
                cornerRadius = corner,
                style = Stroke(width = 1.8f)
            )

            // 4. Pressed Active Highlights & Direction Arrows
            val activeColor = rgbGlowColor.copy(alpha = 0.50f)

            // UP
            if (pressedUp) {
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset((w - armWidth) / 2f, 0f),
                    size = Size(armWidth, armLength),
                    cornerRadius = corner
                )
            }
            val arrowUp = Path().apply {
                moveTo(w / 2f, armLength * 0.30f)
                lineTo(w / 2f - 14f, armLength * 0.65f)
                lineTo(w / 2f + 14f, armLength * 0.65f)
                close()
            }
            drawPath(arrowUp, if (pressedUp) Color.White else rgbGlowColor)

            // DOWN
            if (pressedDown) {
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset((w - armWidth) / 2f, h - armLength),
                    size = Size(armWidth, armLength),
                    cornerRadius = corner
                )
            }
            val arrowDown = Path().apply {
                moveTo(w / 2f, h - armLength * 0.30f)
                lineTo(w / 2f - 14f, h - armLength * 0.65f)
                lineTo(w / 2f + 14f, h - armLength * 0.65f)
                close()
            }
            drawPath(arrowDown, if (pressedDown) Color.White else rgbGlowColor)

            // LEFT
            if (pressedLeft) {
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(0f, (h - armWidth) / 2f),
                    size = Size(armLength, armWidth),
                    cornerRadius = corner
                )
            }
            val arrowLeft = Path().apply {
                moveTo(armLength * 0.30f, h / 2f)
                lineTo(armLength * 0.65f, h / 2f - 14f)
                lineTo(armLength * 0.65f, h / 2f + 14f)
                close()
            }
            drawPath(arrowLeft, if (pressedLeft) Color.White else rgbGlowColor)

            // RIGHT
            if (pressedRight) {
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(w - armLength, (h - armWidth) / 2f),
                    size = Size(armLength, armWidth),
                    cornerRadius = corner
                )
            }
            val arrowRight = Path().apply {
                moveTo(w - armLength * 0.30f, h / 2f)
                lineTo(w - armLength * 0.65f, h / 2f - 14f)
                lineTo(w - armLength * 0.65f, h / 2f + 14f)
                close()
            }
            drawPath(arrowRight, if (pressedRight) Color.White else rgbGlowColor)

            // 5. Center Ergonomic Thumb Pivot (Concave Bowl)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF090D15), Color(0xFF1E283D)),
                    center = Offset(w / 2f, h / 2f),
                    radius = armWidth * 0.38f
                ),
                radius = armWidth * 0.38f,
                center = Offset(w / 2f, h / 2f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.2f),
                radius = armWidth * 0.38f,
                center = Offset(w / 2f, h / 2f),
                style = Stroke(width = 1.0f)
            )
        }
    }
}

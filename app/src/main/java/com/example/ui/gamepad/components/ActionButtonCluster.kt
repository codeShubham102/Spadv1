package com.example.ui.gamepad.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.GamepadButton

@Composable
fun ActionButtonCluster(
    sizeDp: Float = 145f,
    rgbGlowColor: Color,
    onButtonStateChanged: (GamepadButton, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonSize = sizeDp * 0.35f
    val offsetDistance = sizeDp * 0.31f

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .testTag("action_buttons_cluster"),
        contentAlignment = Alignment.Center
    ) {
        // Y Button (Top) - Iconic Xbox Amber Gold
        ActionButton(
            label = "Y",
            button = GamepadButton.Y,
            accentColor = Color(0xFFFFB900),
            glowColor = rgbGlowColor,
            sizeDp = buttonSize,
            onStateChanged = onButtonStateChanged,
            modifier = Modifier.offset(y = (-offsetDistance).dp)
        )

        // A Button (Bottom) - Iconic Xbox Emerald Green
        ActionButton(
            label = "A",
            button = GamepadButton.A,
            accentColor = Color(0xFF10E040),
            glowColor = rgbGlowColor,
            sizeDp = buttonSize,
            onStateChanged = onButtonStateChanged,
            modifier = Modifier.offset(y = offsetDistance.dp)
        )

        // X Button (Left) - Iconic Xbox Azure/Sapphire Blue
        ActionButton(
            label = "X",
            button = GamepadButton.X,
            accentColor = Color(0xFF00A4EF),
            glowColor = rgbGlowColor,
            sizeDp = buttonSize,
            onStateChanged = onButtonStateChanged,
            modifier = Modifier.offset(x = (-offsetDistance).dp)
        )

        // B Button (Right) - Iconic Xbox Crimson Red
        ActionButton(
            label = "B",
            button = GamepadButton.B,
            accentColor = Color(0xFFFF1744),
            glowColor = rgbGlowColor,
            sizeDp = buttonSize,
            onStateChanged = onButtonStateChanged,
            modifier = Modifier.offset(x = offsetDistance.dp)
        )
    }
}

@Composable
private fun ActionButton(
    label: String,
    button: GamepadButton,
    accentColor: Color,
    glowColor: Color,
    sizeDp: Float,
    onStateChanged: (GamepadButton, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(),
        label = "btnScale"
    )

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .scale(scale)
            .testTag("btn_${label.lowercase()}")
            .pointerInput(button) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onStateChanged(button, true)
                        tryAwaitRelease()
                        isPressed = false
                        onStateChanged(button, false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(sizeDp.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.width / 2f

            // 1. Outer Ambient Glow Halo
            val haloAlpha = if (isPressed) 0.55f else 0.20f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accentColor.copy(alpha = haloAlpha), Color.Transparent),
                    center = center,
                    radius = radius * 1.35f
                ),
                radius = radius * 1.35f,
                center = center
            )

            // 2. High-End Jewel Button Body (Specular Radial Gradient)
            val bodyGradient = if (isPressed) {
                Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.5f),
                        Color(0xFF1E2638),
                        Color(0xFF0F1420)
                    ),
                    center = center,
                    radius = radius
                )
            } else {
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF26334A),
                        Color(0xFF131A26),
                        Color(0xFF0A0E17)
                    ),
                    center = Offset(center.x, center.y - radius * 0.25f), // Top specular reflection
                    radius = radius * 1.1f
                )
            }
            drawCircle(brush = bodyGradient, radius = radius, center = center)

            // 3. Metallic Outer Rim
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        if (isPressed) Color.White else accentColor,
                        accentColor.copy(alpha = 0.4f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                ),
                radius = radius,
                center = center,
                style = Stroke(width = if (isPressed) 2.8f else 1.8f)
            )

            // 4. Subtle Inner Inset Bevel
            drawCircle(
                color = Color.White.copy(alpha = if (isPressed) 0.35f else 0.12f),
                radius = radius * 0.86f,
                center = center,
                style = Stroke(width = 1.0f)
            )

            // 5. Specular Acrylic Dome Glass Reflection Arc (Double-Shot Cap)
            drawOval(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isPressed) 0.50f else 0.30f),
                        Color.Transparent
                    ),
                    startY = center.y - radius * 0.78f,
                    endY = center.y + radius * 0.15f
                ),
                topLeft = Offset(center.x - radius * 0.60f, center.y - radius * 0.78f),
                size = Size(radius * 1.20f, radius * 0.65f)
            )
        }

        // Crisp Double-Shot Embossed Character
        Box(contentAlignment = Alignment.Center) {
            // Drop shadow for tactile 3D relief
            Text(
                text = label,
                color = Color.Black.copy(alpha = 0.65f),
                fontSize = (sizeDp * 0.44f).sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.offset(x = 1.2.dp, y = 1.2.dp)
            )
            // Vibrant Illuminated Letter
            Text(
                text = label,
                color = if (isPressed) Color.White else accentColor,
                fontSize = (sizeDp * 0.44f).sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

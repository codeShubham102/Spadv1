package com.example.ui.gamepad.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.model.StickConfig
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun AnalogStickComponent(
    config: StickConfig,
    accentColor: Color,
    rgbGlowColor: Color,
    tag: String,
    onPositionChanged: (rawX: Float, rawY: Float) -> Unit,
    onClickL3R3: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val stickOffsetAnimX = remember { Animatable(0f) }
    val stickOffsetAnimY = remember { Animatable(0f) }

    Box(
        modifier = modifier
            .size(config.sizeDp.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(config.sizeDp.dp)
                .pointerInput(config) {
                    val radius = size.width / 2f
                    val maxTravel = radius * 0.60f

                    detectDragGestures(
                        onDragStart = { offset ->
                            val dx = offset.x - radius
                            val dy = offset.y - radius
                            val dist = sqrt(dx * dx + dy * dy)
                            val clampedDist = dist.coerceAtMost(maxTravel)
                            val angle = atan2(dy, dx)
                            val curX = cos(angle) * clampedDist
                            val curY = sin(angle) * clampedDist

                            scope.launch {
                                stickOffsetAnimX.snapTo(curX)
                                stickOffsetAnimY.snapTo(curY)
                            }
                            onPositionChanged(curX / maxTravel, -(curY / maxTravel))
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val newX = stickOffsetAnimX.value + dragAmount.x
                            val newY = stickOffsetAnimY.value + dragAmount.y
                            val dist = sqrt(newX * newX + newY * newY)
                            val angle = atan2(newY, newX)

                            val clampedDist = dist.coerceAtMost(maxTravel)
                            val finalX = cos(angle) * clampedDist
                            val finalY = sin(angle) * clampedDist

                            scope.launch {
                                stickOffsetAnimX.snapTo(finalX)
                                stickOffsetAnimY.snapTo(finalY)
                            }
                            onPositionChanged(finalX / maxTravel, -(finalY / maxTravel))
                        },
                        onDragEnd = {
                            scope.launch {
                                launch { stickOffsetAnimX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
                                launch { stickOffsetAnimY.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
                            }
                            onPositionChanged(0f, 0f)
                        },
                        onDragCancel = {
                            scope.launch {
                                launch { stickOffsetAnimX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
                                launch { stickOffsetAnimY.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
                            }
                            onPositionChanged(0f, 0f)
                        }
                    )
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.width / 2f
            val thumbRadius = outerRadius * 0.42f
            val maxTravel = outerRadius * 0.60f

            // 1. Ambient RGB Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(rgbGlowColor.copy(alpha = 0.28f), Color.Transparent),
                    center = center,
                    radius = outerRadius * 1.15f
                ),
                radius = outerRadius * 1.15f,
                center = center
            )

            // 2. Beveled Metallic Well Base
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF0F1524), Color(0xFF070A10)),
                    center = center,
                    radius = outerRadius * 0.94f
                ),
                radius = outerRadius * 0.94f,
                center = center
            )

            // Collar Border
            drawCircle(
                brush = Brush.sweepGradient(
                    listOf(
                        rgbGlowColor.copy(alpha = 0.6f),
                        Color(0xFF1E293B),
                        rgbGlowColor.copy(alpha = 0.3f),
                        Color(0xFF334155),
                        rgbGlowColor.copy(alpha = 0.6f)
                    ),
                    center = center
                ),
                radius = outerRadius * 0.94f,
                center = center,
                style = Stroke(width = 2.2f)
            )

            // 3. Directional Compass Crosshairs on Base
            val chLength = outerRadius * 0.88f
            val chColor = Color(0xFF334155).copy(alpha = 0.35f)
            drawLine(chColor, Offset(center.x - chLength, center.y), Offset(center.x + chLength, center.y), 1f)
            drawLine(chColor, Offset(center.x, center.y - chLength), Offset(center.x, center.y + chLength), 1f)

            // 4. Dead-zone Guide Ring
            val deadzoneRadius = maxTravel * config.deadzonePercent
            if (deadzoneRadius > 4f) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.12f),
                    radius = deadzoneRadius,
                    center = center,
                    style = Stroke(width = 1f)
                )
            }

            // 5. Thumbstick Cap
            val thumbCenter = center + Offset(stickOffsetAnimX.value, stickOffsetAnimY.value)

            // Thumb Cap Shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.45f),
                radius = thumbRadius * 1.05f,
                center = thumbCenter + Offset(0f, 3f)
            )

            // Thumb Cap Outer Rubber Rim
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF334155), Color(0xFF1A2232), Color(0xFF0E131E)),
                    center = thumbCenter,
                    radius = thumbRadius
                ),
                radius = thumbRadius,
                center = thumbCenter
            )

            // Glowing Outer Ring on Thumb Cap
            drawCircle(
                color = rgbGlowColor,
                radius = thumbRadius,
                center = thumbCenter,
                style = Stroke(width = 1.8f)
            )

            // Concave Center Dish
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF0A0E17), Color(0xFF1E283C)),
                    center = thumbCenter,
                    radius = thumbRadius * 0.62f
                ),
                radius = thumbRadius * 0.62f,
                center = thumbCenter
            )

            // Concentric Grip Textures (DualSense style)
            drawCircle(
                color = Color.White.copy(alpha = 0.25f),
                radius = thumbRadius * 0.42f,
                center = thumbCenter,
                style = Stroke(width = 1.2f)
            )
            drawCircle(
                color = rgbGlowColor.copy(alpha = 0.5f),
                radius = thumbRadius * 0.20f,
                center = thumbCenter,
                style = Stroke(width = 1.5f)
            )
        }
    }
}

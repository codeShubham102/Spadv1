package com.example.ui.gamepad.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.GamepadButton

@Composable
fun BumperComponent(
    label: String,
    button: GamepadButton,
    accentColor: Color,
    rgbGlowColor: Color,
    sizeDp: Float = 78f,
    widthDp: Float = sizeDp,
    heightDp: Float = sizeDp * 0.50f,
    onStateChanged: (GamepadButton, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .width(widthDp.dp)
            .height(heightDp.dp)
            .testTag("bumper_${label.lowercase()}")
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
        Canvas(modifier = Modifier.matchParentSize()) {
            val corner = CornerRadius(
                (size.width * 0.18f).coerceIn(8f, 22f),
                (size.height * 0.36f).coerceIn(8f, 22f)
            )
            val fill = if (isPressed) {
                Brush.verticalGradient(
                    listOf(accentColor.copy(alpha = 0.5f), Color(0xFF1B2436))
                )
            } else {
                Brush.verticalGradient(
                    listOf(Color(0xFF243048), Color(0xFF101726), Color(0xFF090D16))
                )
            }

            // Body
            drawRoundRect(brush = fill, cornerRadius = corner)

            // Bevel Top Accent
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.3f),
                        accentColor,
                        Color.White.copy(alpha = 0.3f)
                    )
                ),
                cornerRadius = corner,
                style = Stroke(width = if (isPressed) 2.5f else 1.5f)
            )
        }

        Text(
            text = label,
            color = if (isPressed) Color.White else accentColor,
            fontSize = (heightDp * 0.32f).coerceIn(10f, 18f).sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun TriggerComponent(
    label: String,
    accentColor: Color,
    rgbGlowColor: Color,
    sizeDp: Float = 78f,
    widthDp: Float = sizeDp,
    heightDp: Float = sizeDp * 0.58f,
    onTriggerChanged: (Float) -> Unit,
    onDigitalStateChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var pressure by remember { mutableFloatStateOf(0.0f) }
    val animatedPressure by animateFloatAsState(targetValue = pressure, label = "triggerP")

    Box(
        modifier = modifier
            .width(widthDp.dp)
            .height(heightDp.dp)
            .testTag("trigger_${label.lowercase()}")
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressure = 1.0f
                        onTriggerChanged(1.0f)
                        onDigitalStateChanged(true)
                        tryAwaitRelease()
                        pressure = 0.0f
                        onTriggerChanged(0.0f)
                        onDigitalStateChanged(false)
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        pressure = (pressure + dragAmount.y / 50f).coerceIn(0f, 1f)
                        onTriggerChanged(pressure)
                        onDigitalStateChanged(pressure > 0.5f)
                    },
                    onDragEnd = {
                        pressure = 0.0f
                        onTriggerChanged(0.0f)
                        onDigitalStateChanged(false)
                    },
                    onDragCancel = {
                        pressure = 0.0f
                        onTriggerChanged(0.0f)
                        onDigitalStateChanged(false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val corner = CornerRadius(
                (w * 0.18f).coerceIn(8f, 24f),
                (h * 0.32f).coerceIn(8f, 24f)
            )

            // 1. Deep Ergonomic Housing
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF1E283E), Color(0xFF0F1522), Color(0xFF070A10))
                ),
                cornerRadius = corner
            )

            // 2. Analog Pressure Fill Gauge
            if (animatedPressure > 0.02f) {
                val fillH = h * animatedPressure
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(accentColor.copy(alpha = 0.4f), accentColor.copy(alpha = 0.9f)),
                        startY = h - fillH,
                        endY = h
                    ),
                    topLeft = Offset(0f, h - fillH),
                    size = Size(w, fillH),
                    cornerRadius = corner
                )
            }

            // 3. Ergonomic Grip Texture Ribs
            val ribSpacing = w / 5f
            for (i in 1..4) {
                drawLine(
                    color = Color.White.copy(alpha = 0.18f),
                    start = Offset(i * ribSpacing, h * 0.28f),
                    end = Offset(i * ribSpacing, h * 0.72f),
                    strokeWidth = (w * 0.02f).coerceIn(1.5f, 3.5f),
                    cap = StrokeCap.Round
                )
            }

            // 4. Glowing Metallic Border
            drawRoundRect(
                color = if (pressure > 0.8f) Color.White else rgbGlowColor,
                cornerRadius = corner,
                style = Stroke(width = if (pressure > 0.5f) 2.4f else 1.5f)
            )
        }

        Text(
            text = if (pressure > 0.05f) "$label ${(pressure * 100).toInt()}%" else label,
            color = if (pressure > 0.5f) Color.White else accentColor,
            fontSize = (heightDp * 0.28f).coerceIn(10f, 20f).sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun SystemMenuButton(
    label: String,
    button: GamepadButton,
    accentColor: Color,
    rgbGlowColor: Color = Color(0xFF00F0FF),
    sizeDp: Float = 46f,
    onStateChanged: (GamepadButton, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(),
        label = "menuBtnScale"
    )

    val isViewButton = button == GamepadButton.SELECT || label.equals("VIEW", true) || label.equals("SELECT", true)

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .scale(scale)
            .testTag("menu_${label.lowercase()}")
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

            // 1. Soft Ambient / RGB Glow Halo
            val glowColor = if (isPressed) Color.White else accentColor
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = if (isPressed) 0.60f else 0.20f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 1.35f
                ),
                radius = radius * 1.35f,
                center = center
            )

            // 2. High-End Dark Concave Button Dome
            val domeBrush = if (isPressed) {
                Brush.radialGradient(
                    colors = listOf(Color(0xFF26324A), Color(0xFF141C2B), Color(0xFF0A0F18)),
                    center = center,
                    radius = radius
                )
            } else {
                Brush.radialGradient(
                    colors = listOf(Color(0xFF222C3E), Color(0xFF131B27), Color(0xFF0A0E16)),
                    center = Offset(center.x, center.y - radius * 0.25f),
                    radius = radius
                )
            }
            drawCircle(brush = domeBrush, radius = radius, center = center)

            // 3. Specular Metallic Bezel Rim
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        if (isPressed) Color.White else Color(0xFF94A3B8),
                        Color(0xFF334155),
                        Color(0xFF0F172A)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                ),
                radius = radius,
                center = center,
                style = Stroke(width = if (isPressed) 2.5f else 1.8f)
            )

            // 4. Subtle Inner Inset Bevel Ring
            drawCircle(
                color = Color.White.copy(alpha = if (isPressed) 0.25f else 0.08f),
                radius = radius * 0.85f,
                center = center,
                style = Stroke(width = 1.0f)
            )

            // 5. Authentic Console Icon Drawing (View: 2 Overlapping Windows, Menu: 3 Horizontal Lines)
            val iconColor = if (isPressed) Color.White else Color(0xFFE2E8F0)
            val strokeW = (sizeDp * 0.050f).coerceIn(2.0f, 3.2f)

            if (isViewButton) {
                // AUTHENTIC XBOX "VIEW" ICON: Two Overlapping Windows / Squares perfectly centered
                val boxSize = size.width * 0.28f
                val offset = boxSize * 0.36f
                val corner = CornerRadius(3.5f, 3.5f)

                // Rear window (upper-left)
                drawRoundRect(
                    color = iconColor.copy(alpha = if (isPressed) 0.90f else 0.75f),
                    topLeft = Offset(center.x - boxSize / 2f - offset / 2f, center.y - boxSize / 2f - offset / 2f),
                    size = Size(boxSize, boxSize),
                    cornerRadius = corner,
                    style = Stroke(width = strokeW)
                )

                // Front window background cutout (masks rear window with concave cap fill)
                val cutoutColor = if (isPressed) Color(0xFF26324A) else Color(0xFF131B27)
                drawRoundRect(
                    color = cutoutColor,
                    topLeft = Offset(center.x - boxSize / 2f + offset / 2f, center.y - boxSize / 2f + offset / 2f),
                    size = Size(boxSize, boxSize),
                    cornerRadius = corner
                )

                // Front window (lower-right)
                drawRoundRect(
                    color = iconColor,
                    topLeft = Offset(center.x - boxSize / 2f + offset / 2f, center.y - boxSize / 2f + offset / 2f),
                    size = Size(boxSize, boxSize),
                    cornerRadius = corner,
                    style = Stroke(width = strokeW)
                )
            } else {
                // AUTHENTIC XBOX "MENU" ICON: Three Horizontal Parallel Lines (Hamburger) perfectly centered
                val barW = size.width * 0.40f
                val barH = (size.height * 0.058f).coerceIn(2.2f, 3.5f)
                val spacing = size.height * 0.12f

                for (i in -1..1) {
                    drawRoundRect(
                        color = iconColor,
                        topLeft = Offset(center.x - barW / 2f, center.y + (i * spacing) - barH / 2f),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(barH / 2f, barH / 2f)
                    )
                }
            }
        }
    }
}

@Composable
fun StickClickButton(
    label: String,
    button: GamepadButton,
    accentColor: Color,
    rgbGlowColor: Color,
    onStateChanged: (GamepadButton, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(42.dp)
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
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.width / 2f

            if (isPressed) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accentColor.copy(alpha = 0.55f), Color.Transparent),
                        center = center,
                        radius = radius * 1.3f
                    ),
                    radius = radius * 1.3f,
                    center = center
                )
            }

            val fillBrush = if (isPressed) {
                Brush.radialGradient(
                    colors = listOf(accentColor.copy(alpha = 0.45f), Color(0xFF1E2638)),
                    center = center,
                    radius = radius
                )
            } else {
                Brush.radialGradient(
                    colors = listOf(Color(0xFF222C40), Color(0xFF101724)),
                    center = center,
                    radius = radius
                )
            }
            drawCircle(brush = fillBrush, radius = radius, center = center)

            drawCircle(
                color = if (isPressed) Color.White else rgbGlowColor,
                radius = radius,
                center = center,
                style = Stroke(width = if (isPressed) 2.4f else 1.5f)
            )
        }

        Text(
            text = label,
            color = if (isPressed) Color.White else accentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun XboxGuideButton(
    accentColor: Color = Color(0xFF107C10),
    rgbGlowColor: Color = Color(0xFF00FF66),
    sizeDp: Float = 50f,
    onStateChanged: (GamepadButton, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(),
        label = "guideScale"
    )

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .scale(scale)
            .testTag("btn_xbox_guide")
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onStateChanged(GamepadButton.GUIDE, true)
                        tryAwaitRelease()
                        isPressed = false
                        onStateChanged(GamepadButton.GUIDE, false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(sizeDp.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.width / 2f

            // 1. Radiant Glowing Ambient Halo
            val glowColor = if (isPressed) Color(0xFFCCFFCC) else rgbGlowColor
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = if (isPressed) 0.80f else 0.40f),
                        accentColor.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 1.45f
                ),
                radius = radius * 1.45f,
                center = center
            )

            // 2. High-End Turned Metallic Bezel Rim (Specular Gradient)
            val bezelBrush = Brush.linearGradient(
                colors = listOf(
                    if (isPressed) Color.White else Color(0xFFE2E8F0),
                    Color(0xFF64748B),
                    Color(0xFF1E293B),
                    Color(0xFF0F172A)
                ),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height)
            )
            drawCircle(brush = bezelBrush, radius = radius, center = center)

            // 3. Inner Dark Cavity Ring
            drawCircle(
                color = Color(0xFF060D08),
                radius = radius * 0.92f,
                center = center
            )

            // 4. Glossy Acrylic Dome (Nexus Sphere)
            val domeBrush = if (isPressed) {
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00FF66).copy(alpha = 0.5f),
                        Color(0xFF104A1B),
                        Color(0xFF041207)
                    ),
                    center = center,
                    radius = radius * 0.88f
                )
            } else {
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF2E4D32),
                        Color(0xFF132A17),
                        Color(0xFF071209)
                    ),
                    center = Offset(center.x, center.y - radius * 0.25f),
                    radius = radius * 0.88f
                )
            }
            drawCircle(brush = domeBrush, radius = radius * 0.88f, center = center)

            // 5. Specular Dome Glass Reflection Arc (Top highlight)
            drawOval(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isPressed) 0.50f else 0.28f),
                        Color.Transparent
                    ),
                    startY = center.y - radius * 0.80f,
                    endY = center.y
                ),
                topLeft = Offset(center.x - radius * 0.55f, center.y - radius * 0.80f),
                size = Size(radius * 1.10f, radius * 0.60f)
            )

            // 6. Signature Illuminated Xbox Nexus Emblem
            val xRadius = radius * 0.52f
            val emblemColor = if (isPressed) Color.White else rgbGlowColor
            val strokeW = (radius * 0.16f).coerceIn(3.0f, 5.2f)

            // Glowing underlay bloom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(emblemColor.copy(alpha = if (isPressed) 0.9f else 0.5f), Color.Transparent),
                    center = center,
                    radius = xRadius * 1.25f
                ),
                radius = xRadius * 1.25f,
                center = center
            )

            // Sweeping curved Xbox X crossbars with rounded stroke caps
            val path1 = Path().apply {
                moveTo(center.x - xRadius * 0.72f, center.y - xRadius * 0.78f)
                cubicTo(
                    center.x - xRadius * 0.25f, center.y - xRadius * 0.15f,
                    center.x + xRadius * 0.25f, center.y + xRadius * 0.15f,
                    center.x + xRadius * 0.72f, center.y + xRadius * 0.78f
                )
            }
            drawPath(
                path = path1,
                color = emblemColor,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )

            val path2 = Path().apply {
                moveTo(center.x + xRadius * 0.72f, center.y - xRadius * 0.78f)
                cubicTo(
                    center.x + xRadius * 0.25f, center.y - xRadius * 0.15f,
                    center.x - xRadius * 0.25f, center.y + xRadius * 0.15f,
                    center.x - xRadius * 0.72f, center.y + xRadius * 0.78f
                )
            }
            drawPath(
                path = path2,
                color = emblemColor,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )

            // Center radiant jewel spark
            drawCircle(
                color = if (isPressed) Color.White else Color(0xFFCCFFCC),
                radius = (radius * 0.08f).coerceAtLeast(1.8f),
                center = center
            )
        }
    }
}

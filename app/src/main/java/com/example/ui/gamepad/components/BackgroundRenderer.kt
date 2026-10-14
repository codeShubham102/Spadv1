package com.example.ui.gamepad.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun BackgroundRenderer(
    themeName: String,
    brightness: Float = 0.85f,
    opacity: Float = 0.70f,
    modifier: Modifier = Modifier
) {
    val alphaFactor = (opacity * brightness).coerceIn(0.05f, 1.0f)

    Box(modifier = modifier.fillMaxSize()) {
        when (themeName) {
            "cyber_grid" -> CyberGridBackground(alpha = alphaFactor)
            "neon_wave" -> NeonWaveBackground(alpha = alphaFactor)
            "dark_minimal" -> DarkMinimalBackground(alpha = alphaFactor)
            else -> DeepVoidBackground()
        }
    }
}

@Composable
private fun CyberGridBackground(alpha: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // 1. Dark radial gradient backdrop
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF0F172A).copy(alpha = alpha),
                    Color(0xFF070B12).copy(alpha = alpha),
                    Color(0xFF030508)
                ),
                center = Offset(width / 2f, height / 2f),
                radius = width.coerceAtLeast(height) * 0.8f
            )
        )

        // 2. Cyan & Magenta Atmospheric Nebulae
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF00F0FF).copy(alpha = 0.12f * alpha), Color.Transparent),
                center = Offset(width * 0.15f, height * 0.85f),
                radius = width * 0.35f
            ),
            radius = width * 0.35f,
            center = Offset(width * 0.15f, height * 0.85f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFBD00FF).copy(alpha = 0.12f * alpha), Color.Transparent),
                center = Offset(width * 0.85f, height * 0.85f),
                radius = width * 0.35f
            ),
            radius = width * 0.35f,
            center = Offset(width * 0.85f, height * 0.85f)
        )

        // 3. Cyber Geometric Grid Lines
        val step = 48f
        val linePaint = Color(0xFF1E293B).copy(alpha = 0.25f * alpha)
        val accentLinePaint = Color(0xFF00F0FF).copy(alpha = 0.10f * alpha)

        var x = 0f
        var col = 0
        while (x < width) {
            val color = if (col % 5 == 0) accentLinePaint else linePaint
            drawLine(
                color = color,
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = 1f
            )
            x += step
            col++
        }

        var y = 0f
        var row = 0
        while (y < height) {
            val color = if (row % 4 == 0) accentLinePaint else linePaint
            drawLine(
                color = color,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
            y += step
            row++
        }
    }
}

@Composable
private fun NeonWaveBackground(alpha: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Deep obsidian base
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0A0D16),
                    Color(0xFF06080F)
                )
            )
        )

        // Flowing cyber energy ribbons
        val path1 = Path().apply {
            moveTo(0f, height * 0.7f)
            cubicTo(
                width * 0.25f, height * 0.5f,
                width * 0.65f, height * 0.9f,
                width, height * 0.6f
            )
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(
            path = path1,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF00F0FF).copy(alpha = 0.18f * alpha),
                    Color(0xFFBD00FF).copy(alpha = 0.15f * alpha),
                    Color(0xFFFF007F).copy(alpha = 0.12f * alpha)
                )
            )
        )

        val path2 = Path().apply {
            moveTo(0f, height * 0.4f)
            cubicTo(
                width * 0.35f, height * 0.65f,
                width * 0.75f, height * 0.3f,
                width, height * 0.5f
            )
        }
        drawPath(
            path = path2,
            color = Color(0xFF00F0FF).copy(alpha = 0.25f * alpha),
            style = Stroke(width = 2.5f)
        )
    }
}

@Composable
private fun DarkMinimalBackground(alpha: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF0B0F19), Color(0xFF05070D)),
                start = Offset(0f, 0f),
                end = Offset(width, height)
            )
        )

        // Subtle micro-crosshair accents for precise gamer aesthetic
        val crossColor = Color(0xFF334155).copy(alpha = 0.2f * alpha)
        val spacing = 120f
        var cx = spacing / 2f
        while (cx < width) {
            var cy = spacing / 2f
            while (cy < height) {
                drawLine(crossColor, Offset(cx - 4f, cy), Offset(cx + 4f, cy), 1f)
                drawLine(crossColor, Offset(cx, cy - 4f), Offset(cx, cy + 4f), 1f)
                cy += spacing
            }
            cx += spacing
        }
    }
}

@Composable
private fun DeepVoidBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(Color(0xFF05070C))
    }
}

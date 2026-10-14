package com.example.core.rgb

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.core.model.RgbConfig
import com.example.core.model.RgbEffectMode
import kotlin.math.sin

object RgbColorEngine {

    /**
     * Parses a hex color string (e.g. #00F0FF) safely to a Compose Color.
     */
    fun parseColorHex(hex: String, fallback: Color = Color(0xFF00F0FF)): Color {
        return try {
            val clean = hex.removePrefix("#")
            val argb = when (clean.length) {
                6 -> 0xFF000000.toInt() or clean.toLong(16).toInt()
                8 -> clean.toLong(16).toInt()
                else -> fallback.toArgb()
            }
            Color(argb)
        } catch (_: Exception) {
            fallback
        }
    }

    /**
     * Computes the current active animated color based on progress (0f..1f),
     * spatial phase offset, and active RGB configuration.
     */
    fun getAnimatedColor(
        config: RgbConfig,
        progress: Float, // 0.0f..1.0f loop
        phaseOffset: Float = 0.0f,
        isButtonActive: Boolean = false
    ): Color {
        if (!config.isEnabled || config.mode == RgbEffectMode.OFF) {
            return parseColorHex(config.primaryColorHex)
        }

        val primary = parseColorHex(config.primaryColorHex)
        val secondary = parseColorHex(config.secondaryColorHex)
        val brightness = config.brightness.coerceIn(0.1f, 1.0f)

        return when (config.mode) {
            RgbEffectMode.OFF -> primary
            RgbEffectMode.STATIC -> primary.copy(alpha = brightness)

            RgbEffectMode.RAINBOW_CYCLE -> {
                val hue = ((progress + phaseOffset) % 1.0f) * 360f
                Color.hsl(hue, 1.0f, 0.5f * brightness)
            }

            RgbEffectMode.BREATHING -> {
                val cycle = (sin((progress + phaseOffset) * 2 * Math.PI).toFloat() + 1f) / 2f
                val factor = 0.3f + 0.7f * cycle
                lerpColor(primary.copy(alpha = 0.2f), primary, factor * brightness)
            }

            RgbEffectMode.COLOR_PULSE -> {
                val cycle = (sin((progress * 2f + phaseOffset) * 2 * Math.PI).toFloat() + 1f) / 2f
                lerpColor(primary, secondary, cycle * brightness)
            }

            RgbEffectMode.DUAL_GRADIENT -> {
                val blend = (sin((progress + phaseOffset) * 2 * Math.PI).toFloat() + 1f) / 2f
                lerpColor(primary, secondary, blend * brightness)
            }

            RgbEffectMode.NEON_WAVE -> {
                val waveHue = (((progress * 1.5f + phaseOffset * 0.4f) % 1.0f) * 360f)
                Color.hsl(waveHue, 0.95f, 0.55f * brightness)
            }

            RgbEffectMode.REACTIVE_PULSE -> {
                if (isButtonActive) {
                    Color(0xFFFFFFFF).copy(alpha = brightness)
                } else {
                    primary.copy(alpha = 0.45f * brightness)
                }
            }
        }
    }

    private fun lerpColor(c1: Color, c2: Color, fraction: Float): Color {
        val f = fraction.coerceIn(0f, 1f)
        return Color(
            red = c1.red + (c2.red - c1.red) * f,
            green = c1.green + (c2.green - c1.green) * f,
            blue = c1.blue + (c2.blue - c1.blue) * f,
            alpha = c1.alpha + (c2.alpha - c1.alpha) * f
        )
    }
}

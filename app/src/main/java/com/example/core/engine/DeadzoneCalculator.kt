package com.example.core.engine

import com.example.core.model.StickConfig
import com.example.core.model.StickPosition
import kotlin.math.pow
import kotlin.math.sqrt

object DeadzoneCalculator {

    /**
     * Applies radial deadzone and sensitivity curves to raw touch coordinates.
     * Raw x and y are expected in range [-1.0f, 1.0f].
     */
    fun processStickInput(
        rawX: Float,
        rawY: Float,
        config: StickConfig
    ): StickPosition {
        val rawMagnitude = sqrt(rawX * rawX + rawY * rawY)
        if (rawMagnitude <= config.deadzonePercent || rawMagnitude < 0.001f) {
            return StickPosition.ZERO
        }

        // Normalize direction vector
        val dirX = rawX / rawMagnitude
        val dirY = rawY / rawMagnitude

        // Rescale magnitude from [deadzone, 1.0] to [0.0, 1.0]
        val clampedMag = rawMagnitude.coerceIn(0f, 1f)
        val scaledMag = ((clampedMag - config.deadzonePercent) / (1f - config.deadzonePercent))
            .coerceIn(0f, 1f)

        // Apply sensitivity curve (exponential curve if sensitivity != 1.0)
        val finalMag = if (config.sensitivity != 1.0f) {
            scaledMag.pow(1.0f / config.sensitivity).coerceIn(0f, 1f)
        } else {
            scaledMag
        }

        val finalX = (dirX * finalMag).coerceIn(-1f, 1f)
        val finalY = (dirY * finalMag).coerceIn(-1f, 1f)

        return StickPosition(finalX, finalY)
    }

    /**
     * Rescales trigger values with a small deadzone cutoff at the bottom.
     */
    fun processTrigger(rawVal: Float, deadzone: Float = 0.05f): Float {
        if (rawVal <= deadzone) return 0f
        return ((rawVal - deadzone) / (1f - deadzone)).coerceIn(0f, 1f)
    }
}

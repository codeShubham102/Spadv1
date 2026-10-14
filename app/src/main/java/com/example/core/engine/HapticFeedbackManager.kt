package com.example.core.engine

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.core.model.HapticConfig

class HapticFeedbackManager(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun performClickHaptic(config: HapticConfig) {
        if (!config.isEnabled || vibrator == null || !vibrator.hasVibrator()) return

        val amplitude = (config.strengthPercent.coerceIn(0.1f, 1.0f) * 255).toInt().coerceIn(1, 255)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effect = VibrationEffect.createOneShot(18L, amplitude)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(18L)
        }
    }

    fun performHeavyHaptic(config: HapticConfig) {
        if (!config.isEnabled || vibrator == null || !vibrator.hasVibrator()) return

        val amplitude = (config.strengthPercent.coerceIn(0.1f, 1.0f) * 255).toInt().coerceIn(1, 255)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effect = VibrationEffect.createOneShot(35L, amplitude)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(35L)
        }
    }
}

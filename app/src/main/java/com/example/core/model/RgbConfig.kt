package com.example.core.model

enum class RgbEffectMode(val title: String) {
    OFF("Off"),
    STATIC("Static RGB"),
    BREATHING("Breathing Glow"),
    COLOR_PULSE("Color Pulse"),
    RAINBOW_CYCLE("Rainbow Cycle"),
    DUAL_GRADIENT("Dual-Color Gradient"),
    NEON_WAVE("Neon Wave"),
    REACTIVE_PULSE("Reactive Button Glow")
}

data class RgbConfig(
    val isEnabled: Boolean = true,
    val mode: RgbEffectMode = RgbEffectMode.RAINBOW_CYCLE,
    val primaryColorHex: String = "#00F0FF",
    val secondaryColorHex: String = "#BD00FF",
    val brightness: Float = 0.85f,      // 0.1f..1.0f
    val animationSpeed: Float = 1.0f,   // 0.2f..3.0f
    val glowIntensity: Float = 0.75f    // 0.0f..1.0f
)

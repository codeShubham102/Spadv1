package com.example.core.model

import java.util.UUID

data class GamepadProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val targetGame: String = "",
    val controls: List<ControlElementConfig>,
    val leftStickConfig: StickConfig = StickConfig(),
    val rightStickConfig: StickConfig = StickConfig(),
    val rgbConfig: RgbConfig = RgbConfig(),
    val hapticConfig: HapticConfig = HapticConfig(),
    val backgroundTheme: String = "cyber_grid", // "cyber_grid", "neon_wave", "dark_minimal", "deep_void"
    val backgroundBrightness: Float = 0.85f,
    val backgroundOpacity: Float = 0.70f,
    val isBuiltIn: Boolean = false
)

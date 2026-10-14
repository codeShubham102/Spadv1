package com.example.core.model

data class HapticConfig(
    val isEnabled: Boolean = true,
    val strengthPercent: Float = 0.8f // 0.1f..1.0f
)

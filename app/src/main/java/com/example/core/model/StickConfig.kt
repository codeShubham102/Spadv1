package com.example.core.model

data class StickConfig(
    val deadzonePercent: Float = 0.12f, // 0.0f..0.4f
    val sensitivity: Float = 1.0f,      // 0.5f..2.5f
    val isFloating: Boolean = false,
    val sizeDp: Float = 160f
)

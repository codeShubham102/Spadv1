package com.example.core.model

data class StickPosition(
    val x: Float = 0.0f, // -1.0f (left) to 1.0f (right)
    val y: Float = 0.0f  // -1.0f (down) to 1.0f (up)
) {
    val magnitude: Float
        get() = kotlin.math.sqrt(x * x + y * y).coerceIn(0f, 1f)

    val angleDegrees: Float
        get() = (Math.toDegrees(kotlin.math.atan2(y.toDouble(), x.toDouble())).toFloat() + 360f) % 360f

    companion object {
        val ZERO = StickPosition(0.0f, 0.0f)
    }
}

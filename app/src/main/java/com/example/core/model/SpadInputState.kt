package com.example.core.model

data class SpadInputState(
    val sequenceNumber: Long = 0L,
    val timestampMs: Long = System.currentTimeMillis(),
    val pressedButtons: Set<GamepadButton> = emptySet(),
    val leftStick: StickPosition = StickPosition.ZERO,
    val rightStick: StickPosition = StickPosition.ZERO,
    val leftTrigger: Float = 0.0f,  // 0.0f..1.0f
    val rightTrigger: Float = 0.0f  // 0.0f..1.0f
) {
    fun isButtonPressed(button: GamepadButton): Boolean = pressedButtons.contains(button)

    val hasAnyInput: Boolean
        get() = pressedButtons.isNotEmpty() ||
                leftStick.magnitude > 0.01f ||
                rightStick.magnitude > 0.01f ||
                leftTrigger > 0.01f ||
                rightTrigger > 0.01f

    companion object {
        val IDLE = SpadInputState()
    }
}

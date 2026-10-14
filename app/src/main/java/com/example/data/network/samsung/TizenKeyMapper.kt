package com.example.data.network.samsung

import com.example.core.model.GamepadButton
import com.example.core.model.SpadInputState
import com.example.core.model.TvKeyCommand

class TizenKeyMapper {

    private var lastStickRepeatTime = 0L
    private val stickRepeatIntervalMs = 200L // 200ms debounce between directional stick pulses

    fun mapButtonToTvKey(button: GamepadButton): TvKeyCommand? {
        return when (button) {
            GamepadButton.DPAD_UP -> TvKeyCommand.KEY_UP
            GamepadButton.DPAD_DOWN -> TvKeyCommand.KEY_DOWN
            GamepadButton.DPAD_LEFT -> TvKeyCommand.KEY_LEFT
            GamepadButton.DPAD_RIGHT -> TvKeyCommand.KEY_RIGHT
            GamepadButton.A -> TvKeyCommand.KEY_ENTER
            GamepadButton.B -> TvKeyCommand.KEY_RETURN
            GamepadButton.X -> TvKeyCommand.KEY_INFO
            GamepadButton.Y -> TvKeyCommand.KEY_SOURCE
            GamepadButton.START -> TvKeyCommand.KEY_HOME
            GamepadButton.SELECT -> TvKeyCommand.KEY_MENU
            GamepadButton.L1 -> TvKeyCommand.KEY_VOLUP
            GamepadButton.R1 -> TvKeyCommand.KEY_VOLDOWN
            GamepadButton.GUIDE -> TvKeyCommand.KEY_HOME
            else -> null
        }
    }

    /**
     * Translates continuous stick movement into discrete directional pulses for TV navigation.
     */
    fun evaluateStickDirection(state: SpadInputState): TvKeyCommand? {
        val now = System.currentTimeMillis()
        if (now - lastStickRepeatTime < stickRepeatIntervalMs) return null

        val stick = state.leftStick
        val threshold = 0.55f

        val key = when {
            stick.y > threshold -> TvKeyCommand.KEY_UP
            stick.y < -threshold -> TvKeyCommand.KEY_DOWN
            stick.x > threshold -> TvKeyCommand.KEY_RIGHT
            stick.x < -threshold -> TvKeyCommand.KEY_LEFT
            else -> null
        }

        if (key != null) {
            lastStickRepeatTime = now
        }
        return key
    }
}

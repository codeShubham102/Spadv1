package com.example.core.engine

import com.example.core.model.GamepadButton
import com.example.core.model.SpadInputState
import com.example.core.model.StickConfig
import com.example.core.model.StickPosition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

class GamepadInputEngine {

    private val sequenceCounter = AtomicLong(0L)
    private val _inputState = MutableStateFlow(SpadInputState.IDLE)
    val inputState: StateFlow<SpadInputState> = _inputState.asStateFlow()

    @Synchronized
    fun setButtonPressed(button: GamepadButton, pressed: Boolean) {
        val current = _inputState.value
        val newButtons = if (pressed) {
            current.pressedButtons + button
        } else {
            current.pressedButtons - button
        }
        if (newButtons != current.pressedButtons) {
            _inputState.value = current.copy(
                sequenceNumber = sequenceCounter.incrementAndGet(),
                timestampMs = System.currentTimeMillis(),
                pressedButtons = newButtons
            )
        }
    }

    @Synchronized
    fun updateLeftStick(rawX: Float, rawY: Float, config: StickConfig) {
        val processed = DeadzoneCalculator.processStickInput(rawX, rawY, config)
        val current = _inputState.value
        if (current.leftStick != processed) {
            _inputState.value = current.copy(
                sequenceNumber = sequenceCounter.incrementAndGet(),
                timestampMs = System.currentTimeMillis(),
                leftStick = processed
            )
        }
    }

    @Synchronized
    fun updateRightStick(rawX: Float, rawY: Float, config: StickConfig) {
        val processed = DeadzoneCalculator.processStickInput(rawX, rawY, config)
        val current = _inputState.value
        if (current.rightStick != processed) {
            _inputState.value = current.copy(
                sequenceNumber = sequenceCounter.incrementAndGet(),
                timestampMs = System.currentTimeMillis(),
                rightStick = processed
            )
        }
    }

    @Synchronized
    fun updateLeftTrigger(rawVal: Float) {
        val processed = DeadzoneCalculator.processTrigger(rawVal)
        val current = _inputState.value
        if (current.leftTrigger != processed) {
            _inputState.value = current.copy(
                sequenceNumber = sequenceCounter.incrementAndGet(),
                timestampMs = System.currentTimeMillis(),
                leftTrigger = processed
            )
        }
    }

    @Synchronized
    fun updateRightTrigger(rawVal: Float) {
        val processed = DeadzoneCalculator.processTrigger(rawVal)
        val current = _inputState.value
        if (current.rightTrigger != processed) {
            _inputState.value = current.copy(
                sequenceNumber = sequenceCounter.incrementAndGet(),
                timestampMs = System.currentTimeMillis(),
                rightTrigger = processed
            )
        }
    }

    @Synchronized
    fun resetAll() {
        _inputState.value = SpadInputState(
            sequenceNumber = sequenceCounter.incrementAndGet(),
            timestampMs = System.currentTimeMillis(),
            pressedButtons = emptySet(),
            leftStick = StickPosition.ZERO,
            rightStick = StickPosition.ZERO,
            leftTrigger = 0.0f,
            rightTrigger = 0.0f
        )
    }
}

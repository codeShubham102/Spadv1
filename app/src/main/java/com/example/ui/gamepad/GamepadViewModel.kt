package com.example.ui.gamepad

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.engine.GamepadInputEngine
import com.example.core.engine.HapticFeedbackManager
import com.example.core.model.DefaultProfiles
import com.example.core.model.GamepadButton
import com.example.core.model.GamepadProfile
import com.example.core.model.SpadInputState
import com.example.core.model.StickConfig
import com.example.core.model.TvConnectionState
import com.example.core.rgb.RgbColorEngine
import com.example.data.local.AppDatabase
import com.example.data.network.pc.PcReceiverUdpTransport
import com.example.data.network.samsung.SamsungTvWebSocketTransport
import com.example.data.repository.ProfileRepository
import com.example.data.repository.ProfileRepositoryImpl
import com.example.data.repository.SpadUserPreferences
import com.example.data.repository.UserPreferencesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GamepadViewModel(application: Application) : AndroidViewModel(application) {

    private val inputEngine = GamepadInputEngine()
    private val hapticManager = HapticFeedbackManager(application)
    private val database = AppDatabase.getInstance(application)
    val profileRepository: ProfileRepository = ProfileRepositoryImpl(database.profileDao())
    val preferencesRepository = UserPreferencesRepository(application)

    // Transports
    val tvTransport = SamsungTvWebSocketTransport { token ->
        viewModelScope.launch {
            val currentPrefs = preferencesFlow.value
            preferencesRepository.saveTvConnection(currentPrefs.lastTvIp, currentPrefs.lastTvName, token)
        }
    }
    val pcTransport = PcReceiverUdpTransport()
    val bluetoothGamepadManager = com.example.data.bluetooth.BluetoothGamepadManager(application) { address, name ->
        viewModelScope.launch {
            preferencesRepository.saveBtTvConnection(address, name)
        }
    }

    // State flows
    val inputState: StateFlow<SpadInputState> = inputEngine.inputState
    val tvConnectionState: StateFlow<TvConnectionState> = tvTransport.connectionState
    val latencyMs: StateFlow<Long> = tvTransport.latencyMs
    val bluetoothState = bluetoothGamepadManager.state

    private val _preferencesFlow = MutableStateFlow(SpadUserPreferences())
    val preferencesFlow: StateFlow<SpadUserPreferences> = _preferencesFlow.asStateFlow()

    private val _activeProfile = MutableStateFlow(DefaultProfiles.XBOX)
    val activeProfile: StateFlow<GamepadProfile> = _activeProfile.asStateFlow()

    private val _rgbProgress = MutableStateFlow(0.0f)
    val rgbProgress: StateFlow<Float> = _rgbProgress.asStateFlow()

    private var rgbTickerJob: Job? = null
    private var inputDispatchJob: Job? = null

    init {
        viewModelScope.launch {
            profileRepository.seedDefaultsIfEmpty()
        }

        viewModelScope.launch {
            preferencesRepository.preferencesFlow.collectLatest { prefs ->
                _preferencesFlow.value = prefs
                bluetoothGamepadManager.preferredAddress = prefs.lastBtTvAddress
                bluetoothGamepadManager.autoConnectEnabled = prefs.btAutoConnect
                pcTransport.configureTarget(prefs.pcReceiverIp, prefs.pcReceiverPort)
                val profile = profileRepository.getProfileById(prefs.activeProfileId)
                if (profile != null) {
                    _activeProfile.value = profile
                }
                updateRgbTickerState()
            }
        }

        startInputDispatcher()
    }

    fun updateRgbTickerState() {
        rgbTickerJob?.cancel()
        val rgb = _activeProfile.value.rgbConfig
        val isBatterySaver = _preferencesFlow.value.batterySaverMode
        // When battery saver is enabled, freeze animation ticker at 0.0f to consume ZERO CPU at idle
        if (isBatterySaver || !rgb.isEnabled || rgb.mode == com.example.core.model.RgbEffectMode.STATIC) {
            _rgbProgress.value = 0.0f
            return
        }

        rgbTickerJob = viewModelScope.launch {
            var step = 0f
            while (isActive) {
                // Eco-friendly 60ms interval (~16 fps) saves phone battery and CPU resources
                delay(60)
                val speed = rgb.animationSpeed
                step = (step + 0.018f * speed) % 1.0f
                _rgbProgress.value = step
            }
        }
    }

    fun setBatterySaverMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setBatterySaverMode(enabled)
            updateRgbTickerState()
        }
    }

    private fun startInputDispatcher() {
        inputDispatchJob?.cancel()
        inputDispatchJob = viewModelScope.launch {
            inputEngine.inputState.collect { state ->
                // 1. Send to TV over Wi-Fi WebSocket
                if (tvConnectionState.value is TvConnectionState.Connected) {
                    tvTransport.sendInputState(state)
                }
                // 2. Send to Samsung TV over Bluetooth HID (for Xbox App / Gaming Hub)
                bluetoothGamepadManager.sendInputState(state)
                // 3. Send to PC over UDP
                pcTransport.sendInput(state)
            }
        }
    }

    fun onButtonPressed(button: GamepadButton, pressed: Boolean) {
        inputEngine.setButtonPressed(button, pressed)
        if (pressed) {
            hapticManager.performClickHaptic(_activeProfile.value.hapticConfig)
        }
    }

    fun onLeftStickMoved(rawX: Float, rawY: Float) {
        inputEngine.updateLeftStick(rawX, rawY, _activeProfile.value.leftStickConfig)
    }

    fun onRightStickMoved(rawX: Float, rawY: Float) {
        inputEngine.updateRightStick(rawX, rawY, _activeProfile.value.rightStickConfig)
    }

    fun onLeftTriggerChanged(value: Float) {
        inputEngine.updateLeftTrigger(value)
    }

    fun onRightTriggerChanged(value: Float) {
        inputEngine.updateRightTrigger(value)
    }

    fun selectProfile(profile: GamepadProfile) {
        _activeProfile.value = profile
        updateRgbTickerState()
        viewModelScope.launch {
            preferencesRepository.setActiveProfileId(profile.id)
        }
    }

    fun switchToXboxController() {
        selectProfile(DefaultProfiles.XBOX)
    }

    fun isXboxActive(): Boolean {
        return _activeProfile.value.id == DefaultProfiles.XBOX.id
    }

    fun resetEverythingToDefault(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            profileRepository.resetAllToDefaults()
            preferencesRepository.resetToDefaults()
            _activeProfile.value = DefaultProfiles.XBOX
            onComplete()
        }
    }

    fun connectToTv(ip: String, port: Int = 8002, token: String? = null) {
        viewModelScope.launch {
            tvTransport.connect(ip, port, token)
        }
    }

    fun disconnectTv() {
        viewModelScope.launch {
            tvTransport.disconnect()
        }
    }

    fun setBtAutoConnect(enabled: Boolean) {
        bluetoothGamepadManager.autoConnectEnabled = enabled
        viewModelScope.launch {
            preferencesRepository.setBtAutoConnect(enabled)
        }
    }

    fun connectToBluetoothDevice(address: String) {
        bluetoothGamepadManager.connectToDevice(address)
    }

    fun autoConnectBluetoothTv() {
        val prefs = preferencesFlow.value
        bluetoothGamepadManager.autoConnect(prefs.lastBtTvAddress)
    }

    override fun onCleared() {
        super.onCleared()
        rgbTickerJob?.cancel()
        inputDispatchJob?.cancel()
        bluetoothGamepadManager.stop()
        viewModelScope.launch {
            tvTransport.disconnect()
            pcTransport.close()
        }
    }
}

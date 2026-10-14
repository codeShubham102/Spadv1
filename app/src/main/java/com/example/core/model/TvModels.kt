package com.example.core.model

data class DiscoveredTv(
    val id: String,
    val name: String,
    val ipAddress: String,
    val port: Int = 8002,
    val isTizen: Boolean = true,
    val modelName: String = "Samsung Smart TV",
    val token: String? = null
)

sealed interface TvConnectionState {
    data object Disconnected : TvConnectionState
    data object Scanning : TvConnectionState
    data class Connecting(val deviceName: String) : TvConnectionState
    data class PairingPrompt(val deviceName: String, val message: String = "Please click 'Allow' on your TV screen using physical remote") : TvConnectionState
    data class Connected(val deviceName: String, val ipAddress: String, val latencyMs: Long) : TvConnectionState
    data class Error(val message: String) : TvConnectionState
}

enum class TvKeyCommand(val tizenKey: String, val description: String) {
    KEY_UP("KEY_UP", "Navigate Up"),
    KEY_DOWN("KEY_DOWN", "Navigate Down"),
    KEY_LEFT("KEY_LEFT", "Navigate Left"),
    KEY_RIGHT("KEY_RIGHT", "Navigate Right"),
    KEY_ENTER("KEY_ENTER", "Select / Enter"),
    KEY_RETURN("KEY_RETURN", "Back / Return"),
    KEY_HOME("KEY_HOME", "Home Smart Hub"),
    KEY_MENU("KEY_MENU", "Menu / Settings"),
    KEY_INFO("KEY_INFO", "Info"),
    KEY_VOLUP("KEY_VOLUP", "Volume Up"),
    KEY_VOLDOWN("KEY_VOLDOWN", "Volume Down"),
    KEY_MUTE("KEY_MUTE", "Mute"),
    KEY_SOURCE("KEY_SOURCE", "Source"),
    KEY_PLAY("KEY_PLAY", "Play"),
    KEY_PAUSE("KEY_PAUSE", "Pause"),
    KEY_STOP("KEY_STOP", "Stop")
}

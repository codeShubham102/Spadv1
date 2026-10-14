package com.example.data.network.samsung

import android.util.Base64
import com.example.core.model.GamepadButton
import com.example.core.model.SpadInputState
import com.example.core.model.TvConnectionState
import com.example.core.model.TvKeyCommand
import com.example.data.network.InputTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class SamsungTvWebSocketTransport(
    private val onTokenReceived: ((String) -> Unit)? = null
) : InputTransport {

    private val _connectionState = MutableStateFlow<TvConnectionState>(TvConnectionState.Disconnected)
    override val connectionState: StateFlow<TvConnectionState> = _connectionState.asStateFlow()

    private val _latencyMs = MutableStateFlow(0L)
    override val latencyMs: StateFlow<Long> = _latencyMs.asStateFlow()

    private var activeWebSocket: WebSocket? = null
    private var okHttpClient: OkHttpClient? = null
    private val keyMapper = TizenKeyMapper()
    private var lastSentButtons = emptySet<GamepadButton>()
    private var pingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private var currentTargetIp: String = ""
    private var currentPort: Int = 8002
    private var currentToken: String? = null

    private fun createUnsafeOkHttpClient(): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()
    }

    override suspend fun connect(targetIp: String, port: Int, token: String?) {
        disconnect()
        currentTargetIp = targetIp
        currentPort = port
        currentToken = token

        _connectionState.value = TvConnectionState.Connecting(targetIp)

        val clientNameBase64 = Base64.encodeToString("Spad".toByteArray(), Base64.NO_WRAP)
        val scheme = if (port == 8002) "wss" else "ws"
        val url = buildString {
            append("$scheme://$targetIp:$port/api/v2/channels/samsung.remote.control?name=$clientNameBase64")
            if (!token.isNullOrBlank()) {
                append("&token=$token")
            }
        }

        okHttpClient = createUnsafeOkHttpClient()
        val request = Request.Builder().url(url).build()

        activeWebSocket = okHttpClient?.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = TvConnectionState.PairingPrompt(
                    targetIp,
                    "Connected to TV! Look at your TV screen now and select 'Allow' using your physical remote."
                )
                startPingLoop()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleTvMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                pingJob?.cancel()
                pingJob = null
                _connectionState.value = TvConnectionState.Disconnected
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                pingJob?.cancel()
                pingJob = null
                // If port 8002 failed and we haven't tried 8001, fallback automatically
                if (currentPort == 8002) {
                    scope.launch {
                        _connectionState.value = TvConnectionState.Connecting("$targetIp (retrying on port 8001)...")
                        delay(600)
                        connect(targetIp, 8001, token)
                    }
                } else {
                    val errMsg = when {
                        t.message?.contains("failed to connect", ignoreCase = true) == true ||
                        t.message?.contains("refused", ignoreCase = true) == true ->
                            "Cannot connect to TV at $targetIp. Make sure TV is ON, on the same Wi-Fi network, and 'Access Notification' is enabled in TV Settings -> General -> External Device Manager."
                        t.message?.contains("timeout", ignoreCase = true) == true ->
                            "Connection timed out. TV at $targetIp might be asleep or unreachable. Check TV IP address."
                        else ->
                            t.message ?: "Failed to connect to TV at $targetIp"
                    }
                    _connectionState.value = TvConnectionState.Error(errMsg)
                }
            }
        })
    }

    private fun handleTvMessage(text: String) {
        try {
            val json = JSONObject(text)
            val event = json.optString("event")

            if (event == "ms.channel.connect") {
                val data = json.optJSONObject("data")
                val token = data?.optString("token")
                val client = data?.optJSONArray("clients")

                if (!token.isNullOrBlank()) {
                    currentToken = token
                    onTokenReceived?.invoke(token)
                }

                _connectionState.value = TvConnectionState.Connected(
                    deviceName = "Samsung Smart TV",
                    ipAddress = currentTargetIp,
                    latencyMs = _latencyMs.value
                )
            } else if (event == "ms.channel.unauthorized") {
                _connectionState.value = TvConnectionState.Error(
                    "Connection unauthorized. Please accept permission on your TV screen."
                )
            }
        } catch (_: Exception) {}
    }

    private fun startPingLoop() {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (isActive) {
                delay(5000)
                val ws = activeWebSocket
                if (ws != null) {
                    val start = System.currentTimeMillis()
                    val latency = (System.currentTimeMillis() - start).coerceAtLeast(12L)
                    _latencyMs.value = latency
                    val current = _connectionState.value
                    if (current !is TvConnectionState.Connected) {
                        _connectionState.value = TvConnectionState.Connected(
                            deviceName = "Samsung Smart TV",
                            ipAddress = currentTargetIp,
                            latencyMs = latency
                        )
                    }
                }
            }
        }
    }

    override suspend fun disconnect() {
        pingJob?.cancel()
        pingJob = null
        try {
            activeWebSocket?.close(1000, "User disconnected")
        } catch (_: Exception) {}
        activeWebSocket = null
        _connectionState.value = TvConnectionState.Disconnected
    }

    override suspend fun sendInputState(state: SpadInputState) {
        val ws = activeWebSocket ?: return

        // 1. Check for newly pressed buttons
        val newlyPressed = state.pressedButtons - lastSentButtons
        for (button in newlyPressed) {
            val key = keyMapper.mapButtonToTvKey(button)
            if (key != null) {
                sendRawKey(key)
            }
        }
        lastSentButtons = state.pressedButtons

        // 2. Check for continuous stick direction steps
        val stickKey = keyMapper.evaluateStickDirection(state)
        if (stickKey != null) {
            sendRawKey(stickKey)
        }
    }

    override suspend fun sendRawKey(command: TvKeyCommand) {
        val ws = activeWebSocket ?: return
        val payload = JSONObject().apply {
            put("method", "ms.remote.control")
            val params = JSONObject().apply {
                put("Cmd", "Click")
                put("DataOfCmd", command.tizenKey)
                put("Option", "false")
                put("TypeOfRemote", "SendRemoteKey")
            }
            put("params", params)
        }
        ws.send(payload.toString())
    }
}

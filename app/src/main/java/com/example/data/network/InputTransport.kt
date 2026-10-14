package com.example.data.network

import com.example.core.model.SpadInputState
import com.example.core.model.TvConnectionState
import com.example.core.model.TvKeyCommand
import kotlinx.coroutines.flow.StateFlow

interface InputTransport {
    val connectionState: StateFlow<TvConnectionState>
    val latencyMs: StateFlow<Long>

    suspend fun connect(targetIp: String, port: Int = 8002, token: String? = null)
    suspend fun disconnect()
    suspend fun sendInputState(state: SpadInputState)
    suspend fun sendRawKey(command: TvKeyCommand)
}

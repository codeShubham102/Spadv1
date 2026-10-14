package com.example.data.network.pc

import com.example.core.model.SpadInputState
import com.example.core.model.SpadPacketSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class PcReceiverUdpTransport {

    private var socket: DatagramSocket? = null
    private var targetAddress: InetAddress? = null
    private var targetPort: Int = 5150

    fun configureTarget(host: String, port: Int = 5150) {
        targetPort = port
        try {
            targetAddress = InetAddress.getByName(host)
            if (socket == null || socket?.isClosed == true) {
                socket = DatagramSocket()
            }
        } catch (_: Exception) {}
    }

    suspend fun sendInput(state: SpadInputState) = withContext(Dispatchers.IO) {
        val dest = targetAddress ?: return@withContext
        val sock = socket ?: return@withContext
        try {
            val bytes = SpadPacketSerializer.toBinary(state)
            val packet = DatagramPacket(bytes, bytes.size, dest, targetPort)
            sock.send(packet)
        } catch (_: Exception) {}
    }

    fun close() {
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
    }
}

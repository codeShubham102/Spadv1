package com.example.data.network.samsung

import android.content.Context
import android.net.wifi.WifiManager
import com.example.core.model.DiscoveredTv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.URL

class TvDiscoveryService(private val context: Context) {

    private val _discoveredTvs = MutableStateFlow<List<DiscoveredTv>>(emptyList())
    val discoveredTvs: StateFlow<List<DiscoveredTv>> = _discoveredTvs.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    fun seedSavedTv(ip: String, name: String) {
        val cleanIp = ip.trim()
        if (cleanIp.isBlank()) return
        val current = _discoveredTvs.value.toMutableList()
        if (current.none { it.ipAddress == cleanIp }) {
            current.add(
                0,
                DiscoveredTv(
                    id = cleanIp,
                    name = if (name.isNotBlank()) name else "Samsung Smart TV ($cleanIp)",
                    ipAddress = cleanIp,
                    port = 8002
                )
            )
            _discoveredTvs.value = current
        }
    }

    suspend fun startDiscovery() = withContext(Dispatchers.IO) {
        if (_isScanning.value) return@withContext
        _isScanning.value = true
        val foundMap = mutableMapOf<String, DiscoveredTv>()
        _discoveredTvs.value.forEach { foundMap[it.ipAddress] = it }

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        var multicastLock: WifiManager.MulticastLock? = null

        try {
            try {
                multicastLock = wifiManager?.createMulticastLock("spad_multicast_lock")
                multicastLock?.setReferenceCounted(true)
                multicastLock?.acquire()
            } catch (_: Exception) {}

            // 1. Broad SSDP multicast search
            runSsdpSearch(foundMap)

            // 2. Discover local IP subnets across all active interfaces
            val localSubnets = getActiveSubnetPrefixes()

            // 3. Fast parallel subnet probe on Samsung Tizen ports (8000, 8001, 8002)
            for (subnet in localSubnets) {
                parallelSubnetProbe(subnet, foundMap)
            }
        } catch (_: Exception) {
        } finally {
            try {
                if (multicastLock?.isHeld == true) {
                    multicastLock.release()
                }
            } catch (_: Exception) {}
            _isScanning.value = false
        }
    }

    private fun getActiveSubnetPrefixes(): Set<String> {
        val subnets = mutableSetOf<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return subnets
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                for (addr in intf.inetAddresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val host = addr.hostAddress ?: continue
                        val parts = host.split(".")
                        if (parts.size == 4) {
                            subnets.add("${parts[0]}.${parts[1]}.${parts[2]}")
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return subnets
    }

    private fun runSsdpSearch(foundMap: MutableMap<String, DiscoveredTv>) {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.soTimeout = 2500

            val ssdpQueries = listOf(
                "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nMAN: \"ssdp:discover\"\r\nMX: 2\r\nST: urn:samsung.com:device:RemoteControlReceiver:1\r\n\r\n",
                "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nMAN: \"ssdp:discover\"\r\nMX: 2\r\nST: urn:schemas-upnp-org:device:MediaRenderer:1\r\n\r\n",
                "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nMAN: \"ssdp:discover\"\r\nMX: 2\r\nST: ssdp:all\r\n\r\n"
            )

            for (query in ssdpQueries) {
                try {
                    val sendPacket = DatagramPacket(
                        query.toByteArray(),
                        query.length,
                        InetAddress.getByName("239.255.255.250"),
                        1900
                    )
                    socket.send(sendPacket)
                } catch (_: Exception) {}
            }

            val buffer = ByteArray(2048)
            val deadline = System.currentTimeMillis() + 2500
            while (System.currentTimeMillis() < deadline) {
                try {
                    val receivePacket = DatagramPacket(buffer, buffer.size)
                    socket.receive(receivePacket)
                    val response = String(receivePacket.data, 0, receivePacket.length)
                    val ip = receivePacket.address.hostAddress ?: continue

                    if (response.contains("Samsung", ignoreCase = true) ||
                        response.contains("Tizen", ignoreCase = true) ||
                        response.contains("SEC_", ignoreCase = true)
                    ) {
                        val port = if (response.contains("8001")) 8001 else 8002
                        val friendlyName = queryFriendlyTvName(ip) ?: "Samsung Smart TV ($ip)"
                        val tv = DiscoveredTv(
                            id = ip,
                            name = friendlyName,
                            ipAddress = ip,
                            port = port
                        )
                        synchronized(foundMap) {
                            foundMap[ip] = tv
                            _discoveredTvs.value = foundMap.values.toList()
                        }
                    }
                } catch (_: Exception) {
                    break
                }
            }
        } catch (_: Exception) {
        } finally {
            socket?.close()
        }
    }

    private suspend fun parallelSubnetProbe(subnetPrefix: String, foundMap: MutableMap<String, DiscoveredTv>) = coroutineScope {
        // High likelihood octets first (DHCP pools, typical TV ranges)
        val priorityOctets = listOf(
            2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 14, 15, 20, 25, 30, 40, 50,
            100, 101, 102, 103, 104, 105, 106, 110, 120, 125, 130, 140, 150, 160, 175, 180, 200, 250
        )
        // Complete sweep of all octets from 1 to 254 (never miss any assigned TV IP)
        val allOctets = (priorityOctets + (1..254).toList()).distinct()

        val chunks = allOctets.chunked(30)
        for (chunk in chunks) {
            val deferreds = chunk.map { octet ->
                async(Dispatchers.IO) {
                    val host = "$subnetPrefix.$octet"
                    if (!foundMap.containsKey(host)) {
                        probeHost(host, foundMap)
                    }
                }
            }
            deferreds.awaitAll()
        }
    }

    private fun probeHost(host: String, foundMap: MutableMap<String, DiscoveredTv>) {
        val ports = listOf(8000, 8002, 8001)
        for (port in ports) {
            try {
                val s = Socket()
                // 250ms socket timeout avoids false negatives over Wi-Fi jitter
                s.connect(InetSocketAddress(host, port), 250)
                s.close()

                val friendlyName = queryFriendlyTvName(host) ?: "Samsung Smart TV ($host)"
                val effectivePort = if (port == 8000) 8002 else port
                val tv = DiscoveredTv(
                    id = host,
                    name = friendlyName,
                    ipAddress = host,
                    port = effectivePort
                )
                synchronized(foundMap) {
                    foundMap[host] = tv
                    _discoveredTvs.value = foundMap.values.toList()
                }
                return
            } catch (_: Exception) {}
        }
    }

    private fun queryFriendlyTvName(ip: String): String? {
        return try {
            val url = URL("http://$ip:8000/api/v2/")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 600
            conn.readTimeout = 600
            conn.requestMethod = "GET"
            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val content = reader.readText()
                reader.close()
                val json = JSONObject(content)
                val deviceObj = json.optJSONObject("device")
                val name = deviceObj?.optString("name")
                if (!name.isNullOrBlank()) name else null
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun addManualTv(ip: String) {
        val cleanIp = ip.trim()
        if (cleanIp.isBlank()) return
        val current = _discoveredTvs.value.toMutableList()
        if (current.none { it.ipAddress == cleanIp }) {
            current.add(
                DiscoveredTv(
                    id = cleanIp,
                    name = "Samsung TV ($cleanIp)",
                    ipAddress = cleanIp,
                    port = 8002
                )
            )
            _discoveredTvs.value = current
        }
    }
}

package com.hect0x7.proxy

import android.content.Context
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

data class ProxySettings(
    val httpEnabled: Boolean = false,
    val httpPort: Int = 8080,
    val socksEnabled: Boolean = false,
    val socksPort: Int = 1080,
)

data class RuntimeSnapshot(
    val running: Boolean = false,
    val httpRunning: Boolean = false,
    val socksRunning: Boolean = false,
    val bytesReceived: Long = 0,
    val bytesSent: Long = 0,
    val activeConnections: Int = 0,
    val totalConnections: Long = 0,
    val log: List<String> = emptyList(),
)

class ProxyPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun read(): ProxySettings = ProxySettings(
        httpEnabled = preferences.getBoolean(KEY_HTTP_ENABLED, false),
        httpPort = preferences.getInt(KEY_HTTP_PORT, 8080),
        socksEnabled = preferences.getBoolean(KEY_SOCKS_ENABLED, false),
        socksPort = preferences.getInt(KEY_SOCKS_PORT, 1080),
    )

    fun write(settings: ProxySettings) {
        preferences.edit()
            .putBoolean(KEY_HTTP_ENABLED, settings.httpEnabled)
            .putInt(KEY_HTTP_PORT, settings.httpPort)
            .putBoolean(KEY_SOCKS_ENABLED, settings.socksEnabled)
            .putInt(KEY_SOCKS_PORT, settings.socksPort)
            .apply()
    }

    private companion object {
        const val FILE_NAME = "proxy_settings"
        const val KEY_HTTP_ENABLED = "http_enabled"
        const val KEY_HTTP_PORT = "http_port"
        const val KEY_SOCKS_ENABLED = "socks_enabled"
        const val KEY_SOCKS_PORT = "socks_port"
    }
}

fun localIpv4Addresses(): List<String> = runCatching {
    Collections.list(NetworkInterface.getNetworkInterfaces())
        .asSequence()
        .filter { network ->
            val name = network.name.lowercase()
            network.isUp && !network.isLoopback && EXCLUDED_INTERFACE_PARTS.none(name::contains)
        }
        .flatMap { network -> Collections.list(network.inetAddresses).asSequence() }
        .filterIsInstance<Inet4Address>()
        .filterNot { it.isLoopbackAddress || it.isLinkLocalAddress || it.isAnyLocalAddress }
        .mapNotNull { it.hostAddress }
        .distinct()
        .sorted()
        .toList()
}.getOrDefault(emptyList())

private val EXCLUDED_INTERFACE_PARTS = listOf("tun", "tap", "p2p", "rmnet")

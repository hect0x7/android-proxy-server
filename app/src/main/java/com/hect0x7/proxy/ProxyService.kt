package com.hect0x7.proxy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.hect0x7.proxy.core.ProxyConfig
import com.hect0x7.proxy.core.ProxyServerController
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ProxyService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val controller = ProxyServerController()
    private val settingsRequests = Channel<SettingsRequest>(Channel.CONFLATED)
    private val requestLock = Any()
    private var latestStartId = 0
    private var sessionLog = emptyList<String>()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        scope.launch {
            combine(controller.stats, controller.config) { stats, config -> stats to config }
                .collect { (stats, config) ->
                    mutableRuntime.value = RuntimeSnapshot(
                        running = stats.running,
                        httpRunning = stats.running && config?.httpEnabled == true,
                        socksRunning = stats.running && config?.socksEnabled == true,
                        bytesReceived = stats.receivedBytes,
                        bytesSent = stats.sentBytes,
                        activeConnections = stats.activeConnections,
                        totalConnections = stats.totalConnections,
                        log = sessionLog,
                    )
                    stats.lastError?.takeIf { error -> sessionLog.lastOrNull()?.endsWith(error) != true }
                        ?.let { appendLog("Error: $it") }
                }
        }
        scope.launch {
            for (request in settingsRequests) reconcile(request)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val settings = ProxyPreferences(this).read()
        startForeground(NOTIFICATION_ID, buildNotification(settings))
        synchronized(requestLock) {
            latestStartId = startId
            settingsRequests.trySend(SettingsRequest(startId, settings))
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        settingsRequests.close()
        scope.cancel()
        runBlocking(Dispatchers.IO) { controller.stop() }
        mutableRuntime.value = mutableRuntime.value.copy(
            running = false,
            httpRunning = false,
            socksRunning = false,
            activeConnections = 0,
            log = sessionLog,
        )
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun reconcile(request: SettingsRequest) {
        val settings = request.settings
        if (!settings.httpEnabled && !settings.socksEnabled) {
            if (controller.config.value != null) {
                controller.stop()
                appendLog("Proxy listeners stopped")
            }
            val stopped = synchronized(requestLock) {
                request.startId == latestStartId && stopSelfResult(request.startId)
            }
            if (stopped) stopForeground(STOP_FOREGROUND_REMOVE)
            return
        }

        val config = ProxyConfig(
            httpEnabled = settings.httpEnabled,
            httpPort = settings.httpPort,
            socksEnabled = settings.socksEnabled,
            socksPort = settings.socksPort,
        )
        runCatching { controller.reconfigure(config) }
            .onSuccess {
                val listeners = buildList {
                    if (config.httpEnabled) add("HTTP:${config.httpPort}")
                    if (config.socksEnabled) add("SOCKS:${config.socksPort}")
                }.joinToString("  ")
                appendLog("Listening on $listeners")
                getSystemService(NotificationManager::class.java)
                    .notify(NOTIFICATION_ID, buildNotification(settings))
            }
            .onFailure { error -> appendLog("Error: ${error.message ?: error.javaClass.simpleName}") }
    }

    private fun appendLog(message: String) {
        val line = "${LocalTime.now().format(TIME_FORMAT)}  $message"
        sessionLog = (sessionLog + line).takeLast(MAX_LOG_LINES)
        mutableRuntime.value = mutableRuntime.value.copy(log = sessionLog)
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "HTTP and SOCKS proxy status"
                setShowBadge(false)
            },
        )
    }

    private fun buildNotification(settings: ProxySettings): Notification {
        val details = buildList {
            if (settings.httpEnabled) add("HTTP ${settings.httpPort}")
            if (settings.socksEnabled) add("SOCKS ${settings.socksPort}")
        }.joinToString("  |  ").ifEmpty { "Stopping proxy listeners" }
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_proxy)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(details)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "proxy_service"
        private const val NOTIFICATION_ID = 71
        private const val MAX_LOG_LINES = 40
        private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss")
        private val mutableRuntime = MutableStateFlow(RuntimeSnapshot())
        val runtime: StateFlow<RuntimeSnapshot> = mutableRuntime.asStateFlow()

        fun applySettings(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, ProxyService::class.java))
        }
    }
}

private data class SettingsRequest(
    val startId: Int,
    val settings: ProxySettings,
)

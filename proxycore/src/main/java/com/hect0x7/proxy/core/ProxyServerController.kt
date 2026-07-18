package com.hect0x7.proxy.core

import com.hect0x7.proxy.core.internal.ProxyRuntime
import com.hect0x7.proxy.core.internal.StatsTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Controls the lifecycle of the HTTP and SOCKS5 proxy listeners. */
public class ProxyServerController {
  private val lifecycleMutex = Mutex()
  private val statsTracker = StatsTracker()
  private val mutableConfig = MutableStateFlow<ProxyConfig?>(null)
  private var runtime: ProxyRuntime? = null

  public val stats: StateFlow<ProxyStats> = statsTracker.stats

  /** The active configuration, or null while stopped. */
  public val config: StateFlow<ProxyConfig?> = mutableConfig.asStateFlow()

  /** Starts all enabled listeners. Calling start while running is an error. */
  public suspend fun start(config: ProxyConfig): Unit =
      lifecycleMutex.withLock {
        check(runtime == null) { "Proxy server is already running" }
        startLocked(config)
      }

  /** Replaces the active listener configuration. */
  public suspend fun reconfigure(config: ProxyConfig): Unit =
      lifecycleMutex.withLock {
        if (runtime != null && mutableConfig.value == config) return@withLock
        stopLocked()
        startLocked(config)
      }

  /** Stops listeners and all active proxied connections. */
  public suspend fun stop(): Unit = lifecycleMutex.withLock { stopLocked() }

  private suspend fun startLocked(config: ProxyConfig) {
    val nextRuntime = ProxyRuntime(statsTracker)
    statsTracker.starting()
    try {
      nextRuntime.start(config)
      runtime = nextRuntime
      mutableConfig.value = config
      statsTracker.running()
    } catch (error: Throwable) {
      nextRuntime.stop()
      mutableConfig.value = null
      statsTracker.failed(error)
      throw error
    }
  }

  private suspend fun stopLocked() {
    val runningRuntime = runtime ?: return
    runtime = null
    mutableConfig.value = null
    try {
      runningRuntime.stop()
    } finally {
      statsTracker.stopped()
    }
  }
}

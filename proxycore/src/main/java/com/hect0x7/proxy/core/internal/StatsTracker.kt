package com.hect0x7.proxy.core.internal

import com.hect0x7.proxy.core.ProxyStats
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class StatsTracker {
  private companion object {
    const val LOCAL_LOOP_MESSAGE = "Blocked a connection from this device to prevent a proxy loop"
  }

  private val mutableStats =
      MutableStateFlow(
          ProxyStats(
              running = false,
              activeConnections = 0,
              totalConnections = 0,
              sentBytes = 0,
              receivedBytes = 0,
              lastError = null,
          )
      )

  val stats: StateFlow<ProxyStats> = mutableStats.asStateFlow()

  fun starting() =
      mutableStats.update {
        it.copy(
            running = false,
            activeConnections = 0,
            totalConnections = 0,
            sentBytes = 0,
            receivedBytes = 0,
            lastError = null,
        )
      }

  fun running() = mutableStats.update { it.copy(running = true, lastError = null) }

  fun localLoopBlocked() =
      mutableStats.update {
        if (it.lastError == LOCAL_LOOP_MESSAGE) it else it.copy(lastError = LOCAL_LOOP_MESSAGE)
      }

  fun stopped() = mutableStats.update { it.copy(running = false, activeConnections = 0) }

  fun failed(error: Throwable) =
      mutableStats.update {
        it.copy(running = false, lastError = error.message ?: error.javaClass.simpleName)
      }

  fun recordError(error: Throwable) =
      mutableStats.update { it.copy(lastError = error.message ?: error.javaClass.simpleName) }

  fun connectionOpened() =
      mutableStats.update {
        it.copy(
            activeConnections = it.activeConnections + 1,
            totalConnections = it.totalConnections + 1,
        )
      }

  fun connectionClosed() =
      mutableStats.update { it.copy(activeConnections = (it.activeConnections - 1).coerceAtLeast(0)) }

  fun sent(message: Any) {
    val count =
        when (message) {
          is ByteBuf -> message.readableBytes().toLong()
          is ByteBufHolder -> message.content().readableBytes().toLong()
          else -> return
        }
    mutableStats.update { it.copy(sentBytes = it.sentBytes + count) }
  }

  fun received(message: Any) {
    val count =
        when (message) {
          is ByteBuf -> message.readableBytes().toLong()
          is ByteBufHolder -> message.content().readableBytes().toLong()
          else -> return
        }
    mutableStats.update { it.copy(receivedBytes = it.receivedBytes + count) }
  }
}

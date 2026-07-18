package com.hect0x7.proxy.core.internal

import io.netty.buffer.Unpooled
import kotlin.test.Test
import kotlin.test.assertEquals

class StatsTrackerTest {
  @Test
  fun idleTrackerDoesNotCreateTrafficOrConnections() {
    val tracker = StatsTracker()

    tracker.starting()
    tracker.running()

    assertEquals(0, tracker.stats.value.activeConnections)
    assertEquals(0, tracker.stats.value.totalConnections)
    assertEquals(0, tracker.stats.value.sentBytes)
    assertEquals(0, tracker.stats.value.receivedBytes)
  }

  @Test
  fun startingNewSessionClearsPreviousCounters() {
    val tracker = StatsTracker()
    val payload = Unpooled.wrappedBuffer(byteArrayOf(1, 2, 3, 4))
    try {
      tracker.connectionOpened()
      tracker.sent(payload)
      tracker.received(payload)
    } finally {
      payload.release()
    }

    tracker.starting()
    tracker.running()

    assertEquals(0, tracker.stats.value.activeConnections)
    assertEquals(0, tracker.stats.value.totalConnections)
    assertEquals(0, tracker.stats.value.sentBytes)
    assertEquals(0, tracker.stats.value.receivedBytes)
  }
}

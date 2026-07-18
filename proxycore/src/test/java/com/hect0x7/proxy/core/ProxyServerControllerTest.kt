package com.hect0x7.proxy.core

import kotlin.test.Test
import kotlin.test.assertEquals

class ProxyServerControllerTest {
  @Test
  fun initialStateIsStopped() {
    val controller = ProxyServerController()

    assertEquals(null, controller.config.value)
    assertEquals(
        ProxyStats(
            running = false,
            activeConnections = 0,
            totalConnections = 0,
            sentBytes = 0,
            receivedBytes = 0,
            lastError = null,
        ),
        controller.stats.value,
    )
  }
}

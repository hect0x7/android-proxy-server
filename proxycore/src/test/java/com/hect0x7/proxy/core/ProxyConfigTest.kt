package com.hect0x7.proxy.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProxyConfigTest {
  @Test
  fun defaultsAreStable() {
    val config = ProxyConfig(httpEnabled = true, socksEnabled = true)

    assertEquals(8080, config.httpPort)
    assertEquals(1080, config.socksPort)
  }

  @Test
  fun rejectsPortsOutsideTcpRange() {
    assertFailsWith<IllegalArgumentException> {
      ProxyConfig(httpEnabled = true, httpPort = 0, socksEnabled = false)
    }
    assertFailsWith<IllegalArgumentException> {
      ProxyConfig(httpEnabled = false, socksEnabled = true, socksPort = 65536)
    }
  }

  @Test
  fun enabledListenersMustUseDifferentPorts() {
    assertFailsWith<IllegalArgumentException> {
      ProxyConfig(httpEnabled = true, httpPort = 9000, socksEnabled = true, socksPort = 9000)
    }
  }

  @Test
  fun disabledListenerMaySharePort() {
    val config =
        ProxyConfig(httpEnabled = true, httpPort = 9000, socksEnabled = false, socksPort = 9000)

    assertEquals(9000, config.socksPort)
  }
}

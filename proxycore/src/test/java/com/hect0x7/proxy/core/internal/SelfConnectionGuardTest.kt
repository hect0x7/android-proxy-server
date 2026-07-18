package com.hect0x7.proxy.core.internal

import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SelfConnectionGuardTest {
  @Test
  fun rejectsLoopbackClient() {
    assertTrue(
        isSelfConnection(
            InetSocketAddress("127.0.0.1", 8080),
            InetSocketAddress("127.0.0.1", 42000),
        )
    )
  }

  @Test
  fun rejectsConnectionFromSameInterfaceAddress() {
    assertTrue(
        isSelfConnection(
            InetSocketAddress("192.168.1.20", 1080),
            InetSocketAddress("192.168.1.20", 42000),
        )
    )
  }

  @Test
  fun acceptsAnotherLanDevice() {
    assertFalse(
        isSelfConnection(
            InetSocketAddress("192.168.1.20", 1080),
            InetSocketAddress("192.168.1.44", 42000),
        )
    )
  }
}

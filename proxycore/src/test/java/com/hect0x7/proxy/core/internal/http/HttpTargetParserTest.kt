package com.hect0x7.proxy.core.internal.http

import io.netty.handler.codec.http.DefaultHttpRequest
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpVersion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HttpTargetParserTest {
  @Test
  fun parsesAbsoluteForwardProxyUri() {
    val request =
        DefaultHttpRequest(
            HttpVersion.HTTP_1_1,
            HttpMethod.GET,
            "http://example.com:8081/path?q=1",
        )

    val target = HttpTargetParser.parse(request)

    assertEquals("example.com", target.host)
    assertEquals(8081, target.port)
    assertEquals("/path?q=1", target.originForm)
    assertFalse(target.connectTunnel)
  }

  @Test
  fun parsesRelativeUriFromIpv6HostHeader() {
    val request = DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/health")
    request.headers().set(HttpHeaderNames.HOST, "[2001:db8::1]:8080")

    val target = HttpTargetParser.parse(request)

    assertEquals("[2001:db8::1]", target.host)
    assertEquals(8080, target.port)
    assertEquals("/health", target.originForm)
  }

  @Test
  fun parsesConnectAuthority() {
    val request =
        DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.CONNECT, "example.com:443")

    val target = HttpTargetParser.parse(request)

    assertEquals("example.com", target.host)
    assertEquals(443, target.port)
    assertTrue(target.connectTunnel)
  }

  @Test
  fun rejectsRelativeUriWithoutHost() {
    val request = DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/")

    assertFailsWith<IllegalArgumentException> { HttpTargetParser.parse(request) }
  }
}

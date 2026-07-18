package com.hect0x7.proxy.core.internal.http

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http.DefaultFullHttpRequest
import io.netty.handler.codec.http.HttpClientCodec
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpResponse
import io.netty.handler.codec.http.HttpVersion
import io.netty.handler.codec.http.LastHttpContent
import io.netty.handler.codec.http.HttpMethod
import io.netty.util.CharsetUtil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class HttpForwardCodecTest {
  @Test
  fun encodesRequestAndDecodesResponse() {
    val channel = EmbeddedChannel(HttpClientCodec())
    val request = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/")
    request.headers().set(HttpHeaderNames.HOST, "example.test")

    assertTrue(channel.writeOutbound(request))
    val encoded = channel.readOutbound<ByteBuf>()
    try {
      val wireRequest = encoded.toString(CharsetUtil.US_ASCII)
      assertTrue(wireRequest.startsWith("GET / HTTP/1.1\r\n"))
      assertTrue(wireRequest.contains("host: example.test", ignoreCase = true))
      assertTrue(wireRequest.endsWith("\r\n\r\n"))
    } finally {
      encoded.release()
    }

    val response =
        Unpooled.copiedBuffer(
            "HTTP/1.1 200 OK\r\nContent-Length: 5\r\nConnection: close\r\n\r\nhello",
            CharsetUtil.US_ASCII,
        )
    assertTrue(channel.writeInbound(response))
    assertIs<HttpResponse>(channel.readInbound<Any>())
    val content = assertIs<LastHttpContent>(channel.readInbound<Any>())
    try {
      assertEquals("hello", content.content().toString(CharsetUtil.US_ASCII))
    } finally {
      content.release()
    }
    channel.finishAndReleaseAll()
  }
}

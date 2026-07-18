package com.hect0x7.proxy.core.internal.socks

import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.socksx.v5.Socks5AddressType
import io.netty.handler.codec.socksx.v5.Socks5AuthMethod
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder
import io.netty.handler.codec.socksx.v5.Socks5CommandType
import io.netty.handler.codec.socksx.v5.Socks5InitialRequest
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder
import kotlin.test.Test
import kotlin.test.assertEquals

class Socks5ProtocolParsingTest {
  @Test
  fun parsesNoAuthGreeting() {
    val channel = EmbeddedChannel(Socks5InitialRequestDecoder())

    channel.writeInbound(Unpooled.wrappedBuffer(byteArrayOf(0x05, 0x01, 0x00)))
    val request = channel.readInbound<Socks5InitialRequest>()

    assertEquals(listOf(Socks5AuthMethod.NO_AUTH), request.authMethods())
    channel.finishAndReleaseAll()
  }

  @Test
  fun parsesDomainConnectCommand() {
    val host = "example.com".encodeToByteArray()
    val bytes =
        byteArrayOf(
            0x05,
            0x01,
            0x00,
            0x03,
            host.size.toByte(),
            *host,
            0x01,
            0xBB.toByte(),
        )
    val channel = EmbeddedChannel(Socks5CommandRequestDecoder())

    channel.writeInbound(Unpooled.wrappedBuffer(bytes))
    val request = channel.readInbound<Socks5CommandRequest>()

    assertEquals(Socks5CommandType.CONNECT, request.type())
    assertEquals(Socks5AddressType.DOMAIN, request.dstAddrType())
    assertEquals("example.com", request.dstAddr())
    assertEquals(443, request.dstPort())
    channel.finishAndReleaseAll()
  }
}

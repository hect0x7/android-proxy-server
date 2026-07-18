package com.hect0x7.proxy.core.internal.http

import com.hect0x7.proxy.core.internal.StatsTracker
import io.netty.channel.ChannelInitializer
import io.netty.channel.group.ChannelGroup
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpServerCodec

internal class HttpProxyInitializer(
    private val channels: ChannelGroup,
    private val stats: StatsTracker,
) : ChannelInitializer<SocketChannel>() {
  override fun initChannel(channel: SocketChannel) {
    channel.pipeline().addLast(CODEC_NAME, HttpServerCodec())
    channel.pipeline().addLast(HANDLER_NAME, HttpProxyHandler(channels, stats))
  }

  companion object {
    const val CODEC_NAME = "http-codec"
    const val HANDLER_NAME = "http-proxy"
  }
}

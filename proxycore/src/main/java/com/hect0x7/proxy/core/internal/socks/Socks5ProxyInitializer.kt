package com.hect0x7.proxy.core.internal.socks

import com.hect0x7.proxy.core.internal.StatsTracker
import io.netty.channel.ChannelInitializer
import io.netty.channel.group.ChannelGroup
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder

internal class Socks5ProxyInitializer(
    private val channels: ChannelGroup,
    private val stats: StatsTracker,
) : ChannelInitializer<SocketChannel>() {
  override fun initChannel(channel: SocketChannel) {
    channel.pipeline().addLast(ENCODER_NAME, Socks5ServerEncoder.DEFAULT)
    channel.pipeline().addLast(INITIAL_DECODER_NAME, Socks5InitialRequestDecoder())
    channel.pipeline().addLast(HANDLER_NAME, Socks5ProxyHandler(channels, stats))
  }

  companion object {
    const val ENCODER_NAME = "socks5-encoder"
    const val INITIAL_DECODER_NAME = "socks5-initial-decoder"
    const val COMMAND_DECODER_NAME = "socks5-command-decoder"
    const val HANDLER_NAME = "socks5-proxy"
  }
}

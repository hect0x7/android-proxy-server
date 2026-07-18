package com.hect0x7.proxy.core.internal

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter

internal class ConnectionLifecycleHandler(private val stats: StatsTracker) :
    ChannelInboundHandlerAdapter() {
  private var opened = false

  override fun channelActive(ctx: ChannelHandlerContext) {
    if (!opened) {
      opened = true
      stats.connectionOpened()
    }
    ctx.fireChannelActive()
  }

  override fun channelInactive(ctx: ChannelHandlerContext) {
    if (opened) {
      opened = false
      stats.connectionClosed()
    }
    ctx.fireChannelInactive()
  }
}

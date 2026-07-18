package com.hect0x7.proxy.core.internal

import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise

internal class TrafficAccountingHandler(private val stats: StatsTracker) : ChannelDuplexHandler() {
  override fun write(ctx: ChannelHandlerContext, message: Any, promise: ChannelPromise) {
    stats.sent(message)
    ctx.write(message, promise)
  }
}

package com.hect0x7.proxy.core.internal

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.util.ReferenceCountUtil

internal class RelayHandler(
    private val peer: Channel,
    private val direction: Direction,
    private val stats: StatsTracker,
) : ChannelInboundHandlerAdapter() {
  override fun channelRead(ctx: ChannelHandlerContext, message: Any) {
    if (!peer.isActive) {
      ReferenceCountUtil.release(message)
      ctx.close()
      return
    }

    when (direction) {
      Direction.SENT -> stats.sent(message)
      Direction.RECEIVED -> stats.received(message)
    }
    peer.writeAndFlush(message)
  }

  override fun channelInactive(ctx: ChannelHandlerContext) {
    closeOnFlush(peer)
  }

  override fun channelWritabilityChanged(ctx: ChannelHandlerContext) {
    peer.config().isAutoRead = ctx.channel().isWritable
    ctx.fireChannelWritabilityChanged()
  }

  override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    stats.recordError(cause)
    closeOnFlush(ctx.channel())
  }

  internal enum class Direction {
    SENT,
    RECEIVED,
  }
}

internal fun closeOnFlush(channel: Channel) {
  if (channel.isActive) {
    channel.writeAndFlush(channel.alloc().buffer(0)).addListener { channel.close() }
  } else {
    channel.close()
  }
}

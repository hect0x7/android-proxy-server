package com.hect0x7.proxy.core.internal.socks

import com.hect0x7.proxy.core.internal.RelayHandler
import com.hect0x7.proxy.core.internal.StatsTracker
import com.hect0x7.proxy.core.internal.closeOnFlush
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.group.ChannelGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse
import io.netty.handler.codec.socksx.v5.DefaultSocks5InitialResponse
import io.netty.handler.codec.socksx.v5.Socks5AddressType
import io.netty.handler.codec.socksx.v5.Socks5AuthMethod
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus
import io.netty.handler.codec.socksx.v5.Socks5CommandType
import io.netty.handler.codec.socksx.v5.Socks5InitialRequest
import io.netty.util.ReferenceCountUtil
import java.net.Inet6Address
import java.net.InetSocketAddress

internal class Socks5ProxyHandler(
    private val channels: ChannelGroup,
    private val stats: StatsTracker,
) : ChannelInboundHandlerAdapter() {
  private var outbound: Channel? = null
  private var connectFuture: ChannelFuture? = null

  override fun channelRead(ctx: ChannelHandlerContext, message: Any) {
    when (message) {
      is Socks5InitialRequest -> handleInitial(ctx, message)
      is Socks5CommandRequest -> handleCommand(ctx, message)
      else -> {
        ReferenceCountUtil.release(message)
        closeOnFlush(ctx.channel())
      }
    }
  }

  private fun handleInitial(ctx: ChannelHandlerContext, request: Socks5InitialRequest) {
    val supportsNoAuth = request.authMethods().contains(Socks5AuthMethod.NO_AUTH)
    ReferenceCountUtil.release(request)
    if (!supportsNoAuth) {
      ctx.writeAndFlush(DefaultSocks5InitialResponse(Socks5AuthMethod.UNACCEPTED))
          .addListener { closeOnFlush(ctx.channel()) }
      return
    }

    ctx.writeAndFlush(DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH))
    ctx.pipeline().addBefore(
        Socks5ProxyInitializer.HANDLER_NAME,
        Socks5ProxyInitializer.COMMAND_DECODER_NAME,
        Socks5CommandRequestDecoder(),
    )
  }

  private fun handleCommand(ctx: ChannelHandlerContext, request: Socks5CommandRequest) {
    if (request.type() != Socks5CommandType.CONNECT) {
      val response =
          DefaultSocks5CommandResponse(Socks5CommandStatus.COMMAND_UNSUPPORTED, request.dstAddrType())
      ReferenceCountUtil.release(request)
      ctx.writeAndFlush(response).addListener { closeOnFlush(ctx.channel()) }
      return
    }

    val host = request.dstAddr()
    val port = request.dstPort()
    val addressType = request.dstAddrType()
    ReferenceCountUtil.release(request)
    ctx.channel().config().isAutoRead = false
    connect(ctx, host, port, addressType)
  }

  private fun connect(
      ctx: ChannelHandlerContext,
      host: String,
      port: Int,
      addressType: Socks5AddressType,
  ) {
    val client = ctx.channel()
    val future =
        Bootstrap()
            .group(client.eventLoop())
            .channel(NioSocketChannel::class.java)
            .option(ChannelOption.AUTO_READ, false)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MILLIS)
            .option(ChannelOption.TCP_NODELAY, true)
            .handler(
                object : ChannelInitializer<SocketChannel>() {
                  override fun initChannel(channel: SocketChannel) {
                    channel.pipeline().addLast(
                        RelayHandler(client, RelayHandler.Direction.RECEIVED, stats)
                    )
                  }
                }
            )
            .connect(host, port)

    connectFuture = future
    future.addListener { completed ->
      connectFuture = null
      if (!client.isActive) {
        future.channel().close()
        return@addListener
      }
      if (!completed.isSuccess) {
        stats.recordError(completed.cause())
        val response = DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, addressType)
        ctx.writeAndFlush(response).addListener { closeOnFlush(ctx.channel()) }
        return@addListener
      }

      val remote = future.channel()
      outbound = remote
      channels.add(remote)
      val local = remote.localAddress() as InetSocketAddress
      val localType =
          if (local.address is Inet6Address) Socks5AddressType.IPv6 else Socks5AddressType.IPv4
      val response =
          DefaultSocks5CommandResponse(
              Socks5CommandStatus.SUCCESS,
              localType,
              local.address.hostAddress,
              local.port,
          )
      ctx.writeAndFlush(response).addListener { result ->
        if (!result.isSuccess || !ctx.channel().isActive) {
          closeOnFlush(remote)
          closeOnFlush(ctx.channel())
          return@addListener
        }
        establishRelay(ctx, remote)
      }
    }
  }

  private fun establishRelay(ctx: ChannelHandlerContext, remote: Channel) {
    val pipeline = ctx.pipeline()
    if (pipeline.get(Socks5ProxyInitializer.INITIAL_DECODER_NAME) != null) {
      pipeline.remove(Socks5ProxyInitializer.INITIAL_DECODER_NAME)
    }
    pipeline.remove(Socks5ProxyInitializer.COMMAND_DECODER_NAME)
    pipeline.remove(Socks5ProxyInitializer.ENCODER_NAME)
    pipeline.remove(Socks5ProxyInitializer.HANDLER_NAME)
    pipeline.addLast(RelayHandler(remote, RelayHandler.Direction.SENT, stats))
    remote.config().isAutoRead = true
    ctx.channel().config().isAutoRead = true
    remote.read()
    ctx.read()
  }

  override fun channelInactive(ctx: ChannelHandlerContext) {
    connectFuture?.cancel(false)
    connectFuture = null
    outbound?.let(::closeOnFlush)
    ctx.fireChannelInactive()
  }

  override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    stats.recordError(cause)
    connectFuture?.cancel(false)
    connectFuture = null
    outbound?.let(::closeOnFlush)
    closeOnFlush(ctx.channel())
  }

  private companion object {
    const val CONNECT_TIMEOUT_MILLIS = 10_000
  }
}

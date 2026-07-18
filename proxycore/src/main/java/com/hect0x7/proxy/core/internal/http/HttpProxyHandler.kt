package com.hect0x7.proxy.core.internal.http

import com.hect0x7.proxy.core.internal.RelayHandler
import com.hect0x7.proxy.core.internal.StatsTracker
import com.hect0x7.proxy.core.internal.TrafficAccountingHandler
import com.hect0x7.proxy.core.internal.closeOnFlush
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.group.ChannelGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpClientCodec
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpVersion
import io.netty.util.ReferenceCountUtil

internal class HttpProxyHandler(
    private val channels: ChannelGroup,
    private val stats: StatsTracker,
) : ChannelInboundHandlerAdapter() {
  private val pendingMessages = ArrayDeque<Any>()
  private var target: HttpTarget? = null
  private var outbound: Channel? = null
  private var connectFuture: ChannelFuture? = null
  private var connecting = false

  override fun channelRead(ctx: ChannelHandlerContext, message: Any) {
    try {
      when (message) {
        is HttpRequest -> handleRequest(ctx, message)
        else -> forwardOrQueue(message)
      }
    } catch (error: Throwable) {
      ReferenceCountUtil.release(message)
      stats.recordError(error)
      sendError(ctx, HttpResponseStatus.BAD_REQUEST)
    }
  }

  private fun handleRequest(ctx: ChannelHandlerContext, request: HttpRequest) {
    val parsed = HttpTargetParser.parse(request)
    val currentTarget = target
    if (currentTarget != null &&
        (parsed.connectTunnel || parsed.host != currentTarget.host || parsed.port != currentTarget.port)) {
      ReferenceCountUtil.release(request)
      sendError(ctx, HttpResponseStatus.BAD_GATEWAY)
      return
    }

    if (currentTarget == null) {
      target = parsed
      connecting = true
      ctx.channel().config().isAutoRead = false
      if (!parsed.connectTunnel) prepareForwardRequest(request, parsed)
      pendingMessages.addLast(request)
      connect(ctx, parsed)
      return
    }

    prepareForwardRequest(request, parsed)
    forwardOrQueue(request)
  }

  private fun prepareForwardRequest(request: HttpRequest, parsed: HttpTarget) {
    request.setUri(parsed.originForm)
    request.headers().remove(PROXY_CONNECTION)
    request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
  }

  private fun connect(ctx: ChannelHandlerContext, parsed: HttpTarget) {
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
                    if (!parsed.connectTunnel) {
                      channel.pipeline().addLast(TrafficAccountingHandler(stats))
                      channel.pipeline().addLast(HttpClientCodec())
                    }
                    channel.pipeline().addLast(
                        RelayHandler(client, RelayHandler.Direction.RECEIVED, stats)
                    )
                  }
                }
            )
            .connect(parsed.host, parsed.port)

    connectFuture = future
    future.addListener { completed ->
      connectFuture = null
      connecting = false
      if (!client.isActive) {
        releasePending()
        future.channel().close()
        return@addListener
      }
      if (!completed.isSuccess) {
        releasePending()
        stats.recordError(completed.cause())
        sendError(ctx, HttpResponseStatus.BAD_GATEWAY)
        return@addListener
      }

      val remote = future.channel()
      channels.add(remote)
      outbound = remote
      if (parsed.connectTunnel) establishTunnel(ctx, remote) else flushPending(ctx, remote)
    }
  }

  private fun establishTunnel(ctx: ChannelHandlerContext, remote: Channel) {
    releasePending()
    val response =
        DefaultFullHttpResponse(HttpVersion.HTTP_1_1, CONNECTION_ESTABLISHED, Unpooled.EMPTY_BUFFER)
    response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, 0)
    ctx.writeAndFlush(response).addListener { result ->
      if (!result.isSuccess || !ctx.channel().isActive) {
        closeOnFlush(remote)
        closeOnFlush(ctx.channel())
        return@addListener
      }

      val pipeline = ctx.pipeline()
      pipeline.remove(HttpProxyInitializer.CODEC_NAME)
      pipeline.remove(HttpProxyInitializer.HANDLER_NAME)
      pipeline.addLast(RelayHandler(remote, RelayHandler.Direction.SENT, stats))
      remote.config().isAutoRead = true
      ctx.channel().config().isAutoRead = true
      remote.read()
      ctx.read()
    }
  }

  private fun forwardOrQueue(message: Any) {
    val remote = outbound
    if (connecting || remote == null) pendingMessages.addLast(message)
    else remote.writeAndFlush(message)
  }

  private fun flushPending(ctx: ChannelHandlerContext, remote: Channel) {
    while (pendingMessages.isNotEmpty()) remote.write(pendingMessages.removeFirst())
    remote.flush()
    remote.config().isAutoRead = true
    ctx.channel().config().isAutoRead = true
    remote.read()
    ctx.read()
  }

  private fun sendError(ctx: ChannelHandlerContext, status: HttpResponseStatus) {
    connecting = false
    connectFuture?.cancel(false)
    connectFuture = null
    releasePending()
    if (!ctx.channel().isActive) return
    val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.EMPTY_BUFFER)
    response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, 0)
    response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
    ctx.writeAndFlush(response).addListener { closeOnFlush(ctx.channel()) }
  }

  private fun releasePending() {
    while (pendingMessages.isNotEmpty()) ReferenceCountUtil.release(pendingMessages.removeFirst())
  }

  override fun channelInactive(ctx: ChannelHandlerContext) {
    connectFuture?.cancel(false)
    connectFuture = null
    releasePending()
    outbound?.let(::closeOnFlush)
    ctx.fireChannelInactive()
  }

  override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    stats.recordError(cause)
    connectFuture?.cancel(false)
    connectFuture = null
    releasePending()
    outbound?.let(::closeOnFlush)
    closeOnFlush(ctx.channel())
  }

  private companion object {
    val CONNECTION_ESTABLISHED = HttpResponseStatus(200, "Connection Established")
    const val CONNECT_TIMEOUT_MILLIS = 10_000
    const val PROXY_CONNECTION = "Proxy-Connection"
  }
}

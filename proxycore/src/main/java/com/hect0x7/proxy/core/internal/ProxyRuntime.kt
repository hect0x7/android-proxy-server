package com.hect0x7.proxy.core.internal

import com.hect0x7.proxy.core.ProxyConfig
import com.hect0x7.proxy.core.internal.http.HttpProxyInitializer
import com.hect0x7.proxy.core.internal.socks.Socks5ProxyInitializer
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.MultiThreadIoEventLoopGroup
import io.netty.channel.group.ChannelGroup
import io.netty.channel.group.DefaultChannelGroup
import io.netty.channel.nio.NioIoHandler
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.util.concurrent.GlobalEventExecutor

internal class ProxyRuntime(private val stats: StatsTracker) {
  private val channels: ChannelGroup = DefaultChannelGroup(GlobalEventExecutor.INSTANCE, true)
  private var bossGroup: EventLoopGroup? = null
  private var workerGroup: EventLoopGroup? = null

  suspend fun start(config: ProxyConfig) {
    val boss = MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory())
    val worker = MultiThreadIoEventLoopGroup(NioIoHandler.newFactory())
    bossGroup = boss
    workerGroup = worker

    try {
      if (config.httpEnabled) {
        bind(config.httpPort, boss, worker, HttpProxyInitializer(channels, stats))
      }
      if (config.socksEnabled) {
        bind(config.socksPort, boss, worker, Socks5ProxyInitializer(channels, stats))
      }
    } catch (error: Throwable) {
      stop()
      throw error
    }
  }

  suspend fun stop() {
    channels.close().awaitCompletion()
    bossGroup?.shutdownGracefully()?.awaitCompletion()
    workerGroup?.shutdownGracefully()?.awaitCompletion()
    bossGroup = null
    workerGroup = null
  }

  private suspend fun bind(
      port: Int,
      boss: EventLoopGroup,
      worker: EventLoopGroup,
      protocolInitializer: ChannelInitializer<SocketChannel>,
  ) {
    val bindFuture =
        ServerBootstrap()
            .group(boss, worker)
            .channel(NioServerSocketChannel::class.java)
            .childOption(ChannelOption.AUTO_READ, true)
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childHandler(
                object : ChannelInitializer<SocketChannel>() {
                  override fun initChannel(channel: SocketChannel) {
                    if (isSelfConnection(channel.localAddress(), channel.remoteAddress())) {
                      stats.localLoopBlocked()
                      channel.close()
                      return
                    }
                    channels.add(channel)
                    channel.pipeline().addLast(ConnectionLifecycleHandler(stats))
                    channel.pipeline().addLast(protocolInitializer)
                  }
                }
            )
            .bind(LISTEN_HOST, port)

    bindFuture.awaitCompletion()
    channels.add(bindFuture.channel())
  }

  private companion object {
    const val LISTEN_HOST = "0.0.0.0"
  }
}

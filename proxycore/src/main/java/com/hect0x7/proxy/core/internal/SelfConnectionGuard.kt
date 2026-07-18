package com.hect0x7.proxy.core.internal

import java.net.InetSocketAddress
import java.net.SocketAddress

internal fun isSelfConnection(localAddress: SocketAddress?, remoteAddress: SocketAddress?): Boolean {
  val local = localAddress as? InetSocketAddress ?: return false
  val remote = remoteAddress as? InetSocketAddress ?: return false
  if (remote.hostString.equals("localhost", ignoreCase = true)) return true
  val remoteIp = remote.address ?: return false
  val localIp = local.address ?: return false
  return remoteIp.isLoopbackAddress || remoteIp == localIp
}

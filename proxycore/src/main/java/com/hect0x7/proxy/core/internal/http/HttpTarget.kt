package com.hect0x7.proxy.core.internal.http

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpRequest
import java.net.URI

internal data class HttpTarget(
    val host: String,
    val port: Int,
    val connectTunnel: Boolean,
    val originForm: String,
)

internal object HttpTargetParser {
  fun parse(request: HttpRequest): HttpTarget {
    return if (request.method() == HttpMethod.CONNECT) parseConnect(request.uri())
    else parseForward(request)
  }

  private fun parseConnect(authority: String): HttpTarget {
    val uri = parseAuthority(authority)
    return HttpTarget(
        host = requireNotNull(uri.host) { "CONNECT target has no host" },
        port = if (uri.port == -1) 443 else uri.port,
        connectTunnel = true,
        originForm = authority,
    ).validate()
  }

  private fun parseForward(request: HttpRequest): HttpTarget {
    val requestUri = URI(request.uri())
    if (requestUri.isAbsolute) {
      require(requestUri.scheme.equals("http", ignoreCase = true)) {
        "Only http:// absolute proxy requests are supported"
      }
      val host = requireNotNull(requestUri.host) { "Request URI has no host" }
      return HttpTarget(
          host = host,
          port = if (requestUri.port == -1) 80 else requestUri.port,
          connectTunnel = false,
          originForm = requestUri.toOriginForm(),
      ).validate()
    }

    val hostHeader = request.headers()[HttpHeaderNames.HOST]
    require(!hostHeader.isNullOrBlank()) { "Relative proxy request requires a Host header" }
    val authority = parseAuthority(hostHeader)
    return HttpTarget(
        host = requireNotNull(authority.host) { "Host header has no host" },
        port = if (authority.port == -1) 80 else authority.port,
        connectTunnel = false,
        originForm = request.uri().ifBlank { "/" },
    ).validate()
  }

  private fun parseAuthority(authority: String): URI = URI("http://$authority")

  private fun URI.toOriginForm(): String {
    val path = rawPath?.ifBlank { "/" } ?: "/"
    return if (rawQuery == null) path else "$path?$rawQuery"
  }

  private fun HttpTarget.validate(): HttpTarget {
    require(host.isNotBlank()) { "Proxy target host is blank" }
    require(port in 1..65535) { "Proxy target port must be between 1 and 65535" }
    return this
  }
}

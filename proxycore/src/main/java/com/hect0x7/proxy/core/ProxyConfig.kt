package com.hect0x7.proxy.core

/** Configuration for the independent HTTP and SOCKS5 listeners. */
public data class ProxyConfig(
    public val httpEnabled: Boolean,
    public val httpPort: Int = 8080,
    public val socksEnabled: Boolean,
    public val socksPort: Int = 1080,
) {
  init {
    require(httpPort in 1..65535) { "httpPort must be between 1 and 65535" }
    require(socksPort in 1..65535) { "socksPort must be between 1 and 65535" }
    require(!httpEnabled || !socksEnabled || httpPort != socksPort) {
      "HTTP and SOCKS5 listeners must use different ports"
    }
  }
}

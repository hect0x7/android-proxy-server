# Android Proxy Server

English | [简体中文](README.zh-CN.md)

An independent, minimal, open-source, and ad-free HTTP/SOCKS5 proxy server for Android.

<img src="art/app-icon.png" width="160" alt="Android Proxy Server icon">

## Features

- HTTP proxy on port `8080` by default
- HTTPS tunneling through `CONNECT`
- SOCKS5 TCP `CONNECT` on port `1080` by default
- Independent HTTP and SOCKS5 switches and configurable ports
- Listens on local network interfaces for LAN clients
- Rejects connections originating from the phone itself to prevent recursive proxy loops
- Uses Android's current default network, including another active VPN
- Runs as a user-controlled foreground service
- Displays local connection counts, traffic statistics, and in-memory session logs

## Quick Start

Connect the Android phone and client devices to the same trusted LAN, start HTTP and/or SOCKS5 in the app, then enter the phone's LAN IP and the displayed port in the client proxy settings.

```text
Client device        Home router          Android phone             Internet
192.168.1.x   <---->  Same Wi-Fi/LAN  <---->  Proxy server  <---->  VPN/default route
                                               8080 HTTP
                                               1080 SOCKS5
```

See the illustrated [Getting Started Guide](docs/GETTING_STARTED.md) for protocol selection, setup examples, and troubleshooting.

## Privacy And Security

The app contains no advertising, analytics, telemetry, crash reporting, accounts, purchases, rating prompts, remote configuration, or automatic-update SDKs. Settings remain in Android private app storage, while traffic statistics and session logs remain in memory and are cleared when the service stops.

The proxy has no authentication and listens on `0.0.0.0`. Enable it only on trusted networks and never expose its ports directly to the internet.

Read the [Privacy Policy](PRIVACY.md) and [Security Policy](SECURITY.md) for details.

## VPN Compatibility

This app does not use Android `VpnService` and does not create a VPN interface. Outbound proxy connections follow Android's current default route, so the server can remain active while another VPN app is connected and send traffic through that VPN.

## Build And Release

Pushing to `master` runs the GitHub Actions test and APK workflow. Release builds use a fixed signing certificate stored in repository Actions Secrets. The workflow verifies the APK signature and confirms that its certificate matches the configured release keystore before uploading artifacts.

Local build command:

```text
./gradlew test :app:assembleDebug :app:assembleRelease
```

The project contains two modules:

- `app`: Compose UI and Android foreground service
- `proxycore`: HTTP, HTTPS `CONNECT`, and SOCKS5 TCP forwarding

The proxy core does not depend on Android `VpnService`.

## Limitations

- SOCKS5 supports unauthenticated TCP `CONNECT` only
- SOCKS5 `UDP ASSOCIATE` and `BIND` are not supported
- HTTP and SOCKS5 username/password authentication is not supported

## License

Copyright 2026 hect0x7. Licensed under the Apache License 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE).

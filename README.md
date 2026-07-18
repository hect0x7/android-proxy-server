# Android Proxy Server

一个独立、极简、开源、无广告的 Android HTTP / SOCKS5 代理服务器。

<img src="art/app-icon.png" width="160" alt="Android Proxy Server icon">

## 功能

- HTTP 代理，默认端口 `8080`
- HTTPS `CONNECT` 转发
- SOCKS5 TCP `CONNECT`，默认端口 `1080`
- HTTP 与 SOCKS 可独立启停和修改端口
- 监听本机网络接口，供局域网设备连接
- 拒绝来自手机自身的连接，防止与其他 VPN 或系统代理形成递归回环
- 使用 Android 当前默认网络出口，可与其他 VPN 应用同时工作
- 前台服务保持代理运行
- 本地显示连接数、流量统计和本次运行日志

## 隐私

应用不包含广告、分析、遥测、崩溃上报、账号、内购、评分提示、远程配置或自动更新 SDK。配置与本次运行日志只保存在本机；会话日志不会写入磁盘。详见 [PRIVACY.md](PRIVACY.md)。

代理默认不启用身份验证，并监听 `0.0.0.0`。请只在可信网络中启用，不要把端口直接暴露到公网。

## VPN 兼容性

本应用不是 `VpnService`，不会创建 Android VPN，也不会把 socket 强制绑定到物理网络。代理的出站连接遵循 Android 当前默认路由，因此可以在另一款 VPN 已连接时继续运行，并通过该 VPN 出站。

## 构建

推送到 `main` 后，GitHub Actions 会运行测试并生成 Debug 与 Release APK。Release APK 使用仓库 Actions Secrets 中保存的固定签名证书，可以直接覆盖升级。签名私钥不会提交到 Git。

本地构建命令：

```text
./gradlew test :app:assembleDebug :app:assembleRelease
```

项目由两个模块组成：`app` 负责界面和 Android 前台服务，`proxycore` 负责 HTTP、HTTPS CONNECT 与 SOCKS5 TCP 转发。代理核心不依赖 Android `VpnService`。

## 限制

- SOCKS5 仅支持无认证 TCP `CONNECT`
- 不支持 SOCKS `UDP ASSOCIATE` 或 `BIND`
- 不提供 HTTP/SOCKS 用户名密码认证

## License

Copyright 2026 hect0x7。项目采用 Apache License 2.0，参见 [LICENSE](LICENSE) 与 [NOTICE](NOTICE)。

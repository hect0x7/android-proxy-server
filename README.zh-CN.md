# Android Proxy Server

[English](README.md) | 简体中文

一个独立、极简、开源、无广告的 Android HTTP/SOCKS5 代理服务器。

<img src="art/app-icon.png" width="160" alt="Android Proxy Server 图标">

## 功能

- HTTP 代理，默认端口 `8080`
- HTTPS `CONNECT` 隧道转发
- SOCKS5 TCP `CONNECT`，默认端口 `1080`
- HTTP 与 SOCKS5 可独立启停和修改端口
- 监听本机网络接口，供局域网设备连接
- 拒绝来自手机自身的连接，防止递归代理回环
- 使用 Android 当前默认网络出口，包括另一款已连接的 VPN
- 通过用户主动控制的前台服务保持运行
- 本地显示连接数、流量统计和仅存于内存的本次会话日志

## 隐私与安全

应用不包含广告、分析、遥测、崩溃上报、账号、内购、评分提示、远程配置或自动更新 SDK。设置只保存在 Android 私有应用存储中；流量统计和会话日志只保存在内存中，并在服务停止后清除。

代理默认没有身份验证，并监听 `0.0.0.0`。请只在可信网络中启用，不要把代理端口直接暴露到公网。

详情参见[隐私说明](PRIVACY.zh-CN.md)和[安全说明](SECURITY.zh-CN.md)。

## VPN 兼容性

本应用不使用 Android `VpnService`，也不会创建 VPN 网络接口。代理出站连接遵循 Android 当前默认路由，因此可以在另一款 VPN 应用已连接时继续运行，并通过该 VPN 出站。

## 构建与发布

推送到 `master` 后，GitHub Actions 会运行测试并构建 APK。Release 使用仓库 Actions Secrets 中保存的固定签名证书；工作流会在上传产物前验证 APK 签名，并确认 APK 证书与配置的发布 keystore 一致。

本地构建命令：

```text
./gradlew test :app:assembleDebug :app:assembleRelease
```

项目包含两个模块：

- `app`：Compose 界面和 Android 前台服务
- `proxycore`：HTTP、HTTPS `CONNECT` 和 SOCKS5 TCP 转发

代理核心不依赖 Android `VpnService`。

## 限制

- SOCKS5 仅支持无认证 TCP `CONNECT`
- 不支持 SOCKS5 `UDP ASSOCIATE` 或 `BIND`
- 不支持 HTTP/SOCKS5 用户名密码认证

## 许可证

Copyright 2026 hect0x7。项目采用 Apache License 2.0，参见 [LICENSE](LICENSE) 和 [NOTICE](NOTICE)。

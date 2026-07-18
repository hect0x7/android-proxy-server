# Security Policy

English | [简体中文](SECURITY.zh-CN.md)

## Supported Versions

Security fixes are applied to the latest version on the `master` branch. Older APK artifacts are not maintained after a newer release is available.

## Deployment Boundary

Android Proxy Server is designed for a trusted local network. HTTP and SOCKS5 services do not implement authentication and listen on all local interfaces when enabled.

- Do not expose proxy ports directly to the internet.
- Do not enable the server on untrusted public Wi-Fi or hotspot networks.
- Use Android firewall or network-isolation controls when clients on the local network are not trusted.
- Stop both proxy services when they are not needed.

## VPN And Loop Prevention

The app does not use `VpnService`. Outbound connections follow Android's default route, including another active VPN. Connections originating from the phone itself are rejected to reduce recursive proxy loops, but external proxy chains must still be configured carefully.

## Release Integrity

Release APKs are built by GitHub Actions. The workflow verifies the APK signature and compares the APK certificate SHA-256 digest with the configured release keystore before uploading artifacts. Signing keys and passwords are not stored in the repository.

## Reporting A Vulnerability

Do not publish exploit details, credentials, private network addresses, or captured proxy traffic in a public issue. Use the repository's private vulnerability-reporting channel when available. If no private channel is available, open a public issue containing only a request for private contact and a minimal non-sensitive summary.

Include the affected version, Android version, reproduction conditions, expected behavior, and security impact. Remove tokens, passwords, traffic payloads, and personal information from all reports.

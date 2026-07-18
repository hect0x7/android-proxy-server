# Privacy Policy

English | [简体中文](PRIVACY.zh-CN.md)

Android Proxy Server provides only the local HTTP and SOCKS5 proxy services explicitly enabled by the user.

## Data Not Collected

The app contains no advertising, analytics, telemetry, crash reporting, remote configuration, accounts, in-app purchases, rating prompts, or automatic-update SDKs. It does not collect device identifiers, location, contacts, browsing history, proxy payloads, or traffic statistics, and it does not send data to the developer or third parties.

## Local Data

HTTP/SOCKS5 switches and port settings are stored in Android private app storage. Traffic statistics, connection counts, and session logs remain in memory and are cleared when the service stops. Android backup is disabled so app settings are not copied to cloud backup.

## Network Behavior

When a proxy is enabled, the app forwards client requests to their requested destinations as required by the selected proxy protocol. The app does not independently copy, inspect, persist, or upload proxied content.

## Permissions

- `INTERNET`: listen on proxy ports and create outbound connections
- `ACCESS_NETWORK_STATE`: display available local network addresses
- `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_SPECIAL_USE`: keep a user-enabled proxy service running
- `POST_NOTIFICATIONS`: display the foreground-service notification

## Security Boundary

The proxy has no authentication and listens on all local interfaces. Use it only on trusted networks. Do not expose its ports through router port forwarding, public firewall rules, or untrusted hotspot networks.

See the [Security Policy](SECURITY.md) for operational guidance and vulnerability reporting.

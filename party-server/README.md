# AuraMusic Party server

Minimal Node.js 20+ backend for the online-only Party Mode client. It relays song metadata and playback state; it never proxies audio.

## Run

1. Install Node.js 20 or newer.
2. Run npm install.
3. Set PUBLIC_WS_URL to the public secure WebSocket endpoint, for example wss://party-api.example.com/ws.
4. Set PORT if the hosting provider requires it.
5. Run npm start behind a TLS-terminating reverse proxy. Expose HTTPS for REST and WSS for WebSocket; do not expose an unencrypted public endpoint.

The hosting environment must support persistent WebSocket connections. This first version stores parties in memory, so use a single server instance; restarts clear all active parties. Add shared storage and distributed pub/sub before horizontal scaling.

## Android build configuration

Configure Gradle property partyApiBaseUrl with the HTTPS REST base URL (without a trailing slash), for example https://party-api.example.com. The backend's PUBLIC_WS_URL must point to its /ws endpoint.

## Security notes

- Pairing codes are random, short-lived, and rate-limited by client IP.
- A per-member random token authorizes the WebSocket session; the pairing code is not reused as a credential.
- The host alone can publish playback state or issue host commands.
- Only the host receives guest song requests.
- Audio bytes, stream URLs, provider cookies, local file paths, and account tokens are never relayed.
- Configure HTTPS/WSS at the reverse proxy, protect logs, and monitor abuse before production use.

This backend is an MVP foundation, not a hardened public service. Add persistent storage, distributed rate limits, metrics, stronger abuse controls, and end-to-end multi-device tests before a public launch.

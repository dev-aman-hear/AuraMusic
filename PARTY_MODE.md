# AuraMusic Party Mode (online songs only)

Party Mode coordinates online playback between a host and guests across different networks. Local/offline tracks and audio-file transfer are intentionally unsupported.

## Android client status

The Android branch contains the Party Mode screen, pairing flow, secure REST/WebSocket client, host playback-event model, and guest song-request controls. It is not usable end-to-end until a compatible backend is deployed and configured. Do not advertise the feature as production-ready before server and multi-device tests pass.

## Connection modes

The Android client now offers two transport choices:

### Local Wi-Fi

- The host starts an embedded WebSocket relay on port 8765 on the host phone.
- Guests join by entering the host's local IPv4 address and the six-character party code.
- Devices must be on the same reachable Wi-Fi network or a host hotspot. Router client isolation, VPN routing, or hotspot restrictions can prevent connections.
- This mode does not require the Internet Party backend. The host must keep AuraMusic running while hosting.
- Local transport uses a plain `ws://` connection on the LAN. The party code is a lightweight join credential, not end-to-end encryption; use trusted networks only. Do not treat local mode as secure against other people on the same network.
- Automatic discovery uses Android Network Service Discovery (NSD / mDNS, service type `_auramusic-party._tcp.`). When a host starts a party on the local network, guests running AuraMusic on the same Wi-Fi discover it automatically and can tap "Join" to connect.
- Manual IP and party code entry is retained as a robust fallback in case mDNS/multicast is filtered or unsupported by the network router.
- Includes rate limiting (cooldown per request, queue cap) and sanitization against abuse.

### Internet

- Internet parties continue to use the configured HTTPS API and WSS backend.
- Build with `-PpartyApiBaseUrl=https://your-host.example`.
- Devices can be on different networks.

Both modes relay playback metadata and party-control messages only. They do not transmit audio bytes, local files, stream URLs, cookies, or provider credentials. Each phone resolves supported YouTube Music/InnerTube or JioSaavn tracks independently and needs its own Internet access to play them.

### Experimental reliability work on `test/party-sync-reliability`

This test branch adapts several useful patterns from larger listen-together implementations without copying another app's architecture wholesale:

- **Idempotent playback state:** Party Mode applies an explicit play/pause target rather than toggling blindly on repeated snapshots.
- **Deduplicated resolution:** only one stream lookup/resolution runs per guest track identity at a time.
- **Stale-result rejection:** a resolved stream is discarded if the host has changed tracks or the guest has left the session while it was resolving.
- **Position compensation:** guest startup accounts for time elapsed since the latest host update, while paused snapshots keep their position fixed.
- **Testable policy:** provider normalization, track matching, stable resolution keys, and position compensation are extracted into pure helpers with unit tests.
- **Clearer party UI:** connection mode, role, listener count, invite code, current playback, song requests, and errors have distinct visual hierarchy.

Queue revision numbers, authoritative queue snapshots, clock-offset estimation, reconnect restoration, and next-track preloading remain follow-up work. They require coordinated protocol/backend changes or additional device testing; this branch does not claim to have completed them.

### Current verification status

Local Wi-Fi support has been added to the source branch, but it must still be compiled and tested on physical Android devices before being considered reliable. Verify host/guest joining, playback synchronization, requests, hotspot behavior, and reconnect handling. The local relay is a minimal LAN implementation; automatic discovery, QR codes, encrypted LAN transport, and robust abuse/rate limiting remain future improvements.

## Configure the endpoint

Set the secure HTTPS base URL in Gradle properties, for example:

partyApiBaseUrl=https://party-api.example.com

The API URL must use HTTPS and the WebSocket URL returned by the server must use WSS. Do not use a hard-coded test token, public demo server, or clear-text HTTP endpoint.

## REST contract

### POST /v1/parties

Request JSON: displayName (optional, max 32 characters).

Response JSON:
- partyId: opaque party identifier
- pairingCode: short human-readable code with a short server-enforced expiry
- memberToken: high-entropy per-member credential; never log it
- websocketUrl: wss:// URL
- expiresAt: optional ISO-8601 expiry timestamp

### POST /v1/parties/join

Request JSON: pairingCode and displayName. Response has the same shape as create, with guest membership.

The server must rate-limit create/join attempts, expire pairing codes, enforce party capacity, and return generic errors for invalid/expired codes. Pairing codes are not authentication credentials; the member token is.

## WebSocket protocol

On connect, the client sends an authenticate message containing partyId and memberToken. The server must validate membership before accepting commands.

Client message types:
- playback: host-only authoritative online-song metadata, position and play state
- host_command: host-only play, pause, next, previous, optionally with positionMs
- song_request: guest request containing title, artist and source
- song_request_response: host decision containing requestId and approved
- leave

Server message types:
- party_state: current song metadata, position, playing state, server time and member count
- song_request: requestId, title, artist, source and requester
- song_request_removed: requestId
- member_count: current member count
- error: safe user-facing message

## Playback and safety requirements

- The host is authoritative. Guests must never issue playback commands directly.
- Do not transmit audio bytes, local file paths, stream URLs, provider cookies, authorization headers, or account tokens over the party channel.
- Share stable song metadata/source identifiers only. Each device resolves the online song using its own supported provider integration and access context.
- Use server timestamps and scheduled start times for drift correction. On reconnect, receive the latest party state and seek/resume as appropriate.
- Validate message sizes, song metadata, member roles, request IDs, and rate limits server-side.
- Use HTTPS/WSS only, short-lived pairing codes, revocable member tokens, and no credentials in logs.
- Respect each online provider's terms and playback restrictions.

## Required acceptance tests

1. Host creates a party and a second device joins with the code over a different network.
2. Invalid, expired, and rate-limited codes fail safely.
3. Host play/pause/seek/next/previous state is propagated and drift is measured across devices.
4. Guests can request a song; only the host can approve/reject.
5. Reconnect restores party state and revoked tokens are rejected.
6. Offline/local songs are rejected for Party Mode.
7. No stream URLs, cookies, or audio payloads are sent to the server.

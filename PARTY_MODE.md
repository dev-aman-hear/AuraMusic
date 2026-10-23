# AuraMusic Party Mode (online songs only)

Party Mode coordinates online playback between a host and guests across different networks. Local/offline tracks and audio-file transfer are intentionally unsupported.

## Android client status

The Android branch contains the Party Mode screen, pairing flow, secure REST/WebSocket client, host playback-event model, and guest song-request controls. It is not usable end-to-end until a compatible backend is deployed and configured. Do not advertise the feature as production-ready before server and multi-device tests pass.

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

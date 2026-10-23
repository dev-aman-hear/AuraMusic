import { createServer } from "node:http";
import { randomBytes, randomInt } from "node:crypto";
import express from "express";
import { WebSocketServer, WebSocket } from "ws";

const PORT = Number(process.env.PORT || 8080);
const PUBLIC_WS_URL = (process.env.PUBLIC_WS_URL || "").trim();
const PARTY_TTL_MS = Number(process.env.PARTY_TTL_MS || 30 * 60 * 1000);
const MAX_PARTY_MEMBERS = Math.min(20, Math.max(2, Number(process.env.MAX_PARTY_MEMBERS || 8)));
const CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
const ALLOWED_SOURCES = new Set(["youtube", "youtube_music", "youtubemusic", "youtube music", "jiosaavn", "jio saavn", "jiosaavn 320k"]);
const parties = new Map();
const tokenIndex = new Map();
const rateBuckets = new Map();

const app = express();
app.disable("x-powered-by");
app.use(express.json({ limit: "16kb", strict: true }));

function rateLimit(req, res, next) {
  const key = req.ip || req.socket.remoteAddress || "unknown";
  const now = Date.now();
  const bucket = (rateBuckets.get(key) || []).filter((time) => now - time < 60_000);
  if (bucket.length >= 30) {
    res.status(429).json({ message: "Too many attempts. Please wait and try again." });
    return;
  }
  bucket.push(now);
  rateBuckets.set(key, bucket);
  next();
}

function safeText(value, maxLength = 100) {
  return typeof value === "string" ? value.trim().replace(/[\u0000-\u001f\u007f]/g, "").slice(0, maxLength) : "";
}

function randomCode() {
  let code = "";
  for (let i = 0; i < 6; i += 1) code += CODE_ALPHABET[randomInt(CODE_ALPHABET.length)];
  return code;
}

function uniqueCode() {
  let code;
  do { code = randomCode(); } while ([...parties.values()].some((party) => party.code === code));
  return code;
}

function createMember(role, displayName) {
  return {
    token: randomBytes(32).toString("base64url"),
    role,
    displayName: safeText(displayName, 32) || "AuraMusic listener",
    socket: null
  };
}

function sendJson(socket, payload) {
  if (socket && socket.readyState === WebSocket.OPEN) socket.send(JSON.stringify(payload));
}

function memberCount(party) {
  return party.members.size;
}

function broadcast(party, payload, exceptSocket = null) {
  for (const member of party.members.values()) {
    if (member.socket && member.socket !== exceptSocket) sendJson(member.socket, payload);
  }
}

function currentState(party) {
  return { type: "party_state", song: party.song, members: memberCount(party), serverTimeMs: Date.now() };
}

function removeParty(party) {
  parties.delete(party.id);
  for (const member of party.members.values()) {
    tokenIndex.delete(member.token);
    if (member.socket && member.socket.readyState === WebSocket.OPEN) member.socket.close(1000, "Party ended");
  }
  party.members.clear();
}

function isOnlineSource(value) {
  return ALLOWED_SOURCES.has(safeText(value, 40).toLowerCase());
}

function createPartyResponse(party, member) {
  return {
    partyId: party.id,
    pairingCode: party.code,
    memberToken: member.token,
    websocketUrl: PUBLIC_WS_URL,
    expiresAt: new Date(party.expiresAt).toISOString()
  };
}

app.get("/health", (_req, res) => res.json({ ok: true, service: "auramusic-party", activeParties: parties.size }));
app.post("/v1/parties", rateLimit, (req, res) => {
  if (!PUBLIC_WS_URL.startsWith("wss://")) {
    res.status(503).json({ message: "Party service is not configured." });
    return;
  }
  const id = randomBytes(16).toString("hex");
  const member = createMember("host", req.body?.displayName);
  const party = {
    id, code: uniqueCode(), createdAt: Date.now(), expiresAt: Date.now() + PARTY_TTL_MS,
    hostToken: member.token, members: new Map([[member.token, member]]), song: null, requests: new Map()
  };
  parties.set(id, party);
  tokenIndex.set(member.token, { partyId: id, member });
  res.status(201).json(createPartyResponse(party, member));
});

app.post("/v1/parties/join", rateLimit, (req, res) => {
  if (!PUBLIC_WS_URL.startsWith("wss://")) {
    res.status(503).json({ message: "Party service is not configured." });
    return;
  }
  const code = safeText(req.body?.pairingCode, 12).toUpperCase().replace(/\s/g, "");
  const party = [...parties.values()].find((candidate) => candidate.code === code && candidate.expiresAt > Date.now());
  if (!party) {
    res.status(404).json({ message: "That pairing code is invalid or has expired." });
    return;
  }
  if (party.members.size >= MAX_PARTY_MEMBERS) {
    res.status(409).json({ message: "This party is full." });
    return;
  }
  const member = createMember("guest", req.body?.displayName);
  party.members.set(member.token, member);
  tokenIndex.set(member.token, { partyId: party.id, member });
  res.status(201).json(createPartyResponse(party, member));
  broadcast(party, { type: "member_count", members: memberCount(party) });
});

const server = createServer(app);
const wss = new WebSocketServer({ noServer: true, maxPayload: 8 * 1024 });

server.on("upgrade", (request, socket, head) => {
  let pathname = "";
  try { pathname = new URL(request.url || "/", "http://localhost").pathname; } catch {}
  if (pathname !== "/ws") {
    socket.destroy();
    return;
  }
  wss.handleUpgrade(request, socket, head, (webSocket) => wss.emit("connection", webSocket, request));
});

wss.on("connection", (socket) => {
  let memberRecord = null;
  let authenticated = false;
  const authTimeout = setTimeout(() => {
    if (!authenticated) socket.close(1008, "Authentication required");
  }, 5000);

  socket.on("message", (buffer) => {
    if (buffer.length > 8 * 1024) {
      socket.close(1009, "Message too large");
      return;
    }
    let message;
    try { message = JSON.parse(buffer.toString("utf8")); } catch {
      sendJson(socket, { type: "error", message: "Invalid message." });
      return;
    }

    if (!authenticated) {
      if (message.type !== "authenticate") {
        socket.close(1008, "Authenticate first");
        return;
      }
      const partyId = safeText(message.partyId, 64);
      const token = safeText(message.memberToken, 128);
      const record = tokenIndex.get(token);
      const party = record && parties.get(record.partyId);
      if (!record || !party || party.id !== partyId || party.expiresAt <= Date.now()) {
        socket.close(1008, "Invalid or expired membership");
        return;
      }
      memberRecord = { party, member: record.member };
      memberRecord.member.socket = socket;
      authenticated = true;
      clearTimeout(authTimeout);
      sendJson(socket, currentState(party));
      broadcast(party, { type: "member_count", members: memberCount(party) });
      return;
    }

    const { party, member } = memberRecord;
    if (!parties.has(party.id) || party.expiresAt <= Date.now()) {
      socket.close(1008, "Party expired");
      return;
    }

    switch (message.type) {
      case "playback": {
        if (member.role !== "host") break;
        const rawSong = message.song && typeof message.song === "object" ? message.song : {};
        const source = safeText(rawSong.source, 40);
        const songId = safeText(rawSong.songId, 160);
        const title = safeText(rawSong.title, 180);
        const artist = safeText(rawSong.artist, 180);
        if (!songId || !title || !artist || !isOnlineSource(source)) {
          sendJson(socket, { type: "error", message: "Only supported online songs can be shared." });
          break;
        }
        // Never relay stream URLs, file paths, cookies, or provider credentials.
        party.song = {
          songId, title, artist, artworkUrl: safeText(rawSong.artworkUrl, 1000), source,
          positionMs: Math.max(0, Math.min(Number(rawSong.positionMs) || 0, 24 * 60 * 60 * 1000)),
          isPlaying: Boolean(rawSong.isPlaying)
        };
        broadcast(party, currentState(party));
        break;
      }
      case "host_command": {
        if (member.role !== "host") break;
        const command = safeText(message.command, 20);
        if (!["play", "pause", "next", "previous", "seek"].includes(command)) break;
        const payload = { type: "host_command", command };
        if (Number.isFinite(Number(message.positionMs))) payload.positionMs = Math.max(0, Number(message.positionMs));
        broadcast(party, payload, socket);
        break;
      }
      case "song_request": {
        if (member.role !== "guest") break;
        const title = safeText(message.title, 180);
        const artist = safeText(message.artist, 180);
        const source = safeText(message.source, 40);
        if (!title || !artist || !isOnlineSource(source)) {
          sendJson(socket, { type: "error", message: "Request a supported online song." });
          break;
        }
        const requestId = randomBytes(12).toString("hex");
        const request = { requestId, title, artist, source, requester: member.displayName, requesterToken: member.token };
        party.requests.set(requestId, request);
        const host = party.members.get(party.hostToken);
        sendJson(host?.socket, { type: "song_request", requestId, title, artist, source, requester: member.displayName });
        break;
      }
      case "song_request_response": {
        if (member.role !== "host") break;
        const requestId = safeText(message.requestId, 64);
        const pending = party.requests.get(requestId);
        if (!pending) break;
        const approved = Boolean(message.approved);
        const guest = party.members.get(pending.requesterToken);
        sendJson(guest?.socket, { type: "song_request_decision", requestId, approved, title: pending.title, artist: pending.artist, source: pending.source });
        party.requests.delete(requestId);
        broadcast(party, { type: "song_request_removed", requestId });
        break;
      }
      case "leave":
        socket.close(1000, "Member left");
        break;
      default:
        sendJson(socket, { type: "error", message: "Unsupported party message." });
    }
  });

  socket.on("close", () => {
    clearTimeout(authTimeout);
    if (!memberRecord) return;
    const { party, member } = memberRecord;
    if (member.socket === socket) member.socket = null;
    if (member.role === "host") {
      removeParty(party);
      return;
    }
    party.members.delete(member.token);
    tokenIndex.delete(member.token);
    for (const [id, request] of party.requests) if (request.requesterToken === member.token) party.requests.delete(id);
    broadcast(party, { type: "member_count", members: memberCount(party) });
  });
});

setInterval(() => {
  const now = Date.now();
  for (const party of parties.values()) if (party.expiresAt <= now) removeParty(party);
  for (const [ip, times] of rateBuckets) {
    const recent = times.filter((time) => now - time < 60_000);
    if (recent.length) rateBuckets.set(ip, recent);
    else rateBuckets.delete(ip);
  }
}, 30_000).unref();

server.listen(PORT, "0.0.0.0", () => {
  console.log("AuraMusic Party server listening on port " + PORT);
});

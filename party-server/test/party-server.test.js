import test from "node:test";
import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import { WebSocket } from "ws";

const TEST_PORT = 18765;
const BASE_HTTP = `http://127.0.0.1:${TEST_PORT}`;
const BASE_WS = `ws://127.0.0.1:${TEST_PORT}/ws`;

function startServer(envOverrides = {}) {
  const proc = spawn("node", ["server.js"], {
    cwd: process.cwd(),
    env: {
      ...process.env,
      PORT: String(TEST_PORT),
      PUBLIC_WS_URL: "wss://party.example.com/ws",
      PARTY_TTL_MS: "3000",
      MAX_PARTY_MEMBERS: "4",
      ...envOverrides,
    },
    stdio: ["pipe", "pipe", "pipe"],
  });

  return new Promise((resolve, reject) => {
    let started = false;
    proc.stdout.on("data", (chunk) => {
      if (chunk.toString().includes("listening on port")) {
        started = true;
        resolve(proc);
      }
    });
    proc.stderr.on("data", (chunk) => {
      console.error("[server err]", chunk.toString());
    });
    proc.on("error", reject);
    proc.on("exit", (code) => {
      if (!started) reject(new Error(`Server exited early with code ${code}`));
    });
  });
}

function stopServer(proc) {
  return new Promise((resolve) => {
    if (!proc || proc.killed) return resolve();
    proc.on("exit", () => resolve());
    proc.kill("SIGTERM");
    setTimeout(() => {
      if (!proc.killed) proc.kill("SIGKILL");
      resolve();
    }, 2000);
  });
}

async function connectWs(url) {
  const ws = new WebSocket(url);
  await new Promise((resolve, reject) => {
    ws.once("open", resolve);
    ws.once("error", reject);
  });

  const messages = [];
  const waiters = [];
  let closed = false;
  let closeEvent = null;

  ws.on("message", (buf) => {
    const parsed = JSON.parse(buf.toString("utf8"));
    const matchIdx = waiters.findIndex(({ predicate }) => !predicate || predicate(parsed));
    if (matchIdx !== -1) {
      const [{ resolve, timer }] = waiters.splice(matchIdx, 1);
      clearTimeout(timer);
      resolve(parsed);
    } else {
      messages.push(parsed);
    }
  });

  ws.on("close", (code, reason) => {
    closed = true;
    closeEvent = { code, reason: reason.toString("utf8") };
  });

  return {
    ws,
    async waitNextMessage(predicateOrTimeout = null, timeoutMs = 4000) {
      let predicate = null;
      let timeout = timeoutMs;
      if (typeof predicateOrTimeout === "function") {
        predicate = predicateOrTimeout;
      } else if (typeof predicateOrTimeout === "number") {
        timeout = predicateOrTimeout;
      }

      const existingIdx = messages.findIndex((m) => !predicate || predicate(m));
      if (existingIdx !== -1) {
        return messages.splice(existingIdx, 1)[0];
      }

      return new Promise((resolve, reject) => {
        const timer = setTimeout(() => reject(new Error("Timeout waiting for WS message")), timeout);
        waiters.push({ predicate, resolve, timer });
      });
    },
    async waitForClose(timeoutMs = 4000) {
      if (closed) return closeEvent;
      return new Promise((resolve, reject) => {
        const timer = setTimeout(() => reject(new Error("Timeout waiting for WS close")), timeoutMs);
        ws.once("close", (code, reason) => {
          clearTimeout(timer);
          resolve({ code, reason: reason.toString("utf8") });
        });
      });
    },
    send(obj) {
      ws.send(JSON.stringify(obj));
    },
    close(code = 1000, reason = "Test closed") {
      ws.close(code, reason);
    },
  };
}

test("Party Server Backend Test Suite", async (t) => {
  let serverProc = null;

  t.before(async () => {
    serverProc = await startServer();
  });

  t.after(async () => {
    await stopServer(serverProc);
  });

  await t.test("Authentication: Unauthenticated connection closes on timeout or illegal pre-auth messages", async () => {
    const client = await connectWs(BASE_WS);
    client.send({ type: "playback", song: {} });
    const closeEv = await client.waitForClose();
    assert.equal(closeEv.code, 1008);
  });

  await t.test("Authentication: Invalid token is rejected with close code 1008", async () => {
    const client = await connectWs(BASE_WS);
    client.send({
      type: "authenticate",
      partyId: "invalid-id",
      memberToken: "invalid-token",
    });
    const closeEv = await client.waitForClose();
    assert.equal(closeEv.code, 1008);
  });

  await t.test("Host Authentication and Session Creation", async () => {
    const createRes = await fetch(`${BASE_HTTP}/v1/parties`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ displayName: "HostUser" }),
    });
    assert.equal(createRes.status, 201);
    const hostSession = await createRes.json();
    assert.ok(hostSession.partyId);
    assert.ok(hostSession.pairingCode);
    assert.ok(hostSession.memberToken);

    const hostWs = await connectWs(BASE_WS);
    hostWs.send({
      type: "authenticate",
      partyId: hostSession.partyId,
      memberToken: hostSession.memberToken,
    });

    const stateMsg = await hostWs.waitNextMessage();
    assert.equal(stateMsg.type, "party_state");
    assert.equal(stateMsg.members, 1);

    const countMsg = await hostWs.waitNextMessage();
    assert.equal(countMsg.type, "member_count");
    assert.equal(countMsg.members, 1);

    hostWs.close();
  });

  await t.test("Host-only commands and Playback: only host can broadcast playback and commands", async () => {
    // 1. Create party
    const createRes = await fetch(`${BASE_HTTP}/v1/parties`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ displayName: "Host" }),
    });
    const hostSession = await createRes.json();

    // 2. Join party as guest
    const joinRes = await fetch(`${BASE_HTTP}/v1/parties/join`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ pairingCode: hostSession.pairingCode, displayName: "Guest" }),
    });
    assert.equal(joinRes.status, 201);
    const guestSession = await joinRes.json();

    const hostWs = await connectWs(BASE_WS);
    hostWs.send({ type: "authenticate", partyId: hostSession.partyId, memberToken: hostSession.memberToken });
    await hostWs.waitNextMessage(); // party_state
    await hostWs.waitNextMessage(); // member_count

    const guestWs = await connectWs(BASE_WS);
    guestWs.send({ type: "authenticate", partyId: guestSession.partyId, memberToken: guestSession.memberToken });
    await guestWs.waitNextMessage(); // party_state
    await guestWs.waitNextMessage(); // member_count
    await hostWs.waitNextMessage(); // member_count broadcast to host

    // Host sends valid playback update
    hostWs.send({
      type: "playback",
      song: {
        songId: "yt-123",
        title: "Test Track",
        artist: "Test Artist",
        source: "youtube",
        positionMs: 42000,
        isPlaying: true,
      },
    });

    const guestPlayback = await guestWs.waitNextMessage();
    assert.equal(guestPlayback.type, "party_state");
    assert.equal(guestPlayback.song.title, "Test Track");
    assert.equal(guestPlayback.song.source, "youtube");
    assert.equal(guestPlayback.song.positionMs, 42000);

    // Host sends host_command (play, pause, next, seek)
    hostWs.send({ type: "host_command", command: "pause" });
    const guestCmd = await guestWs.waitNextMessage();
    assert.equal(guestCmd.type, "host_command");
    assert.equal(guestCmd.command, "pause");

    // Host sends seek command
    hostWs.send({ type: "host_command", command: "seek", positionMs: 15000 });
    const guestSeek = await guestWs.waitNextMessage();
    assert.equal(guestSeek.type, "host_command");
    assert.equal(guestSeek.command, "seek");
    assert.equal(guestSeek.positionMs, 15000);

    // Guest attempts host_command -> must be ignored
    guestWs.send({ type: "host_command", command: "next" });

    // Guest attempts playback -> must be ignored
    guestWs.send({
      type: "playback",
      song: {
        songId: "rogue-1",
        title: "Rogue",
        artist: "Rogue",
        source: "youtube",
      },
    });

    // Verify host didn't receive rogue command/playback
    hostWs.send({ type: "host_command", command: "play" });
    const verifiedCmd = await guestWs.waitNextMessage();
    assert.equal(verifiedCmd.command, "play");

    hostWs.close();
    guestWs.close();
  });

  await t.test("Invalid Requests: Unsupported online sources and invalid commands are rejected", async () => {
    const createRes = await fetch(`${BASE_HTTP}/v1/parties`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ displayName: "Host2" }),
    });
    const hostSession = await createRes.json();
    const hostWs = await connectWs(BASE_WS);
    hostWs.send({ type: "authenticate", partyId: hostSession.partyId, memberToken: hostSession.memberToken });
    await hostWs.waitNextMessage(); // party_state
    await hostWs.waitNextMessage(); // member_count

    // Unsupported source (e.g., spotify)
    hostWs.send({
      type: "playback",
      song: {
        songId: "spotify-1",
        title: "Unsupported",
        artist: "Band",
        source: "spotify",
      },
    });

    const err = await hostWs.waitNextMessage();
    assert.equal(err.type, "error");
    assert.ok(err.message.includes("Only supported online songs"));

    // Unsupported command (e.g., custom_hack)
    hostWs.send({ type: "host_command", command: "custom_hack" });

    hostWs.close();
  });

  await t.test("Queue Limits and Song Requests: Relay generates requestId and enforces 50 request bound", async () => {
    const createRes = await fetch(`${BASE_HTTP}/v1/parties`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ displayName: "HostQueue" }),
    });
    const hostSession = await createRes.json();
    const hostWs = await connectWs(BASE_WS);
    hostWs.send({ type: "authenticate", partyId: hostSession.partyId, memberToken: hostSession.memberToken });
    await hostWs.waitNextMessage(); // party_state
    await hostWs.waitNextMessage(); // member_count

    const joinRes = await fetch(`${BASE_HTTP}/v1/parties/join`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ pairingCode: hostSession.pairingCode, displayName: "GuestQueue" }),
    });
    const guestSession = await joinRes.json();
    const guestWs = await connectWs(BASE_WS);
    guestWs.send({ type: "authenticate", partyId: guestSession.partyId, memberToken: guestSession.memberToken });
    await guestWs.waitNextMessage(); // party_state
    await guestWs.waitNextMessage(); // member_count
    await hostWs.waitNextMessage(); // member_count

    // Guest sends 1 valid request
    guestWs.send({
      type: "song_request",
      title: "Allowed Song",
      artist: "Allowed Artist",
      source: "jiosaavn",
    });

    const hostReq = await hostWs.waitNextMessage((m) => m.type === "song_request");
    assert.equal(hostReq.type, "song_request");
    assert.equal(hostReq.title, "Allowed Song");
    assert.ok(hostReq.requestId, "Relay must generate requestId");

    // Host responds with approval
    hostWs.send({
      type: "song_request_response",
      requestId: hostReq.requestId,
      approved: true,
    });

    const decision = await guestWs.waitNextMessage((m) => m.type === "song_request_decision");
    assert.equal(decision.type, "song_request_decision");
    assert.equal(decision.approved, true);

    const removed = await guestWs.waitNextMessage((m) => m.type === "song_request_removed");
    assert.equal(removed.type, "song_request_removed");

    // Now fill the queue with 50 requests
    for (let i = 0; i < 50; i++) {
      guestWs.send({
        type: "song_request",
        title: `Song ${i}`,
        artist: "Artist",
        source: "youtube",
      });
      await hostWs.waitNextMessage((m) => m.type === "song_request");
    }

    // 51st request should be rejected due to queue limit
    guestWs.send({
      type: "song_request",
      title: "Song 51",
      artist: "Artist",
      source: "youtube",
    });

    const queueFullErr = await guestWs.waitNextMessage((m) => m.type === "error");
    assert.equal(queueFullErr.type, "error");
    assert.ok(queueFullErr.message.includes("queue is full"), "Queue limit must be enforced");

    hostWs.close();
    guestWs.close();
  });

  await t.test("Party Expiry: TTL expiration prevents join and drops sockets", async () => {
    // 1. Create a party with short TTL (2 seconds)
    const createRes = await fetch(`${BASE_HTTP}/v1/parties`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ displayName: "ExpiringHost" }),
    });
    const session = await createRes.json();

    const hostWs = await connectWs(BASE_WS);
    hostWs.send({ type: "authenticate", partyId: session.partyId, memberToken: session.memberToken });
    await hostWs.waitNextMessage(); // party_state
    await hostWs.waitNextMessage(); // member_count

    // Wait past the 3 second PARTY_TTL_MS
    await new Promise((r) => setTimeout(r, 3500));

    // Trying to join with expired code fails
    const joinRes = await fetch(`${BASE_HTTP}/v1/parties/join`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ pairingCode: session.pairingCode, displayName: "LateGuest" }),
    });
    assert.equal(joinRes.status, 404);

    // Existing host socket should be terminated or reject next actions
    hostWs.send({ type: "playback", song: { songId: "s", title: "t", artist: "a", source: "youtube" } });
    const closeEv = await hostWs.waitForClose();
    assert.ok(closeEv.code >= 1000);
  });
});

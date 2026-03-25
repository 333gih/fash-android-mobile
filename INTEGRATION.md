# Connecting to Fash Realtime Service

This document explains **how clients and other services work with the realtime service**, in plain language. Another developer (or an AI assistant) can use it to integrate without reading all the Go code.

**What this service does in one sentence:** It keeps long-lived **WebSocket** connections from mobile or web apps, validates the same **login tokens** as your main API, listens to **Redis** for events from **core-service**, and pushes matching messages down to the right users.

**Default port:** `8080` (change with environment variable `HTTP_PORT`).

---

## 1. HTTP APIs — what to call, what you get back

All paths below are on the realtime host (example: `https://realtime.example.com` behind a reverse proxy, or `http://localhost:8080` locally).

### 1.1 Health check

| | |
|---|---|
| **Purpose** | Check that the process is alive and (if configured) that **Redis** is reachable. |
| **Request** | `GET /health` |
| **Query / body** | None. |
| **Success response** | JSON with `status: "ok"`, service name/version, `websocket.active_connections`, and either Redis status or `"redis": "not_configured"`. |
| **Failure** | If Redis is configured but fails: status **503** and `status: "degraded"`. |

Use this for load balancers and Kubernetes liveness/readiness probes.

### 1.2 Metrics (operators / Prometheus)

| | |
|---|---|
| **Purpose** | Prometheus scraping (connection counts, message routing, etc.). |
| **Request** | `GET /metrics` |
| **Response** | Plain text in Prometheus format (not JSON). |

Mobile apps and most backends **do not** call this in normal use.

### 1.3 WebSocket (the main API for apps)

| | |
|---|---|
| **Purpose** | Receive realtime events (chat, orders, etc.) after the user is logged in. |
| **Request** | `GET /ws` with **query parameters** (WebSocket upgrade). **Not** a normal REST JSON body. |
| **Required query** | `token` — the user’s **access JWT** (same token type as your REST API). |
| **Optional query** | `platform` — e.g. `android`, `ios`, `web` (used for metrics labels). |

**Important:** Browsers and many HTTP clients do not let you set custom headers on the first WebSocket request the same way as REST. That is why the token is passed as **`?token=...`** in the URL. Treat this URL as sensitive (it contains the session). Always use **WSS** (HTTPS) in production.

**First message from server after connect:** JSON (not wrapped in the envelope described later), for example:

```json
{
  "type": "connected",
  "user_id": "<user id>",
  "conn_id": "<this connection id>"
}
```

**After that**, most server → client messages use a **common envelope**:

- `type` — event name (string)
- `payload` — JSON object (varies by event)
- `ts` — time in milliseconds (Unix)

**Client → server** messages are small JSON objects, for example `{"type":"ping"}` or `{"type":"subscribe.conversation","conversation_id":"..."}`.

---

## 2. How Android (or any app) connects

### 2.1 Steps for the mobile app

1. **Log in through core-service** (or your auth API) and obtain an **access token** (JWT) exactly as you do for REST calls.
2. Open a **WebSocket** to:  
   `wss://<your-realtime-host>/ws?token=<URL_ENCODED_ACCESS_TOKEN>&platform=android`  
   Use your HTTP client’s WebSocket support (e.g. OkHttp `WebSocket` on Android).
3. Wait for the first JSON message (`type: connected`). Then handle incoming text frames as JSON (envelope with `type`, `payload`, `ts`).
4. **Keep the connection alive:** the server sends WebSocket **ping** frames on a schedule; the client library usually answers with **pong** automatically. The app can also send `{"type":"ping"}` and receive a JSON **pong** envelope.
5. **Subscribe to chat threads** the user opens: send `subscribe.conversation` with `conversation_id` so messages for that chat can be routed to this connection.
6. **Mark read in the backend:** when the user reads messages, your product should still call the **core-service HTTP API** for “mark read”. The realtime service can send a lightweight `read.ack` over WebSocket for UX, but **persistent read state lives in core-service**.

### 2.2 How this ties to **core-service** (chat)

Think of two paths:

| Direction | How it works |
|-----------|----------------|
| **App → core-service** | Send chat messages, offers, mark read, etc. using the **normal REST APIs** (same as today). Core-service writes to the database and **publishes events to Redis**. |
| **Redis → realtime service → app** | Realtime service **does not** read your database. It **subscribes to Redis channels** that core-service (and related services) publish to. When an event matches a connected user or a joined conversation, it pushes a WebSocket message to the app. |

So: **write path = REST to core-service**; **read path for live updates = WebSocket to realtime service**, both using the **same Redis** and **compatible JWT settings**.

**Chat alignment:** `chat:message:new` is delivered to the **recipient** by user id (no room subscription needed). Core publishes **`notify_user_id`** on **`chat:read:receipts`** so realtime delivers **`read.receipts`** to the **other participant** directly. **`subscribe.conversation`** is still needed for typing and any events that use conversation rooms; legacy payloads without `notify_user_id` fall back to room broadcast.

**Must match between core-service and realtime service:**

- Same **Redis** instance (host, password, DB if used) for pub/sub and blacklist.
- Same **`ACCESS_TOKEN_SECRET`** and **`APP_NAME`** (JWT issuer) if you use **HS256** tokens — or configure **RS256** public key on realtime if you switch token signing.
- Revoked tokens: core-service stores revoked JWT ids in Redis under keys like **`blacklist:jwt:<jti>`**; realtime checks the same so a logged-out session cannot open `/ws`.

---

## 3. What to watch out for (notices)

1. **Token lifetime** — When the access token expires, the WebSocket does not magically refresh. The app should refresh the token using your existing refresh flow, then **reconnect** `/ws` with the new token.
2. **One user, several devices** — The same user can have multiple connections. Events targeted at that user may be delivered to each active device.
3. **Chat rooms** — Send **`subscribe.conversation`** for typing and room-scoped behaviour. **`message.new`** and **`read.receipts`** (with current core payloads) target users by id and do not require a room join for delivery.
4. **No duplicate push assumption** — Push notifications (FCM) are expected to be handled mainly by **core-service** when the user is offline. Realtime focuses on **online** delivery; do not assume two independent FCM senders without a product decision.
5. **Scaling** — You can run **multiple copies** of the realtime service behind a load balancer. Redis pub/sub delivers each message to every copy; each copy only has the WebSockets connected to **that** instance. Sticky sessions are not strictly required for correctness of pub/sub, but your proxy timeouts for **idle** connections should be **longer** than the WebSocket ping interval so connections are not cut idle.
6. **Security** — Never log full `/ws` URLs in production (they contain the token). Use **WSS** in production.

---

## 4. How to run the service

### 4.1 Minimum configuration

- **`ACCESS_TOKEN_SECRET`** and **`APP_NAME`** — must match how tokens are issued (same as core-service for HS256).
- **`REDIS_HOST`** (and password if needed) — required for pub/sub, presence, and token blacklist in real deployments.

See **`.env.example`** in this repository for all variable names.

### 4.2 Local (developer machine)

```text
go run ./cmd
```

Or build a binary from `./cmd` and run it. Ensure Redis is reachable if you want full behaviour.

### 4.3 Docker

Build the image from the `Dockerfile`, pass the same environment variables, publish port **8080** (or your `HTTP_PORT`).

### 4.4 Docker Compose

The repo includes **`docker-compose.yml`** that starts Redis and the realtime service. Set **`ACCESS_TOKEN_SECRET`** (and **`APP_NAME`** if not default) in a `.env` file or the shell before `docker compose up`.

### 4.5 Behind Traefik / reverse proxy

Expose **`/ws`** (and optionally `/health`, `/metrics`) to this service. Use **HTTPS** so clients use **WSS**.

---

## 5. Quick reference — message types the app will see

Exact **payload** fields depend on the event; all use the envelope (`type`, `payload`, `ts`) unless noted.

| `type` (examples) | Meaning (simple) |
|-------------------|------------------|
| `connected` | Connection ready (first message, not always in envelope form). |
| `message.new` | New chat-related message (from Redis `chat:message:new`). |
| `read.receipts` | Someone marked messages read in a conversation. |
| `read.ack` | Acknowledgement that the server saw a `read.messages` client action (you still save state via REST). |
| `typing.start` / `typing.stop` | Someone is typing in a conversation. |
| `order.status_changed` | Order lifecycle update from payment-related Redis events. |
| `feed.refresh` | Listing/feed hint for the seller (when core publishes listing created). |
| `pong` | Response to client `ping`. |

Your core-service team can extend what is **published to Redis** over time; the realtime service only forwards what it subscribes to and maps in its dispatcher.

---

## 6. If something does not work

- **`401` on `/ws`** — Missing token, invalid token, wrong issuer/secret, or token revoked (blacklist).
- **No chat events** — Check Redis connectivity, that core-service actually **publishes** to the expected channels, and that the user **subscribed** to the conversation when needed.
- **Health unhealthy** — Often Redis connection failure or wrong host/port/password.

For deployment details (Jenkins, staging checks), see **`DEPLOY.md`**.

---

## 7. How to test chat integration (manual)

1. **Shared Redis** — Run core-service and realtime-service against the **same** Redis (same DB index if you use one).
2. **Matching JWT** — Set **`ACCESS_TOKEN_SECRET`** and **`APP_NAME`** on realtime to match core’s token issuer (or configure RS256 public key if core uses that).
3. **WebSocket** — Obtain an access token (login via core), then connect:  
   `ws://localhost:8080/ws?token=<JWT>&platform=web`  
   (use **WSS** in staging/production). You should see `{"type":"connected",...}`.
4. **New message** — From another session or REST client, send a chat message via core so the **other** user is the recipient. The recipient’s WebSocket should receive **`message.new`** without subscribing to the conversation.
5. **Read receipt** — As the recipient, call core’s **mark read** API. The **sender’s** connection (logged in as the peer user) should receive **`read.receipts`** with `reader_id` set to who read.
6. **Redis-only smoke test** (no core) — With realtime connected as user `u1`, publish from `redis-cli`:  
   `PUBLISH chat:message:new '{"conversation_id":"c1","message_id":"m1","sender_id":"s","recipient_id":"u1","preview":"x","message_type":"text"}'`  
   The client for `u1` should get **`message.new`**. For read receipts:  
   `PUBLISH chat:read:receipts '{"conversation_id":"c1","recipient_id":"reader","notify_user_id":"u1"}'`  
   User `u1` should get **`read.receipts`**.

# Connecting to Fash Realtime Service

This document explains **how clients (including Android) and other services work with the realtime service**, in plain language. Another developer (or an AI assistant) can use it to integrate without reading all the Go code.

**What this service does in one sentence:** It keeps long-lived **WebSocket** connections from mobile or web apps, validates the same **login tokens** as your main API, consumes **Redis** events from **core-service** (via **Redis Streams** or legacy **Pub/Sub**), and pushes matching messages down to the right users.

**Default port:** `8080` (change with environment variable `HTTP_PORT`).

---

## 0. What changed (integration changelog)

| Topic | Behaviour |
|-------|-----------|
| **Redis ingestion** | By default the service consumes **Redis Streams** with a **consumer group** (`realtime-group`), matching how core-service publishes with `XADD` to named streams. This aligns realtime with stream-based core publishers. |
| **Legacy Pub/Sub** | Set **`REDIS_USE_STREAMS=false`** to use only **Redis Pub/Sub** (`SUBSCRIBE` on channel names like `chat:message:new`). Use this if core still publishes only with `PUBLISH` and you must not consume streams. |
| **Dispatcher** | Unchanged logical channels (`chat:message:new`, etc.): stream entries are mapped to the same internal routing as Pub/Sub. |
| **Health** | **`GET /health`** returns **HTTP 200** when OK or when Redis is not configured; **HTTP 503** with `status: "degraded"` if Redis is configured but the ping fails. |

---

## 1. HTTP APIs — full list (for Android and backends)

All paths are on the realtime host (example: `https://realtime.example.com` behind a reverse proxy, or `http://localhost:8080` locally). **There is no REST JSON API for business data** — only operational endpoints plus **WebSocket**.

| Method | Path | Purpose | Who uses it |
|--------|------|---------|-------------|
| **GET** | `/health` | Liveness / readiness JSON (process up, optional Redis ping, WebSocket connection count). | Load balancers, K8s probes, ops. Apps *may* call it but usually do not. |
| **GET** | `/metrics` | **Prometheus** metrics (plain text, not JSON). | Operators / monitoring only — **not** for mobile apps. |
| **GET** | `/ws` | **WebSocket upgrade** — main API for realtime events (see §1.3). | **Android, iOS, web clients.** |
| **GET** | `/swagger/*` | **Swagger UI** (OpenAPI) for this service’s documented HTTP surface. | Developers in browser; **not** required for app integration. |

There are **no** `POST` / `PUT` / `DELETE` HTTP routes for app features — chat and orders stay on **core-service REST APIs**.

### 1.1 Health check

| | |
|---|---|
| **Purpose** | Check that the process is alive and (if configured) that **Redis** is reachable. |
| **Request** | `GET /health` |
| **Query / body** | None. |
| **HTTP 200** | JSON: `status: "ok"`, `service.name` / `service.version`, `websocket.active_connections`, and either `redis: "not_configured"` or `redis: { "ok": true }`. |
| **HTTP 503** | Redis is configured but ping failed: `status: "degraded"`, `redis: { "ok": false, "error": "..." }`. |

Use this for load balancers and Kubernetes liveness/readiness probes.

### 1.2 Metrics (operators / Prometheus)

| | |
|---|---|
| **Purpose** | Prometheus scraping (connection counts, message routing, Redis handler latency, etc.). |
| **Request** | `GET /metrics` |
| **Response** | `Content-Type`: Prometheus text exposition format (not JSON). |

Mobile apps and most backends **do not** call this in normal use.

### 1.3 WebSocket — main API for apps (`GET /ws`)

| | |
|---|---|
| **Purpose** | Receive realtime events (chat, typing, orders, feed hints) after the user is logged in. |
| **Request** | `GET /ws` with **query parameters** (WebSocket upgrade). **Not** a normal REST JSON body. |
| **Required query** | **`token`** — the user’s **access JWT** (same token type as your REST API). URL-encode the value. |
| **Optional query** | **`platform`** — e.g. `android`, `ios`, `web` (used for Prometheus labels). |

**HTTP responses before upgrade (not WebSocket):**

| HTTP | When |
|------|------|
| **401** | Missing `token`, invalid JWT, wrong issuer/secret, or **revoked** token (`jti` on blacklist). Response body: JSON `{"error":"..."}` (e.g. `token required`, `invalid token`, `token revoked`). |
| **503** | JWT was valid but **blacklist / Redis** check failed (e.g. Redis down). JSON `{"error":"token check failed"}`. |

**101 Switching Protocols** — connection becomes WebSocket; the app then reads **text frames** (JSON).

**Important:** Many clients cannot set custom headers on the first WebSocket request like REST. That is why the token is passed as **`?token=...`**. Treat this URL as **sensitive** (session). Always use **WSS** in production.

**First message from server after connect** — JSON **without** the `ts` envelope used later:

```json
{
  "type": "connected",
  "user_id": "<user id from JWT>",
  "conn_id": "<this connection id>"
}
```

**After that**, most server → client messages use the **envelope**:

- **`type`** — event name (string)
- **`payload`** — JSON object (varies by event)
- **`ts`** — Unix time in **milliseconds**

**Client → server** — send **text frames** with JSON objects (see §5.1).

---

## 2. How Android (or any app) connects

### 2.1 Steps for the mobile app

1. **Log in through core-service** (or your auth API) and obtain an **access token** (JWT) exactly as you do for REST calls.
2. Open a **WebSocket** to:  
   `wss://<your-realtime-host>/ws?token=<URL_ENCODED_ACCESS_TOKEN>&platform=android`  
   Use your HTTP client’s WebSocket support (e.g. OkHttp `WebSocket` on Android).
3. Wait for the first JSON message (`type: connected`). Then handle incoming text frames as JSON (envelope with `type`, `payload`, `ts` where applicable).
4. **Keep the connection alive:** the server sends WebSocket **ping** frames on a schedule; the client library usually answers with **pong** automatically. The app can also send `{"type":"ping"}` and receive a JSON **`pong`** envelope.
5. **Subscribe to chat threads** the user opens: send **`subscribe.conversation`** with **`conversation_id`** so **typing** and **room-scoped** events reach this connection.
6. **Mark read in the backend:** when the user reads messages, call the **core-service HTTP API** for “mark read”. The realtime service can send a lightweight **`read.ack`** over WebSocket for UX, but **persistent read state lives in core-service**.

### 2.2 How this ties to **core-service** (Redis)

Think of two paths:

| Direction | How it works |
|-----------|----------------|
| **App → core-service** | Send chat messages, offers, mark read, etc. using the **normal REST APIs**. Core writes to the database and **publishes events to Redis** (typically **Redis Streams** in current core; some setups may still use **Pub/Sub**). |
| **Redis → realtime service → app** | Realtime **does not** read your database. It consumes Redis (**Streams** with a consumer group when `REDIS_USE_STREAMS=true`, or **Pub/Sub** when `REDIS_USE_STREAMS=false`), maps events to the same internal routing as before, and pushes **WebSocket** messages to the app. |

So: **write path = REST to core-service**; **read path for live updates = WebSocket to realtime service**, using the **same Redis** and **compatible JWT settings** as core.

**Chat alignment:** New messages are delivered to the **recipient** by user id (no room subscription needed for **`message.new`**). Core should publish **`notify_user_id`** on read-receipt events so realtime delivers **`read.receipts`** to the **other participant** directly. **`subscribe.conversation`** is still needed for **typing** and any events that use **conversation rooms**; legacy payloads without **`notify_user_id`** fall back to room broadcast.

**Must match between core-service and realtime service:**

- Same **Redis** instance (host, password, DB if used) for events, presence, and token blacklist.
- Same **`ACCESS_TOKEN_SECRET`** and JWT **`iss`** expectation: **`JWT_ISSUER`**, **`JWT_ALLOWED_ISSUERS`**, or **`APP_NAME`** (defaults to **`core-service`**) for **HS256** — or **RS256** via **`JWT_PUBLIC_KEY_PEM`** / **`JWT_PUBLIC_KEY_PATH`** on realtime if core signs that way.
- Revoked tokens: core stores revoked JWT ids in Redis (e.g. **`blacklist:jwt:<jti>`**); realtime checks the same so a logged-out session cannot open **`/ws`**.

**Operator note:** **`REDIS_USE_STREAMS`** (default **`true`**) must align with how core publishes. If core only uses **Streams**, realtime should consume Streams; if you temporarily rely on **Pub/Sub** only, set **`REDIS_USE_STREAMS=false`**.

---

## 3. What to watch out for (notices)

1. **Token lifetime** — When the access token expires, the WebSocket does not refresh automatically. Refresh the token with your existing flow, then **reconnect** `/ws` with the new token.
2. **One user, several devices** — The same user can have multiple connections. Events targeted at that user may be delivered to **each** active device.
3. **Chat rooms** — Send **`subscribe.conversation`** for **typing** and room-scoped behaviour. **`message.new`** and **`read.receipts`** (with current core payloads) can target users by id and may not require a room join for delivery.
4. **No duplicate push assumption** — Push notifications (FCM) are expected mainly from **core-service** when offline. Realtime focuses on **online** WebSocket delivery.
5. **Scaling** — You can run **multiple** realtime instances behind a load balancer. **WebSockets** are tied to the instance that accepted the connection (use appropriate proxy timeouts; idle timeout should exceed the server’s WebSocket **ping** interval). **Redis Pub/Sub** delivers each message to **every** instance (each instance forwards only to its own clients). **Redis Streams** with a **shared consumer group** delivers each stream message to **one** consumer in the group (work is split across instances; scale consumers with care for ordering per stream).
6. **Security** — Never log full `/ws` URLs in production (they contain the token). Use **WSS** in production.

---

## 4. How to run the service

### 4.1 Minimum configuration

- **`ACCESS_TOKEN_SECRET`** and **`JWT_ISSUER`** / **`APP_NAME`** / **`JWT_ALLOWED_ISSUERS`** (defaults align with **`core-service`**) — must match token issuance for **HS256**.
- **`REDIS_HOST`** (and password if needed) — required for events, presence, and blacklist in real deployments.
- **`REDIS_USE_STREAMS`** — default **`true`** (Streams + consumer group). Set **`false`** for Pub/Sub-only consumption.

See **`.env.example`** for variable names.

### 4.2 Local (developer machine)

```text
go run ./cmd
```

Or build from `./cmd` and run. Ensure Redis is reachable for full behaviour.

### 4.3 Docker

Build the image from the **`Dockerfile`**, pass the same environment variables, publish port **8080** (or your **`HTTP_PORT`**).

### 4.4 Docker Compose

The repo includes **`docker-compose.yml`**. Set **`ACCESS_TOKEN_SECRET`** (and JWT-related vars if not default) in **`.env`** before `docker compose up`.

### 4.5 Behind Traefik / reverse proxy

Expose **`/ws`** (and optionally **`/health`**, **`/metrics`**) to this service. Use **HTTPS** so clients use **WSS**.

---

## 5. Quick reference — WebSocket messages

### 5.1 Client → server (JSON text frames)

| `type` | Other fields | Effect |
|--------|----------------|--------|
| `ping` | — | Server replies with envelope **`pong`**. Refreshes presence if configured. |
| `subscribe.conversation` | `conversation_id` | Joins the conversation room on this connection (typing, room broadcasts). |
| `unsubscribe.conversation` | `conversation_id` | Leaves the conversation room. |
| `typing.start` | `conversation_id` | Broadcasts **`typing.start`** to others in that conversation room. |
| `typing.stop` | `conversation_id` | Broadcasts **`typing.stop`** to others in that conversation room. |
| `read.messages` | `conversation_id` | Server sends **`read.ack`** to this client only (UX hint); **persist read state via core REST API**. |

Malformed JSON is ignored. Unknown `type` values are ignored.

### 5.2 Server → client

**Envelope** (`type`, `payload`, `ts`) unless noted.

| `type` | Meaning (short) |
|--------|-----------------|
| `connected` | First message only; not in envelope form; includes `user_id`, `conn_id`. |
| `message.new` | New chat message for the recipient (`payload` includes `conversation_id`, `message_id`, `sender_id`, `preview`, `message_type`). |
| `read.receipts` | Read receipt for the peer (`payload` includes `conversation_id`, **`reader_id`** — the user who read). |
| `read.ack` | Server acknowledged `read.messages` (`payload` includes `conversation_id`, reminder to use REST for persistence). |
| `typing.start` / `typing.stop` | Someone is typing in a conversation (`payload` includes `conversation_id`, `user_id`). |
| `order.status_changed` | Order update from payment-related events (`payload` includes `order_id`, `status`, `amount_vnd`, etc., depending on event). |
| `feed.refresh` | Listing hint for the seller (`listing_id`, `seller_id`). |
| `pong` | Response to client `ping`. |

Core may extend Redis payloads over time; realtime forwards what it subscribes to and maps in its dispatcher.

---

## 6. If something does not work

- **`401` on `/ws`** — Missing token, invalid token, wrong issuer/secret, or token revoked (blacklist).
- **`503` on `/ws`** — Blacklist check failed (often Redis unavailable).
- **No chat events** — Check Redis connectivity; that core **publishes** events (Streams vs Pub/Sub must match **`REDIS_USE_STREAMS`**); JWT and **`notify_user_id`** / subscription expectations for typing and read receipts.
- **Health degraded** — Often Redis connection failure or wrong host/port/password.

For deployment details, see **`DEPLOY.md`**.

---

## 7. How to test chat integration (manual)

1. **Shared Redis** — Run core-service and realtime-service against the **same** Redis (same DB index if you use one).
2. **Matching JWT** — Set **`ACCESS_TOKEN_SECRET`** and issuer settings on realtime to match core (or RS256 public key).
3. **WebSocket** — Connect:  
   `ws://localhost:8080/ws?token=<JWT>&platform=web`  
   (use **WSS** in staging/production). Expect `{"type":"connected",...}`.
4. **New message** — Via core REST, send a chat message so the **other** user is the recipient. That user should receive **`message.new`** without subscribing to the conversation.
5. **Read receipt** — Call core’s **mark read** API. The **sender’s** session should receive **`read.receipts`** with **`reader_id`** set appropriately when core publishes **`notify_user_id`**.
6. **Redis-only smoke test (Pub/Sub mode)** — If realtime runs with **`REDIS_USE_STREAMS=false`**, from `redis-cli`:  
   `PUBLISH chat:message:new '{"conversation_id":"c1","message_id":"m1","sender_id":"s","recipient_id":"u1","preview":"x","message_type":"text"}'`  
   User `u1` should get **`message.new`**. Read receipts:  
   `PUBLISH chat:read:receipts '{"conversation_id":"c1","recipient_id":"reader","notify_user_id":"u1"}'`  
7. **Redis Streams (default)** — With **`REDIS_USE_STREAMS=true`**, core should **`XADD`** to the expected streams; realtime consumes via the consumer group. Testing without core is possible with **`XADD`** matching core’s field names (`type`, `payload`); prefer end-to-end tests with core for accuracy.

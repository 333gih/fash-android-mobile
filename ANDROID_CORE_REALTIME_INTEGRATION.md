# Fash Android (Kotlin) — Core Service, Realtime Service & Integration

> **Scope:** This document was produced by reviewing `../core-service`, `../realtime-service`, and **`payment-service`**. It consolidates **REST contracts**, **WebSocket contracts**, **core ↔ realtime integration**, **payment-service integration (§11)**, and **Android-focused notices** so a Kotlin app can integrate safely and consistently.  
> **Canonical REST detail:** `core-service/docs/API.md` (full request/response examples).  
> **Canonical realtime detail:** `realtime-service/INTEGRATION.md` and `core-service/docs/integration-realtime.md`.  
> **Payment detail:** §11 below + `payment-service` Swagger (`/swagger/index.html`).

---

## 1. Architecture (who does what)

| Layer | Responsibility | Protocol |
|--------|----------------|----------|
| **core-service** | Users, listings, orders, chat (persisted), search, auth, FCM registration; publishes order events to Redis; exposes **`POST .../orders/:id/payment-confirm`** for payment-service callbacks | **HTTPS REST** JSON |
| **payment-service** | Payment gateways (MoMo / ZaloPay / VNPay / TPBank), escrow state, audit; **calls core** to confirm paid orders | **HTTPS** — internal APIs + provider webhooks (see §11) |
| **realtime-service** | Push chat/listing/order **hints** to online clients; Redis presence for “in-app” detection | **WSS** WebSocket + **Redis** (from core) |

**Data flow (mental model):**

```
Android app
  │  Authorization: Bearer <access_token>     →  core-service  →  PostgreSQL
  │  GET /ws?token=…&platform=android         →  realtime-service  →  (same Redis as core)
  └─ FCM (when offline / no presence)         ←  core-service  (not realtime)
```

- **Writes** (send message, create order, pay, etc.) always go to **core-service** REST APIs.
- **Live UI updates** come from **realtime-service** WebSocket frames (fed by Redis events published by core).
- **Payment gateways:** **payment-service** integrates with banks/wallets and with **core** (see §11). The **Kotlin app must not** call payment-service internal routes or embed shared secrets; use **core** for checkout UX and any **`payment_url`** (or equivalent) the core API exposes.

---

## 2. Base URLs & configuration (Kotlin)

| Setting | Typical value | Notes |
|---------|----------------|--------|
| **Core API base** | `https://<host>/<APP_API_PREFIX>/v1` | Default `APP_API_PREFIX` is `api` → `/api/v1`. Confirm with deployment. |
| **Realtime base** | `wss://<realtime-host>/` | WebSocket path is `/ws` (see §5). |
| **Auth header** | `Authorization: Bearer <access_token>` | All **protected** core routes. |
| **Content-Type** | `application/json` | Except multipart uploads (`/users/me/avatar`, `/listings/images`, …). |
| **Error envelope** | `{ "error": "<message>" }` | May include machine-readable `code` in some handlers; treat `error` as user-visible fallback. |

**Kotlin (Retrofit example):**

```kotlin
interface FashApi {
    @GET("users/me")
    suspend fun me(@Header("Authorization") bearer: String): ProfileDto
}
// baseUrl = "https://api.example.com/api/v1/"  // trailing slash; paths without leading "api" if already in base
```

**Important:** If the API is mounted behind a reverse proxy with an extra prefix (e.g. `/core-service`), set Retrofit `baseUrl` to include that prefix; align with `APP_PUBLIC_PREFIX` / gateway routing in core.

---

## 3. core-service — REST API summary

Below: **path** is relative to `{APP_API_PREFIX}/v1` (e.g. `/api/v1`). **🔒** = requires Bearer token.

### 3.1 Authentication

| Method | Path | Body (JSON) | Success |
|--------|------|-------------|---------|
| POST | `/auth/login` | `email`, `password`, `application_id` (UUID), optional `ip_address`, `user_agent` | `access_token`, `refresh_token`, `token_type`, `expires_in`, `is_new_user`, `user_id`, `unread_count` |
| POST | `/auth/refresh` | `refresh_token`, `application_id`, … | Same as login |
| POST | `/auth/social-login` | `provider`, `provider_token`, `application_id`, … | Same as login |
| POST | `/auth/otp/request` | `email`, `application_id` | `{ "ok": true }` |
| POST | `/auth/otp/verify` | `email`, `otp` (6 chars), `application_id` | Same as login |
| POST | `/auth/logout` 🔒 | — | `{ "message": "logged out" }` |
| POST | `/auth/logout-all` 🔒 | — | `{ "message": "all sessions logged out" }` |
| POST | `/auth/fcm/register` 🔒 | `fcm_token`, `device_platform` (e.g. `android`) | `{ "ok": true }` |

**Android notices:**

- Store **`refresh_token`** securely (EncryptedSharedPreferences / Keystore).
- On **`is_new_user: true`**, route to onboarding before posting listings.
- Re-register **FCM** on token refresh (`onNewToken`) via `/auth/fcm/register`.

---

### 3.2 Users & profiles

| Method | Path | Notes |
|--------|------|--------|
| POST | `/users/onboard` 🔒 | `username`, `aesthetic_tags` — required before selling |
| GET | `/users/me` 🔒 | Own profile; **404** if not onboarded |
| PATCH | `/users/me` 🔒 | Partial update; `aesthetic_tags` replaces when sent |
| POST | `/users/me/avatar` 🔒 | `multipart/form-data` field `file` — max 5 MB |
| POST | `/users/me/cover` 🔒 | `file` — max 10 MB |
| GET | `/users/:id` | **`:id` is username** (public profile) |
| GET | `/users/suggested-username` | Query `phone` |
| POST | `/users/:id/follow` 🔒 | |
| DELETE | `/users/:id/follow` 🔒 | |
| POST | `/users/:id/block` 🔒 | |
| DELETE | `/users/:id/block` 🔒 | |
| GET | `/users/search` 🔒 | Query `q`, `limit` |
| GET | `/aesthetic-tags` | Public master list |

**Profile object** (response): includes `id`, `user_id`, `username`, `display_name`, `bio`, `avatar_url`, `cover_url`, counts, `onboarding_done`, `verified`, `aesthetic_tags`, `created_at`. Image URLs are usually **presigned** — refresh if expired.

---

### 3.3 Listings

| Method | Path | Notes |
|--------|------|--------|
| POST | `/listings/images` 🔒 | Multipart image → `{ "image_url" }` |
| POST | `/listings` 🔒 | Create; requires onboarded profile |
| PUT | `/listings/:listing_id` 🔒 | Seller only; `active` only; price change expires pending offers |
| DELETE | `/listings/:listing_id` 🔒 | Soft delete only if **no orders** and **no conversations** |
| POST | `/listings/:listing_id/sold` 🔒 | Mark sold outside platform |
| POST | `/listings/:listing_id/like` 🔒 | Toggle → `{ "liked": bool }` |
| POST | `/listings/:listing_id/save` 🔒 | Toggle wishlist → `{ "saved": bool }` |
| POST | `/listings/:listing_id/view` 🔒 | Debounced view |
| GET | `/listings/:listing_id` | Public single listing |
| GET | `/listings/home` 🔒 | Followed sellers feed |
| GET | `/search/listings` 🔒 | Explore tab (browse + search); Android uses this instead of `/listings/explore` |
| GET | `/listings/wishlist` 🔒 | `{ "listing_ids": [...] }` |
| GET | `/users/:id/listings` | Query `status`, `limit`, `offset` |
| GET | `/categories` | Public |

---

### 3.4 Orders

| Method | Path | Notes |
|--------|------|--------|
| POST | `/orders` 🔒 | `listing_id`, `amount_vnd` — creates order; listing → `reserved` |
| GET | `/orders` 🔒 | **Required** query `role=buyer|seller`; optional `status`, pagination |
| GET | `/orders/:order_id` 🔒 | Buyer or seller |
| POST | `/orders/ship` 🔒 | Seller: `order_id`, `tracking_number`, `carrier` |
| POST | `/orders/:order_id/confirm` 🔒 | Buyer confirms delivery |
| POST | `/orders/review` 🔒 | Buyer review after delivered |
| POST | `/orders/dispute` 🔒 | Open dispute |
| POST | `/orders/dispute/evidence` 🔒 | Add evidence |
| POST | `/orders/:order_id/payment-confirm` | **Server-to-server webhook** (payment provider) — **not** for mobile |

**Order statuses (REST):** include `payment_pending`, `payment_held`, `in_transit`, `delivered_confirmed`, `cancelled`, `disputed` — see `API.md` lifecycle.

---

### 3.5 Chat

| Method | Path | Notes |
|--------|------|--------|
| POST | `/chat/conversations` 🔒 | `listing_id` — buyer starts thread |
| GET | `/chat/conversations` 🔒 | Inbox; optional `group_by=listing` for seller views (if supported by route) |
| POST | `/chat/conversations/:conversation_id/read` 🔒 | Mark read — triggers Redis → realtime read receipts |
| GET | `/chat/conversations/:conversation_id/messages` 🔒 | Newest first; pagination |
| POST | `/chat/messages` 🔒 | `conversation_id`, `content` |
| DELETE | `/chat/messages/:message_id` 🔒 | Within 5 minutes; own messages only |
| POST | `/chat/offers` 🔒 | Buyer offer `amount_vnd` |
| POST | `/chat/offers/accept` 🔒 | Seller — creates order |
| POST | `/chat/offers/decline` 🔒 | Seller |
| GET | `/chat/unread` 🔒 | `{ "unread_count": n }` |

**Message types:** `text`, `offer`, `system`. **Offer status:** `pending`, `accepted`, `declined`, `expired`, `cancelled`.

---

### 3.6 Search

All 🔒. `GET /search/listings`, `GET /search/autocomplete`, `GET /search/trending-tags` — query params per `API.md`.

---

### 3.7 Health (no auth)

- `GET /health` — liveness (`ok` text on core; see core routes).
- `GET /api/v1/health-check/database` etc. — DB/Slack checks.

---

## 4. realtime-service — HTTP surface

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/health` | JSON liveness; may include Redis status, WS connection count |
| GET | `/metrics` | Prometheus — **not** for apps |
| GET | `/ws` | **WebSocket upgrade** — main realtime API |
| GET | `/swagger/*` | Swagger UI (dev/staging) |

**No** REST CRUD for business entities on realtime.

---

## 5. WebSocket — connection & security (Android)

### 5.1 URL

```
wss://<realtime-host>/ws?token=<URL_ENCODED_ACCESS_TOKEN>&platform=android
```

- **Token in query:** many clients cannot set `Authorization` on WS handshake; pass JWT as **`token`**.
- **URL-encode** the token (RFC 3986).
- **401** before upgrade: invalid/expired JWT, wrong issuer/secret, or **revoked** token (Redis blacklist).
- **503:** blacklist check failed (often Redis down).
- Use **WSS** in production; never log full URL (contains secret).

### 5.2 First server message (`connected`)

Not wrapped in the usual `{ type, payload, ts }` envelope:

```json
{
  "type": "connected",
  "user_id": "<uuid>",
  "conn_id": "<id>"
}
```

### 5.3 Subsequent server messages (envelope)

```json
{
  "type": "<event_name>",
  "payload": { },
  "ts": 1700000000000
}
```

`ts` is Unix time in **milliseconds**.

### 5.4 Client → server (JSON text frames)

| `type` | Fields | Purpose |
|--------|--------|---------|
| `ping` | — | Heartbeat; server responds with `pong`; refreshes **presence** |
| `subscribe.conversation` | `conversation_id` | Join conversation room (typing, some broadcasts) |
| `unsubscribe.conversation` | `conversation_id` | Leave |
| `subscribe.listing` | `listing_id` | Listing detail lifecycle frames |
| `unsubscribe.listing` | `listing_id` | Leave |
| `typing.start` | `conversation_id` | Typing indicator to peer |
| `typing.stop` | `conversation_id` | Stop typing |
| `read.messages` | `conversation_id` | Optional UX ack; **persist read state via core REST** |

Malformed JSON / unknown types are typically ignored server-side.

---

## 6. Server → client WebSocket events (Kotlin `when`)

Map **`type`** string to handlers. Implement at least:

| `type` | Meaning / action |
|--------|-------------------|
| `message.new` | New chat message for recipient — update thread + badge if screen not open |
| `read.receipts` | Peer read messages — payload includes **`reader_id`** (see §7 naming) |
| `offer.limit_reset` | Seller changed price — reset local offer limits / show banner |
| `listing.reserved` | Listing no longer freely available for other buyers |
| `listing.available` | Listing buyable again (e.g. payment expired) |
| `listing.sold` | Terminal sold state |
| `order.status_changed` | Order/payment lifecycle hint — **see §7** (status strings differ from REST) |
| `feed.refresh` | Hint to refresh home/explore for seller/followers |
| `typing.start` / `typing.stop` | Typing UI |
| `read.ack` | Ack for client `read.messages` (if enabled) |
| `pong` | Response to `ping` |

**Idempotency:** `listing.*` may arrive **more than once** (multiple routing paths). Deduplicate by `(listing_id, status)`.

---

## 7. Core ↔ Realtime integration (Redis)

### 7.1 Shared infrastructure

- **Same Redis** as core for: event streams (or Pub/Sub), JWT **jti** blacklist, **`presence:user:{user_id}`**.
- **JWT:** `ACCESS_TOKEN_SECRET` (HS256) or RS256 public key on realtime must match **core** token issuance. **`iss`** claim must match **`JWT_ISSUER`** / **`JWT_ALLOWED_ISSUERS`** on realtime (defaults align with core **`APP_NAME`** — verify in deployment logs).
- **`REDIS_USE_STREAMS`:** default **`true`** on realtime — must match how **core publishes** (Streams with `XADD` vs legacy `PUBLISH`).

### 7.2 Streams (typical) — core publishes, realtime consumes

| Stream key | Event `type` | WS types emitted |
|------------|--------------|------------------|
| `stream:chat:messages` | `message.new` | `message.new` (skipped if payload `is_closed: true`) |
| `stream:chat:read` | `read.receipts` | `read.receipts` |
| `stream:feed:listings:created` | `listing.created` | `feed.refresh` |
| `stream:listing:offer` | `offer.limit_reset` | `offer.limit_reset` |
| `stream:listing:status` | `listing.status_changed` | `listing.reserved` \| `listing.available` \| `listing.sold` |
| `stream:payment:orders:created` | `order.created` | `order.status_changed` |
| `stream:payment:orders:confirmed` | `order.confirmed` | `order.status_changed` |

**Do not** consume `stream:notifications:push` in realtime — that stream is for core’s **async FCM worker**.

### 7.3 Read receipts — field naming (important for Kotlin models)

- Core/redis payload uses **`recipient_id`** / **`notify_user_id`** semantics in docs.
- **realtime-service** WebSocket **`read.receipts`** payload uses:
  - `conversation_id`
  - **`reader_id`** — the user who performed the read (mapped from core payload)

Use one **`ReadReceiptsPayload`** data class with **`reader_id`** to match the wire format.

### 7.4 `order.status_changed` — WebSocket vs REST

realtime **dispatcher** maps payment events to simplified statuses:

| Source | `order.status_changed` payload `status` (approx.) |
|--------|-----------------------------------------------------|
| Order created (payment pending) | `awaiting_payment` |
| Order confirmed (delivery / auto-release path in dispatcher) | `released_to_seller` |

These strings are **not identical** to REST `Order.status` enum names. Android should:

- Use WebSocket events for **badges / banners / “refresh order detail”**.
- After any WS hint, **re-fetch** `GET /orders/:id` for authoritative state before irreversible UI (e.g. dispute actions).

### 7.5 Presence & FCM

- Realtime sets **`presence:user:{user_id}`** with TTL (e.g. **300s**; refreshed on `ping` / pong).
- Core **skips FCM** when presence exists (user considered in-app).
- App must send **`ping`** ~every **30s** (or per `WS_PING_INTERVAL`) so users don’t get duplicate tray notifications while chatting.

See `core-service/docs/android-notifications.md` and `presence-fcm-contract.md`.

---

## 8. Kotlin / Android checklist

| Area | Recommendation |
|------|----------------|
| **HTTP** | Retrofit + OkHttp; coroutines; centralized 401 → refresh token → retry once |
| **WS** | OkHttp `WebSocket` or Scarlet; **reconnect** with exponential backoff (cap ~30s) |
| **JSON** | kotlinx.serialization or Moshi; **snake_case** field names match API |
| **Threads** | WS callbacks on OkHttp thread — `Dispatchers.Main` for UI updates |
| **Proguard** | Keep DTOs used by reflection if applicable |
| **TLS** | Certificate pinning optional; always WSS in prod |
| **Deep links** | FCM `data` payload: prefer `conversation_id`, `order_id`, `listing_id` when server adds them |

### 8.1 Suggested polling fallback (WS down)

| Screen | Endpoint | Interval (suggested) |
|--------|----------|----------------------|
| Inbox | `GET /chat/conversations`, `GET /chat/unread` | ~10s |
| Chat | `GET /chat/conversations/:id`, `GET .../messages` | ~3s |

Stop polling when WebSocket reconnects.

### 8.2 Money display

Amounts are **VND integers** (`Long`). Format with **`vi_VN`** locale.

---

## 9. Common HTTP error codes (core)

| HTTP | Typical meaning |
|------|-----------------|
| 400 | Validation / bad input |
| 401 | Missing/invalid token |
| 403 | Forbidden (blocked, wrong role) |
| 404 | Not found |
| 409 | Conflict — `USERNAME_TAKEN`, `LISTING_NOT_AVAILABLE`, `PENDING_OFFER_EXISTS`, `DISPUTE_ALREADY_OPEN`, etc. |
| 429 | Rate limit |
| 503 | Redis unavailable (JWT blacklist) — rare |

Parse **`error`** string; optionally map **`code`** if present in JSON extensions.

---

## 10. Related documents (read in order)

1. `core-service/docs/API.md` — **full** REST reference  
2. `core-service/docs/integration-realtime.md` — product contract + Android snippets  
3. `realtime-service/INTEGRATION.md` — operational truth for realtime (env, streams, WS)  
4. `core-service/docs/android-notifications.md` — FCM + presence  
5. `core-service/docs/presence-fcm-contract.md` — Redis keys & suppression rules  
6. `payment-service/docs` (Swagger: `/swagger/index.html` on the payment service) — internal + webhook contracts  

---

## 11. payment-service — integration (core, gateways, Android)

This section describes **`payment-service`** in the same repo layout as this document (`payment-service/`). It is the **checkout / escrow** microservice behind the gateways.

### 11.1 Roles in the payment flow

| Direction | What happens |
|-----------|----------------|
| **core → Redis** | After `POST /orders`, core publishes **`order.created`** on Redis (see `core-service` `OrderEventPublisher`). Downstream consumers (payment workers) can use this to start checkout. |
| **core → payment-service** (server-to-server) | **`POST /api/v1/internal/payments/initiate`** (and the same under **`/api/v2/...`**) with header **`X-Internal-Secret`** — **not for mobile**. Core (or an orchestrator) supplies order amounts, buyer/seller IDs, `payment_method`, and `redirect_url`. |
| **payment-service → provider** | User is redirected or app opens WebView to **`payment_url`** returned by **initiate** (MoMo / ZaloPay / VNPay / TPBank as configured). |
| **provider → payment-service** | **`POST /api/webhooks/:provider`** — asynchronous IPN/callbacks (e.g. `momo`, `vnpay`, `tpbank`). **Not** called by the Android app. |
| **payment-service → core** | After a successful payment, **`POST {CORE_SERVICE_BASE_URL}/api/v1/orders/{order_id}/payment-confirm`** with header **`X-Payment-Signature`** = `hex(HMAC-SHA256(PAYMENT_WEBHOOK_SECRET, order_id))` — matches **`core-service/docs/API.md`** webhook row. Idempotent on core. |

**Android rule:** the app talks only to **core** for “I’m checking out” and order state. It **never** sends `X-Internal-Secret` and never calls **`/api/.../internal/...`** on the payment host. If your product needs the raw **gateway URL**, expose it via **core** (e.g. order detail or a dedicated checkout endpoint) so secrets stay server-side.

---

### 11.2 Base path (payment-service HTTP)

Default env: **`API_PREFIX=api`**, **`API_VERSION_V1=v1`**, **`API_VERSION_V2=v2`**.

| Area | Full path pattern (default) |
|------|------------------------------|
| Internal (secret) | `/api/v1/internal/...` and `/api/v2/internal/...` |
| Provider webhooks | `/api/webhooks/:provider` |
| Liveness | `GET /health` |
| Docs | `GET /swagger/*` |

---

### 11.3 Internal API (core → payment-service only)

All routes below require **`X-Internal-Secret`** (same value as core’s **`PAYMENT_SERVICE_INTERNAL_SECRET`** / payment-service **`INTERNAL_SECRET`**). Responses use JSON; errors typically `{ "error": "..." }`.

#### `POST .../internal/payments/initiate`

**Headers**

| Header | Required | Notes |
|--------|----------|--------|
| `X-Internal-Secret` | Yes | Shared secret |
| `X-Idempotency-Key` | Yes | **UUID v4** — duplicate keys return the same logical result |

**Body (JSON)**

| Field | Type | Notes |
|-------|------|--------|
| `order_id` | string (UUID) | Core order id |
| `buyer_id` | string (UUID) | |
| `seller_id` | string (UUID) | |
| `amount_vnd` | int64 | ≥ 1000 |
| `platform_fee_vnd` | int64 | ≥ 0 |
| `seller_payout_vnd` | int64 | ≥ 1 |
| `payment_method` | string | e.g. `momo`, `zalopay`, `vnpay`, `tpbank` (must be enabled in payment-service env) |
| `redirect_url` | string (URL) | Return URL after wallet flow |
| `order_info` | string | Optional display / reference |

**Response `200` (success)**

| Field | Notes |
|-------|--------|
| `payment_url` | Where to send the user (WebView / external browser) |
| `transaction_id` | Internal transaction id |
| `provider_ref` | Provider reference when available |
| `expires_at` | RFC3339 time |
| `provider_degraded` | Optional bool |
| `error` | Present on some degraded paths |

#### `GET .../internal/payments/:order_id/status`

Returns escrow / payment status for an order (e.g. `escrow_status`, `amount_vnd`, `paid_at`).

#### `GET .../internal/payments/:order_id/audit`

Audit trail for support / ops.

#### Escrow & disputes (internal)

| Method | Path | Purpose |
|--------|------|---------|
| POST | `.../internal/escrow/:order_id/release` | Release escrow (body: `order_id`, `actor_id`, `reason` — see Swagger) |
| POST | `.../internal/escrow/:order_id/refund` | Refund path |
| POST | `.../internal/escrow/:order_id/set-shipment` | Shipment metadata for escrow rules |
| POST | `.../internal/disputes/:dispute_id/evidence` | Dispute evidence |
| POST | `.../internal/webhooks/:webhook_log_id/replay` | Admin replay (protected workflow) |

---

### 11.4 Provider webhooks (gateway → payment-service)

- **`POST /api/webhooks/:provider`** — **`provider`** matches the configured gateway (e.g. `momo`, `vnpay`, `tpbank`).
- Signature / header rules are **provider-specific** (see `payment-service` `.env.example` and provider packages).
- Used for **server-to-server** confirmation; the mobile app does not post here.

---

### 11.5 Realtime / UI alignment

- After payment succeeds, core moves the order toward **`payment_held`**; **realtime** may emit **`order.status_changed`** (see §7.4 — WS `status` strings are simplified).
- **`GET /orders/:order_id`** on **core** remains the source of truth for order status after checkout.

---

### 11.6 Kotlin / Android checklist (payment)

| Do | Don’t |
|----|--------|
| Use **core** REST for orders and checkout entry points your backend exposes | Call **`/api/v1/internal/*`** on payment-service |
| Open **`payment_url`** only from **trusted** data returned by **core** (or in-app browser with same origin policy as product) | Ship **`INTERNAL_SECRET`**, **`PAYMENT_WEBHOOK_SECRET`**, or provider keys in the APK |
| Handle **`order.status_changed`** on WebSocket; then **refresh** order from core | Assume WS status strings match DB enums 1:1 (see §7.4) |
| Use **deep links** for return from wallet apps if core defines `redirect_url` | Post to **`/orders/.../payment-confirm`** — that is **payment-service → core** only |

---

## 12. Disclaimer

- **Route registration** in core uses module paths under `auth-service` in source; deployed paths follow **`AppApiPrefix` + `/v1`** as in `routes.go`. Always verify **staging OpenAPI/Swagger** if `SWAGGER_ENABLE` allows.  
- **Legacy docs** (`realtime-service-integration.md`) may describe older Pub/Sub names; prefer **Streams** + `INTEGRATION.md` when `REDIS_USE_STREAMS=true`.  
- **Payment mobile flows** should follow **core** order/checkout APIs; **`payment-service`** internal and webhook routes are **backend-to-backend** (§11).
- **Core** may evolve to return **`payment_url`** on order creation or a follow-up endpoint — confirm the live **core** OpenAPI for your release.

---

*Generated for Kotlin Android integration; align each release with the deployed `APP_API_PREFIX`, JWT issuer, Redis mode, and payment-service **`API_PREFIX`** / internal secrets in your environment.*

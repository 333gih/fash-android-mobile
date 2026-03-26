# Realtime Service — Complete Integration Contract

> **Audience:** This document is the single source of truth for three teams:
> 1. **Core-service** (already implemented — confirms what it publishes)
> 2. **Realtime-service** (what to consume and forward)
> 3. **Android app** (what WebSocket frames to expect and how to react)

---

## Architecture Overview

```
Android App
    │  WebSocket  (wss://{host}/ws?token=...&platform=android)
    ▼
Realtime Service
    │  Redis Streams (XREADGROUP)   │  Redis Presence (SET/DEL)
    ▼                               ▼
Core Service ──── PostgreSQL       Redis
  (REST API)
```

- **Core-service** writes to PostgreSQL and publishes events to Redis Streams.
- **Realtime-service** consumes streams, maintains WebSocket connections, and manages presence keys.
- **Android** connects via WebSocket (JWT in query string), subscribes to rooms, and receives push frames.

---

## Part 1 — Core-Service: What It Publishes

### 1.1 Redis Streams Reference

All events use the XADD format with two required string fields:

| Field | Value |
|---|---|
| `type` | Event type string (see table below) |
| `payload` | JSON-encoded payload object |

| Stream Key | `type` value | Published by | When |
|---|---|---|---|
| `stream:chat:messages` | `message.new` | SendMessage / SendOffer usecases | Any user sends a text or offer |
| `stream:chat:read` | `read.receipts` | MarkRead usecase | User opens conversation and marks read |
| `stream:feed:listings:created` | `listing.created` | CreateListing usecase | Seller publishes new listing |
| `stream:listing:offer` | `offer.limit_reset` | UpdateListing usecase (price change) | Seller changes listing price |
| `stream:listing:status` | `listing.status_changed` | OrderPaymentConfirm / OrderConfirm / OrderAutoRelease / OrderPaymentExpiry | Listing transitions reserved ↔ active ↔ sold |
| `stream:payment:orders:created` | `order.created` | CreateOrder usecase | Buyer initiates checkout |
| `stream:payment:orders:confirmed` | `order.confirmed` | ConfirmOrder / OrderAutoRelease usecases | Delivery confirmed |

---

### 1.2 Payload Schemas

#### `message.new` → `stream:chat:messages`

```json
{
  "conversation_id": "uuid",
  "message_id":      "uuid",
  "sender_id":       "uuid",
  "recipient_id":    "uuid",
  "preview":         "Is this still available?",
  "message_type":    "text | offer | system",
  "is_closed":       false
}
```

> **`is_closed: true`** — the conversation is in read-only state. Realtime **must not** deliver this event via WebSocket or offline push. State change is handled by `listing.status_changed`.

---

#### `read.receipts` → `stream:chat:read`

```json
{
  "conversation_id": "uuid",
  "recipient_id":    "uuid",
  "notify_user_id":  "uuid"
}
```

`notify_user_id` is the peer to deliver the `read.receipts` WebSocket frame to.

---

#### `listing.created` → `stream:feed:listings:created`

```json
{
  "listing_id": "uuid",
  "seller_id":  "uuid"
}
```

---

#### `offer.limit_reset` → `stream:listing:offer`

Published when the seller updates the listing price. Resets all buyer offer counters.

```json
{
  "listing_id":   "uuid",
  "new_price":    130000,
  "affected_conversations": [
    {
      "conversation_id": "uuid",
      "buyer_id":        "uuid",
      "seller_id":       "uuid"
    }
  ]
}
```

---

#### `listing.status_changed` → `stream:listing:status`

Published on every listing status transition (reserved / active / sold).

```json
{
  "listing_id":        "uuid",
  "status":            "reserved | active | sold",
  "online_user_ids":   ["uuid-buyer1", "uuid-buyer2", "uuid-seller"],
  "conversation_ids":  ["uuid-conv1", "uuid-conv2"]
}
```

> `online_user_ids` contains **all** buyer + seller IDs from affected conversations. Realtime intersects with its own presence set to find who is actually online before delivery.

**Status lifecycle:**

| Trigger | Status published | Side effects (already done by core-service) |
|---|---|---|
| Payment confirmed (`payment_held`) | `reserved` | Other conversations closed; system msg inserted |
| Buyer confirms receipt / auto-release | `sold` | System msg "Sản phẩm đã được bán thành công." inserted |
| Payment expiry (15 min timer) | `active` | Conversations reopened; system msg "Sản phẩm này hiện đã có thể mua lại." inserted |

---

#### `order.created` → `stream:payment:orders:created`

```json
{
  "order_id":          "uuid",
  "buyer_id":          "uuid",
  "seller_id":         "uuid",
  "listing_id":        "uuid",
  "amount_vnd":        130000,
  "platform_fee_vnd":  13000,
  "seller_payout_vnd": 117000
}
```

---

#### `order.confirmed` → `stream:payment:orders:confirmed`

```json
{
  "order_id":     "uuid",
  "buyer_id":     "uuid",
  "seller_id":    "uuid",
  "amount_vnd":   130000,
  "auto_release": false
}
```

---

## Part 2 — Realtime Service: Contract & Requirements

### 2.1 WebSocket Endpoint

```
GET wss://{host}/ws?token={access_token}&platform={android|ios|web}
```

- Validate JWT: same `ACCESS_TOKEN_SECRET`, `JWT_ISSUER`, and algorithm (HS256 or RS256) as core-service.
- Check JWT blacklist in Redis (key: `blacklist:{jti}`) — reject if key exists.
- On success: send `connected` frame immediately.
- Set presence key: `SET presence:user:{user_id} 1 EX 300` (refresh on each ping).

---

### 2.2 Redis Consumer Group Setup

Run once on startup per stream. Use `MKSTREAM` so the stream is created if it does not exist:

```bash
XGROUP CREATE stream:chat:messages       realtime-group $ MKSTREAM
XGROUP CREATE stream:chat:read           realtime-group $ MKSTREAM
XGROUP CREATE stream:feed:listings:created realtime-group $ MKSTREAM
XGROUP CREATE stream:listing:offer       realtime-group $ MKSTREAM
XGROUP CREATE stream:listing:status      realtime-group $ MKSTREAM
XGROUP CREATE stream:payment:orders:created  realtime-group $ MKSTREAM
XGROUP CREATE stream:payment:orders:confirmed realtime-group $ MKSTREAM
```

Use `XREADGROUP GROUP realtime-group {consumer-id} COUNT 100 BLOCK 2000 STREAMS ...` in a loop.  
ACK each message with `XACK` after successful delivery or confirmed non-delivery.

---

### 2.3 Stream Dispatch Table

| Stream | `type` | Action |
|---|---|---|
| `stream:chat:messages` | `message.new` | If `is_closed == true` → skip. Otherwise forward `message.new` WS frame to `recipient_id` |
| `stream:chat:read` | `read.receipts` | Forward `read.receipts` WS frame to `notify_user_id` |
| `stream:feed:listings:created` | `listing.created` | Forward `feed.refresh` WS frame to all followers of `seller_id` (query follower list from Redis cache or accept from payload extension) |
| `stream:listing:offer` | `offer.limit_reset` | For each entry in `affected_conversations`: send `offer.limit_reset` WS frame to `buyer_id` and `seller_id` |
| `stream:listing:status` | `listing.status_changed` | Map status → WS type (see §2.4). Deliver to: each user in `online_user_ids` (intersect presence), each room `conv:{id}` in `conversation_ids`, and listing room `listing:{listing_id}` |
| `stream:payment:orders:created` | `order.created` | Send `order.status_changed` WS frame to `buyer_id` and `seller_id` |
| `stream:payment:orders:confirmed` | `order.confirmed` | Send `order.status_changed` WS frame to `buyer_id` and `seller_id` |

---

### 2.4 Listing Status → WebSocket Type Mapping

| `status` value (case-insensitive) | WS `type` sent to client |
|---|---|
| `reserved` | `listing.reserved` |
| `active` | `listing.available` |
| `sold` | `listing.sold` |

---

### 2.5 Client → Server WebSocket Frames

All frames are JSON text.

| Client `type` | Required fields | Description |
|---|---|---|
| `ping` | — | Heartbeat. Server responds `pong` and refreshes presence TTL |
| `subscribe.conversation` | `conversation_id` | Join room `conv:{id}` for message delivery |
| `unsubscribe.conversation` | `conversation_id` | Leave room |
| `subscribe.listing` | `listing_id` | Join listing room `listing:{id}` for status updates |
| `unsubscribe.listing` | `listing_id` | Leave listing room |
| `typing.start` | `conversation_id` | Broadcast `typing.start` to other participant |
| `typing.stop` | `conversation_id` | Broadcast `typing.stop` to other participant |
| `read.messages` | `conversation_id` | Mark messages read (realtime signals core via REST or internal call) |

---

### 2.6 Server → Client WebSocket Frames

All frames (except `connected`) use the envelope:

```json
{ "type": "...", "payload": { ... }, "ts": 1700000000000 }
```

#### `connected` (first frame on open, no envelope)

```json
{ "type": "connected", "user_id": "uuid" }
```

#### `message.new`

```json
{
  "type": "message.new",
  "payload": {
    "conversation_id": "uuid",
    "message_id":      "uuid",
    "sender_id":       "uuid",
    "preview":         "string",
    "message_type":    "text | offer | system"
  },
  "ts": 1700000000000
}
```

#### `read.receipts`

```json
{
  "type": "read.receipts",
  "payload": { "conversation_id": "uuid", "read_by": "uuid" },
  "ts": 1700000000000
}
```

#### `offer.limit_reset`

```json
{
  "type": "offer.limit_reset",
  "payload": {
    "listing_id":      "uuid",
    "new_price":       130000,
    "conversation_id": "uuid"
  },
  "ts": 1700000000000
}
```

#### `listing.reserved`

```json
{
  "type": "listing.reserved",
  "payload": { "listing_id": "uuid", "status": "reserved" },
  "ts": 1700000000000
}
```

#### `listing.available`

```json
{
  "type": "listing.available",
  "payload": { "listing_id": "uuid", "status": "active" },
  "ts": 1700000000000
}
```

#### `listing.sold`

```json
{
  "type": "listing.sold",
  "payload": { "listing_id": "uuid", "status": "sold" },
  "ts": 1700000000000
}
```

#### `order.status_changed`

```json
{
  "type": "order.status_changed",
  "payload": {
    "order_id":  "uuid",
    "status":    "payment_pending | payment_held | in_transit | delivered_confirmed | cancelled",
    "amount_vnd": 130000
  },
  "ts": 1700000000000
}
```

#### `feed.refresh`

```json
{
  "type": "feed.refresh",
  "payload": { "seller_id": "uuid", "listing_id": "uuid" },
  "ts": 1700000000000
}
```

#### `typing.start` / `typing.stop`

```json
{
  "type": "typing.start",
  "payload": { "conversation_id": "uuid", "user_id": "uuid" },
  "ts": 1700000000000
}
```

#### `pong`

```json
{ "type": "pong" }
```

---

### 2.7 Presence Protocol

| Key pattern | Set by | Expires |
|---|---|---|
| `presence:user:{user_id}` | Realtime service | 300 seconds (refreshed on each `ping` frame) |

Core-service reads this key in `SendMessageUsecase` to skip FCM if the recipient has an active WebSocket.

---

### 2.8 Environment Variables (Realtime Service)

```env
# Redis — must match core-service
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password
REDIS_DB=0
REDIS_USE_STREAMS=true           # default true; false = legacy Pub/Sub mode

# JWT — must match core-service token issuer
ACCESS_TOKEN_SECRET=your_jwt_secret   # for HS256
JWT_ISSUER=fash-app                   # must match APP_NAME in core-service
JWT_ALLOWED_ISSUERS=fash-app          # comma-separated if multiple

# WebSocket
WS_PORT=8080
WS_PING_INTERVAL=30s
WS_PRESENCE_TTL=300

# Consumer group
STREAM_CONSUMER_GROUP=realtime-group
STREAM_CONSUMER_ID=realtime-1         # unique per pod/replica
```

> **Docker warning:** If `JWT_ISSUER` contains `${APP_ENV}`, Docker Compose will NOT expand it automatically. Set the final resolved value or add `$$` escaping. Example:
> ```yaml
> environment:
>   JWT_ISSUER: "fash-app"   # NOT "${APP_ENV}-app"
> ```

---

## Part 3 — Android App: Integration Guide

### 3.1 WebSocket Connection

```kotlin
val wsUrl = "wss://${host}/ws?token=${accessToken}&platform=android"
val request = Request.Builder().url(wsUrl).build()
val ws = okHttpClient.newWebSocket(request, listener)
```

**On `connected` frame:** store `user_id`, update connection state.  
**On close / error:** reconnect with exponential backoff (500ms → 1s → 2s → 4s → 8s, max 30s).  
**Send `ping` every 30 seconds** to maintain presence and prevent proxy timeouts.

---

### 3.2 Subscribe / Unsubscribe Rooms

```kotlin
// When entering a chat conversation screen
ws.send("""{"type":"subscribe.conversation","conversation_id":"$convId"}""")

// When entering a listing detail screen
ws.send("""{"type":"subscribe.listing","listing_id":"$listingId"}""")

// When leaving
ws.send("""{"type":"unsubscribe.conversation","conversation_id":"$convId"}""")
ws.send("""{"type":"unsubscribe.listing","listing_id":"$listingId"}""")
```

---

### 3.3 Incoming Frame Dispatcher

```kotlin
override fun onMessage(webSocket: WebSocket, text: String) {
    val frame = gson.fromJson(text, WsFrame::class.java)
    when (frame.type) {
        "message.new"       -> handleNewMessage(frame.payload)
        "read.receipts"     -> handleReadReceipts(frame.payload)
        "offer.limit_reset" -> handleOfferLimitReset(frame.payload)
        "listing.reserved"  -> handleListingStatusChange(frame.payload, "reserved")
        "listing.available" -> handleListingStatusChange(frame.payload, "active")
        "listing.sold"      -> handleListingStatusChange(frame.payload, "sold")
        "order.status_changed" -> handleOrderStatusChanged(frame.payload)
        "feed.refresh"      -> handleFeedRefresh(frame.payload)
        "typing.start"      -> handleTypingIndicator(frame.payload, typing = true)
        "typing.stop"       -> handleTypingIndicator(frame.payload, typing = false)
        "pong"              -> { /* heartbeat ack — no action needed */ }
    }
}
```

---

### 3.4 Handler: `message.new`

```kotlin
fun handleNewMessage(payload: MessageNewPayload) {
    // Append to the local message list for this conversation
    messageViewModel.appendMessage(payload.conversationId, payload)
    // Update conversation list preview
    conversationViewModel.updatePreview(payload.conversationId, payload.preview)
    // Increment unread badge if the screen is not currently open
    if (!isConversationScreenOpen(payload.conversationId)) {
        badgeViewModel.incrementUnread()
    }
}
```

---

### 3.5 Handler: `offer.limit_reset`

```kotlin
fun handleOfferLimitReset(payload: OfferLimitResetPayload) {
    // Re-enable the offer button and reset the local counter
    chatViewModel.onOfferLimitReset(payload.conversationId, payload.newPrice)
    // Show a snackbar/banner in the chat screen
    showBanner("Giá sản phẩm đã thay đổi — ${formatVND(payload.newPrice)}")
}
```

---

### 3.6 Handler: `listing.reserved` / `listing.available` / `listing.sold`

> These frames are idempotent on (`listing_id`, `status`). Process them even if you receive duplicates (the client may be in multiple delivery audiences simultaneously).

```kotlin
fun handleListingStatusChange(payload: ListingStatusPayload, status: String) {
    when (status) {
        "reserved" -> {
            // Disable chat input and offer button for ALL open conversations about this listing
            chatViewModel.markConversationClosed(payload.listingId)
            showBanner("Sản phẩm này đã được đặt mua bởi người khác")
            // Refresh listing detail if the user is viewing it
            listingDetailViewModel.refreshStatus(payload.listingId, "reserved")
        }
        "active" -> {
            // Re-enable chat input and offer button
            chatViewModel.markConversationOpen(payload.listingId)
            showBanner("Sản phẩm này hiện đã có thể mua lại")
            listingDetailViewModel.refreshStatus(payload.listingId, "active")
        }
        "sold" -> {
            // Keep input disabled; update the final state label
            chatViewModel.markListingSold(payload.listingId)
            listingDetailViewModel.refreshStatus(payload.listingId, "sold")
        }
    }
}
```

---

### 3.7 Chat Screen State Machine (full)

```
WebSocket state machine per conversation screen:

OPEN STATE (conversation.is_closed == false)
│
├── offer_count < 3 AND no pending offer  →  Offer button ENABLED
├── offer_count < 3 AND pending offer     →  Offer button DISABLED ("Waiting for seller")
├── offer_count >= 3                      →  Offer button DISABLED ("Offer limit reached")
│       └── on WS frame offer.limit_reset →  Reset to 0, re-enable
│
├── conversation.order_id == null         →  Normal chat UI
└── conversation.order_id != null         →  Show Pay banner
        BUYER:  "Deal agreed → Pay ₫130.000" (navigate to /orders/{order_id})
        SELLER: "Waiting for buyer payment" / "In transit" / "Completed"

CLOSED STATE (conversation.is_closed == true)
│   Triggered by: WS frame listing.reserved
│
├── Chat input:   DISABLED
├── Offer button: HIDDEN
├── Banner:       "Sản phẩm này đã được đặt mua bởi người khác"
│
├── on WS frame listing.sold     →  Banner: "Sản phẩm đã được bán thành công"
└── on WS frame listing.available →
        Remove CLOSED STATE → OPEN STATE
        Banner: "Sản phẩm này hiện đã có thể mua lại"
        Reset offer_count = 0, re-enable offer button
```

---

### 3.8 Offer Count Tracking

The server is the source of truth (`conversation.offer_count`). The Android app should:

1. Read `offer_count` from `GET /api/v1/chat/conversations/{id}` on screen open.
2. Increment local counter optimistically when the buyer sends an offer (before server response).
3. On `offer.limit_reset` WS frame: reset local counter to 0, re-enable button, show price-changed banner.
4. On HTTP 409 `OFFER_LIMIT_REACHED`: show error toast (should be prevented by UI, but handle defensively).

---

### 3.9 Polling Fallback (when WebSocket is disconnected)

```
Inbox screen:     GET /chat/conversations          every 10s
                  GET /chat/unread                  every 10s

Chat screen:      GET /chat/conversations/{id}      every 3s  (detect order_id + is_closed)
                  GET /chat/conversations/{id}/messages  every 3s
```

Stop all polling when WebSocket reconnects successfully.

---

### 3.10 REST API Quick Reference (Chat)

| Method | URL | Who | Description |
|---|---|---|---|
| POST | `/chat/conversations` | Buyer | Start or resume conversation |
| GET | `/chat/conversations` | Both | Inbox (flat list) |
| GET | `/chat/conversations?group_by=listing` | Seller | Grouped by listing |
| GET | `/chat/conversations/{id}` | Both | Single conversation (check `order_id`, `is_closed`, `offer_count`) |
| GET | `/chat/conversations/{id}/messages` | Both | Message history (newest first) |
| POST | `/chat/conversations/{id}/read` | Both | Mark as read |
| POST | `/chat/messages` | Both | Send text message |
| DELETE | `/chat/messages/{id}` | Sender | Delete own message (5 min window) |
| POST | `/chat/offers` | Buyer | Send price offer |
| POST | `/chat/offers/accept` | Seller | Accept offer → creates order |
| POST | `/chat/offers/decline` | Seller | Decline offer |
| GET | `/chat/unread` | Both | Unread badge count |

---

### 3.11 New / Changed Error Codes

| HTTP | Code | When | Action |
|---|---|---|---|
| 409 | `OFFER_LIMIT_REACHED` | Buyer sent 4th offer | Disable offer button, show toast |
| 409 | `CONVERSATION_CLOSED` | Message/offer sent to closed conversation | Show closed banner, disable input |
| 409 | `LISTING_RESERVED` | New conversation started on reserved/sold listing | Show "item unavailable" dialog |
| 409 | `CONVERSATION_ORDER_EXISTS` | Second order attempt on same conversation | Navigate to existing order |
| 409 | `PENDING_OFFER_EXISTS` | Offer sent while one is pending | Keep offer button disabled |

---

### 3.12 VND Formatting

```kotlin
fun formatVND(amount: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    return "₫${formatter.format(amount)}"
}
// 80000    → "₫80.000"
// 1500000  → "₫1.500.000"
```

---

## Part 4 — Deployment Checklist

### Core-service
- [ ] `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` set on container
- [ ] `APP_NAME` / `JWT_ISSUER` set without unexpanded shell variables
- [ ] S3 env vars set (`S3_PUBLIC_BASE_URL` for presigned URL signing)
- [ ] Database migration run — new columns `offer_count`, `is_closed` on `conversations` table

### Realtime-service
- [ ] Same Redis credentials as core-service
- [ ] `ACCESS_TOKEN_SECRET` + `JWT_ISSUER` match core-service
- [ ] Consumer groups created (use `MKSTREAM` flag) on startup
- [ ] `STREAM_CONSUMER_ID` unique per replica (use pod name or hostname)
- [ ] `REDIS_USE_STREAMS=true` (default)

### Android
- [ ] WebSocket client with auto-reconnect and exponential backoff
- [ ] All 8 `message_type` handler cases implemented
- [ ] `offer_count` / `is_closed` read from `GET /conversations/{id}` on screen enter
- [ ] `subscribe.listing` sent when entering listing detail screen
- [ ] Polling fallback active when WebSocket is not connected

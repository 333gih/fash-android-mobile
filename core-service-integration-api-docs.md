# Chat Feature — Integration Guide

Base URL: `https://{host}/api/v1`  
All endpoints require: `Authorization: Bearer {access_token}`  
All responses are `Content-Type: application/json`

---

## Data Models

### Conversation Object

```json
{
  "id":              "uuid",
  "listing_id":      "uuid",
  "buyer_id":        "uuid",
  "seller_id":       "uuid",
  "last_message":    "string",
  "last_message_at": "2024-01-15T10:30:00Z | null",
  "order_id":        "uuid | null",
  "created_at":      "2024-01-15T10:00:00Z",
  "updated_at":      "2024-01-15T10:30:00Z",

  "listing": {
    "id":             "uuid",
    "title":          "string",
    "cover_image_url":"https://... (presigned, valid 24h)",
    "image_urls":     ["https://...", "https://..."],
    "price":          100000,
    "status":         "active | reserved | sold",
    "condition":      "string",
    "brand":          "string"
  },

  "buyer": {
    "user_id":      "uuid",
    "display_name": "string",
    "username":     "string",
    "avatar_url":   "https://... (presigned, valid 24h)"
  },

  "seller": {
    "user_id":      "uuid",
    "display_name": "string",
    "username":     "string",
    "avatar_url":   "https://... (presigned, valid 24h)"
  }
}
```

**`order_id` field logic:**
| Value | Meaning | UI action |
|---|---|---|
| `null` | No deal yet — negotiation in progress | Show offer button normally |
| `"uuid"` | Offer accepted, order created | Hide offer button, show Pay banner → `GET /orders/{order_id}` |

---

### Message Object

```json
{
  "id":              "uuid",
  "conversation_id": "uuid",
  "sender_id":       "uuid",
  "message_type":    "text | offer | system",
  "content":         "string (empty for offer type)",
  "offer_amount_vnd":"80000 | null",
  "offer_status":    "pending | accepted | declined | expired | cancelled | null",
  "is_deleted":      false,
  "read_at":         "2024-01-15T10:30:00Z | null",
  "created_at":      "2024-01-15T10:00:00Z",
  "updated_at":      "2024-01-15T10:00:00Z"
}
```

**`message_type` values:**
| Value | Description | Render as |
|---|---|---|
| `text` | Normal chat message | Chat bubble (right=me, left=other) |
| `offer` | Price offer from buyer | Special offer card (see offer card section) |
| `system` | Auto-generated event text | Centered grey italic text, no bubble |

**`offer_status` values (only set when `message_type == "offer"`):**
| Value | Description | UI |
|---|---|---|
| `pending` | Waiting for seller response | Yellow pill "Waiting..." |
| `accepted` | Seller accepted — order created | Green pill "Accepted ✓" |
| `declined` | Seller declined | Red pill "Declined" |
| `expired` | Auto-expired after 24 hours | Grey pill "Expired" |
| `cancelled` | Cancelled by system | Grey pill "Cancelled" |

**System message content examples:**
- `"Người bán đã chấp nhận giá ₫80.000"` — offer accepted
- `"Người bán đã từ chối giá"` — offer declined
- `"Đề xuất giá đã hết hạn"` — offer expired (auto after 24h)

---

## Error Response Format

```json
{
  "error": "human readable message"
}
```

### All Error Codes

| HTTP | Code | Message | When |
|---|---|---|---|
| 400 | `VALIDATION_ERROR` | request validation failed | Missing/invalid fields |
| 401 | `TOKEN_INVALID` | token is invalid | Bad or missing JWT |
| 401 | `TOKEN_EXPIRED` | token has expired | JWT expired |
| 403 | `FORBIDDEN` | permission denied | Not a conversation participant |
| 403 | `CANNOT_MESSAGE_SELF` | cannot message the seller of your own listing | Buyer = seller |
| 404 | `CONVERSATION_NOT_FOUND` | conversation not found | Bad conversation_id |
| 404 | `MESSAGE_NOT_FOUND` | message not found | Bad message_id |
| 409 | `PENDING_OFFER_EXISTS` | wait for seller to respond before making another offer | Buyer sends offer while one is pending |
| 409 | `CONVERSATION_ORDER_EXISTS` | an order has already been created for this conversation | Any action after deal is done |
| 400 | `MESSAGE_NOT_DELETABLE` | message can only be deleted within 5 minutes | Delete attempted after 5 min window |
| 500 | `INTERNAL_ERROR` | an internal error occurred | Server error |

---

## API Reference

---

### 1. Start Conversation

Opens a new conversation or returns existing one between the logged-in buyer and the listing's seller.

```
POST /chat/conversations
```

**Who can call:** Buyer only (cannot be the listing owner)

**Request body:**
```json
{
  "listing_id": "uuid"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `listing_id` | uuid | ✅ | The listing to discuss |

**Response `200`** — conversation already exists:
```json
{ ...Conversation Object... }
```

**Response `201`** — new conversation created:
```json
{ ...Conversation Object... }
```

**Errors:**
| HTTP | Code | Cause |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Missing or invalid listing_id |
| 403 | `FORBIDDEN` | You are blocked by / have blocked the seller |
| 403 | `CANNOT_MESSAGE_SELF` | You own the listing |
| 404 | `LISTING_NOT_FOUND` | listing_id does not exist |

> **Note:** Status `200` = existing conversation returned. Status `201` = freshly created. Use HTTP status code to distinguish.

---

### 2. Get Conversations (Inbox)

Returns all conversations for the logged-in user, sorted by most recent message.

```
GET /chat/conversations?limit=20&offset=0
```

**Query params:**
| Param | Type | Default | Max | Description |
|---|---|---|---|---|
| `limit` | int | 20 | 50 | Page size |
| `offset` | int | 0 | — | Pagination offset |

**Response `200`:**
```json
[
  { ...Conversation Object... },
  { ...Conversation Object... }
]
```

Returns empty array `[]` when no conversations.

---

### 3. Get Single Conversation

Returns one conversation by ID. Use this to:
- Check `order_id` when entering the chat screen
- Poll every 3 seconds to detect when a deal is made (`order_id` changes from `null` → UUID)

```
GET /chat/conversations/{conversation_id}
```

**Path params:**
| Param | Required | Description |
|---|---|---|
| `conversation_id` | ✅ | UUID of the conversation |

**Response `200`:**
```json
{ ...Conversation Object... }
```

**Errors:**
| HTTP | Code | Cause |
|---|---|---|
| 403 | `FORBIDDEN` | You are not buyer or seller of this conversation |
| 404 | `CONVERSATION_NOT_FOUND` | conversation_id does not exist |

---

### 4. Get Messages

Returns messages in a conversation. **Newest first** (index 0 = latest).

```
GET /chat/conversations/{conversation_id}/messages?limit=20&offset=0
```

**Path params:**
| Param | Required | Description |
|---|---|---|
| `conversation_id` | ✅ | UUID of the conversation |

**Query params:**
| Param | Type | Default | Max | Description |
|---|---|---|---|---|
| `limit` | int | 20 | 50 | Page size |
| `offset` | int | 0 | — | Pagination offset (load older messages) |

**Response `200`:**
```json
[
  {
    "id":              "uuid",
    "conversation_id": "uuid",
    "sender_id":       "uuid",
    "message_type":    "text",
    "content":         "Can you lower the price?",
    "offer_amount_vnd": null,
    "offer_status":    "",
    "is_deleted":      false,
    "read_at":         null,
    "created_at":      "2024-01-15T10:30:00Z",
    "updated_at":      "2024-01-15T10:30:00Z"
  },
  {
    "id":              "uuid",
    "conversation_id": "uuid",
    "sender_id":       "uuid",
    "message_type":    "offer",
    "content":         "",
    "offer_amount_vnd": 80000,
    "offer_status":    "pending",
    "is_deleted":      false,
    "read_at":         null,
    "created_at":      "2024-01-15T10:25:00Z",
    "updated_at":      "2024-01-15T10:25:00Z"
  }
]
```

**Errors:**
| HTTP | Code | Cause |
|---|---|---|
| 403 | `FORBIDDEN` | You are not buyer or seller of this conversation |
| 404 | `CONVERSATION_NOT_FOUND` | conversation_id does not exist |

> **Polling:** Call this every 3 seconds while the chat screen is open. Render messages by `created_at` ascending in the UI (oldest at top, newest at bottom).

---

### 5. Send Text Message

Sends a plain text message. Both buyer and seller can send.

```
POST /chat/messages
```

**Request body:**
```json
{
  "conversation_id": "uuid",
  "content": "Is this still available?"
}
```

| Field | Type | Required | Validation |
|---|---|---|---|
| `conversation_id` | uuid | ✅ | Must be a valid conversation you participate in |
| `content` | string | ✅ | 1–1000 characters, whitespace-trimmed |

**Response `200`:**
```json
{
  "id":              "uuid",
  "conversation_id": "uuid",
  "sender_id":       "uuid",
  "message_type":    "text",
  "content":         "Is this still available?",
  "offer_amount_vnd": null,
  "offer_status":    "",
  "is_deleted":      false,
  "read_at":         null,
  "created_at":      "2024-01-15T10:30:00Z",
  "updated_at":      "2024-01-15T10:30:00Z"
}
```

**Errors:**
| HTTP | Code | Cause |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Empty content or > 1000 chars |
| 403 | `FORBIDDEN` | You are blocked / not a participant |
| 404 | `CONVERSATION_NOT_FOUND` | conversation_id does not exist |

> **Side effect:** When the **seller** sends a text message, any pending offer in the conversation is automatically expired. This unblocks the buyer to send a new offer.

---

### 6. Send Price Offer

Buyer sends a price offer. Only one pending offer allowed at a time. Offer must be less than listing price and ≥ ₫1,000.

```
POST /chat/offers
```

**Who can call:** Buyer only

**Request body:**
```json
{
  "conversation_id": "uuid",
  "amount_vnd": 80000
}
```

| Field | Type | Required | Validation |
|---|---|---|---|
| `conversation_id` | uuid | ✅ | Must be a conversation where you are the buyer |
| `amount_vnd` | int64 | ✅ | Min ₫1,000, must be **less than** listing price |

**Response `200`:**
```json
{
  "id":              "uuid",
  "conversation_id": "uuid",
  "sender_id":       "uuid (buyer)",
  "message_type":    "offer",
  "content":         "",
  "offer_amount_vnd": 80000,
  "offer_status":    "pending",
  "is_deleted":      false,
  "read_at":         null,
  "created_at":      "2024-01-15T10:30:00Z",
  "updated_at":      "2024-01-15T10:30:00Z"
}
```

**Errors:**
| HTTP | Code | Cause |
|---|---|---|
| 400 | `VALIDATION_ERROR` | amount_vnd < 1000 OR amount_vnd ≥ listing price |
| 403 | `FORBIDDEN` | You are not the buyer, or you are blocked |
| 404 | `CONVERSATION_NOT_FOUND` | conversation_id does not exist |
| 409 | `PENDING_OFFER_EXISTS` | You already have a pending offer — wait for seller response |
| 409 | `CONVERSATION_ORDER_EXISTS` | Deal already done — order exists for this conversation |

> **Offer expiry:** Offers automatically expire after **24 hours** if not responded to. A system message is added to the conversation when this happens.

---

### 7. Accept Offer (Seller)

Seller accepts a pending offer. **Automatically creates an order** and sets `conversation.order_id`.

```
POST /chat/offers/accept
```

**Who can call:** Seller only

**Request body:**
```json
{
  "conversation_id":  "uuid",
  "offer_message_id": "uuid"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `conversation_id` | uuid | ✅ | The conversation |
| `offer_message_id` | uuid | ✅ | The `id` of the offer message to accept (must have `offer_status == "pending"`) |

**Response `200`:**
```json
{
  "ok": true
}
```

**What happens automatically:**
1. Order created with `status = "payment_pending"`
2. Listing status → `"reserved"`
3. `conversation.order_id` set to the new order UUID
4. Offer message `offer_status` → `"accepted"`
5. System message added: `"Người bán đã chấp nhận giá ₫80.000"`
6. Buyer receives FCM push notification

**After accept:** Call `GET /chat/conversations/{id}` → `order_id` will be set → navigate buyer to payment screen using `GET /orders/{order_id}`.

**Errors:**
| HTTP | Code | Cause |
|---|---|---|
| 400 | `VALIDATION_ERROR` | offer_message_id is not a pending offer |
| 403 | `FORBIDDEN` | You are not the seller |
| 404 | `CONVERSATION_NOT_FOUND` | conversation_id does not exist |
| 404 | `MESSAGE_NOT_FOUND` | offer_message_id does not exist |
| 409 | `CONVERSATION_ORDER_EXISTS` | Order already exists for this conversation |

---

### 8. Decline Offer (Seller)

Seller declines a pending offer. Buyer can send a new offer after.

```
POST /chat/offers/decline
```

**Who can call:** Seller only

**Request body:**
```json
{
  "conversation_id":  "uuid",
  "offer_message_id": "uuid"
}
```

**Response `200`:**
```json
{
  "ok": true
}
```

**What happens automatically:**
1. Offer message `offer_status` → `"declined"`
2. System message added: `"Người bán đã từ chối giá"`
3. Buyer receives FCM push notification

**Errors:**
| HTTP | Code | Cause |
|---|---|---|
| 400 | `VALIDATION_ERROR` | offer_message_id is not a pending offer |
| 403 | `FORBIDDEN` | You are not the seller |
| 404 | `CONVERSATION_NOT_FOUND` / `MESSAGE_NOT_FOUND` | Bad IDs |

---

### 9. Mark Conversation as Read

Marks all unread messages from the other party as read. Call when the user opens the chat screen.

```
POST /chat/conversations/{conversation_id}/read
```

**Path params:**
| Param | Required | Description |
|---|---|---|
| `conversation_id` | ✅ | UUID of the conversation |

**Response `200`:**
```json
{
  "ok": true
}
```

**Errors:**
| HTTP | Code | Cause |
|---|---|---|
| 403 | `FORBIDDEN` | Not a participant |
| 404 | `CONVERSATION_NOT_FOUND` | Bad conversation_id |

---

### 10. Delete Message

Deletes own message. Only allowed within **5 minutes** of sending. The message is soft-deleted (marked `is_deleted: true`), not removed from the list.

```
DELETE /chat/messages/{message_id}
```

**Path params:**
| Param | Required | Description |
|---|---|---|
| `message_id` | ✅ | UUID of the message to delete |

**Response `200`:**
```json
{
  "ok": true
}
```

**Errors:**
| HTTP | Code | Cause |
|---|---|---|
| 400 | `MESSAGE_NOT_DELETABLE` | More than 5 minutes have passed since sending |
| 403 | `FORBIDDEN` | Not your message |
| 404 | `MESSAGE_NOT_FOUND` | Bad message_id |

> **Render deleted messages as:** Italicised placeholder text e.g. *"Message deleted"*. Check `is_deleted == true` in the message list.

---

### 11. Get Unread Count

Returns total unread message count across all conversations. Use for the inbox badge.

```
GET /chat/unread
```

**Response `200`:**
```json
{
  "unread_count": 3
}
```

---

## Polling Strategy (while realtime service is not yet live)

```
Screen: Conversation List (Inbox)
  → On enter: GET /chat/conversations
  → On enter: GET /chat/unread  (badge)
  → Poll every 10 seconds: GET /chat/conversations
  → Poll every 10 seconds: GET /chat/unread

Screen: Chat Screen (open conversation)
  → On enter:
      1. GET /chat/conversations/{id}    ← check order_id, listing status
      2. GET /chat/conversations/{id}/messages?limit=20&offset=0
      3. POST /chat/conversations/{id}/read  ← mark as read
  → Poll every 3 seconds:
      4. GET /chat/conversations/{id}/messages?limit=20&offset=0
      5. GET /chat/conversations/{id}   ← detect order_id change
  → On exit:
      6. POST /chat/conversations/{id}/read
      7. Stop polling
```

---

## Chat Screen State Machine

```
conversation.order_id == null
    ├── I am BUYER:
    │     ├── No pending offer → show "Make Offer" button (enabled)
    │     └── Pending offer exists in message list → show "Make Offer" button (disabled)
    │         tooltip: "Waiting for seller response"
    │
    └── I am SELLER:
          └── Offer message with offer_status=="pending" in list
              → show Accept (green) + Decline (red) buttons inside offer card

conversation.order_id != null
    ├── Hide offer button entirely
    ├── Show sticky Pay banner below header:
    │     BUYER:  "Deal agreed! Tap to pay" → navigate to /orders/{order_id}
    │     SELLER: "Waiting for buyer payment" (if order.status == payment_pending)
    │             "Order in progress" (if order.status == payment_held or in_transit)
    └── Chat input still active (can still send text messages)
```

---

## Offer Card Rendering

When `message_type == "offer"`, render as a card instead of a bubble:

```
┌─────────────────────────────────────┐
│  🏷️  Price Offer                    │
│                                     │
│      ₫80.000                        │  ← offer_amount_vnd formatted
│                                     │
│  [  pending  ]                      │  ← status badge (colour by status)
│                                     │
│  [  Accept  ]  [  Decline  ]        │  ← only when: I am SELLER + status==pending
│  (green btn)   (red btn)            │
└─────────────────────────────────────┘
```

**Status badge colours:**
| Status | Background | Text |
|---|---|---|
| `pending` | `#FFF9C4` yellow | "Waiting..." |
| `accepted` | `#E8F5E9` green | "Accepted ✓" |
| `declined` | `#FFEBEE` red | "Declined" |
| `expired` | `#F5F5F5` grey | "Expired" |
| `cancelled` | `#F5F5F5` grey | "Cancelled" |

---

## VND Currency Formatting

All amounts are integers. Format for display:

```kotlin
fun formatVND(amount: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    return "₫${formatter.format(amount)}"
}
// 80000    → "₫80.000"
// 1500000  → "₫1.500.000"
// 100000   → "₫100.000"
```

---

## Image URLs

All `avatar_url`, `cover_image_url`, `image_urls` are **presigned S3 URLs** valid for 24 hours.

- Load directly with Glide / Coil — no auth headers required
- Do not persist to disk cache beyond 24 hours
- On 403 from image URL: re-fetch the API to get fresh presigned URL

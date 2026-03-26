# Order API Integration Guide

**Base path:** `POST /api/v1/orders`  
**Authentication:** `Authorization: Bearer <access_token>` is required on all endpoints except the payment webhook.  
**Content-Type:** `application/json`

---

## Table of Contents

1. [Common Objects](#1-common-objects)
2. [Order Status Lifecycle](#2-order-status-lifecycle)
3. [Create Order](#3-post-apiv1orders--create-order)
4. [Get My Orders](#4-get-apiv1orders--list-my-orders)
5. [Get Order by ID](#5-get-apiv1ordersorder_id--get-single-order)
6. [Mark Shipped](#6-post-apiv1ordersship--mark-order-shipped)
7. [Confirm Receipt](#7-post-apiv1ordersorder_idconfirm--confirm-receipt)
8. [Submit Review](#8-post-apiv1ordersreview--submit-review)
9. [Open Dispute](#9-post-apiv1ordersdispute--open-dispute)
10. [Submit Dispute Evidence](#10-post-apiv1ordersdisputeevidence--submit-dispute-evidence)
11. [Payment Webhook](#11-post-apiv1ordersorder_idpayment-confirm--payment-webhook)
12. [Error Codes](#12-error-codes)
13. [Business Rules](#13-business-rules)
14. [Realtime Events](#14-realtime-events-redis-streams)

---

## 1. Common Objects

### Order Object

Returned by Create, Get, and List endpoints. All image URLs inside nested `Listing` are **presigned** (ready to display).

```json
{
  "ID":               "uuid",
  "CreatedAt":        "2026-03-26T05:00:00Z",
  "UpdatedAt":        "2026-03-26T05:00:00Z",
  "listing_id":       "uuid",
  "buyer_id":         "uuid",
  "seller_id":        "uuid",
  "amount_vnd":       450000,
  "platform_fee_vnd": 45000,
  "seller_payout_vnd":405000,
  "status":           "payment_pending",
  "tracking_number":  "",
  "carrier":          "",
  "auto_release_at":  "2026-03-31T05:00:00Z",
  "confirmed_at":     null,
  "Listing": {
    "ID":             "uuid",
    "title":          "Vintage Denim Jacket",
    "cover_image_url":"https://signed-url...",
    "image_urls":     ["https://signed-url..."],
    "price":          450000,
    "condition":      "like_new",
    "status":         "reserved",
    "Seller": { /* Profile object */ },
    "Category": { /* Category object */ }
  },
  "Buyer":  { /* Profile object */ },
  "Seller": { /* Profile object */ }
}
```

### Profile Object (nested)

```json
{
  "ID":             "uuid",
  "user_id":        "uuid",
  "username":       "fashuser",
  "display_name":   "Fash User",
  "avatar_url":     "https://signed-url...",
  "average_rating": 4.80
}
```

### Error Object

```json
{
  "code":    400,
  "error":   "human-readable message"
}
```

---

## 2. Order Status Lifecycle

```
                      [buyer creates order]
                               │
                      payment_pending   ◄── listing stays "reserved"
                               │             after create until paid
               [webhook: payment confirmed]
                               │
                      payment_held      ◄── listing = "reserved"
                               │             auto_release_at set (+5 days)
                    [seller marks shipped]
                               │
                          in_transit
                          /          \
           [buyer confirms]        [dispute opened]
                  │                        │
       delivered_confirmed            disputed
                  │
      [auto-release timer OR manual confirm]
                  │
          listing = "sold"
          closed conversations notified
```

### Status Values

| Status | Who triggers | Listing state |
|---|---|---|
| `payment_pending` | Buyer creates order | `reserved` (locked) |
| `payment_held` | Payment webhook fires | `reserved` |
| `in_transit` | Seller ships | `reserved` |
| `delivered_confirmed` | Buyer confirms OR auto-release | `sold` |
| `cancelled` | Payment expiry job | `active` (reopened) |
| `disputed` | Buyer or seller opens dispute | `reserved` |

---

## 3. `POST /api/v1/orders` — Create Order

Buyer initiates checkout. The listing and order are created atomically — no other buyer can race to purchase the same listing.

**Platform fee:** 10% of `amount_vnd` is deducted. `seller_payout_vnd = amount_vnd - platform_fee_vnd`.

### Request Body

```json
{
  "listing_id": "uuid",    // required — the listing being purchased
  "amount_vnd": 450000     // required, min 1000 — agreed price (listing price or accepted offer)
}
```

> **Note:** `amount_vnd` must be ≤ listing's current `price`. Pass the offer amount when buyer accepted a seller counter-offer.

### Response `201 Created`

```json
{ /* Order object — status = "payment_pending" */ }
```

### Errors

| HTTP | Code | Cause |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Missing fields or amount < 1000 or amount > listing price |
| 400 | `CANNOT_BUY_OWN` | Buyer is the seller of the listing |
| 400 | `ORDER_NOT_AVAILABLE` | Listing is not `active` (already reserved/sold) |
| 403 | `FORBIDDEN` | Buyer is blocked by seller |
| 404 | `LISTING_NOT_FOUND` | Listing does not exist |
| 409 | `CONVERSATION_ORDER_EXISTS` | An order was already created for this conversation |
| 500 | `INTERNAL_ERROR` | Database or server error |

---

## 4. `GET /api/v1/orders` — List My Orders

Returns paginated orders for the authenticated user, either as buyer or seller.

### Query Parameters

| Param | Required | Type | Default | Description |
|---|---|---|---|---|
| `role` | **yes** | `buyer` \| `seller` | `buyer` | Which side to query as |
| `status` | no | string | *(all)* | Filter by status (see lifecycle table) |
| `limit` | no | int 1–50 | `20` | Page size |
| `offset` | no | int | `0` | Pagination offset |

### Response `200 OK`

```json
[
  { /* Order object */ },
  { /* Order object */ }
]
```

> Empty array `[]` when no orders match.

### Errors

| HTTP | Code | Cause |
|---|---|---|
| 400 | `VALIDATION_ERROR` | `role` is not `buyer` or `seller` |
| 401 | — | Missing or invalid token |
| 500 | `INTERNAL_ERROR` | Server error |

---

## 5. `GET /api/v1/orders/:order_id` — Get Single Order

Returns a single order. Only the buyer or seller of that order can access it.

### Path Parameter

| Param | Type | Description |
|---|---|---|
| `order_id` | UUID | The order ID |

### Response `200 OK`

```json
{ /* Order object */ }
```

### Errors

| HTTP | Code | Cause |
|---|---|---|
| 400 | — | Missing `order_id` |
| 403 | `FORBIDDEN` | Caller is not buyer or seller of this order |
| 404 | `ORDER_NOT_FOUND` | Order does not exist |

---

## 6. `POST /api/v1/orders/ship` — Mark Order Shipped

Seller marks the order as shipped with a tracking number.

**Allowed from:** `payment_held` → transitions to `in_transit`.

### Request Body

```json
{
  "order_id":        "uuid",             // required
  "tracking_number": "VN123456789VN",    // required, max 100 chars
  "carrier":         "Giao Hang Nhanh"  // required, max 50 chars
}
```

### Response `200 OK`

```json
{ "ok": true }
```

### Errors

| HTTP | Code | Cause |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Missing or invalid fields |
| 400 | `ORDER_NOT_AVAILABLE` | Order not in `payment_held` |
| 403 | `FORBIDDEN` | Caller is not the seller |
| 404 | `ORDER_NOT_FOUND` | Order does not exist |

---

## 7. `POST /api/v1/orders/:order_id/confirm` — Confirm Receipt

Buyer confirms they received the item. Triggers escrow release to seller.

**Allowed from:** `in_transit` → transitions to `delivered_confirmed`.

**Side effects:**
- Listing status updated to `sold`
- Final system message inserted into all closed conversations for the listing: *"Sản phẩm đã được bán thành công."*
- `listing.status_changed` (`sold`) event published to Redis Stream

### Path Parameter

| Param | Type | Description |
|---|---|---|
| `order_id` | UUID | The order ID |

### Request Body

*(none)*

### Response `200 OK`

```json
{ "ok": true }
```

### Errors

| HTTP | Code | Cause |
|---|---|---|
| 400 | `ORDER_NOT_AVAILABLE` | Order not in `in_transit` |
| 403 | `FORBIDDEN` | Caller is not the buyer |
| 404 | `ORDER_NOT_FOUND` | Order does not exist |

---

## 8. `POST /api/v1/orders/review` — Submit Review

Buyer submits a 1–5 star review after receiving the item. Seller's `average_rating` on their profile is recalculated.

**Allowed after:** `delivered_confirmed` status.  
**One review per order** — cannot be edited or resubmitted.

### Request Body

```json
{
  "order_id": "uuid",      // required
  "rating":   5,           // required, integer 1–5
  "comment":  "Excellent quality, fast shipping!" // optional, max 500 chars
}
```

### Response `200 OK`

```json
{
  "ID":         "uuid",
  "CreatedAt":  "2026-03-26T05:00:00Z",
  "UpdatedAt":  "2026-03-26T05:00:00Z",
  "order_id":   "uuid",
  "buyer_id":   "uuid",
  "seller_id":  "uuid",
  "rating":     5,
  "comment":    "Excellent quality, fast shipping!"
}
```

### Errors

| HTTP | Code | Cause |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Missing fields or rating out of 1–5 range |
| 400 | `ORDER_NOT_REVIEWABLE` | Order is not in `delivered_confirmed` |
| 403 | `FORBIDDEN` | Caller is not the buyer |
| 404 | `ORDER_NOT_FOUND` | Order does not exist |
| 409 | `ORDER_ALREADY_REVIEWED` | Review was already submitted for this order |

---

## 9. `POST /api/v1/orders/dispute` — Open Dispute

Buyer or seller opens a dispute. The order must be in `in_transit` or `delivered_confirmed`. Only one dispute is allowed per order.

**Side effect:** Order transitions to `disputed`.

### Request Body

```json
{
  "order_id":    "uuid",                 // required
  "description": "Item arrived broken.", // required, max 2000 chars
  "photo_urls":  [                       // optional, max 10 items, each max 512 chars
    "https://signed-url..."
  ]
}
```

### Response `201 Created`

```json
{
  "ID":                 "uuid",
  "CreatedAt":          "2026-03-26T05:00:00Z",
  "UpdatedAt":          "2026-03-26T05:00:00Z",
  "order_id":           "uuid",
  "opened_by":          "uuid",
  "description":        "Item arrived broken.",
  "photos_json":        "[\"https://...\"]",
  "buyer_description":  "",
  "buyer_photos_json":  "",
  "seller_description": "",
  "seller_photos_json": "",
  "status":             "open"
}
```

### Errors

| HTTP | Code | Cause |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Missing fields |
| 400 | `DISPUTE_WINDOW_EXPIRED` | Outside the 48-hour window after `delivered_confirmed` |
| 403 | `FORBIDDEN` | Caller is not buyer or seller |
| 404 | `ORDER_NOT_FOUND` | Order does not exist |
| 409 | `DISPUTE_ALREADY_OPEN` | Dispute already exists for this order |

---

## 10. `POST /api/v1/orders/dispute/evidence` — Submit Dispute Evidence

After a dispute is opened, both sides can submit their own evidence (description + photos). Each call updates the caller's side (`buyer_*` or `seller_*` fields). Can be called multiple times — last write wins.

### Request Body

```json
{
  "order_id":    "uuid",
  "description": "Here is my evidence proving the item was fine when shipped.",
  "photo_urls":  ["https://signed-url..."]
}
```

### Response `200 OK`

```json
{
  "ID":                 "uuid",
  "CreatedAt":          "...",
  "UpdatedAt":          "...",
  "order_id":           "uuid",
  "opened_by":          "uuid",
  "description":        "Item arrived broken.",
  "photos_json":        "[\"https://...\"]",
  "buyer_description":  "I have photos of unboxing.",
  "buyer_photos_json":  "[\"https://...\"]",
  "seller_description": "Item was packed carefully.",
  "seller_photos_json": "[\"https://...\"]",
  "status":             "open"
}
```

### Errors

| HTTP | Code | Cause |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Missing fields |
| 403 | `FORBIDDEN` | Caller is not buyer or seller |
| 404 | `ORDER_NOT_FOUND` | Order or dispute does not exist |

---

## 11. `POST /api/v1/orders/:order_id/payment-confirm` — Payment Webhook

Called by the **payment service** (not the app client) when the buyer's payment is successfully captured.

> **No user JWT required.** Secured via HMAC-SHA256 request signature instead.

### Path Parameter

| Param | Type | Description |
|---|---|---|
| `order_id` | UUID | The order being paid |

### Required Header

```
X-Payment-Signature: <hex>
```

The payment service must compute the signature as:

```
hex( HMAC-SHA256( PAYMENT_WEBHOOK_SECRET, order_id ) )
```

Where `PAYMENT_WEBHOOK_SECRET` is the shared secret configured in `.env`.

### Request Body

*(none)*

### Response `200 OK`

```json
{ "ok": true }
```

### Side Effects on Success

1. Order transitions `payment_pending` → `payment_held`
2. `auto_release_at` set to `now + 5 days`
3. Listing status remains `reserved`
4. **All other open conversations** for this listing (not the winning buyer's) are:
   - Closed (`is_closed = true`)
   - Receive system message: *"Sản phẩm này đã được đặt mua bởi người khác. Cuộc trò chuyện đã kết thúc."*
5. FCM push notification sent to seller
6. `listing.status_changed` (`reserved`) event published to Redis Stream `stream:listing:status`
7. `order.created` event published to Redis Stream `stream:payment:orders:confirmed`

### Errors

| HTTP | Code | Cause |
|---|---|---|
| 400 | — | Missing `order_id` path param |
| 401 | — | Missing or invalid `X-Payment-Signature` header |
| 400 | `ORDER_NOT_PAYABLE` | Order is not in `payment_pending` |
| 404 | `ORDER_NOT_FOUND` | Order does not exist |

---

## 12. Error Codes

| HTTP | Code | Message | Endpoint(s) |
|---|---|---|---|
| 400 | `VALIDATION_ERROR` | request validation failed | All |
| 400 | `ORDER_NOT_AVAILABLE` | listing is not available for purchase | Create |
| 400 | `CANNOT_BUY_OWN` | cannot buy your own listing | Create |
| 400 | `ORDER_NOT_PAYABLE` | order is not pending payment | Payment webhook |
| 400 | `ORDER_NOT_REVIEWABLE` | order cannot be reviewed yet | Review |
| 400 | `DISPUTE_WINDOW_EXPIRED` | dispute can only be opened within 48 hours | Dispute |
| 401 | — | authentication required | Protected endpoints |
| 403 | `FORBIDDEN` | permission denied | All |
| 404 | `LISTING_NOT_FOUND` | listing not found | Create |
| 404 | `ORDER_NOT_FOUND` | order not found | All order ops |
| 409 | `ORDER_ALREADY_REVIEWED` | order already has a review | Review |
| 409 | `DISPUTE_ALREADY_OPEN` | order already has an open dispute | Dispute |
| 409 | `CONVERSATION_ORDER_EXISTS` | an order has already been created for this conversation | Create |
| 500 | `INTERNAL_ERROR` | an internal error occurred | All |

---

## 13. Business Rules

### Fee Calculation

```
platform_fee_vnd  = round(amount_vnd × 0.10)
seller_payout_vnd = amount_vnd - platform_fee_vnd
```

### Auto-Release Timer

If the buyer does not confirm receipt within **5 days** of `payment_held`, the background job auto-confirms the order:
- Order → `delivered_confirmed`
- Listing → `sold`
- System message sent to closed conversations: *"Sản phẩm đã được bán thành công."*
- `listing.status_changed` (`sold`) event published

### Payment Expiry

If the buyer never pays (order stays `payment_pending` too long), the expiry job:
- Order → `cancelled`
- Listing → `active` (back on market)
- All previously closed conversations are **reopened** (`is_closed = false`)
- System message sent: *"Sản phẩm này hiện đã có thể mua lại."*
- `listing.status_changed` (`active`) event published

### One Order Per Conversation

Once an order is created for a conversation, no second order can be created from the same conversation. The `conversation.order_id` field is set and guards against duplicates.

---

## 14. Realtime Events (Redis Streams)

The order flow publishes events that the **Realtime Service** consumes to push WebSocket messages to connected clients.

### `stream:payment:orders:created` — Order Created

Published when `POST /orders` succeeds.

```json
{
  "type": "order.created",
  "payload": {
    "order_id":          "uuid",
    "buyer_id":          "uuid",
    "seller_id":         "uuid",
    "listing_id":        "uuid",
    "amount_vnd":        450000,
    "platform_fee_vnd":  45000,
    "seller_payout_vnd": 405000
  }
}
```

**Realtime dispatch:** WebSocket `order.status_changed` frame to seller.

---

### `stream:payment:orders:confirmed` — Payment Confirmed

Published when the payment webhook fires successfully.

```json
{
  "type": "order.confirmed",
  "payload": {
    "order_id":   "uuid",
    "buyer_id":   "uuid",
    "seller_id":  "uuid",
    "listing_id": "uuid"
  }
}
```

**Realtime dispatch:** WebSocket `order.status_changed` to buyer and seller.

---

### `stream:listing:status` — Listing Status Changed

Published on payment confirmed, order confirmed (sold), and order cancelled (reopen).

```json
{
  "type": "listing.status_changed",
  "payload": {
    "listing_id":       "uuid",
    "status":           "reserved | sold | active",
    "online_user_ids":  ["uuid", "uuid"],
    "conversation_ids": ["uuid", "uuid"]
  }
}
```

**Realtime dispatch mapping:**

| `status` | WebSocket type sent to client |
|---|---|
| `reserved` | `listing.reserved` |
| `active` | `listing.available` |
| `sold` | `listing.sold` |

**Delivered to:** each user in `online_user_ids`, each conversation room in `conversation_ids`, and the `listing-detail` room keyed by `listing_id`.

---

## Android Integration Notes

### Checkout flow

```
1. User taps "Mua ngay" or "Thanh toán" on chat screen
2. POST /api/v1/orders  { listing_id, amount_vnd }
3. Store returned order.ID in memory / local state
4. Navigate to payment screen → pass order_id + amount_vnd
5. Complete in-app payment → payment service calls webhook
6. Poll GET /api/v1/orders/{order_id} OR listen on WebSocket order.status_changed
7. When status = payment_held → show shipping wait screen
8. When status = in_transit  → show tracking info + "Confirm Receipt" button
9. When status = delivered_confirmed → show review prompt
```

### Disable Pay button (one order per conversation)

After order creation, the conversation's `order_id` field is set. Check before showing Pay:

```
GET /api/v1/chat/conversations/{conversation_id}
→ if conversation.order_id != null → hide/disable Pay button
```

### Order status display labels (Vietnamese)

| Status | Label |
|---|---|
| `payment_pending` | Chờ thanh toán |
| `payment_held` | Đã thanh toán – Chờ giao hàng |
| `in_transit` | Đang giao hàng |
| `delivered_confirmed` | Đã nhận hàng |
| `cancelled` | Đã hủy |
| `disputed` | Đang khiếu nại |

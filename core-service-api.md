# Core Service API Documentation

> **Auth:** All protected endpoints require `Authorization: Bearer <access_token>` header.  
> **Base URL:** Configure per environment.  
> **Content-Type:** `application/json` unless noted (file uploads use `multipart/form-data`).  
> **Prices:** All monetary values are in **VND** (Vietnamese Dong).

---

## Table of Contents

- [Auth](#auth)
- [Users](#users)
- [Listings](#listings)
- [Search](#search)
- [Chat](#chat)
- [Orders](#orders)
- [Health Check](#health-check)
- [Data Models](#data-models)
- [Error Response](#error-response)

---

## Auth

### `POST /auth/login`
Email + password login.

**Body:**
```json
{
  "email": "user@gmail.com",       // required, max 255
  "password": "password",          // required, max 72
  "application_id": "string",      // required
  "ip_address": "string",          // optional (auto-detected)
  "user_agent": "string"           // optional (auto-detected)
}
```

**Response `200`:** [`LoginResponse`](#loginresponse)

> If MFA is required, returns HTTP 200 with error code `MFA_REQUIRED`.

**Errors:** `400` invalid JSON, `401` wrong credentials, `403` inactive account, `429` rate limited.

---

### `POST /auth/social-login`
Login with a social provider (e.g. Google). Finds or creates user by verified email.

**Body:**
```json
{
  "provider": "google",
  "provider_token": "string",   // required — ID token from provider
  "application_id": "string",   // required
  "ip_address": "string",
  "user_agent": "string"
}
```

**Response `200`:** [`LoginResponse`](#loginresponse)

**Errors:** `400`, `401` invalid/unverified token, `403` inactive, `429`.

---

### `POST /auth/otp/request`
Request an email OTP. Sends code via SMTP or logs it in dev mode.

**Body:**
```json
{
  "email": "user@example.com",   // required, max 255
  "application_id": "string"     // required
}
```

**Response `200`:** `{ "ok": true }`

**Errors:** `400`, `429` too many OTP requests.

---

### `POST /auth/otp/verify`
Verify OTP and receive tokens (same response as login).

**Body:**
```json
{
  "email": "user@example.com",
  "otp": "123456",
  "application_id": "string"
}
```

**Response `200`:** [`LoginResponse`](#loginresponse)

**Errors:** `400`, `401` invalid/expired OTP, `429` rate-limited or account locked.

---

### `POST /auth/refresh` 
Rotate refresh token — returns a new access + refresh token pair.  
⚠️ Reusing a revoked token triggers reuse detection and invalidates all sessions.

**Body:**
```json
{
  "refresh_token": "string",   // required
  "application_id": "string",  // required
  "ip_address": "string",
  "user_agent": "string"
}
```

**Response `200`:** [`LoginResponse`](#loginresponse)

**Errors:** `400`, `401` invalid/expired/reused token, `403` inactive, `404` user not found, `429`.

---

### `POST /auth/logout` 🔒
Revoke current session (reads session ID from JWT claims).

**Response `200`:** `{ "message": "string" }`

**Errors:** `401`, `400`, `500`.

---

### `POST /auth/logout-all` 🔒
Revoke **all** sessions for the authenticated user.

**Response `200`:** `{ "message": "string" }`

---

### `POST /auth/fcm/register` 🔒
Register a Firebase Cloud Messaging token for push notifications. Idempotent.

**Body:**
```json
{
  "fcm_token": "string",          // required, max 256
  "device_platform": "android"    // optional, max 20
}
```

**Response `200`:** `{ "ok": true }`

---

## Users

### `POST /users/onboard` 🔒
Create profile for the authenticated user (first-time setup).

**Body:**
```json
{
  "username": "johndoe",          // required, 3–30 chars, lowercase/numbers/dots/underscores
  "aesthetic_tags": ["y2k", "..."] // optional, max 10 — seeds personalized feed
}
```

**Response `200`:** Profile object

**Errors:** `400` invalid username, `409` username taken.

---

### `GET /users/suggested-username?phone=+84912345678`
Returns a username suggestion based on last 6 digits of phone (e.g. `user_345678`). Used during onboarding.

**Query:** `phone` — required (e.g. `+84912345678`)

**Response `200`:** `{ "suggested_username": "user_345678" }`

**Errors:** `400` phone required or too short.

---

### `GET /users/me` 🔒
Get the authenticated user's own profile.

**Response `200`:** Profile object

---

### `PATCH /users/me` 🔒
Update profile fields (all optional).

**Body:**
```json
{
  "display_name": "string",     // max 50
  "bio": "string",              // max 150
  "avatar_url": "string",       // max 512
  "cover_url": "string",        // max 512
  "aesthetic_tags": ["..."]     // max 10
}
```

**Response `200`:** Updated profile object

---

### `POST /users/me/avatar` 🔒
Upload avatar image. Replaces existing avatar.

**Content-Type:** `multipart/form-data`  
**Field:** `file` — JPEG, PNG, GIF, WebP, max **5MB**

**Response `200`:** `{ "avatar_url": "https://..." }`

---

### `POST /users/me/cover` 🔒
Upload cover image. Replaces existing cover.

**Content-Type:** `multipart/form-data`  
**Field:** `file` — JPEG, PNG, GIF, WebP, max **10MB**

**Response `200`:** `{ "cover_url": "https://..." }`

---

### `GET /users/{id}`
Get public profile by username or user ID.

**Path:** `id` — username or UUID

**Response `200`:** Profile object

**Errors:** `400`, `404`.

---

### `GET /users/search` 🔒
Search users by username prefix or display name substring.  
Results ranked: exact username → partial username → display name. Blocked users excluded.

**Query:**
| Param | Type | Description |
|-------|------|-------------|
| `q` | string | **required** — search query |
| `limit` | int | Max results (default 20, max 50) |

**Response `200`:** Array of [`UserSearchResult`](#usersearchresult)

---

### `POST /users/{id}/follow` 🔒
Follow a user. Cannot follow self or blocked users.

**Response `200`:** `{ "ok": true }`

**Errors:** `400` self-follow, `403` blocked.

---

### `DELETE /users/{id}/follow` 🔒
Unfollow a user.

**Response `200`:** `{ "ok": true }`

---

### `POST /users/{id}/block` 🔒
Block a user. Silently removes mutual follow relationships.

**Response `200`:** `{ "ok": true }`

---

### `DELETE /users/{id}/block` 🔒
Unblock a user. Does **not** restore previous follow relationships.

**Response `200`:** `{ "ok": true }`

---

### `GET /aesthetic-tags`
Returns the master list of aesthetic tags (for onboarding / profile updates).

**Response `200`:** Array of tag objects

---

## Listings

### `POST /listings` 🔒
Create a new listing. Requires a complete user profile.

**Body:**
```json
{
  "title": "string",            // required, 3–60 chars
  "category_id": "uuid",        // required
  "condition": "string",        // required, max 30
  "price": 150000,              // required, 1,000–100,000,000 VND
  "image_urls": ["url1", ...],  // required, 1–6 URLs (upload first via POST /listings/images)
  "description": "string",      // optional, max 500
  "brand": "string",            // optional, max 50
  "size": "string",             // optional, max 20
  "aesthetic_tags": ["y2k"]     // optional, max 5
}
```

**Response `201`:** Created listing object

**Errors:** `400`, `403` incomplete profile.

---

### `GET /listings/{listing_id}`
Get a listing by ID. **Public endpoint.**

**Response `200`:** Listing object

**Errors:** `400`, `404`.

---

### `PUT /listings/{listing_id}` 🔒
Update a listing. Category **cannot** be changed.  
⚠️ Price changes invalidate open offers and add a system message to affected conversations.

**Body:** (all optional)
```json
{
  "title": "string",
  "condition": "string",
  "price": 200000,
  "description": "string",
  "brand": "string",
  "size": "string",
  "aesthetic_tags": ["..."]
}
```

**Response `200`:** Updated listing object

---

### `DELETE /listings/{listing_id}` 🔒
Soft-delete a listing. Fails if listing has an active order.

**Response `200`:** `{ "ok": true }`

**Errors:** `400` active order exists.

---

### `POST /listings/images` 🔒
Upload an image for use in a new listing.

**Content-Type:** `multipart/form-data`  
**Field:** `file` — JPEG, PNG, GIF, WebP, max **10MB**

**Response `200`:** `{ "image_url": "https://..." }` — use this URL in `image_urls` when creating a listing.

---

### `POST /listings/{listing_id}/like` 🔒
Toggle like on a listing (idempotent).

**Response `200`:** `{ "liked": true }` — `true` if now liked, `false` if unliked.

---

### `POST /listings/{listing_id}/save` 🔒
Toggle save (wishlist) on a listing (idempotent).

**Response `200`:** `{ "saved": true }`

---

### `POST /listings/{listing_id}/view` 🔒
Record a view. Debounced: **one count per user per listing per 24 hours**.

**Response `200`:** `{ "ok": true }`

---

### `POST /listings/{listing_id}/sold` 🔒
Mark listing as sold (outside the platform).

**Response `200`:** `{ "ok": true }`

---

### `GET /listings/home` 🔒
Home feed — listings from followed sellers, ranked by engagement × time decay.

**Query:**
| Param | Type | Description |
|-------|------|-------------|
| `limit` | int | Page size (default 20, max 50) |
| `offset` | int | Offset for pagination |

**Response `200`:** Array of listing objects

---

### `GET /listings/explore` 🔒
Explore feed — all listings ranked by heat score, with optional filters.

**Query:**
| Param | Type | Description |
|-------|------|-------------|
| `category_id` | string | Filter by category UUID |
| `tags` | string | Comma-separated aesthetic tags |
| `min_price` | int | Min price (VND) |
| `max_price` | int | Max price (VND) |
| `condition` | string | Item condition |
| `limit` | int | Page size (default 20, max 50) |
| `offset` | int | Offset |

**Response `200`:** Array of listing objects

---

### `GET /listings/wishlist` 🔒
Get the authenticated user's saved listing IDs.

**Query:** `limit` (default 20), `offset`

**Response `200`:** `{ "listing_ids": [...] }`

---

### `GET /users/{id}/listings`
Get listings by a seller's user ID. Public endpoint.

**Query:**
| Param | Type | Description |
|-------|------|-------------|
| `status` | string | `active` (default), `sold`, `deleted` |
| `limit` | int | Page size (default 20, max 50) |
| `offset` | int | Offset |

**Response `200`:** Array of listing objects

---

### `GET /categories`
Get the master list of listing categories. **Public endpoint.**

**Response `200`:** Array of category objects

---

## Search

### `GET /search/listings` 🔒
Full-text search on title, description, brand. Empty `q` = category browse.

**Query:**
| Param | Type | Description |
|-------|------|-------------|
| `q` | string | Search query |
| `category_id` | string | Category UUID |
| `tags` | string | Comma-separated aesthetic tags |
| `min_price` | int | VND |
| `max_price` | int | VND |
| `condition` | string | Item condition |
| `sort` | string | `recent` (default), `popular`, `price_asc`, `price_desc` |
| `limit` | int | Page size (default 20, max 50) |
| `offset` | int | Offset |

**Response `200`:** Array of listing objects

---

### `GET /search/autocomplete?q=<prefix>` 🔒
Suggest listing titles by first-word prefix. Returns max **5** results.

**Query:** `q` — required

**Response `200`:** Array of strings (titles)

---

### `GET /search/trending-tags` 🔒
Top 10 aesthetic tags by new listings in the last 7 days.

**Response `200`:** Array of strings (tag names)

---

## Chat

### `POST /chat/conversations` 🔒
Start or return an existing conversation between buyer and seller for a listing. **Buyer only.**

**Body:**
```json
{
  "listing_id": "uuid"   // required
}
```

**Response `200`** (existing) or **`201`** (new): Conversation object

**Errors:** `403` blocked, `404` listing not found.

---

### `GET /chat/conversations` 🔒
Get all conversations for the authenticated user, ordered by last message.

**Query:** `limit` (default 20, max 50), `offset`

**Response `200`:** Array of conversation objects

---

### `GET /chat/conversations/{conversation_id}/messages` 🔒
Get messages in a conversation. **Newest first.**

**Query:** `limit` (default 20, max 50), `offset`

**Response `200`:** Array of message objects

---

### `POST /chat/conversations/{conversation_id}/read` 🔒
Mark all messages from the other party as read.

**Response `200`:** `{ "ok": true }`

---

### `GET /chat/unread` 🔒
Get total unread message count (for inbox badge).

**Response `200`:** `{ "unread_count": 3 }`

---

### `POST /chat/messages` 🔒
Send a text message. Max **1,000 characters**.

**Body:**
```json
{
  "conversation_id": "uuid",   // required
  "content": "Hello!"          // required, max 1000 chars
}
```

**Response `200`:** Message object

---

### `DELETE /chat/messages/{message_id}` 🔒
Delete own message. Only allowed within **5 minutes** of sending. Replaced with a placeholder.

**Response `200`:** `{ "ok": true }`

**Errors:** `400` too late to delete.

---

### `POST /chat/offers` 🔒
Buyer sends a price offer. Rules: one pending offer per conversation; must be less than listing price.

**Body:**
```json
{
  "conversation_id": "uuid",   // required
  "amount_vnd": 120000         // required, min 1000 VND
}
```

**Response `200`:** Offer message object

**Errors:** `409` pending offer already exists.

---

### `POST /chat/offers/accept` 🔒
Seller accepts an offer. **Automatically creates an order.**

**Body:**
```json
{
  "conversation_id": "uuid",
  "offer_message_id": "uuid"
}
```

**Response `200`:** `{ "ok": true }`

---

### `POST /chat/offers/decline` 🔒
Seller declines an offer. Buyer can then make a new offer.

**Body:**
```json
{
  "conversation_id": "uuid",
  "offer_message_id": "uuid"
}
```

**Response `200`:** `{ "ok": true }`

---

## Orders

> **Order statuses:** `payment_pending` → `payment_held` → `in_transit` → `delivered_confirmed` | `cancelled` | `disputed`

### `POST /orders` 🔒
Create an order (checkout). Listing must be active. **10% platform fee** applied. Listing becomes reserved.

**Body:**
```json
{
  "listing_id": "uuid",     // required
  "amount_vnd": 150000      // required, min 1000 — use listing price or accepted offer price
}
```

**Response `201`:** Created order object

**Errors:** `400` listing not available, `403` blocked.

---

### `GET /orders` 🔒
Get orders for the authenticated user.

**Query:**
| Param | Type | Description |
|-------|------|-------------|
| `role` | string | **required** — `buyer` or `seller` |
| `status` | string | Optional status filter |
| `limit` | int | Page size (default 20, max 50) |
| `offset` | int | Offset |

**Response `200`:** Array of order objects

---

### `GET /orders/{order_id}` 🔒
Get a single order by ID. Buyer or seller only.

**Response `200`:** Order object

---

### `POST /orders/{order_id}/payment-confirm`
**Webhook** — called by the payment service when an order is paid.  
Transitions order to `payment_held` and sets a **5-day auto-release** timer.

**Header:** `X-Payment-Signature` — webhook signature for verification

**Response `200`:** `{ "ok": true }`

---

### `POST /orders/ship` 🔒
Seller marks order as shipped.

**Body:**
```json
{
  "order_id": "uuid",           // required
  "tracking_number": "string",  // required, max 100
  "carrier": "string"           // required, max 50
}
```

**Response `200`:** `{ "ok": true }`

---

### `POST /orders/{order_id}/confirm-handoff` 🔒
**Seller only** — confirms in-person / meetup handoff after the scheduled meetup time, when the order is `payment_held` and linked to a confirmed meetup (MEETUP / in-person fulfilment). Transitions toward `in_transit` so the buyer can confirm receipt (`POST /orders/{order_id}/confirm`).

**Response `200`:** `{ "ok": true }` or updated order object (per implementation)

**Errors:** `400` / `403` wrong role or state, `404` unknown order.

---

### `POST /orders/{order_id}/confirm` 🔒
Buyer confirms receipt. **Triggers escrow release** to seller.

**Response `200`:** `{ "ok": true }`

---

### `POST /orders/review` 🔒
Buyer submits a review (1–5 stars). Recalculates seller rating.

**Body:**
```json
{
  "order_id": "uuid",     // required
  "rating": 5,            // required, 1–5
  "comment": "string"     // optional, max 500
}
```

**Response `200`:** Submitted review object

**Errors:** `409` already reviewed.

---

### `POST /orders/dispute` 🔒
Open a dispute. Order must be in `in_transit` or `delivered_confirmed` status.

**Body:**
```json
{
  "order_id": "uuid",          // required
  "description": "string",     // required, max 2000
  "photo_urls": ["url", ...]   // optional, max 10
}
```

**Response `201`:** Dispute object

**Errors:** `409` dispute already open.

---

### `POST /orders/dispute/evidence` 🔒
Submit evidence for an open dispute (buyer or seller).

**Body:**
```json
{
  "order_id": "uuid",          // required
  "description": "string",     // required, max 2000
  "photo_urls": ["url", ...]   // optional, max 10
}
```

**Response `200`:** Updated dispute object

---

## Health Check

| Endpoint | Description | `200` | Error |
|----------|-------------|-------|-------|
| `GET /health-check/database` | DB connectivity | `"ok"` | `500` |
| `GET /health-check/kafka` | Kafka connectivity | `"ok"` | `503` |
| `GET /health-check/slack` | Slack channel health | `"ok"` | `503` |

---

## Data Models

### `LoginResponse`
```json
{
  "access_token": "eyJhbGci...",
  "refresh_token": "def50200...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "user_id": "uuid",
  "is_new_user": false,       // true → redirect to onboarding
  "unread_count": 2
}
```

### `UserSearchResult`
```json
{
  "user_id": "uuid",
  "username": "johndoe",
  "display_name": "John Doe",
  "avatar_url": "https://...",
  "follower_count": 120,
  "verified": true            // verified sellers rank higher when relevance is equal
}
```

---

## Error Response

All errors follow this shape:

```json
{
  "code": 400,
  "error": "validation error message"
}
```

### Common HTTP Status Codes

| Code | Meaning |
|------|---------|
| `400` | Bad request / validation error |
| `401` | Missing or invalid auth token |
| `403` | Forbidden (inactive account, blocked user, etc.) |
| `404` | Resource not found |
| `409` | Conflict (duplicate, already exists) |
| `429` | Rate limit exceeded |
| `500` | Internal server error |
| `503` | External dependency unavailable |

---

## Quick Reference

### Authentication Flow
1. Register/login → get `access_token` + `refresh_token`
2. Attach `Authorization: Bearer <access_token>` to all 🔒 endpoints
3. When access token expires, call `POST /auth/refresh` with `refresh_token`
4. On logout, call `POST /auth/logout` or `POST /auth/logout-all`

### Listing Lifecycle
1. Upload images → `POST /listings/images` → get `image_url`
2. Create listing → `POST /listings` with `image_urls`
3. Buyer saves/likes → `POST /listings/{id}/save` or `/like`
4. Buyer messages → `POST /chat/conversations` → `POST /chat/messages`
5. Buyer offers → `POST /chat/offers`
6. Seller accepts → `POST /chat/offers/accept` → order auto-created
7. Buyer pays → payment webhook fires `POST /orders/{id}/payment-confirm`
8. Seller ships → `POST /orders/ship`
9. Buyer confirms → `POST /orders/{id}/confirm` → escrow released
10. Buyer reviews → `POST /orders/review`
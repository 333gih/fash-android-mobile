# Listing update and delete — rules and behavior

This document describes **how updating and deleting listings works** in core-service: HTTP surface, validation, authorization, domain rules, and side effects (chat, feed, notifications). It reflects the current implementation in `internal/usecase`, `internal/delivery/http`, and `internal/domain/repositories/postgres`.

---

## HTTP surface

| Action | Method | Path | Auth |
|--------|--------|------|------|
| Update | `PUT` | `/listings/:listing_id` | Required (JWT) |
| Delete | `DELETE` | `/listings/:listing_id` | Required (JWT) |

Routes are registered under the authenticated `/listings` group (`internal/delivery/http/routes/listing/listing.route.go`).

---

## Update listing (`PUT /listings/:listing_id`)

### Purpose

Sellers may change **mutable** fields on an **active** listing. **Category** and **images** are **not** updated through this endpoint (category is fixed after create; image changes are not part of `UpdateListingRequest`).

### Preconditions (use case)

1. **Listing exists** — otherwise `404` / `LISTING_NOT_FOUND`.
2. **Caller is the seller** — `listing.SellerID` must match the authenticated user; otherwise `403` / forbidden (`ErrForbidden`).
3. **Listing status is `active`** — updates are rejected for sold, reserved, deleted, or any non-active status — `409` / `LISTING_NOT_AVAILABLE` (`ErrListingNotAvailable`).

### Request body

All fields are **optional**. Send a JSON object with only the fields to change.

| Field | Type | Rules | Semantics |
|-------|------|-------|-----------|
| `title` | `string` | HTTP: if present, 3–60 chars. Use case: trimmed; length 3–60. | Replaces title. |
| `description` | `string` | Max 500 (HTTP); use case trims and **truncates** to 500 if longer. | Replaces description. |
| `price` | `int64` | 1,000–100,000,000 (VND). | Replaces price. |
| `condition` | `string` | Max 30 (HTTP); use case trims and truncates to 30. | Replaces condition. |
| `size` | `string` | Max 20; use case truncates to 20. | Replaces size. |
| `brand` | `string` | Max 50; use case truncates to 50. | Replaces brand. |
| `aesthetic_tags` | `string[]` or omitted | HTTP: each tag max 50 chars. Use case: max **5** tags; extra entries dropped. | **`null` / field omitted** → **keep** existing tags. **Non-null** (including **`[]`**) → **replace** listing tags (empty array **clears** all tags). |

**Binding note:** `UpdateListingRequest` uses pointers so the API can distinguish “do not change tags” from “clear tags” (`internal/delivery/http/requests/listing/listing.request.go`).

### What is *not* changed via this endpoint

- **Listing status** — use dedicated flows (e.g. mark sold, order lifecycle).
- **Category** — not in update payload; documented as not changeable after creation.
- **Images** — not in update payload; creation controls 1–6 images; this endpoint does not add or replace image URLs.

### Behavior when **price** changes

When the new price **differs** from the stored price:

1. **Offer counters** — `ResetOfferCountByListingID` runs for conversations tied to this listing (new negotiation cycle).
2. **Realtime / events** — If a listing event publisher is wired, `PublishOfferLimitReset` is emitted with affected buyers/conversations so clients can re-enable the offer button without a full reload.
3. **Push notifications** — If a notifier is wired, affected buyers may receive an FCM-style message (e.g. that they can offer again after a price change).
4. **System messages** — For each **open** conversation that still has a **pending offer**, pending offers are marked expired, and a **system message** is created (Vietnamese copy: price changed). Closed conversations are skipped; conversations without pending offers are not given the expiry path in that loop (see `notifyPriceChangeToConversations` in `listing_update.usecase.go`).

### Other side effects (update)

- **Feed cache** — If feed invalidation is wired, each follower’s feed cache is invalidated so followers see the updated listing.

### Success response

- **`200`** — body: updated `Listing` object (image URLs may be signed for response).

### Typical errors

| HTTP | Code (when `AppError`) | Meaning |
|------|------------------------|---------|
| `400` | `VALIDATION` | Invalid body or field constraints (e.g. title length, price range). |
| `401` | — | Not authenticated. |
| `403` | — | Not the seller. |
| `404` | `LISTING_NOT_FOUND` | Listing does not exist. |
| `409` | `LISTING_NOT_AVAILABLE` | Listing is not `active`. |
| `500` | — | Internal error. |

---

## Delete listing (`DELETE /listings/:listing_id`)

### Purpose

**Soft-delete** a listing: set status to **`deleted`**, hide it from normal feeds/search, and run cleanup side effects. The row remains in the database for history (e.g. orders).

### Preconditions

1. **Path** — `listing_id` required; empty → `400` “listing_id required”.
2. **Listing exists** — otherwise `404` / `LISTING_NOT_FOUND`.
3. **Caller is the seller** — otherwise `403`.
4. **No blocking order** — see below.

### When delete is **not** allowed (`LISTING_NOT_DELETABLE`)

A listing **cannot** be deleted if there is at least one order for that listing with status in:

- `payment_pending`
- `payment_held`
- `in_transit`
- `disputed`

This is implemented in `listing_repository.HasActiveOrder` (`internal/domain/repositories/postgres/listing_repository.go`).  
**Note:** Some high-level docs only mention `payment_pending` and `payment_held`; the **code is stricter** and also blocks delete while the order is **`in_transit`** or **`disputed`**.

Error: **`400`** with message from `ErrListingNotDeletable` — *“listing has active order”* (code `LISTING_NOT_DELETABLE`).

### What happens on successful delete

1. **Status** — `UpdateStatus` → `deleted` (soft delete only).
2. **Images** — If an image deleter is configured, storage **delete by URL** is attempted for cover and gallery URLs. Failures are **non-fatal** (listing already deleted).
3. **Seller profile** — `UpdateCounts` with listing count **−1** (other deltas 0).
4. **Chat** — `closePendingOffers`: for conversations with **pending offers**, offers are expired; a **system message** is added: *“Sản phẩm đã bị xóa bởi người bán”*; last message is updated.
5. **Feed** — If wired, **followers’** feed caches are invalidated so the listing disappears from feeds.

### Success response

- **`200`** — `{ "ok": true }`

### Typical errors

| HTTP | Code (when `AppError`) | Meaning |
|------|------------------------|---------|
| `400` | `LISTING_NOT_DELETABLE` | Listing has an order in a blocking status (see above). |
| `400` | — | Missing `seller_id`/`listing_id` in use case validation edge cases. |
| `401` | — | Not authenticated. |
| `403` | — | Not the seller. |
| `404` | `LISTING_NOT_FOUND` | Listing does not exist. |
| `500` | — | Internal error. |

---

## Error codes reference (listing update/delete)

Defined in `pkg/apperr/error.go` (subset relevant here):

| Code | HTTP | Message (default) |
|------|------|-------------------|
| `LISTING_NOT_FOUND` | 404 | listing not found |
| `LISTING_NOT_AVAILABLE` | 409 | listing is no longer available |
| `LISTING_NOT_DELETABLE` | 400 | listing has active order |

Handlers map `AppError` to HTTP status via `writeError` (`listing.handler.go`).

---

## Related endpoints (not update/delete)

| Endpoint | Role |
|----------|------|
| `POST /listings/:listing_id/sold` | Mark sold outside platform; status change, not `PUT`. |
| `GET /listings/:listing_id` | Public fetch by ID (including non-active for history, depending on repo behavior). |
| `GET /users/:id/listings?status=...` | Seller’s listings with optional `active`, `sold`, `reserved`, `deleted`. |

---

## Source files (for maintainers)

| Concern | Location |
|---------|----------|
| Update use case | `internal/usecase/listing_update.usecase.go` |
| Delete use case | `internal/usecase/listing_delete.usecase.go` |
| HTTP handler | `internal/delivery/http/handlers/listing.handler.go` |
| Request DTOs | `internal/delivery/http/requests/listing/listing.request.go` |
| Routes | `internal/delivery/http/routes/listing/listing.route.go` |
| Active order check | `internal/domain/repositories/postgres/listing_repository.go` → `HasActiveOrder` |
| App errors | `pkg/apperr/error.go` |

---

## Changelog

- Document generated from codebase behavior; align product docs if `HasActiveOrder` semantics are intentionally different from older narrative docs.

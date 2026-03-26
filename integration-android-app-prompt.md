# Android app — integration prompt (core-service + realtime)

Use this document as a **single briefing** for Android engineers: what to implement, what to watch for, and where deeper detail lives in this repo.

---

## 1. Transport and auth

| Topic | Detail |
|-------|--------|
| **Base URL** | Configure per environment. HTTP routes are under **`{APP_API_PREFIX}/v1`** (e.g. `/api/v1` when `APP_API_PREFIX=/api`). A reverse proxy may add another prefix (`PublicPathPrefix` / Swagger — see `internal/config/app.config.go`). |
| **Auth** | `Authorization: Bearer <access_jwt>` on protected routes. Token type in login response is `Bearer` (RFC 6750). |
| **Content-Type** | `application/json` for bodies. |
| **Errors (typical)** | JSON: `{ "code": <http_status_int>, "error": "<human-readable message>" }`. The numeric `code` mirrors **HTTP status** (e.g. 400, 404). There is **no separate machine `LISTING_*` string in this JSON today** — branch on **HTTP status** + parse **`error`** for UI copy or substring checks. |
| **Success** | Many endpoints return raw models or `{ "ok": true }` — see `docs/API.md` per route. |

**Android should:** centralize an HTTP client; map 401 → login/refresh flow; avoid caching error bodies as stable API contracts for machine codes until the server exposes explicit `error_code` fields.

---

## 2. Listings — update (`PUT` … `/listings/:listing_id`)

| Topic | Detail |
|-------|--------|
| **Who** | Seller only; must be **owner** of the listing. |
| **When** | Listing must be **`active`**. Sold / reserved / deleted → **409** (`LISTING_NOT_AVAILABLE` in app code; message: *listing is no longer available*). |
| **Body** | All fields optional: `title`, `description`, `price`, `condition`, `size`, `brand`, `aesthetic_tags`. |
| **Validation** | Title 3–60 chars if sent; description ≤500; price **1,000–100,000,000 VND**; aesthetic tags max 5 when replacing. |
| **Tags semantics** | **Omit field or `null`** → keep existing tags. **`[]`** → clear all. **`["a","b"]`** → replace. |
| **Not supported here** | Category change, image add/replace, status change — other endpoints or flows. |
| **Price change** | Resets offer limits; realtime `offer.limit_reset`; optional FCM to buyers; system messages in chat for pending offers. **Android:** refresh conversation + listing UI after price update; listen on WebSocket for offer-limit events per `integration-realtime.md`. |

---

## 3. Listings — delete (`DELETE` … `/listings/:listing_id`)

| Topic | Detail |
|-------|--------|
| **Success** | **200** `{ "ok": true }`. Listing becomes **`deleted`** (soft delete); not shown in normal feeds. |
| **Strict gates** | Delete is **rejected** if **any order** exists for this listing (**any status**) **or** **any conversation** row exists (even empty chat). |
| **400 — orders** | Message like *listing has orders and cannot be deleted* (`LISTING_HAS_ORDERS` in server code). **UX:** explain that orders must be resolved / use support — seller cannot force-delete. |
| **400 — chat** | Message like *listing has conversations and cannot be deleted* (`LISTING_HAS_CONVERSATIONS`). **UX:** as soon as a buyer opens a thread, delete is blocked; product choice — do not show delete, or show disabled + explanation. |
| **403** | Not the seller. |
| **404** | Listing not found. |

**Important:** Likes/saves alone **do not** block delete. **Any** chat thread or **any** order does.

Full rules: `docs/listing-update-delete.md`.

---

## 4. Chat and orders (why delete is often blocked)

- Starting a conversation creates a row → **delete listing** may no longer be allowed.
- Creating **any** order → **delete** blocked permanently for that listing id.

Android should **not** assume** delete is always available on “my listing” screens; refresh eligibility from server errors or preflight if you add an API later.

---

## 5. Push notifications (FCM) + presence (no duplicates)

| Topic | Detail |
|-------|--------|
| **Register token** | After login: `POST` `{prefix}/v1/auth/fcm/register` with `fcm_token`, optional `device_platform`. On `onNewToken`, register again. |
| **Logout** | Tokens tied to session may be invalidated — re-register after next login. |
| **Duplicate avoidance** | While WebSocket is connected and **presence** is updated, core may **skip FCM** for that user. Connect realtime with the **same access JWT** as REST. |
| **Ping** | Send periodic `{"type":"ping"}` on WebSocket (~30s) so presence TTL stays fresh. |
| **Foreground** | If chat already shows the message via WS, **do not** show a second heads-up from FCM. |
| **Channels** | Use Android notification channels (`chat`, `orders`, …) on API 26+. |

Deep dive: `docs/integration-android-fullstack.md`, `docs/integration-realtime.md`, `docs/presence-fcm-contract.md` (if present).

---

## 6. Realtime (WebSocket) — what Android listens for

- URL pattern (realtime service): `GET /ws?token=<access_jwt>&platform=android` — **same JWT family** as core (`iss` + secret must match deployment).
- Streams/events (listing feed refresh, chat messages, offer reset, order/listing status): see **`docs/integration-realtime.md`** — frame types, payloads, and which events to ignore when `is_closed` is true on chat.

---

## 7. Images and signed URLs

- Listing images may be returned as **signed URLs** in responses; treat expiry as normal — **refresh** by re-fetching the listing if a load fails.

---

## 8. Localization

- Some server **system messages** and notification bodies are **Vietnamese**. Android may still show them as-is or map by future `message_type` / keys if you add them.

---

## 9. Testing checklist (Android)

- [ ] Bearer token on all protected listing/chat/order calls.
- [ ] Parse `{ code, error }` on failure; handle **400** delete with two different messages (orders vs conversations).
- [ ] **PUT** listing: optional fields; tag clear vs omit.
- [ ] After **price** change: UI + WS handling for offer limits.
- [ ] FCM register + WS ping + no duplicate notification when chat is open.
- [ ] Delete button: disabled or server-driven messaging when thread or order exists.

---

## 10. Source docs in this repo

| Doc | Purpose |
|-----|---------|
| `docs/API.md` | REST endpoints and examples. |
| `docs/listing-update-delete.md` | Listing PUT/DELETE rules and error codes (Go `AppError` names). |
| `docs/integration-android-fullstack.md` | FCM + presence architecture. |
| `docs/integration-realtime.md` | WebSocket contract, Redis events, payloads. |
| `scripts/cleanup-deleted-listing-chat.sql` | DB maintenance only — not used by the app. |

---

## 11. One-paragraph “prompt” you can paste into an AI or ticket

> Build/maintain the Android client against **core-service** REST and **realtime-service** WebSocket. Authenticate with **Bearer JWT**. Error JSON is **`{ "code": httpStatus, "error": string }`** — use HTTP status and message text; machine codes like `LISTING_HAS_ORDERS` are not guaranteed in JSON. **Listings:** update with optional fields only when status is **active**; **price** changes trigger offer reset and realtime/FCM side effects. **Delete listing** only succeeds if there are **no orders** and **no conversations** for that listing; otherwise **400** with an English error message. Register **FCM** after login; keep **WebSocket** connected with periodic **ping** so **presence** suppresses duplicate FCM when the user is in-app. Follow **`docs/integration-realtime.md`** for event types and **`docs/integration-android-fullstack.md`** for notification behavior.

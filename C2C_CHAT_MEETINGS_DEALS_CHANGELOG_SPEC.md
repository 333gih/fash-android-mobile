# C2C chat, meetings, offline deals & FCM — change & addition spec

This document inventories **everything to change** and **everything to add** in **core-service** (and client contracts) to implement the four requirement prompts:

1. **Chat + offer/counter** (message-only negotiation; accept does **not** create an order)  
2. **Schedule meeting** (bottom sheet → structured chat card + appointment model)  
3. **Deal record** (post-meeting “mark completed” log; unlock reviews without payment)  
4. **FCM** (triggers + server-side scheduling + deep links)

It is written to **minimize impact** on existing **paid checkout / order / escrow** flows by keeping new behavior **additive** and **parallel** where possible.

---

## Guiding rules (non-negotiable for this rollout)

- **Do not** rename or repurpose `orders` / payment fields for offline deals.  
- **Do not** add forbidden names on deal/appointment objects: `payment_status`, `paid`, `transaction_id`, `escrow`, `hold`, `deal_confirmed` (use `status` with enums as specified).  
- **Existing** `POST /chat/offers/accept` today **creates an order** — that path must remain for in-app checkout unless product retires it; Prompt 1 needs a **separate** “soft accept” path (see below).

---

## 1. Prompt 1 — Chat + offer / counter-offer (message-only)

### 1.1 Current behavior (baseline)

| Piece | Today |
|--------|--------|
| Send offer | `POST /chat/offers` → `message_type = offer`, pending state |
| Accept | `POST /chat/offers/accept` → **`CreateOrder`**, listing reserved, `conversation.order_id`, payment pipeline |
| Decline | `POST /chat/offers/decline` → offer `declined` + system message |
| Counter | **Not** a first-class API; buyer blocked while a pending offer exists |

### 1.2 **Add (new)**

| Item | Detail |
|------|--------|
| **API** | **New** endpoint e.g. `POST /chat/offers/accept-in-chat` (name TBD) — seller only, same auth/guards as today except **no** `CreateOrder`, **no** `SetOrderID`, **no** listing status change |
| **Usecase** | New `AcceptOfferInChatUsecase` (or similar): validate pending offer → set `offer_status = accepted` → insert **system** message with copy like “Offer accepted ✓ [price]” (i18n keys EN/VI) |
| **Counter — option A (minimal)** | Document-only: receiver taps Counter → client opens sheet → buyer sends **new** `POST /chat/offers` only after seller **declines** or you relax pending-offer rule for “counter” |
| **Counter — option B (recommended)** | **New** API e.g. `POST /chat/offers/counter` **or** extend send-offer with optional `counter_to_message_id` + server rules for seller-originated counter offers |
| **Message model** | If counter B: optional columns or metadata linking offers (`parent_message_id` / `counter_sequence`) — **migration** |
| **i18n** | Keys for soft-accept system message; any counter-specific copy |
| **Swagger / OpenAPI** | New operations + request/response bodies |
| **Android contract** | Per message: `message_type`, `offer_status`, who is “receiver” for Accept/Counter/Decline (client hides actions for sender) |

### 1.3 **Change (modify existing)**

| Item | Change | Risk if mishandled |
|------|--------|---------------------|
| **Routing / product** | Android must call **soft accept** for Prompt 1 and **existing accept** only for real checkout — document clearly | Wrong endpoint → order created when it should not, or opposite |
| **`SendOffer` rules** | If counter B: adjust `PENDING_OFFER_EXISTS` / who may send next | Could allow duplicate offers or break negotiation |
| **Docs** | `docs/API.md`, chat integration docs: two accept flows | Confusion for integrators |

### 1.4 **Explicitly do not change (to keep impact low)**

- **`AcceptOfferUsecase`** core logic for transactional checkout — keep as-is; do not merge with soft accept in one blind code path without feature flag.

---

## 2. Prompt 2 — Schedule meeting

### 2.1 **Add (new)**

| Layer | Addition |
|-------|----------|
| **Migration** | Table **`meeting_appointments`** (name may vary) with at least: `id`, `conversation_id`, `message_id` (unique FK → `messages.id`), `proposer_id`, `location_url`, `scheduled_at` (timestamptz), `reminder_offset_minutes` (enum: 15, 60, 1440), `reminder_enabled` (bool, default true), `status` (`pending` \| `confirmed` \| `cancelled`), `reminder_sent_at` (nullable), audit timestamps |
| **Entity** | `MeetingAppointment` (+ JSON tags for API) |
| **Repository** | CRUD + `ListDueConfirmedReminders` (for job) + batch by `message_id` for message list enrichment |
| **Message type** | New `message_type` e.g. `meeting_proposal` on `messages` (no new columns required on `messages` if card payload lives in joined appointment row) |
| **API** | `POST /chat/meetings/propose` — body: `conversation_id`, `location_url`, `scheduled_at` (RFC3339), `reminder_enabled`, `reminder_offset_minutes` |
| **API** | `POST /chat/meetings/{appointment_id}/confirm` — only **non-proposer** participant |
| **API** | `POST /chat/meetings/{appointment_id}/cancel` — for reschedule / withdrawal; rules per product |
| **Usecase** | Propose (transaction: create message + appointment), Confirm, Cancel |
| **Validation** | `location_url` must look like a maps URL (goo.gl, maps.app.goo.gl, google.com/maps, etc.) — lenient string rules |
| **GET messages** | Response enrichment: attach `meeting_appointment` object on messages of type `meeting_proposal` |
| **Realtime** | Publish `message.new` (or existing chat event) when proposal/confirm/cancel updates require push to other party |
| **Job** | Periodic worker: find `status = confirmed` + `reminder_enabled` + `reminder_sent_at IS NULL` where `scheduled_at - interval(reminder_offset_minutes)` **`<= now()`**, send FCM to **both** participants, set `reminder_sent_at` |
| **i18n + notifier** | Meeting confirmed + meeting reminder copy |
| **Swagger** | New paths and schemas |

### 2.2 **Change (modify existing)**

| Item | Change |
|------|--------|
| **`ChatHandler`** | Wire new usecases; optionally helper to merge meeting rows into `GET /chat/conversations/:id/messages` |
| **`chat_container.go`** | Construct repos + usecases + pass into handler |
| **`app` bootstrap** | Register new background job (e.g. next to `chat_jobs`) |
| **`routes`** | New routes under `/chat/...` |

### 2.3 **Client (Android) — specified in product, not in core**

- Bottom sheet UI, link preview, weekday `DD/MM · h:mm AM/PM` formatting, pills — **client**; server stores **ISO** `scheduled_at` + `reminder_offset_minutes`.

---

## 3. Prompt 3 — Deal record (“Mark as completed”)

### 3.1 **Add (new)**

| Layer | Addition |
|-------|----------|
| **Migration** | Table **`deals`** (or **`offline_deals`**) with: `id`, `product_id` (FK → `listings.id` or UUID reference), `buyer_id`, `seller_id`, `agreed_price` (bigint VND), `meeting_location_url`, `meeting_at` (timestamptz), `completed_at` (nullable until marked), `status` (`scheduled` \| `completed` \| `cancelled`), optional `conversation_id` / `meeting_appointment_id` for traceability, audit fields |
| **Entity + repository** | `Deal` + CRUD |
| **API** | `POST /deals` — create deal in `scheduled` when user confirms “mark deal flow” (product defines exact trigger, e.g. after meeting confirm) |
| **API** | `POST /deals/{id}/complete` — sets `completed_at`, `status = completed` |
| **API** | `POST /deals/{id}/cancel` — optional |
| **Review unlock** | **New** review path — e.g. `deal_reviews` table + `POST /deals/{id}/reviews` **or** extend review model with nullable `deal_id` **and** separate validation from `OrderReview` |
| **Usecase** | Create deal, complete deal, submit deal review (buyer and/or seller per product rules) |
| **Guards** | Only participants; `agreed_price` display-only; **no** calls to payment / `CreateOrder` |
| **Swagger** | New tags `deals`, `deal-reviews` |

### 3.2 **Change (modify existing)**

| Item | Change |
|------|--------|
| **Review system** | Today reviews are **order-scoped** and status-gated on **`delivered_confirmed`**. Introduce **parallel** deal reviews — **do not** overload `order_reviews` without a clear migration story |
| **Router / container** | New module registration |

### 3.3 **Explicitly forbidden on this model**

- No `payment_status`, `paid`, `transaction_id`, `escrow`, `hold` columns or JSON keys.

---

## 4. Prompt 4 — FCM push notifications

### 4.1 **Add (new)**

| # | Trigger | Recipients | Scheduling | Deep link (data payload) |
|---|---------|------------|------------|---------------------------|
| 1 | New offer card | Offer **receiver** | Immediate (on send offer — largely **already** present; verify payload) | `conversation_id`, optional `message_id` |
| 2 | Offer accepted (soft) | Offer **sender** | Immediate on soft accept | `conversation_id`, `message_id` |
| 3 | Meeting confirmed | **Both** users | Immediate on confirm | `conversation_id`, `appointment_id`, `message_id` |
| 4 | Meeting reminder | **Both** users | **Server**: `scheduled_at - reminder_offset_minutes` | same as above |
| 5 | Deal completion prompt | **Both** users | **Server**: `scheduled_at + 1 hour` if deal `status == scheduled` | `conversation_id`, `deal_id` |

### 4.2 **Add (infrastructure)**

| Item | Detail |
|------|--------|
| **Job: meeting reminders** | Covered in §2 |
| **Job: deal “mark complete” nudge** | Poll deals where `status = scheduled` and `meeting_at + 1 hour <= now()` and **nudge not yet sent** — requires `deal_reminder_sent_at` or side table to avoid duplicates |
| **FCM data envelope** | Standard keys e.g. `type`, `conversation_id`, `deeplink` — Android **must** align with app routing |
| **NotificationDispatcher** | Extend with narrow methods **or** `SendWithData(ctx, userID, title, body, data map[string]string)` to reduce duplication |

### 4.3 **Change (modify existing)**

| Item | Change |
|------|--------|
| **`infrastructure/notification/dispatcher.go`** | Implement new variants / data payload |
| **`batcher.go`** | Delegate any new methods if interface grows |

---

## 5. Cross-cutting additions

| Area | Addition |
|------|----------|
| **Errors (`pkg/apperr`)** | e.g. `MEETING_NOT_FOUND`, `MEETING_INVALID_STATE`, `INVALID_MAPS_URL`, `DEAL_NOT_FOUND`, `DEAL_NOT_COMPLETABLE`, `REMINDER_OFFSET_INVALID` |
| **i18n** | EN/VI strings for new pushes and system messages |
| **Tests** | Integration tests: soft accept, meeting propose/confirm, deal complete, reminder job idempotency |
| **Ops** | Env vars if payment-unrelated HTTP clients added later; FCM already token-based |

---

## 6. Summary: new vs changed (checklist)

### New database objects

- [ ] `meeting_appointments` (+ indexes)  
- [ ] `deals` (+ indexes)  
- [ ] `deal_reviews` (or equivalent)  
- [ ] Optional: offer linkage columns for counter-offers  
- [ ] Optional: `deal_completion_nudge_sent_at` on `deals`  

### New API routes (indicative)

- [ ] `POST /chat/offers/accept-in-chat` (soft accept)  
- [ ] `POST /chat/offers/counter` (if option B)  
- [ ] `POST /chat/meetings/propose`  
- [ ] `POST /chat/meetings/:id/confirm`  
- [ ] `POST /chat/meetings/:id/cancel`  
- [ ] `POST /deals`  
- [ ] `POST /deals/:id/complete`  
- [ ] `POST /deals/:id/cancel`  
- [ ] `POST /deals/:id/reviews` (or under `/reviews`)  

### Modified API / behavior

- [ ] `GET /chat/conversations/:id/messages` — embed `meeting_appointment` when applicable  
- [ ] `SendOffer` — only if counter-offer rules change  

### New / modified jobs

- [ ] Meeting reminder sweeper  
- [ ] Deal completion nudge sweeper  

### Unchanged (preserve)

- [ ] `POST /chat/offers/accept` → **order creation** (transactional checkout)  
- [ ] Order payment confirm webhook, escrow-related payment doc  
- [ ] `OrderReview` for **delivered** paid orders — keep; add **parallel** deal reviews  

---

## 7. Implementation order (recommended)

1. **Meeting model + APIs + GET messages enrichment + meeting FCM + reminder job** (Prompt 2 + parts of 4).  
2. **Deal model + complete + deal review + deal nudge job + FCM** (Prompt 3 + parts of 4).  
3. **Soft offer accept + counter policy + FCM** (Prompt 1 + parts of 4).  
4. Harden **deep links** and **duplicate** push prevention across all jobs.

This order avoids rewriting commerce acceptance until meetings/deals exist for the “after meetup” story.

---

## 8. Document history

| Version | Note |
|---------|------|
| 1.0 | Initial spec from product prompts 1–4; aligned with low-impact / additive strategy |
| 1.1 | Android: `CHAT_USE_SOFT_OFFER_ACCEPT` (env / `BuildConfig`), `accept-in-chat`, meetings propose/confirm/cancel, `meeting_proposal` UI; chat copy avoids “pay now” for C2C; escrow banner uses “continue in app”. |

---

## 9. Android client (fash-android-mobile)

| Item | Detail |
|------|--------|
| **Env** | `CHAT_USE_SOFT_OFFER_ACCEPT=true|false` → `BuildConfig.CHAT_USE_SOFT_OFFER_ACCEPT`. Dev default `true` (C2C); prod default `false` until backend rollout. |
| **Soft accept** | `POST .../chat/offers/accept-in-chat` via [ChatRepository.acceptOfferInChat]. Legacy `POST .../offers/accept` when flag is `false`. |
| **Meetings** | `propose` / `confirm` / `cancel` under `api/v1/chat/meetings/...`; messages typed `meeting_proposal` parse nested `meeting_appointment`. |
| **UX** | Post–soft-accept **Agreed price** strip + **Schedule meetup** sheet (maps URL, ISO time, reminder offset). Escrow `payment_pending` banner CTA: “Hoàn tất trên ứng dụng” / “Continue in app”. |
| **Deals / FCM** | Deal record APIs and FCM data keys: wire when core-service exposes them; follow §3–§4 for paths and payloads. |

---

*Maintainers: keep this file in sync with `cmd/docs`, `docs/API.md`, and Android integration docs when routes are implemented.*

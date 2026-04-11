# Order + meetup flow — improvement spec

This document aligns product requirements with the **core-service** implementation.

**Implemented in code (see migration `018_order_meetup_deadline.sql`):**

- `orders.meeting_appointment_id`, `orders.meetup_deadline_at`
- On **meetup confirm**: link order + set `meetup_deadline_at = scheduled_at + 30m` only if order is still `payment_pending`; if already `payment_held`, only the meetup link is stored (no payment deadline).
- **Job** `meetup-payment-deadline` (every 2m): cancels unpaid orders past `meetup_deadline_at` (same side effects as payment expiry).
- **15-minute** unpaid expiry **skips** orders that have `meetup_deadline_at` set (they use the meetup-based deadline only).
- **`POST /orders/:order_id/confirm-handoff`** (seller): after `scheduled_at`, `payment_held` + linked meetup → `in_transit` (`MEETUP` / `in_person`).
- **Meeting reminder job**: FCM + **email** (verified addresses) via `MailSender.SendMeetingReminderEmail` when SMTP is configured.

## 1. Current behavior (as implemented)

| Step | Behavior |
|------|----------|
| Offer finalized (seller accepts) | `AcceptOfferUsecase` creates an **order** immediately (`CreateOrder` → `payment_pending`) and links `conversations.order_id`. |
| Payment | Buyer pays; webhook `POST /orders/:id/payment-confirm` moves order forward (typ. `payment_held`). |
| Payment not completed | Job `payment-expiry` cancels `payment_pending` orders after **15 minutes** from `created_at` (`OrderPaymentExpiryUsecase`). |
| Meetup | `POST /chat/meetings/propose` → other party `POST .../confirm` or `cancel`. Stored in `meeting_appointments` with `scheduled_at`, `reminder_offset_minutes`, `reminder_enabled`. |
| Meetup reminder | Job `meeting-reminder` (every **5m**) sends **FCM + email** (verified user emails) when `scheduled_at - offset <= now` (`MeetingReminderJobUsecase`). |
| Delivery | Seller `POST /orders/ship` → `in_transit`. Buyer `POST /orders/:id/confirm` → `delivered_confirmed` (only if `in_transit`). |

**Gap:** Orders are **not** linked to `meeting_appointments`. Meetup times do **not** change payment deadlines or order cancellation rules. Buyer “confirm” today means **received shipment**, not **meetup completed**.

---

## 2. Requirements (interpreted)

1. **After offer is finalized → order created immediately** — already true on `POST /chat/offers/accept` (when checkout enabled). No change unless you also want **`accept-in-chat`** to create an order (today it does not).

2. **After meetup is set up → order deadline = scheduled meet time + 30 minutes**  
   - Needs a **stored deadline** on the order (or derived from linked meeting) once the meetup is **confirmed**.  
   - Clarify what the deadline governs: **payment must complete by then**, **handoff must be confirmed**, or **both** (see §6).

3. **Past that deadline → order auto-cancelled**  
   - New or extended background job: cancel orders where `now > meetup_deadline_at` and status is still in a **cancellable** set (e.g. `payment_pending` and/or a new “waiting for meetup” state).

4. **Past scheduled time → seller may still confirm**  
   - Today `POST /orders/:id/confirm` is **buyer-only** and requires `in_transit`. You likely need a **separate** action: **seller confirms meetup / handoff** (name it e.g. `POST /orders/:id/confirm-meetup` or `seller-confirm-handoff`) with rules:  
     - Allowed only **after** `meeting.scheduled_at` (or after meetup window).  
     - Allowed even **after** the +30 minute deadline if product allows “late confirmation” — specify explicitly.

5. **On confirm → order moves to the correct state**  
   - Define the state machine: e.g. after seller handoff confirm → `payment_held` / `in_transit` / custom `meetup_completed` → then existing ship/confirm or merged steps.

6. **Automatic email reminder at meetup reminder time**  
   - Today: FCM only. **Improvements:** extend `MailSender` (or a dedicated `TransactionalMailer`) with `SendMeetingReminder(to, locale, appointment)`; call from the **same** job window as `ListDueConfirmedReminders` (or a parallel pass), using user **email** from `users` / profile. Respect `reminder_offset_minutes` and `reminder_enabled` (already on `meeting_appointments`).

---

## 3. Data model improvements

Add to **`orders`** (migration):

| Column | Type | Purpose |
|--------|------|---------|
| `meeting_appointment_id` | `UUID NULL REFERENCES meeting_appointments(id) ON DELETE SET NULL` | Links order to the active confirmed meetup for this deal. |
| `meetup_deadline_at` | `TIMESTAMPTZ NULL` | `scheduled_at + 30 minutes` when meetup is confirmed (or set by job once). Indexed for cancellation job. |
| Optional: `meetup_scheduled_at` | `TIMESTAMPTZ NULL` | Denormalized copy for reporting if you prefer not to join every time. |

Optional: **`email_reminder_sent_at`** on `meeting_appointments` is already `reminder_sent_at` for push; for **email**, either reuse the same flag (one shot for both channels) or add `email_reminder_sent_at` if push and email must be independent.

**SQL sketch:**

```sql
ALTER TABLE orders
  ADD COLUMN IF NOT EXISTS meeting_appointment_id UUID REFERENCES meeting_appointments(id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS meetup_deadline_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_orders_meetup_deadline
  ON orders (meetup_deadline_at)
  WHERE meetup_deadline_at IS NOT NULL AND status IN ('payment_pending', 'payment_held'); -- adjust statuses
```

Populate `meetup_deadline_at` when:

- Meetup moves to **confirmed**, and  
- Conversation has `order_id` set → load order, set `meeting_appointment_id`, set `meetup_deadline_at = scheduled_at + interval '30 minutes'`.

---

## 4. API / use case improvements

1. **`ConfirmMeetingUsecase` (or follow-up service)**  
   After successful confirm, if `conv.order_id` is set: load order, load appointment, set `orders.meeting_appointment_id` and `meetup_deadline_at`.  
   Optionally **replace** or **extend** the 15-minute payment window for meetup-tied orders (product decision).

2. **Auto-cancel job**  
   - New use case: `OrderMeetupDeadlineExpiryUsecase` (or extend `OrderPaymentExpiryUsecase`):  
     - Query orders where `meetup_deadline_at < now()` and status in (`payment_pending`, …) → cancel order, reopen listing/conversation like existing payment expiry.  
   - Register in `order_jobs.go` (e.g. every **1–5 minutes** if deadlines are tight).

3. **Seller confirm after scheduled time**  
   - New endpoint + use case (seller-only): validates order linked to conversation/meeting, `now >= scheduled_at`, role seller, transitions to the **agreed** next status.  
   - Do **not** overload buyer `ConfirmOrderUsecase` without renaming — it would confuse `in_transit` vs meetup.

4. **Offer accept**  
   - Already creates order immediately. Ensure product allows meetup **after** order exists; link fields when meetup is confirmed (§4.1).

5. **GET order / list**  
   - Expose `meetup_deadline_at`, `meeting_appointment_id` (or nested meeting summary) for UI countdown.

---

## 5. Notification improvements

| Channel | Current | Improvement |
|---------|---------|-------------|
| Push | Meeting reminder, payment, offers | Keep; ensure deep links include `order_id` when linked. |
| Email | OTP / auth only (`MailSender`) | Add template + `SendMeetingReminderEmail(userIDs…)`; load emails from DB; send in job next to FCM. Idempotent with `reminder_sent_at` or new column. |

**Config:** Reuse `MAIL_*` env; add HTML/text templates under `internal/infrastructure/mail/templates/` if you use embedded templates.

---

## 6. Decisions you must lock (product)

1. **What does `meetup_deadline_at` enforce?**  
   - A) Payment must be captured before deadline.  
   - B) Seller/buyer must “complete meetup” before deadline.  
   - C) Both (two deadlines).

2. **If the 30-minute window passes without payment**, cancel order only, or also **invalidate meetup**?

3. **Seller “confirm” after scheduled time** — does it:  
   - Release escrow / mark goods handed over, or  
   - Only unlock the next step (buyer still confirms receipt separately)?

4. **Checkout off** (`ORDER_CHECKOUT_ENABLED=false`): orders may not exist from chat accept — meetup deadline may apply only to **deals**; scope separately if needed.

---

## 7. Implementation order (recommended)

1. Migration: `orders.meeting_appointment_id`, `meetup_deadline_at`.  
2. On meeting confirm: populate fields + index.  
3. Job: auto-cancel past `meetup_deadline_at` (clear rules).  
4. New seller meetup/handoff confirm API + state transitions + i18n + `error_code`.  
5. Email: extend mailer + wire meeting reminder job.  
6. Tests: use cases + repository queries for deadline listing.

---

## 8. Files likely to touch (reference)

- `internal/usecase/chat_confirm_meeting.usecase.go` — link order + deadline.  
- `internal/usecase/order_payment_expiry.usecase.go` or new expiry use case.  
- `internal/infrastructure/jobs/order_jobs.go` — schedule new job.  
- `internal/domain/repositories/postgres/order_repository.go` — queries by `meetup_deadline_at`.  
- `internal/delivery/http/handlers/order.handler.go` — new route + existing GET payload.  
- `internal/infrastructure/mail/*` — email templates + sender.  
- `internal/usecase/meeting_reminder_job.usecase.go` — add email sends.  
- `internal/i18n/messages_*.go` — new strings.

This spec is the full improvement list; implement in phases after product signs off on §6.

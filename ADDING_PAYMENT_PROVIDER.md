# Adding a payment gateway (wallet, bank, aggregator)

Use this checklist when integrating a new channel (e.g. another bank after TPBank).

## 1. Identifier

- Choose a stable channel id: **lowercase**, `a-z` first character, then `a-z0-9_`, length **2–64** (matches DB and `internal/validation.PaymentMethodID`).
- Examples: `tpbank`, `vietcombank`, `napas`.

## 2. Provider package

- Create `internal/provider/<id>/client.go` implementing `internal/provider.PaymentProvider`:
  - `Name()` returns the same string as the id (e.g. `"vietcombank"`).
  - **Do not hardcode** URLs, timeouts, JSON field names, or success codes in Go — load them from env via `internal/config` (see **TPBank**: `FromConfig`, `Settings`, and `.env.example` `TPBANK_*` keys).
  - `Initiate` — call the bank’s create-order URL from env.
  - `VerifyWebhook` / `ParseWebhook` — header names and JSON keys from env.
  - `Refund` — call their refund API when available.

## 3. Registry

- In `internal/provider/registry/registry.go`, register the client inside `Build(cfg *config.Config)` when `cfg.<Provider>Enabled && cfg.<Provider>Configured()`.

## 4. Config

- Add fields to `internal/config.Config`, load from env in `Load()`, and:
  - `Validate()` — if `*_ENABLED=true`, require credentials.
  - `RegisteredProviderNames()` — append the id when enabled and configured.

## 5. HTTP / callbacks

- **No new route** — clients and banks use `POST /api/webhooks/{provider}` with `provider` equal to the channel id (e.g. `/api/webhooks/vietcombank`).
- `writeWebhookAck` in `internal/adapter/http/handler/webhook.go` — add a `case` if the provider needs a non-JSON response (like VNPay `OK`/`ERR`); otherwise the `default` branch applies.

## 6. Database

- Migration `000002_payment_method_expand.up.sql` widens `payment_method` to accept new ids. **Existing databases** created before that migration must run the SQL once (same file as in `internal/infrastructure/database/migrations/`).

## 7. Core / clients

- `payment_method` in initiate requests must carry the new id. Core and mobile apps should list only enabled gateways from your config or a separate catalog endpoint.

## 8. Swagger

- Run `swag init -g cmd/main.go -o docs --parseDependency --parseInternal` after changing annotations.

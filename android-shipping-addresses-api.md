# Shipping addresses API — Android integration

This document describes **core-service** endpoints for **saved shipping addresses** (ship-from / pickup addresses tied to the authenticated user). Use these when building listing flows, checkout, or address pickers.

**Related:** Listing APIs may reference an address by `shipping_address_id` and can embed a `shipping_address` object on responses (see [Listing payload](#listing-payload-shipping-fields)).

---

## Base URL and auth

| Item | Detail |
|------|--------|
| **Typical base** | `https://<host>/<prefix>/<lang>/api/v1` — e.g. `https://example.com/core-service/en/api/v1` |
| **Legacy (no locale)** | `https://<host>/<prefix>/api/v1` |
| **Auth** | `Authorization: Bearer <access_token>` (required for all shipping-address routes) |

All routes below are under **`/users`** and require a valid JWT.

---

## Data model (semantic fields)

A saved address is stored as **`user_shipping_addresses`** with:

| Field | Type | Notes |
|-------|------|--------|
| `id` | UUID string | Server-generated |
| `user_id` | UUID string | Owner (redundant in “me” APIs) |
| `label` | string | e.g. “Home”, “Office” |
| `recipient_name` | string | Contact name |
| `line1` | string | **Required** on create |
| `line2` | string | Optional second line |
| `city` | string | |
| `region` | string | State / region |
| `postal_code` | string | |
| `country_code` | string | **ISO 3166-1 alpha-2**, 2 chars (e.g. `VN`) |
| `is_default` | boolean | Only one default per user (enforced on create/set default) |
| `province_id` | UUID string or null | Vietnam catalog IDs from **common-service** (stored only; no FK in core) |
| `province_name` | string | Display |
| `district_id` | UUID string or null | |
| `district_name` | string | |
| `ward_id` | UUID string or null | |
| `ward_name` | string | |
| `created_at` | ISO 8601 / RFC3339 | |
| `updated_at` | ISO 8601 / RFC3339 | |

**Note:** List and create handlers return the **`UserShippingAddress`** entity from Go. If JSON keys appear in **PascalCase** (`ID`, `UserID`, …), align your client with the live response or OpenAPI `cmd/docs/swagger.json`. Prefer mapping to the snake_case names above in your models for consistency with listing payloads.

---

## 1. List shipping addresses

Returns all saved addresses for the current user.

| | |
|---|---|
| **Method / path** | `GET /users/me/shipping-addresses` |
| **Success** | `200 OK` |
| **Body** | JSON **array** of address objects (see [Data model](#data-model-semantic-fields)) |

**Errors**

| HTTP | When |
|------|------|
| `401` | Missing or invalid token |
| `500` | Internal / shipping module unavailable |

---

## 2. Create shipping address

Creates a new saved address. If `is_default` is `true`, the server clears default on other rows for this user (see repository create logic).

| | |
|---|---|
| **Method / path** | `POST /users/me/shipping-addresses` |
| **Content-Type** | `application/json` |
| **Success** | `201 Created` |
| **Body** | Created address object (same shape as list items) |

### Request body (`CreateShippingAddressRequest`)

All JSON keys are **snake_case**.

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `line1` | string | **Yes** | max 200 |
| `country_code` | string | **Yes** | exactly **2** characters (e.g. `VN`) |
| `label` | string | No | max 50 |
| `recipient_name` | string | No | max 100 |
| `line2` | string | No | max 200 |
| `city` | string | No | max 100 |
| `region` | string | No | max 100 |
| `postal_code` | string | No | max 20 |
| `is_default` | boolean | No | default `false` |
| `province_id` | string (UUID) or null | No | valid UUID if set |
| `province_name` | string | No | max 255 |
| `district_id` | string (UUID) or null | No | valid UUID if set |
| `district_name` | string | No | max 255 |
| `ward_id` | string (UUID) or null | No | valid UUID if set |
| `ward_name` | string | No | max 255 |

### Example request

```json
{
  "label": "Nhà riêng",
  "recipient_name": "Nguyen Van A",
  "line1": "123 Đường ABC",
  "line2": "",
  "city": "TP.HCM",
  "region": "South",
  "postal_code": "700000",
  "country_code": "VN",
  "is_default": true,
  "province_id": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  "province_name": "Ho Chi Minh",
  "district_id": "bbbbbbbb-cccc-dddd-eeee-ffffffffffff",
  "district_name": "District 1",
  "ward_id": "cccccccc-dddd-eeee-ffff-000000000000",
  "ward_name": "Ward 1"
}
```

**Errors**

| HTTP | When |
|------|------|
| `400` | Validation / bind failure (`common.invalid_request`) |
| `401` | Not authenticated |
| `500` | Internal error |

---

## 3. Set default shipping address

Marks one address as default; others for the user become non-default.

| | |
|---|---|
| **Method / path** | `PATCH /users/me/shipping-addresses/{addr_id}/default` |
| **Path param** | `addr_id` — UUID of an address that **belongs to the current user** |
| **Success** | `200 OK` |

### Response body

```json
{
  "ok": true
}
```

**Errors**

| HTTP | When |
|------|------|
| `400` | Missing `addr_id` |
| `401` | Not authenticated |
| `404` | Address not found or not owned (`SHIPPING_ADDRESS_NOT_FOUND` / i18n message) |
| `500` | Internal error |

---

## Listing payload (shipping fields)

When **creating** or **updating** a listing, you may send:

| Field | Location | Description |
|-------|----------|-------------|
| `shipping_address_id` | JSON body | UUID string of one of **your** saved addresses (`GET /users/me/shipping-addresses`). Must belong to the seller. |

Listing **responses** (`ListingResponse`) may include:

| Field | Type | Description |
|-------|------|-------------|
| `shipping_address_id` | string or omitted | UUID when set |
| `shipping_address` | object or omitted | Embedded summary when preloaded |

### `shipping_address` summary shape (listing response)

Matches **`ShippingAddressSummary`** — snake_case:

| Field | Type |
|-------|------|
| `id` | string (UUID) |
| `label` | string |
| `recipient_name` | string |
| `line1`, `line2` | string |
| `city`, `region`, `postal_code`, `country_code` | string |
| `is_default` | boolean |
| `province_id`, `district_id`, `ward_id` | string or null |
| `province_name`, `district_name`, `ward_name` | string (may be omitted if empty) |

---

## Android integration checklist

1. **Auth:** Attach `Authorization: Bearer <token>` to every call above.
2. **Locales:** Include `en` or `vi` in the path if your gateway uses localized routes (`.../en/api/v1/...`).
3. **Vietnam structured address:** Load province/district/ward from **common-service**; send IDs + display names into create body as shown.
4. **Listing create:** Pick `id` from list response and send `shipping_address_id` in the listing create JSON (when the backend expects it).
5. **Errors:** Parse JSON error payloads from the app’s standard wrapper (e.g. `code`, `error` / localized message key) — see `pkg/apperr` and i18n keys in the service.

---

## Source of truth in repo

| Area | Path |
|------|------|
| HTTP routes | `internal/delivery/http/routes/user/user.route.go` |
| Handlers | `internal/delivery/http/handlers/user.handler.go` |
| Create request DTO | `internal/delivery/http/requests/user/shipping_address.request.go` |
| Use case | `internal/usecase/shipping_address.usecase.go` |
| Entity | `internal/domain/entites/postgresql/user_shipping_address.entity.go` |
| Listing embed | `internal/delivery/http/responses/listing/listing.response.go` (`ShippingAddressSummary`) |

---

## OpenAPI

Generated Swagger: run `make swagger` (see `Makefile`), then open `cmd/docs/swagger.yaml` / Swagger UI `/docs` for machine-readable schemas (`user_request.CreateShippingAddressRequest`, etc.).

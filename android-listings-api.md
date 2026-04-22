# Listings API — Android integration (core-service)

This document describes **HTTP requests, responses, and behavior** for listing-related endpoints exposed by **core-service**. Catalog masters (categories, brands, countries, Vietnam administrative units, aesthetic tags) are maintained in **common-service**; core stores **IDs plus denormalized names** on listings so clients can render listings without calling common-service for every read.

- **JSON field names:** `snake_case` everywhere.
- **Money:** `price`, `floor_price`, and filter `min_price` / `max_price` are integer **VND** (same units as validation on create: 1,000–100,000,000 where applicable).
- **IDs:** UUID strings unless noted otherwise.

---

## Base URL and locale

Routes are mounted under **`{api_prefix}/v1`**, typically `api/v1` (see `APP_API_PREFIX`).

| Style | Example base |
|--------|----------------|
| **Localized** | `https://{host}/{en\|vi}/api/v1` |
| **Legacy (defaults to `en`)** | `https://{host}/api/v1` |

If the gateway uses a public prefix (e.g. `APP_PUBLIC_PREFIX` / `PublicPathPrefix`), the same routes also exist under:

`https://{host}{prefix}/{en|vi}/api/v1/...` and `https://{host}{prefix}/api/v1/...`

Invalid locale in the path (not `en` or `vi`) returns **404** with body `{ "code": 404, "error": "unsupported locale; use en or vi" }`.

---

## Authentication

| Header | Value |
|--------|--------|
| `Authorization` | `Bearer <access_token>` |

Protected routes return **401** if the token is missing or invalid. Error body shape: [Error response](#error-response).

**Public (no auth):**

- `GET .../listings/{listing_id}`
- `GET .../users/{id}/listings`

All other endpoints in this document require authentication unless stated otherwise.

---

## Error response

Most errors use:

```json
{
  "code": 400,
  "error": "Human-readable message (locale-aware when using i18n keys on the server)"
}
```

`code` matches the HTTP status in typical cases. Validation failures often return **400** with a generic invalid-request message depending on server configuration.

---

## Shared type: `ListingResponse`

Returned by create, update, get, feeds, search, and seller listing list. Optional objects may be omitted or empty arrays as implemented.

| Field | Type | Notes |
|-------|------|--------|
| `id` | string | Listing UUID |
| `seller_id` | string | Seller user UUID |
| `title` | string | |
| `cover_image_url` | string | First image; may be signed URL |
| `image_urls` | string[] | All images |
| `price` | number (int64) | VND |
| `condition` | string | |
| `description` | string | |
| `size` | string | |
| `status` | string | See [Listing status](#listing-status) |
| `like_count` | number | |
| `save_count` | number | |
| `view_count` | number | |
| `measurement_unit` | string | |
| `measurement_hem` | number \| null | |
| `measurement_chest` | number \| null | |
| `measurement_length` | number \| null | |
| `measurement_shoulders` | number \| null | |
| `measurement_sleeve_length` | number \| null | |
| `accept_offers` | boolean | |
| `auto_price_drop_enabled` | boolean | |
| `floor_price` | number \| null | VND |
| `price_drop_percent` | number | |
| `next_price_drop_at` | string \| null | RFC3339 UTC when set |
| `shipping_address_id` | string \| null | UUID of seller ship-from address |
| `created_at` | string | RFC3339 UTC |
| `updated_at` | string | RFC3339 UTC |
| `parent_category` | object \| omitted | `{ "id", "name" }` |
| `category` | object | `{ "id", "name" }` — leaf category |
| `brand` | object \| omitted | `{ "id"?, "name" }` |
| `country` | object \| omitted | `{ "id"?, "iso2", "name" }` |
| `aesthetic_tags` | array | `{ "id", "name", "display_name" }[]` — may be `[]` |
| `seller` | object \| omitted | See `SellerSummary` below |
| `shipping_address` | object \| omitted | See `ShippingAddressSummary` below |

### `SellerSummary` (when present)

| Field | Type |
|-------|------|
| `user_id` | string |
| `username` | string |
| `display_name` | string |
| `avatar_url` | string |
| `cover_url` | string |
| `following_count` | number |
| `follower_count` | number |
| `listing_count` | number |
| `average_rating` | number |
| `verified` | boolean |

### `ShippingAddressSummary` (when present)

| Field | Type |
|-------|------|
| `id` | string |
| `label` | string |
| `recipient_name` | string |
| `line1`, `line2` | string |
| `city`, `region`, `postal_code`, `country_code` | string |
| `is_default` | boolean |
| `province_id`, `district_id`, `ward_id` | string \| null |
| `province_name`, `district_name`, `ward_name` | string |

---

## Listing status

Used in `ListingResponse.status` and in `GET .../users/{id}/listings?status=`.

| Value | Meaning |
|-------|---------|
| `active` | Live listing |
| `sold` | Marked sold |
| `reserved` | Active order exists |
| `deleted` | Soft-deleted |

---

## Endpoints

### 1. Upload listing image (multipart)

**`POST`** `{base}/listings/images`

**Auth:** required.

**Content-Type:** `multipart/form-data`

| Part | Type | Required |
|------|------|----------|
| `file` | file | Yes — JPEG, PNG, GIF, WebP; max size enforced server-side (e.g. 10MB) |

**200** body:

```json
{
  "image_url": "https://... or /api/v1/uploads/..."
}
```

If presigning is enabled, a `presigned_url` field may also be present for direct display.

Use returned URL(s) in `image_urls` when creating a listing.

---

### 2. Create listing

**`POST`** `{base}/listings`

**Auth:** required.

**Content-Type:** `application/json`

**Body:** `CreateListingRequest`

| Field | Type | Required | Validation / notes |
|-------|------|----------|---------------------|
| `title` | string | Yes | 3–60 chars |
| `image_urls` | object[] | Yes | 1–20 **listing image steps** (`step_key`, `label`, optional `label_vi`, `sort_order`, `required`, `image_url`) — upload images first via `POST /listings/images` |
| `price` | number | Yes | 1000–100000000 (VND) |
| `condition` | string | Yes | max 30 |
| `category` | object | Yes | **`NamedRef`:** `{ "id": "<leaf UUID>", "name": "<max 100>" }` — leaf category (not top-level `category_id`) |
| `parent_category` | object | No | **`NamedRef`:** `{ "id", "name" }` when the leaf has a parent in the tree |
| `description` | string | No | max 500 |
| `size` | string | No | max 20 |
| `brand` | object | No | **`BrandRef`:** `{ "id": "<UUID>", "name": "<max 255>" }` (not `brand_id` / `brand_name`) |
| `aesthetic_tags` | object[] | No | **`NamedRef[]`:** each `{ "id", "name" }` from common-service catalog (max items per policy); not `aesthetic_tag_ids` alone |
| `country_of_origin` | string | No | ISO2 length 2 if set |
| `country_id` | string | No | UUID |
| `country_name` | string | No | max 128 |
| `measurement_unit` | string | No | max 8 |
| `measurement_hem`, `measurement_chest`, `measurement_length`, `measurement_shoulders`, `measurement_sleeve_length` | number \| null | No | |
| `accept_offers` | boolean \| null | No | |
| `auto_price_drop_enabled` | boolean \| null | No | |
| `floor_price` | number \| null | No | 1000–100000000 if set |
| `price_drop_percent` | number \| null | No | 1–50 if set |
| `shipping_address_id` | string \| null | No | UUID — seller’s saved address |

**201** body: `ListingResponse`

**Errors:** 400 validation, 401, 403 profile incomplete, 500, etc.

---

### 3. Update listing

**`PUT`** `{base}/listings/{listing_id}`

**Auth:** required (seller).

**Body:** `UpdateListingRequest` — all fields optional; only sent fields are updated.

| Field | Type | Notes |
|-------|------|--------|
| `title` | string | 3–60 |
| `description` | string | max 500 |
| `price` | number | VND range as create |
| `condition` | string | max 30 |
| `size` | string | max 20 |
| `brand_id`, `brand_name` | string | UUID / snapshot |
| `aesthetic_tags` | string[] \| null | **null** = keep existing; **non-null** = replace (empty array clears) |
| `aesthetic_tag_ids` | string[] \| null | Same semantics; IDs take precedence over names |
| `country_of_origin`, `country_id`, `country_name` | … | |
| `measurement_*` | number \| null | Same as create |
| `accept_offers`, `auto_price_drop_enabled` | boolean \| null | |
| `floor_price`, `price_drop_percent` | number \| null | |
| `shipping_address_id` | string \| null | |

Category fields are **not** changeable via this API (per server behavior).

**200** body: `ListingResponse`

---

### 4. Get listing by ID (public)

**`GET`** `{base}/listings/{listing_id}`

**Auth:** none.

**200** body: `ListingResponse` (includes aesthetic tags and signed image URLs when applicable).

**404** if not found.

---

### 5. Seller’s listings (public)

**`GET`** `{base}/users/{id}/listings`

**Auth:** none.

| Query | Default | Notes |
|-------|---------|--------|
| `status` | `active` | `active` \| `sold` \| `reserved` \| `deleted` |
| `limit` | 20 | max 50 |
| `offset` | 0 | Pagination |

**200** body: `ListingResponse[]`

---

### 6. Home feed

**`GET`** `{base}/listings/home`

**Auth:** required.

| Query | Default | Notes |
|-------|---------|--------|
| `limit` | 20 | max 50 |
| `offset` | 0 | |

**200** body: `ListingResponse[]` — from followed sellers, ranked by engagement × time decay.

---

### 7. Toggle like

**`POST`** `{base}/listings/{listing_id}/like`

**Auth:** required.

**200:**

```json
{ "liked": true }
```

`liked` is **true** if the listing is liked **after** the toggle.

---

### 8. Toggle save (wishlist)

**`POST`** `{base}/listings/{listing_id}/save`

**Auth:** required.

**200:**

```json
{ "saved": true }
```

---

### 9. Record view

**`POST`** `{base}/listings/{listing_id}/view`

**Auth:** required.

**Behavior:** Debounced server-side (e.g. one counted view per user per listing per 24h).

**200:**

```json
{ "ok": true }
```

---

### 10. Wishlist listing IDs

**`GET`** `{base}/listings/wishlist`

**Auth:** required.

| Query | Default |
|-------|---------|
| `limit` | 20 |
| `offset` | 0 |

**200:**

```json
{
  "listing_ids": ["uuid1", "uuid2"]
}
```

Use `GET /listings/{id}` per ID (or batch in a future API if added) to load full `ListingResponse` objects.

---

### 11. Delete listing (soft)

**`DELETE`** `{base}/listings/{listing_id}`

**Auth:** required (seller).

**200:**

```json
{ "ok": true }
```

Fails if listing has orders or conversations tied to it (per business rules).

---

### 12. Mark sold

**`POST`** `{base}/listings/{listing_id}/sold`

**Auth:** required (seller).

**200:**

```json
{ "ok": true }
```

---

## Search and discovery (listing-shaped responses)

Base path: `{base}/search/...` (same `v1` prefix and auth as other protected routes).

### Search listings

**`GET`** `{base}/search/listings`

**Auth:** required.

| Query | Description |
|-------|-------------|
| `q` | Search text (title, description, brand). Empty `q` → browse / heat-ranked listing set (implementation-dependent). |
| `category_id` | Category UUID filter |
| `tags` | Comma-separated **aesthetic tag names** (whitespace trimmed per token) |
| `min_price`, `max_price` | VND bounds (integers ≥ 0) |
| `condition` | Condition string filter |
| `sort` | `recent` (default), `popular`, `price_asc`, `price_desc` |
| `limit` | default 20, max 50 |
| `offset` | default 0 |

Non-empty `q` on the **first page** (`offset=0`) may be logged for recent/trending query features.

**200** body: `ListingResponse[]`

---

### Autocomplete (titles)

**`GET`** `{base}/search/autocomplete?q=...`

**Auth:** required.

**200:** `string[]` — up to ~5 listing titles (prefix behavior as implemented).

---

### Recent search queries

**`GET`** `{base}/search/recent-queries`

**Auth:** required.

**200:** `string[]` — normalized queries for the current user (e.g. up to 20).

---

### Trending search queries

**`GET`** `{base}/search/trending-queries`

**Auth:** required.

**200:** array of objects with query statistics (e.g. `query`, `count` — exact shape follows server DTO).

---

### Trending aesthetic tags

**`GET`** `{base}/search/trending-tags`

**Auth:** required.

**200:** `string[]` — tag names for discovery (not full listing search).

---

## Operational / dev-only endpoints

These are **not** for normal Android builds unless you explicitly enable them in environment:

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `.../listings/bulk-seed` | Dev bulk create (feature flag) |
| `POST` | `.../listings/typesense/reindex` | Search index backfill (feature flag) |

---

## Android checklist

1. **Use `snake_case`** in JSON for all request and response mapping (e.g. Kotlin with `@SerializedName` or Moshi/ kotlinx.serialization names).
2. **Locale:** Prefer `/{en|vi}/api/v1` for correct translations on error messages where the server uses i18n.
3. **Images:** Upload first via `POST /listings/images`, then pass returned URLs inside each `image_urls[]` step object on create (see `image_urls` row above).
4. **Catalog:** Resolve category/brand/tag/country/address choices using **common-service** (or cached data). On **create**, send nested `category`, optional `parent_category` and `brand`, and `aesthetic_tags` as `{ "id", "name" }` objects matching core `listing_request.CreateListingRequest` — flat `category_id` / `aesthetic_tag_ids` keys are not bound by the server.
5. **Wishlist:** `GET /listings/wishlist` returns IDs only — hydrate with `GET /listings/{id}` (consider caching and batching on the client).
6. **Pagination:** Use `limit`/`offset` consistently; enforce max 50 where documented to match server caps.

---

## OpenAPI (Swagger)

When enabled (`SWAGGER_ENABLE`, non-production by default), interactive docs list the same operations under `/docs/index.html` (and optional public prefix). Generated spec may use tag **listings** or **search** for the routes above.

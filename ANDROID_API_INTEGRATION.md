# Common Service — Android API integration

Reference for **Kotlin** clients using **Retrofit**, **OkHttp**, and **Gson** or **Moshi**. Describes every route under the default base **`/api/v1`**, including **request/response JSON**, **query parameters**, **auth**, **errors**, and **integration notes**.

If Traefik (or similar) exposes a path prefix, prepend it: e.g. `https://host/common-service/api/v1/...`.

---

## 1. Conventions

| Topic | Detail |
|--------|--------|
| **Content-Type** | `application/json` for POST/PUT bodies |
| **Accept** | `application/json` |
| **IDs** | UUID strings (e.g. `"550e8400-e29b-41d4-a716-446655440000"`) |
| **Timestamps** | ISO-8601 in JSON (e.g. `"2025-01-15T08:30:00Z"`); use `Instant` / `OffsetDateTime` on Android |
| **Pagination** | Query `offset` (default `0`) and `limit` (default `20`, max `100`) where listed |
| **Bulk limits** | Most bulk endpoints accept at most **500** items per request |
| **Nulls** | Omitted optional fields vs explicit `null` follow typical JSON rules; Kotlin nullable types should use defaults where the server omits keys |

---

## 2. Base URL

| Item | Typical value |
|------|----------------|
| Path prefix | `/api/v1` (from `API_PREFIX=api`, `API_VERSION=v1`) |
| Example | `https://api.example.com/api/v1` |

Append resource paths below to this base (e.g. `GET …/api/v1/brands`).

---

## 3. Authentication

All routes under **`/api/v1/*`** require **exactly one** of the following. **`GET /health`** is **unauthenticated**.

### 3.1 Internal secret (service-to-service)

| Header | Value |
|--------|--------|
| `X-Internal-Secret` | Same as server `INTERNAL_SECRET` or `PAYMENT_SERVICE_INTERNAL_SECRET` |

Treated as role **`internal-service`** (read + write on mutating routes).

### 3.2 JWT (mobile / web apps)

| Header | Value |
|--------|--------|
| `Authorization` | `Bearer <JWT>` |

- Signed with `JWT_SECRET` / `ACCESS_TOKEN_SECRET`.
- Claim **`role`** (string):
  - **`admin`** or **`internal-service`**: can call **write** routes (POST/PUT/DELETE and destructive operations where allowed).
  - **`frontend`**: **read-only** on GETs; **403** on routes protected by write middleware.

**Android:** Do **not** embed the internal secret in consumer APKs; obtain a **JWT** from your auth backend.

---

## 4. Error response

Failures return JSON:

```json
{
  "code": 400,
  "error": "validation_failed: Key: 'CreateBrandRequest.Name' Error:Field validation for 'Name' failed on the 'required' tag"
}
```

| Field | Meaning |
|--------|---------|
| `code` | HTTP status (same as response status) |
| `error` | Human-readable message; often prefixed with a short reason (`validation_failed:`, `forbidden:`, …) |

Common statuses: **400** validation/bad input, **401** missing/invalid auth, **403** wrong role or feature disabled on server, **404** not found, **500** server error.

---

## 5. Health (no auth)

| Method | Path | Success |
|--------|------|---------|
| `GET` | `/health` | **200** — body is JSON string `"ok"` (not an object) |

Use for connectivity checks; no `Authorization` header.

---

## 6. Addresses

Base path: **`/api/v1/addresses`**

| Method | Path | Auth | Write role |
|--------|------|------|------------|
| `GET` | `/addresses/tree` | Yes | No |
| `GET` | `/addresses` | Yes | No |
| `GET` | `/addresses/history` | Yes | No |
| `POST` | `/addresses` | Yes | Yes |
| `POST` | `/addresses/bulk` | Yes | Yes |
| `PUT` | `/addresses/{id}` | Yes | Yes |
| `DELETE` | `/addresses/{id}` | Yes | Yes |
| `DELETE` | `/addresses/all` | Yes | Yes (purge — see §10) |

There is **no** `GET /addresses/{id}`; use list, tree, or history.

### 6.1 `GET /addresses/tree`

**Output (200):**

```json
{
  "tree": [ { …AddressTreeNode } ]
}
```

**`AddressTreeNode`** (nested): `id`, `name`, `code`, `parent_id`, `level` (1–3), `status`, `effective_from`, `effective_to`, `children` (array of same shape).

### 6.2 `GET /addresses`

**Query parameters:**

| Param | Type | Default | Notes |
|--------|------|---------|--------|
| `level` | int | — | `1` province, `2` district, `3` ward; omit for no filter |
| `parent_id` | UUID | — | Parent row id |
| `current` | bool | `true` | `false` to include non-current rows |

**Output (200):** `{ "items": [ AddressDTO, … ] }`

**`AddressDTO`:** `id`, `name`, `code`, `parent_id`, `level`, `status`, `effective_from`, `effective_to`

### 6.3 `GET /addresses/history`

**Query:** `current` (`true`/`false`), `offset`, `limit` (pagination defaults §1).

**Output (200):** `{ "items": [ … ], "offset": 0, "limit": 20 }`

### 6.4 `POST /addresses`

**Body — `CreateAddressRequest`:**

| Field | Type | Required | Notes |
|--------|------|----------|--------|
| `name` | string | yes | 1–255 chars |
| `code` | string | yes | 1–32 chars |
| `parent_id` | UUID \| null | no | |
| `level` | int | yes | 1–3 |
| `status` | string | yes | `active`, `merged`, `deprecated` |
| `effective_from` | datetime \| null | no | |
| `effective_to` | datetime \| null | no | |

**Output (201):** `{ "address": AddressDTO }`

### 6.5 `POST /addresses/bulk`

**Body:**

```json
{
  "addresses": [ { …CreateAddressRequest }, … ]
}
```

Max **500** entries.

**Output (201):** `{ "addresses": [ AddressDTO, … ] }`

### 6.6 `PUT /addresses/{id}`

**Path:** `id` — UUID.

**Body — `UpdateAddressRequest`** (all optional unless noted):

| Field | Type | Notes |
|--------|------|--------|
| `name`, `code`, `parent_id`, `level`, `status` | optional | Validation same as create where set |
| `effective_from`, `effective_to` | datetime \| null | |
| `clear_parent` | bool | Clear `parent_id` when true |
| `clear_effective_to` | bool | Clear `effective_to` when true |

**Output (200):** `{ "address": AddressDTO }`

### 6.7 `DELETE /addresses/{id}`

**Output:** **204** empty body.

### 6.8 `DELETE /addresses/all`

Requires **`ALLOW_PURGE_ADDRESSES=true`** on server. **Output (200):** `{ "purged": true, "table": "addresses" }`. Otherwise **403**.

---

## 7. Brands

Base path: **`/api/v1/brands`**

| Method | Path | Write |
|--------|------|-------|
| `GET` | `/brands` | No |
| `POST` | `/brands` | Yes |
| `POST` | `/brands/bulk` | Yes |
| `PUT` | `/brands/{id}` | Yes |
| `DELETE` | `/brands/{id}` | Yes |
| `DELETE` | `/brands/all` | Yes (purge — §10) |

No `GET /brands/{id}`; use list with search.

### 7.1 `GET /brands`

**Query:** `q` (name contains), `offset`, `limit`.

**Output (200) — paginated:**

```json
{
  "items": [ BrandDTO, … ],
  "total": 123,
  "offset": 0,
  "limit": 20,
  "has_more": true
}
```

**`BrandDTO`:** `id`, `name`, `slug`, `country`, `logo_url`, `status`, `created_at`, `updated_at`

### 7.2 `POST /brands`

**Body — `CreateBrandRequest`:**

| Field | Type | Required | Notes |
|--------|------|----------|--------|
| `name` | string | yes | max 255 |
| `slug` | string | yes | max 255 |
| `country` | string | no | max 128 |
| `logo_url` | string | no | |
| `status` | string | no | `draft`, `active`, `inactive`, `archived` |

**Output (201):** `{ "brand": BrandDTO }`

### 7.3 `POST /brands/bulk`

**Body:** `{ "brands": [ CreateBrandRequest, … ] }` (max **500**).

**Output (201):** `{ "brands": [ BrandDTO, … ], "created": <int>, "skipped": <int> }` — `skipped` counts duplicates/skipped rows from the bulk logic.

### 7.4 `PUT /brands/{id}`

**Body — `UpdateBrandRequest`:** optional `name`, `slug`, `country`, `logo_url`, `status` (same enums as create).

**Output (200):** `{ "brand": BrandDTO }`

### 7.5 `DELETE /brands/{id}`

**204** empty.

### 7.6 `DELETE /brands/all`

Purge; needs **`ALLOW_PURGE_BRANDS=true`**. **200:** `{ "purged": true, "table": "brands" }`.

---

## 8. Categories

Base path: **`/api/v1/categories`**

| Method | Path | Notes |
|--------|------|--------|
| `GET` | `/categories/tree` | Nested tree |
| `GET` | `/categories` | Flat paginated |
| `GET` | `/categories/{id}` | Single row |
| `POST` | `/categories` | Create |
| `POST` | `/categories/bulk` | Max 500; optional client `id` for ordering |
| `POST` | `/categories/bulk-delete` | Needs env (§10) |
| `PUT` | `/categories/{id}` | Update |
| `DELETE` | `/categories/{id}` | Needs env; CASCADE children |

### 8.1 `GET /categories/tree`

**Output (200):** `{ "categories": [ CategoryTreeNode, … ] }` — top-level roots only; each node has `children: [ … ]`.

**`CategoryTreeNode`:** `id`, `name`, `slug`, `parent_id`, `sort_order`, `status`, `created_at`, `updated_at`, `children`

### 8.2 `GET /categories`

**Query:** `q` (name or slug contains), `offset`, `limit`.

**Output (200):** same paginated shape as brands (`items`, `total`, `offset`, `limit`, `has_more`) with **`CategoryDTO`** in `items`.

### 8.3 `GET /categories/{id}`

**Output (200):** `{ "category": CategoryDTO }`

### 8.4 `POST /categories`

**Body — `CreateCategoryRequest`:**

| Field | Type | Required | Notes |
|--------|------|----------|--------|
| `id` | UUID string | no | If set, insert uses this id (for bulk graphs) |
| `name` | string | yes | |
| `slug` | string | yes | |
| `parent_id` | UUID string | no | Empty/absent = root |
| `sort_order` | int | no | |
| `status` | string | no | `draft`, `active`, `inactive`, `archived` |

**Output (201):** `{ "category": CategoryDTO }`

### 8.5 `POST /categories/bulk`

**Body:** `{ "categories": [ CreateCategoryRequest, … ] }` (max **500**). Later rows may reference **`parent_id`** or **`id`** of earlier rows in the same array (inserted in order in one transaction).

**Output (201):** `{ "categories": [ CategoryDTO, … ] }`

### 8.6 `POST /categories/bulk-delete`

**Body:** `{ "ids": [ "<uuid>", … ] }` (max **500**).

Requires **`ALLOW_BULK_DELETE_CATEGORIES=true`**.

**Output (200):** `{ "deleted": <number> }`

### 8.7 `PUT /categories/{id}`

**Body — `UpdateCategoryRequest`:** optional `name`, `slug`, `parent_id`, `sort_order`, `status`; **`clear_parent`: true** makes the node a root (do not send conflicting `parent_id`).

**Output (200):** `{ "category": CategoryDTO }`

### 8.8 `DELETE /categories/{id}`

Requires **`ALLOW_DELETE_CATEGORY=true`**. Deletes the row and **descendants** (DB CASCADE). **204** empty.

**Integration:** If listings use `category_id` from this service, keep core-service or other apps in sync when deleting (call this endpoint with service credentials after enabling env).

---

## 9. Aesthetic tags

Base path: **`/api/v1/aesthetic-tags`**

| Method | Path | Write / notes |
|--------|------|----------------|
| `GET` | `/aesthetic-tags` | `all=true` returns full catalog |
| `POST` | `/aesthetic-tags/resolve` | Validates names; **any** authenticated role |
| `POST` | `/aesthetic-tags` | Yes |
| `POST` | `/aesthetic-tags/bulk` | Yes |
| `POST` | `/aesthetic-tags/bulk-delete` | Yes + env |
| `GET` | `/aesthetic-tags/{id}` | No |
| `PUT` | `/aesthetic-tags/{id}` | Yes |
| `DELETE` | `/aesthetic-tags/{id}` | Yes + env |
| `DELETE` | `/aesthetic-tags/all` | Purge + env |

### 9.1 `GET /aesthetic-tags`

- **`all=true`** (or `1` / `yes`): **200** `{ "tags": [ AestheticTagDTO, … ] }` — full ordered list (large; Redis may cache server-side). Ignores `offset`/`limit`.
- **Otherwise:** paginated: **`q`**, **`status`**, **`offset`**, **`limit`** → same `items`/`total`/`has_more` shape as brands.

**`AestheticTagDTO`:** `id`, `name` (canonical, lowercase storage), `display_name`, `sort_order`, `status`, `created_at`, `updated_at`

### 9.2 `POST /aesthetic-tags/resolve`

**Body:** `{ "names": [ "minimal", "vintage", … ] }` — max **50** strings, each 1–50 chars.

**Output (200):**

```json
{
  "tags": [ AestheticTagDTO, … ],
  "unknown": [ "not-in-catalog", … ]
}
```

Use **`unknown`** empty to confirm every submitted name exists in the catalog.

### 9.3 `POST /aesthetic-tags`

**Body — `CreateAestheticTagRequest`:** `name`, `display_name` (required, max 50), optional `sort_order`, `status`.

**Output (201):** `{ "tag": AestheticTagDTO }`

### 9.4 `POST /aesthetic-tags/bulk`

**Body:** `{ "tags": [ CreateAestheticTagRequest, … ] }` (max **500**).

**Output (201):** `{ "tags": [ AestheticTagDTO, … ] }`

### 9.5 `GET /aesthetic-tags/{id}`

**200:** `{ "tag": AestheticTagDTO }`

### 9.6 `PUT /aesthetic-tags/{id}`

**Body — `UpdateAestheticTagRequest`:** optional `name`, `display_name`, `sort_order`, `status`.

**200:** `{ "tag": AestheticTagDTO }`

### 9.7 `POST /aesthetic-tags/bulk-delete`

**Body:** `{ "ids": [ "<uuid>", … ] }` (max **500**). Needs **`ALLOW_BULK_DELETE_AESTHETIC_TAGS=true`**.

**200:** `{ "deleted": <number> }`

### 9.8 `DELETE /aesthetic-tags/{id}`

Needs **`ALLOW_DELETE_AESTHETIC_TAG=true`**. **204** empty.

### 9.9 `DELETE /aesthetic-tags/all`

Needs **`ALLOW_PURGE_AESTHETIC_TAGS=true`**. **200:** `{ "purged": true, "table": "aesthetic_tags" }`

---

## 10. Countries

Base path: **`/api/v1/countries`**

Register client paths **exactly** as below so **`iso`** is not parsed as a UUID **`id`**.

| Method | Path | Write |
|--------|------|-------|
| `GET` | `/countries` | No |
| `GET` | `/countries/iso/{iso2}` | No — **two-letter** ISO code, e.g. `VN` |
| `GET` | `/countries/{id}` | No — `id` is UUID |
| `POST` | `/countries` | Yes |
| `POST` | `/countries/bulk` | Yes (max 500) |
| `POST` | `/countries/bulk-delete` | Yes + env |
| `PUT` | `/countries/{id}` | Yes |
| `DELETE` | `/countries/{id}` | Yes + env |
| `DELETE` | `/countries/all` | Purge + env |

**Notices:** Server normalizes **`iso2`** / **`iso3`** to uppercase. Prefer storing **`iso2`** for display keys and phone rules.

### 10.1 `GET /countries`

- **`all=true`**: **200** `{ "countries": [ CountryDTO, … ] }` — full list (ordered), ignores pagination.
- **Else:** **`q`** (search name or iso2/iso3), **`status`**, **`offset`**, **`limit`** → paginated `items`/`total`/`has_more` with **`CountryDTO`** in `items`.

**`CountryDTO`:** `id`, `iso2`, `iso3`, `name`, `numeric_code`, `phone_prefix`, `emoji`, `sort_order`, `status`, `created_at`, `updated_at`

### 10.2 `GET /countries/iso/{iso2}`

Path param: ISO-3166-1 alpha-2 (e.g. `US`, `vn` accepted; server uppercases).

**200:** `{ "country": CountryDTO }` — **404** if unknown.

### 10.3 `GET /countries/{id}`

**200:** `{ "country": CountryDTO }`

### 10.4 `POST /countries`

**Body — `CreateCountryRequest`:**

| Field | Type | Notes |
|--------|------|--------|
| `iso2` | string | Required, length 2 |
| `iso3` | string | Optional, length 3 |
| `name` | string | Required |
| `numeric_code` | int | Optional |
| `phone_prefix` | string | e.g. `+84` |
| `emoji` | string | Flag emoji optional |
| `sort_order` | int | Optional |
| `status` | string | `draft`, `active`, `inactive`, `archived` |

**201:** `{ "country": CountryDTO }`

### 10.5 `POST /countries/bulk`

**Body:** `{ "countries": [ CreateCountryRequest, … ] }` (max **500**).

**201:** `{ "countries": [ CountryDTO, … ] }`

### 10.6 `PUT /countries/{id}`

**Body — `UpdateCountryRequest`:** all fields optional; same semantics as create.

**200:** `{ "country": CountryDTO }`

### 10.7 `POST /countries/bulk-delete`

**Body:** `{ "ids": [ "<uuid>", … ] }`. Requires **`ALLOW_BULK_DELETE_COUNTRIES=true`**.

**200:** `{ "deleted": <number> }`

### 10.8 `DELETE /countries/{id}`

Requires **`ALLOW_DELETE_COUNTRY=true`**. **204** empty.

### 10.9 `DELETE /countries/all`

Requires **`ALLOW_PURGE_COUNTRIES=true`**. **200:** `{ "purged": true }`

---

## 11. Server-side feature flags (dangerous / ops)

These return **403** with a message if disabled. They require **write** role (**admin** / **internal-service**), not **`frontend`**.

| Env variable | Enables |
|----------------|---------|
| `ALLOW_PURGE_BRANDS=true` | `DELETE /brands/all` |
| `ALLOW_PURGE_ADDRESSES=true` | `DELETE /addresses/all` |
| `ALLOW_PURGE_AESTHETIC_TAGS=true` | `DELETE /aesthetic-tags/all` |
| `ALLOW_PURGE_COUNTRIES=true` | `DELETE /countries/all` |
| `ALLOW_DELETE_CATEGORY=true` | `DELETE /categories/{id}` |
| `ALLOW_BULK_DELETE_CATEGORIES=true` | `POST /categories/bulk-delete` |
| `ALLOW_DELETE_AESTHETIC_TAG=true` | `DELETE /aesthetic-tags/{id}` |
| `ALLOW_BULK_DELETE_AESTHETIC_TAGS=true` | `POST /aesthetic-tags/bulk-delete` |
| `ALLOW_DELETE_COUNTRY=true` | `DELETE /countries/{id}` |
| `ALLOW_BULK_DELETE_COUNTRIES=true` | `POST /countries/bulk-delete` |

Defaults are **false** except where your deployment explicitly sets them.

---

## 12. Android implementation notes

1. **Secrets:** Never ship **`X-Internal-Secret`** in retail apps; use **JWT** with appropriate **`role`** for the user or BFF pattern.
2. **Purges and bulk deletes:** Treat as **admin/back-office** only; do not expose in consumer UI.
3. **Offline / cache:** Tree endpoints (`addresses`, `categories`) and `aesthetic-tags?all=true` / `countries?all=true` are good candidates for Room + periodic refresh; store UUIDs as strings.
4. **Validation:** Prefer **`POST /aesthetic-tags/resolve`** before persisting user-selected tag names.
5. **OpenAPI:** Generated Swagger lives at **`/swagger/index.html`** when enabled; models align with this document and `internal/model/*.go`.

---

## 13. Endpoint summary (quick index)

| Method | Path | Auth | Write |
|--------|------|------|-------|
| `GET` | `/health` | No | — |
| `GET` | `/api/v1/addresses/tree` | Yes | No |
| `GET` | `/api/v1/addresses` | Yes | No |
| `GET` | `/api/v1/addresses/history` | Yes | No |
| `POST` | `/api/v1/addresses` | Yes | Yes |
| `POST` | `/api/v1/addresses/bulk` | Yes | Yes |
| `PUT` | `/api/v1/addresses/{id}` | Yes | Yes |
| `DELETE` | `/api/v1/addresses/{id}` | Yes | Yes |
| `DELETE` | `/api/v1/addresses/all` | Yes | Yes |
| `GET` | `/api/v1/brands` | Yes | No |
| `POST` | `/api/v1/brands` | Yes | Yes |
| `POST` | `/api/v1/brands/bulk` | Yes | Yes |
| `PUT` | `/api/v1/brands/{id}` | Yes | Yes |
| `DELETE` | `/api/v1/brands/{id}` | Yes | Yes |
| `DELETE` | `/api/v1/brands/all` | Yes | Yes |
| `GET` | `/api/v1/categories/tree` | Yes | No |
| `GET` | `/api/v1/categories` | Yes | No |
| `GET` | `/api/v1/categories/{id}` | Yes | No |
| `POST` | `/api/v1/categories` | Yes | Yes |
| `POST` | `/api/v1/categories/bulk` | Yes | Yes |
| `POST` | `/api/v1/categories/bulk-delete` | Yes | Yes |
| `PUT` | `/api/v1/categories/{id}` | Yes | Yes |
| `DELETE` | `/api/v1/categories/{id}` | Yes | Yes |
| `GET` | `/api/v1/aesthetic-tags` | Yes | No |
| `POST` | `/api/v1/aesthetic-tags/resolve` | Yes | No* |
| `POST` | `/api/v1/aesthetic-tags` | Yes | Yes |
| `POST` | `/api/v1/aesthetic-tags/bulk` | Yes | Yes |
| `POST` | `/api/v1/aesthetic-tags/bulk-delete` | Yes | Yes |
| `GET` | `/api/v1/aesthetic-tags/{id}` | Yes | No |
| `PUT` | `/api/v1/aesthetic-tags/{id}` | Yes | Yes |
| `DELETE` | `/api/v1/aesthetic-tags/{id}` | Yes | Yes |
| `DELETE` | `/api/v1/aesthetic-tags/all` | Yes | Yes |
| `GET` | `/api/v1/countries` | Yes | No |
| `GET` | `/api/v1/countries/iso/{iso2}` | Yes | No |
| `GET` | `/api/v1/countries/{id}` | Yes | No |
| `POST` | `/api/v1/countries` | Yes | Yes |
| `POST` | `/api/v1/countries/bulk` | Yes | Yes |
| `POST` | `/api/v1/countries/bulk-delete` | Yes | Yes |
| `PUT` | `/api/v1/countries/{id}` | Yes | Yes |
| `DELETE` | `/api/v1/countries/{id}` | Yes | Yes |
| `DELETE` | `/api/v1/countries/all` | Yes | Yes |

\*Resolve does not change server data; **`frontend`** JWT can call it if authenticated.

Adjust `/api/v1` if your deployment uses another `API_PREFIX` / `API_VERSION`.

# Common Service — Android API integration

This document describes every HTTP endpoint exposed by **common-service** so Android clients (Kotlin, Retrofit, OkHttp, etc.) can integrate without opening the Go codebase.

Default API base path is **`/api/v1`**. Your deployment may add a prefix (for example Traefik: `https://<host>/common-service/api/v1`). Confirm the **base URL** with your backend team.

---

## 1. Base URL and versioning

| Item | Typical value |
|------|----------------|
| Path prefix | `/api/v1` (from `API_PREFIX=api`, `API_VERSION=v1`) |
| Example full base | `https://api.example.com/api/v1` |
| Example behind path router | `https://76.13.211.193/common-service/api/v1` |

All API routes below are **relative to that base** (e.g. `GET .../addresses/tree` → `GET https://<host>/api/v1/addresses/tree`).

---

## 2. Authentication

Every route under `/api/v1` requires **one** of the following (not both required at once).

### Option A — Internal secret (server-to-server or trusted clients)

| Header | Value |
|--------|--------|
| `X-Internal-Secret` | Shared secret (same as server `INTERNAL_SECRET` or `PAYMENT_SERVICE_INTERNAL_SECRET`) |

This path is treated as role **`internal-service`**, which **can read and write** (when the route allows writes).

### Option B — JWT (mobile / user flows)

| Header | Value |
|--------|--------|
| `Authorization` | `Bearer <JWT>` |

The JWT must be signed with the server’s **`JWT_SECRET`** (or **`ACCESS_TOKEN_SECRET`** if that alias is used). The payload should include a **`role`** claim:

| `role` value | Read API | Write API (POST/PUT/DELETE) |
|----------------|----------|-----------------------------|
| `admin` | Yes | Yes |
| `internal-service` | Yes | Yes |
| `frontend` | Yes | **No** (403 on writes) |

Unknown or missing `role` defaults to **`frontend`** (read-only).

### Headers for JSON bodies

| Header | When |
|--------|------|
| `Content-Type` | `application/json` on **POST** and **PUT** with a body |
| `Accept` | `application/json` (optional; recommended) |

---

## 3. Response and error format

### Success

Responses are **plain JSON objects** (no `success` / `data` wrapper). Shapes are documented per endpoint below.

### Errors

HTTP status is set on the response. Body shape:

```json
{
  "code": 401,
  "error": "unauthorized: missing credentials"
}
```

- **`code`**: HTTP status code (same as the real status).
- **`error`**: Human-readable message (often `codeName: detail`).

Common statuses: **400** validation, **401** auth, **403** insufficient role, **404** not found, **500** server error.

---

## 4. Health (no API auth)

| Method | Path | Auth |
|--------|------|------|
| `GET` | `/health` | None |

**Success:** HTTP **200**, body is the JSON string **`"ok"`** (quoted string in JSON).

Use this for connectivity checks. It is **outside** `/api/v1` and does not use `X-Internal-Secret` / `Authorization`.

---

## 5. Addresses

All paths below are under the API base (e.g. `/api/v1/addresses/...`).

### 5.1 Address tree (provinces → districts → wards)

| | |
|---|---|
| **Method** | `GET` |
| **Path** | `/addresses/tree` |
| **Auth** | Required |
| **Write** | No |

**Response 200**

```json
{
  "tree": [
    {
      "id": "uuid",
      "name": "string",
      "code": "string",
      "parent_id": null,
      "level": 1,
      "status": "active",
      "effective_from": "2024-01-01T00:00:00Z",
      "effective_to": null,
      "children": []
    }
  ]
}
```

`level`: **1** province/city, **2** district, **3** ward. Children are nested under `children`.

---

### 5.2 List addresses (flat list)

| | |
|---|---|
| **Method** | `GET` |
| **Path** | `/addresses` |
| **Auth** | Required |
| **Write** | No |

**Query parameters**

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `level` | int | No | `1`, `2`, or `3` |
| `parent_id` | UUID string | No | Filter by parent |
| `current` | boolean | **Yes** (validated) | `true` = only rows effective “now”; `false` for all |

**Response 200**

```json
{
  "items": [ { /* AddressDTO */ } ]
}
```

**AddressDTO fields**

| Field | Type | Notes |
|-------|------|--------|
| `id` | UUID | |
| `name` | string | |
| `code` | string | |
| `parent_id` | UUID or omitted | |
| `level` | number | 1–3 |
| `status` | string | e.g. `active`, `merged`, `deprecated` |
| `effective_from` | ISO-8601 | |
| `effective_to` | ISO-8601 or omitted | |

---

### 5.3 Address history (merged / deprecated)

| | |
|---|---|
| **Method** | `GET` |
| **Path** | `/addresses/history` |
| **Auth** | Required |
| **Write** | No |

**Query parameters**

| Name | Type | Default | Description |
|------|------|---------|-------------|
| `current` | boolean | `false` | If `true`, restrict to rows effective at now |
| `offset` | int | `0` | |
| `limit` | int | `20` | Max **100** |

**Response 200**

```json
{
  "items": [ { /* AddressDTO */ } ],
  "offset": 0,
  "limit": 20
}
```

---

### 5.4 Create address

| | |
|---|---|
| **Method** | `POST` |
| **Path** | `/addresses` |
| **Auth** | Required |
| **Write** | Yes (`admin` or `internal-service`, or internal secret) |

**Request body**

```json
{
  "name": "string",
  "code": "string",
  "parent_id": "uuid-or-null",
  "level": 1,
  "status": "active",
  "effective_from": "2024-01-01T00:00:00Z",
  "effective_to": null
}
```

| Field | Required | Notes |
|-------|----------|--------|
| `name` | Yes | 1–255 chars |
| `code` | Yes | 1–32 chars |
| `parent_id` | No | Omit or null for root-level |
| `level` | Yes | 1, 2, or 3 |
| `status` | Yes | `active` \| `merged` \| `deprecated` |
| `effective_from` | No | Defaults to server “now” if omitted |
| `effective_to` | No | Optional end of validity |

**Response 201**

```json
{
  "address": { /* AddressDTO */ }
}
```

---

### 5.5 Bulk create addresses

| | |
|---|---|
| **Method** | `POST` |
| **Path** | `/addresses/bulk` |
| **Auth** | Required |
| **Write** | Yes |

**Request body**

```json
{
  "addresses": [
    { /* same fields as single CreateAddressRequest */ }
  ]
}
```

- **1–500** items per request.

**Response 201**

```json
{
  "addresses": [ /* AddressDTO[] */ ]
}
```

All rows are inserted in **one transaction** (all succeed or all fail).

---

### 5.6 Update address

| | |
|---|---|
| **Method** | `PUT` |
| **Path** | `/addresses/{id}` |
| **Auth** | Required |
| **Write** | Yes |

**Path:** `id` = address UUID.

**Request body** — all fields optional; only sent fields are updated.

```json
{
  "name": "string",
  "code": "string",
  "parent_id": "uuid",
  "level": 2,
  "status": "active",
  "effective_from": "2024-01-01T00:00:00Z",
  "effective_to": "2025-01-01T00:00:00Z",
  "clear_parent": false,
  "clear_effective_to": false
}
```

| Field | Notes |
|-------|--------|
| `clear_parent` | If `true`, clears `parent_id` |
| `clear_effective_to` | If `true`, clears `effective_to` |

**Response 200**

```json
{
  "address": { /* AddressDTO */ }
}
```

---

## 6. Brands

### 6.1 List brands

| | |
|---|---|
| **Method** | `GET` |
| **Path** | `/brands` |
| **Auth** | Required |
| **Write** | No |

**Query parameters**

| Name | Type | Default | Description |
|------|------|---------|-------------|
| `q` | string | — | Search substring in name |
| `offset` | int | `0` | |
| `limit` | int | `20` | Max **100** |

**Response 200**

```json
{
  "items": [
    {
      "id": "uuid",
      "name": "string",
      "slug": "string",
      "country": "string",
      "logo_url": "string",
      "status": "draft",
      "created_at": "2024-01-01T00:00:00Z",
      "updated_at": "2024-01-01T00:00:00Z"
    }
  ],
  "total": 100,
  "offset": 0,
  "limit": 20,
  "has_more": true
}
```

---

### 6.2 Get brand by ID

| | |
|---|---|
| **Method** | `GET` |
| **Path** | `/brands/{id}` |
| **Auth** | Required |
| **Write** | No |

**Response 200**

```json
{
  "brand": { /* same shape as list item */ }
}
```

---

### 6.3 Create brand

| | |
|---|---|
| **Method** | `POST` |
| **Path** | `/brands` |
| **Auth** | Required |
| **Write** | Yes |

**Request body**

```json
{
  "name": "string",
  "slug": "string",
  "country": "string",
  "logo_url": "string",
  "status": "draft"
}
```

| Field | Required | Notes |
|-------|----------|--------|
| `name`, `slug` | Yes | 1–255 chars |
| `country` | No | max 128 |
| `logo_url` | No | |
| `status` | No | `draft` \| `active` \| `inactive` \| `archived`; default **`draft`** |

**Response 201**

```json
{
  "brand": { /* BrandDTO */ }
}
```

---

### 6.4 Bulk create brands

| | |
|---|---|
| **Method** | `POST` |
| **Path** | `/brands/bulk` |
| **Auth** | Required |
| **Write** | Yes |

**Request body**

```json
{
  "brands": [ { /* CreateBrandRequest */ } ]
}
```

- **1–500** items.

**Response 201**

```json
{
  "brands": [ /* BrandDTO[] */ ]
}
```

---

### 6.5 Update brand

| | |
|---|---|
| **Method** | `PUT` |
| **Path** | `/brands/{id}` |
| **Auth** | Required |
| **Write** | Yes |

**Request body** — optional fields (JSON `null` can clear optional strings depending on server parsing; prefer omitting keys if unsure).

```json
{
  "name": "string",
  "slug": "string",
  "country": "string",
  "logo_url": "string",
  "status": "active"
}
```

**Response 200**

```json
{
  "brand": { /* BrandDTO */ }
}
```

---

### 6.6 Delete brand

| | |
|---|---|
| **Method** | `DELETE` |
| **Path** | `/brands/{id}` |
| **Auth** | Required |
| **Write** | Yes |

**Response 204** — empty body on success.

---

## 7. Taxonomy (categories, materials, colors, sizes, tags)

| | |
|---|---|
| **Method** | `GET` |
| **Path** | `/taxonomy` |
| **Auth** | Required |
| **Write** | No |

**Query parameters**

| Name | Type | Description |
|------|------|-------------|
| `kind` | string | Optional filter: `category`, `material`, `color`, `size`, `tag` |
| `offset` | int | Default `0` |
| `limit` | int | Default `20`, max **100** |

**Response 200**

```json
{
  "items": [
    {
      "id": "uuid",
      "kind": "category",
      "code": "string",
      "name": "string",
      "slug": "string",
      "metadata": {},
      "sort_order": 0,
      "status": "string",
      "created_at": "2024-01-01T00:00:00Z",
      "updated_at": "2024-01-01T00:00:00Z"
    }
  ],
  "total": 50,
  "offset": 0,
  "limit": 20,
  "has_more": true
}
```

`metadata` is a JSON object; shape depends on `kind`.

---

## 8. Android integration notes

1. **Store secrets safely**  
   Do **not** embed `X-Internal-Secret` in a public app. Prefer **JWT** issued by your auth/backend service with `role` set appropriately. Reserve internal secret for build flavors or admin tools if absolutely required.

2. **Retrofit example (conceptual)**  
   - One `Interceptor` that adds `Authorization: Bearer <token>` from your `SessionStore` / `DataStore`.  
   - Or adds `X-Internal-Secret` only in **debug** / internal builds.

3. **UUID and dates**  
   Use `java.util.UUID` / Kotlin string UUIDs matching server format. Parse `created_at` / `effective_*` with `Instant` or `ZonedDateTime` (ISO-8601).

4. **Rate limiting**  
   The service may apply per-IP rate limits; handle **429** if introduced at the gateway and backoff.

5. **IP allowlist**  
   Some deployments restrict client IPs. If requests fail from mobile networks, confirm **`ALLOWED_CLIENT_IPS`** / Traefik settings with ops.

6. **OpenAPI**  
   If Swagger is enabled on the server, you can also use  
   `GET /swagger/index.html`  
   (same host as the API; path may include a prefix like `/common-service/swagger/`) to export the live spec for code generation tools.

---

## 9. Endpoint summary

| Method | Path | Auth | Write |
|--------|------|------|-------|
| `GET` | `/health` | No | — |
| `GET` | `/api/v1/addresses/tree` | Yes | No |
| `GET` | `/api/v1/addresses` | Yes | No |
| `GET` | `/api/v1/addresses/history` | Yes | No |
| `POST` | `/api/v1/addresses` | Yes | Yes |
| `POST` | `/api/v1/addresses/bulk` | Yes | Yes |
| `PUT` | `/api/v1/addresses/{id}` | Yes | Yes |
| `GET` | `/api/v1/brands` | Yes | No |
| `GET` | `/api/v1/brands/{id}` | Yes | No |
| `POST` | `/api/v1/brands` | Yes | Yes |
| `POST` | `/api/v1/brands/bulk` | Yes | Yes |
| `PUT` | `/api/v1/brands/{id}` | Yes | Yes |
| `DELETE` | `/api/v1/brands/{id}` | Yes | Yes |
| `GET` | `/api/v1/taxonomy` | Yes | No |

Replace `/api/v1` with your configured base path if different.

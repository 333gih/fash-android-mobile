# Search API — Android integration reference

This document describes every **`/search/*`** HTTP endpoint exposed by **core-service**: authentication, URL shape, query parameters, success payloads, errors, and behavior notes for mobile clients.

---

## Base URL and versioning

| Item | Detail |
|------|--------|
| **Path pattern** | `{optional_public_prefix}/{lang}{api_prefix}/v1/search/...` |
| **Typical `api_prefix`** | From env `APP_API_PREFIX` (often `/api` → segment is `/api/v1/...`). If unset, check deployment; many setups use `/api/v1`. |
| **Localized routes** | `GET /{lang}/api/v1/search/...` with `lang` = `en` or `vi` (locale middleware). |
| **Legacy (no lang in path)** | `GET /api/v1/search/...` with default locale `en`. |
| **Reverse proxy** | `APP_PUBLIC_PREFIX` may prepend (e.g. `/core-service`) so full path becomes `/core-service/vi/api/v1/search/listings`. **Configure the Android base URL to match your environment.** |

Use the same **base URL** as the rest of the app’s REST API (see `internal/config/app.config.go` and `internal/delivery/http/routes/routes.go`).

---

## Authentication

All **`/search/*`** routes are **protected**.

| Header | Value |
|--------|--------|
| `Authorization` | `Bearer <access_jwt>` |

| Response | When |
|----------|------|
| **401** | Missing/invalid/expired token, or user id not resolved. |

Error body shape (typical):

```json
{
  "code": 401,
  "error": "authentication required"
}
```

The `error` string may be **localized** (e.g. Vietnamese) depending on **`Accept-Language`** and/or the **`/:lang/`** path. Treat **`code`** (HTTP status) as the stable signal; **`error`** is for display.

---

## Locale

- Prefer calling the **localized** URL (`/en/...` or `/vi/...`) so routing and messages stay consistent.
- Alternatively, send **`Accept-Language`** (`en`, `vi`) per your app language.
- Details align with `docs/integration-android-app-prompt.md` (transport and auth section).

---

## Endpoints overview

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/search/listings` | Text search **or** heat-ranked browse (no query). Logs normalized `q` on **first page** (`offset=0`) for recent + trending queries. |
| `GET` | `/search/autocomplete` | Listing title suggestions (prefix / first word). |
| `GET` | `/search/recent-queries` | This user’s **recent** normalized queries (up to **20**). |
| `GET` | `/search/trending-queries` | **Global** top **20** query strings by count (**last 7 days**). |
| `GET` | `/search/trending-tags` | Top **20** aesthetic tag names (listing tags, **not** text search; last 7 days). |

*(Paths are relative to `{api_prefix}/v1` — e.g. `/api/v1/search/listings`.)*

**Storage:** Recent and trending **queries** come from PostgreSQL table `search_query_events` (not Typesense). Trending **queries** response is cached in Redis ~1h when Redis is enabled (same pattern as trending tags).

---

## 1. `GET /search/listings`

### Purpose

- **`q` present and non-empty (after trim):** full-text style search on **title**, **description**, and **brand** (server-side `ILIKE`). Supports **filters**, **sort**, pagination.
- **`q` empty or omitted:** **heat-ranked browse** (discovery feed semantics: engagement-weighted ordering, same family of behavior as the old explore feed). **Sort is ignored** in this mode; filters still apply.

Listings from **blocked users**, **users who blocked you**, and **your own listings** are **excluded** from results.

### Query parameters (all optional except as noted)

| Parameter | Type | Default | Max / notes |
|-----------|------|---------|----------------|
| `q` | string | *(empty)* | Search text. Empty ⇒ browse mode (heat), not “search with sort”. |
| `category_id` | UUID string | — | Filter by listing category. |
| `tags` | string | — | Comma-separated **aesthetic tag names** (e.g. `streetwear,y2k`). Whitespace around tags is trimmed; empty segments skipped. Matching is case-insensitive on the server. |
| `min_price` | integer (VND) | — | Non-negative; invalid values ignored. |
| `max_price` | integer (VND) | — | Non-negative; invalid values ignored. |
| `condition` | string | — | Must match stored listing `condition` exactly (e.g. `new`, `like_new` — use values your app already uses for create/update listing). |
| `sort` | string | `recent` | Used only when **`q` is non-empty**: `recent`, `popular`, `price_asc`, `price_desc`. |
| `limit` | integer | `20` | Capped at **50** on the server. |
| `offset` | integer | `0` | Offset pagination (`offset = pageIndex * limit`). |

### Sort behavior when `q` is non-empty

| `sort` | Behavior (summary) |
|--------|---------------------|
| `recent` (default) | Newest first (`created_at DESC`). |
| `popular` | Restricts to listings from the **last 7 days**, then by engagement (`like_count + save_count`) and recency. |
| `price_asc` / `price_desc` | By price, then `created_at DESC`. |

### Success response

- **200 OK**
- **Body:** JSON **array** of listing objects (may be empty `[]`).
- **No** wrapper like `{ "data": ... }` — the top-level value is the array.

Images are returned with **signed URLs** where applicable (server applies URL signing before respond).

### Listing JSON shape (conceptual)

The server serializes Go models with **`encoding/json`** (exported fields use **PascalCase** keys unless a field has a `json` tag). Nested `Seller` (profile) and `Category` are included when loaded.

Typical fields you will see on each element:

| JSON field (PascalCase) | Meaning |
|-------------------------|--------|
| `ID` | Listing UUID |
| `CreatedAt`, `UpdatedAt` | RFC3339 timestamps |
| `SellerID` | Seller user UUID |
| `Title` | Title |
| `CoverImageURL` | Cover image URL (signed when applicable) |
| `ImageURLs` | Array of image URL strings |
| `Price` | Integer VND |
| `Condition` | Condition string |
| `CategoryID` | Category UUID |
| `Description`, `Size`, `Brand` | Strings |
| `Status` | e.g. `active` |
| `LikeCount`, `SaveCount`, `ViewCount` | Integers |
| `Seller` | Object (profile) when present |
| `Category` | Object (`ID`, `Name`, `Slug`, …) when present |
| `aesthetic_tags` | Array of tag name strings (lowercase key from struct tag) |

**Android:** map with **Gson/Moshi/Kotlinx.serialization** using PascalCase property names, or configure `@SerializedName` / `@SerialName` to match.

### Errors

| HTTP | Typical cause |
|------|----------------|
| 401 | Not authenticated |
| 500 | Server error |

Error JSON uses `{ "code": <int>, "error": "<message>" }`.

### Integration tips (Android)

- **Browse vs search:** Use the **same endpoint** for “Explore / Discover” and “Search”: omit `q` (or send empty) for browse; set `q` for search.
- **Pagination:** Use `limit` + `offset`; clamp `limit` to ≤ 50 client-side to avoid wasted requests.
- **Filters:** Reuse the same filter UI for both modes; remember **`sort` only applies when `q` is non-empty**.

---

## 2. `GET /search/autocomplete`

### Purpose

Returns up to **5** distinct **listing titles** that start with the **first word** of the user’s input (prefix match on `title`). Respects the same **block** rules as search (excludes blocked / blocker sellers’ listings).

### Query parameters

| Parameter | Required | Notes |
|-----------|----------|--------|
| `q` | **Yes** (for useful results) | Prefix text; only the **first word** is used for matching (`word%` pattern). If `q` is empty or only whitespace, the API returns **`[]`**. |

### Success response

- **200 OK**
- **Body:** JSON **array of strings** (titles), e.g.:

```json
[
  "Áo thun oversize basic",
  "Áo thun vintage 90s"
]
```

### Errors

Same pattern as above: **401**, **500**.

### Integration tips (Android)

- Call while the user types in the search box; debounce (e.g. 200–300 ms).
- Only the **first token** affects matching — document in UI if needed (“matches first word of title”).
- Results are cached server-side per user/prefix (see use case); still safe to cache briefly on device.

---

## 3. `GET /search/trending-tags`

### Purpose

Returns **up to 20** **aesthetic tag names** ranked by recent listing activity (last **7 days**), server-cached.

### Query parameters

None.

### Success response

- **200 OK**
- **Body:** JSON **array of strings**:

```json
["streetwear", "y2k", "minimalist"]
```

### Errors

**401**, **500** as above.

### Integration tips (Android)

- Use for “Trending” chips on the search or home screen; refresh on screen open or pull-to-refresh (no need to poll aggressively).

---

## 4. `GET /search/recent-queries`

### Purpose

Returns up to **20** **distinct** normalized query strings for the **current user**, most recently used first. Populated when the user runs **`GET /search/listings`** with non-empty **`q`** on the **first page** (`offset=0`).

### Query parameters

None.

### Success response

- **200 OK**
- **Body:** JSON array of strings (normalized: lowercase, collapsed whitespace).

### Errors

**401**, **500** as above.

---

## 5. `GET /search/trending-queries`

### Purpose

Returns the **top 20** normalized queries **globally** by occurrence count in the **last 7 days** (PostgreSQL aggregate). Response is **cached in Redis for ~1 hour** when Redis is configured (same TTL pattern as trending tags).

### Query parameters

None.

### Success response

- **200 OK**
- **Body:** JSON array of objects:

```json
[
  { "query": "áo thun", "count": 42 },
  { "query": "denim", "count": 31 }
]
```

### Errors

**401**, **500** as above.

---

## Quick reference — HTTP summary

| Endpoint | Auth | Success body |
|----------|------|----------------|
| `GET .../search/listings` | Bearer | `[ ... Listing ]` |
| `GET .../search/autocomplete?q=` | Bearer | `[ "title1", ... ]` |
| `GET .../search/recent-queries` | Bearer | `[ "query1", ... ]` |
| `GET .../search/trending-queries` | Bearer | `[ { "query", "count" }, ... ]` |
| `GET .../search/trending-tags` | Bearer | `[ "tag1", ... ]` |

---

## Related code (for maintainers)

| Area | Location |
|------|----------|
| Routes | `internal/delivery/http/routes/search/search.route.go` |
| Handlers | `internal/delivery/http/handlers/search.handler.go` |
| Listings search + browse | `internal/usecase/search_listings.usecase.go` |
| Autocomplete | `internal/usecase/search_autocomplete.usecase.go` |
| Recent / trending **queries** | `internal/usecase/search_query_log.usecase.go`, `internal/domain/repositories/postgres/search_query_repository.go` |
| Query normalization | `internal/searchutil/normalize.go` |
| Trending tags | `internal/usecase/search_trending.usecase.go` |
| Listing entity | `internal/domain/entites/postgresql/listing.entity.go` |

---

## Changelog note for Android

The previous **`GET /listings/explore`** feed was **removed**. **Heat-ranked browse** is now **`GET /search/listings`** with **no `q`** (or empty `q`), same filters as before (`category_id`, `tags`, `min_price`, `max_price`, `condition`, `limit`, `offset`).


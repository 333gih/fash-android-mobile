# Listing `image_urls`: string array → step-based JSON

This document describes the **breaking API and schema change** that replaces `image_urls: string[]` with an array of **step objects** (aligned with listing photo flows / catalog steps). It lists every touched area, how to roll out safely, and design notes for keeping the feature maintainable.

## JSON shape (create, update, and all listing responses)

`image_urls` is now:

```json
[
  {
    "step_key": "front",
    "label": "Front view",
    "label_vi": "mat truoc",
    "sort_order": 0,
    "required": true,
    "image_url": "https://..."
  },
  {
    "step_key": "step_2",
    "label": "Back side",
    "label_vi": "mat sau",
    "sort_order": 1,
    "required": true,
    "image_url": "https://..."
  }
]
```

Rules enforced in **core-service**:

| Rule | Detail |
|------|--------|
| Count | 1–20 slots per listing (DB + handler validation). |
| `step_key` | Required, trimmed, max 64 chars, **unique** within the array. |
| `label` | Required, trimmed, max 120 chars. |
| `label_vi` | Optional, trimmed, max 120 chars. |
| `required` | If `true`, `image_url` must be non-empty after trim/canonicalize. |
| Cover | `cover_image_url` is **not** chosen by array index. It is the `image_url` on the step with the **smallest `sort_order`** among steps that have a non-empty URL (stable tie-break: first in sorted order). |
| Storage | URLs are passed through `CanonicalStoredImageURL` (strip presigned query/hash) before save; max stored length 512 per URL. |

`POST /listings/images` is unchanged: it still returns `{ "image_url", "presigned_url?" }`. Clients assign that URL to the correct slot’s `image_url`.

## Database

- Column `listings.image_urls` type: **`TEXT[]` → `JSONB`** (non-null array of objects).
- Constraint: **`chk_listing_image_urls_json_len`** — `jsonb_array_length` between **1 and 20** (and value must be a JSON array).
- **Upgrade path:** apply `migrations/026_listing_image_urls_jsonb.sql` on existing databases. It backfills each legacy string into a step object (`front`, `step_2`, … with default labels).
- **Fresh installs:** `migrations/init.sql`, `migrations/init_schema.sql`, and `migrations/listings_table_current.sql` already define `image_urls` as JSONB with the new constraint.

## Code changes (by area)

### Domain / persistence

- `internal/domain/entites/postgresql/listing_image_steps.go` — `ListingImageStep`, `ListingImageStepsJSON` (GORM `jsonb` + `Scan`/`Value`), `ValidateAndNormalizeListingImageSteps`, `UniqueListingImageURLs`.
- `internal/domain/entites/postgresql/listing.entity.go` — `ImageURLs` is now `ListingImageStepsJSON` (column `image_urls`).

### Use cases

- `internal/usecase/listing_create.usecase.go` — `CreateListingRequest.ImageURLs` is `[]entities.ListingImageStep`; validates via `ValidateAndNormalizeListingImageSteps`.
- `internal/usecase/listing_update.usecase.go` — optional `ImageSteps *[]entities.ListingImageStep`; optional `WithImageDeleter` to remove **replaced** images from storage when URLs are dropped on update.
- `internal/usecase/listing_delete.usecase.go` — deletes unique URLs from `cover_image_url` + all steps (no double-delete).
- `internal/usecase/listing_bulk_seed.usecase.go` — builds synthetic steps (`front`, `step_2`, …) for dev seed data.

### HTTP

- `internal/delivery/http/requests/listing/listing.request.go` — `ListingImageStepRequest`; create `image_urls` 1–20; update optional `image_urls`.
- `internal/delivery/http/responses/listing/listing.response.go` — `ListingImageStep`; `image_urls` is `[]ListingImageStep`.
- `internal/delivery/http/handlers/listing.handler.go` — maps HTTP ↔ use case; swagger descriptions adjusted.

### Infrastructure

- `internal/infrastructure/storage/url_signer.go` — presigns each step’s `image_url` in place.

### Container

- `internal/container/listing_container.go` — `ListingUpdateUsecase.WithImageDeleter(listingImgStorage)` when the storage backend implements `ImageDeleter`.

### Docs / OpenAPI

- `cmd/docs/*` — regenerated with `make swagger` (`listing_request.ListingImageStepRequest`, `listing.ListingImageStep`).
- **Consumer docs** outside this file (`docs/API.md`, `docs/android-listings-api.md`, `docs/integration-chat.md`, etc.) still mention `string[]` in places; update them when publishing client guidance (search for `image_urls`).

## Client migration checklist

1. Replace models: `List<String> imageUrls` → `List<ListingImageStep> imageUrls` (same field name).
2. **Create listing:** build one object per step from your listing wizard / common-service catalog (`step_key`, labels, `sort_order`, `required`); fill `image_url` after `POST /listings/images`.
3. **Update listing (optional):** send `image_urls` only when replacing the gallery; omitted = unchanged.
4. **Display:** iterate `image_urls` sorted by `sort_order`; use `label` / `label_vi` for UI copy.
5. **Cover:** continue to use `cover_image_url` for cards/thumbnails; it stays in sync with the primary slot rule above.

## Design brainstorm — making this “depend on steps” cleanly

### 1. Single source of truth for step definitions

- **Today (core-service):** Stores whatever the client sends (metadata + URL). The server validates shape, uniqueness, and required slots, but does **not** fetch step definitions from another service.
- **Stronger approach:** common-service (or static config) exposes **canonical steps** per category or global template. The client sends only `{ step_key, image_url }`; core merges labels from catalog. That reduces spoofed labels and keeps EN/VI consistent.
- **Trade-off:** Requires an API contract and versioning when steps change; listings created under an old template need read-time merge or snapshot-only storage.

### 2. Why store labels in JSON at all?

- **Pros:** Historical accuracy if copy changes in catalog; no extra join on read; works offline in mobile caches.
- **Cons:** Larger rows; duplicate strings.
- **Hybrid:** Store `step_key` + `image_url` only, resolve labels at read time from catalog by `step_key` (needs catalog available and stable keys).

### 3. Cover image consistency

- Cover is **derived** from steps to avoid conflicting `cover_image_url` vs slots. Any code that assumed “first URL in array = cover” must use **`cover_image_url`** or the same sort_order rule.

### 4. Validation and abuse

- Cap **20** slots (DB + app) to bound payload size and signing work.
- Reject duplicate `step_key` and empty `label` for required UX.
- Optional future: allow `required: false` slots with empty `image_url` for “skipped” optional angles (already supported).

### 5. Storage cleanup

- **Delete listing:** still deletes all unique stored object URLs once.
- **Update listing:** when `image_urls` is replaced, URLs present before but not after are passed to `ImageDeleter` (same interface as profile/listing storage). Failures are ignored after DB update (same philosophy as delete).

### 6. Testing recommendations

- Unit tests around `ValidateAndNormalizeListingImageSteps` (required empty, duplicate keys, cover pick with sort_order ties, URL canonicalization).
- Integration: create → GET → update images → GET; presigned URLs on GET when S3 signer enabled.
- Regression: orders/chat payloads that embed `Listing` still presign nested listing images via `SignListingURLs`.

### 7. Rollout order

1. Deploy **core-service** with this code.
2. Run **DB migration** `026` before traffic hits new binaries (or in same maintenance window).
3. Release **mobile/web** clients that send the new JSON shape (old clients will get **400** on create).

---

**Regenerate OpenAPI after further handler changes:** `make swagger` (see `docs/SWAGGER.md`).

## Android app (`fash-android-mobile`)

- **Step definitions:** `GET {COMMON_SERVICE_BASE_URL}/api/v1/categories/{category_id}/listing-image-setup` — response `listing_image_setup.listing_image_setups[].steps[]` (`step_key`, `label`, `label_vi`, `sort_order`, `required`). The first template with a non-empty `steps` array is used (max 20 steps). If none, the client falls back to the same two-step default as core DB backfill (`front`, `step_2`).
- **Create listing:** After `POST /listings/images`, the client sends `image_urls` as a JSON array of objects (`step_key`, `label`, optional `label_vi`, `sort_order`, `required`, `image_url`) via [ListingRepository.createListing](app/src/main/java/com/pc/fash_android_mobile/data/listing/ListingRepository.kt).
- **Read paths:** Feed, listing detail, orders, and chat parse both legacy `string[]` and step objects via [ListingImageUrlsWire](app/src/main/java/com/pc/fash_android_mobile/data/listing/ListingImageUrlsWire.kt); gallery order follows `sort_order`, cover thumb prefers `cover_image_url` then the smallest `sort_order` with a non-empty `image_url`.

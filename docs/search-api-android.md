# Search API — Android integration

The Explore tab uses **`GET /api/v1/search/listings`** only (legacy **`GET /api/v1/listings/explore`** is not used).

## Endpoint

| Method | Path | Auth |
|--------|------|------|
| GET | `/api/v1/search/listings` | Required (secured client) |

## Query parameters (all optional except pagination)

| Param | Description |
|-------|-------------|
| `q` | Full-text query. **Empty string = browse** (category + tag filters still apply). |
| `category_id` | Category UUID |
| `tags` | Comma-separated aesthetic tags |
| `min_price` / `max_price` | VND |
| `condition` | Item condition |
| `sort` | `recent` (default server), `popular`, `price_asc`, `price_desc` — app uses **`popular`** for browse and **`recent`** when the user entered a non-empty search query |
| `limit` | Page size (default 20, max 50) — app uses **20** |
| `offset` | **0** for first page; **`listings.size`** for “load more” |

## Client code

- **`SearchRepository.searchListings(...)`** — builds the query string (including encoded `q` and `tags`).
- **`ExploreViewModel`** — browse vs search mode, filters, **`hasMore`**, **`loadMore()`** with `offset = listings.size`, deduplication by listing id on append.

## UI / performance

- **`LazyVerticalGrid`** + **`rememberLazyGridState`**
- **`snapshotFlow`** on `gridState.layoutInfo` to trigger **`loadMore()`** when the last visible row is within **3** cells of the end
- Full-width footer row: **loading** (brand primary `CircularProgressIndicator`) while **`isLoadingMore`**; **brand strip** when **`!hasMore`** and the grid is non-empty
- Pull-to-refresh reloads tags, featured sellers, categories, and **first page** of listings

## Related

- Core reference: `core-service-api.md` (Search section)
- Trending tags: `GET /api/v1/search/trending-tags`
- Typed search from the top bar: same `searchListings` with non-empty `q` and **`isSearchMode`**

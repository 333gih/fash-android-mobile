# Seller listing focus — `GET /users/{id}/seller-focus`

## Purpose

Returns **distinct** **categories** (leaf + parent name snapshots), **brands**, and **aesthetic tags** that appear on a seller’s **active** listings. Use this to drive filter chips / “what this seller sells” UI without scanning all listing rows on the client.

## Authentication

**Required** (Bearer access token). The **viewer** must be logged in.

## Path

- **`id`**: Seller **username** (e.g. `johndoe`) or seller **user UUID**, same resolution idea as `GET /users/:id` public profile.

## Blocking

If there is a **block in either direction** between viewer and seller, the API returns **403 Forbidden** (same spirit as follow/search exclusions).

## Data rules (performance)

- **Source**: PostgreSQL only — **three** aggregation queries over `listings` for the given `seller_id`:
  1. **Categories** — `DISTINCT ON (category_id)` from **active** rows; includes `category_name_snapshot`, `parent_category_id`, `parent_category_name_snapshot`.
  2. **Brands** — `DISTINCT ON (brand_id)` where `brand_id IS NOT NULL`.
  3. **Aesthetic tags** — `jsonb_array_elements` on `aesthetic_tags`, then distinct tag id/name; invalid UUID strings in JSON are skipped in the app layer.

- **Listing scope**: **`status = 'active'`** only (sold/reserved/deleted excluded).

- **Typesense**: Not used. This is a **relational aggregate**; listing search remains on Typesense where configured.

## Indexes

For large catalogs, ensure `listings` can filter by seller + status efficiently (e.g. composite index on `(seller_id, status)` if not already present).

## Response shape

```json
{
  "categories": [
    {
      "id": "uuid",
      "name": "Tops",
      "parent_id": "uuid-or-null",
      "parent_name": "Women"
    }
  ],
  "brands": [{ "id": "uuid", "name": "Zara" }],
  "aesthetic_tags": [{ "id": "uuid", "name": "y2k" }]
}
```

Empty arrays mean no matching data (e.g. no brands on any active listing).

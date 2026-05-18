package com.pc.fash_android_mobile.data.sellerpackages

import org.json.JSONArray
import org.json.JSONObject

/** Wire DTO for `GET /api/v1/app/advertising/product-packages`. */
fun parseSellerProductPackagesResponse(raw: String): SellerProductPackagesResponse {
    val root = JSONObject(raw.trim())
    val arr = root.optJSONArray("packages") ?: JSONArray()
    val packages = buildList {
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val pkg = parseSellerProductPackageJson(o) ?: continue
            add(pkg)
        }
    }
    val sorted = packages.sortedWith(
        compareBy<SellerProductPackage> { it.tier.ordinal }.thenBy { it.code },
    )
    return SellerProductPackagesResponse(
        packages = sorted,
        serverNowUtc = root.optString("server_now_utc").takeIf { it.isNotBlank() },
    )
}

private fun parseSellerProductPackageJson(o: JSONObject): SellerProductPackage? {
    val code = o.optString("code").trim()
    if (code.isEmpty()) return null
    val tierRaw = o.optString("tier", "starter").trim().lowercase()
    val tier = when (tierRaw) {
        "growth" -> PackageTier.GROWTH
        "premium" -> PackageTier.PREMIUM
        else -> PackageTier.STARTER
    }
    val features = buildList {
        val arr = o.optJSONArray("features") ?: JSONArray()
        for (i in 0 until arr.length()) {
            val f = arr.optJSONObject(i) ?: continue
            val id = f.optString("id").trim()
            if (id.isEmpty()) continue
            val highlight = f.optString("highlight").trim().takeIf { it.isNotEmpty() }
            val apiName = f.optString("name").trim().takeIf { it.isNotEmpty() }
            add(
                SellerPackageFeature(
                    id = id,
                    included = f.wireBoolean("included", "Included", default = false),
                    highlight = highlight,
                    name = apiName,
                ),
            )
        }
    }
    val badge = o.optString("badge_label").trim().takeIf { it.isNotEmpty() }
    return SellerProductPackage(
        id = o.optString("id"),
        code = code,
        name = o.optString("name"),
        description = o.optString("description"),
        priceVnd = o.optLong("price_vnd", 0L),
        durationDays = o.optInt("duration_days", 30),
        tier = tier,
        isReleased = o.wireReleasedFlag(),
        isBestSeller = o.wireBoolean("is_best_seller", "isBestSeller", "IsBestSeller", default = false),
        badgeLabel = badge,
        active = o.wireBoolean("active", "Active", default = true),
        features = features,
    )
}

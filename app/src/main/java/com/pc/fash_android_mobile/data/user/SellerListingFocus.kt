package com.pc.fash_android_mobile.data.user

/** `GET /api/v1/users/{id}/seller-focus` — active listing aggregates for shop UI. */
data class SellerListingFocus(
    val categories: List<SellerFocusCategory>,
    val brands: List<SellerFocusBrand>,
    val aestheticTags: List<SellerFocusTag>,
) {
    fun isEmpty(): Boolean =
        categories.isEmpty() && brands.isEmpty() && aestheticTags.isEmpty()
}

data class SellerFocusCategory(
    val id: String,
    val name: String,
    val parentId: String?,
    val parentName: String?,
) {
    /** Leaf + parent snapshot for chips, e.g. `Women · Tops`. */
    fun displayLabel(): String = when {
        !parentName.isNullOrBlank() && name.isNotBlank() -> "$parentName · $name"
        else -> name
    }
}

data class SellerFocusBrand(
    val id: String,
    val name: String,
)

data class SellerFocusTag(
    val id: String,
    val name: String,
)

/** Block in either direction — API returns 403. */
class SellerFocusForbiddenException : Exception()

/** No valid Bearer (guest / expired). */
class SellerFocusUnauthorizedException : Exception()

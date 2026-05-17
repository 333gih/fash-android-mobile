package com.pc.fash_android_mobile.data.listing

import android.content.Context

/** Per-user flag: first-visit purchase guide on listing detail. */
class ProductDetailGuideStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasSeenPurchaseGuide(userId: String): Boolean =
        prefs.getBoolean(key(userId), false)

    fun markPurchaseGuideSeen(userId: String) {
        prefs.edit().putBoolean(key(userId), true).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    private fun key(userId: String): String {
        val id = userId.trim().ifBlank { "anonymous" }
        return "pdp_purchase_guide_$id"
    }

    private companion object {
        const val PREFS_NAME = "product_detail_guide"
    }
}

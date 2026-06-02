package com.pc.fash_android_mobile.data.recommendation

import org.json.JSONObject

/** GET /recommendations/context — locale-aware metro + season for discovery. */
data class ShoppingContext(
    val source: String = "national",
    val provinceId: String = "",
    val provinceName: String = "",
    val metroKey: String = "",
    val metroLabel: String = "",
    val macroRegion: String = "",
    val climateZone: String = "",
    val seasonKey: String = "",
    val seasonLabel: String = "",
    val hasContext: Boolean = false,
    val suggestBrowseFromShipping: Boolean = false,
) {
    fun chipLabel(): String? {
        val metro = metroLabel.trim().ifEmpty { provinceName.trim() }
        val season = seasonLabel.trim()
        if (metro.isEmpty() && season.isEmpty()) return null
        if (metro.isEmpty()) return season
        if (season.isEmpty()) return metro
        return "$metro · $season"
    }

    companion object {
        fun fromJson(o: JSONObject?): ShoppingContext? {
            if (o == null) return null
            return ShoppingContext(
                source = o.optString("source", "national"),
                provinceId = o.optString("province_id", o.optString("provinceId", "")),
                provinceName = o.optString("province_name", o.optString("provinceName", "")),
                metroKey = o.optString("metro_key", o.optString("metroKey", "")),
                metroLabel = o.optString("metro_label", o.optString("metroLabel", "")),
                macroRegion = o.optString("macro_region", o.optString("macroRegion", "")),
                climateZone = o.optString("climate_zone", o.optString("climateZone", "")),
                seasonKey = o.optString("season_key", o.optString("seasonKey", "")),
                seasonLabel = o.optString("season_label", o.optString("seasonLabel", "")),
                hasContext = o.optBoolean("has_context", o.optBoolean("hasContext", false)),
                suggestBrowseFromShipping = o.optBoolean(
                    "suggest_browse_from_shipping",
                    o.optBoolean("suggestBrowseFromShipping", false),
                ),
            )
        }
    }
}

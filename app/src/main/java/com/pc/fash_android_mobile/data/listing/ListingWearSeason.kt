package com.pc.fash_android_mobile.data.listing

/** Wear metadata for seasonal recommendations — mirrors core-service `listing-wear-season.md`. */
object ListingWearSeason {
    data class Option(val id: String, val labelVi: String, val labelEn: String) {
        fun label(localeVi: Boolean): String = if (localeVi) labelVi else labelEn
    }

    val seasonOptions = listOf(
        Option("dry_hot", "Mùa khô nóng", "Dry hot season"),
        Option("rainy", "Mùa mưa", "Rainy season"),
        Option("cold_dry", "Mùa lạnh khô", "Cool dry season"),
        Option("hot_humid", "Mùa nóng ẩm", "Hot humid season"),
        Option("mild", "Mùa dịu", "Mild season"),
        Option("dry", "Mùa khô", "Dry season"),
        Option("cool_dry", "Mùa se lạnh", "Cool dry season"),
        Option("warm_rain", "Mùa mưa ấm", "Warm rainy season"),
    )

    val climateZoneOptions = listOf(
        Option("south_tropical", "Nam Bộ nhiệt đới", "South tropical"),
        Option("north_subtropical", "Bắc Bộ cận nhiệt", "North subtropical"),
        Option("central_rainy", "Miền Trung mưa nhiều", "Central rainy"),
        Option("highland_cool", "Tây Nguyên mát", "Highland cool"),
    )

    val macroRegionOptions = listOf(
        Option("south", "Miền Nam", "South"),
        Option("north", "Miền Bắc", "North"),
        Option("central", "Miền Trung", "Central"),
        Option("highland", "Tây Nguyên", "Highland"),
    )

    fun labelForSeasonKey(key: String, localeVi: Boolean): String =
        seasonOptions.firstOrNull { it.id == key.trim().lowercase() }?.label(localeVi) ?: key

    fun summary(
        seasonKeys: List<String>,
        climateZones: List<String>,
        macroRegions: List<String>,
        yearRoundWear: Boolean,
        localeVi: Boolean,
    ): String? {
        val parts = mutableListOf<String>()
        if (yearRoundWear) {
            parts.add(if (localeVi) "Mặc quanh năm" else "Year-round wear")
        }
        val seasons = seasonKeys.map { labelForSeasonKey(it, localeVi) }.filter { it.isNotBlank() }
        if (seasons.isNotEmpty()) parts.add(seasons.joinToString(", "))
        val zones = climateZones.mapNotNull { z -> climateZoneOptions.firstOrNull { it.id == z }?.label(localeVi) }
        if (zones.isNotEmpty()) parts.add(zones.joinToString(", "))
        val regions = macroRegions.mapNotNull { r -> macroRegionOptions.firstOrNull { it.id == r }?.label(localeVi) }
        if (regions.isNotEmpty()) parts.add(regions.joinToString(", "))
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }
}

package com.pc.fash_android_mobile.data.recommendation

data class OutfitEventPayload(
    val eventType: String,
    val setId: String,
    val listingId: String = "",
    val surface: String,
    val sessionId: String = "",
)

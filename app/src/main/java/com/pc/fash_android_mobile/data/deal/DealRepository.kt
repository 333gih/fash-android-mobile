package com.pc.fash_android_mobile.data.deal

import com.pc.fash_android_mobile.config.AppEnvironment
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

/**
 * Offline / in-person deals (parallel to paid orders).
 * Paths: `api/v1/deals`, `.../complete`, `.../cancel`, `.../review`.
 */
class DealRepository(
    private val securedClient: OkHttpClient,
) {

    fun createDeal(
        listingId: String,
        conversationId: String,
        meetingLocationUrl: String,
        meetingAtRfc3339: String,
        meetingAppointmentId: String? = null,
        agreedPriceVnd: Long? = null,
    ): Result<DealRecord> = runCatching {
        val json = JSONObject()
            .put("listing_id", listingId.trim())
            .put("conversation_id", conversationId.trim())
            .put("meeting_location_url", meetingLocationUrl.trim())
            .put("meeting_at", meetingAtRfc3339.trim())
        meetingAppointmentId?.trim()?.takeIf { it.isNotEmpty() }?.let {
            json.put("meeting_appointment_id", it)
        }
        agreedPriceVnd?.takeIf { it > 0 }?.let { json.put("agreed_price", it) }
        val body = postJson(AppEnvironment.apiPath("api/v1/deals"), json.toString())
        parseDeal(body)
    }

    fun completeDeal(dealId: String): Result<DealRecord> = runCatching {
        val id = dealId.trim()
        if (id.isEmpty()) error("deal id required")
        val body = postJson(AppEnvironment.apiPath("api/v1/deals/$id/complete"), "{}")
        parseDeal(body)
    }

    fun cancelDeal(dealId: String): Result<Unit> = runCatching {
        val id = dealId.trim()
        if (id.isEmpty()) error("deal id required")
        postJson(AppEnvironment.apiPath("api/v1/deals/$id/cancel"), "{}")
    }

    fun submitDealReview(
        dealId: String,
        rating: Int,
        comment: String?,
        badgeIds: List<ReviewBadgeRefPayload> = emptyList(),
    ): Result<Unit> = runCatching {
        val id = dealId.trim()
        if (id.isEmpty()) error("deal id required")
        val json = JSONObject().put("rating", rating.coerceIn(1, 5))
        comment?.trim()?.takeIf { it.isNotEmpty() }?.let { json.put("comment", it) }
        if (badgeIds.isNotEmpty()) {
            val arr = JSONArray()
            badgeIds.forEach { b ->
                arr.put(
                    JSONObject().apply {
                        put("id", b.id)
                        b.slug?.takeIf { it.isNotBlank() }?.let { put("slug", it) }
                        b.name?.takeIf { it.isNotBlank() }?.let { put("name", it) }
                        b.emoji?.takeIf { it.isNotBlank() }?.let { put("emoji", it) }
                    },
                )
            }
            json.put("badge_ids", arr)
        }
        postJson(AppEnvironment.apiPath("api/v1/deals/$id/review"), json.toString())
    }

    private fun postJson(url: String, json: String): String =
        securedClient.newCall(
            Request.Builder()
                .url(url)
                .post(json.toRequestBody(JSON_MEDIA))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("User-Agent", "FashAndroid/1.0")
                .build(),
        ).execute().use { response ->
            val b = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val msg = try {
                    JSONObject(b).optString("error", b).ifBlank { b }
                } catch (_: Exception) {
                    b
                }
                error("HTTP ${response.code}: $msg")
            }
            b
        }

    private fun parseDeal(raw: String): DealRecord {
        val trimmed = raw.trim()
        val o = when {
            trimmed.startsWith("{") -> JSONObject(trimmed)
            else -> JSONObject("{}")
        }
        val root = if (o.has("data") && !o.isNull("data")) o.getJSONObject("data") else o
        return DealRecord(
            dealId = root.optString("id", root.optString("ID", root.optString("deal_id", ""))),
            status = root.optString("status", root.optString("Status", "")).lowercase(),
            listingId = root.optString("listing_id", root.optString("ListingID", "")),
            conversationId = root.optString("conversation_id", root.optString("ConversationID", "")),
            agreedPriceVnd = root.optLong("agreed_price", root.optLong("AgreedPrice", root.optLong("agreed_price_vnd", 0L))),
            meetingLocationUrl = root.optString("meeting_location_url", root.optString("MeetingLocationURL", "")),
            meetingAt = root.optString("meeting_at", root.optString("MeetingAt", "")),
        )
    }
}

data class DealRecord(
    val dealId: String,
    val status: String,
    val listingId: String = "",
    val conversationId: String = "",
    val agreedPriceVnd: Long = 0L,
    val meetingLocationUrl: String = "",
    val meetingAt: String = "",
)

data class ReviewBadgeRefPayload(
    val id: String,
    val slug: String? = null,
    val name: String? = null,
    val emoji: String? = null,
)

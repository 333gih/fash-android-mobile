package com.pc.fash_android_mobile.data.advertising

import android.util.Log
import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.http.CoreServiceErrors
import com.pc.fash_android_mobile.data.http.CoreServiceHttpException
import com.pc.fash_android_mobile.network.PublicBrowseHttp
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

private const val TAG = "AdvertisingRepository"
private const val USER_AGENT = "FashAndroid/1.0"

/**
 * Core-service advertising CMS.
 *
 * Guest / logged-out browse: `GET /api/v1/public/app/advertising/slides` via [PublicBrowseHttp] (client attestation).
 * Signed-in: `GET /api/v1/app/advertising/slides` with Bearer (same handler on core-service).
 */
class AdvertisingRepository(
    private val securedClient: OkHttpClient,
    private val publicBrowseClient: OkHttpClient?,
    private val localeTagProvider: () -> String = { "vi" },
) {

    private fun throwHttp(httpCode: Int, body: String): Nothing =
        throw CoreServiceHttpException(httpCode, CoreServiceErrors.parseErrorMessage(httpCode, body))

    /**
     * @param placement e.g. `promo_slider_main` (core-service default).
     * @param publicBrowse when true, uses `GET /api/v1/public/app/advertising/slides` (guest shell).
     */
    fun getSlides(
        placement: String = "promo_slider_main",
        publicBrowse: Boolean = false,
    ): Result<AppAdvertisingSlidesResponse> = runCatching {
        val q = if (placement.isBlank()) "promo_slider_main" else placement.trim()
        if (publicBrowse) {
            return@runCatching getSlidesPublic(q)
        }
        val relative =
            "api/v1/app/advertising/slides?placement=${java.net.URLEncoder.encode(q, Charsets.UTF_8.name())}"
        val urls = AppEnvironment.coreApiCandidateUrls(relative)
        var last: Exception? = null
        for (url in urls) {
            try {
                val raw = executeGet(url, securedClient)
                return@runCatching parseAppAdvertisingSlidesResponse(raw)
            } catch (e: Exception) {
                last = e
            }
        }
        if (PublicBrowseHttp.isConfigured() && publicBrowseClient != null) {
            Log.w(TAG, "getSlides secured failed, trying public browse placement=$q", last)
            return@runCatching getSlidesPublic(q)
        }
        Log.w(TAG, "getSlides failed placement=$q urls=$urls", last)
        throw last ?: IllegalStateException("advertising slides request failed")
    }

    /** Guest promo carousel — mirrors [com.pc.fash_android_mobile.data.editorial.EditorialGuideRepository]. */
    private fun getSlidesPublic(placement: String): AppAdvertisingSlidesResponse {
        val client = publicBrowseClient ?: error("Public browse HTTP client is not configured")
        val url = PublicBrowseHttp.publicApiPath("app/advertising/slides")
            .toHttpUrlOrNull()
            ?.newBuilder()
            ?.addQueryParameter("placement", placement)
            ?.build()
            ?.toString()
            ?: error("invalid public advertising slides url")
        Log.d(TAG, "getSlidesPublic GET $url")
        val raw = executeGet(url, client)
        return parseAppAdvertisingSlidesResponse(raw)
    }

    private fun executeGet(url: String, client: OkHttpClient): String {
        val locale = localeTagProvider().trim().ifBlank { "vi" }
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/json")
            .header("Accept-Language", locale)
            .header("X-Fash-Lang", locale)
            .header("User-Agent", USER_AGENT)
            .build()
        return client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throwHttp(response.code, body)
            body
        }
    }
}

package com.pc.fash_android_mobile.data.advertising

import android.util.Log
import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.http.CoreServiceErrors
import com.pc.fash_android_mobile.data.http.CoreServiceHttpException
import okhttp3.OkHttpClient
import okhttp3.Request

private const val TAG = "AdvertisingRepository"
private const val USER_AGENT = "FashAndroid/1.0"

/**
 * Core-service advertising CMS (`GET …/app/advertising/slides`).
 *
 * Uses [securedClient] (SecuredApiClient) so every request includes `Authorization: Bearer <access_token>`
 * when the user is logged in — same as orders, listings, chat, etc.
 */
class AdvertisingRepository(
    private val securedClient: OkHttpClient,
    private val localeTagProvider: () -> String = { "vi" },
) {

    private fun throwHttp(httpCode: Int, body: String): Nothing =
        throw CoreServiceHttpException(httpCode, CoreServiceErrors.parseErrorMessage(httpCode, body))

    private fun executeGet(url: String): String {
        val locale = localeTagProvider().trim().ifBlank { "vi" }
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/json")
            .header("Accept-Language", locale)
            .header("X-Fash-Lang", locale)
            .header("User-Agent", USER_AGENT)
            .build()
        return securedClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throwHttp(response.code, body)
            body
        }
    }

    /**
     * @param placement e.g. `promo_slider_main` (core-service default).
     */
    fun getSlides(placement: String = "promo_slider_main"): Result<AppAdvertisingSlidesResponse> = runCatching {
        val q = if (placement.isBlank()) "promo_slider_main" else placement.trim()
        val relative =
            "api/v1/app/advertising/slides?placement=${java.net.URLEncoder.encode(q, Charsets.UTF_8.name())}"
        val urls = AppEnvironment.coreApiCandidateUrls(relative)
        var last: Exception? = null
        for (url in urls) {
            try {
                val raw = executeGet(url)
                return@runCatching parseAppAdvertisingSlidesResponse(raw)
            } catch (e: Exception) {
                last = e
            }
        }
        Log.w(TAG, "getSlides failed (Bearer required) placement=$q urls=$urls", last)
        throw last ?: IllegalStateException("advertising slides request failed")
    }
}

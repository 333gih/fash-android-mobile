package com.pc.fash_android_mobile.data.advertising

import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.http.CoreServiceErrors
import com.pc.fash_android_mobile.data.http.CoreServiceHttpException
import okhttp3.OkHttpClient
import okhttp3.Request

private const val USER_AGENT = "FashAndroid/1.0"

/**
 * Core-service public advertising CMS (`GET …/app/advertising/slides`).
 * Uses the same secured client as the rest of the app (Bearer when logged in).
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
            .addHeader("Accept-Language", locale)
            .addHeader("X-Fash-Lang", locale)
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
        val path = "api/v1/app/advertising/slides?placement=${java.net.URLEncoder.encode(q, Charsets.UTF_8.name())}"
        val raw = executeGet(AppEnvironment.apiPath(path))
        parseAppAdvertisingSlidesResponse(raw)
    }
}

package com.pc.fash_android_mobile.data.promo

import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.http.CoreServiceErrors
import com.pc.fash_android_mobile.data.http.CoreServiceHttpException
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Pull backup for admin promo interstitials (`GET /api/v1/app/promo-interstitials/active`).
 * Catalog is synced to Redis by fash-admin-core-service; core-service serves this endpoint.
 */
class AppPromoInterstitialRepository(
    private val securedClient: OkHttpClient,
) {
    fun fetchActiveCampaigns(): Result<List<AppPromoCampaign>> = runCatching {
        val raw = executeGet(AppEnvironment.apiPath("api/v1/app/promo-interstitials/active"))
        val root = JSONObject(raw)
        val arr = root.optJSONArray("campaigns") ?: return@runCatching emptyList()
        val out = mutableListOf<AppPromoCampaign>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            parseRemoteAppPromoPayload(item)?.toAppPromoCampaign()?.let { out.add(it) }
        }
        out.sortedByDescending { it.priority }
    }

    private fun executeGet(url: String): String {
        val request = Request.Builder().url(url).get().build()
        return securedClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw CoreServiceHttpException(
                    response.code,
                    CoreServiceErrors.parseErrorMessage(response.code, body),
                )
            }
            body
        }
    }
}

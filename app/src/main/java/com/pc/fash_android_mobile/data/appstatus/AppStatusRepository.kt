package com.pc.fash_android_mobile.data.appstatus

import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.http.CoreServiceErrors
import com.pc.fash_android_mobile.data.http.CoreServiceHttpException
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class AppMaintenanceStatus(
    val maintenance: Boolean,
    val title: String?,
    val message: String?,
) {
    companion object {
        val Open = AppMaintenanceStatus(maintenance = false, title = null, message = null)
    }
}

/**
 * Public kill-switch: `GET /api/v1/app/status` (no attestation).
 */
class AppStatusRepository(
    private val securedClient: OkHttpClient,
    private val localeTagProvider: () -> String = { "vi" },
) {
    fun fetch(): Result<AppMaintenanceStatus> = runCatching {
        val urls = AppEnvironment.coreApiCandidateUrls("api/v1/app/status")
        var last: Exception? = null
        for (url in urls) {
            try {
                return@runCatching parse(executeGet(url))
            } catch (e: Exception) {
                last = e
            }
        }
        throw last ?: IllegalStateException("app status request failed")
    }

    private fun executeGet(url: String): String {
        val locale = localeTagProvider().trim().ifBlank { "vi" }
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/json")
            .header("Accept-Language", locale)
            .header("X-Fash-Lang", locale)
            .header("User-Agent", "FashAndroid/1.0")
            .build()
        return securedClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw CoreServiceHttpException(response.code, CoreServiceErrors.parseErrorMessage(response.code, body))
            }
            body
        }
    }

    private fun parse(raw: String): AppMaintenanceStatus {
        val root = JSONObject(raw)
        val payload = if (root.has("data") && root.opt("data") is JSONObject) {
            root.getJSONObject("data")
        } else {
            root
        }
        val on = payload.optBoolean("maintenance", false) || payload.optBoolean("enabled", false)
        val title = payload.optString("title").trim().ifEmpty { null }
        val message = payload.optString("message").trim().ifEmpty { null }
        return AppMaintenanceStatus(maintenance = on, title = title, message = message)
    }
}

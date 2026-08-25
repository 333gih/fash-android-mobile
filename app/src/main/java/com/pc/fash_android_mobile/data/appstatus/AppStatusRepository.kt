package com.pc.fash_android_mobile.data.appstatus

import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.http.CoreServiceErrors
import com.pc.fash_android_mobile.data.http.CoreServiceHttpException
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AppMaintenanceStatus(
    val maintenance: Boolean,
    val title: String?,
    val message: String?,
) {
    companion object {
        val Open = AppMaintenanceStatus(maintenance = false, title = null, message = null)

        fun parse(root: JSONObject): AppMaintenanceStatus {
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

        fun parseJson(raw: String): AppMaintenanceStatus? = runCatching {
            parse(JSONObject(raw))
        }.getOrNull()

        fun fromPushData(data: Map<String, String>): AppMaintenanceStatus? {
            val type = data["type"]?.trim()?.lowercase().orEmpty()
            if (type != "app.maintenance" && type != "app.status.changed") return null
            val raw = (data["maintenance"] ?: data["enabled"] ?: "").trim().lowercase()
            val on = raw == "true" || raw == "1" || raw == "yes"
            return AppMaintenanceStatus(
                maintenance = on,
                title = data["title"]?.trim()?.ifEmpty { null },
                message = data["message"]?.trim()?.ifEmpty { null },
            )
        }
    }
}

/**
 * Public kill-switch: `GET /api/v1/app/status` with a plain client (no JWT).
 */
class AppStatusRepository(
    private val localeTagProvider: () -> String = { "vi" },
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .build()

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
        return client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw CoreServiceHttpException(response.code, CoreServiceErrors.parseErrorMessage(response.code, body))
            }
            body
        }
    }

    private fun parse(raw: String): AppMaintenanceStatus = AppMaintenanceStatus.parse(JSONObject(raw))
}

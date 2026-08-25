package com.pc.fash_android_mobile.data.appstatus

import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.http.CoreServiceErrors
import com.pc.fash_android_mobile.data.http.CoreServiceHttpException
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.Instant
import java.util.concurrent.TimeUnit

data class AppMaintenanceStatus(
    val maintenance: Boolean,
    val phase: String,
    val mode: String,
    val startsAtIso: String?,
    val countdownSeconds: Int,
    val title: String?,
    val message: String?,
) {
    val isWarning: Boolean get() = !maintenance && phase.equals("warning", ignoreCase = true)
    val isLocked: Boolean get() = maintenance || phase.equals("maintenance", ignoreCase = true)
    val sawRestricted: Boolean get() = isWarning || isLocked

    fun remainingSeconds(nowMillis: Long = System.currentTimeMillis()): Int {
        val iso = startsAtIso?.trim().orEmpty()
        if (iso.isNotEmpty()) {
            val epoch = runCatching { Instant.parse(iso).toEpochMilli() }.getOrNull()
            if (epoch != null) {
                return ((epoch - nowMillis) / 1000L).toInt().coerceAtLeast(0)
            }
        }
        return countdownSeconds.coerceAtLeast(0)
    }

    fun pollIntervalMs(): Long = when {
        isWarning -> 1_000L
        isLocked -> 5_000L
        else -> 8_000L
    }

    companion object {
        val Open = AppMaintenanceStatus(
            maintenance = false,
            phase = "open",
            mode = "none",
            startsAtIso = null,
            countdownSeconds = 0,
            title = null,
            message = null,
        )

        fun parse(root: JSONObject): AppMaintenanceStatus {
            val payload = if (root.has("data") && root.opt("data") is JSONObject) {
                root.getJSONObject("data")
            } else {
                root
            }
            val phase = payload.optString("phase").trim().ifEmpty { "" }
            val locked = payload.optBoolean("maintenance", false) ||
                payload.optBoolean("enabled", false) ||
                phase.equals("maintenance", ignoreCase = true)
            val resolvedPhase = when {
                locked -> "maintenance"
                phase.equals("warning", ignoreCase = true) -> "warning"
                else -> phase.ifEmpty { "open" }
            }
            return AppMaintenanceStatus(
                maintenance = locked,
                phase = resolvedPhase,
                mode = payload.optString("mode").trim().ifEmpty { "none" },
                startsAtIso = payload.optString("starts_at").trim().ifEmpty { null },
                countdownSeconds = payload.optInt("countdown_seconds", 0),
                title = payload.optString("title").trim().ifEmpty { null },
                message = payload.optString("message").trim().ifEmpty { null },
            )
        }

        fun parseJson(raw: String): AppMaintenanceStatus? = runCatching {
            parse(JSONObject(raw))
        }.getOrNull()

        fun fromPushData(data: Map<String, String>): AppMaintenanceStatus? {
            val type = data["type"]?.trim()?.lowercase().orEmpty()
            if (
                type != "app.maintenance" &&
                type != "app.status.changed" &&
                type != "admin.app_maintenance"
            ) {
                return null
            }
            val phase = data["phase"]?.trim().orEmpty()
            val raw = (data["maintenance"] ?: data["enabled"] ?: "").trim().lowercase()
            val on = raw == "true" || raw == "1" || raw == "yes" || phase.equals("maintenance", ignoreCase = true)
            val resolvedPhase = when {
                on -> "maintenance"
                phase.equals("warning", ignoreCase = true) -> "warning"
                else -> phase.ifEmpty { if (on) "maintenance" else "open" }
            }
            return AppMaintenanceStatus(
                maintenance = on,
                phase = resolvedPhase,
                mode = data["mode"]?.trim().orEmpty().ifEmpty { "none" },
                startsAtIso = data["starts_at"]?.trim()?.ifEmpty { null },
                countdownSeconds = data["countdown_seconds"]?.toIntOrNull() ?: 0,
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

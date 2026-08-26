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
    val updatedAtIso: String? = null,
    val resumeMoment: String? = null,
    val releaseNotesTitle: String? = null,
    val releaseNotes: String? = null,
) {
    val isWarning: Boolean get() = !maintenance && phase.equals("warning", ignoreCase = true)
    val isLocked: Boolean get() = maintenance || phase.equals("maintenance", ignoreCase = true)
    val sawRestricted: Boolean get() = isWarning || isLocked

    /** Countdown elapsed while app was backgrounded — lock locally until server confirms. */
    fun isEffectivelyLocked(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (isLocked) return true
        if (isWarning && remainingSeconds(nowMillis) <= 0) return true
        return false
    }

    fun isEffectivelyWarning(nowMillis: Long = System.currentTimeMillis()): Boolean =
        isWarning && !isEffectivelyLocked(nowMillis)

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

    /**
     * When leaving warning/lock, prefer the server [resumeMoment]; otherwise infer so the
     * return UI still shows if the payload omitted the field.
     */
    fun inferredResumeMoment(previous: AppMaintenanceStatus): String? {
        if (!previous.sawRestricted || sawRestricted) return null
        val fromServer = resumeMoment?.trim().orEmpty()
        if (fromServer.isNotEmpty()) return fromServer
        return if (previous.isLocked) "back_online" else "warning_cleared"
    }

    fun resumeDedupeToken(previous: AppMaintenanceStatus): String {
        updatedAtIso?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        val moment = inferredResumeMoment(previous) ?: "open"
        return "local:$moment:${previous.phase}"
    }

    /** Reopen-sheet title: admin release-notes title only (not the lock-screen title). */
    fun resumeTitle(previous: AppMaintenanceStatus): String? =
        firstNonBlank(releaseNotesTitle, previous.releaseNotesTitle)

    /** Reopen body: admin release notes, else maintenance message, else null (app generic copy). */
    fun resumeBody(previous: AppMaintenanceStatus): String? =
        firstNonBlank(releaseNotes, previous.releaseNotes, message, previous.message)

    companion object {
        val Open = AppMaintenanceStatus(
            maintenance = false,
            phase = "open",
            mode = "none",
            startsAtIso = null,
            countdownSeconds = 0,
            title = null,
            message = null,
            updatedAtIso = null,
            resumeMoment = null,
            releaseNotesTitle = null,
            releaseNotes = null,
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
                updatedAtIso = payload.optString("updated_at").trim().ifEmpty { null },
                resumeMoment = payload.optString("resume_moment").trim().ifEmpty { null },
                releaseNotesTitle = payload.optString("release_notes_title").trim().ifEmpty { null },
                releaseNotes = payload.optString("release_notes").trim().ifEmpty { null },
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
                updatedAtIso = data["updated_at"]?.trim()?.ifEmpty { null },
                resumeMoment = data["resume_moment"]?.trim()?.ifEmpty { null },
                releaseNotesTitle = data["release_notes_title"]?.trim()?.ifEmpty { null },
                releaseNotes = data["release_notes"]?.trim()?.ifEmpty { null },
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
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .writeTimeout(4, TimeUnit.SECONDS)
        .callTimeout(6, TimeUnit.SECONDS)
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

private fun firstNonBlank(vararg values: String?): String? {
    for (raw in values) {
        val t = raw?.trim().orEmpty()
        if (t.isNotEmpty()) return t
    }
    return null
}

package com.pc.fash_android_mobile.data.recommendation

import org.json.JSONArray
import org.json.JSONObject

/**
 * Session-scoped A/B assignment from recommendation API meta/headers (`experiment_key:variant`).
 */
object RecExperimentContext {
    private val lock = Any()
    private var experimentId: String? = null
    private var configVersionId: String? = null

    fun update(configVersionId: String?, activeExperiments: JSONArray?) {
        synchronized(lock) {
            this.configVersionId = configVersionId?.trim()?.takeIf { it.isNotEmpty() }
            experimentId = experimentIdFrom(activeExperiments)
        }
    }

    fun update(experimentIdHeader: String?) {
        val trimmed = experimentIdHeader?.trim()?.takeIf { it.isNotEmpty() } ?: return
        synchronized(lock) {
            experimentId = trimmed
        }
    }

    fun experimentIdForFeedEvents(): String? = synchronized(lock) { experimentId }

    fun clear() {
        synchronized(lock) {
            experimentId = null
            configVersionId = null
        }
    }

    fun isRecommendationSurface(surface: String): Boolean {
        val s = surface.trim().lowercase()
        if (s.isEmpty()) return false
        if (s == FeedSurfaces.APP_OPEN || s == FeedSurfaces.NOTIFICATION_OPEN) return false
        val recSurfaces = setOf(
            "for_you", "recommendation_for_you", "style_picks", "recommendation_style",
            "similar_to_saved", "seasonal_near_you", "hunt_today", "explore", "home",
            "recommendation_continue", "recommendation_daily_digest", "continue_browsing",
        )
        return recSurfaces.contains(s) || s.startsWith("recommendation_")
    }

    fun parseMeta(data: JSONObject) {
        val meta = data.optJSONObject("recommendation_meta") ?: return
        update(
            configVersionId = meta.optString("config_version_id").takeIf { it.isNotBlank() },
            activeExperiments = meta.optJSONArray("active_experiments"),
        )
    }

    fun applyResponseHeaders(headers: okhttp3.Headers) {
        val id = headers["X-Rec-Experiment-Id"]?.trim()?.takeIf { it.isNotEmpty() } ?: return
        update(id)
    }

    private fun experimentIdFrom(experiments: JSONArray?): String? {
        if (experiments == null || experiments.length() == 0) return null
        val first = experiments.optJSONObject(0) ?: return null
        val key = first.optString("experiment_key").trim()
        val variant = first.optString("variant").trim()
        if (key.isEmpty()) return null
        return if (variant.isEmpty()) key else "$key:$variant"
    }
}

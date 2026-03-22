package com.pc.fash_android_mobile.data.search

import com.pc.fash_android_mobile.config.AppEnvironment
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

/**
 * Search API client. Uses secured client for protected endpoints.
 */
class SearchRepository(
    private val securedClient: OkHttpClient,
) {

    fun getTrendingTags(): Result<List<String>> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/search/trending-tags")
        val body = executeGet(url)
        parseStringArray(body)
    }

    private fun executeGet(url: String): String {
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/json")
            .header("User-Agent", "FashAndroid/1.0")
            .build()
        return securedClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val msg = try { JSONObject(body).optString("error", body).ifBlank { body } } catch (_: Exception) { body }
                error("HTTP ${response.code}: $msg")
            }
            body
        }
    }

    private fun parseStringArray(json: String): List<String> {
        val raw = json.trim()
        return when {
            raw.startsWith("[") -> {
                val arr = JSONArray(raw)
                (0 until arr.length()).map { arr.optString(it, "") }.filter { it.isNotBlank() }
            }
            else -> try {
                val obj = JSONObject(raw)
                if (obj.has("data")) {
                    val arr = obj.getJSONArray("data")
                    (0 until arr.length()).map { arr.optString(it, "") }.filter { it.isNotBlank() }
                } else emptyList()
            } catch (_: Exception) { emptyList() }
        }
    }
}

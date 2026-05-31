package com.pc.fash_android_mobile.data.user

import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.http.CoreServiceErrors
import com.pc.fash_android_mobile.data.http.CoreServiceHttpException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Core-service GET/PUT `/api/v1/users/me/notification-preferences`.
 */
class NotificationPreferencesRepository(
    private val securedClient: OkHttpClient,
) {
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private fun baseUrl(): String =
        AppEnvironment.apiPath("api/v1/users/me/notification-preferences")

    private fun throwHttp(httpCode: Int, body: String): Nothing =
        throw CoreServiceHttpException(httpCode, CoreServiceErrors.parseErrorMessage(httpCode, body))

    fun getNotificationPreferences(): Result<NotificationPreferences> = runCatching {
        val request = Request.Builder()
            .url(baseUrl())
            .get()
            .header("Accept", "application/json")
            .header("User-Agent", "FashAndroid/1.0")
            .build()
        securedClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throwHttp(response.code, body)
            parseNotificationPreferencesJson(JSONObject(body.trim().ifBlank { "{}" }))
        }
    }

    fun updateNotificationPreferences(prefs: NotificationPreferences): Result<NotificationPreferences> =
        runCatching {
            val request = Request.Builder()
                .url(baseUrl())
                .put(prefs.toPutJson().toString().toRequestBody(jsonMedia))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("User-Agent", "FashAndroid/1.0")
                .build()
            securedClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) throwHttp(response.code, body)
                parseNotificationPreferencesJson(JSONObject(body.trim().ifBlank { "{}" }))
            }
        }
}

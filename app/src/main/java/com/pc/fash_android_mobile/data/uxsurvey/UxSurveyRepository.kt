package com.pc.fash_android_mobile.data.uxsurvey

import android.util.Log
import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.http.CoreServiceErrors
import com.pc.fash_android_mobile.data.http.CoreServiceHttpException
import com.pc.fash_android_mobile.network.PublicBrowseHttp
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "UxSurveyRepository"

/**
 * UX surveys from core-service.
 * - Logged-in: `GET/POST /api/v1/app/ux-surveys/:key`
 * - Guest: `GET/POST /api/v1/public/app/ux-surveys/:key` + `guest_key`
 */
class UxSurveyRepository(
    private val securedClient: OkHttpClient,
    private val publicBrowseClient: OkHttpClient?,
    private val guestSurveyProvider: () -> Boolean,
    private val guestKeyProvider: () -> String,
    private val localeTagProvider: () -> String = { "vi" },
) {
    private fun localeSegment(): String {
        val tag = localeTagProvider().trim().lowercase()
        return if (tag.startsWith("en")) "en" else "vi"
    }

    private fun throwHttp(code: Int, body: String): Nothing =
        throw CoreServiceHttpException(code, CoreServiceErrors.parseErrorMessage(code, body))

    private fun guestKey(): String = guestKeyProvider().trim()

    private fun useGuestApi(): Boolean = guestSurveyProvider()

    private fun executeGet(url: String, client: OkHttpClient): String {
        val locale = localeSegment()
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/json")
            .header("Accept-Language", locale)
            .header("X-Fash-Lang", locale)
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throwHttp(response.code, body)
            return body
        }
    }

    private fun executePost(url: String, client: OkHttpClient, json: JSONObject): String {
        val locale = localeSegment()
        val request = Request.Builder()
            .url(url)
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .header("Accept", "application/json")
            .header("Accept-Language", locale)
            .header("X-Fash-Lang", locale)
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throwHttp(response.code, body)
            return body
        }
    }

    private fun getSurveyUrls(relative: String): List<String> {
        if (useGuestApi()) {
            val path = PublicBrowseHttp.publicApiPath(relative.removePrefix("api/v1/public/"))
            val gk = guestKey()
            val url = path.toHttpUrlOrNull()
                ?.newBuilder()
                ?.apply { if (gk.isNotEmpty()) addQueryParameter("guest_key", gk) }
                ?.build()
                ?.toString()
            return listOfNotNull(url)
        }
        return AppEnvironment.coreApiCandidateUrls(relative)
    }

    private fun unwrapSurveyObject(raw: String): JSONObject {
        val root = JSONObject(raw.trim())
        val data = root.optJSONObject("data")
        val container = data ?: root
        return container.getJSONObject("survey")
    }

    fun getSurvey(surveyKey: String): Result<UxSurveyDetail> = runCatching {
        val key = surveyKey.trim()
        require(key.isNotEmpty())
        val enc = java.net.URLEncoder.encode(key, Charsets.UTF_8.name())
        val relative = if (useGuestApi()) {
            "api/v1/public/app/ux-surveys/$enc"
        } else {
            "api/v1/app/ux-surveys/$enc"
        }
        val client = if (useGuestApi()) {
            publicBrowseClient ?: error("Public browse HTTP client is not configured")
        } else {
            securedClient
        }
        val urls = getSurveyUrls(relative)
        var last: Exception? = null
        for (url in urls) {
            try {
                return@runCatching parseSurvey(unwrapSurveyObject(executeGet(url, client)))
            } catch (e: Exception) {
                last = e
            }
        }
        Log.w(TAG, "getSurvey failed key=$key urls=$urls", last)
        throw last ?: IllegalStateException("ux survey GET failed")
    }

    fun submit(surveyKey: String, answers: List<UxSurveyAnswerInput>): Result<Unit> = runCatching {
        val key = surveyKey.trim()
        require(key.isNotEmpty())
        val enc = java.net.URLEncoder.encode(key, Charsets.UTF_8.name())
        val arr = JSONArray()
        for (a in answers) {
            val o = JSONObject().put("question_id", a.questionId)
            a.rating?.let { o.put("answer_rating", it) }
            a.text?.takeIf { it.isNotBlank() }?.let { o.put("answer_text", it) }
            if (a.values.isNotEmpty()) {
                o.put("answer_values", JSONArray(a.values))
            }
            arr.put(o)
        }
        val body = JSONObject().put("answers", arr)
        if (useGuestApi()) {
            val gk = guestKey()
            require(gk.isNotEmpty()) { "guest_key required" }
            body.put("guest_key", gk)
        }
        val relative = if (useGuestApi()) {
            "api/v1/public/app/ux-surveys/$enc/responses"
        } else {
            "api/v1/app/ux-surveys/$enc/responses"
        }
        val client = if (useGuestApi()) {
            publicBrowseClient ?: error("Public browse HTTP client is not configured")
        } else {
            securedClient
        }
        val urls = if (useGuestApi()) {
            listOfNotNull(PublicBrowseHttp.publicApiPath("app/ux-surveys/$enc/responses"))
        } else {
            AppEnvironment.coreApiCandidateUrls(relative)
        }
        var last: Exception? = null
        for (url in urls) {
            try {
                executePost(url, client, body)
                return@runCatching
            } catch (e: Exception) {
                last = e
            }
        }
        Log.w(TAG, "submit failed key=$key urls=$urls", last)
        throw last ?: IllegalStateException("ux survey POST failed")
    }

    private fun parseSurvey(o: JSONObject): UxSurveyDetail {
        val questions = o.optJSONArray("questions") ?: JSONArray()
        val outQ = ArrayList<UxSurveyQuestion>(questions.length())
        for (i in 0 until questions.length()) {
            val q = questions.getJSONObject(i)
            outQ.add(
                UxSurveyQuestion(
                    id = q.optString("id"),
                    questionKey = q.optString("question_key"),
                    prompt = q.optString("prompt"),
                    questionType = q.optString("question_type", "rating"),
                    required = q.optBoolean("required", true),
                ),
            )
        }
        return UxSurveyDetail(
            id = o.optString("id"),
            surveyKey = o.optString("survey_key"),
            title = o.optString("title"),
            description = o.optString("description"),
            submitted = o.optBoolean("submitted", false),
            questions = outQ,
        )
    }
}

data class UxSurveyDetail(
    val id: String,
    val surveyKey: String,
    val title: String,
    val description: String,
    val submitted: Boolean,
    val questions: List<UxSurveyQuestion>,
)

data class UxSurveyQuestion(
    val id: String,
    val questionKey: String,
    val prompt: String,
    val questionType: String,
    val required: Boolean,
)

data class UxSurveyAnswerInput(
    val questionId: String,
    val rating: Int? = null,
    val text: String? = null,
    val values: List<String> = emptyList(),
)

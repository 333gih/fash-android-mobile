package com.pc.fash_android_mobile.data.editorial

import android.util.Log
import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.http.CoreServiceErrors
import com.pc.fash_android_mobile.data.http.CoreServiceHttpException
import com.pc.fash_android_mobile.data.home.HomeEditorialPostStub
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val TAG = "EditorialGuideRepo"
private const val USER_AGENT = "FashAndroid/1.0"

/** Default cover when CMS row has no `cover_image_url` (matches common-service seed fallback). */
const val EDITORIAL_GUIDE_DEFAULT_COVER_URL =
    "https://images.unsplash.com/photo-1523381210434-271e8be1f52b?w=1200&q=80"

/**
 * Public common-service editorial guides (`GET /api/v1/public/editorial-guides`).
 * No Bearer — read-only catalog for Home carousel and in-app reader.
 */
class EditorialGuideRepository(
    private val localeTagProvider: () -> String,
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun localeSegment(): String {
        val tag = localeTagProvider().trim().lowercase()
        return if (tag.startsWith("en")) "en" else "vi"
    }

    private fun throwHttp(code: Int, body: String): Nothing =
        throw CoreServiceHttpException(code, CoreServiceErrors.parseErrorMessage(code, body))

    private fun executeGet(url: String): String {
        val locale = localeSegment()
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/json")
            .header("Accept-Language", locale)
            .header("X-Fash-Lang", locale)
            .header("User-Agent", USER_AGENT)
            .build()
        return client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Log.w(TAG, "GET $url failed code=${response.code} body=${body.take(500)}")
                throwHttp(response.code, body)
            }
            body
        }
    }

    fun listCarousel(limit: Int = 12): Result<List<HomeEditorialPostStub>> =
        listAll(limit = limit, offset = 0).map { it.items }

    data class EditorialGuideListPage(
        val items: List<HomeEditorialPostStub>,
        val hasMore: Boolean,
    )

    fun listAll(limit: Int = 50, offset: Int = 0): Result<EditorialGuideListPage> = runCatching {
        val locale = localeSegment()
        val base = AppEnvironment.commonServicePath("api/v1/public/editorial-guides")
        val url = base.toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("locale", locale)
            ?.addQueryParameter("limit", limit.coerceIn(1, 50).toString())
            ?.addQueryParameter("offset", offset.coerceAtLeast(0).toString())
            ?.build()
            ?.toString()
            ?: error("invalid editorial guides url")
        Log.d(TAG, "listAll GET $url")
        val page = parseCarouselPage(executeGet(url))
        Log.d(TAG, "listAll locale=$locale items=${page.items.size}")
        page
    }

    fun getBySlug(slug: String): Result<EditorialGuideDetail> = runCatching {
        val trimmed = slug.trim()
        require(trimmed.isNotEmpty()) { "slug required" }
        val locale = localeSegment()
        val path = "api/v1/public/editorial-guides/${java.net.URLEncoder.encode(trimmed, Charsets.UTF_8.name())}"
        val url = AppEnvironment.commonServicePath(path).toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("locale", locale)
            ?.build()
            ?.toString()
            ?: error("invalid editorial guide url")
        parseDetail(JSONObject(executeGet(url)).getJSONObject("guide"))
    }

    private fun parseCarouselPage(raw: String): EditorialGuideListPage {
        val root = JSONObject(raw)
        val items = root.optJSONArray("items") ?: return EditorialGuideListPage(emptyList(), false)
        val out = ArrayList<HomeEditorialPostStub>(items.length())
        for (i in 0 until items.length()) {
            val o = items.getJSONObject(i)
            val cover = o.optString("cover_image_url").trim().ifBlank { EDITORIAL_GUIDE_DEFAULT_COVER_URL }
            val exploreCat = o.optString("explore_category_id").trim().ifBlank { null }
            val exploreQ = o.optString("explore_search_query").trim().ifBlank { null }
            out.add(
                HomeEditorialPostStub(
                    id = o.optString("id"),
                    slug = o.optString("slug"),
                    title = o.optString("title"),
                    summary = o.optString("summary"),
                    coverImageUrl = cover,
                    exploreCategoryId = exploreCat,
                    exploreSearchQuery = exploreQ,
                ),
            )
        }
        return EditorialGuideListPage(out, root.optBoolean("has_more", false))
    }

    private fun parseCarousel(raw: String): List<HomeEditorialPostStub> = parseCarouselPage(raw).items

    private fun parseDetail(o: JSONObject): EditorialGuideDetail {
        val cover = o.optString("cover_image_url").trim().ifBlank { EDITORIAL_GUIDE_DEFAULT_COVER_URL }
        val exploreCat = o.optString("explore_category_id").trim().ifBlank { null }
        val exploreQ = o.optString("explore_search_query").trim().ifBlank { null }
        return EditorialGuideDetail(
            id = o.optString("id"),
            slug = o.optString("slug"),
            title = o.optString("title"),
            summary = o.optString("summary"),
            bodyMarkdown = o.optString("body_markdown"),
            coverImageUrl = cover,
            exploreCategoryId = exploreCat,
            exploreSearchQuery = exploreQ,
        )
    }
}

data class EditorialGuideDetail(
    val id: String,
    val slug: String,
    val title: String,
    val summary: String,
    val bodyMarkdown: String,
    val coverImageUrl: String,
    val exploreCategoryId: String? = null,
    val exploreSearchQuery: String? = null,
)

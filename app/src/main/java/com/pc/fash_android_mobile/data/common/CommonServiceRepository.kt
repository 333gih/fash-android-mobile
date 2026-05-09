package com.pc.fash_android_mobile.data.common

import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.http.CoreServiceErrors
import com.pc.fash_android_mobile.data.http.CoreServiceHttpException
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

private const val COMMON_SERVICE_USER_AGENT = "FashAndroid/1.0"

/**
 * Common-service GET catalog (see project ANDROID_API_INTEGRATION.md).
 *
 * Authenticated catalog calls use [securedClient]: same as core — `Accept`, `User-Agent`,
 * optional `X-Internal-Secret`, and `Authorization` Bearer (user JWT from auth-service, or internal service token when logged out).
 *
 * [getHealth] uses a separate client so the health check sends only Accept and User-Agent.
 *
 * @see com.pc.fash_android_mobile.network.SecuredApiClient
 */
class CommonServiceRepository(
    private val securedClient: OkHttpClient,
) {

    /** Same timeouts as SecuredApiClient; no auth interceptors. */
    private val healthClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun throwHttp(httpCode: Int, body: String): Nothing =
        throw CoreServiceHttpException(httpCode, CoreServiceErrors.parseErrorMessage(httpCode, body))

    /** Secured GET: interceptors add Accept, User-Agent, and Bearer when a session exists. */
    private fun executeGet(url: String): String {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        return securedClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throwHttp(response.code, body)
            body
        }
    }

    /** `GET /health` only — no Authorization header (per API doc). */
    private fun executeGetHealth(url: String): String {
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/json")
            .header("User-Agent", COMMON_SERVICE_USER_AGENT)
            .build()
        return healthClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throwHttp(response.code, body)
            body
        }
    }

    private fun apiV1(path: String): String =
        AppEnvironment.commonServicePath("api/v1/${path.trimStart('/')}")

    private fun enc(s: String): String = URLEncoder.encode(s, Charsets.UTF_8.name())

    // --- Health (no /api/v1) ---

    /** `GET /health` — body is JSON string `"ok"`. Unauthenticated (no Bearer). */
    fun getHealth(): Result<String> = runCatching {
        val raw = executeGetHealth(AppEnvironment.commonServiceHealthUrl()).trim()
        when {
            raw.startsWith("\"") && raw.endsWith("\"") && raw.length >= 2 ->
                raw.substring(1, raw.length - 1)
            else -> raw.trim('"')
        }
    }

    // --- Addresses ---

    fun getAddressTree(): Result<List<AddressTreeNode>> = runCatching {
        val body = executeGet(apiV1("addresses/tree"))
        val root = JSONObject(body.trim())
        val tree = root.optJSONArray("tree") ?: JSONArray()
        (0 until tree.length()).mapNotNull { i ->
            tree.optJSONObject(i)?.let { parseAddressTreeNode(it) }
        }
    }

    fun getAddresses(
        level: Int? = null,
        parentId: String? = null,
        current: Boolean = true,
    ): Result<List<CommonAddressDto>> = runCatching {
        val q = mutableListOf<String>()
        q.add("current=$current")
        level?.let { q.add("level=$it") }
        parentId?.takeIf { it.isNotBlank() }?.let { q.add("parent_id=${enc(it)}") }
        val url = "${apiV1("addresses")}?${q.joinToString("&")}"
        parseAddressList(JSONObject(executeGet(url).trim()))
    }

    fun getAddressHistory(
        current: Boolean? = null,
        offset: Int = 0,
        limit: Int = 20,
    ): Result<AddressHistoryPage> = runCatching {
        val q = mutableListOf("offset=$offset", "limit=$limit")
        current?.let { q.add("current=$it") }
        val url = "${apiV1("addresses/history")}?${q.joinToString("&")}"
        val obj = JSONObject(executeGet(url).trim())
        AddressHistoryPage(
            items = parseAddressList(obj),
            offset = obj.optInt("offset", offset),
            limit = obj.optInt("limit", limit),
        )
    }

    // --- Brands ---

    fun getBrands(
        q: String? = null,
        offset: Int = 0,
        limit: Int = 20,
    ): Result<BrandsPage> = runCatching {
        val params = mutableListOf("offset=$offset", "limit=$limit")
        q?.takeIf { it.isNotBlank() }?.let { params.add("q=${enc(it)}") }
        val url = "${apiV1("brands")}?${params.joinToString("&")}"
        parseBrandsPage(JSONObject(executeGet(url).trim()))
    }

    // --- Categories ---

    fun getCategoryTree(): Result<List<CategoryTreeNode>> = runCatching {
        val body = executeGet(apiV1("categories/tree"))
        val root = JSONObject(body.trim())
        val arr = root.optJSONArray("categories") ?: JSONArray()
        (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let { parseCategoryTreeNode(it) }
        }
    }

    fun getCategories(
        q: String? = null,
        offset: Int = 0,
        limit: Int = 20,
    ): Result<CategoriesPage> = runCatching {
        val params = mutableListOf("offset=$offset", "limit=$limit")
        q?.takeIf { it.isNotBlank() }?.let { params.add("q=${enc(it)}") }
        val url = "${apiV1("categories")}?${params.joinToString("&")}"
        parseCategoriesPage(JSONObject(executeGet(url).trim()))
    }

    fun getCategoryById(id: String): Result<CommonCategoryDto> = runCatching {
        val body = executeGet(apiV1("categories/${id.trim()}"))
        val obj = JSONObject(body.trim())
        val c = obj.optJSONObject("category") ?: obj
        parseCategoryDto(c)
    }

    /**
     * Photo wizard steps for a leaf category (`GET .../categories/{id}/listing-image-setup`).
     * Uses the first template with non-empty [steps]; otherwise [defaultListingImageCatalogSteps].
     */
    fun getListingImageSetup(categoryId: String): Result<ListingImageSetupDto> = runCatching {
        val body = executeGet(apiV1("categories/${categoryId.trim()}/listing-image-setup"))
        parseListingImageSetup(JSONObject(body.trim()))
    }

    // --- Aesthetic tags ---

    /** Full catalog when [all] is true (ignores pagination). */
    fun getAestheticTags(
        all: Boolean = false,
        q: String? = null,
        status: String? = null,
        offset: Int = 0,
        limit: Int = 20,
    ): Result<List<CommonAestheticTagDto>> = runCatching {
        val params = mutableListOf<String>()
        if (all) {
            params.add("all=true")
        } else {
            params.add("offset=$offset")
            params.add("limit=$limit")
            q?.takeIf { it.isNotBlank() }?.let { params.add("q=${enc(it)}") }
            status?.takeIf { it.isNotBlank() }?.let { params.add("status=${enc(it)}") }
        }
        val url = "${apiV1("aesthetic-tags")}?${params.joinToString("&")}"
        val body = executeGet(url).trim()
        val obj = JSONObject(body)
        if (all) {
            val arr = obj.optJSONArray("tags") ?: JSONArray()
            (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let { parseAestheticTag(it) } }
        } else {
            parseAestheticTagsPage(obj).items
        }
    }

    /** Paginated listing (when [all] is false). */
    fun getAestheticTagsPage(
        q: String? = null,
        status: String? = null,
        offset: Int = 0,
        limit: Int = 20,
    ): Result<AestheticTagsPage> = runCatching {
        val params = mutableListOf("offset=$offset", "limit=$limit")
        q?.takeIf { it.isNotBlank() }?.let { params.add("q=${enc(it)}") }
        status?.takeIf { it.isNotBlank() }?.let { params.add("status=${enc(it)}") }
        val url = "${apiV1("aesthetic-tags")}?${params.joinToString("&")}"
        parseAestheticTagsPage(JSONObject(executeGet(url).trim()))
    }

    fun getAestheticTagById(id: String): Result<CommonAestheticTagDto> = runCatching {
        val body = executeGet(apiV1("aesthetic-tags/${id.trim()}"))
        val obj = JSONObject(body.trim())
        val t = obj.optJSONObject("tag") ?: obj
        parseAestheticTag(t)
    }

    // --- Countries ---

    /** Full list when [all] is true. */
    fun getCountries(
        all: Boolean = false,
        q: String? = null,
        status: String? = null,
        offset: Int = 0,
        limit: Int = 20,
    ): Result<List<CommonCountryDto>> = runCatching {
        val params = mutableListOf<String>()
        if (all) {
            params.add("all=true")
        } else {
            params.add("offset=$offset")
            params.add("limit=$limit")
            q?.takeIf { it.isNotBlank() }?.let { params.add("q=${enc(it)}") }
            status?.takeIf { it.isNotBlank() }?.let { params.add("status=${enc(it)}") }
        }
        val url = "${apiV1("countries")}?${params.joinToString("&")}"
        val body = executeGet(url).trim()
        val obj = JSONObject(body)
        if (all) {
            val arr = obj.optJSONArray("countries") ?: JSONArray()
            (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let { parseCountry(it) } }
        } else {
            parseCountriesPage(obj).items
        }
    }

    fun getCountriesPage(
        q: String? = null,
        status: String? = null,
        offset: Int = 0,
        limit: Int = 20,
    ): Result<CountriesPage> = runCatching {
        val params = mutableListOf("offset=$offset", "limit=$limit")
        q?.takeIf { it.isNotBlank() }?.let { params.add("q=${enc(it)}") }
        status?.takeIf { it.isNotBlank() }?.let { params.add("status=${enc(it)}") }
        val url = "${apiV1("countries")}?${params.joinToString("&")}"
        parseCountriesPage(JSONObject(executeGet(url).trim()))
    }

    /** `GET /countries/iso/{iso2}` — two-letter ISO alpha-2. */
    fun getCountryByIso(iso2: String): Result<CommonCountryDto> = runCatching {
        val seg = iso2.trim().uppercase()
        val body = executeGet(apiV1("countries/iso/$seg"))
        val obj = JSONObject(body.trim())
        val c = obj.optJSONObject("country") ?: obj
        parseCountry(c)
    }

    fun getCountryById(id: String): Result<CommonCountryDto> = runCatching {
        val body = executeGet(apiV1("countries/${id.trim()}"))
        val obj = JSONObject(body.trim())
        val c = obj.optJSONObject("country") ?: obj
        parseCountry(c)
    }

    // --- Parsers ---

    private fun parseAddressTreeNode(o: JSONObject): AddressTreeNode {
        val children = mutableListOf<AddressTreeNode>()
        val arr = o.optJSONArray("children") ?: JSONArray()
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { children.add(parseAddressTreeNode(it)) }
        }
        return AddressTreeNode(
            id = o.optString("id"),
            name = o.optString("name"),
            code = o.optString("code"),
            parentId = o.optString("parent_id").takeIf { it.isNotBlank() },
            level = o.optInt("level", 1),
            status = o.optString("status"),
            effectiveFrom = o.optString("effective_from").takeIf { it.isNotBlank() },
            effectiveTo = o.optString("effective_to").takeIf { it.isNotBlank() },
            children = children,
        )
    }

    private fun parseAddressDto(o: JSONObject): CommonAddressDto =
        CommonAddressDto(
            id = o.optString("id"),
            name = o.optString("name"),
            code = o.optString("code"),
            parentId = o.optString("parent_id").takeIf { it.isNotBlank() },
            level = o.optInt("level", 1),
            status = o.optString("status"),
            effectiveFrom = o.optString("effective_from").takeIf { it.isNotBlank() },
            effectiveTo = o.optString("effective_to").takeIf { it.isNotBlank() },
        )

    private fun parseAddressList(obj: JSONObject): List<CommonAddressDto> {
        val arr = obj.optJSONArray("items") ?: JSONArray()
        return (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let { parseAddressDto(it) }
        }
    }

    private fun parseBrandsPage(obj: JSONObject): BrandsPage {
        val arr = obj.optJSONArray("items") ?: JSONArray()
        val items = (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let { parseBrand(it) }
        }
        return BrandsPage(
            items = items,
            total = obj.optLong("total", items.size.toLong()),
            offset = obj.optInt("offset", 0),
            limit = obj.optInt("limit", 20),
            hasMore = obj.optBoolean("has_more", false),
        )
    }

    private fun parseBrand(o: JSONObject): CommonBrandDto =
        CommonBrandDto(
            id = o.optString("id"),
            name = o.optString("name"),
            slug = o.optString("slug"),
            country = o.optString("country"),
            logoUrl = o.optString("logo_url"),
            status = o.optString("status"),
            createdAt = o.optString("created_at").takeIf { it.isNotBlank() },
            updatedAt = o.optString("updated_at").takeIf { it.isNotBlank() },
        )

    private fun parseCategoryTreeNode(o: JSONObject): CategoryTreeNode {
        val children = mutableListOf<CategoryTreeNode>()
        val arr = o.optJSONArray("children") ?: JSONArray()
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { children.add(parseCategoryTreeNode(it)) }
        }
        return CategoryTreeNode(
            id = o.optString("id"),
            name = o.optString("name"),
            slug = o.optString("slug"),
            parentId = o.optString("parent_id").takeIf { it.isNotBlank() },
            sortOrder = o.optInt("sort_order", 0),
            status = o.optString("status"),
            createdAt = o.optString("created_at").takeIf { it.isNotBlank() },
            updatedAt = o.optString("updated_at").takeIf { it.isNotBlank() },
            children = children,
        )
    }

    private fun parseCategoryDto(o: JSONObject): CommonCategoryDto =
        CommonCategoryDto(
            id = o.optString("id"),
            name = o.optString("name"),
            slug = o.optString("slug"),
            parentId = o.optString("parent_id").takeIf { it.isNotBlank() },
            sortOrder = o.optInt("sort_order", 0),
            status = o.optString("status"),
            createdAt = o.optString("created_at").takeIf { it.isNotBlank() },
            updatedAt = o.optString("updated_at").takeIf { it.isNotBlank() },
        )

    private fun parseCategoriesPage(obj: JSONObject): CategoriesPage {
        val arr = obj.optJSONArray("items") ?: JSONArray()
        val items = (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let { parseCategoryDto(it) }
        }
        return CategoriesPage(
            items = items,
            total = obj.optLong("total", items.size.toLong()),
            offset = obj.optInt("offset", 0),
            limit = obj.optInt("limit", 20),
            hasMore = obj.optBoolean("has_more", false),
        )
    }

    private fun parseListingImageSetup(root: JSONObject): ListingImageSetupDto {
        val wrapped = root.optJSONObject("listing_image_setup") ?: root
        val categoryId = wrapped.optString("category_id", "")
            .ifBlank { wrapped.optString("categoryId", "") }
        val setups = wrapped.optJSONArray("listing_image_setups") ?: JSONArray()
        val merged = mutableListOf<ListingImageStepCatalog>()
        for (i in 0 until setups.length()) {
            val template = setups.optJSONObject(i) ?: continue
            val stepsArr = template.optJSONArray("steps") ?: JSONArray()
            if (stepsArr.length() == 0) continue
            for (j in 0 until stepsArr.length()) {
                val s = stepsArr.optJSONObject(j) ?: continue
                merged.add(parseListingImageCatalogStep(s))
            }
            break
        }
        val normalized = when {
            merged.isNotEmpty() ->
                merged
                    .filter { it.stepKey.isNotBlank() }
                    .distinctBy { it.stepKey }
                    .sortedBy { it.sortOrder }
                    .take(20)
            else -> defaultListingImageCatalogSteps()
        }
        return ListingImageSetupDto(categoryId = categoryId, steps = normalized)
    }

    private fun parseListingImageCatalogStep(s: JSONObject): ListingImageStepCatalog {
        val stepKey = s.optString("step_key", "")
            .ifBlank { s.optString("stepKey", "") }
            .trim()
        val label = s.optString("label", "").trim()
        val labelVi = s.optString("label_vi", "")
            .ifBlank { s.optString("labelVi", "") }
            .trim()
        val sortOrder = when {
            s.has("sort_order") && !s.isNull("sort_order") -> s.optInt("sort_order", 0)
            s.has("SortOrder") && !s.isNull("SortOrder") -> s.optInt("SortOrder", 0)
            else -> 0
        }
        val required = when {
            s.has("required") && !s.isNull("required") -> s.optBoolean("required", true)
            s.has("Required") && !s.isNull("Required") -> s.optBoolean("Required", true)
            else -> true
        }
        return ListingImageStepCatalog(
            stepKey = stepKey,
            label = label.ifBlank { stepKey },
            labelVi = labelVi,
            sortOrder = sortOrder,
            required = required,
        )
    }

    private fun parseAestheticTag(o: JSONObject): CommonAestheticTagDto =
        CommonAestheticTagDto(
            id = o.optString("id"),
            name = o.optString("name"),
            displayName = o.optString("display_name").ifBlank { o.optString("displayName") },
            sortOrder = o.optInt("sort_order", 0),
            status = o.optString("status"),
            createdAt = o.optString("created_at").takeIf { it.isNotBlank() },
            updatedAt = o.optString("updated_at").takeIf { it.isNotBlank() },
        )

    private fun parseAestheticTagsPage(obj: JSONObject): AestheticTagsPage {
        val arr = obj.optJSONArray("items") ?: JSONArray()
        val items = (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let { parseAestheticTag(it) }
        }
        return AestheticTagsPage(
            items = items,
            total = obj.optLong("total", items.size.toLong()),
            offset = obj.optInt("offset", 0),
            limit = obj.optInt("limit", 20),
            hasMore = obj.optBoolean("has_more", false),
        )
    }

    private fun parseCountry(o: JSONObject): CommonCountryDto =
        CommonCountryDto(
            id = o.optString("id"),
            iso2 = o.optString("iso2"),
            iso3 = o.optString("iso3"),
            name = o.optString("name"),
            numericCode = when {
                !o.has("numeric_code") || o.isNull("numeric_code") -> null
                else -> o.optInt("numeric_code")
            },
            phonePrefix = o.optString("phone_prefix"),
            emoji = o.optString("emoji"),
            sortOrder = o.optInt("sort_order", 0),
            status = o.optString("status"),
            createdAt = o.optString("created_at").takeIf { it.isNotBlank() },
            updatedAt = o.optString("updated_at").takeIf { it.isNotBlank() },
        )

    private fun parseCountriesPage(obj: JSONObject): CountriesPage {
        val arr = obj.optJSONArray("items") ?: JSONArray()
        val items = (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let { parseCountry(it) }
        }
        return CountriesPage(
            items = items,
            total = obj.optLong("total", items.size.toLong()),
            offset = obj.optInt("offset", 0),
            limit = obj.optInt("limit", 20),
            hasMore = obj.optBoolean("has_more", false),
        )
    }
}

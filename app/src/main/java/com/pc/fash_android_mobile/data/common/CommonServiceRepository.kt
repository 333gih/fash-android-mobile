package com.pc.fash_android_mobile.data.common

import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.http.CoreServiceErrors
import com.pc.fash_android_mobile.data.http.CoreServiceHttpException
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
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
 * Explore filter catalog (categories tree, brands, aesthetic-tags, countries) uses
 * [PublicCommonCatalogRepository] — same pattern as [com.pc.fash_android_mobile.data.editorial.EditorialGuideRepository].
 *
 * Other reads/writes use [securedClient] with user JWT (or internal service token when logged out).
 *
 * Query strings for `addresses` / `countries` mirror **fash-admin-portal-fe** → common-service
 * (`URLSearchParams` / [okhttp3.HttpUrl.addQueryParameter]): `level`, `parent_id`, `current`, `all`, etc.
 *
 * [getHealth] uses a separate client so the health check sends only Accept and User-Agent.
 *
 * @see com.pc.fash_android_mobile.network.SecuredApiClient
 */
class CommonServiceRepository(
    private val securedClient: OkHttpClient,
    private val publicCatalogRepository: PublicCommonCatalogRepository,
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

    private fun apiV1(path: String): String {
        val rel = path.trimStart('/')
        return AppEnvironment.commonServicePath("api/v1/$rel")
    }

    /** Encodes query the same way as the admin portal BFF / browser (RFC 3986 via OkHttp). */
    private fun apiV1UrlWithQuery(
        pathAfterApiV1: String,
        queryParams: List<Pair<String, String>>,
    ): String {
        val base = apiV1(pathAfterApiV1)
        val resolved = base.toHttpUrlOrNull() ?: error("Invalid common-service catalog URL: $base")
        val b = resolved.newBuilder()
        queryParams.forEach { (k, v) -> b.addQueryParameter(k, v) }
        return b.build().toString()
    }

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
        val params = buildList {
            add("current" to if (current) "true" else "false")
            level?.let { add("level" to it.toString()) }
            parentId?.trim()?.takeIf { it.isNotEmpty() }?.let { add("parent_id" to it) }
        }
        val url = apiV1UrlWithQuery("addresses", params)
        parseAddressList(JSONObject(executeGet(url).trim()))
    }

    /**
     * Level-1 provinces: `GET .../addresses?level=1&current=true` (same as admin portal list tab);
     * if empty or HTTP error, falls back to `GET .../addresses/tree` and collects `level == 1` nodes
     * (portal tree tab).
     */
    fun getProvincesCatalog(): Result<List<CommonAddressDto>> = runCatching {
        val listAttempt = kotlin.runCatching { getAddresses(level = 1, current = true).getOrThrow() }
        if (listAttempt.isSuccess && listAttempt.getOrThrow().isNotEmpty()) {
            return@runCatching listAttempt.getOrThrow()
        }
        flattenAddressesAtLevel(getAddressTree().getOrThrow(), 1)
    }

    private fun flattenAddressesAtLevel(nodes: List<AddressTreeNode>, targetLevel: Int): List<CommonAddressDto> {
        val out = mutableListOf<CommonAddressDto>()
        fun walk(list: List<AddressTreeNode>) {
            for (n in list) {
                if (n.level == targetLevel) {
                    out.add(
                        CommonAddressDto(
                            id = n.id,
                            name = n.name,
                            code = n.code,
                            parentId = n.parentId,
                            level = n.level,
                            status = n.status,
                            effectiveFrom = n.effectiveFrom,
                            effectiveTo = n.effectiveTo,
                        ),
                    )
                }
                if (n.children.isNotEmpty()) walk(n.children)
            }
        }
        walk(nodes)
        return out.sortedBy { it.name }
    }

    private fun findTreeNodeById(nodes: List<AddressTreeNode>, id: String): AddressTreeNode? {
        val target = id.trim()
        if (target.isEmpty()) return null
        for (n in nodes) {
            if (n.id.equals(target, ignoreCase = true)) return n
            findTreeNodeById(n.children, target)?.let { return it }
        }
        return null
    }

    private fun addressTreeNodeToDto(n: AddressTreeNode): CommonAddressDto =
        CommonAddressDto(
            id = n.id,
            name = n.name,
            code = n.code,
            parentId = n.parentId,
            level = n.level,
            status = n.status,
            effectiveFrom = n.effectiveFrom,
            effectiveTo = n.effectiveTo,
        )

    /**
     * Direct children of [parentId] at [childLevel] (2 = district, 3 = ward).
     * Matches admin portal `GET .../addresses?level=&parent_id=&current=true`; if that returns empty
     * or throws, uses the same parent's **children** from `GET .../addresses/tree` (portal tree tab).
     */
    fun getAdministrativeChildren(parentId: String, childLevel: Int): Result<List<CommonAddressDto>> = runCatching {
        val pid = parentId.trim().ifEmpty { return@runCatching emptyList() }
        require(childLevel in 2..3) { "childLevel must be 2 or 3" }

        val listTry = kotlin.runCatching {
            getAddresses(level = childLevel, parentId = pid, current = true).getOrThrow()
        }
        listTry.getOrNull()?.takeIf { it.isNotEmpty() }?.let { return@runCatching it }

        val tree = kotlin.runCatching { getAddressTree().getOrThrow() }.getOrElse { ex ->
            throw listTry.exceptionOrNull() ?: ex
        }
        val node = findTreeNodeById(tree, pid)
        val fromTree = node?.children.orEmpty()
            .filter { it.level == childLevel }
            .map { addressTreeNodeToDto(it) }
            .sortedBy { it.name }

        val looseList = kotlin.runCatching {
            getAddresses(level = null, parentId = pid, current = true).getOrThrow()
                .filter { it.level == childLevel }
                .sortedBy { it.name }
        }.getOrNull()?.takeIf { it.isNotEmpty() }

        when {
            fromTree.isNotEmpty() -> fromTree
            looseList != null -> looseList
            listTry.isSuccess -> listTry.getOrThrow()
            else -> throw listTry.exceptionOrNull()!!
        }
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
        @Suppress("UNUSED_PARAMETER") publicBrowse: Boolean = true,
    ): Result<BrandsPage> = publicCatalogRepository.getBrands(q = q, offset = offset, limit = limit)

    // --- Categories ---

    fun getCategoryTree(
        @Suppress("UNUSED_PARAMETER") publicBrowse: Boolean = true,
    ): Result<List<CategoryTreeNode>> = publicCatalogRepository.getCategoryTree()

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
        @Suppress("UNUSED_PARAMETER") publicBrowse: Boolean = true,
    ): Result<List<CommonAestheticTagDto>> =
        publicCatalogRepository.getAestheticTags(all = all, q = q, status = status, offset = offset, limit = limit)

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
        @Suppress("UNUSED_PARAMETER") publicBrowse: Boolean = true,
    ): Result<List<CommonCountryDto>> =
        publicCatalogRepository.getCountries(all = all, q = q, status = status, offset = offset, limit = limit)

    fun getCountriesPage(
        q: String? = null,
        status: String? = null,
        offset: Int = 0,
        limit: Int = 20,
    ): Result<CountriesPage> = runCatching {
        val params = buildList<Pair<String, String>> {
            add("offset" to offset.toString())
            add("limit" to limit.toString())
            q?.trim()?.takeIf { it.isNotEmpty() }?.let { add("q" to it) }
            status?.trim()?.takeIf { it.isNotEmpty() }?.let { add("status" to it) }
        }
        val url = apiV1UrlWithQuery("countries", params)
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
            id = o.optString("id").ifBlank { o.optString("ID") },
            name = o.optString("name").ifBlank { o.optString("Name") },
            code = o.optString("code").ifBlank { o.optString("Code") },
            parentId = o.optString("parent_id").ifBlank { o.optString("ParentID") }.takeIf { it.isNotBlank() },
            level = when {
                o.has("level") && !o.isNull("level") -> o.optInt("level", 1)
                o.has("Level") && !o.isNull("Level") -> o.optInt("Level", 1)
                else -> 1
            },
            status = o.optString("status").ifBlank { o.optString("Status") },
            effectiveFrom = o.optString("effective_from").ifBlank { o.optString("EffectiveFrom") }.takeIf { it.isNotBlank() },
            effectiveTo = o.optString("effective_to").ifBlank { o.optString("EffectiveTo") }.takeIf { it.isNotBlank() },
            children = children,
        )
    }

    private fun parseAddressDto(o: JSONObject): CommonAddressDto =
        CommonAddressDto(
            id = o.optString("id").ifBlank { o.optString("ID") },
            name = o.optString("name").ifBlank { o.optString("Name") },
            code = o.optString("code").ifBlank { o.optString("Code") },
            parentId = o.optString("parent_id").ifBlank { o.optString("ParentID") }.takeIf { it.isNotBlank() },
            level = when {
                o.has("level") && !o.isNull("level") -> o.optInt("level", 1)
                o.has("Level") && !o.isNull("Level") -> o.optInt("Level", 1)
                else -> 1
            },
            status = o.optString("status").ifBlank { o.optString("Status") },
            effectiveFrom = o.optString("effective_from").ifBlank { o.optString("EffectiveFrom") }.takeIf { it.isNotBlank() },
            effectiveTo = o.optString("effective_to").ifBlank { o.optString("EffectiveTo") }.takeIf { it.isNotBlank() },
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
            displayNameVi = o.optString("display_name_vi", o.optString("displayNameVi", "")),
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

package com.pc.fash_android_mobile.data.address

import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.http.CoreServiceErrors
import com.pc.fash_android_mobile.data.http.CoreServiceHttpException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Core-service user shipping addresses ([android-shipping-addresses-api.md]).
 * Paths: `GET/POST …/users/me/shipping-addresses`, `PATCH …/{id}/default`.
 */
class UserShippingAddressRepository(
    private val securedClient: OkHttpClient,
) {

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private fun throwHttp(httpCode: Int, body: String): Nothing =
        throw CoreServiceHttpException(httpCode, CoreServiceErrors.parseErrorMessage(httpCode, body))

    private fun baseListUrl(): String =
        AppEnvironment.apiPath("api/v1/users/me/shipping-addresses")

    /** `GET /users/me/shipping-addresses` — JSON array of address objects. */
    fun listShippingAddresses(): Result<List<ShippingAddress>> = runCatching {
        val body = executeGet(baseListUrl())
        parseAddressListBody(body)
    }

    /** `POST /users/me/shipping-addresses` — [CreateShippingAddressRequest]. */
    fun createShippingAddress(request: CreateUserShippingAddressRequest): Result<ShippingAddress> =
        runCatching {
            val json = request.toJson()
            val responseBody = executePostJson(baseListUrl(), json.toString())
            val o = unwrapDataObject(JSONObject(responseBody.trim()))
            parseShippingAddress(o)
        }

    /** `PATCH /users/me/shipping-addresses/{id}/default` */
    fun setDefaultShippingAddress(addressId: String): Result<Unit> = runCatching {
        val url = AppEnvironment.apiPath(
            "api/v1/users/me/shipping-addresses/${addressId.trim()}/default",
        )
        executePatchEmpty(url)
    }

    private fun executeGet(url: String): String {
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/json")
            .header("User-Agent", "FashAndroid/1.0")
            .build()
        return securedClient.newCall(request).execute().use { response ->
            val b = response.body?.string().orEmpty()
            if (!response.isSuccessful) throwHttp(response.code, b)
            b
        }
    }

    private fun executePostJson(url: String, json: String): String {
        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody(jsonMedia))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("User-Agent", "FashAndroid/1.0")
            .build()
        return securedClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throwHttp(response.code, body)
            body.ifBlank { "{}" }
        }
    }

    private fun executePatchEmpty(url: String) {
        val request = Request.Builder()
            .url(url)
            .patch("{}".toRequestBody(jsonMedia))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("User-Agent", "FashAndroid/1.0")
            .build()
        securedClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throwHttp(response.code, body)
        }
    }

    private fun parseAddressListBody(raw: String): List<ShippingAddress> {
        val trimmed = raw.trim()
        val arr: JSONArray = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            else -> {
                val o = JSONObject(trimmed)
                when {
                    o.has("data") && o.opt("data") is JSONArray -> o.getJSONArray("data")
                    o.has("addresses") -> o.optJSONArray("addresses") ?: JSONArray()
                    else -> JSONArray()
                }
            }
        }
        return (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let { parseShippingAddress(it) }
        }
    }

    private fun unwrapDataObject(o: JSONObject): JSONObject {
        if (o.has("data")) {
            val d = o.opt("data")
            if (d is JSONObject) return d
        }
        return o
    }
}

data class CreateUserShippingAddressRequest(
    val line1: String,
    val countryCode: String,
    val label: String = "",
    val recipientName: String = "",
    val line2: String = "",
    val city: String = "",
    val region: String = "",
    val postalCode: String = "",
    val isDefault: Boolean = false,
    val provinceId: String? = null,
    val provinceName: String = "",
    val districtId: String? = null,
    val districtName: String = "",
    val wardId: String? = null,
    val wardName: String = "",
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("line1", line1)
        put("country_code", countryCode)
        if (label.isNotBlank()) put("label", label)
        if (recipientName.isNotBlank()) put("recipient_name", recipientName)
        if (line2.isNotBlank()) put("line2", line2)
        if (city.isNotBlank()) put("city", city)
        if (region.isNotBlank()) put("region", region)
        if (postalCode.isNotBlank()) put("postal_code", postalCode)
        put("is_default", isDefault)
        provinceId?.takeIf { it.isNotBlank() }?.let { put("province_id", it) }
        if (provinceName.isNotBlank()) put("province_name", provinceName)
        districtId?.takeIf { it.isNotBlank() }?.let { put("district_id", it) }
        if (districtName.isNotBlank()) put("district_name", districtName)
        wardId?.takeIf { it.isNotBlank() }?.let { put("ward_id", it) }
        if (wardName.isNotBlank()) put("ward_name", wardName)
    }
}

internal fun parseShippingAddress(o: JSONObject): ShippingAddress {
    fun s(vararg keys: String): String {
        for (k in keys) {
            val v = o.optString(k, "")
            if (v.isNotBlank()) return v
        }
        return ""
    }
    val provinceName = s("province_name", "provinceName")
    val districtName = s("district_name", "districtName")
    val wardName = s("ward_name", "wardName")
    val city = s("city").ifBlank { provinceName }
    val district = districtName.ifBlank { s("district") }
    val ward = wardName.ifBlank { s("ward") }
    return ShippingAddress(
        id = s("id", "ID").ifBlank { UUID.randomUUID().toString() },
        recipientName = s("recipient_name", "recipientName"),
        phone = s("phone"),
        city = city,
        district = district,
        ward = ward,
        line1 = s("line1", "line_1", "address_line1", "address_line_1", "street", "street_address"),
        isDefault = o.optBoolean("is_default", o.optBoolean("isDefault", false)),
        label = s("label"),
        line2 = s("line2", "line_2"),
        region = s("region"),
        postalCode = s("postal_code", "postalCode"),
        countryCode = s("country_code", "countryCode").ifBlank { "VN" },
        provinceId = s("province_id", "provinceId").takeIf { it.isNotBlank() },
        districtId = s("district_id", "districtId").takeIf { it.isNotBlank() },
        wardId = s("ward_id", "wardId").takeIf { it.isNotBlank() },
    )
}

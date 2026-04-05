package com.pc.fash_android_mobile.data.address

import org.json.JSONObject
import java.util.UUID

/**
 * Parses core-service shipping address JSON (snake_case, camelCase, or PascalCase keys).
 */
internal fun parseShippingAddressJson(o: JSONObject): ShippingAddress {
    fun s(vararg keys: String): String {
        for (k in keys) {
            if (!o.has(k)) continue
            val v = o.optString(k, "")
            if (v.isNotBlank()) return v
        }
        return ""
    }
    val provinceName = s("province_name", "ProvinceName", "provinceName")
    val districtName = s("district_name", "DistrictName", "districtName")
    val wardName = s("ward_name", "WardName", "wardName")
    val cityRaw = s("city", "City").ifBlank { provinceName }
    val districtRaw = districtName.ifBlank { s("district", "District") }
    val wardRaw = wardName.ifBlank { s("ward", "Ward") }
    val isDefault = when {
        o.has("IsDefault") -> o.optBoolean("IsDefault", false)
        o.has("is_default") -> o.optBoolean("is_default", false)
        else -> o.optBoolean("isDefault", false)
    }
    return ShippingAddress(
        id = s("id", "ID", "Uuid", "uuid").ifBlank { UUID.randomUUID().toString() },
        recipientName = s("recipient_name", "RecipientName", "recipientName"),
        phone = s("phone", "Phone", "mobile", "Mobile", "recipient_phone", "RecipientPhone"),
        city = cityRaw,
        district = districtRaw,
        ward = wardRaw,
        line1 = s("line1", "Line1", "line_1", "address_line1", "AddressLine1", "street", "Street"),
        isDefault = isDefault,
        label = s("label", "Label"),
        line2 = s("line2", "Line2", "line_2"),
        region = s("region", "Region"),
        postalCode = s("postal_code", "PostalCode", "postalCode"),
        countryCode = s("country_code", "CountryCode", "country").ifBlank { "VN" },
        provinceId = s("province_id", "ProvinceID", "provinceId").takeIf { it.isNotBlank() },
        districtId = s("district_id", "DistrictID", "districtId").takeIf { it.isNotBlank() },
        wardId = s("ward_id", "WardID", "wardId").takeIf { it.isNotBlank() },
    )
}

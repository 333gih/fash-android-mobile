package com.pc.fash_android_mobile.data.address

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Persists shipping addresses and per-order selections on device (per logged-in user).
 * Replace or extend with core `GET/POST /users/me/addresses` when available.
 */
class AddressLocalStore(
    application: Application,
) {
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun listAddresses(userId: String): List<ShippingAddress> {
        val raw = prefs.getString(keyAddresses(userId), null) ?: return emptyList()
        return parseAddressArray(raw)
    }

    fun saveAddresses(userId: String, list: List<ShippingAddress>) {
        prefs.edit { putString(keyAddresses(userId), addressListToJson(list)) }
    }

    fun upsertAddress(userId: String, address: ShippingAddress): List<ShippingAddress> {
        val current = listAddresses(userId).toMutableList()
        val idx = current.indexOfFirst { it.id == address.id }
        if (idx >= 0) current[idx] = address else current.add(address)
        val normalized = if (address.isDefault) {
            current.map { a -> if (a.id != address.id) a.copy(isDefault = false) else a }
        } else {
            current
        }
        saveAddresses(userId, normalized)
        return normalized
    }

    fun deleteAddress(userId: String, addressId: String): List<ShippingAddress> {
        val current = listAddresses(userId).filter { it.id != addressId }
        saveAddresses(userId, current)
        return current
    }

    fun setDefault(userId: String, addressId: String): List<ShippingAddress> {
        val current = listAddresses(userId).map {
            it.copy(isDefault = it.id == addressId)
        }
        saveAddresses(userId, current)
        return current
    }

    fun getDefaultOrFirst(userId: String): ShippingAddress? {
        val list = listAddresses(userId)
        return list.firstOrNull { it.isDefault } ?: list.firstOrNull()
    }

    fun getOrderAddressId(userId: String, orderId: String): String? {
        val map = loadOrderMap(userId)
        return map[orderId.trim().lowercase()]
    }

    fun setOrderAddressId(userId: String, orderId: String, addressId: String) {
        val map = loadOrderMap(userId).toMutableMap()
        map[orderId.trim().lowercase()] = addressId
        saveOrderMap(userId, map)
    }

    fun clearOrderSelection(userId: String, orderId: String) {
        val map = loadOrderMap(userId).toMutableMap()
        map.remove(orderId.trim().lowercase())
        saveOrderMap(userId, map)
    }

    private fun loadOrderMap(userId: String): Map<String, String> {
        val raw = prefs.getString(keyOrderMap(userId), null) ?: return emptyMap()
        return try {
            val o = JSONObject(raw)
            val out = mutableMapOf<String, String>()
            val keys = o.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                out[k] = o.optString(k, "")
            }
            out.filterValues { it.isNotBlank() }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun saveOrderMap(userId: String, map: Map<String, String>) {
        val o = JSONObject()
        map.forEach { (k, v) -> o.put(k, v) }
        prefs.edit { putString(keyOrderMap(userId), o.toString()) }
    }

    private fun parseAddressArray(raw: String): List<ShippingAddress> {
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                ShippingAddress(
                    id = o.optString("id", "").ifBlank { UUID.randomUUID().toString() },
                    recipientName = o.optString("recipient_name", o.optString("recipientName", "")),
                    phone = o.optString("phone", ""),
                    city = o.optString("city", ""),
                    district = o.optString("district", ""),
                    ward = o.optString("ward", ""),
                    line1 = o.optString("line1", o.optString("line_1", "")),
                    isDefault = o.optBoolean("is_default", o.optBoolean("isDefault", false)),
                    label = o.optString("label", ""),
                    line2 = o.optString("line2", ""),
                    region = o.optString("region", ""),
                    postalCode = o.optString("postal_code", o.optString("postalCode", "")),
                    countryCode = o.optString("country_code", o.optString("countryCode", "VN")).ifBlank { "VN" },
                    provinceId = o.optString("province_id", o.optString("provinceId", "")).takeIf { it.isNotBlank() },
                    districtId = o.optString("district_id", o.optString("districtId", "")).takeIf { it.isNotBlank() },
                    wardId = o.optString("ward_id", o.optString("wardId", "")).takeIf { it.isNotBlank() },
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun addressListToJson(list: List<ShippingAddress>): String {
        val arr = JSONArray()
        list.forEach { a ->
            arr.put(
                JSONObject()
                    .put("id", a.id)
                    .put("recipient_name", a.recipientName)
                    .put("phone", a.phone)
                    .put("city", a.city)
                    .put("district", a.district)
                    .put("ward", a.ward)
                    .put("line1", a.line1)
                    .put("is_default", a.isDefault)
                    .put("label", a.label)
                    .put("line2", a.line2)
                    .put("region", a.region)
                    .put("postal_code", a.postalCode)
                    .put("country_code", a.countryCode)
                    .put("province_id", a.provinceId ?: "")
                    .put("district_id", a.districtId ?: "")
                    .put("ward_id", a.wardId ?: ""),
            )
        }
        return arr.toString()
    }

    private companion object {
        const val PREFS_NAME = "fash_shipping_addresses"
        fun keyAddresses(userId: String) = "addr_list_${userId.trim()}"
        fun keyOrderMap(userId: String) = "addr_order_map_${userId.trim()}"
    }
}

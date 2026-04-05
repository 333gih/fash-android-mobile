package com.pc.fash_android_mobile.data.address

import com.pc.fash_android_mobile.data.payment.CheckoutAddress

/**
 * Saved shipping address (local book; can be synced with core later via [AddressLocalStore]).
 */
data class ShippingAddress(
    val id: String,
    val recipientName: String,
    val phone: String,
    val city: String,
    val district: String,
    val ward: String,
    val line1: String,
    val isDefault: Boolean,
    val label: String = "",
    val line2: String = "",
    val region: String = "",
    val postalCode: String = "",
    val countryCode: String = "VN",
    val provinceId: String? = null,
    val districtId: String? = null,
    val wardId: String? = null,
) {
    /**
     * Street + ward/district/city only (no [label]), with consecutive duplicates removed
     * (e.g. same province repeated as city and district).
     */
    fun formattedAddressLine(): String {
        return dedupeConsecutiveLocality(
            buildList {
                add(line1)
                if (line2.isNotBlank()) add(line2)
                add(ward)
                add(district)
                add(city)
            }.filter { it.isNotBlank() },
        ).joinToString(", ")
    }

    /** Full one-line summary including label when present. */
    fun formattedSingleLine(): String {
        return dedupeConsecutiveLocality(
            buildList {
                if (label.isNotBlank()) add(label)
                add(line1)
                if (line2.isNotBlank()) add(line2)
                add(ward)
                add(district)
                add(city)
            }.filter { it.isNotBlank() },
        ).joinToString(", ")
    }

    fun toCheckoutAddress(): CheckoutAddress {
        val addrLine = formattedAddressLine().ifBlank { formattedSingleLine() }
        return CheckoutAddress(
            fullName = recipientName.trim(),
            phone = phone.trim(),
            address = addrLine.ifBlank { "—" },
            district = district.trim(),
            city = city.trim(),
        )
    }

    /**
     * Single line for review step / draft label when name may be empty.
     */
    fun labelForDraft(): String {
        val nameOrLabel = recipientName.trim().ifBlank { label.trim() }
        val detail = listOf(line1, ward, district, city).filter { it.isNotBlank() }.joinToString(", ")
            .ifBlank { formattedSingleLine() }
        return when {
            nameOrLabel.isNotBlank() && detail.isNotBlank() -> "$nameOrLabel · $detail"
            nameOrLabel.isNotBlank() -> nameOrLabel
            detail.isNotBlank() -> detail
            else -> formattedSingleLine()
        }
    }

    private companion object {
        fun dedupeConsecutiveLocality(parts: List<String>): List<String> {
            val out = mutableListOf<String>()
            for (p in parts) {
                val t = p.trim()
                if (t.isBlank()) continue
                if (out.lastOrNull()?.equals(t, ignoreCase = true) == true) continue
                out.add(t)
            }
            return out
        }
    }
}

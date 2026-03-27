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
) {
    fun formattedSingleLine(): String =
        listOf(line1, ward, district, city).filter { it.isNotBlank() }.joinToString(", ")

    fun toCheckoutAddress(): CheckoutAddress {
        val addrLine = listOf(line1, ward).filter { it.isNotBlank() }.joinToString(", ")
        return CheckoutAddress(
            fullName = recipientName.trim(),
            phone = phone.trim(),
            address = addrLine.ifBlank { "—" },
            district = district.trim(),
            city = city.trim(),
        )
    }
}

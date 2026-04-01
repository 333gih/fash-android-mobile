package com.pc.fash_android_mobile.data.address

/**
 * Merges API list with local rows: overlays [ShippingAddress.phone] from local for matching ids,
 * and appends local addresses missing from the API response (eventual consistency after create).
 */
fun mergeShippingAddressesWithLocal(
    api: List<ShippingAddress>,
    local: List<ShippingAddress>,
): List<ShippingAddress> {
    val apiIds = api.map { it.id }.toSet()
    val localById = local.associateBy { it.id }
    val mergedFromApi = api.map { a ->
        val l = localById[a.id]
        if (l != null) {
            a.copy(phone = l.phone.takeIf { it.isNotBlank() } ?: a.phone)
        } else {
            a
        }
    }
    val localOnly = local.filter { it.id !in apiIds }
    return mergedFromApi + localOnly
}

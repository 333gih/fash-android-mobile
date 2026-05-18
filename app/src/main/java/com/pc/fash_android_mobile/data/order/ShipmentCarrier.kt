package com.pc.fash_android_mobile.data.order

/** Row from `GET /shipment/carriers`. */
data class ShipmentCarrier(
    val id: String,
    val name: String,
    val description: String = "",
)

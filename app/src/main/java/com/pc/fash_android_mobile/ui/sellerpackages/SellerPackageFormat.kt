package com.pc.fash_android_mobile.ui.sellerpackages

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

fun formatVnd(amount: Long): String {
    val symbols = DecimalFormatSymbols(Locale("vi", "VN")).apply {
        groupingSeparator = '.'
    }
    return DecimalFormat("#,###", symbols).format(amount.coerceAtLeast(0)) + "₫"
}

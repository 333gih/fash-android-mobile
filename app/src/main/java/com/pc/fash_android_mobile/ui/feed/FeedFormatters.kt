package com.pc.fash_android_mobile.ui.feed

/** VND display aligned with INTEGRATION.md / app convention (đ + dot thousands). */
fun formatListingPriceVnd(vnd: Long): String =
    "đ ${"%,d".format(vnd).replace(',', '.')}"

fun formatFollowerCountShort(count: Int): String =
    when {
        count >= 1_000_000 -> "${count / 1_000_000}.${(count % 1_000_000) / 100_000}M"
        count >= 1_000 -> "${count / 1_000}.${(count % 1_000) / 100}k"
        else -> count.toString()
    }

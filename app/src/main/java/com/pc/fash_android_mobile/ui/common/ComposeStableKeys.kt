package com.pc.fash_android_mobile.ui.common

/**
 * Lazy list item keys must be unique. APIs sometimes return blank ids;
 * using those as keys yields "Key \"\" was already used" crashes. Prefer a stable id, else index.
 */
fun stableLazyKey(primary: String?, index: Int, prefix: String = "row"): Any =
    primary?.takeIf { it.isNotBlank() } ?: "${prefix}_$index"

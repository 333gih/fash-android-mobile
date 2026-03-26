package com.pc.fash_android_mobile.ui.common

/**
 * Lazy list item keys must be unique. APIs sometimes return blank ids or duplicate ids;
 * we always include [index] so blank keys and duplicate ids cannot collide.
 */
fun stableLazyKey(primary: String?, index: Int, prefix: String = "row"): Any =
    if (primary.isNullOrBlank()) "${prefix}_$index" else "${prefix}_${primary}_$index"

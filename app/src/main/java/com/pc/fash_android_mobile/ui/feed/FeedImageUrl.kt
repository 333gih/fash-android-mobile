package com.pc.fash_android_mobile.ui.feed

import com.pc.fash_android_mobile.config.AppEnvironment

fun resolveListingImageUrl(path: String): String {
    if (path.startsWith("http")) return path
    val base = AppEnvironment.apiBaseUrl.trimEnd('/')
    return if (path.startsWith("/")) "$base$path" else "$base/$path"
}

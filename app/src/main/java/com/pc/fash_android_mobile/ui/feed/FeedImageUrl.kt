package com.pc.fash_android_mobile.ui.feed

import com.pc.fash_android_mobile.config.AppEnvironment

fun resolveListingImageUrl(path: String): String {
    if (path.startsWith("http")) return path
    val base = AppEnvironment.apiBaseUrl.trimEnd('/')
    return if (path.startsWith("/")) "$base$path" else "$base/$path"
}

/** Profile avatars/covers — same resolution rules as listing media URLs. */
fun resolveProfileImageUrl(path: String): String = resolveListingImageUrl(path)

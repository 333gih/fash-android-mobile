package com.pc.fash_android_mobile.data.common

import android.content.Context
import com.pc.fash_android_mobile.data.locale.AppLocale

/** Locale-aware label: Vietnamese when [isVi] and [displayNameVi] is set, else English [displayName]. */
fun CommonAestheticTagDto.displayLabel(isVi: Boolean): String {
    val vi = displayNameVi.trim()
    val en = displayName.trim().ifBlank { name.trim() }
    return if (isVi && vi.isNotEmpty()) vi else en.ifBlank { vi.ifBlank { name } }
}

fun CommonAestheticTagDto.displayLabel(context: Context): String =
    displayLabel(AppLocale.currentTag(context) != AppLocale.TAG_EN)

/** Resolve a stored snapshot (id + canonical name) against the common-service catalog. */
fun resolveAestheticLabel(
    catalog: List<CommonAestheticTagDto>,
    id: String?,
    rawName: String,
    isVi: Boolean,
): String {
    id?.trim()?.takeIf { it.isNotEmpty() }?.let { tagId ->
        catalog.find { it.id.equals(tagId, ignoreCase = true) }?.let { return it.displayLabel(isVi) }
    }
    val raw = rawName.trim()
    if (raw.isEmpty()) return raw
    catalog.find {
        it.name.equals(raw, ignoreCase = true) ||
            it.displayName.equals(raw, ignoreCase = true) ||
            it.displayNameVi.equals(raw, ignoreCase = true)
    }?.let { return it.displayLabel(isVi) }
    return raw
}

fun resolveAestheticLabel(
    catalog: List<CommonAestheticTagDto>,
    id: String?,
    rawName: String,
    context: Context,
): String = resolveAestheticLabel(catalog, id, rawName, AppLocale.currentTag(context) != AppLocale.TAG_EN)

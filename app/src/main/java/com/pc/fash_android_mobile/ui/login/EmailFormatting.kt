package com.pc.fash_android_mobile.ui.login

/** Hides most of the local part for in-app copy (e.g. OTP subtitle). */
fun maskEmailForDisplay(email: String): String {
    val e = email.trim()
    val at = e.indexOf('@')
    if (at <= 0) return e
    val local = e.substring(0, at)
    val domain = e.substring(at + 1)
    val maskedLocal = when {
        local.isEmpty() -> "•••"
        local.length == 1 -> "${local.first()}••"
        else -> "${local.first()}•••${local.last()}"
    }
    return "$maskedLocal@$domain"
}

package com.pc.fash_android_mobile.deeplink

import android.content.Intent
import android.net.Uri
import com.pc.fash_android_mobile.BuildConfig

/**
 * Invite flow deep links:
 * - In-app: `fash://invite` or `fash://invite?ref=username` (optional `&r=jwt` for referral points)
 * - Shareable (linkified in SMS / most chat apps): `https://{LISTING_SHARE_HOST}/invite?...`
 */
object InviteDeepLinks {

    fun inviteDeepLinkUri(referrerUsername: String?, referralToken: String? = null): Uri {
        val u = referrerUsername?.trim().orEmpty()
        val t = referralToken?.trim().orEmpty()
        val b = Uri.Builder().scheme("fash").authority("invite")
        if (u.isNotEmpty()) {
            b.appendQueryParameter("ref", u)
        }
        if (t.isNotEmpty()) {
            b.appendQueryParameter("r", t)
        }
        return b.build()
    }

    /**
     * Public HTTPS URL on the marketing host (same origin as [BuildConfig.LISTING_SHARE_BASE_URL]).
     * Messengers auto-link `https://` only; custom schemes like `fash://` stay plain text.
     */
    fun publicInviteHttpsUrl(referrerUsername: String?, referralToken: String? = null): String {
        val raw = BuildConfig.LISTING_SHARE_BASE_URL.trim()
        val parsed = try {
            Uri.parse(if (raw.contains("://", ignoreCase = true)) raw else "https://$raw")
        } catch (_: Exception) {
            Uri.parse("https://fash.app/p/l")
        }
        val scheme = (parsed.scheme ?: "https").trim()
        val host = parsed.host?.trim()?.ifEmpty { null } ?: "fash.app"
        val origin = "$scheme://$host"
        val b = Uri.parse("$origin/invite").buildUpon()
        val r = referrerUsername?.trim().orEmpty()
        if (r.isNotEmpty()) {
            b.appendQueryParameter("ref", r)
        }
        val tok = referralToken?.trim().orEmpty()
        if (tok.isNotEmpty()) {
            b.appendQueryParameter("r", tok)
        }
        return b.build().toString()
    }

    private fun listingSharePublicHost(): String? =
        try {
            val raw = BuildConfig.LISTING_SHARE_BASE_URL.trim()
            val withScheme = if (raw.contains("://", ignoreCase = true)) raw else "https://$raw"
            Uri.parse(withScheme).host?.trim()?.lowercase()
        } catch (_: Exception) {
            null
        }

    private fun isHttpsInviteUri(uri: Uri): Boolean {
        if (!uri.scheme.equals("https", ignoreCase = true) &&
            !uri.scheme.equals("http", ignoreCase = true)
        ) {
            return false
        }
        val host = uri.host?.trim()?.lowercase() ?: return false
        val expected = listingSharePublicHost() ?: return false
        if (host != expected) return false
        val first = uri.pathSegments.firstOrNull() ?: return false
        return first.equals("invite", ignoreCase = true)
    }

    private fun isFashInviteUri(uri: Uri): Boolean =
        uri.scheme.equals("fash", ignoreCase = true) &&
            uri.host.equals("invite", ignoreCase = true)

    /** True when this VIEW intent should open the in-app invite screen (consumed by MainActivity). */
    fun parseInviteOpenFromIntent(intent: Intent?): Boolean {
        val data = intent?.data ?: return false
        return isFashInviteUri(data) || isHttpsInviteUri(data)
    }

    fun parseReferrerFromIntent(intent: Intent?): String? =
        intent?.data?.getQueryParameter("ref")?.trim()?.takeIf { it.isNotEmpty() }

    /** Server-minted referral JWT (`r` query); length-bounded. */
    fun parseReferralTokenFromIntent(intent: Intent?): String? {
        val t = intent?.data?.getQueryParameter("r")?.trim() ?: return null
        if (t.length < 16 || t.length > 8192) return null
        return t
    }
}

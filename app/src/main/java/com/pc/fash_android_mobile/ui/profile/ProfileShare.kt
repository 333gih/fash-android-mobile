package com.pc.fash_android_mobile.ui.profile

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.deeplink.ProfileDeepLinks

/** System share sheet for a public seller shop / profile link. */
object ProfileShare {

    fun launch(context: Context, username: String, displayName: String?) {
        val handle = ProfileDeepLinks.normalizeUsername(username)
        if (handle.isEmpty()) return
        val web = AppEnvironment.profileShareUrl(handle)
        if (web.isEmpty()) return
        val fashUri = ProfileDeepLinks.fashProfileUri(handle).toString()
        val name = displayName?.trim()?.takeIf { it.isNotEmpty() } ?: "@$handle"
        val text = context.getString(R.string.share_profile_text, name, web, fashUri)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_profile_subject, name))
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(send, context.getString(R.string.share))
        val host = context.findActivity() ?: context
        if (host !is Activity) {
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { host.startActivity(chooser) }
    }

    private fun Context.findActivity(): Activity? {
        var ctx: Context? = this
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }
}

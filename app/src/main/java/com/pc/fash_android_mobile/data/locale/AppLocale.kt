package com.pc.fash_android_mobile.data.locale

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Vietnamese uses `res/values/`; English uses `res/values-en/`.
 * Persists the in-app language choice and applies it via [AppCompatDelegate.setApplicationLocales].
 */
object AppLocale {

    private const val PREFS_NAME = "fash_app_prefs"
    private const val KEY_LANGUAGE_TAG = "app_language_tag"

    const val TAG_VI = "vi"
    const val TAG_EN = "en"

    /**
     * Reflects the locale AppCompat is actually applying (fixes UI stuck out of sync with [stringResource]).
     */
    fun currentTag(context: Context): String {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        if (appLocales.size() > 0) {
            val lang = appLocales[0]?.language.orEmpty()
            return if (lang == TAG_EN) TAG_EN else TAG_VI
        }
        val app = context.applicationContext
        val saved = readTag(app)
        if (saved != null) return saved
        return defaultTagForNewInstall(app)
    }

    fun setLocale(context: Context, languageTag: String) {
        val tag = normalizeTag(languageTag)
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE_TAG, tag)
            .commit()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    /**
     * Call from [android.app.Application.onCreate] before other startup work so every
     * [android.content.res.Resources] lookup uses the saved (or default) locale.
     */
    fun applyPersistedOrDefault(applicationContext: Context) {
        val app = applicationContext
        val tag = readTag(app) ?: defaultTagForNewInstall(app).also { writeTag(app, it) }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    private fun readTag(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE_TAG, null)
            ?.let { normalizeTag(it) }

    private fun writeTag(context: Context, tag: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE_TAG, tag)
            .commit()
    }

    private fun defaultTagForNewInstall(context: Context): String {
        val lang = context.resources.configuration.locales[0].language
        return if (lang == TAG_EN) TAG_EN else TAG_VI
    }

    private fun normalizeTag(raw: String): String =
        if (raw.startsWith(TAG_EN)) TAG_EN else TAG_VI
}

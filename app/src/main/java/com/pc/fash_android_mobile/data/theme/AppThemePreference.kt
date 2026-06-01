package com.pc.fash_android_mobile.data.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists light / dark appearance. Default is dark; light is opt-in via Settings.
 * [revision] notifies Compose to re-apply [FashTheme].
 * Uses the same SharedPreferences file as [com.pc.fash_android_mobile.data.locale.AppLocale] (`fash_app_prefs`).
 */
object AppThemePreference {

    private const val PREFS_NAME = "fash_app_prefs"
    private const val KEY_THEME_MODE = "app_theme_mode"

    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision.asStateFlow()

    enum class Mode {
        /** Legacy persisted value; treated as [DARK] when read. */
        SYSTEM,
        LIGHT,
        DARK,
    }

    fun readMode(context: Context): Mode {
        val raw = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME_MODE, null)
            ?: return Mode.DARK
        return when (raw) {
            "light" -> Mode.LIGHT
            "dark" -> Mode.DARK
            "system" -> Mode.DARK
            else -> Mode.DARK
        }
    }

    fun setMode(context: Context, mode: Mode) {
        val resolved = if (mode == Mode.SYSTEM) Mode.DARK else mode
        val value = when (resolved) {
            Mode.LIGHT -> "light"
            Mode.DARK -> "dark"
            Mode.SYSTEM -> "dark"
        }
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_MODE, value)
            .apply()
        _revision.value += 1
    }
}

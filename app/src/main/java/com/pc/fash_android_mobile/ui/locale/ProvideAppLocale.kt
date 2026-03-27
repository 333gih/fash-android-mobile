package com.pc.fash_android_mobile.ui.locale

import android.content.res.Configuration
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.pc.fash_android_mobile.data.locale.AppLocale

/**
 * Aligns Compose [androidx.compose.ui.res.stringResource] with [AppCompatDelegate] app locales.
 *
 * [androidx.activity.ComponentActivity] does not apply the same resource overlay as
 * AppCompatActivity, so [stringResource] can stay on the system/default language while the
 * login toggle matches AppCompat. This provider builds a
 * [android.content.Context.createConfigurationContext] from [AppLocale.currentTag] so
 * `values` / `values-en` resolve correctly after each toggle.
 *
 * [LocalContext] must stay tied to localized [android.content.res.Resources], but replacing it with
 * [android.content.Context.createConfigurationContext] drops the [android.app.Activity] identity.
 * Compose resolves [LocalActivityResultRegistryOwner] from the activity context, so we re-provide
 * the host [ComponentActivity] for activity-result and back-dispatch locals.
 */
@Composable
fun ProvideAppLocale(content: @Composable () -> Unit) {
    val baseContext = LocalContext.current
    val activity = baseContext as? ComponentActivity
    val rev by AppLocale.localeRevisionFlow.collectAsState()
    val tag = AppLocale.currentTag(baseContext)
    val localizedContext = remember(tag, rev) {
        val config = Configuration(baseContext.resources.configuration)
        config.setLocales(LocaleList.forLanguageTags(tag))
        baseContext.createConfigurationContext(config)
    }
    val localizedConfiguration = localizedContext.resources.configuration
    if (activity != null) {
        CompositionLocalProvider(
            LocalActivityResultRegistryOwner provides activity,
            LocalOnBackPressedDispatcherOwner provides activity,
            LocalContext provides localizedContext,
            LocalConfiguration provides localizedConfiguration,
        ) {
            content()
        }
    } else {
        CompositionLocalProvider(
            LocalContext provides localizedContext,
            LocalConfiguration provides localizedConfiguration,
        ) {
            content()
        }
    }
}

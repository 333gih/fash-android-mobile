package com.pc.fash_android_mobile.ui.main.tabs

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.BuildConfig
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.theme.AppThemePreference
import com.pc.fash_android_mobile.ui.locale.LoginLanguageToggle
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

/**
 * App settings: language, appearance, shortcuts to profile/shopping flows, notifications, account, about.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onLogoutAll: () -> Unit,
    isLoggingOut: Boolean,
    onOpenShippingAddresses: () -> Unit,
    onOpenOrders: () -> Unit,
    onOpenEditProfile: () -> Unit,
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val themeRev by AppThemePreference.revision.collectAsState()
    val currentThemeMode = remember(themeRev) { AppThemePreference.readMode(context) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = scheme.surface,
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.settings_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = scheme.onSurface,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                                tint = FashColors.Primary,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = scheme.surface,
                        titleContentColor = scheme.onSurface,
                    ),
                )
            },
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = FashTheme.spacing.editorialStart)
                    .padding(bottom = 32.dp),
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                SettingsSectionTitle(text = stringResource(R.string.settings_section_language))
                Text(
                    text = stringResource(R.string.settings_language_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    LoginLanguageToggle()
                }

                Spacer(modifier = Modifier.height(24.dp))
                SettingsSectionTitle(text = stringResource(R.string.settings_section_display))
                ThemeModeSelector(
                    selected = currentThemeMode,
                    onSelect = { mode -> AppThemePreference.setMode(context, mode) },
                )

                Spacer(modifier = Modifier.height(24.dp))
                SettingsSectionTitle(text = stringResource(R.string.settings_section_shopping))
                SettingsNavCard {
                    SettingsClickRow(
                        icon = {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = FashColors.Primary,
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        title = stringResource(R.string.settings_row_shipping_addresses),
                        onClick = onOpenShippingAddresses,
                    )
                    HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.35f))
                    SettingsClickRow(
                        icon = {
                            Icon(
                                Icons.Default.LocalMall,
                                contentDescription = null,
                                tint = FashColors.Primary,
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        title = stringResource(R.string.settings_row_orders),
                        onClick = onOpenOrders,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionTitle(text = stringResource(R.string.settings_section_profile))
                SettingsNavCard {
                    SettingsClickRow(
                        icon = {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = FashColors.Primary,
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        title = stringResource(R.string.settings_row_edit_profile),
                        onClick = onOpenEditProfile,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionTitle(text = stringResource(R.string.settings_section_notifications))
                SettingsNavCard {
                    SettingsClickRow(
                        icon = {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = FashColors.Primary,
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        title = stringResource(R.string.settings_row_notification_settings),
                        subtitle = stringResource(R.string.settings_row_notification_settings_sub),
                        onClick = { openAppNotificationSettings(context) },
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                SettingsSectionTitle(text = stringResource(R.string.settings_section_account))
                OutlinedButton(
                    onClick = onLogout,
                    enabled = !isLoggingOut,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_logout),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onLogoutAll,
                    enabled = !isLoggingOut,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_logout_all),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))
                SettingsSectionTitle(text = stringResource(R.string.settings_section_about))
                SettingsNavCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = FashColors.Primary.copy(alpha = 0.85f),
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_about_version_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.onSurfaceVariant,
                            )
                            Text(
                                text = stringResource(
                                    R.string.settings_about_version_value,
                                    BuildConfig.VERSION_NAME,
                                    BuildConfig.VERSION_CODE,
                                ),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = scheme.onSurface,
                            )
                            if (!AppEnvironment.isProd) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(
                                        R.string.settings_about_env_line,
                                        AppEnvironment.environmentName,
                                        AppEnvironment.flavor,
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.settings_footer_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 10.dp),
    )
}

@Composable
private fun SettingsNavCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun SettingsClickRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = scheme.onSurface,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = scheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun ThemeModeSelector(
    selected: AppThemePreference.Mode,
    onSelect: (AppThemePreference.Mode) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                scheme.surfaceContainerHighest,
                RoundedCornerShape(14.dp),
            )
            .padding(vertical = 4.dp),
    ) {
        ThemeModeRow(
            label = stringResource(R.string.settings_theme_system),
            selected = selected == AppThemePreference.Mode.SYSTEM,
            onClick = { onSelect(AppThemePreference.Mode.SYSTEM) },
            isSystemDefaultOption = true,
        )
        ThemeModeRow(
            label = stringResource(R.string.settings_theme_light),
            selected = selected == AppThemePreference.Mode.LIGHT,
            onClick = { onSelect(AppThemePreference.Mode.LIGHT) },
        )
        ThemeModeRow(
            label = stringResource(R.string.settings_theme_dark),
            selected = selected == AppThemePreference.Mode.DARK,
            onClick = { onSelect(AppThemePreference.Mode.DARK) },
        )
    }
}

@Composable
private fun ThemeModeRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    isSystemDefaultOption: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val rowBackground = when {
        !selected -> Color.Transparent
        isSystemDefaultOption -> FashColors.SystemDefaultThemeHighlight
        else -> scheme.surfaceContainerHighest.copy(alpha = 0.85f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .background(rowBackground, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = FashColors.Primary),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun openAppNotificationSettings(context: android.content.Context) {
    val pkg = context.packageName
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, pkg)
        }
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
    }
    runCatching { context.startActivity(intent) }
}

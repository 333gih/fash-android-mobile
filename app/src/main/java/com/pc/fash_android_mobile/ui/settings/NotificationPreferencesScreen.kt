package com.pc.fash_android_mobile.ui.settings

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.user.NotificationPreferences
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationPreferencesScreen(
    prefs: NotificationPreferences?,
    isLoading: Boolean,
    loadFailed: Boolean = false,
    onRetryLoad: () -> Unit = {},
    isSaving: Boolean,
    onRecommendationPushChanged: (Boolean) -> Unit,
    onRecommendationEmailChanged: (Boolean) -> Unit,
    onQuietHoursEnabledChanged: (Boolean) -> Unit,
    onQuietHoursStartChanged: (Int) -> Unit,
    onQuietHoursEndChanged: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    Surface(modifier = modifier.fillMaxSize(), color = scheme.surface) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.notification_preferences_title),
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
                    actions = {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = scheme.surface),
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = FashTheme.spacing.editorialStart)
                    .padding(bottom = 32.dp),
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.notification_preferences_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(20.dp))

                if (isLoading && prefs == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                } else if (loadFailed && prefs == null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = scheme.error,
                            modifier = Modifier.size(40.dp),
                        )
                        Text(
                            text = stringResource(R.string.notification_preferences_load_error),
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = onRetryLoad,
                            colors = ButtonDefaults.buttonColors(containerColor = FashColors.Primary),
                        ) {
                            Text(stringResource(R.string.feed_retry))
                        }
                    }
                } else if (prefs != null) {
                    PreferencesCard {
                        PreferenceSwitchRow(
                            title = stringResource(R.string.notification_preferences_recommendation_push),
                            checked = prefs.recommendationPushEnabled,
                            enabled = !isSaving,
                            onCheckedChange = onRecommendationPushChanged,
                        )
                        HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.35f))
                        PreferenceSwitchRow(
                            title = stringResource(R.string.notification_preferences_recommendation_email),
                            checked = prefs.recommendationEmailEnabled,
                            enabled = !isSaving,
                            onCheckedChange = onRecommendationEmailChanged,
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = stringResource(R.string.notification_preferences_quiet_hours),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.notification_preferences_quiet_hours_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val quietHoursEnabled = prefs.quietHoursStart != null && prefs.quietHoursEnd != null
                    PreferencesCard {
                        PreferenceSwitchRow(
                            title = stringResource(R.string.notification_preferences_quiet_hours_enabled),
                            checked = quietHoursEnabled,
                            enabled = !isSaving,
                            onCheckedChange = onQuietHoursEnabledChanged,
                        )
                        if (quietHoursEnabled) {
                            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.35f))
                            HourPickerRow(
                                label = stringResource(R.string.notification_preferences_quiet_hours_start),
                                selectedHour = prefs.quietHoursStart ?: 22,
                                enabled = !isSaving,
                                onHourSelected = onQuietHoursStartChanged,
                            )
                            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.35f))
                            HourPickerRow(
                                label = stringResource(R.string.notification_preferences_quiet_hours_end),
                                selectedHour = prefs.quietHoursEnd ?: 8,
                                enabled = !isSaving,
                                onHourSelected = onQuietHoursEndChanged,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreferencesCard(content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = scheme.surfaceContainerHighest,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun PreferenceSwitchRow(
    title: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HourPickerRow(
    label: String,
    selectedHour: Int,
    enabled: Boolean,
    onHourSelected: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }
    val hourLabel = formatHourLabel(selectedHour)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (enabled) expanded = !expanded },
        ) {
            OutlinedTextField(
                value = hourLabel,
                onValueChange = {},
                readOnly = true,
                enabled = enabled,
                modifier = Modifier
                    .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = enabled)
                    .fillMaxWidth(0.42f),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                shape = RoundedCornerShape(12.dp),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                (0..23).forEach { hour ->
                    DropdownMenuItem(
                        text = { Text(formatHourLabel(hour)) },
                        onClick = {
                            onHourSelected(hour)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

private fun formatHourLabel(hour: Int): String = "%02d:00".format(hour.coerceIn(0, 23))

@file:OptIn(ExperimentalLayoutApi::class)

package com.pc.fash_android_mobile.ui.listing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import com.pc.fash_android_mobile.data.locale.AppLocale
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.listing.ListingWearSeason

@Composable
fun ListingWearSeasonEditor(
    yearRoundWear: Boolean,
    seasonKeys: Set<String>,
    climateZones: Set<String>,
    macroRegions: Set<String>,
    enabled: Boolean,
    onYearRoundChange: (Boolean) -> Unit,
    onToggleSeason: (String) -> Unit,
    onToggleClimate: (String) -> Unit,
    onToggleRegion: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val localeVi = AppLocale.currentTag(LocalContext.current) != AppLocale.TAG_EN
    Column(modifier = modifier) {
        WearRowSwitch(
            label = stringResource(R.string.post_wear_year_round),
            checked = yearRoundWear,
            enabled = enabled,
            onCheckedChange = onYearRoundChange,
        )
        Spacer(modifier = Modifier.height(16.dp))
        WearChipSection(
            title = stringResource(R.string.post_wear_season_keys),
            options = ListingWearSeason.seasonOptions,
            selected = seasonKeys,
            enabled = enabled,
            localeVi = localeVi,
            onToggle = onToggleSeason,
        )
        Spacer(modifier = Modifier.height(12.dp))
        WearChipSection(
            title = stringResource(R.string.post_wear_climate_zones),
            options = ListingWearSeason.climateZoneOptions,
            selected = climateZones,
            enabled = enabled,
            localeVi = localeVi,
            onToggle = onToggleClimate,
        )
        Spacer(modifier = Modifier.height(12.dp))
        WearChipSection(
            title = stringResource(R.string.post_wear_macro_regions),
            options = ListingWearSeason.macroRegionOptions,
            selected = macroRegions,
            enabled = enabled,
            localeVi = localeVi,
            onToggle = onToggleRegion,
        )
    }
}

@Composable
private fun WearRowSwitch(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun WearChipSection(
    title: String,
    options: List<ListingWearSeason.Option>,
    selected: Set<String>,
    enabled: Boolean,
    localeVi: Boolean,
    onToggle: (String) -> Unit,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { opt ->
                FilterChip(
                    selected = selected.contains(opt.id),
                    onClick = { if (enabled) onToggle(opt.id) },
                    label = { Text(opt.label(localeVi)) },
                    enabled = enabled,
                )
            }
        }
    }
}

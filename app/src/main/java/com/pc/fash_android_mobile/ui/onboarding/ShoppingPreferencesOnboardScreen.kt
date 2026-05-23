package com.pc.fash_android_mobile.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R

@Composable
fun ShoppingPreferencesOnboardScreen(
    buySelected: Boolean,
    sellSelected: Boolean,
    onToggleBuy: () -> Unit,
    onToggleSell: () -> Unit,
    /** Currently selected gender preference ("women"|"men"|"non_binary"|"prefer_not_to_say"|""). */
    selectedGender: String = "",
    onGenderSelect: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.onboarding_shopping_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.onboarding_shopping_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = buySelected,
                onClick = onToggleBuy,
                label = { Text(stringResource(R.string.onboarding_shopping_intent_buy)) },
            )
            FilterChip(
                selected = sellSelected,
                onClick = onToggleSell,
                label = { Text(stringResource(R.string.onboarding_shopping_intent_sell)) },
            )
        }

        // Gender preference — helps power personalised recommendations
        Text(
            text = stringResource(R.string.onboarding_shopping_gender_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.onboarding_shopping_gender_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "women" to R.string.onboarding_shopping_gender_women,
                "men" to R.string.onboarding_shopping_gender_men,
                "non_binary" to R.string.onboarding_shopping_gender_non_binary,
            ).forEach { (value, labelRes) ->
                FilterChip(
                    selected = selectedGender == value,
                    onClick = { onGenderSelect(if (selectedGender == value) "" else value) },
                    label = { Text(stringResource(labelRes)) },
                )
            }
        }
    }
}

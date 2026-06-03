package com.pc.fash_android_mobile.ui.post

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.listing.ListingWearSeasonEditor
import com.pc.fash_android_mobile.ui.theme.FashTheme

@Composable
fun CreateListingWearSeasonStep(viewModel: PostViewModel, onCloseRequest: () -> Unit) {
    val draft by viewModel.draft.collectAsState()
    val canNext = draft.canProceedFromStep(6)
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PostListingColors.stepCanvas())
            .navigationBarsPadding(),
    ) {
        CreateListingFlowHeader(
            step = 6,
            totalSteps = TotalPostSteps,
            onBackClick = { viewModel.prevStep() },
            onCloseClick = onCloseRequest,
            primaryLabelRes = R.string.create_listing_next,
            onPrimaryClick = { viewModel.nextStep() },
            primaryEnabled = canNext,
            nextDisabledReasonRes = draft.nextStepBlockedReasonRes(6),
        )
        PostStepScrollWithBottomNotice(
            modifier = Modifier.weight(1f),
            horizontalPadding = FashTheme.spacing.editorialStart,
            bottomNotice = stringResource(R.string.post_hint_wear_season),
            scrollState = scrollState,
        ) {
            Text(
                text = stringResource(R.string.post_step_wear_season),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.post_step_wear_season_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            ListingWearSeasonEditor(
                yearRoundWear = draft.yearRoundWear,
                seasonKeys = draft.seasonKeys,
                climateZones = draft.climateZones,
                macroRegions = draft.macroRegions,
                enabled = true,
                onYearRoundChange = { viewModel.updateDraft { copy(yearRoundWear = it) } },
                onToggleSeason = { key ->
                    viewModel.updateDraft {
                        val next = seasonKeys.toMutableSet()
                        if (next.contains(key)) next.remove(key) else next.add(key)
                        copy(seasonKeys = next)
                    }
                },
                onToggleClimate = { key ->
                    viewModel.updateDraft {
                        val next = climateZones.toMutableSet()
                        if (next.contains(key)) next.remove(key) else next.add(key)
                        copy(climateZones = next)
                    }
                },
                onToggleRegion = { key ->
                    viewModel.updateDraft {
                        val next = macroRegions.toMutableSet()
                        if (next.contains(key)) next.remove(key) else next.add(key)
                        copy(macroRegions = next)
                    }
                },
            )
        }
    }
}

package com.pc.fash_android_mobile.ui.post

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

@Composable
fun CreateListingFillModeStep(
    viewModel: PostViewModel,
    onCloseRequest: () -> Unit,
) {
    val meProfile by viewModel.meProfile.collectAsState()
    val catalogLoading by viewModel.catalogLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadCatalogIfNeeded()
        viewModel.loadProfileForPreview()
    }

    val scrollState = rememberScrollState()
    val profileReady = meProfile != null
    val hasStyleData = meProfile?.hasStyleReferenceForListing() == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PostListingColors.stepCanvas())
            .navigationBarsPadding(),
    ) {
        CreateListingFlowHeader(
            step = 1,
            totalSteps = TotalPostSteps,
            onBackClick = null,
            onCloseClick = onCloseRequest,
            primaryLabelRes = R.string.create_listing_next,
            onPrimaryClick = {},
            primaryEnabled = false,
            showPrimaryAction = false,
            centerTitleRes = R.string.post_fill_mode_title,
            showStepCaptionUnderTitle = false,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = FashTheme.spacing.editorialStart),
        ) {
            Text(
                text = stringResource(R.string.post_fill_mode_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (!profileReady && catalogLoading) {
                BoxLoading()
            } else {
                FillModeOptionCard(
                    title = stringResource(R.string.post_fill_mode_from_profile_title),
                    subtitle = if (hasStyleData) {
                        stringResource(R.string.post_fill_mode_from_profile_subtitle_ready)
                    } else {
                        stringResource(R.string.post_fill_mode_from_profile_subtitle_empty)
                    },
                    detail = buildProfileStyleSummary(meProfile),
                    icon = Icons.Default.Style,
                    selected = false,
                    onClick = { viewModel.selectFillMode(CreateListingFillMode.FROM_PROFILE_STYLE) },
                )
                Spacer(modifier = Modifier.height(12.dp))
                FillModeOptionCard(
                    title = stringResource(R.string.post_fill_mode_manual_title),
                    subtitle = stringResource(R.string.post_fill_mode_manual_subtitle),
                    detail = null,
                    icon = Icons.Default.Edit,
                    selected = false,
                    onClick = { viewModel.selectFillMode(CreateListingFillMode.MANUAL) },
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BoxLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = FashColors.Primary)
    }
}

@Composable
private fun FillModeOptionCard(
    title: String,
    subtitle: String,
    detail: String?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val borderColor = if (selected) FashColors.Primary else scheme.outlineVariant.copy(alpha = 0.6f)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, RoundedCornerShape(FashTheme.spacing.radiusCard))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
        color = PostListingColors.fieldSurface(),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = FashColors.Primary,
                modifier = Modifier.size(28.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
                if (!detail.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
fun PostProfilePrefilledBanner() {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = scheme.primaryContainer.copy(alpha = 0.35f),
        tonalElevation = 0.dp,
    ) {
        Text(
            text = stringResource(R.string.post_fill_mode_prefilled_hint),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun buildProfileStyleSummary(profile: com.pc.fash_android_mobile.data.user.ProfileInfo?): String? {
    if (profile == null) return null
    val parts = mutableListOf<String>()
    if (profile.aestheticTagSnapshots.isNotEmpty()) {
        val names = profile.aestheticTagSnapshots.take(3).joinToString(", ") { it.name.ifBlank { it.id } }
        parts.add(stringResource(R.string.post_fill_mode_profile_tags, names))
    }
    if (!profile.referenceSize.isNullOrBlank()) {
        parts.add(stringResource(R.string.post_fill_mode_profile_size, profile.referenceSize!!))
    }
    if (profile.gender.isNotBlank()) {
        mapProfileGenderToListingTarget(profile.gender)?.let { target ->
            val label = when (target) {
                "women" -> stringResource(R.string.gender_target_women)
                "men" -> stringResource(R.string.gender_target_men)
                "unisex" -> stringResource(R.string.gender_target_unisex)
                else -> target
            }
            parts.add(stringResource(R.string.post_fill_mode_profile_gender, label))
        }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

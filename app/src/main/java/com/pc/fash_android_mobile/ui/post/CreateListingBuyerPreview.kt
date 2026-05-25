@file:OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.pc.fash_android_mobile.ui.post

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.common.CommonAestheticTagDto
import com.pc.fash_android_mobile.data.common.displayLabel
import com.pc.fash_android_mobile.data.locale.AppLocale
import com.pc.fash_android_mobile.data.user.ProfileInfo
import com.pc.fash_android_mobile.ui.components.FashAsyncImage
import com.pc.fash_android_mobile.ui.feed.resolveListingImageUrl
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

@Composable
fun CreateListingBuyerPreview(
    draft: CreateListingDraft,
    meProfile: ProfileInfo?,
    aestheticTagsById: Map<String, CommonAestheticTagDto>,
    onEditStep: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val isVi = AppLocale.currentTag(androidx.compose.ui.platform.LocalContext.current) != AppLocale.TAG_EN
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = scheme.primaryContainer.copy(alpha = 0.35f),
            tonalElevation = 0.dp,
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = stringResource(R.string.post_review_buyer_banner_title),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.post_review_buyer_banner_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }

        Text(
            text = stringResource(R.string.post_review_feed_preview_heading),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = scheme.onSurfaceVariant,
        )
        CreateListingReviewCard(
            draft = draft,
            meProfile = meProfile,
            aestheticTagsById = aestheticTagsById,
        )

        Text(
            text = stringResource(R.string.post_review_detail_preview_heading),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = scheme.onSurfaceVariant,
        )
        CreateListingDetailPreviewCard(
            draft = draft,
            aestheticTagsById = aestheticTagsById,
            onEditStep = onEditStep,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CreateListingDetailPreviewCard(
    draft: CreateListingDraft,
    aestheticTagsById: Map<String, CommonAestheticTagDto>,
    onEditStep: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val isVi = AppLocale.currentTag(androidx.compose.ui.platform.LocalContext.current) != AppLocale.TAG_EN
    val imageModels = remember(draft.listingPhotoSlots) {
        draft.listingPhotoSlots
            .sortedBy { it.sortOrder }
            .mapNotNull { slot ->
                when {
                    slot.uploadedImageUrl?.isNotBlank() == true ->
                        resolveListingImageUrl(slot.uploadedImageUrl!!)
                    slot.localImageUri?.isNotBlank() == true -> slot.localImageUri!!
                    else -> null
                }
            }
    }
    val tagLabels = draft.selectedAestheticTagIds.mapNotNull { id ->
        aestheticTagsById[id]?.displayLabel(isVi)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
        color = PostListingColors.fieldSurface(),
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(FashTheme.spacing.spacing3)) {
            if (imageModels.isNotEmpty()) {
                val pagerState = rememberPagerState(pageCount = { imageModels.size })
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(12.dp)),
                ) {
                    HorizontalPager(state = pagerState) { page ->
                        FashAsyncImage(
                            model = imageModels[page],
                            contentDescription = draft.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    if (imageModels.size > 1) {
                        Text(
                            text = stringResource(
                                R.string.explore_preview_image_page,
                                pagerState.currentPage + 1,
                                imageModels.size,
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onPrimary,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp)
                                .background(
                                    androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f),
                                    RoundedCornerShape(8.dp),
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
                PreviewEditRow(
                    label = stringResource(R.string.post_step_photos),
                    value = stringResource(R.string.post_review_photo_count, imageModels.size),
                    onEdit = { onEditStep(7) },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = scheme.outlineVariant.copy(alpha = 0.5f),
                )
            }

            Text(
                text = draft.title.ifBlank { stringResource(R.string.post_review_missing_title) },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (draft.title.isBlank()) scheme.error else scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = formatDraftPriceVnd(draft.priceVnd),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = FashColors.Primary,
            )

            Spacer(modifier = Modifier.height(12.dp))
            PreviewEditRow(
                label = stringResource(R.string.create_listing_category_label),
                value = draft.categoryName.ifBlank { stringResource(R.string.post_review_not_set) },
                onEdit = { onEditStep(1) },
                highlightMissing = draft.categoryId.isBlank(),
            )
            PreviewEditRow(
                label = stringResource(R.string.create_listing_condition_label),
                value = formatConditionDisplay(draft.condition).ifBlank { stringResource(R.string.post_review_not_set) },
                onEdit = { onEditStep(5) },
                highlightMissing = draft.condition.isBlank(),
            )
            PreviewEditRow(
                label = stringResource(R.string.create_listing_brand_label),
                value = draft.brandName.ifBlank { stringResource(R.string.post_review_optional_empty) },
                onEdit = { onEditStep(3) },
            )
            PreviewEditRow(
                label = stringResource(R.string.create_listing_size_label),
                value = draft.size.ifBlank { stringResource(R.string.post_review_optional_empty) },
                onEdit = { onEditStep(6) },
            )
            if (draft.color.isNotBlank()) {
                PreviewEditRow(
                    label = stringResource(R.string.post_step_color),
                    value = draft.color,
                    onEdit = { onEditStep(5) },
                )
            }
            if (draft.genderTarget.isNotBlank()) {
                PreviewEditRow(
                    label = stringResource(R.string.post_step_gender_target),
                    value = genderTargetLabel(draft.genderTarget),
                    onEdit = { onEditStep(6) },
                )
            }
            PreviewEditRow(
                label = stringResource(R.string.post_step_country),
                value = draft.countryName.ifBlank { stringResource(R.string.post_review_optional_empty) },
                onEdit = { onEditStep(4) },
            )
            PreviewEditRow(
                label = stringResource(R.string.post_step_price),
                value = formatDraftPriceVnd(draft.priceVnd),
                onEdit = { onEditStep(8) },
                highlightMissing = draft.priceVnd.trim().isEmpty(),
            )
            PreviewEditRow(
                label = stringResource(R.string.post_step_shipping),
                value = draft.shippingAddressLabel.ifBlank { stringResource(R.string.post_review_not_set) },
                onEdit = { onEditStep(9) },
                highlightMissing = draft.shippingAddressLabel.isBlank(),
            )

            if (draft.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.product_section_description),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                )
                Text(
                    text = draft.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                TextButtonEdit(onEdit = { onEditStep(5) })
            }

            if (hasAnyMeasurement(draft)) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.product_section_measurements),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                )
                measurementPreviewLines(draft).forEach { (label, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(text = label, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        Text(text = value, style = MaterialTheme.typography.bodySmall, color = scheme.onSurface)
                    }
                }
                TextButtonEdit(onEdit = { onEditStep(6) })
            }

            if (tagLabels.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.product_section_style),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 6.dp),
                ) {
                    tagLabels.forEach { tag ->
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier
                                .background(scheme.surfaceContainerHigh, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
                TextButtonEdit(onEdit = { onEditStep(2) })
            }
        }
    }
}

@Composable
private fun PreviewEditRow(
    label: String,
    value: String,
    onEdit: () -> Unit,
    highlightMissing: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = if (highlightMissing) scheme.error else scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = stringResource(R.string.post_review_edit_section),
            tint = FashColors.Primary,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun TextButtonEdit(onEdit: () -> Unit) {
    Text(
        text = stringResource(R.string.post_review_edit_section),
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = FashColors.Primary,
        modifier = Modifier
            .padding(top = 6.dp)
            .clickable(onClick = onEdit),
    )
}

@Composable
private fun genderTargetLabel(value: String): String = when (value.lowercase()) {
    "women" -> stringResource(R.string.gender_target_women)
    "men" -> stringResource(R.string.gender_target_men)
    "unisex" -> stringResource(R.string.gender_target_unisex)
    "kids" -> stringResource(R.string.gender_target_kids)
    else -> value
}

internal fun hasAnyMeasurement(draft: CreateListingDraft): Boolean =
    listOf(
        draft.measurementHem,
        draft.measurementChest,
        draft.measurementLength,
        draft.measurementShoulders,
        draft.measurementSleeveLength,
    ).any { it.isNotBlank() }

@Composable
private fun measurementPreviewLines(draft: CreateListingDraft): List<Pair<String, String>> {
    val unit = if (draft.measurementUnit.equals("in", ignoreCase = true)) {
        stringResource(R.string.post_unit_in)
    } else {
        stringResource(R.string.post_unit_cm)
    }
    return buildList {
        if (draft.measurementHem.isNotBlank()) {
            add(stringResource(R.string.profile_setup_measurement_hem) to "${draft.measurementHem.trim()} $unit")
        }
        if (draft.measurementChest.isNotBlank()) {
            add(stringResource(R.string.profile_setup_measurement_chest) to "${draft.measurementChest.trim()} $unit")
        }
        if (draft.measurementLength.isNotBlank()) {
            add(stringResource(R.string.profile_setup_measurement_length) to "${draft.measurementLength.trim()} $unit")
        }
        if (draft.measurementShoulders.isNotBlank()) {
            add(stringResource(R.string.profile_setup_measurement_shoulders) to "${draft.measurementShoulders.trim()} $unit")
        }
        if (draft.measurementSleeveLength.isNotBlank()) {
            add(stringResource(R.string.profile_setup_measurement_sleeve) to "${draft.measurementSleeveLength.trim()} $unit")
        }
    }
}

internal fun formatConditionDisplay(condition: String): String = when (condition.lowercase()) {
    "new" -> "Mới"
    "like_new", "like new" -> "Như mới"
    "good" -> "Tốt"
    "fair" -> "Khá"
    else -> condition.ifBlank { "—" }
}

internal fun formatDraftPriceVnd(raw: String): String {
    val v = raw.trim().replace(".", "").replace(",", "").toLongOrNull() ?: 0L
    return "₫ ${java.text.NumberFormat.getIntegerInstance(java.util.Locale.getDefault()).format(v).replace(',', '.')}"
}

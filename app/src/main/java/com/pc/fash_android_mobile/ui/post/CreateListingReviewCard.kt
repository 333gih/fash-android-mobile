package com.pc.fash_android_mobile.ui.post

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.locale.AppLocale
import com.pc.fash_android_mobile.data.common.CommonAestheticTagDto
import com.pc.fash_android_mobile.data.common.displayLabel
import com.pc.fash_android_mobile.data.user.ProfileInfo
import com.pc.fash_android_mobile.ui.components.FashAsyncImage
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

@Composable
fun CreateListingReviewCard(
    draft: CreateListingDraft,
    meProfile: ProfileInfo?,
    aestheticTagsById: Map<String, CommonAestheticTagDto>,
) {
    val scheme = MaterialTheme.colorScheme
    val isVi = AppLocale.currentTag(LocalContext.current) != AppLocale.TAG_EN
    val coverSlot = draft.listingPhotoSlots
        .sortedBy { it.sortOrder }
        .firstOrNull { it.hasImageSelected() }
    val coverImageUrl = coverSlot?.uploadedImageUrl?.takeIf { it.isNotBlank() }
    val coverImageUri = coverSlot?.localImageUri?.takeIf { it.isNotBlank() && coverImageUrl == null }
    val firstTagDisplay = draft.selectedAestheticTagIds.firstOrNull()?.let { id ->
        aestheticTagsById[id]?.displayLabel(isVi)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
        colors = CardDefaults.cardColors(containerColor = PostListingColors.fieldSurface()),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(FashTheme.spacing.spacing3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(FashColors.Primary.copy(alpha = 0.2f)),
                ) {
                    meProfile?.avatarUrl?.takeIf { it.isNotBlank() }?.let { url ->
                        val fullUrl = resolveListingImageUrl(url)
                        FashAsyncImage(
                            model = fullUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "@${meProfile?.username ?: "user"}",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                        color = scheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.create_listing_seller_just_active),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                firstTagDisplay?.let { tag ->
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier
                            .background(
                                scheme.surfaceContainerHigh,
                                RoundedCornerShape(8.dp),
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .padding(horizontal = FashTheme.spacing.spacing3),
            ) {
                val imageModel = when {
                    coverImageUrl?.isNotBlank() == true -> resolveListingImageUrl(coverImageUrl)
                    coverImageUri != null -> coverImageUri
                    else -> null
                }
                if (imageModel != null) {
                    FashAsyncImage(
                        model = imageModel,
                        contentDescription = draft.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(PostListingColors.fieldSurface()),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.no_image),
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                ) {
                    Text(
                        text = formatConditionDisplay(draft.condition),
                        style = MaterialTheme.typography.labelSmall,
                        color = FashColors.Primary,
                        modifier = Modifier
                            .background(
                                PostListingColors.fieldSurface(),
                                RoundedCornerShape(6.dp),
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = formatDraftPriceVnd(draft.priceVnd),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = androidx.compose.ui.graphics.Color.White,
                    )
                }
            }

            Text(
                text = draft.title.ifBlank { "—" },
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = scheme.onSurface,
                modifier = Modifier.padding(
                    horizontal = FashTheme.spacing.spacing3,
                    vertical = FashTheme.spacing.spacing2,
                ),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            if (draft.brandName.isNotBlank() || draft.size.isNotBlank()) {
                Text(
                    text = listOfNotNull(
                        draft.brandName.takeIf { it.isNotBlank() },
                        draft.size.takeIf { it.isNotBlank() }?.let { "${stringResource(R.string.create_listing_size_label)}: $it" },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = FashTheme.spacing.spacing3),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (draft.description.isNotBlank()) {
                Text(
                    text = draft.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        horizontal = FashTheme.spacing.spacing3,
                        vertical = 4.dp,
                    ),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = FashTheme.spacing.spacing3,
                        vertical = FashTheme.spacing.spacing2,
                    ),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = scheme.onSurfaceVariant,
                )
                Text(
                    text = draft.countryName.ifBlank { stringResource(R.string.create_listing_location_default) },
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = scheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.create_listing_just_now),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }

            if (draft.shippingAddressLabel.isNotBlank()) {
                Text(
                    text = draft.shippingAddressLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.primary,
                    modifier = Modifier
                        .padding(horizontal = FashTheme.spacing.spacing3)
                        .padding(bottom = FashTheme.spacing.spacing2),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun CreateListingReviewFooter() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    PostFlowLegalNoticeCard(
        horizontalPadding = FashTheme.spacing.editorialStart,
        title = stringResource(R.string.post_review_notice_title),
        onTermsClick = {
            uriHandler.openUri(AppEnvironment.legalTermsUrl(AppLocale.currentTag(context)))
        },
        onPrivacyClick = {
            uriHandler.openUri(AppEnvironment.legalPrivacyUrl(AppLocale.currentTag(context)))
        },
    )
}

private fun resolveListingImageUrl(path: String): String {
    if (path.startsWith("http")) return path
    val base = AppEnvironment.apiBaseUrl.trimEnd('/')
    return if (path.startsWith("/")) "$base$path" else "$base/$path".takeIf { path.isNotBlank() } ?: ""
}

@file:OptIn(ExperimentalFoundationApi::class)

package com.pc.fash_android_mobile.ui.feed

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.pc.fash_android_mobile.ui.components.LocalFashTabSwipeConsuming
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.ui.components.FashAsyncImage
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme
import java.time.Instant
import java.util.Locale

private const val JustListedThresholdMs = 2 * 60 * 60 * 1000L
private const val SavedCountThreshold = 3
private const val DwellMinMs = 800

/**
 * Discovery grid cell: image, price, engagement, title, condition (pill) + category/brand/size/tag, seller — optional like/save.
 * [compactFooter] hides extra lines for narrow horizontal carousels (e.g. product detail “more from seller”).
 * [onDwell] fires when the card leaves composition with dwell time in ms (≥ DwellMinMs).
 */
@Composable
fun ListingGridCard(
    item: ListingFeedItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Width / height of the image area (taller = larger preview for the same column width). */
    imageAspectRatio: Float = 3f / 4f,
    showQuickActions: Boolean = false,
    onLike: () -> Unit = {},
    onSave: () -> Unit = {},
    /** Fewer text lines for thin columns. */
    compactFooter: Boolean = false,
    /** Short marketplace status (e.g. own profile); drawn top-start with the photo-stack badge. */
    statusOverlayLabel: String? = null,
    /** Called when the card leaves composition; provides dwell time in ms (≥ DwellMinMs). */
    onDwell: ((dwellMs: Int) -> Unit)? = null,
    /** When set (masonry grids), used for Coil decode size instead of a screen estimate. */
    columnWidthDp: Float? = null,
) {
    if (onDwell != null) {
        DisposableEffect(item.id) {
            val startMs = System.currentTimeMillis()
            onDispose {
                val dwell = (System.currentTimeMillis() - startMs).toInt()
                if (dwell >= DwellMinMs) onDwell(dwell)
            }
        }
    }
    val scarcityBadge = listingScarcityBadge(item, compactFooter)
    val commitmentBadge = if (item.onsiteInspectionCommitment && !compactFooter) {
        stringResource(R.string.listing_commitment_badge)
    } else {
        null
    }
    val density = LocalDensity.current
    val resolvedColumnWidthDp = columnWidthDp
        ?: rememberListingMasonryColumnWidthDp()
    val feedImageUrl = item.coverImageUrl.trim().ifEmpty { item.imageUrls.firstOrNull().orEmpty() }
        .let { raw ->
            if (raw.isEmpty()) ""
            else FeedListingImageSizer.urlForFeedGrid(raw, resolvedColumnWidthDp, density.density)
        }
    val decodeSize = FeedListingImageSizer.pixelSize(resolvedColumnWidthDp, density.density, imageAspectRatio)
    val shape = RoundedCornerShape(FashTheme.spacing.radiusSoftMin)
    val metaUi = listingCardMetaUi(item, compactFooter)
    val sellerLine = listingCardSellerLine(item)
    val cardA11y = listingCardContentDescription(item, metaUi.combinedA11y, statusOverlayLabel)
    val tabSwipeConsuming = LocalFashTabSwipeConsuming.current

    val boxModifier = if (columnWidthDp != null) {
        modifier.fillMaxWidth()
    } else {
        modifier
            .fillMaxWidth()
            .aspectRatio(imageAspectRatio)
    }
    Box(
        modifier = boxModifier
            .clip(shape)
            .semantics(mergeDescendants = true) {
                contentDescription = cardA11y
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = !tabSwipeConsuming, onClick = onClick),
        ) {
            if (feedImageUrl.isNotEmpty()) {
                FashAsyncImage(
                    model = feedImageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    targetPixelSize = decodeSize,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.no_image),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Scarcity / commitment badges: top-end
            if (scarcityBadge != null || commitmentBadge != null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    commitmentBadge?.let { label ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1B5E20).copy(alpha = 0.88f),
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.2.sp,
                                ),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    scarcityBadge?.let { label ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = FashColors.Primary.copy(alpha = 0.88f),
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.2.sp,
                                ),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            val statusTrimmed = statusOverlayLabel?.trim()?.takeIf { it.isNotEmpty() }
            if (item.imageUrls.size > 1 || statusTrimmed != null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (item.imageUrls.size > 1) {
                        val photoStackA11y = stringResource(
                            R.string.listing_card_photo_stack_a11y,
                            item.imageUrls.size,
                        )
                        Surface(
                            modifier = Modifier.semantics(mergeDescendants = true) {
                                contentDescription = photoStackA11y
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.45f),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Collections,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = item.imageUrls.size.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                )
                            }
                        }
                    }
                    if (statusTrimmed != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.45f),
                        ) {
                            Text(
                                text = statusTrimmed,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.2.sp,
                                ),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.82f),
                            ),
                        ),
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatListingPriceVnd(item.priceVnd),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    val likes = formatListingEngagementShort(item.likeCount)
                    if (likes.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.95f),
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = likes,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.2.sp,
                                ),
                                color = Color.White.copy(alpha = 0.95f),
                            )
                        }
                    }
                }

                val title = sanitizeListingUiText(item.title).trim()
                if (title.isNotEmpty()) {
                    Text(
                        text = title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .listingCardMarquee(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 16.sp,
                        ),
                        color = Color.White,
                        maxLines = 1,
                    )
                }

                if (!compactFooter && metaUi.hasAny) {
                    ListingCardMetaRow(metaUi)
                }

                Text(
                    text = sellerLine,
                    modifier = Modifier
                        .fillMaxWidth()
                        .listingCardMarquee(),
                    style = MaterialTheme.typography.labelSmall.copy(lineHeight = 14.sp),
                    color = Color.White.copy(alpha = 0.88f),
                    maxLines = 1,
                )
            }
        }

        if (showQuickActions) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .zIndex(2f)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(
                    onClick = onLike,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.35f),
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(
                        imageVector = if (item.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = stringResource(R.string.like),
                        tint = if (item.isLiked) FashColors.Primary else Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(
                    onClick = onSave,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.35f),
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(
                        imageVector = if (item.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = stringResource(R.string.save),
                        tint = if (item.isSaved) FashColors.Primary else Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun listingConditionLabel(raw: String): String {
    val cleaned = sanitizeListingUiText(raw)
    if (cleaned.isBlank()) return ""
    val v = cleaned.lowercase(Locale.ROOT).trim().replace(' ', '_')
    return when (v) {
        "new" -> stringResource(R.string.condition_new)
        "like_new", "like-new" -> stringResource(R.string.condition_like_new)
        "good" -> stringResource(R.string.condition_good)
        "fair" -> stringResource(R.string.condition_fair)
        else -> cleaned
    }
}

private data class ListingMetaUi(
    val conditionLabel: String,
    val secondary: String,
    val combinedA11y: String,
) {
    val hasAny: Boolean get() = conditionLabel.isNotBlank() || secondary.isNotBlank()
}

@Composable
private fun listingCardMetaUi(item: ListingFeedItem, compactFooter: Boolean): ListingMetaUi {
    if (compactFooter) return ListingMetaUi("", "", "")
    val cond = listingConditionLabel(item.condition)
    val secondary = buildListingCardSecondaryMeta(item)
    val combined = listOfNotNull(
        cond.takeIf { it.isNotBlank() },
        secondary.takeIf { it.isNotBlank() },
    ).joinToString(" · ")
    return ListingMetaUi(cond, secondary, combined)
}

/** Category · brand · size · listing vibe — not the condition (that is the fixed pill). */
private fun buildListingCardSecondaryMeta(item: ListingFeedItem): String {
    fun p(raw: String?): String? =
        raw?.trim()?.takeIf { it.isNotEmpty() }?.let { sanitizeListingUiText(it) }?.takeIf { it.isNotEmpty() }
    val cat = p(item.categoryName)
    val brand = p(item.brand)
    val size = p(item.size)
    val sellerStyle = p(item.sellerStyleTag)
    val vibe = p(item.listingAestheticTag)
        ?.takeUnless { v -> sellerStyle != null && v.equals(sellerStyle, ignoreCase = true) }
    return listOfNotNull(cat, brand, size, vibe).joinToString(" · ")
}

@Composable
private fun ListingCardMetaRow(parts: ListingMetaUi) {
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        lineHeight = 14.sp,
        letterSpacing = 0.15.sp,
    )
    when {
        parts.conditionLabel.isNotBlank() && parts.secondary.isNotBlank() -> Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(5.dp),
                color = Color.White.copy(alpha = 0.24f),
            ) {
                Text(
                    text = parts.conditionLabel,
                    modifier = Modifier
                        .widthIn(max = 112.dp)
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                        .listingCardMarquee(),
                    style = labelStyle.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    maxLines = 1,
                )
            }
            Text(
                text = parts.secondary,
                modifier = Modifier
                    .weight(1f)
                    .listingCardMarquee(),
                style = labelStyle,
                color = Color.White.copy(alpha = 0.92f),
                maxLines = 1,
            )
        }
        parts.conditionLabel.isNotBlank() -> Surface(
            shape = RoundedCornerShape(5.dp),
            color = Color.White.copy(alpha = 0.24f),
        ) {
            Text(
                text = parts.conditionLabel,
                modifier = Modifier
                    .widthIn(max = 160.dp)
                    .padding(horizontal = 6.dp, vertical = 3.dp)
                    .listingCardMarquee(),
                style = labelStyle.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                maxLines = 1,
            )
        }
        parts.secondary.isNotBlank() -> Text(
            text = parts.secondary,
            modifier = Modifier
                .fillMaxWidth()
                .listingCardMarquee(),
            style = labelStyle,
            color = Color.White.copy(alpha = 0.92f),
            maxLines = 1,
        )
    }
}

private fun listingCardSellerLine(item: ListingFeedItem): String {
    val user = sanitizeListingUiText(item.sellerUsername ?: "").ifBlank { "user" }
    val tag = item.sellerStyleTag?.trim()?.takeIf { it.isNotEmpty() }
        ?.let { sanitizeListingUiText(it) }
        ?.takeIf { it.isNotEmpty() }
    return buildString {
        append('@')
        append(user)
        if (tag != null) {
            append(" · ")
            append(tag)
        }
    }
}

@Composable
private fun listingCardContentDescription(
    item: ListingFeedItem,
    metaLine: String,
    statusOverlay: String? = null,
): String {
    val title = sanitizeListingUiText(item.title).trim()
    val price = formatListingPriceVnd(item.priceVnd)
    val user = sanitizeListingUiText(item.sellerUsername ?: "").ifBlank { "user" }
    val status = statusOverlay?.trim()?.takeIf { it.isNotEmpty() }
    return buildString {
        if (title.isNotEmpty()) {
            append(title)
            append(". ")
        }
        append(price)
        if (metaLine.isNotEmpty()) {
            append(". ")
            append(metaLine)
        }
        if (status != null) {
            append(". ")
            append(status)
        }
        append(". ")
        append(stringResource(R.string.listing_card_a11y_seller_role))
        append(" ")
        append(user)
    }
}

private fun Modifier.listingCardMarquee(): Modifier = basicMarquee(
    initialDelayMillis = 900,
    repeatDelayMillis = 1_200,
    velocity = 35.dp,
)

@Composable
private fun listingScarcityBadge(item: ListingFeedItem, compactFooter: Boolean): String? {
    if (compactFooter) return null
    val createdAt = item.createdAt?.takeIf { it.isNotBlank() }
    if (createdAt != null) {
        val ageMs = runCatching {
            System.currentTimeMillis() - Instant.parse(createdAt).toEpochMilli()
        }.getOrElse { Long.MAX_VALUE }
        if (ageMs in 0L until JustListedThresholdMs) {
            return stringResource(R.string.listing_badge_just_listed)
        }
    }
    if (item.saveCount >= SavedCountThreshold) {
        return stringResource(R.string.listing_badge_saved_count, item.saveCount)
    }
    return null
}
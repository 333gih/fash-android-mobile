@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.pc.fash_android_mobile.ui.explore

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.config.BusinessFlowConfig
import com.pc.fash_android_mobile.data.listing.ListingDetail
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.ui.components.FashAsyncImage
import com.pc.fash_android_mobile.ui.components.FashAvatarCircle
import com.pc.fash_android_mobile.ui.feed.formatListingEngagementShort
import com.pc.fash_android_mobile.ui.feed.formatListingPriceVnd
import com.pc.fash_android_mobile.ui.feed.resolveListingImageUrl
import com.pc.fash_android_mobile.ui.feed.resolveProfileImageUrlOrNull
import com.pc.fash_android_mobile.ui.guest.GuestLoginReason
import com.pc.fash_android_mobile.ui.locale.ProvideAppLocale
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.distinctUntilChanged

/** Quick-look sheet — one third of the viewport; content scrolls inside. */
private const val ExplorePreviewSheetHeightFraction = 1f / 3f
/** Hide the bounce hint after the user scrolls this many px. */
private const val ExplorePreviewScrollHintDismissPx = 8
private val PreviewEdgeStart = 16.dp
private val PreviewEdgeEnd = 12.dp
private val PreviewThumbWidth = 96.dp
private val PreviewThumbHeight = 72.dp
private val PreviewAvatarSize = 28.dp
private val PreviewActionIconButtonSize = 38.dp

@Composable
fun ExploreListingPreviewSheet(
    feedItem: ListingFeedItem,
    detail: ListingDetail?,
    isDetailLoading: Boolean,
    onDismiss: () -> Unit,
    onViewDetail: () -> Unit,
    onLike: () -> Unit,
    onSave: () -> Unit,
    isGuestMode: Boolean,
    onRequestLogin: (GuestLoginReason) -> Unit,
    onMessageSeller: () -> Unit,
    hasExistingConversation: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val configuration = LocalConfiguration.current
    val sheetHeight = remember(configuration.screenHeightDp) {
        (configuration.screenHeightDp * ExplorePreviewSheetHeightFraction).roundToInt().dp
    }
    val imageUrls = remember(feedItem.id, detail?.imageUrls, feedItem.imageUrls, feedItem.coverImageUrl) {
        val fromDetail = detail?.imageUrls.orEmpty().map { resolveListingImageUrl(it) }.filter { it.isNotEmpty() }
        if (fromDetail.isNotEmpty()) {
            fromDetail
        } else {
            feedItem.imageUrls.map { resolveListingImageUrl(it) }.filter { it.isNotEmpty() }.ifEmpty {
                resolveListingImageUrl(feedItem.coverImageUrl).takeIf { it.isNotEmpty() }?.let { listOf(it) }
                    ?: emptyList()
            }
        }
    }
    val status = (detail?.status ?: feedItem.listingStatus)?.lowercase(Locale.ROOT).orEmpty()
    val isSold = status == "sold"
    val isReserved = status == "reserved"
    val buyNowEnabled = BusinessFlowConfig.c2cBuyNowEnabled && !isSold && !isReserved

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = scheme.surface,
        contentColor = scheme.onSurface,
    ) {
        // Dialog window uses system/default Configuration — re-apply app locale for stringResource.
        ProvideAppLocale {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetHeight)
                .navigationBarsPadding(),
        ) {
            val scrollState = rememberScrollState()
            var showScrollHint by remember(feedItem.id) { mutableStateOf(true) }
            var canScrollFurther by remember(feedItem.id) { mutableStateOf(false) }

            LaunchedEffect(scrollState, feedItem.id, isDetailLoading, detail?.id) {
                snapshotFlow {
                    scrollState.value to scrollState.maxValue
                }
                    .distinctUntilChanged()
                    .collect { (offset, max) ->
                        canScrollFurther = max > 0 && offset < max - 4
                        if (offset > ExplorePreviewScrollHintDismissPx) {
                            showScrollHint = false
                        }
                    }
            }

            Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(start = PreviewEdgeStart, end = PreviewEdgeEnd),
                ) {
                    Text(
                        text = stringResource(R.string.explore_preview_sheet_title),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.explore_preview_sheet_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 1.dp, bottom = 6.dp),
                    )

                    if (isSold || isReserved) {
                        ExplorePreviewStatusBanner(isSold = isSold)
                        Spacer(Modifier.height(6.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        ExplorePreviewImageThumb(
                            imageUrls = imageUrls,
                            title = feedItem.title,
                            modifier = Modifier.size(PreviewThumbWidth, PreviewThumbHeight),
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            ExplorePreviewPriceRow(
                                priceVnd = detail?.priceVnd ?: feedItem.priceVnd,
                                listPriceVnd = detail?.listPriceVnd,
                            )
                            Text(
                                text = detail?.title?.ifBlank { feedItem.title } ?: feedItem.title,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 16.sp,
                                ),
                                color = scheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            ExplorePreviewMetaChips(
                                feedItem = feedItem,
                                detail = detail,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }

                    ExplorePreviewSellerRow(
                        feedItem = feedItem,
                        detail = detail,
                        modifier = Modifier.padding(top = 8.dp),
                    )

                    ExplorePreviewSocialRow(
                        likeCount = detail?.likeCount ?: feedItem.likeCount,
                        saveCount = detail?.saveCount ?: feedItem.saveCount,
                        viewCount = detail?.viewCount ?: 0,
                        isLiked = detail?.isLiked ?: feedItem.isLiked,
                        isSaved = detail?.isSaved ?: feedItem.isSaved,
                        modifier = Modifier.padding(top = 6.dp),
                    )

                    ExplorePreviewDescriptionBlock(
                        description = detail?.description,
                        isLoading = isDetailLoading,
                    )

                    ExplorePreviewShippingHint(detail = detail)

                    Text(
                        text = stringResource(R.string.explore_preview_trust_line),
                        style = MaterialTheme.typography.labelSmall,
                        color = FashColors.Primary.copy(alpha = 0.88f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp),
                    )

                    ExplorePreviewDetailTeaser(
                        onClick = onViewDetail,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = showScrollHint && canScrollFurther,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = fadeIn(tween(280)),
                    exit = fadeOut(tween(220)),
                ) {
                    ExplorePreviewScrollMoreHint(
                        modifier = Modifier.fillMaxWidth(),
                        surfaceColor = scheme.surface,
                    )
                }
            }

            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.35f))

            ExplorePreviewActionBar(
                isLiked = detail?.isLiked ?: feedItem.isLiked,
                isSaved = detail?.isSaved ?: feedItem.isSaved,
                buyNowEnabled = buyNowEnabled,
                hasExistingConversation = hasExistingConversation,
                onLike = {
                    if (isGuestMode) onRequestLogin(GuestLoginReason.Like) else onLike()
                },
                onSave = {
                    if (isGuestMode) onRequestLogin(GuestLoginReason.Saved) else onSave()
                },
                onViewDetail = onViewDetail,
                onMessageSeller = onMessageSeller,
            )
        }
        }
    }
}

@Composable
private fun ExplorePreviewScrollMoreHint(
    surfaceColor: Color,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "previewScrollHint")
    val bounceY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(720, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "previewScrollHintBounce",
    )
    Box(
        modifier = modifier.height(44.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            surfaceColor.copy(alpha = 0f),
                            surfaceColor.copy(alpha = 0.78f),
                            surfaceColor,
                        ),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .padding(bottom = 4.dp)
                .clip(RoundedCornerShape(FashTheme.spacing.radiusPill))
                .background(FashColors.Primary.copy(alpha = 0.1f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(R.string.explore_preview_scroll_hint_cd),
                modifier = Modifier
                    .size(14.dp)
                    .offset(y = bounceY.dp),
                tint = FashColors.Primary,
            )
            Text(
                text = stringResource(R.string.explore_preview_scroll_hint),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = FashColors.Primary,
            )
        }
    }
}

@Composable
private fun ExplorePreviewDetailTeaser(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(FashTheme.spacing.radiusSoftMin),
        color = scheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            FashColors.Primary.copy(alpha = 0.28f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.explore_preview_detail_nudge),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.explore_preview_detail_nudge_sub),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = stringResource(R.string.explore_preview_detail_nudge_cta),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = FashColors.Primary,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = FashColors.Primary,
                )
            }
        }
    }
}

@Composable
private fun ExplorePreviewImageThumb(
    imageUrls: List<String>,
    title: String,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(scheme.surfaceContainerHighest),
    ) {
        when {
            imageUrls.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.no_image),
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
            imageUrls.size == 1 -> {
                FashAsyncImage(
                    model = imageUrls.first(),
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            else -> {
                val pagerState = rememberPagerState(pageCount = { imageUrls.size })
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    FashAsyncImage(
                        model = imageUrls[page],
                        contentDescription = stringResource(
                            R.string.explore_preview_image_pager_cd,
                            page + 1,
                            imageUrls.size,
                        ),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = scheme.scrim.copy(alpha = 0.55f),
                ) {
                    Text(
                        text = stringResource(
                            R.string.explore_preview_image_page,
                            pagerState.currentPage + 1,
                            imageUrls.size,
                        ),
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 9.sp,
                        ),
                        color = scheme.onPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExplorePreviewStatusBanner(isSold: Boolean) {
    val bg = if (isSold) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        FashColors.Primary.copy(alpha = 0.12f)
    }
    val fg = if (isSold) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        FashColors.Primary
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = bg,
    ) {
        Text(
            text = stringResource(
                if (isSold) R.string.product_listing_sold_bar else R.string.product_reserved_other,
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = fg,
        )
    }
}

@Composable
private fun ExplorePreviewPriceRow(priceVnd: Long, listPriceVnd: Long?) {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = formatListingPriceVnd(priceVnd),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            ),
            color = FashColors.Primary,
        )
        listPriceVnd?.takeIf { it > priceVnd }?.let { original ->
            Text(
                text = formatListingPriceVnd(original),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                textDecoration = TextDecoration.LineThrough,
            )
        }
    }
}

@Composable
private fun ExplorePreviewMetaChips(
    feedItem: ListingFeedItem,
    detail: ListingDetail?,
    modifier: Modifier = Modifier,
) {
    val chips = buildList {
        previewConditionLabel(detail?.condition ?: feedItem.condition)?.let { add(it) }
        (detail?.size ?: feedItem.size)?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
        (detail?.brand ?: feedItem.brand)?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
        (detail?.category ?: feedItem.categoryName)?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
        detail?.aestheticTags?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
            ?: feedItem.listingAestheticTag?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
    }
    if (chips.isEmpty()) return
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        chips.forEach { label ->
            ExplorePreviewChip(text = label)
        }
    }
}

@Composable
private fun ExplorePreviewChip(text: String) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(FashTheme.spacing.radiusPill),
        color = scheme.surfaceContainerHighest,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            scheme.outlineVariant.copy(alpha = 0.35f),
        ),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
            ),
            color = scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ExplorePreviewSellerRow(
    feedItem: ListingFeedItem,
    detail: ListingDetail?,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val avatarUrl = resolvePreviewSellerAvatarUrl(feedItem, detail)
    val displayName = detail?.sellerDisplayName?.trim()?.takeIf { it.isNotEmpty() }
    val username = detail?.sellerUsername ?: feedItem.sellerUsername
        ?: stringResource(R.string.explore_preview_seller_username_fallback)
    val listingCount = detail?.sellerListingCount
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FashAvatarCircle(
            imageUrl = avatarUrl,
            contentDescription = null,
            size = PreviewAvatarSize,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName ?: stringResource(R.string.explore_preview_seller_at_username, username),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildString {
                    append(stringResource(R.string.explore_preview_seller_at_username, username))
                    listingCount?.takeIf { it >= 0 }?.let {
                        append(stringResource(R.string.explore_preview_inline_separator))
                        append(stringResource(R.string.product_seller_products_count, it))
                    }
                },
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ExplorePreviewSocialRow(
    likeCount: Int,
    saveCount: Int,
    viewCount: Int,
    isLiked: Boolean,
    isSaved: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExplorePreviewStat(
            icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            value = formatListingEngagementShort(likeCount),
            tint = if (isLiked) FashColors.Primary else scheme.onSurfaceVariant,
        )
        ExplorePreviewStat(
            icon = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
            value = formatListingEngagementShort(saveCount),
            tint = if (isSaved) FashColors.Primary else scheme.onSurfaceVariant,
        )
        if (viewCount > 0) {
            ExplorePreviewStat(
                icon = Icons.Default.Visibility,
                value = formatListingEngagementShort(viewCount),
                tint = scheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ExplorePreviewStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    tint: androidx.compose.ui.graphics.Color,
) {
    if (value.isEmpty()) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(13.dp),
            tint = tint,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ExplorePreviewDescriptionBlock(description: String?, isLoading: Boolean) {
    val scheme = MaterialTheme.colorScheme
    when {
        isLoading && description.isNullOrBlank() -> {
            Row(
                modifier = Modifier.padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                    color = FashColors.Primary,
                )
                Text(
                    text = stringResource(R.string.explore_preview_loading),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        !description.isNullOrBlank() -> {
            Text(
                text = description.trim(),
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.labelSmall.copy(lineHeight = 14.sp),
                color = scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ExplorePreviewShippingHint(detail: ListingDetail?) {
    val d = detail ?: return
    val fee = d.estimatedShippingVnd?.takeIf { it > 0L } ?: return
    val region = d.shippingAddress?.city?.trim()?.takeIf { it.isNotEmpty() }
        ?: d.shippingAddress?.region?.trim()?.takeIf { it.isNotEmpty() }
        ?: d.countryName?.trim()?.takeIf { it.isNotEmpty() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.LocalShipping,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = FashColors.Primary,
        )
        Text(
            text = buildString {
                append(stringResource(R.string.product_shipping_estimate, formatListingPriceVnd(fee)))
                region?.let {
                    append(stringResource(R.string.explore_preview_inline_separator))
                    append(stringResource(R.string.product_ship_from_upper, it))
                }
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ExplorePreviewActionBar(
    isLiked: Boolean,
    isSaved: Boolean,
    buyNowEnabled: Boolean,
    hasExistingConversation: Boolean,
    onLike: () -> Unit,
    onSave: () -> Unit,
    onViewDetail: () -> Unit,
    onMessageSeller: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = PreviewEdgeStart,
                end = PreviewEdgeEnd,
                top = 6.dp,
                bottom = 8.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onLike,
                modifier = Modifier
                    .size(PreviewActionIconButtonSize)
                    .border(1.dp, scheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = stringResource(R.string.like),
                    modifier = Modifier.size(18.dp),
                    tint = if (isLiked) FashColors.Primary else scheme.onSurface,
                )
            }
            IconButton(
                onClick = onSave,
                modifier = Modifier
                    .size(PreviewActionIconButtonSize)
                    .border(1.dp, scheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = stringResource(R.string.save),
                    modifier = Modifier.size(18.dp),
                    tint = if (isSaved) FashColors.Primary else scheme.onSurface,
                )
            }
            Button(
                onClick = onViewDetail,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = PreviewActionIconButtonSize),
                contentPadding = PaddingValues(horizontal = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FashColors.Primary,
                    contentColor = scheme.onPrimary,
                ),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.explore_preview_view_detail),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        OutlinedButton(
            onClick = onMessageSeller,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 34.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, FashColors.Primary.copy(alpha = 0.45f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = FashColors.Primary),
            shape = RoundedCornerShape(10.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Message,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(
                    when {
                        hasExistingConversation -> R.string.notification_action_open_chat
                        buyNowEnabled -> R.string.explore_preview_message_seller
                        else -> R.string.product_chat
                    },
                ),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

private fun resolvePreviewSellerAvatarUrl(
    feedItem: ListingFeedItem,
    detail: ListingDetail?,
): String? {
    val raw = detail?.sellerAvatarUrl?.trim()?.takeIf { it.isNotEmpty() }
        ?: feedItem.sellerAvatarUrl?.trim()?.takeIf { it.isNotEmpty() }
        ?: return null
    return resolveProfileImageUrlOrNull(raw)
}

@Composable
private fun previewConditionLabel(raw: String): String? {
    val cleaned = raw.trim()
    if (cleaned.isEmpty()) return null
    val v = cleaned.lowercase(Locale.ROOT).replace(' ', '_')
    return when (v) {
        "new" -> stringResource(R.string.condition_new)
        "like_new", "like-new" -> stringResource(R.string.condition_like_new)
        "good" -> stringResource(R.string.condition_good)
        "fair" -> stringResource(R.string.condition_fair)
        else -> cleaned
    }
}

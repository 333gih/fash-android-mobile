package com.pc.fash_android_mobile.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.search.TrendingQueryItem
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

private const val ExploreSearchAdHeightFraction = 0.2f
private val ExploreSearchAdMinHeight = 72.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExploreSearchOverlay(
    modifier: Modifier = Modifier,
    viewModel: ExploreViewModel,
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val explorePrimarySection by viewModel.primarySection.collectAsState()
    val recent by viewModel.searchOverlayRecentQueries.collectAsState()
    val trendingQueries by viewModel.searchOverlayTrendingQueries.collectAsState()
    val trendingTags by viewModel.searchOverlayTrendingTags.collectAsState()
    val overlayLoading by viewModel.searchOverlayLoading.collectAsState()
    val suggestions by viewModel.autocompleteSuggestions.collectAsState()
    val autocompleteLoading by viewModel.autocompleteLoading.collectAsState()

    val trimmed = searchQuery.trim()
    val typing = trimmed.isNotEmpty()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val adHeight = (maxHeight * ExploreSearchAdHeightFraction).coerceAtLeast(ExploreSearchAdMinHeight)
            val listHeight = (maxHeight - adHeight).coerceAtLeast(0.dp)

            Column(Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .height(listHeight)
                        .fillMaxWidth(),
                ) {
                    if (
                        overlayLoading &&
                        !typing &&
                        recent.isEmpty() &&
                        trendingQueries.isEmpty() &&
                        trendingTags.isEmpty()
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = FashColors.Primary)
                        }
                    } else if (typing) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 12.dp),
                        ) {
                            item {
                                ExploreSearchSectionTitle(
                                    text = when (explorePrimarySection) {
                                        ExplorePrimarySection.Listings ->
                                            stringResource(R.string.explore_search_suggestions_title_listings)
                                        ExplorePrimarySection.Sellers ->
                                            stringResource(R.string.explore_search_suggestions_title_sellers)
                                    },
                                )
                            }
                            if (autocompleteLoading && suggestions.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(28.dp),
                                            color = FashColors.Primary,
                                            strokeWidth = 2.dp,
                                        )
                                    }
                                }
                            } else if (suggestions.isEmpty()) {
                                item {
                                    Text(
                                        text = stringResource(R.string.explore_search_no_suggestions),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(
                                            horizontal = FashTheme.spacing.editorialStart,
                                            vertical = 8.dp,
                                        ),
                                    )
                                }
                            } else {
                                items(suggestions, key = { it }) { title ->
                                    ExploreSearchSuggestionRow(
                                        title = title,
                                        onClick = { viewModel.selectSearchSuggestionAndSubmit(title) },
                                    )
                                }
                            }
                        }
                    } else {
                        val idleEmpty =
                            !overlayLoading &&
                                recent.isEmpty() &&
                                trendingQueries.isEmpty() &&
                                trendingTags.isEmpty()

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 12.dp),
                        ) {
                            if (idleEmpty) {
                                item {
                                    Text(
                                        text = stringResource(R.string.explore_search_idle_empty),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(
                                            horizontal = FashTheme.spacing.editorialStart,
                                            vertical = 16.dp,
                                        ),
                                    )
                                }
                            }
                            if (recent.isNotEmpty()) {
                                item {
                                    ExploreSearchSectionTitle(
                                        text = stringResource(R.string.explore_search_recent_title),
                                    )
                                }
                                items(recent, key = { "r:$it" }) { q ->
                                    ExploreSearchSuggestionRow(
                                        title = q,
                                        onClick = { viewModel.selectSearchSuggestionAndSubmit(q) },
                                    )
                                }
                            }
                            if (trendingQueries.isNotEmpty()) {
                                item {
                                    ExploreSearchSectionTitle(
                                        text = stringResource(R.string.explore_search_trending_queries_title),
                                    )
                                }
                                items(trendingQueries, key = { "t:${it.query}" }) { row ->
                                    ExploreSearchTrendingQueryRow(
                                        item = row,
                                        onClick = {
                                            viewModel.selectSearchSuggestionAndSubmit(row.query)
                                        },
                                    )
                                }
                            }
                            if (trendingTags.isNotEmpty()) {
                                item {
                                    ExploreSearchSectionTitle(
                                        text = stringResource(R.string.explore_search_trending_tags_title),
                                    )
                                }
                                item {
                                    FlowRow(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                start = FashTheme.spacing.editorialStart,
                                                end = FashTheme.spacing.editorialEnd,
                                                bottom = 8.dp,
                                            ),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        trendingTags.forEach { tag ->
                                            val cd = stringResource(R.string.explore_search_tag_cd, tag)
                                            Text(
                                                text = tag,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(FashTheme.spacing.radiusPill))
                                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                                    .clickable {
                                                        viewModel.selectSearchOverlayTrendingTag(tag)
                                                    }
                                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                                    .semantics { contentDescription = cd },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                ExploreSearchAdPanel(
                    modifier = Modifier
                        .height(adHeight)
                        .fillMaxWidth(),
                    onCloseOverlay = { viewModel.setSearchBarExpanded(false) },
                )
            }
        }
    }
}

@Composable
private fun ExploreSearchSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(
            start = FashTheme.spacing.editorialStart,
            end = FashTheme.spacing.editorialEnd,
            top = 12.dp,
            bottom = 8.dp,
        ),
    )
}

@Composable
private fun ExploreSearchSuggestionRow(
    title: String,
    onClick: () -> Unit,
) {
    val cd = stringResource(R.string.explore_search_row_cd, title)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = FashTheme.spacing.editorialStart,
                vertical = 12.dp,
            )
            .semantics(mergeDescendants = true) { contentDescription = cd },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ExploreSearchTrendingQueryRow(
    item: TrendingQueryItem,
    onClick: () -> Unit,
) {
    val cd = stringResource(R.string.explore_search_row_cd, item.query)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = FashTheme.spacing.editorialStart,
                vertical = 12.dp,
            )
            .semantics(mergeDescendants = true) { contentDescription = cd },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = item.query,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (item.count > 0) {
            Text(
                text = item.count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun ExploreSearchAdPanel(
    modifier: Modifier = Modifier,
    onCloseOverlay: () -> Unit,
) {
    val shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = shape,
        color = scheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = FashTheme.spacing.editorialStart, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.chat_inbox_ad_title),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.chat_inbox_ad_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            OutlinedButton(
                onClick = onCloseOverlay,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FashColors.Primary),
            ) {
                Text(
                    text = stringResource(R.string.chat_inbox_ad_cta),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}

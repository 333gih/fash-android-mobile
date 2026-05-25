package com.pc.fash_android_mobile.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.home.HomeEditorialPostStub
import com.pc.fash_android_mobile.ui.components.FashAsyncImage
import com.pc.fash_android_mobile.ui.components.FashEmptyState
import com.pc.fash_android_mobile.ui.feed.resolveListingImageUrl
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeEditorialListScreen(
    onBack: () -> Unit,
    onPostClick: (HomeEditorialPostStub) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeEditorialListViewModel = viewModel(),
) {
    LaunchedEffect(Unit) { viewModel.loadInitial() }
    val loading by viewModel.loading.collectAsState()
    val loadingMore by viewModel.loadingMore.collectAsState()
    val error by viewModel.error.collectAsState()
    val posts by viewModel.posts.collectAsState()
    val spacing = FashTheme.spacing
    val listState = rememberLazyListState()

    LaunchedEffect(listState, posts.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .collect { last ->
                if (posts.isNotEmpty() && last >= posts.size - 3) viewModel.loadMore()
            }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_editorial_list_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        when {
            loading && posts.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FashColors.Primary)
                }
            }
            error && posts.isEmpty() -> {
                FashEmptyState(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    icon = Icons.Outlined.ErrorOutline,
                    title = stringResource(R.string.feed_load_error),
                    subtitle = "",
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    state = listState,
                    contentPadding = PaddingValues(
                        horizontal = spacing.editorialStart,
                        vertical = spacing.spacing3,
                    ),
                    verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.home_editorial_list_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = spacing.spacing2),
                        )
                    }
                    items(posts, key = { it.id.ifBlank { it.slug } }) { post ->
                        EditorialGuideListCard(post = post, onClick = { onPostClick(post) })
                    }
                    if (loadingMore) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.height(28.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorialGuideListCard(
    post: HomeEditorialPostStub,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val spacing = FashTheme.spacing
    val cover = post.coverImageUrl?.let { resolveListingImageUrl(it) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(spacing.radiusCard))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(spacing.radiusCard),
        color = scheme.surfaceContainerLow,
    ) {
        Column(Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(scheme.surfaceVariant),
            ) {
                if (cover != null) {
                    FashAsyncImage(
                        model = cover,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Column(
                modifier = Modifier.padding(horizontal = spacing.spacing3, vertical = spacing.spacing2),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = post.title.ifBlank { post.slug },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (post.summary.isNotBlank()) {
                    Text(
                        text = post.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = stringResource(R.string.home_editorial_view_detail),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = FashColors.Primary,
                )
            }
        }
    }
}

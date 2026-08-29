package com.pc.fash_android_mobile.ui.outfit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Checkroom
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.recommendation.OutfitSetCard
import com.pc.fash_android_mobile.data.recommendation.RecommendationRepository
import com.pc.fash_android_mobile.ui.components.FashAsyncImage
import com.pc.fash_android_mobile.ui.components.FashEmptyState
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyOutfitDropListScreen(
    initialSets: List<OutfitSetCard>,
    repository: RecommendationRepository,
    onBack: () -> Unit,
    onSetClick: (OutfitSetCard) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sets by remember(initialSets) { mutableStateOf(initialSets) }
    var loading by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf(false) }
    val spacing = FashTheme.spacing

    LaunchedEffect(Unit) {
        loading = true
        loadError = false
        repository.fetchDailyOutfitDrop(limit = 24)
            .onSuccess { fetched ->
                if (fetched.isNotEmpty()) sets = fetched
            }
            .onFailure {
                if (sets.isEmpty()) loadError = true
            }
        loading = false
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_section_daily_outfit_drop_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { inner ->
        when {
            loading && sets.isEmpty() -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 48.dp),
                    color = FashColors.Primary,
                )
            }
            loadError && sets.isEmpty() -> {
                FashEmptyState(
                    icon = Icons.Outlined.Checkroom,
                    title = stringResource(R.string.feed_load_error),
                    subtitle = "",
                    modifier = Modifier.fillMaxSize(),
                )
            }
            sets.isEmpty() -> {
                FashEmptyState(
                    icon = Icons.Outlined.Checkroom,
                    title = stringResource(R.string.outfit_daily_drop_list_empty),
                    subtitle = stringResource(R.string.home_section_daily_outfit_drop_subtitle),
                    modifier = Modifier.fillMaxSize(),
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inner),
                    contentPadding = PaddingValues(
                        start = spacing.editorialStart,
                        end = spacing.editorialEnd,
                        top = spacing.spacing3,
                        bottom = spacing.spacing4,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.home_section_daily_outfit_drop_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(sets, key = { it.id }) { set ->
                        OutfitSetListRow(set = set, onClick = { onSetClick(set) })
                    }
                }
            }
        }
    }
}

@Composable
private fun OutfitSetListRow(
    set: OutfitSetCard,
    onClick: () -> Unit,
) {
    val spacing = FashTheme.spacing
    Surface(
        shape = RoundedCornerShape(spacing.radiusSoftMin),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = set.title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (set.reasonLabel.isNotBlank()) {
                Text(
                    text = set.reasonLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                set.items.take(4).forEach { item ->
                    FashAsyncImage(
                        model = item.coverImageUrl,
                        contentDescription = item.title,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(0.75f)
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
    }
}

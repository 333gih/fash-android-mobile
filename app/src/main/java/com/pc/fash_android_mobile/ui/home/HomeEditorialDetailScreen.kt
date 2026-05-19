package com.pc.fash_android_mobile.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.components.FashAsyncImage
import com.pc.fash_android_mobile.ui.components.FashEmptyState
import com.pc.fash_android_mobile.ui.feed.resolveListingImageUrl
import com.pc.fash_android_mobile.ui.theme.FashTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeEditorialDetailScreen(
    slug: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeEditorialDetailViewModel = viewModel(),
) {
    LaunchedEffect(slug) { viewModel.load(slug) }
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val guide by viewModel.guide.collectAsState()
    val spacing = FashTheme.spacing

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(guide?.title?.take(48).orEmpty() ?: stringResource(R.string.home_section_editorial_title)) },
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
            loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            error || guide == null -> {
                FashEmptyState(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    icon = Icons.Outlined.ErrorOutline,
                    title = stringResource(R.string.feed_load_error),
                    subtitle = "",
                )
            }
            else -> {
                val g = guide!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = spacing.editorialStart, vertical = spacing.spacing3),
                ) {
                    val cover = resolveListingImageUrl(g.coverImageUrl)
                    if (cover != null) {
                        FashAsyncImage(
                            model = cover,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(Modifier.height(spacing.spacing3))
                    }
                    Text(
                        text = g.title,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    )
                    if (g.summary.isNotBlank()) {
                        Spacer(Modifier.height(spacing.spacing2))
                        Text(
                            text = g.summary,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(spacing.spacing3))
                    EditorialMarkdownBody(g.bodyMarkdown)
                }
            }
        }
    }
}

@Composable
private fun EditorialMarkdownBody(markdown: String) {
    val blocks = markdown.split("\n\n").map { it.trim() }.filter { it.isNotEmpty() }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (block in blocks) {
            if (block.startsWith("## ")) {
                Text(
                    text = block.removePrefix("## ").trim(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            } else {
                Text(
                    text = block.replace("**", "").replace("*", ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

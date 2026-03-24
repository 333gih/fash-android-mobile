package com.pc.fash_android_mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.pc.fash_android_mobile.ui.theme.fashShimmer

/**
 * Drop-in replacement for [coil.compose.AsyncImage] that shows an animated shimmer sweep
 * while the image loads and a muted surface fill on error — both aligned with the Fash
 * design system.
 *
 * Pass raw data (URL string, [android.net.Uri], etc.) as [model]; crossfade is handled
 * internally so callers do not need to build an [ImageRequest].
 */
@Composable
fun FashAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(model)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
    ) {
        when (painter.state) {
            is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
            is AsyncImagePainter.State.Error -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            )
            else -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .fashShimmer(),
            )
        }
    }
}

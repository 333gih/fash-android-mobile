package com.pc.fash_android_mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import coil.size.Size
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
    alignment: Alignment = Alignment.Center,
    /** When set, Coil decodes near display size (faster feed scroll, less memory). */
    targetPixelSize: Pair<Int, Int>? = null,
) {
    val requestBuilder = ImageRequest.Builder(LocalContext.current)
        .data(model)
        .crossfade(true)
    if (targetPixelSize != null) {
        val (w, h) = targetPixelSize
        if (w > 0 && h > 0) {
            requestBuilder.size(Size(w, h))
        }
    }
    SubcomposeAsyncImage(
        model = requestBuilder.build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        alignment = alignment,
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

/**
 * Remote or local profile photo: uses Coil with center-crop for URLs; brand default vector otherwise
 * (sharp at all sizes — use [FashDefaultProfileAvatar] when there is no URL; do not pass the old
 * default drawable through Coil).
 */
@Composable
fun FashProfileAvatarImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    if (!imageUrl.isNullOrBlank()) {
        FashAsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
        )
    } else {
        FashDefaultProfileAvatar(
            contentDescription = contentDescription,
            modifier = modifier,
        )
    }
}

/**
 * Circular avatar: loads [imageUrl] when non-blank; otherwise shows the brand default avatar.
 */
@Composable
fun FashAvatarCircle(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(scheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        FashProfileAvatarImage(
            imageUrl = imageUrl,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

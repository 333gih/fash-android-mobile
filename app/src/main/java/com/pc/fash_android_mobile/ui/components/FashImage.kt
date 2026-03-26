package com.pc.fash_android_mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.pc.fash_android_mobile.R
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

/**
 * Circular avatar: loads [imageUrl] when non-blank; otherwise shows a default person icon
 * (and optional [fallbackInitial] letter on the same surface).
 */
@Composable
fun FashAvatarCircle(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    fallbackInitial: Char? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(scheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (!imageUrl.isNullOrBlank()) {
            FashAsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else if (fallbackInitial != null && fallbackInitial.isLetter()) {
            Text(
                text = fallbackInitial.uppercaseChar().toString(),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurfaceVariant,
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = contentDescription
                    ?: stringResource(R.string.avatar_default_cd),
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(size * 0.55f),
            )
        }
    }
}

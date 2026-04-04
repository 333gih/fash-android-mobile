package com.pc.fash_android_mobile.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.pc.fash_android_mobile.R

/**
 * Brand default assets when a user or seller has no [avatarUrl] / [coverImageUrl].
 * Use everywhere profile photos are shown (self profile, seller storefront, orders, chat header, etc.).
 */
object FashDefaultProfileAssets {
    val avatarRes: Int = R.drawable.fash_default_profile_avatar
    val coverRes: Int = R.drawable.fash_default_profile_cover
}

/**
 * Default avatar: vector drawn with [Image] (not Coil) so it stays sharp at 28–80dp.
 * [ContentScale.Fit] + inner inset preserves a thin ring of the parent background (typically
 * [MaterialTheme.colorScheme.surfaceContainerHigh]) so the circle does not look cropped wrong.
 */
@Composable
fun FashDefaultProfileAvatar(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(FashDefaultProfileAssets.avatarRes),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(0.88f),
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center,
        )
    }
}

@Composable
fun FashDefaultProfileCover(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Image(
        painter = painterResource(FashDefaultProfileAssets.coverRes),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop,
    )
}

package com.pc.fash_android_mobile.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.pc.fash_android_mobile.R

/**
 * Brand default assets when a user or seller has no [avatarUrl] / [coverImageUrl].
 * Use everywhere profile photos are shown (self profile, seller storefront, orders, chat header, etc.).
 */
object FashDefaultProfileAssets {
    val coverRes: Int = R.drawable.fash_default_profile_cover
}

/**
 * Default avatar: primary circle with an italic **F** (Compose [Text], not Coil) so it stays sharp at 28–80dp.
 * Inner inset preserves a thin ring of the parent background (typically
 * [MaterialTheme.colorScheme.surfaceContainerHigh]) so the circle does not look cropped wrong.
 */
@Composable
fun FashDefaultProfileAvatar(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val avatarLabel = contentDescription
    Box(
        modifier = modifier.semantics(mergeDescendants = true) {
            avatarLabel?.let { desc -> this.contentDescription = desc }
        },
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize(0.88f)
                .clip(CircleShape)
                .background(scheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            val fontSize = (maxWidth.value * 0.5f).sp
            Text(
                text = "F",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    fontSize = fontSize,
                ),
                color = scheme.onPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
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

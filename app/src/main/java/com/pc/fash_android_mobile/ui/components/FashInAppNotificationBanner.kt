package com.pc.fash_android_mobile.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.ui.theme.FashColors

/**
 * Facebook-style transient banner: slides down from the status bar area, auto-dismiss handled by caller.
 */
@Composable
fun FashInAppNotificationBanner(
    visible: Boolean,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onDismissClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        enter = slideInVertically(
            animationSpec = tween(durationMillis = 280),
            initialOffsetY = { -it },
        ),
        exit = slideOutVertically(
            animationSpec = tween(durationMillis = 220),
            targetOffsetY = { -it },
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .shadow(8.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val t = title.trim()
                    if (t.isNotEmpty()) {
                        Text(
                            text = t,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    val b = body.trim().ifBlank { " " }
                    Text(
                        text = b,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.widthIn(min = 8.dp))
                Text(
                    text = "✕",
                    style = MaterialTheme.typography.labelLarge,
                    color = FashColors.Primary,
                    modifier = Modifier
                        .clickable(onClick = onDismissClick),
                )
            }
        }
    }
}

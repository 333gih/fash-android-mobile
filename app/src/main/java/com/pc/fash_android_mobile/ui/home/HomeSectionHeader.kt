package com.pc.fash_android_mobile.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

/**
 * Home-only section chrome: slightly smaller type than [com.pc.fash_android_mobile.ui.feed.FeedSectionHeader]
 * so more content fits above the fold on typical phones.
 */
@Composable
fun HomeSectionHeader(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
) {
    if (title.isNullOrBlank() && subtitle.isNullOrBlank()) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = FashTheme.spacing.editorialStart,
                end = FashTheme.spacing.editorialEnd,
                top = 2.dp,
                bottom = 6.dp,
            ),
    ) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = FashColors.Primary,
            )
        }
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = if (title.isNullOrBlank()) 0.dp else 2.dp),
            )
        }
    }
}

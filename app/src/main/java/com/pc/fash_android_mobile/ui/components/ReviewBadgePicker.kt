package com.pc.fash_android_mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.common.ReviewBadgeDto
import com.pc.fash_android_mobile.ui.theme.FashColors
import java.util.Locale

/** Maps badge selection to backend 1–5 rating (all catalog badges are positive → always 5). */
fun deriveReviewRatingFromBadgeCount(badgeCount: Int): Int = 5

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReviewBadgePicker(
    badges: List<ReviewBadgeDto>,
    selectedIds: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    isLoading: Boolean,
    loadFailed: Boolean,
    modifier: Modifier = Modifier,
    maxSelection: Int = 3,
    enabled: Boolean = true,
) {
    val isVi = remember { Locale.getDefault().language.startsWith("vi") }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.badge_review_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.CenterHorizontally),
                    strokeWidth = 2.dp,
                    color = FashColors.Primary,
                )
            }
            loadFailed && badges.isEmpty() -> {
                Text(
                    text = stringResource(R.string.badge_review_load_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            badges.isEmpty() -> {
                Text(
                    text = stringResource(R.string.badge_review_load_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    badges.forEach { badge ->
                        val selected = selectedIds.contains(badge.id)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                if (!enabled) return@FilterChip
                                onSelectionChange(
                                    if (selected) {
                                        selectedIds - badge.id
                                    } else if (selectedIds.size < maxSelection) {
                                        selectedIds + badge.id
                                    } else {
                                        selectedIds
                                    },
                                )
                            },
                            label = {
                                Text(
                                    buildString {
                                        if (badge.emoji.isNotBlank()) {
                                            append(badge.emoji)
                                            append(' ')
                                        }
                                        append(badge.displayName(isVi))
                                    },
                                )
                            },
                            enabled = enabled,
                        )
                    }
                }
                if (selectedIds.isEmpty()) {
                    Text(
                        text = stringResource(R.string.badge_review_required),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = FashColors.Primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

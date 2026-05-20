package com.pc.fash_android_mobile.ui.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.common.CommonAestheticTagDto
import com.pc.fash_android_mobile.data.search.EXPLORE_STYLE_QUICK_CHIP_LIMIT
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme
import java.text.Normalizer
import java.util.Locale

/**
 * One-tap aesthetic filters at the top of Explore listings.
 * Chips come from core trending tag names resolved against the common-service catalog (limited count).
 */
@Composable
fun ExploreStyleQuickChipsRow(
    quickTags: List<CommonAestheticTagDto>,
    selectedIds: Set<String>,
    onTagToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (quickTags.isEmpty()) return
    val spacing = FashTheme.spacing
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.explore_style_chips_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
        ) {
            items(quickTags, key = { it.id }) { tag ->
                val selected = selectedIds.contains(tag.id)
                FilterChip(
                    selected = selected,
                    onClick = { onTagToggle(tag.id) },
                    label = {
                        Text(
                            text = tag.labelForChip(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    shape = FashTheme.spacing.chipShape(),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FashColors.Primary.copy(alpha = 0.14f),
                        selectedLabelColor = FashColors.Primary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}

private fun CommonAestheticTagDto.labelForChip(): String =
    displayName.takeIf { it.isNotBlank() } ?: name.takeIf { it.isNotBlank() } ?: id

/**
 * Maps core trending tag names to catalog rows (id required for filter sheet parity).
 * Order follows trending rank; skips unknown names.
 */
internal fun resolveStyleQuickTagsFromTrending(
    trendingNames: List<String>,
    catalog: List<CommonAestheticTagDto>,
    limit: Int = EXPLORE_STYLE_QUICK_CHIP_LIMIT,
): List<CommonAestheticTagDto> {
    if (trendingNames.isEmpty() || catalog.isEmpty()) return emptyList()
    val cap = limit.coerceIn(1, EXPLORE_STYLE_QUICK_CHIP_LIMIT)
    val byNormalizedName = catalog.associateBy { normalizeTagLabel(it.labelForChip()) }
    val picked = mutableListOf<CommonAestheticTagDto>()
    val usedIds = mutableSetOf<String>()
    for (raw in trendingNames) {
        val key = normalizeTagLabel(raw)
        if (key.isEmpty()) continue
        val match = catalog.firstOrNull { tag ->
            normalizeTagLabel(tag.labelForChip()) == key ||
                normalizeTagLabel(tag.name) == key ||
                normalizeTagLabel(tag.displayName) == key
        } ?: byNormalizedName[key]
        if (match != null && usedIds.add(match.id)) {
            picked.add(match)
        }
        if (picked.size >= cap) break
    }
    return picked
}

private fun normalizeTagLabel(raw: String): String {
    val lower = raw.trim().lowercase(Locale.getDefault())
    val nfd = Normalizer.normalize(lower, Normalizer.Form.NFD)
    return nfd.replace(Regex("\\p{M}+"), "")
}

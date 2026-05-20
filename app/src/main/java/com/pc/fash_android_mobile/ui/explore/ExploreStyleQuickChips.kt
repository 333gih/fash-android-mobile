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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.common.CommonAestheticTagDto
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme
import java.text.Normalizer
import java.util.Locale

private val StyleQuickPickKeywords = listOf(
    "streetwear",
    "vintage",
    "y2k",
    "minimal",
    "local brand",
    "korean",
    "thrift",
)

/**
 * One-tap aesthetic filters at the top of Explore listings (catalog-driven; no account required).
 */
@Composable
fun ExploreStyleQuickChipsRow(
    catalog: List<CommonAestheticTagDto>,
    selectedIds: Set<String>,
    onTagToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val quickTags = remember(catalog) { pickStyleQuickTags(catalog) }
    if (quickTags.isEmpty()) return
    val spacing = FashTheme.spacing
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.explore_style_chips_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(
                    start = spacing.editorialStart,
                    end = spacing.editorialEnd,
                    bottom = 6.dp,
                ),
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = spacing.editorialStart,
                end = spacing.editorialEnd,
                bottom = 4.dp,
            ),
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

internal fun pickStyleQuickTags(catalog: List<CommonAestheticTagDto>): List<CommonAestheticTagDto> {
    if (catalog.isEmpty()) return emptyList()
    val normalizedCatalog = catalog.map { tag ->
        tag to normalizeTagLabel(tag.labelForChip())
    }
    val picked = mutableListOf<CommonAestheticTagDto>()
    val usedIds = mutableSetOf<String>()
    for (keyword in StyleQuickPickKeywords) {
        val match = normalizedCatalog.firstOrNull { (_, label) ->
            label.contains(keyword)
        }?.first
        if (match != null && usedIds.add(match.id)) {
            picked.add(match)
        }
        if (picked.size >= 6) return picked
    }
    for ((tag, _) in normalizedCatalog) {
        if (usedIds.add(tag.id)) {
            picked.add(tag)
        }
        if (picked.size >= 6) break
    }
    return picked
}

private fun normalizeTagLabel(raw: String): String {
    val lower = raw.trim().lowercase(Locale.getDefault())
    val nfd = Normalizer.normalize(lower, Normalizer.Form.NFD)
    return nfd.replace(Regex("\\p{M}+"), "")
}

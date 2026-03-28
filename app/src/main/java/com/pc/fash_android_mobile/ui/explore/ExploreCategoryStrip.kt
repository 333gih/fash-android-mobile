package com.pc.fash_android_mobile.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.listing.Category
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

/**
 * Shop-by-category chips (Depop-style) — filters [ExploreViewModel] `/search/listings` via `category_id`.
 */
@Composable
fun ExploreCategoryStrip(
    categories: List<Category>,
    selectedCategoryId: String?,
    onSelectCategory: (categoryId: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (categories.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.explore_category_section_title),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(
                start = FashTheme.spacing.editorialStart,
                end = FashTheme.spacing.editorialEnd,
                bottom = 8.dp,
            ),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = FashTheme.spacing.editorialStart, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val allSelected = selectedCategoryId.isNullOrBlank()
            CategoryChip(
                label = stringResource(R.string.explore_category_all),
                selected = allSelected,
                onClick = { onSelectCategory(null) },
            )
            categories.forEach { cat ->
                val id = cat.id.trim()
                if (id.isEmpty()) return@forEach
                CategoryChip(
                    label = cat.name.ifBlank { cat.slug },
                    selected = !allSelected && id == selectedCategoryId,
                    onClick = { onSelectCategory(id) },
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(FashTheme.spacing.radiusPill)
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(shape)
            .background(
                if (selected) FashColors.Primary
                else MaterialTheme.colorScheme.surfaceContainerHigh,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

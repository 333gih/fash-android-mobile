package com.pc.fash_android_mobile.ui.explore

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.listing.Category
import com.pc.fash_android_mobile.ui.theme.FashTheme
import java.util.Locale

/**
 * Compact category control: shows current choice and opens a searchable list (no long horizontal chip strip).
 */
@Composable
fun ExploreCategoryFilterRow(
    categories: List<Category>,
    selectedCategoryId: String?,
    onOpenPicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (categories.isEmpty()) return
    val edge = FashTheme.spacing.editorialStart
    val selectedLabel = when (val id = selectedCategoryId?.trim()) {
        null, "" -> stringResource(R.string.explore_category_all)
        else -> categories.find { it.id == id }?.let { c ->
            c.name.ifBlank { c.slug }.trim().takeIf { it.isNotEmpty() }
        } ?: stringResource(R.string.explore_category_all)
    }
    val cd = stringResource(R.string.explore_filter_category_summary_cd, selectedLabel)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = FashTheme.spacing.spacing2),
    ) {
        ExploreFilterSectionLabel(text = stringResource(R.string.explore_category_section_title))
        FilterSelectionSummaryCard(
            summary = selectedLabel,
            onClick = onOpenPicker,
            contentDescription = cd,
            modifier = Modifier.padding(horizontal = edge),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreCategoryPickerSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    categories: List<Category>,
    selectedCategoryId: String?,
    onSelectCategory: (String?) -> Unit,
) {
    if (!visible || categories.isEmpty()) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scheme = MaterialTheme.colorScheme
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val q = searchQuery.trim().lowercase(Locale.getDefault())
    val filtered = remember(categories, q) {
        if (q.isEmpty()) categories
        else categories.filter { c ->
            c.name.lowercase(Locale.getDefault()).contains(q) ||
                c.slug.lowercase(Locale.getDefault()).contains(q)
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = scheme.surface,
        contentColor = scheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = FashTheme.spacing.spacing4),
        ) {
            ExploreFilterPickerHeader(
                title = stringResource(R.string.explore_category_section_title),
                onDone = onDismiss,
            )
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FashTheme.spacing.editorialStart, vertical = 4.dp),
                placeholder = {
                    Text(
                        text = stringResource(R.string.explore_filter_category_search_placeholder),
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = RoundedCornerShape(FashTheme.spacing.radiusSoftMin),
                colors = exploreFilterSearchFieldColors(),
            )
            ExploreFilterHorizontalDivider()
            ExploreFilterListSurface {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp),
                ) {
                    item {
                        ExploreFilterSingleSelectRow(
                            label = stringResource(R.string.explore_category_all),
                            selected = selectedCategoryId.isNullOrBlank(),
                            onClick = {
                                onSelectCategory(null)
                                onDismiss()
                            },
                        )
                    }
                    items(filtered, key = { it.id }) { cat ->
                        val id = cat.id.trim()
                        if (id.isEmpty()) return@items
                        ExploreFilterSingleSelectRow(
                            label = cat.name.ifBlank { cat.slug },
                            selected = id == selectedCategoryId,
                            onClick = {
                                onSelectCategory(id)
                                onDismiss()
                            },
                        )
                    }
                }
            }
        }
    }
}

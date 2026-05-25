package com.pc.fash_android_mobile.ui.listing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.common.CommonAestheticTagDto
import com.pc.fash_android_mobile.data.common.displayLabel
import com.pc.fash_android_mobile.data.common.CommonBrandDto
import com.pc.fash_android_mobile.data.common.CommonCountryDto
import com.pc.fash_android_mobile.data.locale.AppLocale
import com.pc.fash_android_mobile.ui.post.PostListingSearchField
import com.pc.fash_android_mobile.ui.post.PostSelectableListRow
import com.pc.fash_android_mobile.ui.post.PostSelectablePill
import com.pc.fash_android_mobile.ui.post.matchesTagQuery
import com.pc.fash_android_mobile.ui.post.postListingOutlinedFieldColors
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditListingBrandDropdown(
    brandName: String,
    brandId: String?,
    brandsFeatured: List<CommonBrandDto>,
    brandsSearch: List<CommonBrandDto>,
    enabled: Boolean,
    onSearchBrands: (String) -> Unit,
    onSelectBrand: (CommonBrandDto?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val brandListScroll = rememberScrollState()

    val list = if (query.isBlank()) brandsFeatured else brandsSearch

    LaunchedEffect(query) {
        delay(280)
        onSearchBrands(query)
    }

    LaunchedEffect(expanded) {
        if (expanded) {
            query = ""
            onSearchBrands("")
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = brandName,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(stringResource(R.string.create_listing_brand_label)) },
            placeholder = {
                Text(stringResource(R.string.edit_listing_brand_dropdown_placeholder))
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor(
                    type = MenuAnchorType.PrimaryNotEditable,
                    enabled = enabled,
                )
                .fillMaxWidth(),
            shape = RoundedCornerShape(FashTheme.spacing.radiusSoftMin),
            colors = postListingOutlinedFieldColors(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 380.dp),
        ) {
            if (brandName.isNotBlank()) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.post_clear_brand)) },
                    onClick = {
                        onSelectBrand(null)
                        expanded = false
                    },
                )
            }
            Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                PostListingSearchField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.post_search_brand)) },
                )
            }
            // LazyColumn is not allowed inside ExposedDropdownMenu (intrinsic measurement / SubcomposeLayout).
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
                    .verticalScroll(brandListScroll)
                    .padding(horizontal = 8.dp),
            ) {
                if (list.isEmpty()) {
                    Text(
                        text = stringResource(R.string.edit_listing_brand_dropdown_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                } else {
                    list.forEach { brand ->
                        PostSelectableListRow(
                            text = brand.name,
                            selected = brandId == brand.id,
                            onClick = {
                                onSelectBrand(brand)
                                expanded = false
                            },
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditListingCountryDropdown(
    countryName: String,
    countryIso2: String,
    countryId: String?,
    countries: List<CommonCountryDto>,
    enabled: Boolean,
    onSearchCountries: (String) -> Unit,
    onSelectCountry: (CommonCountryDto?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val listScroll = rememberScrollState()

    val display = when {
        countryName.isNotBlank() && countryIso2.isNotBlank() ->
            "$countryName (${countryIso2.uppercase()})"
        countryName.isNotBlank() -> countryName
        else -> ""
    }

    LaunchedEffect(query) {
        delay(280)
        onSearchCountries(query)
    }

    LaunchedEffect(expanded) {
        if (expanded) {
            query = ""
            onSearchCountries("")
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(stringResource(R.string.post_step_country)) },
            placeholder = {
                Text(stringResource(R.string.edit_listing_country_dropdown_placeholder))
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor(
                    type = MenuAnchorType.PrimaryNotEditable,
                    enabled = enabled,
                )
                .fillMaxWidth(),
            shape = RoundedCornerShape(FashTheme.spacing.radiusSoftMin),
            colors = postListingOutlinedFieldColors(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 380.dp),
        ) {
            if (display.isNotBlank()) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.edit_listing_clear_country)) },
                    onClick = {
                        onSelectCountry(null)
                        expanded = false
                    },
                )
            }
            Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                PostListingSearchField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.post_search_country)) },
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
                    .verticalScroll(listScroll)
                    .padding(horizontal = 8.dp),
            ) {
                if (countries.isEmpty()) {
                    Text(
                        text = stringResource(R.string.edit_listing_country_dropdown_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                } else {
                    countries.forEach { c ->
                        PostSelectableListRow(
                            text = c.name,
                            selected = countryId == c.id,
                            onClick = {
                                onSelectCountry(c)
                                expanded = false
                            },
                            leadingEmoji = c.emoji,
                            subtitle = c.iso2.uppercase(),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditListingStyleTagsDropdown(
    catalogTags: List<CommonAestheticTagDto>,
    selectedTagIds: Set<String>,
    maxTags: Int,
    enabled: Boolean,
    onToggleTag: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var filterQuery by remember { mutableStateOf("") }
    val tagListScroll = rememberScrollState()
    val isVi = AppLocale.currentTag(androidx.compose.ui.platform.LocalContext.current) != AppLocale.TAG_EN
    val filtered = remember(filterQuery, catalogTags) {
        catalogTags.filter { it.matchesTagQuery(filterQuery) }
    }

    val summary = remember(selectedTagIds, catalogTags, isVi) {
        selectedTagIds.mapNotNull { id ->
            catalogTags.find { it.id == id }?.displayLabel(isVi)
        }
    }
    val fieldText = when {
        summary.isEmpty() -> ""
        else -> summary.joinToString(", ")
    }

    Column(modifier = modifier.fillMaxWidth()) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (enabled) expanded = !expanded },
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = fieldText,
                onValueChange = {},
                readOnly = true,
                enabled = enabled,
                label = { Text(stringResource(R.string.edit_listing_style_tags)) },
                placeholder = {
                    Text(stringResource(R.string.edit_listing_tags_dropdown_placeholder))
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .menuAnchor(
                        type = MenuAnchorType.PrimaryNotEditable,
                        enabled = enabled,
                    )
                    .fillMaxWidth(),
                shape = RoundedCornerShape(FashTheme.spacing.radiusSoftMin),
                colors = postListingOutlinedFieldColors(),
                singleLine = true,
                maxLines = 1,
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 420.dp),
            ) {
                Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    PostListingSearchField(
                        value = filterQuery,
                        onValueChange = { filterQuery = it },
                        label = { Text(stringResource(R.string.post_search_style_tags)) },
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(tagListScroll)
                        .padding(horizontal = 8.dp),
                ) {
                    if (filtered.isEmpty()) {
                        Text(
                            text = stringResource(R.string.post_style_tags_no_matches),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    } else {
                        filtered.forEach { tag ->
                            val selected = tag.id in selectedTagIds
                            StyleTagMenuRow(
                                label = tag.displayLabel(isVi),
                                selected = selected,
                                enabled = enabled,
                                onToggle = { onToggleTag(tag.id) },
                            )
                        }
                    }
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.edit_listing_tags_done)) },
                    onClick = { expanded = false },
                )
            }
        }
        Text(
            text = stringResource(R.string.edit_listing_tags_count_fmt, selectedTagIds.size, maxTags),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )

        if (summary.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                selectedTagIds.forEach { id ->
                    val tag = catalogTags.find { it.id == id } ?: return@forEach
                    PostSelectablePill(
                        text = tag.displayLabel(isVi),
                        selected = true,
                        onClick = {
                            if (enabled) onToggleTag(id)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun StyleTagMenuRow(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = { if (enabled) onToggle() },
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = FashColors.Primary,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

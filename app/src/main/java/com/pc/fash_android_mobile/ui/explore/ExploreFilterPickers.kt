package com.pc.fash_android_mobile.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.address.VnAddressDropdown
import com.pc.fash_android_mobile.data.common.CommonAddressDto
import com.pc.fash_android_mobile.data.common.CommonAestheticTagDto
import com.pc.fash_android_mobile.data.common.displayLabel
import com.pc.fash_android_mobile.data.common.CommonBrandDto
import com.pc.fash_android_mobile.data.common.CommonCountryDto
import com.pc.fash_android_mobile.data.common.CommonServiceRepository
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme
import com.pc.fash_android_mobile.data.locale.AppLocale
import java.util.Locale

// —— Design system (aligned with ExploreFiltersBar + ExploreMarketplaceFilters) ——

/** Search fields in filter pickers — same cursor/container rules as price filters. */
@Composable
internal fun exploreFilterSearchFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = FashColors.Primary.copy(alpha = 0.55f),
    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    cursorColor = FashColors.Primary,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

@Composable
internal fun ExploreFilterSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val edge = FashTheme.spacing.editorialStart
    val edgeEnd = FashTheme.spacing.editorialEnd
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = scheme.onSurface,
        modifier = modifier.padding(start = edge, end = edgeEnd, bottom = 6.dp),
    )
}

@Composable
internal fun ExploreFilterPickerHeader(
    title: String,
    onDone: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart, vertical = FashTheme.spacing.spacing2),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = scheme.onSurface,
        )
        TextButton(onClick = onDone) {
            Text(
                text = stringResource(R.string.explore_filter_sheet_done),
                style = MaterialTheme.typography.labelLarge,
                color = scheme.primary,
            )
        }
    }
}

/** Hairline — matches divider use in [ExploreFilterBottomSheet]. */
@Composable
internal fun ExploreFilterHorizontalDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = FashTheme.spacing.spacing2),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f),
    )
}

/**
 * Inset list panel — soft [surfaceContainerHigh] block without a drawn frame (tonal only).
 */
@Composable
internal fun ExploreFilterListSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart)
            .padding(bottom = FashTheme.spacing.spacing2),
        shape = RoundedCornerShape(FashTheme.spacing.radiusSoftMin),
        color = scheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        content()
    }
}

@Composable
internal fun ExploreFilterSingleSelectRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FashTheme.spacing.radiusSoftMin))
            .background(
                if (selected) scheme.primary.copy(alpha = 0.10f) else Color.Transparent,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = FashTheme.spacing.spacing3, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FashTheme.spacing.spacing2),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = scheme.primary,
                unselectedColor = scheme.outlineVariant,
            ),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun ExploreFilterMultiSelectRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FashTheme.spacing.radiusSoftMin))
            .background(
                if (checked) scheme.primary.copy(alpha = 0.08f) else Color.Transparent,
            )
            .clickable(onClick = onToggle)
            .padding(horizontal = FashTheme.spacing.spacing3, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FashTheme.spacing.spacing2),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = scheme.primary,
                uncheckedColor = scheme.outlineVariant,
                checkmarkColor = scheme.onPrimary,
            ),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

// —— Filter rows & summary card ——

/**
 * Brand filter: one line summary + opens searchable picker (replaces long horizontal chip strip).
 */
@Composable
fun ExploreBrandFilterRow(
    brands: List<CommonBrandDto>,
    selectedBrandId: String?,
    onOpenPicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (brands.isEmpty()) return
    val edge = FashTheme.spacing.editorialStart
    val summary = when (val id = selectedBrandId?.trim()) {
        null, "" -> stringResource(R.string.explore_filter_brand_any)
        else -> brands.find { it.id == id }?.let { b ->
            b.name.ifBlank { b.slug }.trim().takeIf { it.isNotEmpty() }
        } ?: stringResource(R.string.explore_filter_brand_any)
    }
    val cd = stringResource(R.string.explore_filter_brand_summary_cd, summary)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = FashTheme.spacing.spacing2),
    ) {
        ExploreFilterSectionLabel(text = stringResource(R.string.explore_filter_brand_title))
        FilterSelectionSummaryCard(
            summary = summary,
            onClick = onOpenPicker,
            contentDescription = cd,
            modifier = Modifier.padding(horizontal = edge),
        )
    }
}

/**
 * Country of origin (made in): summary + searchable single-select (matches `country_id` + `country_iso2` on search).
 */
@Composable
fun ExploreCountryFilterRow(
    countries: List<CommonCountryDto>,
    selectedCountryId: String?,
    selectedCountryIso2: String?,
    onOpenPicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (countries.isEmpty()) return
    val edge = FashTheme.spacing.editorialStart
    val summary = when {
        selectedCountryId.isNullOrBlank() && selectedCountryIso2.isNullOrBlank() ->
            stringResource(R.string.explore_filter_country_any)
        else -> {
            val byId = selectedCountryId?.let { id -> countries.find { it.id == id } }
            val byIso = if (byId == null && !selectedCountryIso2.isNullOrBlank()) {
                countries.find { it.iso2.equals(selectedCountryIso2, ignoreCase = true) }
            } else {
                null
            }
            (byId ?: byIso)?.name?.trim()?.takeIf { it.isNotEmpty() }
                ?: stringResource(R.string.explore_filter_country_any)
        }
    }
    val cd = stringResource(R.string.explore_filter_country_summary_cd, summary)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = FashTheme.spacing.spacing2),
    ) {
        ExploreFilterSectionLabel(text = stringResource(R.string.explore_filter_country_title))
        FilterSelectionSummaryCard(
            summary = summary,
            onClick = onOpenPicker,
            contentDescription = cd,
            modifier = Modifier.padding(horizontal = edge),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreCountryPickerSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    countries: List<CommonCountryDto>,
    selectedCountryId: String?,
    selectedCountryIso2: String?,
    onSelectCountry: (countryId: String?, iso2: String?) -> Unit,
) {
    if (!visible || countries.isEmpty()) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scheme = MaterialTheme.colorScheme
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val q = searchQuery.trim().lowercase(Locale.getDefault())
    val filtered = remember(countries, q) {
        if (q.isEmpty()) countries
        else countries.filter { c ->
            c.name.lowercase(Locale.getDefault()).contains(q) ||
                c.iso2.lowercase(Locale.getDefault()).contains(q)
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
                title = stringResource(R.string.explore_filter_country_title),
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
                        text = stringResource(R.string.explore_filter_country_search_placeholder),
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
                            label = stringResource(R.string.explore_filter_country_any),
                            selected = selectedCountryId.isNullOrBlank() && selectedCountryIso2.isNullOrBlank(),
                            onClick = {
                                onSelectCountry(null, null)
                                onDismiss()
                            },
                        )
                    }
                    items(filtered, key = { it.id }) { c ->
                        val id = c.id.trim()
                        if (id.isEmpty()) return@items
                        val iso = c.iso2.trim()
                        val selected = id == selectedCountryId ||
                            (!iso.isEmpty() && iso.equals(selectedCountryIso2, ignoreCase = true))
                        ExploreFilterSingleSelectRow(
                            label = c.name.ifBlank { iso },
                            selected = selected,
                            onClick = {
                                onSelectCountry(id, iso)
                                onDismiss()
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Aesthetic tags: summary line + opens multi-select searchable sheet (main filter sheet stays short).
 */
@Composable
fun ExploreAestheticFilterRow(
    catalog: List<CommonAestheticTagDto>,
    selectedIds: Set<String>,
    onOpenPicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val edge = FashTheme.spacing.editorialStart
    val isVi = AppLocale.currentTag(LocalContext.current) != AppLocale.TAG_EN
    val summary = when {
        selectedIds.isEmpty() ->
            stringResource(R.string.explore_filter_aesthetic_none)
        selectedIds.size == 1 -> {
            val id = selectedIds.first()
            catalog.find { it.id == id }?.let { t ->
                t.displayLabel(isVi).trim().takeIf { it.isNotEmpty() }
            } ?: stringResource(R.string.explore_filter_aesthetic_selected_count, 1)
        }
        else -> stringResource(R.string.explore_filter_aesthetic_selected_count, selectedIds.size)
    }
    val cd = stringResource(R.string.explore_filter_aesthetic_summary_cd, summary)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = FashTheme.spacing.spacing2),
    ) {
        ExploreFilterSectionLabel(text = stringResource(R.string.explore_style_section_title))
        FilterSelectionSummaryCard(
            summary = summary,
            onClick = onOpenPicker,
            contentDescription = cd,
            modifier = Modifier.padding(horizontal = edge),
        )
    }
}

/**
 * Tappable summary row — tonal fill only (no outline), aligned with [ExploreFiltersBar].
 */
@Composable
fun FilterSelectionSummaryCard(
    summary: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(FashTheme.spacing.radiusSoftMin)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .semantics(mergeDescendants = true) {
                contentDescription?.let { this.contentDescription = it }
                role = Role.Button
            }
            .clickable(onClick = onClick),
        shape = shape,
        color = scheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FashTheme.spacing.spacing3, vertical = FashTheme.spacing.spacing3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = FashColors.Primary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreBrandPickerSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    brands: List<CommonBrandDto>,
    selectedBrandId: String?,
    onSelectBrand: (String?) -> Unit,
) {
    if (!visible || brands.isEmpty()) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scheme = MaterialTheme.colorScheme
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val q = searchQuery.trim().lowercase(Locale.getDefault())
    val filtered = remember(brands, q) {
        if (q.isEmpty()) brands
        else brands.filter { b ->
            b.name.lowercase(Locale.getDefault()).contains(q) ||
                b.slug.lowercase(Locale.getDefault()).contains(q)
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
                title = stringResource(R.string.explore_filter_brand_title),
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
                        text = stringResource(R.string.explore_filter_brand_search_placeholder),
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
                            label = stringResource(R.string.explore_filter_brand_any),
                            selected = selectedBrandId.isNullOrBlank(),
                            onClick = {
                                onSelectBrand(null)
                                onDismiss()
                            },
                        )
                    }
                    items(filtered, key = { it.id }) { b ->
                        val id = b.id.trim()
                        if (id.isEmpty()) return@items
                        ExploreFilterSingleSelectRow(
                            label = b.name.ifBlank { b.slug },
                            selected = id == selectedBrandId,
                            onClick = {
                                onSelectBrand(id)
                                onDismiss()
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreAestheticTagsPickerSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    catalog: List<CommonAestheticTagDto>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    onClear: () -> Unit,
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scheme = MaterialTheme.colorScheme
    val isVi = AppLocale.currentTag(LocalContext.current) != AppLocale.TAG_EN
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val q = searchQuery.trim().lowercase(Locale.getDefault())
    val filtered = remember(catalog, q) {
        if (q.isEmpty()) catalog
        else catalog.filter { t ->
            t.displayName.lowercase(Locale.getDefault()).contains(q) ||
                t.displayNameVi.lowercase(Locale.getDefault()).contains(q) ||
                t.name.lowercase(Locale.getDefault()).contains(q)
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
                title = stringResource(R.string.explore_style_section_title),
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
                        text = stringResource(R.string.explore_filter_aesthetic_search_placeholder),
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = RoundedCornerShape(FashTheme.spacing.radiusSoftMin),
                colors = exploreFilterSearchFieldColors(),
            )
            if (selectedIds.isNotEmpty()) {
                TextButton(
                    onClick = onClear,
                    modifier = Modifier.padding(start = FashTheme.spacing.editorialStart),
                ) {
                    Text(
                        text = stringResource(R.string.explore_filter_aesthetic_clear),
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.primary,
                    )
                }
            }
            ExploreFilterHorizontalDivider()
            ExploreFilterListSurface {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp),
                ) {
                    items(filtered, key = { it.id }) { tag ->
                        val id = tag.id.trim()
                        if (id.isEmpty()) return@items
                        val label = tag.displayLabel(isVi)
                        val checked = id in selectedIds
                        ExploreFilterMultiSelectRow(
                            label = label,
                            checked = checked,
                            onToggle = { onToggle(id) },
                        )
                    }
                }
            }
        }
    }
}

/** Province → district → ward cascade — same flow as address creation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreBrowseLocationPickerSheet(
    visible: Boolean,
    commonServiceRepository: CommonServiceRepository,
    initialProvinceId: String? = null,
    initialDistrictId: String? = null,
    initialWardId: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        provinceId: String,
        provinceName: String,
        districtId: String,
        districtName: String,
        wardId: String,
        wardName: String,
    ) -> Unit,
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scheme = MaterialTheme.colorScheme
    val edge = FashTheme.spacing.editorialStart
    var provinces by remember { mutableStateOf<List<CommonAddressDto>>(emptyList()) }
    var districts by remember { mutableStateOf<List<CommonAddressDto>>(emptyList()) }
    var wards by remember { mutableStateOf<List<CommonAddressDto>>(emptyList()) }
    var selectedProvince by remember(initialProvinceId) { mutableStateOf<CommonAddressDto?>(null) }
    var selectedDistrict by remember(initialDistrictId) { mutableStateOf<CommonAddressDto?>(null) }
    var selectedWard by remember(initialWardId) { mutableStateOf<CommonAddressDto?>(null) }
    var loadingProvinces by remember { mutableStateOf(true) }
    var loadingDistricts by remember { mutableStateOf(false) }
    var loadingWards by remember { mutableStateOf(false) }
    val emptyDropdownText = stringResource(R.string.address_dropdown_no_options)
    val loadingText = stringResource(R.string.explore_filter_loading)

    LaunchedEffect(Unit) {
        loadingProvinces = true
        provinces = commonServiceRepository.getProvincesCatalog().getOrElse { emptyList() }
        val pid = initialProvinceId?.trim()?.takeIf { it.isNotEmpty() }
        if (pid != null) {
            selectedProvince = provinces.find { it.id == pid }
        }
        loadingProvinces = false
    }
    LaunchedEffect(selectedProvince?.id) {
        val pid = selectedProvince?.id ?: run {
            districts = emptyList()
            wards = emptyList()
            selectedDistrict = null
            selectedWard = null
            return@LaunchedEffect
        }
        loadingDistricts = true
        districts = commonServiceRepository.getAdministrativeChildren(pid, 2).getOrElse { emptyList() }
        val did = initialDistrictId?.trim()?.takeIf { it.isNotEmpty() }
        selectedDistrict = if (did != null && selectedProvince?.id == initialProvinceId) {
            districts.find { it.id == did }
        } else {
            null
        }
        selectedWard = null
        loadingDistricts = false
    }
    LaunchedEffect(selectedDistrict?.id) {
        val did = selectedDistrict?.id ?: run {
            wards = emptyList()
            selectedWard = null
            return@LaunchedEffect
        }
        loadingWards = true
        wards = commonServiceRepository.getAdministrativeChildren(did, 3).getOrElse { emptyList() }
        val wid = initialWardId?.trim()?.takeIf { it.isNotEmpty() }
        selectedWard = if (wid != null && selectedDistrict?.id == initialDistrictId) {
            wards.find { it.id == wid }
        } else {
            null
        }
        loadingWards = false
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
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = FashTheme.spacing.spacing4),
        ) {
            ExploreFilterPickerHeader(
                title = stringResource(R.string.browse_location_picker_title),
                onDone = onDismiss,
            )
            Text(
                text = stringResource(R.string.browse_location_picker_hierarchy_hint),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = edge, vertical = 4.dp),
            )
            if (loadingProvinces) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = edge, vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = FashColors.Primary,
                    )
                    Text(
                        text = loadingText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            } else {
                VnAddressDropdown(
                    label = stringResource(R.string.address_field_province),
                    options = provinces,
                    selected = selectedProvince,
                    onSelect = { selected ->
                        selectedProvince = selected
                        selectedDistrict = null
                        selectedWard = null
                    },
                    placeholder = stringResource(R.string.address_select_province),
                    emptyOptionsText = emptyDropdownText,
                    modifier = Modifier.padding(horizontal = edge),
                )
                Spacer(modifier = Modifier.heightIn(min = 10.dp))
                VnAddressDropdown(
                    label = stringResource(R.string.address_field_district),
                    options = if (loadingDistricts) emptyList() else districts,
                    selected = selectedDistrict,
                    onSelect = { selected ->
                        selectedDistrict = selected
                        selectedWard = null
                    },
                    enabled = selectedProvince != null && !loadingDistricts,
                    placeholder = stringResource(R.string.address_select_district),
                    emptyOptionsText = emptyDropdownText,
                    modifier = Modifier.padding(horizontal = edge),
                )
                Spacer(modifier = Modifier.heightIn(min = 10.dp))
                VnAddressDropdown(
                    label = stringResource(R.string.address_field_ward),
                    options = if (loadingWards) emptyList() else wards,
                    selected = selectedWard,
                    onSelect = { selectedWard = it },
                    enabled = selectedDistrict != null && !loadingWards,
                    placeholder = stringResource(R.string.browse_location_picker_ward_optional),
                    emptyOptionsText = emptyDropdownText,
                    modifier = Modifier.padding(horizontal = edge),
                )
            }
            Button(
                onClick = {
                    val p = selectedProvince ?: return@Button
                    val d = selectedDistrict
                    val w = selectedWard
                    onConfirm(
                        p.id,
                        p.name,
                        d?.id.orEmpty(),
                        d?.name.orEmpty(),
                        w?.id.orEmpty(),
                        w?.name.orEmpty(),
                    )
                },
                enabled = selectedProvince != null && !loadingProvinces,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = edge, vertical = 12.dp),
                shape = RoundedCornerShape(FashTheme.spacing.radiusSoftMin),
                colors = ButtonDefaults.buttonColors(containerColor = FashColors.Primary),
            ) {
                Text(stringResource(R.string.browse_location_picker_apply))
            }
        }
    }
}

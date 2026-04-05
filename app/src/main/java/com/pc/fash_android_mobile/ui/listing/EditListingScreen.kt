package com.pc.fash_android_mobile.ui.listing

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.listing.ListingDetail
import com.pc.fash_android_mobile.data.listing.ListingShippingAddress
import com.pc.fash_android_mobile.ui.components.FashAsyncImage
import com.pc.fash_android_mobile.ui.post.ListingConditionOptions
import com.pc.fash_android_mobile.ui.post.MaxListingDescriptionLength
import com.pc.fash_android_mobile.ui.post.MaxListingTitleLength
import com.pc.fash_android_mobile.ui.post.PostListingColors
import com.pc.fash_android_mobile.ui.post.PostListingOutlinedTextField
import com.pc.fash_android_mobile.ui.post.MaxAestheticTags
import com.pc.fash_android_mobile.ui.post.PostSelectablePill
import com.pc.fash_android_mobile.ui.post.PostStepScrollWithBottomNotice
import com.pc.fash_android_mobile.ui.feed.formatListingPriceVnd
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme
import com.pc.fash_android_mobile.ui.theme.fashReadableOn

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditListingScreen(
    modifier: Modifier = Modifier,
    listingId: String,
    viewModel: EditListingViewModel,
    onBack: () -> Unit,
) {
    val detail by viewModel.detail.collectAsState()
    val form by viewModel.form.collectAsState()
    val catalogTags by viewModel.catalogTags.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val isDeleting by viewModel.isDeleting.collectAsState()
    val baselineTagIds by viewModel.baselineTagIds.collectAsState()
    val brandsFeatured by viewModel.brandsFeatured.collectAsState()
    val brandsSearch by viewModel.brandsSearch.collectAsState()
    val countrySearch by viewModel.countrySearch.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(listingId) {
        viewModel.load(listingId)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = PostListingColors.stepCanvas(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.edit_listing_screen_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.orders_back),
                            tint = FashColors.Primary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PostListingColors.stepCanvas(),
                ),
            )
        },
        bottomBar = {
            val d = detail
            if (d != null && d.status.equals("active", ignoreCase = true)) {
                Surface(
                    tonalElevation = 1.dp,
                    shadowElevation = 0.dp,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = FashTheme.spacing.editorialStart)
                            .padding(top = 12.dp, bottom = 8.dp)
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = { viewModel.save() },
                            enabled = viewModel.canSave() && !isDeleting,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = FashColors.Primary),
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = FashColors.Primary.fashReadableOn(),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text(stringResource(R.string.edit_listing_save))
                            }
                        }
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            enabled = !isSaving && !isDeleting,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.edit_listing_delete))
                        }
                    }
                }
            }
        },
    ) { padding ->
        when {
            isLoading && detail == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = FashColors.Primary)
                }
            }
            loadError != null && detail == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = loadError!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.load(listingId) }) {
                        Text(stringResource(R.string.feed_retry))
                    }
                }
            }
            detail != null -> {
                val d = detail!!
                val editable = d.status.equals("active", ignoreCase = true)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    Column(Modifier.weight(1f)) {
                        PostStepScrollWithBottomNotice(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalPadding = FashTheme.spacing.editorialStart,
                            bottomNotice = if (editable) {
                                stringResource(R.string.edit_listing_bottom_hint)
                            } else {
                                null
                            },
                            scrollState = scrollState,
                        ) {
                            if (!editable) {
                                Text(
                                    text = stringResource(R.string.edit_listing_readonly_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            FashColors.Primary.copy(alpha = 0.12f),
                                            RoundedCornerShape(12.dp),
                                        )
                                        .padding(16.dp),
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            Text(
                                text = stringResource(R.string.edit_listing_photos_section),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.edit_listing_photos_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            ListingImageStrip(detail = d)
                            Spacer(modifier = Modifier.height(20.dp))

                            EditListingSectionCard {
                                Text(
                                    text = stringResource(R.string.edit_listing_category_locked),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = buildString {
                                        d.parentCategoryName?.takeIf { it.isNotBlank() }?.let { append("$it · ") }
                                        append(d.category ?: stringResource(R.string.create_listing_select))
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = stringResource(R.string.post_step_style_only),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.edit_listing_tags_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            EditListingStyleTagsDropdown(
                                catalogTags = catalogTags,
                                selectedTagIds = form.selectedTagIds,
                                maxTags = MaxAestheticTags,
                                enabled = editable && !isSaving,
                                onToggleTag = { viewModel.toggleTag(it) },
                            )
                            if (form.selectedTagIds != baselineTagIds && form.selectedTagIds.isEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stringResource(R.string.edit_listing_tags_cleared_note),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            EditListingSectionCard {
                                Text(
                                    text = stringResource(R.string.post_step_brand),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                EditListingBrandDropdown(
                                    brandName = form.brandName,
                                    brandId = form.brandId,
                                    brandsFeatured = brandsFeatured,
                                    brandsSearch = brandsSearch,
                                    enabled = editable && !isSaving,
                                    onSearchBrands = { viewModel.searchBrands(it) },
                                    onSelectBrand = { viewModel.selectBrand(it) },
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = stringResource(R.string.post_step_condition_short),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                ListingConditionOptions.uiValues.forEach { cond ->
                                    PostSelectablePill(
                                        text = cond,
                                        selected = form.condition == cond,
                                        onClick = {
                                            if (editable && !isSaving) {
                                                viewModel.updateForm { copy(condition = cond) }
                                            }
                                        },
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            PostListingOutlinedTextField(
                                value = form.title,
                                onValueChange = {
                                    viewModel.updateForm { copy(title = it.take(MaxListingTitleLength)) }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.create_listing_title_label)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                enabled = editable && !isSaving,
                            )
                            Text(
                                text = "${form.title.length}/$MaxListingTitleLength",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            PostListingOutlinedTextField(
                                value = form.description,
                                onValueChange = {
                                    viewModel.updateForm { copy(description = it.take(MaxListingDescriptionLength)) }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp),
                                label = { Text(stringResource(R.string.post_description_label)) },
                                singleLine = false,
                                minLines = 4,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                enabled = editable && !isSaving,
                            )
                            Text(
                                text = "${form.description.length}/$MaxListingDescriptionLength",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            EditListingSectionCard {
                                PostListingOutlinedTextField(
                                    value = form.size,
                                    onValueChange = {
                                        viewModel.updateForm { copy(size = it.take(20)) }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(stringResource(R.string.create_listing_size_label)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                    enabled = editable && !isSaving,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                PostListingOutlinedTextField(
                                    value = form.priceText,
                                    onValueChange = { v ->
                                        if (v.all { it.isDigit() } && v.length <= 12) {
                                            viewModel.updateForm { copy(priceText = v) }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(stringResource(R.string.create_listing_price_label)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    enabled = editable && !isSaving,
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.post_step_country),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            EditListingSectionCard {
                                EditListingCountryDropdown(
                                    countryName = form.countryName,
                                    countryIso2 = form.countryIso2,
                                    countryId = form.countryId,
                                    countries = countrySearch,
                                    enabled = editable && !isSaving,
                                    onSearchCountries = { viewModel.searchCountries(it) },
                                    onSelectCountry = { viewModel.selectCountry(it) },
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.post_step_measure),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.post_measure_step_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            val unitSuffix =
                                if (form.measurementUnit.equals("cm", ignoreCase = true)) {
                                    stringResource(R.string.post_unit_cm)
                                } else {
                                    stringResource(R.string.post_unit_in)
                                }
                            EditListingSectionCard {
                                Text(
                                    text = stringResource(R.string.post_measure_section_unit),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PostSelectablePill(
                                        text = stringResource(R.string.post_unit_cm),
                                        selected = form.measurementUnit.equals("cm", ignoreCase = true),
                                        onClick = {
                                            if (editable && !isSaving) {
                                                viewModel.updateForm { copy(measurementUnit = "cm") }
                                            }
                                        },
                                    )
                                    PostSelectablePill(
                                        text = stringResource(R.string.post_unit_in),
                                        selected = form.measurementUnit.equals("in", ignoreCase = true),
                                        onClick = {
                                            if (editable && !isSaving) {
                                                viewModel.updateForm { copy(measurementUnit = "in") }
                                            }
                                        },
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.post_measure_section_details),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(FashTheme.spacing.spacing3)) {
                                    EditListingMeasurementField(
                                        label = stringResource(R.string.post_measurement_hem),
                                        value = form.measurementHem,
                                        unitSuffix = unitSuffix,
                                        enabled = editable && !isSaving,
                                        onChange = { viewModel.updateForm { copy(measurementHem = it) } },
                                    )
                                    EditListingMeasurementField(
                                        label = stringResource(R.string.post_measurement_chest),
                                        value = form.measurementChest,
                                        unitSuffix = unitSuffix,
                                        enabled = editable && !isSaving,
                                        onChange = { viewModel.updateForm { copy(measurementChest = it) } },
                                    )
                                    EditListingMeasurementField(
                                        label = stringResource(R.string.post_measurement_length),
                                        value = form.measurementLength,
                                        unitSuffix = unitSuffix,
                                        enabled = editable && !isSaving,
                                        onChange = { viewModel.updateForm { copy(measurementLength = it) } },
                                    )
                                    EditListingMeasurementField(
                                        label = stringResource(R.string.post_measurement_shoulders),
                                        value = form.measurementShoulders,
                                        unitSuffix = unitSuffix,
                                        enabled = editable && !isSaving,
                                        onChange = { viewModel.updateForm { copy(measurementShoulders = it) } },
                                    )
                                    EditListingMeasurementField(
                                        label = stringResource(R.string.post_measurement_sleeve),
                                        value = form.measurementSleeveLength,
                                        unitSuffix = unitSuffix,
                                        enabled = editable && !isSaving,
                                        onChange = { viewModel.updateForm { copy(measurementSleeveLength = it) } },
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.edit_listing_offers_section),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.edit_listing_offers_section_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            EditListingSectionCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        stringResource(R.string.post_accept_offers),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Switch(
                                        checked = form.acceptOffers,
                                        onCheckedChange = {
                                            if (editable && !isSaving) {
                                                viewModel.updateForm { copy(acceptOffers = it) }
                                            }
                                        },
                                        enabled = editable && !isSaving,
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        stringResource(R.string.post_auto_price_drop),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Switch(
                                        checked = form.autoPriceDropEnabled,
                                        onCheckedChange = { on ->
                                            if (editable && !isSaving) {
                                                viewModel.updateForm {
                                                    copy(
                                                        autoPriceDropEnabled = on,
                                                        priceDropPercentInput = if (
                                                            on && priceDropPercentInput.filter { ch -> ch.isDigit() }.isEmpty()
                                                        ) {
                                                            "10"
                                                        } else {
                                                            priceDropPercentInput
                                                        },
                                                    )
                                                }
                                            }
                                        },
                                        enabled = editable && !isSaving,
                                    )
                                }
                                if (form.autoPriceDropEnabled) {
                                    PostListingOutlinedTextField(
                                        value = form.floorPriceText,
                                        onValueChange = {
                                            if (editable && !isSaving) {
                                                viewModel.updateForm {
                                                    copy(floorPriceText = it.filter { ch -> ch.isDigit() }.take(12))
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        label = { Text(stringResource(R.string.post_floor_price)) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        enabled = editable && !isSaving,
                                    )
                                    PostListingOutlinedTextField(
                                        value = form.priceDropPercentInput,
                                        onValueChange = { raw ->
                                            if (editable && !isSaving) {
                                                val digits = raw.filter { it.isDigit() }.take(2)
                                                viewModel.updateForm { copy(priceDropPercentInput = digits) }
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        label = { Text(stringResource(R.string.post_drop_percent)) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        enabled = editable && !isSaving,
                                    )
                                }
                            }

                            EditListingReadOnlyDetailsSection(detail = d)

                            Spacer(modifier = Modifier.height(24.dp))

                            if (!editable) {
                                OutlinedButton(
                                    onClick = { showDeleteDialog = true },
                                    enabled = !isSaving && !isDeleting,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error,
                                    ),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.edit_listing_delete))
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
            title = { Text(stringResource(R.string.edit_listing_delete_title)) },
            text = { Text(stringResource(R.string.edit_listing_delete_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.delete()
                    },
                    enabled = !isDeleting,
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = FashColors.Primary,
                        )
                    } else {
                        Text(
                            stringResource(R.string.edit_listing_delete_confirm),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }, enabled = !isDeleting) {
                    Text(stringResource(R.string.create_listing_cancel))
                }
            },
        )
    }
}

@Composable
private fun EditListingSectionCard(content: @Composable ColumnScope.() -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
        color = scheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FashTheme.spacing.spacing4),
            verticalArrangement = Arrangement.spacedBy(FashTheme.spacing.spacing3),
            content = content,
        )
    }
}

@Composable
private fun EditListingReadOnlyDetailsSection(detail: ListingDetail) {
    val hasShipping = detail.shippingAddress != null
    val hasEstShipping = detail.estimatedShippingVnd != null
    if (!hasShipping && !hasEstShipping) return

    Spacer(modifier = Modifier.height(20.dp))
    Text(
        text = stringResource(R.string.edit_listing_readonly_section_title),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.edit_listing_readonly_section_subtitle),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(12.dp))

    EditListingSectionCard {
        if (hasShipping) {
            Text(
                text = stringResource(R.string.post_step_shipping),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatShippingAddress(detail.shippingAddress!!),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (hasEstShipping) {
            if (hasShipping) Spacer(modifier = Modifier.height(8.dp))
            detail.estimatedShippingVnd?.let { fee ->
                Text(
                    text = stringResource(R.string.product_shipping_estimate, formatListingPriceVnd(fee)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EditListingMeasurementField(
    label: String,
    value: String,
    unitSuffix: String,
    enabled: Boolean,
    onChange: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    PostListingOutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        enabled = enabled,
        suffix = {
            Text(
                text = unitSuffix,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
        },
    )
}

private fun formatShippingAddress(a: ListingShippingAddress): String = buildString {
    a.label?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
    append(a.line1)
    a.line2?.takeIf { it.isNotBlank() }?.let { append(", ").append(it) }
    a.city?.takeIf { it.isNotBlank() }?.let { append(", ").append(it) }
    a.region?.takeIf { it.isNotBlank() }?.let { append(", ").append(it) }
    a.postalCode?.takeIf { it.isNotBlank() }?.let { append(" ").append(it) }
}

@Composable
private fun ListingImageStrip(detail: ListingDetail) {
    val urls = detail.imageUrls.mapNotNull { u ->
        u.takeIf { it.isNotBlank() }?.let { resolveEditImageUrl(it) }
    }
    if (urls.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        urls.forEach { url ->
            Box(
                modifier = Modifier
                    .size(width = 120.dp, height = 150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                FashAsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

private fun resolveEditImageUrl(path: String): String {
    if (path.startsWith("http")) return path
    val base = AppEnvironment.apiBaseUrl.trimEnd('/')
    return "$base/${path.trimStart('/')}"
}

package com.pc.fash_android_mobile.ui.post

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.address.ShippingAddressSelectableCard
import com.pc.fash_android_mobile.ui.components.FashAsyncImage
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme
import com.pc.fash_android_mobile.ui.theme.dashedRoundRectBorder

@Composable
fun CreateListingPostStep6(viewModel: PostViewModel, onCloseRequest: () -> Unit) {
    val draft by viewModel.draft.collectAsState()
    val canNext = draft.canProceedFromStep(6)
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PostListingColors.stepCanvas())
            .navigationBarsPadding(),
    ) {
        CreateListingFlowHeader(
            step = 6,
            totalSteps = TotalPostSteps,
            onBackClick = { viewModel.prevStep() },
            onCloseClick = onCloseRequest,
            primaryLabelRes = R.string.create_listing_next,
            onPrimaryClick = { viewModel.nextStep() },
            primaryEnabled = canNext,
            nextDisabledReasonRes = draft.nextStepBlockedReasonRes(6),
        )
        PostStepScrollWithBottomNotice(
            modifier = Modifier.weight(1f),
            horizontalPadding = FashTheme.spacing.editorialStart,
            bottomNotice = stringResource(R.string.post_measure_notice_combined),
            scrollState = scrollState,
        ) {
            val unitSuffix =
                if (draft.measurementUnit.equals("cm", ignoreCase = true)) {
                    stringResource(R.string.post_unit_cm)
                } else {
                    stringResource(R.string.post_unit_in)
                }
            Text(
                text = stringResource(R.string.post_step_measure),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.post_measure_step_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            PostMeasureSectionCard {
                PostMeasureSectionLabel(stringResource(R.string.post_measure_section_size))
                PostListingOutlinedTextField(
                    value = draft.size,
                    onValueChange = { viewModel.updateDraft { copy(size = it.take(20)) } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.create_listing_size_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            PostMeasureSectionCard {
                PostMeasureSectionLabel(stringResource(R.string.post_measure_section_unit))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PostSelectablePill(
                        text = stringResource(R.string.post_unit_cm),
                        selected = draft.measurementUnit.equals("cm", ignoreCase = true),
                        onClick = { viewModel.updateDraft { copy(measurementUnit = "cm") } },
                    )
                    PostSelectablePill(
                        text = stringResource(R.string.post_unit_in),
                        selected = draft.measurementUnit.equals("in", ignoreCase = true),
                        onClick = { viewModel.updateDraft { copy(measurementUnit = "in") } },
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            PostMeasureSectionCard {
                PostMeasureSectionLabel(stringResource(R.string.post_measure_section_details))
                Column(verticalArrangement = Arrangement.spacedBy(FashTheme.spacing.spacing3)) {
                    MeasurementField(
                        label = stringResource(R.string.post_measurement_hem),
                        value = draft.measurementHem,
                        unitSuffix = unitSuffix,
                        onChange = { viewModel.updateDraft { copy(measurementHem = it) } },
                    )
                    MeasurementField(
                        label = stringResource(R.string.post_measurement_chest),
                        value = draft.measurementChest,
                        unitSuffix = unitSuffix,
                        onChange = { viewModel.updateDraft { copy(measurementChest = it) } },
                    )
                    MeasurementField(
                        label = stringResource(R.string.post_measurement_length),
                        value = draft.measurementLength,
                        unitSuffix = unitSuffix,
                        onChange = { viewModel.updateDraft { copy(measurementLength = it) } },
                    )
                    MeasurementField(
                        label = stringResource(R.string.post_measurement_shoulders),
                        value = draft.measurementShoulders,
                        unitSuffix = unitSuffix,
                        onChange = { viewModel.updateDraft { copy(measurementShoulders = it) } },
                    )
                    MeasurementField(
                        label = stringResource(R.string.post_measurement_sleeve),
                        value = draft.measurementSleeveLength,
                        unitSuffix = unitSuffix,
                        onChange = { viewModel.updateDraft { copy(measurementSleeveLength = it) } },
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PostMeasureSectionCard(content: @Composable ColumnScope.() -> Unit) {
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
private fun PostMeasureSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun MeasurementField(
    label: String,
    value: String,
    unitSuffix: String,
    onChange: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    PostListingOutlinedTextField(
        value = value,
        onValueChange = { onChange(it) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        suffix = {
            Text(
                text = unitSuffix,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
        },
    )
}

@Composable
fun CreateListingPostStep7(
    viewModel: PostViewModel,
    onCloseRequest: () -> Unit,
) {
    val draft by viewModel.draft.collectAsState()
    val canNext = draft.canProceedFromStep(7)
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
    ) { uris: List<Uri> ->
        viewModel.setImageUris(uris)
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PostListingColors.stepCanvas())
            .navigationBarsPadding(),
    ) {
        CreateListingFlowHeader(
            step = 7,
            totalSteps = TotalPostSteps,
            onBackClick = { viewModel.prevStep() },
            onCloseClick = onCloseRequest,
            primaryLabelRes = R.string.create_listing_next,
            onPrimaryClick = { viewModel.nextStep() },
            primaryEnabled = canNext,
            nextDisabledReasonRes = draft.nextStepBlockedReasonRes(7),
        )
        PostStepScrollWithBottomNotice(
            modifier = Modifier.weight(1f),
            horizontalPadding = FashTheme.spacing.editorialStart,
            bottomNotice = stringResource(R.string.post_hint_photos) + "\n" + stringResource(R.string.create_listing_tip_text),
            scrollState = scrollState,
        ) {
            Text(
                text = stringResource(R.string.post_step_photos),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Spacer(modifier = Modifier.height(16.dp))
            PostAddPhotoBox(onClick = { imagePicker.launch("image/*") })
            if (draft.imageUris.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                PostImagePreviewRow(
                    imageUriStrings = draft.imageUris,
                    onRemove = { viewModel.removeImage(it) },
                    onAddMore = { imagePicker.launch("image/*") },
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PostAddPhotoBox(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.5f)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp),
            )
            .background(PostListingColors.fieldSurface())
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.create_listing_add_photo),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.create_listing_photo_tip),
                style = MaterialTheme.typography.bodySmall,
                color = FashColors.Primary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PostImagePreviewRow(
    imageUriStrings: List<String>,
    onRemove: (Int) -> Unit,
    onAddMore: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        imageUriStrings.forEachIndexed { index, uriStr ->
            val uri = Uri.parse(uriStr)
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PostListingColors.fieldSurface()),
            ) {
                FashAsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                if (index == 0) {
                    Text(
                        text = stringResource(R.string.create_listing_cover_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .background(FashColors.Primary)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
                IconButton(
                    onClick = { onRemove(index) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        if (imageUriStrings.size < 6) {
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PostListingColors.fieldSurface())
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .clickable(onClick = onAddMore),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun CreateListingPostStep8(viewModel: PostViewModel, onCloseRequest: () -> Unit) {
    val draft by viewModel.draft.collectAsState()
    val canNext = draft.canProceedFromStep(8)
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PostListingColors.stepCanvas())
            .navigationBarsPadding(),
    ) {
        CreateListingFlowHeader(
            step = 8,
            totalSteps = TotalPostSteps,
            onBackClick = { viewModel.prevStep() },
            onCloseClick = onCloseRequest,
            primaryLabelRes = R.string.create_listing_next,
            onPrimaryClick = { viewModel.nextStep() },
            primaryEnabled = canNext,
            nextDisabledReasonRes = draft.nextStepBlockedReasonRes(8),
        )
        PostStepScrollWithBottomNotice(
            modifier = Modifier.weight(1f),
            horizontalPadding = FashTheme.spacing.editorialStart,
            bottomNotice = stringResource(R.string.post_hint_price),
            scrollState = scrollState,
        ) {
            Text(
                text = stringResource(R.string.post_step_price),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Spacer(modifier = Modifier.height(12.dp))
            PostListingOutlinedTextField(
                value = draft.priceVnd,
                onValueChange = { viewModel.updateDraft { copy(priceVnd = it.filter { ch -> ch.isDigit() }.take(12)) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.create_listing_price_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.post_accept_offers))
                Switch(
                    checked = draft.acceptOffers,
                    onCheckedChange = { viewModel.updateDraft { copy(acceptOffers = it) } },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.post_auto_price_drop))
                Switch(
                    checked = draft.autoPriceDropEnabled,
                    onCheckedChange = { on ->
                        viewModel.updateDraft {
                            copy(
                                autoPriceDropEnabled = on,
                                priceDropPercentInput = if (on && priceDropPercentInput.filter { d -> d.isDigit() }.isEmpty()) {
                                    "10"
                                } else {
                                    priceDropPercentInput
                                },
                            )
                        }
                    },
                )
            }
            if (draft.autoPriceDropEnabled) {
                PostListingOutlinedTextField(
                    value = draft.floorPriceVnd,
                    onValueChange = {
                        viewModel.updateDraft { copy(floorPriceVnd = it.filter { ch -> ch.isDigit() }.take(12)) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    label = { Text(stringResource(R.string.post_floor_price)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                PostListingOutlinedTextField(
                    value = draft.priceDropPercentInput,
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }.take(2)
                        viewModel.updateDraft { copy(priceDropPercentInput = digits) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    label = { Text(stringResource(R.string.post_drop_percent)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun CreateListingPostStep9(
    viewModel: PostViewModel,
    onCloseRequest: () -> Unit,
    onAddAddressClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val draft by viewModel.draft.collectAsState()
    val addresses by viewModel.localAddresses.collectAsState()
    val canNext = draft.canProceedFromStep(9)

    LaunchedEffect(Unit) {
        viewModel.loadShippingAddresses()
        viewModel.applyDefaultShippingIfNeeded()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PostListingColors.stepCanvas())
            .navigationBarsPadding(),
    ) {
        CreateListingFlowHeader(
            step = 9,
            totalSteps = TotalPostSteps,
            onBackClick = { viewModel.prevStep() },
            onCloseClick = onCloseRequest,
            primaryLabelRes = R.string.create_listing_next,
            onPrimaryClick = { viewModel.nextStep() },
            primaryEnabled = canNext,
            nextDisabledReasonRes = draft.nextStepBlockedReasonRes(9),
            centerTitleRes = R.string.address_list_title,
            showStepCaptionUnderTitle = true,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(PostListingColors.stepCanvas())
                .padding(horizontal = FashTheme.spacing.editorialStart),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.address_list_header),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = scheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.address_list_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (addresses.isEmpty()) {
                    Text(
                        text = stringResource(R.string.post_no_saved_address),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.error,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(addresses, key = { it.id }) { addr ->
                            ShippingAddressSelectableCard(
                                address = addr,
                                selected = draft.shippingAddressId == addr.id,
                                onClick = {
                                    viewModel.updateDraft {
                                        copy(
                                            shippingAddressId = addr.id,
                                            shippingAddressLabel = addr.labelForDraft(),
                                        )
                                    }
                                },
                                onSetDefault = null,
                                showSetDefaultButton = false,
                            )
                        }
                    }
                }
            }
            PostFlowNoticeCard(
                text = stringResource(R.string.post_hint_shipping),
                horizontalPadding = 0.dp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .dashedRoundRectBorder(
                        width = 1.dp,
                        color = scheme.outlineVariant,
                        cornerRadius = 12.dp,
                    )
                    .clickable(onClick = onAddAddressClick),
                shape = RoundedCornerShape(12.dp),
                color = PostListingColors.fieldSurface().copy(alpha = 0.92f),
                shadowElevation = 0.dp,
                tonalElevation = 0.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = FashColors.Primary)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.address_add_new),
                        color = FashColors.Primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun CreateListingPostStep10(
    viewModel: PostViewModel,
    onCloseRequest: () -> Unit,
    onSubmitSuccess: () -> Unit,
) {
    val draft by viewModel.draft.collectAsState()
    val meProfile by viewModel.meProfile.collectAsState()
    val tagsById by viewModel.aestheticTagsById.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val context = LocalContext.current
    val uriResolver: (Uri) -> Pair<ByteArray, String>? = { uri ->
        val mimeType = context.contentResolver.getType(uri)?.takeIf { !it.contains('*') } ?: "image/jpeg"
        context.contentResolver.openInputStream(uri)?.use { stream ->
            Pair(stream.readBytes(), mimeType)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadProfileForPreview()
        viewModel.loadShippingAddresses()
    }

    val reviewScrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PostListingColors.stepCanvas())
            .navigationBarsPadding(),
    ) {
        CreateListingFlowHeader(
            step = 10,
            totalSteps = TotalPostSteps,
            onBackClick = { viewModel.prevStep() },
            onCloseClick = onCloseRequest,
            primaryLabelRes = R.string.create_listing_post_for_sale,
            onPrimaryClick = {
                viewModel.submitListing(uriResolver) { onSubmitSuccess() }
            },
            primaryEnabled = !isSubmitting,
            primaryLoading = isSubmitting,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(reviewScrollState)
                    .padding(horizontal = FashTheme.spacing.editorialStart),
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.create_listing_step3_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                CreateListingReviewCard(
                    draft = draft,
                    meProfile = meProfile,
                    aestheticTagsById = tagsById,
                )
            }
        }
        CreateListingReviewFooter()
    }
}

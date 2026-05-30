@file:OptIn(ExperimentalLayoutApi::class)

package com.pc.fash_android_mobile.ui.post

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.pc.fash_android_mobile.data.listing.ListingImagePixelSize
import com.pc.fash_android_mobile.data.locale.AppLocale
import com.pc.fash_android_mobile.ui.address.ShippingAddressSelectableCard
import com.pc.fash_android_mobile.ui.feed.resolveListingImageUrl
import com.pc.fash_android_mobile.ui.components.FashAsyncImage
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme
import com.pc.fash_android_mobile.ui.feed.formatListingPriceVnd
import com.pc.fash_android_mobile.ui.onboarding.ProfileSetupSizingSection
import com.pc.fash_android_mobile.ui.theme.dashedRoundRectBorder

@Composable
fun CreateListingPostStep6(viewModel: PostViewModel, onCloseRequest: () -> Unit) {
    val draft by viewModel.draft.collectAsState()
    val meProfile by viewModel.meProfile.collectAsState()
    val canNext = draft.canProceedFromStep(6)
    val scrollState = rememberScrollState()
    val sizingGender = remember(draft.genderTarget, meProfile?.gender) {
        draft.sizingChartGender(meProfile)
    }

    LaunchedEffect(Unit) {
        viewModel.loadProfileForPreview()
    }

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
            Text(
                text = stringResource(R.string.post_step_measure),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.post_listing_sizing_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (draft.fillMode == CreateListingFillMode.FROM_PROFILE_STYLE &&
                (draft.size.isNotBlank() || hasAnyMeasurement(draft) || draft.genderTarget.isNotBlank())
            ) {
                PostProfilePrefilledBanner()
                Spacer(modifier = Modifier.height(12.dp))
            }

            PostMeasureSectionCard {
                PostMeasureSectionLabel(
                    stringResource(
                        R.string.post_step_gender_target,
                    ) + " (${stringResource(R.string.post_listing_sizing_gender_recommended)})",
                )
                Text(
                    text = stringResource(R.string.post_listing_sizing_gender_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        "women" to stringResource(R.string.gender_target_women),
                        "men" to stringResource(R.string.gender_target_men),
                        "unisex" to stringResource(R.string.gender_target_unisex),
                        "kids" to stringResource(R.string.gender_target_kids),
                    ).forEach { (value, label) ->
                        PostSelectablePill(
                            text = label,
                            selected = draft.genderTarget == value,
                            onClick = {
                                viewModel.updateDraft {
                                    copy(genderTarget = if (genderTarget == value) "" else value)
                                }
                            },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            PostMeasureSectionCard {
                ProfileSetupSizingSection(
                    referenceSize = draft.size,
                    onReferenceSizeChange = { viewModel.updateDraft { copy(size = it.take(20)) } },
                    measurementUnit = draft.measurementUnit,
                    onMeasurementUnitChange = { viewModel.updateDraft { copy(measurementUnit = it) } },
                    hem = draft.measurementHem,
                    onHemChange = { viewModel.updateDraft { copy(measurementHem = it) } },
                    chest = draft.measurementChest,
                    onChestChange = { viewModel.updateDraft { copy(measurementChest = it) } },
                    length = draft.measurementLength,
                    onLengthChange = { viewModel.updateDraft { copy(measurementLength = it) } },
                    shoulders = draft.measurementShoulders,
                    onShouldersChange = { viewModel.updateDraft { copy(measurementShoulders = it) } },
                    sleeve = draft.measurementSleeveLength,
                    onSleeveChange = { viewModel.updateDraft { copy(measurementSleeveLength = it) } },
                    genderPreference = sizingGender,
                    compactDensity = true,
                    showTitle = false,
                    showSubtitle = false,
                    referenceSizeLabelRes = R.string.post_measure_section_size,
                    referenceSizeHintRes = R.string.profile_setup_reference_size_hint,
                    enableSizeRecommendation = false,
                )
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
fun CreateListingPostStep7(
    viewModel: PostViewModel,
    onCloseRequest: () -> Unit,
) {
    val draft by viewModel.draft.collectAsState()
    val setupLoading by viewModel.listingPhotoSetupLoading.collectAsState()
    val canNext = draft.canProceedFromStep(7)
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    LaunchedEffect(draft.categoryId) {
        viewModel.ensureListingPhotoSlotsLoaded()
    }

    var pickStepKey by remember { mutableStateOf<String?>(null) }
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val key = pickStepKey
        pickStepKey = null
        if (uri != null && key != null) {
            val size = ListingImagePixelSize.fromUri(context, uri)
            viewModel.setListingPhotoForStep(
                stepKey = key,
                uriString = uri.toString(),
                width = size?.first,
                height = size?.second,
            )
        }
    }

    fun stepLabel(slot: ListingPhotoSlotDraft): String =
        if (AppLocale.currentTag(context) == AppLocale.TAG_EN) {
            slot.label.ifBlank { slot.stepKey }
        } else {
            slot.labelVi.ifBlank { slot.label }.ifBlank { slot.stepKey }
        }

    val coverStepKey = remember(draft.listingPhotoSlots) {
        draft.listingPhotoSlots
            .filter { it.hasImageSelected() }
            .minByOrNull { it.sortOrder }
            ?.stepKey
    }

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
            when {
                setupLoading && draft.listingPhotoSlots.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = FashColors.Primary)
                    }
                }
                draft.listingPhotoSlots.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.post_listing_photo_slots_unavailable),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        draft.listingPhotoSlots
                            .sortedBy { it.sortOrder }
                            .forEach { slot ->
                                PostListingPhotoSlotCard(
                                    label = stepLabel(slot),
                                    required = slot.required,
                                    isCover = coverStepKey == slot.stepKey && slot.hasImageSelected(),
                                    imageModel = when {
                                        slot.uploadedImageUrl?.isNotBlank() == true ->
                                            resolveListingImageUrl(slot.uploadedImageUrl!!)
                                        slot.localImageUri?.isNotBlank() == true ->
                                            Uri.parse(slot.localImageUri!!)
                                        else -> null
                                    },
                                    onPick = {
                                        pickStepKey = slot.stepKey
                                        photoPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                        )
                                    },
                                    onClear = { viewModel.clearListingPhotoForStep(slot.stepKey) },
                                )
                            }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PostListingPhotoSlotCard(
    label: String,
    required: Boolean,
    isCover: Boolean,
    imageModel: Any?,
    onPick: () -> Unit,
    onClear: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
        color = scheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FashTheme.spacing.spacing4),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PostListingColors.fieldSurface())
                    .then(
                        if (imageModel == null) {
                            Modifier
                                .border(
                                    width = 1.dp,
                                    color = scheme.outlineVariant.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(12.dp),
                                )
                                .clickable(onClick = onPick)
                        } else {
                            Modifier
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                when (imageModel) {
                    null -> {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = scheme.onSurfaceVariant,
                        )
                    }
                    else -> {
                        FashAsyncImage(
                            model = imageModel,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                        if (isCover) {
                            Text(
                                text = stringResource(R.string.create_listing_cover_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onPrimary,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .background(FashColors.Primary)
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                            )
                        }
                        IconButton(
                            onClick = onClear,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(28.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = scheme.onSurface,
                            )
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = label + if (required) " *" else "",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.create_listing_add_photo),
                    style = MaterialTheme.typography.labelLarge.copy(color = FashColors.Primary),
                    modifier = Modifier.clickable(onClick = onPick),
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
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.post_price_step_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.post_price_vnd_range_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.post_price_vnd_example,
                    stringResource(R.string.create_listing_price_placeholder),
                    formatListingPriceVnd(350_000L),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(12.dp))
            ListingVndPriceField(
                value = draft.priceVnd,
                onValueChange = { viewModel.updateDraft { copy(priceVnd = it) } },
                labelRes = R.string.create_listing_price_label,
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
                ListingVndPriceField(
                    value = draft.floorPriceVnd,
                    onValueChange = { viewModel.updateDraft { copy(floorPriceVnd = it) } },
                    labelRes = R.string.post_floor_price,
                    modifier = Modifier.padding(top = 8.dp),
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
private fun ListingVndPriceField(
    value: String,
    onValueChange: (String) -> Unit,
    labelRes: Int,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val digitsOnly = value.filter { it.isDigit() }.take(12)
    val parsedAmount = digitsOnly.toLongOrNull()
    PostListingOutlinedTextField(
        value = digitsOnly,
        onValueChange = { onValueChange(it.filter { ch -> ch.isDigit() }.take(12)) },
        modifier = modifier.fillMaxWidth(),
        label = { Text(stringResource(labelRes)) },
        placeholder = { Text(stringResource(R.string.create_listing_price_placeholder)) },
        prefix = {
            Text(
                text = stringResource(R.string.post_price_vnd_prefix),
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurfaceVariant,
            )
        },
        suffix = {
            Text(
                text = stringResource(R.string.post_price_vnd_suffix),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
            )
        },
        supportingText = {
            if (parsedAmount != null && parsedAmount > 0L) {
                Text(
                    text = stringResource(
                        R.string.post_price_vnd_preview,
                        formatListingPriceVnd(parsedAmount),
                    ),
                    color = FashColors.Primary,
                )
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
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
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = scheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.updateDraft { copy(onsiteInspectionCommitment = !onsiteInspectionCommitment) }
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    androidx.compose.material3.Checkbox(
                        checked = draft.onsiteInspectionCommitment,
                        onCheckedChange = { checked ->
                            viewModel.updateDraft { copy(onsiteInspectionCommitment = checked) }
                        },
                        colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = FashColors.Primary),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.listing_commitment_title),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Text(
                            text = stringResource(R.string.listing_commitment_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
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
            centerTitleRes = R.string.post_step_review,
            showStepCaptionUnderTitle = true,
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
                    text = stringResource(R.string.post_review_buyer_page_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                CreateListingBuyerPreview(
                    draft = draft,
                    meProfile = meProfile,
                    aestheticTagsById = tagsById,
                    onEditStep = { targetStep -> viewModel.goToStep(targetStep) },
                )
            }
        }
        CreateListingReviewFooter()
    }
}

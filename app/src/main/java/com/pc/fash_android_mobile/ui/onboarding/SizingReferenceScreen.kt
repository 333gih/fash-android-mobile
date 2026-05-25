package com.pc.fash_android_mobile.ui.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.components.FashPillFilterChip
import com.pc.fash_android_mobile.ui.components.FashPrimaryButton
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme
import com.pc.fash_android_mobile.ui.theme.fashReadableOn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SizingReferenceScreen(
    modifier: Modifier = Modifier,
    referenceSize: String,
    onReferenceSizeChange: (String) -> Unit,
    measurementUnit: String,
    onMeasurementUnitChange: (String) -> Unit,
    measurementHem: String,
    onMeasurementHemChange: (String) -> Unit,
    measurementChest: String,
    onMeasurementChestChange: (String) -> Unit,
    measurementLength: String,
    onMeasurementLengthChange: (String) -> Unit,
    measurementShoulders: String,
    onMeasurementShouldersChange: (String) -> Unit,
    measurementSleeve: String,
    onMeasurementSleeveChange: (String) -> Unit,
    canSubmit: Boolean,
    isSubmitting: Boolean,
    displayProgressStep: Int = 2,
    progressTotal: Int = OnboardingFlowProgress.TOTAL_STEPS,
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    genderPreference: String = "",
    heightCm: String = "",
    onHeightCmChange: (String) -> Unit = {},
    weightKg: String = "",
    onWeightKgChange: (String) -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    val topAnim = remember { Animatable(0f) }
    val bodyAnim = remember { Animatable(0f) }
    val ctaAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { topAnim.animateTo(1f, tween(420, easing = FastOutSlowInEasing)) }
        launch {
            delay(70)
            bodyAnim.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
        }
        launch {
            delay(130)
            ctaAnim.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = scheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Column(
                modifier = Modifier.graphicsLayer {
                    alpha = topAnim.value
                    translationY = (1f - topAnim.value) * 16f
                },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = FashColors.Primary,
                        )
                    }
                    Text(
                        text = stringResource(R.string.onboarding_sizing_screen_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onSurface,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                }
                OnboardingProgressHeader(
                    displayProgressStep = displayProgressStep,
                    totalSteps = progressTotal,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .graphicsLayer {
                        alpha = bodyAnim.value
                        translationY = (1f - bodyAnim.value) * 20f
                    }
                    .padding(start = FashTheme.spacing.editorialStart, end = FashTheme.spacing.editorialEnd),
            ) {
                Text(
                    text = stringResource(R.string.onboarding_sizing_required_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.onboarding_sizing_optional_body_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.onboarding_sizing_body_first_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = heightCm,
                        onValueChange = onHeightCmChange,
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.onboarding_sizing_optional_height_label)) },
                        placeholder = { Text(stringResource(R.string.onboarding_sizing_optional_height_placeholder)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = scheme.surfaceContainerHighest,
                            unfocusedContainerColor = scheme.surfaceContainerHighest,
                        ),
                    )
                    OutlinedTextField(
                        value = weightKg,
                        onValueChange = onWeightKgChange,
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.onboarding_sizing_optional_weight_label)) },
                        placeholder = { Text(stringResource(R.string.onboarding_sizing_optional_weight_placeholder)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = scheme.surfaceContainerHighest,
                            unfocusedContainerColor = scheme.surfaceContainerHighest,
                        ),
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                ProfileSetupSizingSection(
                    referenceSize = referenceSize,
                    onReferenceSizeChange = onReferenceSizeChange,
                    measurementUnit = measurementUnit,
                    onMeasurementUnitChange = onMeasurementUnitChange,
                    hem = measurementHem,
                    onHemChange = onMeasurementHemChange,
                    chest = measurementChest,
                    onChestChange = onMeasurementChestChange,
                    length = measurementLength,
                    onLengthChange = onMeasurementLengthChange,
                    shoulders = measurementShoulders,
                    onShouldersChange = onMeasurementShouldersChange,
                    sleeve = measurementSleeve,
                    onSleeveChange = onMeasurementSleeveChange,
                    genderPreference = genderPreference,
                    heightCm = heightCm,
                    weightKg = weightKg,
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            Column(
                modifier = Modifier.graphicsLayer {
                    alpha = ctaAnim.value
                    translationY = (1f - ctaAnim.value) * 14f
                },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = FashTheme.spacing.editorialStart, end = FashTheme.spacing.editorialEnd)
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_skip),
                        style = MaterialTheme.typography.labelLarge,
                        color = FashColors.Primary,
                        modifier = Modifier.clickable(onClick = onSkip),
                    )
                }
                FashPrimaryButton(
                    onClick = onComplete,
                    enabled = canSubmit && !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = FashTheme.spacing.editorialStart, end = FashTheme.spacing.editorialEnd)
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = FashColors.Primary.fashReadableOn(),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.onboarding_continue),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileSetupSizingSection(
    referenceSize: String,
    onReferenceSizeChange: (String) -> Unit,
    measurementUnit: String,
    onMeasurementUnitChange: (String) -> Unit,
    hem: String,
    onHemChange: (String) -> Unit,
    chest: String,
    onChestChange: (String) -> Unit,
    length: String,
    onLengthChange: (String) -> Unit,
    shoulders: String,
    onShouldersChange: (String) -> Unit,
    sleeve: String,
    onSleeveChange: (String) -> Unit,
    supportedMeasurementUnits: List<String> = listOf("cm", "in"),
    genderPreference: String = "",
    heightCm: String = "",
    weightKg: String = "",
    compactDensity: Boolean = false,
    showTitle: Boolean = true,
    showSubtitle: Boolean = true,
    titleRes: Int = R.string.profile_setup_sizing_title,
    subtitleRes: Int = R.string.profile_setup_sizing_subtitle,
    referenceSizeLabelRes: Int = R.string.profile_setup_reference_size_label,
    referenceSizeHintRes: Int = R.string.profile_setup_reference_size_hint,
    referenceSizeCustomLabelRes: Int = R.string.profile_setup_reference_size_custom_label,
    enableSizeRecommendation: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val gapTitleToSubtitle = if (compactDensity) 4.dp else 6.dp
    val gapSubtitleToRef = if (compactDensity) 8.dp else 16.dp
    val gapRefToUnitLabel = if (compactDensity) 8.dp else 12.dp
    val gapUnitLabelToToggle = if (compactDensity) 6.dp else 8.dp
    val gapBeforeMeasurements = if (compactDensity) 10.dp else 16.dp
    val gapMeasurementsLabelToFields = if (compactDensity) 6.dp else 8.dp
    val fieldBottom = if (compactDensity) 4.dp else 8.dp

    val parsedHeight = remember(heightCm) { parseHeightCmInput(heightCm) }
    val parsedWeight = remember(weightKg) { parseWeightKgInput(weightKg) }
    val sizeRecommendation = remember(parsedHeight, parsedWeight, genderPreference, enableSizeRecommendation) {
        if (!enableSizeRecommendation) null
        else recommendSizeFromBodyMetrics(parsedHeight, parsedWeight, genderPreference)
    }
    var userDismissedRecommendation by remember { mutableStateOf(false) }
    LaunchedEffect(parsedHeight, parsedWeight) {
        userDismissedRecommendation = false
    }

    val standardSizes = remember(genderPreference) { standardReferenceSizes(genderPreference) }
    val selectedGuide = remember(referenceSize, genderPreference) {
        lookupSizingGuide(referenceSize, genderPreference)
    }
    var customSizeMode by remember(referenceSize, genderPreference) {
        mutableStateOf(
            referenceSize.isNotBlank() && !isStandardReferenceSize(referenceSize, genderPreference),
        )
    }

    if (showTitle) {
        Text(
            text = stringResource(titleRes),
            style = if (compactDensity) {
                MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            } else {
                MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            },
            color = scheme.onSurface,
        )
        Spacer(modifier = Modifier.height(gapTitleToSubtitle))
    }
    if (showSubtitle) {
        Text(
            text = stringResource(subtitleRes),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(gapSubtitleToRef))
    }

    Text(
        text = stringResource(referenceSizeLabelRes),
        style = MaterialTheme.typography.labelMedium,
        color = scheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(8.dp))

    val recommendedSize = sizeRecommendation?.primarySize
    val showRecommendationBanner = sizeRecommendation != null &&
        !userDismissedRecommendation &&
        (referenceSize.isBlank() || !referenceSize.equals(recommendedSize, ignoreCase = true))
    if (showRecommendationBanner) {
        SizingRecommendationBanner(
            recommendation = sizeRecommendation!!,
            heightCm = parsedHeight,
            weightKg = parsedWeight,
            onApply = {
                userDismissedRecommendation = true
                customSizeMode = false
                onReferenceSizeChange(sizeRecommendation.primarySize)
                if (measurementsAreBlank(chest, hem, length, shoulders, sleeve)) {
                    applyTypicalMeasurementsForSize(
                        referenceSize = sizeRecommendation.primarySize,
                        genderPreference = genderPreference,
                        measurementUnit = measurementUnit,
                        onChestChange = onChestChange,
                        onWaistChange = onHemChange,
                        onLengthChange = onLengthChange,
                        onShouldersChange = onShouldersChange,
                        onSleeveChange = onSleeveChange,
                    )
                }
            },
            onDismiss = { userDismissedRecommendation = true },
        )
        Spacer(modifier = Modifier.height(10.dp))
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        standardSizes.forEach { size ->
            val isRecommended = recommendedSize != null &&
                size.equals(recommendedSize, ignoreCase = true) &&
                !userDismissedRecommendation
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FashPillFilterChip(
                    selected = !customSizeMode && referenceSize.equals(size, ignoreCase = true),
                    onClick = {
                        customSizeMode = false
                        userDismissedRecommendation = true
                        onReferenceSizeChange(size)
                    },
                    label = size,
                )
                if (isRecommended && !( !customSizeMode && referenceSize.equals(size, ignoreCase = true))) {
                    Text(
                        text = stringResource(R.string.profile_setup_size_chip_recommended),
                        style = MaterialTheme.typography.labelSmall,
                        color = FashColors.Primary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
        FashPillFilterChip(
            selected = customSizeMode,
            onClick = {
                customSizeMode = true
                if (isStandardReferenceSize(referenceSize, genderPreference)) {
                    onReferenceSizeChange("")
                }
            },
            label = stringResource(R.string.profile_setup_reference_size_custom),
        )
    }
    if (customSizeMode) {
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = referenceSize,
            onValueChange = onReferenceSizeChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(referenceSizeCustomLabelRes)) },
            placeholder = { Text(stringResource(referenceSizeHintRes)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = scheme.surfaceContainerHighest,
                unfocusedContainerColor = scheme.surfaceContainerHighest,
            ),
        )
    }

    if (selectedGuide != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.profile_setup_sizing_inline_hint),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
    }

    Spacer(modifier = Modifier.height(gapRefToUnitLabel))
    Text(
        text = stringResource(R.string.profile_setup_measurement_unit),
        style = MaterialTheme.typography.labelMedium,
        color = scheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(gapUnitLabelToToggle))
    ProfileMeasurementUnitToggle(
        unit = measurementUnit,
        onSelect = onMeasurementUnitChange,
        supportedUnits = supportedMeasurementUnits,
    )
    Spacer(modifier = Modifier.height(gapBeforeMeasurements))
    Text(
        text = stringResource(R.string.profile_setup_measurements_optional),
        style = MaterialTheme.typography.labelMedium,
        color = scheme.onSurfaceVariant,
    )
    Text(
        text = stringResource(R.string.profile_setup_measurements_guideline_hint),
        style = MaterialTheme.typography.bodySmall,
        color = scheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
    if (selectedGuide != null) {
        TextButton(
            onClick = {
                typicalMeasurementsForSize(referenceSize, genderPreference, measurementUnit)?.let { typical ->
                    onChestChange(typical.chest)
                    onHemChange(typical.waist)
                    onLengthChange(typical.length)
                    onShouldersChange(typical.shoulders)
                    onSleeveChange(typical.sleeve)
                }
            },
            modifier = Modifier.padding(top = 2.dp),
        ) {
            Text(stringResource(R.string.profile_setup_fill_typical_measurements))
        }
    }
    Spacer(modifier = Modifier.height(gapMeasurementsLabelToFields))

    var focusedMeasurement by remember { mutableStateOf<SizingMeasurementField?>(null) }
    val chestRange = selectedGuide?.let { formatMeasurementRangeCm(it.chestCm, measurementUnit) }
    val waistRange = selectedGuide?.let { formatMeasurementRangeCm(it.waistCm, measurementUnit) }
    val lengthRange = selectedGuide?.let { formatMeasurementRangeCm(it.lengthCm, measurementUnit) }
    val shouldersRange = selectedGuide?.let { formatMeasurementRangeCm(it.shouldersCm, measurementUnit) }
    val sleeveRange = selectedGuide?.let { formatMeasurementRangeCm(it.sleeveCm, measurementUnit) }

    ProfileMeasurementField(
        label = stringResource(R.string.profile_setup_measurement_chest),
        value = chest,
        onValueChange = onChestChange,
        guideRange = chestRange,
        showGuide = focusedMeasurement == SizingMeasurementField.Chest,
        onFocusChange = { focused -> if (focused) focusedMeasurement = SizingMeasurementField.Chest },
        bottomPadding = fieldBottom,
    )
    ProfileMeasurementField(
        label = stringResource(R.string.profile_setup_measurement_hem),
        value = hem,
        onValueChange = onHemChange,
        guideRange = waistRange,
        showGuide = focusedMeasurement == SizingMeasurementField.Waist,
        onFocusChange = { focused -> if (focused) focusedMeasurement = SizingMeasurementField.Waist },
        bottomPadding = fieldBottom,
    )
    ProfileMeasurementField(
        label = stringResource(R.string.profile_setup_measurement_length),
        value = length,
        onValueChange = onLengthChange,
        guideRange = lengthRange,
        showGuide = focusedMeasurement == SizingMeasurementField.Length,
        onFocusChange = { focused -> if (focused) focusedMeasurement = SizingMeasurementField.Length },
        bottomPadding = fieldBottom,
    )
    ProfileMeasurementField(
        label = stringResource(R.string.profile_setup_measurement_shoulders),
        value = shoulders,
        onValueChange = onShouldersChange,
        guideRange = shouldersRange,
        showGuide = focusedMeasurement == SizingMeasurementField.Shoulders,
        onFocusChange = { focused -> if (focused) focusedMeasurement = SizingMeasurementField.Shoulders },
        bottomPadding = fieldBottom,
    )
    ProfileMeasurementField(
        label = stringResource(R.string.profile_setup_measurement_sleeve),
        value = sleeve,
        onValueChange = onSleeveChange,
        guideRange = sleeveRange,
        showGuide = focusedMeasurement == SizingMeasurementField.Sleeve,
        onFocusChange = { focused -> if (focused) focusedMeasurement = SizingMeasurementField.Sleeve },
        bottomPadding = fieldBottom,
    )
}

private enum class SizingMeasurementField {
    Chest, Waist, Length, Shoulders, Sleeve,
}

@Composable
private fun SizingRecommendationBanner(
    recommendation: SizeRecommendation,
    heightCm: Int?,
    weightKg: Double?,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val bodyText = when {
        heightCm != null && weightKg != null -> stringResource(
            R.string.profile_setup_size_recommendation_banner_both,
            heightCm,
            formatWeightForDisplay(weightKg),
            recommendation.primarySize,
        )
        heightCm != null -> stringResource(
            R.string.profile_setup_size_recommendation_banner_height,
            heightCm,
            recommendation.primarySize,
        )
        weightKg != null -> stringResource(
            R.string.profile_setup_size_recommendation_banner_weight,
            formatWeightForDisplay(weightKg),
            recommendation.primarySize,
        )
        else -> ""
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = FashColors.Primary.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, FashColors.Primary.copy(alpha = 0.22f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.profile_setup_size_recommendation_title),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = FashColors.Primary,
            )
            Text(
                text = bodyText,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
            recommendation.alternateSize?.let { alternate ->
                Text(
                    text = stringResource(
                        R.string.profile_setup_size_recommendation_alternate,
                        alternate,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant.copy(alpha = 0.85f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.profile_setup_size_recommendation_dismiss))
                }
                TextButton(onClick = onApply) {
                    Text(
                        text = stringResource(R.string.profile_setup_size_recommendation_apply),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

private fun formatWeightForDisplay(weightKg: Double): String =
    if (weightKg % 1.0 == 0.0) weightKg.toInt().toString()
    else String.format(java.util.Locale.US, "%.1f", weightKg)

@Composable
private fun ProfileMeasurementUnitToggle(
    unit: String,
    onSelect: (String) -> Unit,
    supportedUnits: List<String> = listOf("cm", "in"),
) {
    val scheme = MaterialTheme.colorScheme
    val labelResByUnit = mapOf(
        "cm" to R.string.profile_setup_unit_cm,
        "in" to R.string.profile_setup_unit_in,
        "st" to R.string.edit_profile_unit_st,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        supportedUnits.forEach { u ->
            val labelRes = labelResByUnit[u] ?: R.string.profile_setup_unit_cm
            val selected = unit.equals(u, ignoreCase = true)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(u.lowercase()) },
                shape = RoundedCornerShape(999.dp),
                color = if (selected) FashColors.Primary.copy(alpha = 0.12f) else scheme.surfaceContainerLow,
                border = if (selected) {
                    BorderStroke(1.5.dp, FashColors.Primary.copy(alpha = 0.82f))
                } else {
                    BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.35f))
                },
            ) {
                Text(
                    text = stringResource(labelRes),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) FashColors.Primary else scheme.onSurface,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ProfileMeasurementField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    guideRange: String? = null,
    showGuide: Boolean = false,
    onFocusChange: (Boolean) -> Unit = {},
    bottomPadding: Dp = 8.dp,
) {
    val scheme = MaterialTheme.colorScheme
    val guideText = if (showGuide && !guideRange.isNullOrBlank()) {
        stringResource(R.string.profile_setup_measurement_reference_range, guideRange)
    } else {
        null
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = bottomPadding)
            .onFocusChanged { onFocusChange(it.isFocused) },
        label = { Text(label) },
        placeholder = if (!showGuide && !guideRange.isNullOrBlank() && value.isEmpty()) {
            { Text(guideRange) }
        } else {
            null
        },
        supportingText = guideText?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = scheme.surfaceContainerHighest,
            unfocusedContainerColor = scheme.surfaceContainerHighest,
        ),
    )
}

package com.pc.fash_android_mobile.ui.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.components.FashPrimaryButton
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.fashReadableOn
import com.pc.fash_android_mobile.ui.theme.FashTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Canvas = androidx.compose.ui.graphics.Color.White

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
    progressStep: Int = 2,
    progressTotal: Int = 3,
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
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
        color = Canvas,
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
                OnboardingProgressBar(
                    currentStep = progressStep,
                    totalSteps = progressTotal,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                    text = stringResource(R.string.onboarding_sizing_optional_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
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

@Composable
internal fun ProfileSetupSizingSection(
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
) {
    val scheme = MaterialTheme.colorScheme
    Text(
        text = stringResource(R.string.profile_setup_sizing_title),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = scheme.onSurface,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = stringResource(R.string.profile_setup_sizing_subtitle),
        style = MaterialTheme.typography.bodySmall,
        color = scheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(
        value = referenceSize,
        onValueChange = onReferenceSizeChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.profile_setup_reference_size_label)) },
        placeholder = { Text(stringResource(R.string.profile_setup_reference_size_hint)) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = scheme.surfaceContainerHighest,
            unfocusedContainerColor = scheme.surfaceContainerHighest,
        ),
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.profile_setup_measurement_unit),
        style = MaterialTheme.typography.labelMedium,
        color = scheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(8.dp))
    ProfileMeasurementUnitToggle(
        unit = measurementUnit,
        onSelect = onMeasurementUnitChange,
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = stringResource(R.string.profile_setup_measurements_optional),
        style = MaterialTheme.typography.labelMedium,
        color = scheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(8.dp))
    ProfileMeasurementField(
        label = stringResource(R.string.profile_setup_measurement_chest),
        value = chest,
        onValueChange = onChestChange,
    )
    ProfileMeasurementField(
        label = stringResource(R.string.profile_setup_measurement_hem),
        value = hem,
        onValueChange = onHemChange,
    )
    ProfileMeasurementField(
        label = stringResource(R.string.profile_setup_measurement_length),
        value = length,
        onValueChange = onLengthChange,
    )
    ProfileMeasurementField(
        label = stringResource(R.string.profile_setup_measurement_shoulders),
        value = shoulders,
        onValueChange = onShouldersChange,
    )
    ProfileMeasurementField(
        label = stringResource(R.string.profile_setup_measurement_sleeve),
        value = sleeve,
        onValueChange = onSleeveChange,
    )
}

@Composable
private fun ProfileMeasurementUnitToggle(
    unit: String,
    onSelect: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf("cm" to R.string.profile_setup_unit_cm, "in" to R.string.profile_setup_unit_in).forEach { (u, labelRes) ->
            val selected = unit.equals(u, ignoreCase = true)
            Surface(
                modifier = Modifier.clickable { onSelect(u) },
                shape = RoundedCornerShape(999.dp),
                color = if (selected) FashColors.Primary.copy(alpha = 0.12f) else scheme.surfaceContainerHighest,
                border = BorderStroke(
                    1.dp,
                    if (selected) FashColors.Primary else scheme.outlineVariant.copy(alpha = 0.72f),
                ),
            ) {
                Text(
                    text = stringResource(labelRes),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) FashColors.Primary else scheme.onSurface,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
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
) {
    val scheme = MaterialTheme.colorScheme
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = scheme.surfaceContainerHighest,
            unfocusedContainerColor = scheme.surfaceContainerHighest,
        ),
    )
}

package com.pc.fash_android_mobile.ui.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
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

private val ProfileSetupCanvas = androidx.compose.ui.graphics.Color(0xFFF9F9F9)
private val InputCorner = RoundedCornerShape(16.dp)
private val AvatarSize = 72.dp

@Composable
fun ProfileSetupScreen(
    modifier: Modifier = Modifier,
    username: String,
    onUsernameChange: (String) -> Unit,
    isUsernameValid: Boolean,
    canSubmit: Boolean,
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
    isSubmitting: Boolean,
    progressStep: Int = 3,
    progressTotal: Int = 3,
    onComplete: () -> Unit,
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

    val infiniteTransition = rememberInfiniteTransition(label = "avatarFloat")
    val avatarFloatY = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "avatarY",
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = ProfileSetupCanvas,
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
                        text = stringResource(R.string.profile_setup_title),
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
                Box(
                    modifier = Modifier
                        .size(AvatarSize)
                        .graphicsLayer { translationY = avatarFloatY.value }
                        .clip(RoundedCornerShape(12.dp))
                        .background(scheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = FashColors.Primary,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.profile_setup_choose_name),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = scheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.profile_setup_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(24.dp))

                UsernameInput(
                    value = username,
                    onValueChange = onUsernameChange,
                    isValid = isUsernameValid,
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (isUsernameValid && username.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = FashColors.Success,
                        )
                        Text(
                            text = stringResource(R.string.profile_setup_username_available),
                            style = MaterialTheme.typography.bodyMedium,
                            color = FashColors.Success,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

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

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(FashTheme.spacing.radiusSoftMin),
                    color = scheme.surfaceContainerHighest,
                    border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.55f)),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = FashColors.Primary.copy(alpha = 0.9f),
                        )
                        Text(
                            text = stringResource(R.string.profile_setup_username_rules),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            Column(
                modifier = Modifier.graphicsLayer {
                    alpha = ctaAnim.value
                    translationY = (1f - ctaAnim.value) * 14f
                },
            ) {
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
                            text = stringResource(R.string.profile_setup_complete),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileSetupSizingSection(
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
                    if (selected) FashColors.Primary else scheme.outlineVariant.copy(alpha = 0.5f),
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

@Composable
private fun UsernameInput(
    value: String,
    onValueChange: (String) -> Unit,
    isValid: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = InputCorner,
        color = scheme.surfaceContainerHighest,
        border = BorderStroke(
            width = if (isValid && value.isNotBlank()) 2.dp else 1.dp,
            color = if (isValid && value.isNotBlank()) FashColors.Primary.copy(alpha = 0.55f)
            else scheme.outlineVariant.copy(alpha = 0.5f),
        ),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "@",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = FashColors.Primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = scheme.onSurface,
                ),
                singleLine = true,
                cursorBrush = SolidColor(FashColors.Primary),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = stringResource(R.string.profile_setup_username_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                        inner()
                    }
                },
            )
            if (isValid && value.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color = FashColors.Success,
                            shape = RoundedCornerShape(12.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = FashColors.Success.fashReadableOn(),
                    )
                }
            }
        }
    }
}

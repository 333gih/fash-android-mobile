package com.pc.fash_android_mobile.ui.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.common.CommonAestheticTagDto
import com.pc.fash_android_mobile.data.common.displayLabel
import com.pc.fash_android_mobile.data.locale.AppLocale
import com.pc.fash_android_mobile.ui.components.FashPillFilterChip
import com.pc.fash_android_mobile.ui.components.FashPrimaryButton
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.fashReadableOn
import com.pc.fash_android_mobile.ui.theme.FashTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    tags: List<CommonAestheticTagDto>,
    selectedIds: Set<String>,
    isLoading: Boolean,
    isSubmitting: Boolean,
    displayProgressStep: Int = 2,
    progressTotal: Int = OnboardingFlowProgress.TOTAL_STEPS,
    onToggleSelection: (CommonAestheticTagDto) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val isVi = AppLocale.currentTag(androidx.compose.ui.platform.LocalContext.current) != AppLocale.TAG_EN

    val topAnim = remember { Animatable(0f) }
    val gridAnim = remember { Animatable(0f) }
    val bottomAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { topAnim.animateTo(1f, tween(420, easing = FastOutSlowInEasing)) }
        launch {
            delay(70)
            gridAnim.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
        }
        launch {
            delay(130)
            bottomAnim.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
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
                .navigationBarsPadding(),
        ) {
            Column(
                modifier = Modifier.graphicsLayer {
                    alpha = topAnim.value
                    translationY = (1f - topAnim.value) * 18f
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
                        text = stringResource(R.string.onboarding_title),
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

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = FashTheme.spacing.editorialStart, end = FashTheme.spacing.editorialEnd),
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_question),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.onboarding_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = gridAnim.value
                            translationY = (1f - gridAnim.value) * 20f
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = FashColors.Primary)
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .graphicsLayer {
                            alpha = gridAnim.value
                            translationY = (1f - gridAnim.value) * 20f
                        }
                        .padding(horizontal = FashTheme.spacing.editorialStart),
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        tags.forEach { tag ->
                            val label = tag.displayLabel(isVi)
                            FashPillFilterChip(
                                selected = selectedIds.contains(tag.id),
                                onClick = { onToggleSelection(tag) },
                                label = label,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.graphicsLayer {
                    alpha = bottomAnim.value
                    translationY = (1f - bottomAnim.value) * 16f
                },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = FashTheme.spacing.editorialStart, end = FashTheme.spacing.editorialEnd)
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_selected_count, selectedIds.size, 3),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.onboarding_skip),
                        style = MaterialTheme.typography.labelLarge,
                        color = FashColors.Primary,
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .clickable(onClick = onSkip),
                    )
                }

                FashPrimaryButton(
                    onClick = onContinue,
                    enabled = !isSubmitting,
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
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }
    }
}

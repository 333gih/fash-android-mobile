package com.pc.fash_android_mobile.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.main.tabs.ProfileCompletionState
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

private enum class PersonalizationStep(
    val titleRes: Int,
    val subtitleRes: Int,
    val icon: ImageVector,
) {
    PHOTO(
        R.string.personalization_step_photo,
        R.string.profile_completion_step_photo,
        Icons.Filled.CameraAlt,
    ),
    TAGS(
        R.string.personalization_step_tags,
        R.string.profile_completion_step_tags,
        Icons.Outlined.Tag,
    ),
    SIZING(
        R.string.personalization_step_sizing,
        R.string.profile_completion_step_sizing,
        Icons.Filled.Straighten,
    ),
    BIO(
        R.string.personalization_step_bio,
        R.string.profile_completion_step_bio,
        Icons.Outlined.Person,
    ),
    FOLLOWING(
        R.string.personalization_step_following,
        R.string.profile_completion_step_follow,
        Icons.Filled.Person,
    ),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalizationScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onOpenEditProfile: () -> Unit,
    onNavigateToExplore: () -> Unit,
    viewModel: PersonalizationViewModel = viewModel(),
) {
    val context = LocalContext.current
    val profile by viewModel.profile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isUploadingPhoto by viewModel.isUploadingPhoto.collectAsState()
    val photoError by viewModel.photoError.collectAsState()
    val completionState = ProfileCompletionState.from(profile)
    val uploadErrorMessage = stringResource(R.string.personalization_upload_error)

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let {
            viewModel.handlePhotoSelection(it, context.contentResolver, uploadErrorMessage)
        }
    }

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.profile_personalization_section),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        if (isLoading && profile == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = FashColors.Primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
            ) {
                ProgressHeader(
                    completionState = completionState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = FashTheme.spacing.editorialStart)
                        .padding(top = FashTheme.spacing.spacing4, bottom = FashTheme.spacing.spacing5),
                )

                PersonalizationStep.entries.forEachIndexed { index, step ->
                    val done = isStepCompleted(step, completionState)
                    StepRow(
                        step = step,
                        done = done,
                        isUploadingPhoto = isUploadingPhoto && step == PersonalizationStep.PHOTO,
                        onClick = {
                            if (!done) {
                                when (step) {
                                    PersonalizationStep.PHOTO -> photoLauncher.launch("image/*")
                                    PersonalizationStep.TAGS,
                                    PersonalizationStep.SIZING,
                                    PersonalizationStep.BIO -> onOpenEditProfile()
                                    PersonalizationStep.FOLLOWING -> onNavigateToExplore()
                                }
                            }
                        },
                    )
                    if (index < PersonalizationStep.entries.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(
                                start = FashTheme.spacing.editorialStart + 52.dp,
                            ),
                        )
                    }
                }

                photoError?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .padding(horizontal = FashTheme.spacing.editorialStart)
                            .padding(top = FashTheme.spacing.spacing3),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressHeader(
    completionState: ProfileCompletionState,
    modifier: Modifier = Modifier,
) {
    val animatedFraction by animateFloatAsState(
        targetValue = completionState.fraction.toFloat(),
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 300f),
        label = "personalization_progress",
    )
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.personalization_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(FashTheme.spacing.spacing2))
        LinearProgressIndicator(
            progress = { animatedFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = FashColors.Primary,
            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
        )
        Spacer(modifier = Modifier.height(FashTheme.spacing.spacing2))
        Text(
            text = stringResource(
                R.string.personalization_progress_format,
                completionState.completedSteps,
                completionState.totalSteps,
            ),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = FashColors.Primary,
        )
    }
}

@Composable
private fun StepRow(
    step: PersonalizationStep,
    done: Boolean,
    isUploadingPhoto: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = FashTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !done && !isUploadingPhoto, onClick = onClick)
            .padding(horizontal = spacing.editorialStart, vertical = spacing.spacing3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (done) FashColors.Primary
                    else MaterialTheme.colorScheme.surfaceContainer,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (done) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Icon(
                    imageVector = step.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(spacing.spacing3))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(step.titleRes),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (done) stringResource(R.string.personalization_step_done)
                else stringResource(step.subtitleRes),
                style = MaterialTheme.typography.bodySmall,
                color = if (done) FashColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (!done) {
            if (isUploadingPhoto) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = FashColors.Primary,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

private fun isStepCompleted(step: PersonalizationStep, state: ProfileCompletionState): Boolean =
    when (step) {
        PersonalizationStep.PHOTO -> state.hasPhoto
        PersonalizationStep.TAGS -> state.hasAestheticTags
        PersonalizationStep.SIZING -> state.hasSizing
        PersonalizationStep.BIO -> state.hasBio
        PersonalizationStep.FOLLOWING -> state.hasFollowing
    }

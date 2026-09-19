package com.pc.fash_android_mobile.ui.main.tabs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.user.ProfileInfo
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

// MARK: - Completion State

data class ProfileCompletionState(
    val hasPhoto: Boolean,
    val hasBio: Boolean,
    val hasAestheticTags: Boolean,
    val hasSizing: Boolean,
    val hasFollowing: Boolean,
) {
    val completedSteps: Int
        get() = listOf(hasPhoto, hasBio, hasAestheticTags, hasSizing, hasFollowing).count { it }

    val totalSteps: Int = 5

    val fraction: Float
        get() = completedSteps.toFloat() / totalSteps.toFloat()

    val isComplete: Boolean
        get() = completedSteps == totalSteps

    companion object {
        fun from(profile: ProfileInfo?): ProfileCompletionState {
            if (profile == null) {
                return ProfileCompletionState(
                    hasPhoto = false, hasBio = false,
                    hasAestheticTags = false, hasSizing = false, hasFollowing = false,
                )
            }
            return ProfileCompletionState(
                hasPhoto = profile.avatarUrl.isNotBlank(),
                hasBio = profile.bio.isNotBlank(),
                hasAestheticTags = profile.aestheticTags.isNotEmpty(),
                hasSizing = profile.sizingReferenceCompleted,
                hasFollowing = profile.followingCount > 0,
            )
        }
    }
}

@Composable
fun ProfileCompletionCard(
    state: ProfileCompletionState,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nextStepLabel = when {
        !state.hasPhoto -> stringResource(R.string.profile_completion_step_photo)
        !state.hasAestheticTags -> stringResource(R.string.profile_completion_step_tags)
        !state.hasSizing -> stringResource(R.string.profile_completion_step_sizing)
        !state.hasBio -> stringResource(R.string.profile_completion_step_bio)
        else -> stringResource(R.string.profile_completion_step_follow)
    }

    val animatedFraction by animateFloatAsState(
        targetValue = state.fraction,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 300f),
        label = "profileCompletionFraction",
    )

    Surface(
        onClick = onAction,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart)
            .padding(top = FashTheme.spacing.spacing3),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FashTheme.spacing.editorialStart)
                .padding(vertical = FashTheme.spacing.spacing3),
            verticalArrangement = Arrangement.spacedBy(FashTheme.spacing.spacing2),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.profile_completion_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${(state.fraction * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = FashColors.Primary,
                )
            }

            LinearProgressIndicator(
                progress = { animatedFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = FashColors.Primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                strokeCap = StrokeCap.Round,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FashTheme.spacing.spacing1),
            ) {
                Text(
                    text = nextStepLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.height(16.dp),
                )
            }
        }
    }
}

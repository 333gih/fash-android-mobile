package com.pc.fash_android_mobile.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.appstatus.MaintenanceResumePresentation
import com.pc.fash_android_mobile.ui.components.FashPrimaryButton
import com.pc.fash_android_mobile.ui.theme.FashColors

@Composable
fun MaintenanceReturnScreen(
    presentation: MaintenanceResumePresentation,
    onExplore: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val title = presentation.releaseNotesTitle?.trim().orEmpty()
        .ifEmpty { stringResource(R.string.maintenance_resume_back_online_title) }
    val noteLines = presentation.noteLines
    val showNotesCard = noteLines.size >= 2
    val body = when {
        showNotesCard -> stringResource(R.string.maintenance_return_apology)
        noteLines.size == 1 -> noteLines.first()
        else -> stringResource(R.string.maintenance_return_apology)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                FashColors.Primary.copy(alpha = 0.12f),
                                scheme.background,
                            ),
                        ),
                    ),
                contentAlignment = Alignment.BottomCenter,
            ) {
                MaintenanceMascotImage(maxHeightDp = 160)
            }
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (showNotesCard) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                        .background(scheme.surfaceContainerHigh, RoundedCornerShape(16.dp))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(R.string.maintenance_resume_whats_new),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onSurfaceVariant,
                    )
                    noteLines.forEach { line ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 7.dp)
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(FashColors.Primary),
                            )
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
        FashPrimaryButton(
            onClick = onExplore,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            Text(stringResource(R.string.maintenance_resume_cta_explore))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            TextButton(onClick = onHome) {
                Text(
                    text = stringResource(R.string.maintenance_return_cta_home),
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

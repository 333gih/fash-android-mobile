package com.pc.fash_android_mobile.ui.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.appstatus.MaintenanceResumePresentation
import com.pc.fash_android_mobile.ui.theme.FashColors
import kotlinx.coroutines.delay

@Composable
fun MaintenanceResumeOverlay(
    presentation: MaintenanceResumePresentation?,
    onDismiss: () -> Unit,
) {
    if (presentation == null || !presentation.isWarningCleared) return

    var visible by remember(presentation.updatedAtToken) { mutableStateOf(true) }
    LaunchedEffect(presentation.updatedAtToken) {
        delay(4_000)
        visible = false
        onDismiss()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(280)),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(220)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                tonalElevation = 6.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, FashColors.Primary.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MaintenanceMascotImage(
                        modifier = Modifier.widthIn(max = 52.dp),
                        maxHeightDp = 52,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.maintenance_resume_warning_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.maintenance_resume_warning_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = {
                        visible = false
                        onDismiss()
                    }) {
                        Text(stringResource(R.string.maintenance_resume_cta_continue))
                    }
                }
            }
        }
    }
}

@Composable
fun MaintenanceReturnGate(
    presentation: MaintenanceResumePresentation?,
    onExplore: () -> Unit,
    onHome: () -> Unit,
) {
    if (presentation == null || !presentation.isBackOnline) return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.02f)),
    ) {
        MaintenanceReturnScreen(
            presentation = presentation,
            onExplore = onExplore,
            onHome = onHome,
        )
    }
}

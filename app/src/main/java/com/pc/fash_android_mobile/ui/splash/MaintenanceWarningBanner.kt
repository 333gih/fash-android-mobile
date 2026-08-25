package com.pc.fash_android_mobile.ui.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.appstatus.AppMaintenanceStatus
import com.pc.fash_android_mobile.ui.theme.FashColors
import kotlinx.coroutines.delay

@Composable
fun MaintenanceWarningBanner(
    status: AppMaintenanceStatus,
    modifier: Modifier = Modifier,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(status.isWarning) {
        while (status.isWarning) {
            delay(1_000)
            now = System.currentTimeMillis()
        }
    }
    val seconds = status.remainingSeconds(now)
    AnimatedVisibility(
        visible = status.isWarning,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(260, easing = FastOutSlowInEasing),
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(220, easing = FastOutSlowInEasing),
        ) + fadeOut(),
        modifier = modifier
            .fillMaxWidth()
            .zIndex(5_000f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(FashColors.Primary)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(
                text = stringResource(R.string.maintenance_warning_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Text(
                text = stringResource(R.string.maintenance_warning_countdown, seconds),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

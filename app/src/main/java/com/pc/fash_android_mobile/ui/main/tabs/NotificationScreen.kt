package com.pc.fash_android_mobile.ui.main.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.components.FashEmptyBulletTipLine
import com.pc.fash_android_mobile.ui.components.FashEmptyState
import com.pc.fash_android_mobile.ui.theme.FashColors

/**
 * In-app notification inbox. Renders an empty state until a real notification feed API exists.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier.fillMaxSize(),
        color = scheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.notifications),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = scheme.onSurface,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                                tint = FashColors.Primary,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = scheme.surface,
                        titleContentColor = scheme.onSurface,
                    ),
                )
            },
        ) { paddingValues ->
            FashEmptyState(
                icon = Icons.Outlined.Notifications,
                title = stringResource(R.string.notification_empty_title),
                subtitle = stringResource(R.string.notification_empty_subtitle),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentDescription = null,
                footer = {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.notification_empty_hints_title),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onSurface,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                    ) {
                        FashEmptyBulletTipLine(text = stringResource(R.string.notification_empty_tip_1))
                        FashEmptyBulletTipLine(text = stringResource(R.string.notification_empty_tip_2))
                        FashEmptyBulletTipLine(text = stringResource(R.string.notification_empty_tip_3))
                    }
                },
            )
        }
    }
}

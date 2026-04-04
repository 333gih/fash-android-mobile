package com.pc.fash_android_mobile.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

/**
 * Depop-style shortcuts: discover inventory, list an item, check orders — always one tap away.
 */
@Composable
fun HomeQuickActionsRow(
    onExplore: () -> Unit,
    onSell: () -> Unit,
    onOrders: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = FashTheme.spacing.editorialStart,
                end = FashTheme.spacing.editorialEnd,
                top = 4.dp,
                bottom = 8.dp,
            ),
    ) {
        Text(
            text = stringResource(R.string.home_quick_actions_title),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StaggeredEntrance(
                index = 0,
                baseDelayMs = 320L,
                modifier = Modifier.weight(1f),
            ) {
                QuickActionCell(
                    label = stringResource(R.string.home_quick_explore),
                    onClick = onExplore,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = FashColors.Primary,
                    )
                }
            }
            StaggeredEntrance(
                index = 1,
                baseDelayMs = 320L,
                modifier = Modifier.weight(1f),
            ) {
                QuickActionCell(
                    label = stringResource(R.string.home_quick_sell),
                    onClick = onSell,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = FashColors.Primary,
                    )
                }
            }
            StaggeredEntrance(
                index = 2,
                baseDelayMs = 320L,
                modifier = Modifier.weight(1f),
            ) {
                QuickActionCell(
                    label = stringResource(R.string.home_quick_orders),
                    onClick = onOrders,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalMall,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = FashColors.Primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionCell(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(FashTheme.spacing.radiusSoftMin)
    Column(
        modifier = modifier
            .clip(shape)
            .background(scheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        icon()
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

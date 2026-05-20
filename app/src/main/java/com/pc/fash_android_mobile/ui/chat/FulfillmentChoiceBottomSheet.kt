package com.pc.fash_android_mobile.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.theme.FashColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FulfillmentChoiceBottomSheet(
    onDismiss: () -> Unit,
    /** Opens the existing meetup scheduling sheet (caller sets visibility). */
    onChooseMeetup: () -> Unit,
    /** Opens ship flow when [shipFulfillmentEnabled] is true. */
    onChooseShip: () -> Unit,
    shipFulfillmentEnabled: Boolean,
    /** Buyer can cancel unpaid order from this sheet (payment_pending). */
    orderCancellable: Boolean = false,
    onCancelOrder: (() -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.chat_fulfillment_sheet_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (shipFulfillmentEnabled) {
                    stringResource(R.string.chat_fulfillment_sheet_subtitle)
                } else {
                    stringResource(R.string.chat_fulfillment_sheet_subtitle_ship_disabled)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            FulfillmentOptionRow(
                icon = Icons.Outlined.Event,
                title = stringResource(R.string.chat_fulfillment_option_meetup_title),
                subtitle = stringResource(R.string.chat_fulfillment_option_meetup_subtitle),
                onClick = {
                    onDismiss()
                    onChooseMeetup()
                },
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
            )
            FulfillmentOptionRow(
                icon = Icons.Outlined.LocalShipping,
                title = stringResource(R.string.chat_fulfillment_option_ship_title),
                subtitle = if (shipFulfillmentEnabled) {
                    stringResource(R.string.chat_fulfillment_option_ship_subtitle)
                } else {
                    stringResource(R.string.chat_fulfillment_option_ship_coming_soon)
                },
                badge = if (!shipFulfillmentEnabled) stringResource(R.string.common_coming_soon) else null,
                enabled = shipFulfillmentEnabled,
                dimmed = !shipFulfillmentEnabled,
                onClick = {
                    if (!shipFulfillmentEnabled) return@FulfillmentOptionRow
                    onDismiss()
                    onChooseShip()
                },
            )
            if (orderCancellable && onCancelOrder != null) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
                )
                FulfillmentOptionRow(
                    icon = Icons.Outlined.Cancel,
                    title = stringResource(R.string.chat_fulfillment_cancel_order_title),
                    subtitle = stringResource(R.string.chat_fulfillment_cancel_order_subtitle),
                    onClick = {
                        onDismiss()
                        onCancelOrder.invoke()
                    },
                    accentDestructive = true,
                )
            }
        }
    }
}

@Composable
private fun FulfillmentOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    dimmed: Boolean = false,
    badge: String? = null,
    accentDestructive: Boolean = false,
) {
    val alpha = if (dimmed) 0.55f else 1f
    val iconTint = when {
        accentDestructive -> MaterialTheme.colorScheme.error.copy(alpha = alpha)
        else -> FashColors.Primary.copy(alpha = alpha)
    }
    val titleColor = when {
        accentDestructive -> MaterialTheme.colorScheme.error.copy(alpha = alpha)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(28.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = titleColor,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    badge?.let { label ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = alpha),
                            shape = RoundedCornerShape(6.dp),
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = alpha),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                )
            }
        }
    }
}

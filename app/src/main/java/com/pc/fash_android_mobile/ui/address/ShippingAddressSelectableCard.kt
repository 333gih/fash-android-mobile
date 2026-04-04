package com.pc.fash_android_mobile.ui.address

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.address.ShippingAddress
import com.pc.fash_android_mobile.ui.theme.FashColors

/**
 * Card row for choosing a saved shipping address (profile, checkout, post listing).
 * Selected: primary border + light tint; [onSetDefault] shown only when [showSetDefaultButton] is true.
 */
@Composable
fun ShippingAddressSelectableCard(
    address: ShippingAddress,
    selected: Boolean,
    onClick: () -> Unit,
    onSetDefault: (() -> Unit)?,
    modifier: Modifier = Modifier,
    showSetDefaultButton: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val fallbackName = stringResource(R.string.shipping_address_row_untitled)
    val detail = listOf(address.line1, address.ward, address.district, address.city)
        .filter { it.isNotBlank() }
        .joinToString(", ")
        .ifBlank { address.formattedSingleLine() }
    val nameOrLabel = address.recipientName.trim().ifBlank { address.label.trim() }
    val titleText = nameOrLabel.ifBlank { detail }.ifBlank { fallbackName }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            FashColors.Primary.copy(alpha = 0.10f)
        } else {
            scheme.surfaceContainerLow
        },
        border = if (selected) {
            BorderStroke(1.5.dp, FashColors.Primary.copy(alpha = 0.85f))
        } else {
            null
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onSurface,
                    )
                    if (address.isDefault) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = FashColors.Primary.copy(alpha = 0.12f),
                        ) {
                            Text(
                                text = stringResource(R.string.address_badge_default),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = FashColors.Primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                if (address.label.isNotBlank() && address.recipientName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = address.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
                if (address.phone.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = address.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = address.formattedSingleLine(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurface,
                )
                if (showSetDefaultButton && onSetDefault != null && !address.isDefault) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onSetDefault) {
                        Text(
                            text = stringResource(R.string.address_set_default),
                            color = FashColors.Primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            Icon(
                imageVector = if (selected) Icons.Filled.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) FashColors.Primary else scheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

package com.pc.fash_android_mobile.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.theme.FashColors

/** Same cap style as chat unread badges ([formatUnreadBadgeCount] in chat package). */
private fun inboxBadgeLabel(count: Int): String =
    when {
        count <= 0 -> ""
        count > 99 -> "99+"
        else -> count.toString()
    }

/**
 * Top-bar notification entry: optional numeric badge when [unreadCount] &gt; 0, clear state when zero.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FashInboxNotificationIconButton(
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val label = inboxBadgeLabel(unreadCount)
    val cd = if (unreadCount > 0) {
        stringResource(R.string.notifications_cd_with_unread, unreadCount)
    } else {
        stringResource(R.string.notifications_cd_none)
    }
    BadgedBox(
        modifier = modifier.semantics { contentDescription = cd },
        badge = {
            if (unreadCount > 0) {
                Badge(
                    containerColor = FashColors.Primary,
                    contentColor = Color.White,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
            }
        },
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = scheme.onSurface,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

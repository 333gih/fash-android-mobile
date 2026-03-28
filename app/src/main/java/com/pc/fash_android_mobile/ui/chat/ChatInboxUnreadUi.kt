package com.pc.fash_android_mobile.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MarkChatUnread
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.fashReadableOn

/** Caps badge label for very large counts. */
internal fun formatUnreadBadgeCount(count: Int): String =
    when {
        count <= 0 -> ""
        count > 99 -> "99+"
        else -> count.toString()
    }

/**
 * Inbox summary strip: total unread from [GET /chat/unread] (synced with tab badge).
 */
@Composable
fun ChatInboxUnreadBanner(
    unreadTotal: Int,
    modifier: Modifier = Modifier,
) {
    if (unreadTotal <= 0) return
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = FashColors.Primary.copy(alpha = 0.10f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.MarkChatUnread,
                contentDescription = null,
                tint = FashColors.Primary,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = stringResource(R.string.chat_inbox_unread_banner, unreadTotal),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * Avatar with numeric unread badge (or dot when has unread but count not provided).
 */
@Composable
fun ChatConversationAvatarWithUnread(
    hasUnread: Boolean,
    unreadCount: Int,
    modifier: Modifier = Modifier,
    avatar: @Composable () -> Unit,
) {
    val badgeLabel = formatUnreadBadgeCount(unreadCount)
    val cd = if (unreadCount > 0) {
        stringResource(R.string.chat_unread_messages_cd, unreadCount)
    } else if (hasUnread) {
        stringResource(R.string.chat_has_unread_cd)
    } else {
        null
    }
    BadgedBox(
        modifier = modifier.then(
            if (cd != null) Modifier.semantics { contentDescription = cd } else Modifier,
        ),
        badge = {
            when {
                unreadCount > 0 -> {
                    Badge(
                        containerColor = FashColors.Primary,
                    ) {
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = FashColors.Primary.fashReadableOn(),
                        )
                    }
                }
                hasUnread -> Badge(containerColor = FashColors.Primary) { }
            }
        },
    ) {
        avatar()
    }
}

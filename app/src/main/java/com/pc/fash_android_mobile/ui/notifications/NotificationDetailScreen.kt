package com.pc.fash_android_mobile.ui.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.user.InboxNotificationItem
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDetailScreen(
    item: InboxNotificationItem,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onOpenOrder: (String) -> Unit = {},
    onOpenListing: (String, String?) -> Unit = { _, _ -> },
    onOpenChat: (String) -> Unit = {},
    onOpenFollowConnections: (Int) -> Unit = {},
    onOpenExplore: () -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    val actions = parseNotificationDetailActions(item)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.notification_detail_title),
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FashTheme.spacing.editorialStart, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            actions.imageUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(FashTheme.spacing.radiusCard)),
                    contentScale = ContentScale.Crop,
                )
            }

            Text(
                text = item.title.ifBlank { stringResource(R.string.notification_detail_no_title) },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurface,
            )
            Text(
                text = item.body,
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurface,
            )

            actions.richDetailBody?.takeIf { it.isNotBlank() && it != item.body }?.let { extra ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.notification_detail_more_label),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = scheme.onSurface,
                        )
                        Text(
                            text = extra,
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurface,
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.notification_detail_time,
                    formatNotificationInstant(item.createdAtIso),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
            if (!item.readAtIso.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.notification_detail_read_at, formatNotificationInstant(item.readAtIso)),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = scheme.outlineVariant.copy(alpha = 0.35f),
            )
            item.payloadType?.let { pt ->
                MetaRow(label = stringResource(R.string.notification_detail_payload_type), value = pt)
            }
            item.source?.let { s ->
                MetaRow(label = stringResource(R.string.notification_detail_source), value = s)
            }
            item.sourceEventId?.let { sid ->
                MetaRow(label = stringResource(R.string.notification_detail_source_event), value = sid)
            }
            val data = item.dataMap
            if (!data.isNullOrEmpty()) {
                Text(
                    text = stringResource(R.string.notification_detail_payload_title),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                )
                Surface(
                    color = scheme.surfaceContainerHighest,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        text = data.entries.joinToString("\n") { (k, v) -> "$k: ${v ?: "—"}" },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                        color = scheme.onSurface,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.notification_detail_actions_heading),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurface,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (!actions.orderId.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = { onOpenOrder(actions.orderId!!) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.notification_action_open_order))
                    }
                }
                if (!actions.listingId.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = { onOpenListing(actions.listingId!!, actions.sellerUserId) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.notification_action_open_listing))
                    }
                }
                if (!actions.conversationId.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = { onOpenChat(actions.conversationId!!) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.notification_action_open_chat))
                    }
                }
                if (actions.openFollowersTab) {
                    OutlinedButton(
                        onClick = { onOpenFollowConnections(1) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.notification_action_open_followers))
                    }
                }
                if (actions.openFollowingTab) {
                    OutlinedButton(
                        onClick = { onOpenFollowConnections(0) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.notification_action_open_following))
                    }
                }
                if (actions.openExploreTab) {
                    OutlinedButton(
                        onClick = onOpenExplore,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.notification_action_open_explore))
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun formatNotificationInstant(iso: String): String =
    runCatching {
        val instant = Instant.parse(iso)
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .format(instant.atZone(ZoneId.systemDefault()))
    }.getOrDefault(iso)

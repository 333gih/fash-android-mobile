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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val payloadLines = remember(item.id, item.dataMap, item.payloadType) { buildFriendlyPayloadLines(item) }
    val rawPayloadText = remember(item.id, item.dataMap) { buildRawPayloadDump(item.dataMap) }

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

            if (!item.dataMap.isNullOrEmpty() && (payloadLines.isNotEmpty() || rawPayloadText.isNotBlank())) {
                FriendlyPayloadCard(
                    lines = payloadLines,
                    rawDump = rawPayloadText,
                )
            }
        }
    }
}

private sealed interface FriendlyPayloadLine {
    data class TextLine(val labelRes: Int, val value: String) : FriendlyPayloadLine
    data class NavLine(val labelRes: Int, val rawNav: String) : FriendlyPayloadLine
}

@Composable
private fun FriendlyPayloadCard(
    lines: List<FriendlyPayloadLine>,
    rawDump: String,
) {
    val scheme = MaterialTheme.colorScheme
    var rawOpen by remember { mutableStateOf(false) }
    val showRawToggle = rawDump.isNotBlank()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.notification_detail_payload_title),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurface,
            )
            if (lines.isEmpty() && showRawToggle) {
                Text(
                    text = stringResource(R.string.notification_data_raw_only_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            } else if (lines.isNotEmpty()) {
                lines.forEachIndexed { i, line ->
                    if (i > 0) {
                        HorizontalDivider(
                            color = scheme.outlineVariant.copy(alpha = 0.25f),
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                    when (line) {
                        is FriendlyPayloadLine.TextLine -> PayloadLabeledValue(
                            label = stringResource(line.labelRes),
                            value = line.value,
                        )
                        is FriendlyPayloadLine.NavLine -> PayloadLabeledValue(
                            label = stringResource(line.labelRes),
                            value = navTargetDisplay(line.rawNav),
                        )
                    }
                }
            }
            if (showRawToggle) {
                TextButton(
                    onClick = { rawOpen = !rawOpen },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (rawOpen) stringResource(R.string.notification_data_hide_raw)
                        else stringResource(R.string.notification_data_show_raw),
                    )
                }
                if (rawOpen) {
                    Surface(
                        color = scheme.surfaceContainerHighest,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            text = rawDump,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 18.sp,
                            ),
                            modifier = Modifier.padding(12.dp),
                            color = scheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PayloadLabeledValue(label: String, value: String) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
            ),
            color = scheme.onSurface,
        )
    }
}

@Composable
private fun navTargetDisplay(raw: String): String {
    val key = raw.trim().lowercase(Locale.ROOT)
    return when (key) {
        "listing" -> stringResource(R.string.notification_data_nav_listing)
        "order" -> stringResource(R.string.notification_data_nav_order)
        "chat" -> stringResource(R.string.notification_data_nav_chat)
        "followers_tab" -> stringResource(R.string.notification_data_nav_followers_tab)
        "following_tab" -> stringResource(R.string.notification_data_nav_following_tab)
        "explore_tab" -> stringResource(R.string.notification_data_nav_explore_tab)
        else -> if (key.isEmpty()) "—" else stringResource(R.string.notification_data_nav_generic, raw.trim())
    }
}

private fun buildFriendlyPayloadLines(item: InboxNotificationItem): List<FriendlyPayloadLine> {
    val data = item.dataMap ?: return emptyList()
    val pt = item.payloadType?.trim().orEmpty()
    val lines = mutableListOf<FriendlyPayloadLine>()

    fun str(vararg keys: String): String? = firstStringFromDataCi(data, *keys)

    str("listing_id", "listingId")?.let {
        lines.add(FriendlyPayloadLine.TextLine(R.string.notification_data_label_listing, it))
    }
    str("seller_user_id", "sellerUserId", "seller_id", "sellerId")?.let {
        lines.add(FriendlyPayloadLine.TextLine(R.string.notification_data_label_seller, it))
    }
    val orderVal = str("marketplace_order_id", "order_id", "orderId")
    if (!orderVal.isNullOrBlank()) {
        lines.add(FriendlyPayloadLine.TextLine(R.string.notification_data_label_order, orderVal))
    }
    str("conversation_id", "conversationId")?.let {
        lines.add(FriendlyPayloadLine.TextLine(R.string.notification_data_label_conversation, it))
    }
    str("tracking_number", "trackingNumber")?.let {
        lines.add(FriendlyPayloadLine.TextLine(R.string.notification_data_label_tracking, it))
    }
    str("nav_target", "navTarget")?.let {
        lines.add(FriendlyPayloadLine.NavLine(R.string.notification_data_label_nav, it))
    }
    str("follower_user_id", "followerUserId")?.let {
        lines.add(FriendlyPayloadLine.TextLine(R.string.notification_data_label_follower, it))
    }
    str("followee_id", "followeeId")?.let {
        lines.add(FriendlyPayloadLine.TextLine(R.string.notification_data_label_followee, it))
    }
    str("screen")?.let {
        lines.add(FriendlyPayloadLine.TextLine(R.string.notification_data_label_screen, it))
    }
    str("event")?.let {
        lines.add(FriendlyPayloadLine.TextLine(R.string.notification_data_label_event, it))
    }
    str("type")?.let { t ->
        if (pt.isEmpty() || !t.equals(pt, ignoreCase = true)) {
            lines.add(FriendlyPayloadLine.TextLine(R.string.notification_detail_payload_type, t))
        }
    }
    return lines
}

/** Keys stored for FCM / inbox plumbing — shown only in optional raw dump. */
private val internalPayloadKeysLowercase: Set<String> = setOf(
    "deep_link",
    "user_notification_id",
    "inbox_refresh",
    "title",
    "body",
    "channel_id",
    "notification_image_url",
    "image_url",
    "detail_body",
    "detailbody",
    "rich_body",
    "richbody",
)

private fun buildRawPayloadDump(data: Map<String, Any?>?): String {
    if (data.isNullOrEmpty()) return ""
    return data.entries
        .filter { (k, _) -> k.lowercase(Locale.ROOT) !in internalPayloadKeysLowercase }
        .sortedBy { it.key.lowercase(Locale.ROOT) }
        .joinToString("\n") { (k, v) ->
            val disp = when (v) {
                null -> "—"
                is String -> v
                is Number -> v.toString()
                else -> v.toString()
            }
            "$k: $disp"
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

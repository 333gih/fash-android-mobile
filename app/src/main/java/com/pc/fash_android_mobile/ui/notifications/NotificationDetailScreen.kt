package com.pc.fash_android_mobile.ui.notifications

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.promo.AppPromoCampaign
import com.pc.fash_android_mobile.data.promo.AppPromoNavigation
import com.pc.fash_android_mobile.data.promo.sanitizePromoDisplayString
import com.pc.fash_android_mobile.data.user.InboxNotificationItem
import com.pc.fash_android_mobile.ui.components.FashAsyncImage
import com.pc.fash_android_mobile.ui.components.FashPromoPageIndicator
import com.pc.fash_android_mobile.ui.main.MainTab
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
    onOpenInviteFriends: () -> Unit = {},
    onPromoMainTab: (MainTab) -> Unit = {},
    onPromoOpenOrders: () -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val activity = context as? Activity
    val promoCampaign = remember(item.id, item.dataMap, item.payloadType) {
        parseAppPromoCampaignFromInbox(item)
    }
    val actions = parseNotificationDetailActions(item)
    val payloadLines = remember(item.id, item.dataMap, item.payloadType) {
        buildFriendlyPayloadLines(item)
    }
    val rawPayloadText = remember(item.id, item.dataMap) {
        buildRawPayloadDump(item.dataMap)
    }
    val displayTitle = promoCampaign?.remoteTitle?.takeIf { it.isNotBlank() }
        ?: item.title.ifBlank { stringResource(R.string.notification_detail_no_title) }
    val displayBody = promoCampaign?.remoteMessage?.takeIf { it.isNotBlank() }
        ?: item.body

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
            if (promoCampaign != null) {
                NotificationPromoMediaSection(campaign = promoCampaign)
            } else {
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
            }

            sanitizePromoDisplayString(promoCampaign?.remoteBadge)?.let { badge ->
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = scheme.primary.copy(alpha = 0.12f),
                ) {
                    Text(
                        text = badge,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.primary,
                    )
                }
            }

            Text(
                text = displayTitle,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurface,
            )
            Text(
                text = displayBody,
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurface,
            )

            if (promoCampaign != null) {
                NotificationPromoCtaSection(
                    campaign = promoCampaign,
                    activity = activity,
                    onAfterNavigate = onBack,
                    onPromoMainTab = onPromoMainTab,
                    onPromoOpenOrders = onPromoOpenOrders,
                )
            }

            actions.richDetailBody?.takeIf {
                promoCampaign == null && it.isNotBlank() && it != item.body
            }?.let { extra ->
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
                if (actions.openInviteFriends) {
                    OutlinedButton(
                        onClick = onOpenInviteFriends,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.notification_action_open_invite_friends))
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = scheme.outlineVariant.copy(alpha = 0.35f),
            )

            item.payloadType?.let { pt ->
                val res = inboxPayloadTypeStringRes(pt)
                MetaRow(
                    label = stringResource(R.string.notification_detail_payload_type),
                    value = if (res != null) stringResource(res) else pt,
                )
            }
            item.source?.let { s ->
                val res = inboxSourceStringRes(s)
                MetaRow(
                    label = stringResource(R.string.notification_detail_source),
                    value = if (res != null) stringResource(res) else s,
                )
            }
            item.sourceEventId?.let { sid ->
                MetaRow(label = stringResource(R.string.notification_detail_source_event), value = sid)
            }

            if (
                promoCampaign == null &&
                !item.dataMap.isNullOrEmpty() &&
                (payloadLines.isNotEmpty() || rawPayloadText.isNotBlank())
            ) {
                FriendlyPayloadCard(
                    lines = payloadLines,
                    rawDump = rawPayloadText,
                )
            }
        }
    }
}

private sealed interface LineValue {
    data class Plain(val text: String) : LineValue
    data class TitleWithId(val title: String, val id: String) : LineValue
}

private sealed interface FriendlyPayloadLine {
    data class TextLine(val labelRes: Int, val value: LineValue) : FriendlyPayloadLine
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
                        is FriendlyPayloadLine.TextLine -> PayloadLabeledLine(
                            label = stringResource(line.labelRes),
                            value = line.value,
                        )
                        is FriendlyPayloadLine.NavLine -> PayloadLabeledPlain(
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
private fun PayloadLabeledPlain(label: String, value: String) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurface,
        )
    }
}

@Composable
private fun PayloadLabeledLine(label: String, value: LineValue) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurfaceVariant,
        )
        when (value) {
            is LineValue.Plain -> Text(
                text = value.text,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurface,
            )
            is LineValue.TitleWithId -> Text(
                text = stringResource(R.string.notification_data_title_with_id, value.title, value.id),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurface,
            )
        }
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

private fun lineValueTitleOrPlain(title: String?, id: String?): LineValue {
    val t = title?.trim().orEmpty()
    val i = id?.trim().orEmpty()
    return when {
        t.isNotEmpty() && i.isNotEmpty() && t.equals(i, ignoreCase = true) -> LineValue.Plain(t)
        t.isNotEmpty() && i.isNotEmpty() -> LineValue.TitleWithId(t, i)
        t.isNotEmpty() -> LineValue.Plain(t)
        i.isNotEmpty() -> LineValue.Plain(i)
        else -> LineValue.Plain("—")
    }
}

private fun buildFriendlyPayloadLines(item: InboxNotificationItem): List<FriendlyPayloadLine> {
    if (isAppPromoInboxNotification(item)) return emptyList()
    val data = item.dataMap ?: return emptyList()
    val pt = item.payloadType?.trim().orEmpty()
    val lines = mutableListOf<FriendlyPayloadLine>()

    fun str(vararg keys: String): String? = firstStringFromDataCi(data, *keys)
    fun addText(labelRes: Int, value: LineValue) {
        lines.add(FriendlyPayloadLine.TextLine(labelRes, value))
    }

    val listingTitle = str("listing_title", "listingTitle")
    val listingId = str("listing_id", "listingId")
    if (!listingTitle.isNullOrBlank() || !listingId.isNullOrBlank()) {
        addText(R.string.notification_data_label_listing, lineValueTitleOrPlain(listingTitle, listingId))
    }

    val sellerName = str("seller_display_name", "sellerDisplayName")
    val sellerId = str("seller_user_id", "sellerUserId", "seller_id", "sellerId")
    if (!sellerName.isNullOrBlank() || !sellerId.isNullOrBlank()) {
        addText(R.string.notification_data_label_seller, lineValueTitleOrPlain(sellerName, sellerId))
    }

    str("buyer_display_name", "buyerDisplayName")?.trim()?.takeIf { it.isNotEmpty() }?.let {
        addText(R.string.notification_data_label_buyer, LineValue.Plain(it))
    }

    str("marketplace_order_id", "order_id", "orderId")?.takeIf { it.isNotBlank() }?.let {
        addText(R.string.notification_data_label_order, LineValue.Plain(it))
    }

    val preview = str("message_preview", "messagePreview")
    val conv = str("conversation_id", "conversationId")
    when {
        !preview.isNullOrBlank() && !conv.isNullOrBlank() ->
            addText(R.string.notification_data_label_chat_message, LineValue.TitleWithId(preview, conv))
        !preview.isNullOrBlank() ->
            addText(R.string.notification_data_label_chat_message, LineValue.Plain(preview))
        !conv.isNullOrBlank() ->
            addText(R.string.notification_data_label_conversation, LineValue.Plain(conv))
    }

    str("tracking_number", "trackingNumber")?.takeIf { it.isNotBlank() }?.let {
        addText(R.string.notification_data_label_tracking, LineValue.Plain(it))
    }

    str("nav_target", "navTarget")?.takeIf { it.isNotBlank() }?.let {
        lines.add(FriendlyPayloadLine.NavLine(R.string.notification_data_label_nav, it))
    }

    str("follower_names_summary", "followerNamesSummary")?.takeIf { it.isNotBlank() }?.let {
        addText(R.string.notification_data_label_follower_batch_names, LineValue.Plain(it))
    }

    val followerName = str("follower_display_name", "followerDisplayName")?.takeIf { it.isNotBlank() }
        ?: if (pt.equals("marketplace.follower.new", ignoreCase = true) && item.title.isNotBlank()) {
            item.title.trim()
        } else {
            null
        }
    val followerId = str("follower_user_id", "followerUserId")
    if (!followerName.isNullOrBlank() || !followerId.isNullOrBlank()) {
        addText(R.string.notification_data_label_follower, lineValueTitleOrPlain(followerName, followerId))
    }

    str("reviewer_display_name", "reviewerDisplayName")?.takeIf { it.isNotBlank() }?.let {
        addText(R.string.notification_data_label_reviewer, LineValue.Plain(it))
    }

    str("star_count", "starCount")?.trim()?.toIntOrNull()?.let { n ->
        addText(R.string.notification_data_label_rating_stars, LineValue.Plain("$n/5 ★"))
    }

    str("event")?.takeIf { it.isNotBlank() }?.let {
        addText(R.string.notification_data_label_event, LineValue.Plain(it))
    }

    str("type")?.let { t ->
        if (pt.isEmpty() || !t.equals(pt, ignoreCase = true)) {
            addText(R.string.notification_detail_payload_type, LineValue.Plain(t))
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
    "listing_title",
    "listingtitle",
    "seller_display_name",
    "sellerdisplayname",
    "buyer_display_name",
    "buyerdisplayname",
    "follower_display_name",
    "followerdisplayname",
    "follower_names_summary",
    "followernamessummary",
    "message_preview",
    "messagepreview",
    "reviewer_display_name",
    "reviewerdisplayname",
    "star_count",
    "starcount",
    "screen",
    "event",
    "promo_payload",
    "promopayload",
    "campaign",
    "campaign_id",
    "campaignid",
    "campaign_version",
    "image_urls",
    "imageurls",
    "badge_label",
    "badgelabel",
    "primary_button",
    "secondary_button",
    "primary_button_label",
    "secondary_button_label",
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

@Composable
private fun NotificationPromoMediaSection(campaign: AppPromoCampaign) {
    val urls = campaign.remoteImageUrls.filter { it.isNotBlank() }
    if (urls.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(FashTheme.spacing.radiusCard)
    if (urls.size == 1) {
        FashAsyncImage(
            model = urls.first(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(shape),
            contentScale = ContentScale.Crop,
        )
        return
    }
    val pagerState = rememberPagerState(pageCount = { urls.size })
    val currentPage = pagerState.currentPage
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(shape),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            FashAsyncImage(
                model = urls[page],
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(40.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)),
                    ),
                ),
        )
        FashPromoPageIndicator(
            pageCount = urls.size,
            currentPage = currentPage,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp),
        )
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp),
            shape = RoundedCornerShape(10.dp),
            color = Color.Black.copy(alpha = 0.55f),
        ) {
            Text(
                text = stringResource(
                    R.string.notification_detail_promo_gallery_cd,
                    currentPage + 1,
                    urls.size,
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )
        }
    }
}

@Composable
private fun NotificationPromoCtaSection(
    campaign: AppPromoCampaign,
    activity: Activity?,
    onAfterNavigate: () -> Unit,
    onPromoMainTab: (MainTab) -> Unit,
    onPromoOpenOrders: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val primary = campaign.remotePrimaryLabel?.trim().orEmpty()
    val secondary = sanitizePromoDisplayString(campaign.remoteSecondaryLabel)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        if (primary.isNotEmpty() && campaign.primaryAction != null && activity != null) {
            Button(
                onClick = {
                    AppPromoNavigation.applyPrimary(
                        activity = activity,
                        campaign = campaign,
                        onTab = { tab ->
                            onPromoMainTab(tab)
                            onAfterNavigate()
                        },
                        onOpenOrders = {
                            onPromoOpenOrders()
                            onAfterNavigate()
                        },
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.primary,
                    contentColor = scheme.onPrimary,
                ),
            ) {
                Text(
                    text = primary,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
        if (!secondary.isNullOrBlank() && campaign.secondaryAction != null && activity != null) {
            TextButton(
                onClick = {
                    AppPromoNavigation.applySecondary(
                        activity = activity,
                        campaign = campaign,
                        onTab = { tab ->
                            onPromoMainTab(tab)
                            onAfterNavigate()
                        },
                        onOpenOrders = {
                            onPromoOpenOrders()
                            onAfterNavigate()
                        },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatNotificationInstant(iso: String): String =
    runCatching {
        val instant = Instant.parse(iso)
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .format(instant.atZone(ZoneId.systemDefault()))
    }.getOrDefault(iso)

package com.pc.fash_android_mobile.ui.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.address.ShippingAddress
import com.pc.fash_android_mobile.data.order.OrderBuyerReview
import com.pc.fash_android_mobile.data.order.OrderDetail
import com.pc.fash_android_mobile.data.order.OrderMeetingAppointment
import com.pc.fash_android_mobile.data.order.OrderMeetingGrace
import com.pc.fash_android_mobile.data.order.sellerShowsConfirmHandoffCta
import com.pc.fash_android_mobile.ui.components.FashAsyncImage
import com.pc.fash_android_mobile.ui.components.FashProfileAvatarImage
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.fashReadableOn
import com.pc.fash_android_mobile.ui.theme.FashTheme

internal enum class OrderViewerRole {
    Buyer,
    Seller,
    /** Neither buyer nor seller matched session — read-only. */
    Viewer,
}

/**
 * Bottom padding for the scroll column so the last cards (tracking, product, etc.) sit fully above
 * the sticky action bar (primary + secondary rows + safe area).
 */
internal val OrderDetailStickyScrollBottomInset = 232.dp

/** After a buyer-cancelled order: explains chat negotiation + offer limit; optional shortcut to thread. */
@Composable
internal fun BuyerCancelledNegotiationHint(
    listingStatus: String,
    maxOffersPerConversation: Int,
    conversationId: String,
    onOpenChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sold = listingStatus.trim().equals("sold", ignoreCase = true)
    val scheme = MaterialTheme.colorScheme
    val (container, onContainer) = when {
        sold -> scheme.surfaceContainerHighest to scheme.onSurfaceVariant
        else -> scheme.primaryContainer.copy(alpha = 0.38f) to scheme.onPrimaryContainer
    }
    val text = if (sold) {
        stringResource(R.string.order_cancelled_buyer_hint_sold)
    } else {
        stringResource(R.string.order_cancelled_buyer_hint_active, maxOffersPerConversation)
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = container,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = null,
                    tint = onContainer,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = onContainer,
                )
            }
            if (!sold && conversationId.isNotBlank()) {
                TextButton(onClick = onOpenChat) {
                    Text(
                        text = stringResource(R.string.order_cancelled_buyer_open_chat),
                        color = FashColors.Primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
internal fun OrderHeroCard(
    d: OrderDetail,
    role: OrderViewerRole,
    formatDate: (String) -> String,
) {
    val scheme = MaterialTheme.colorScheme
    val st = d.status.trim().lowercase()
    val (title, subtitle, accent) = heroContent(d, role, st, formatDate)
    val (container, onContainer) = when (st) {
        "cancelled" -> scheme.errorContainer to scheme.onErrorContainer
        "disputed" -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        else -> scheme.surface to scheme.onSurface
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
        color = container,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(FashColors.Primary),
                )
                Text(
                    text = stringResource(R.string.order_detail_status_caption),
                    style = MaterialTheme.typography.labelSmall,
                    color = onContainer.copy(alpha = 0.75f),
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = if (st == "cancelled" || st == "disputed") onContainer else accent,
            )
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = onContainer.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun heroContent(
    d: OrderDetail,
    role: OrderViewerRole,
    st: String,
    formatDate: (String) -> String,
): Triple<String, String, Color> {
    val primary = FashColors.Primary
    return when (st) {
        "payment_pending" -> when (role) {
            OrderViewerRole.Seller -> Triple(
                stringResource(R.string.order_hero_seller_wait_payment_title),
                stringResource(R.string.order_hero_seller_wait_payment_sub),
                primary,
            )
            else -> Triple(
                stringResource(R.string.order_hero_buyer_payment_pending_title),
                stringResource(R.string.order_hero_buyer_payment_pending_sub),
                primary,
            )
        }
        "cash_meetup_open" -> Triple(
            stringResource(R.string.order_status_cash_meetup_open),
            stringResource(R.string.order_hero_cash_meetup_sub),
            primary,
        )
        "payment_held" -> when (role) {
            OrderViewerRole.Seller -> {
                val shipBy = d.shipByAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty()
                val sub = if (shipBy.isNotBlank()) {
                    stringResource(R.string.order_hero_seller_prepare_sub_deadline, shipBy)
                } else {
                    stringResource(R.string.order_hero_seller_prepare_sub)
                }
                Triple(stringResource(R.string.order_hero_seller_prepare_title), sub, primary)
            }
            else -> Triple(
                stringResource(R.string.order_hero_buyer_payment_held_title),
                stringResource(R.string.order_hero_buyer_payment_held_sub),
                primary,
            )
        }
        "in_transit" -> {
            val eta = d.expectedDeliveryAt.takeIf { it.isNotBlank() }?.let {
                stringResource(R.string.order_hero_eta_line, formatDate(it))
            }.orEmpty()
            val track = d.trackingStatusSummary.takeIf { it.isNotBlank() }.orEmpty()
            val sub = listOf(eta, track).filter { it.isNotBlank() }.joinToString("\n")
            Triple(
                stringResource(R.string.order_status_in_transit),
                sub.ifBlank { stringResource(R.string.order_hero_in_transit_sub) },
                primary,
            )
        }
        "delivered_confirmed" -> {
            val sub = d.deliveredAt.takeIf { it.isNotBlank() }?.let { at ->
                stringResource(R.string.order_hero_completed_sub, formatDate(at))
            }.orEmpty()
            Triple(stringResource(R.string.order_status_delivered_confirmed), sub, primary)
        }
        "cancelled" -> Triple(
            stringResource(R.string.order_status_cancelled),
            d.cancelledAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }
                ?.let { stringResource(R.string.order_hero_cancelled_sub, it) }
                ?: stringResource(R.string.order_hero_cancelled_sub_generic),
            primary,
        )
        "disputed" -> Triple(
            stringResource(R.string.order_status_disputed),
            d.disputeSummary.ifBlank { stringResource(R.string.order_hero_disputed_sub) },
            primary,
        )
        else -> Triple(
            d.status.ifBlank { stringResource(R.string.order_status_unknown) },
            "",
            primary,
        )
    }
}

/** Meetup / in-person: order has a linked meeting but no ship tracking — hide courier “shipped” steps. */
private fun isMeetupStyleOrder(d: OrderDetail): Boolean =
    d.meetingAppointment != null &&
        d.trackingNumber.isBlank() &&
        d.carrier.isBlank()

@Composable
internal fun OrderTimelineSection(
    d: OrderDetail,
    formatDate: (String) -> String,
) {
    val st = d.status.trim().lowercase()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.order_timeline_title),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            when (st) {
                "cancelled" -> {
                    TimelineRow(
                        TimelineStepUi(
                            title = stringResource(R.string.order_timeline_placed),
                            subtitle = d.createdAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty(),
                            state = TimelineStepState.Done,
                        ),
                        isLast = false,
                    )
                    TimelineRow(
                        TimelineStepUi(
                            title = stringResource(R.string.order_timeline_cancelled),
                            subtitle = d.cancelledAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty(),
                            state = TimelineStepState.Current,
                        ),
                        isLast = true,
                    )
                }
                "disputed" -> {
                    TimelineRow(
                        TimelineStepUi(
                            title = stringResource(R.string.order_timeline_placed),
                            subtitle = d.createdAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty(),
                            state = TimelineStepState.Done,
                        ),
                        isLast = false,
                    )
                    TimelineRow(
                        TimelineStepUi(
                            title = stringResource(R.string.order_timeline_dispute_open),
                            subtitle = d.disputeSummary,
                            state = TimelineStepState.Current,
                        ),
                        isLast = true,
                    )
                }
                "payment_pending" -> {
                    TimelineRow(
                        TimelineStepUi(
                            title = stringResource(R.string.order_timeline_placed),
                            subtitle = d.createdAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty(),
                            state = TimelineStepState.Done,
                        ),
                        isLast = false,
                    )
                    TimelineRow(
                        TimelineStepUi(
                            title = stringResource(R.string.order_timeline_waiting_payment),
                            subtitle = "",
                            state = TimelineStepState.Current,
                        ),
                        isLast = true,
                    )
                }
                "cash_meetup_open" -> {
                    TimelineRow(
                        TimelineStepUi(
                            title = stringResource(R.string.order_timeline_placed),
                            subtitle = d.createdAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty(),
                            state = TimelineStepState.Done,
                        ),
                        isLast = false,
                    )
                    TimelineRow(
                        TimelineStepUi(
                            title = stringResource(R.string.order_timeline_cash_meetup),
                            subtitle = "",
                            state = TimelineStepState.Current,
                        ),
                        isLast = true,
                    )
                }
                "payment_held" -> {
                    TimelineRow(
                        TimelineStepUi(
                            title = stringResource(R.string.order_timeline_placed),
                            subtitle = d.createdAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty(),
                            state = TimelineStepState.Done,
                        ),
                        isLast = false,
                    )
                    TimelineRow(
                        TimelineStepUi(
                            title = stringResource(R.string.order_timeline_paid),
                            subtitle = d.paidAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty(),
                            state = TimelineStepState.Done,
                        ),
                        isLast = false,
                    )
                    TimelineRow(
                        TimelineStepUi(
                            title = stringResource(R.string.order_timeline_preparing),
                            subtitle = stringResource(R.string.order_timeline_preparing_sub),
                            state = TimelineStepState.Current,
                        ),
                        isLast = true,
                    )
                }
                "in_transit" -> {
                    if (isMeetupStyleOrder(d)) {
                        TimelineRow(
                            TimelineStepUi(
                                title = stringResource(R.string.order_timeline_placed),
                                subtitle = d.createdAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty(),
                                state = TimelineStepState.Done,
                            ),
                            isLast = false,
                        )
                        TimelineRow(
                            TimelineStepUi(
                                title = stringResource(R.string.order_timeline_paid),
                                subtitle = d.paidAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty(),
                                state = TimelineStepState.Done,
                            ),
                            isLast = false,
                        )
                        TimelineRow(
                            TimelineStepUi(
                                title = stringResource(R.string.order_timeline_meetup_handoff),
                                subtitle = "",
                                state = TimelineStepState.Done,
                            ),
                            isLast = false,
                        )
                        TimelineRow(
                            TimelineStepUi(
                                title = stringResource(R.string.order_timeline_meetup_awaiting_buyer),
                                subtitle = stringResource(R.string.order_timeline_meetup_awaiting_buyer_sub),
                                state = TimelineStepState.Current,
                            ),
                            isLast = true,
                        )
                    } else {
                        val shipSub = d.shippedAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty()
                        val transitSub = d.trackingStatusSummary.ifBlank {
                            d.expectedDeliveryAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty()
                        }
                        TimelineRow(
                            TimelineStepUi(
                                title = stringResource(R.string.order_timeline_placed),
                                subtitle = d.createdAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty(),
                                state = TimelineStepState.Done,
                            ),
                            isLast = false,
                        )
                        TimelineRow(
                            TimelineStepUi(
                                title = stringResource(R.string.order_timeline_paid),
                                subtitle = d.paidAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty(),
                                state = TimelineStepState.Done,
                            ),
                            isLast = false,
                        )
                        TimelineRow(
                            TimelineStepUi(
                                title = stringResource(R.string.order_timeline_shipped),
                                subtitle = shipSub,
                                state = TimelineStepState.Done,
                            ),
                            isLast = false,
                        )
                        TimelineRow(
                            TimelineStepUi(
                                title = stringResource(R.string.order_timeline_in_transit),
                                subtitle = transitSub,
                                state = TimelineStepState.Current,
                            ),
                            isLast = true,
                        )
                    }
                }
                "delivered_confirmed" -> {
                    if (isMeetupStyleOrder(d)) {
                        TimelineRow(
                            TimelineStepUi(
                                title = stringResource(R.string.order_timeline_placed),
                                subtitle = d.createdAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty(),
                                state = TimelineStepState.Done,
                            ),
                            isLast = false,
                        )
                        TimelineRow(
                            TimelineStepUi(
                                title = stringResource(R.string.order_timeline_paid),
                                subtitle = d.paidAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty(),
                                state = TimelineStepState.Done,
                            ),
                            isLast = false,
                        )
                        TimelineRow(
                            TimelineStepUi(
                                title = stringResource(R.string.order_timeline_meetup_handoff),
                                subtitle = "",
                                state = TimelineStepState.Done,
                            ),
                            isLast = false,
                        )
                        TimelineRow(
                            TimelineStepUi(
                                title = stringResource(R.string.order_timeline_delivered),
                                subtitle = d.deliveredAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty(),
                                state = TimelineStepState.Done,
                            ),
                            isLast = true,
                        )
                    } else {
                        TimelineRow(
                            TimelineStepUi(
                                title = stringResource(R.string.order_timeline_placed),
                                subtitle = d.createdAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty(),
                                state = TimelineStepState.Done,
                            ),
                            isLast = false,
                        )
                        TimelineRow(
                            TimelineStepUi(
                                title = stringResource(R.string.order_timeline_paid),
                                subtitle = d.paidAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty(),
                                state = TimelineStepState.Done,
                            ),
                            isLast = false,
                        )
                        TimelineRow(
                            TimelineStepUi(
                                title = stringResource(R.string.order_timeline_shipped),
                                subtitle = d.shippedAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty(),
                                state = TimelineStepState.Done,
                            ),
                            isLast = false,
                        )
                        TimelineRow(
                            TimelineStepUi(
                                title = stringResource(R.string.order_timeline_delivered),
                                subtitle = d.deliveredAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty(),
                                state = TimelineStepState.Done,
                            ),
                            isLast = true,
                        )
                    }
                }
                else -> {
                    TimelineRow(
                        TimelineStepUi(
                            title = stringResource(R.string.order_timeline_placed),
                            subtitle = d.createdAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty(),
                            state = TimelineStepState.Done,
                        ),
                        isLast = false,
                    )
                    TimelineRow(
                        TimelineStepUi(
                            title = orderStatusLabelString(st),
                            subtitle = "",
                            state = TimelineStepState.Current,
                        ),
                        isLast = true,
                    )
                }
            }
        }
    }
}

/** Meetup-linked payment deadline (`meetup_deadline_at`) while order is still unpaid. */
@Composable
internal fun OrderMeetupDeadlineStrip(
    meetupDeadlineAt: String,
    formatDate: (String) -> String,
    modifier: Modifier = Modifier,
) {
    val raw = meetupDeadlineAt.trim()
    if (raw.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
        color = FashColors.Primary.copy(alpha = 0.08f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Filled.Schedule,
                contentDescription = null,
                tint = FashColors.Primary,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = stringResource(R.string.order_detail_meetup_pay_by, formatDate(raw)),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurface,
            )
        }
    }
}

@Composable
internal fun OrderMeetingSection(
    meeting: OrderMeetingAppointment,
    formatDate: (String) -> String,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current
    val stLabel = orderMeetupStatusLabel(meeting.status)
    val whenStr = meeting.scheduledAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
        color = scheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.order_detail_meetup_section_title),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                text = stringResource(R.string.order_detail_meetup_status, stLabel),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (whenStr.isNotBlank()) {
                Text(
                    text = stringResource(R.string.order_detail_meetup_when, whenStr),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            meeting.buyerCheckInAt.takeIf { it.isNotBlank() }?.let { raw ->
                Text(
                    text = stringResource(R.string.order_detail_meetup_buyer_check_in, formatDate(raw)),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            meeting.sellerCheckInAt.takeIf { it.isNotBlank() }?.let { raw ->
                Text(
                    text = stringResource(R.string.order_detail_meetup_seller_check_in, formatDate(raw)),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            if (meeting.buyerCheckInAt.isNotBlank() || meeting.sellerCheckInAt.isNotBlank()) {
                Text(
                    text = stringResource(R.string.order_detail_meetup_check_in_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant.copy(alpha = 0.9f),
                )
            }
            if (meeting.locationUrl.isNotBlank()) {
                OutlinedButton(
                    onClick = { runCatching { uriHandler.openUri(meeting.locationUrl) } },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, FashColors.Primary.copy(alpha = 0.5f)),
                ) {
                    Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.order_detail_meetup_open_maps))
                }
            }
            Text(
                text = if (meeting.reminderEnabled) {
                    stringResource(R.string.order_detail_meetup_reminder_on, meeting.reminderOffsetMinutes)
                } else {
                    stringResource(R.string.order_detail_meetup_reminder_off)
                },
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
            val activityLines = buildList {
                meeting.createdAt.takeIf { it.isNotBlank() }?.let {
                    add(stringResource(R.string.order_detail_meetup_created, formatDate(it)))
                }
                meeting.reminderSentAt.takeIf { it.isNotBlank() }?.let {
                    add(stringResource(R.string.order_detail_meetup_reminder_sent, formatDate(it)))
                }
                meeting.updatedAt.takeIf { it.isNotBlank() }?.let {
                    add(stringResource(R.string.order_detail_meetup_updated, formatDate(it)))
                }
            }
            if (activityLines.isNotEmpty()) {
                HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.45f))
                Text(
                    text = stringResource(R.string.order_detail_meetup_activity_title),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                activityLines.forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
internal fun OrderMeetingGraceSection(
    grace: OrderMeetingGrace,
    appointmentId: String?,
    busy: OrderDetailBusyAction,
    onCheckIn: (String) -> Unit,
    onReportNoShow: (reason: String, note: String?) -> Unit,
    showAcknowledgeCash: Boolean,
    onAcknowledgeCash: () -> Unit,
    formatDate: (String) -> String,
) {
    val idle = busy == OrderDetailBusyAction.None
    var showNoShowPicker by remember { mutableStateOf(false) }
    var noShowNote by remember { mutableStateOf("") }
    val hasAny =
        grace.sosUnlocked ||
            grace.canCheckIn || grace.canReportNoShow || showAcknowledgeCash ||
            grace.checkInHint.isNotBlank() || grace.noShowHint.isNotBlank() ||
            grace.buyerCheckedInAt.isNotBlank() || grace.sellerCheckedInAt.isNotBlank() ||
            grace.phase.isNotBlank() ||
            !appointmentId.isNullOrBlank()
    if (!hasAny) return

    val showSyncOnly =
        !appointmentId.isNullOrBlank() &&
            !grace.sosUnlocked &&
            !grace.canCheckIn &&
            !grace.canReportNoShow &&
            !showAcknowledgeCash &&
            grace.checkInHint.isBlank() &&
            grace.noShowHint.isBlank() &&
            grace.buyerCheckedInAt.isBlank() &&
            grace.sellerCheckedInAt.isBlank() &&
            grace.phase.isBlank()

    val scheme = MaterialTheme.colorScheme
    LaunchedEffect(showNoShowPicker) {
        if (showNoShowPicker) noShowNote = ""
    }
    if (showNoShowPicker) {
        AlertDialog(
            onDismissRequest = { showNoShowPicker = false },
            title = { Text(stringResource(R.string.order_detail_no_show_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = noShowNote,
                        onValueChange = { if (it.length <= 500) noShowNote = it },
                        label = { Text(stringResource(R.string.order_detail_no_show_note_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        enabled = idle,
                    )
                    TextButton(
                        onClick = {
                            onReportNoShow("other_absent", noShowNote.trim().ifBlank { null })
                            showNoShowPicker = false
                        },
                        enabled = idle,
                    ) {
                        Text(stringResource(R.string.order_detail_no_show_other_absent))
                    }
                    TextButton(
                        onClick = {
                            onReportNoShow("other_late", noShowNote.trim().ifBlank { null })
                            showNoShowPicker = false
                        },
                        enabled = idle,
                    ) {
                        Text(stringResource(R.string.order_detail_no_show_other_late))
                    }
                    TextButton(
                        onClick = {
                            onReportNoShow("mutual_cancel", noShowNote.trim().ifBlank { null })
                            showNoShowPicker = false
                        },
                        enabled = idle,
                    ) {
                        Text(stringResource(R.string.order_detail_no_show_mutual_cancel))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNoShowPicker = false }) {
                    Text(stringResource(R.string.create_listing_cancel))
                }
            },
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
        color = scheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.order_detail_meeting_grace_title),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            )
            if (showSyncOnly) {
                Text(
                    text = stringResource(R.string.order_detail_meeting_grace_sync_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            if (grace.sosUnlocked) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = scheme.errorContainer.copy(alpha = 0.45f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            tint = scheme.onErrorContainer,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            text = stringResource(R.string.order_detail_grace_sos_unlocked),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = scheme.onErrorContainer,
                        )
                    }
                }
            }
            if (grace.phase.isNotBlank()) {
                Text(
                    text = grace.phase,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            if (grace.checkInHint.isNotBlank()) {
                Text(
                    text = grace.checkInHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            if (grace.noShowHint.isNotBlank()) {
                Text(
                    text = grace.noShowHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            grace.buyerCheckedInAt.takeIf { it.isNotBlank() }?.let { raw ->
                Text(
                    text = stringResource(R.string.order_detail_grace_buyer_checked, formatDate(raw)),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            grace.sellerCheckedInAt.takeIf { it.isNotBlank() }?.let { raw ->
                Text(
                    text = stringResource(R.string.order_detail_grace_seller_checked, formatDate(raw)),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (grace.canCheckIn && !appointmentId.isNullOrBlank()) {
                val checkInBusy = busy == OrderDetailBusyAction.CheckIn
                val checkInOn = FashColors.Primary.fashReadableOn()
                Button(
                    onClick = { onCheckIn(appointmentId) },
                    enabled = idle,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FashColors.Primary,
                        contentColor = checkInOn,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    if (checkInBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = checkInOn,
                        )
                    } else {
                        Text(
                            stringResource(R.string.order_detail_check_in_cta),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            if (grace.canReportNoShow) {
                val reportBusy = busy == OrderDetailBusyAction.ReportNoShow
                OutlinedButton(
                    onClick = { showNoShowPicker = true },
                    enabled = idle,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, FashColors.Primary.copy(alpha = 0.45f)),
                ) {
                    if (reportBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = FashColors.Primary,
                        )
                    } else {
                        Text(
                            stringResource(R.string.order_detail_report_no_show_cta),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            if (showAcknowledgeCash) {
                val ackBusy = busy == OrderDetailBusyAction.AcknowledgeCash
                OutlinedButton(
                    onClick = onAcknowledgeCash,
                    enabled = idle,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, FashColors.Primary.copy(alpha = 0.45f)),
                ) {
                    if (ackBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = FashColors.Primary,
                        )
                    } else {
                        Text(
                            stringResource(R.string.order_detail_ack_offline_cash),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun orderMeetupStatusLabel(status: String): String {
    return when (status.trim().lowercase()) {
        "pending" -> stringResource(R.string.chat_meeting_status_pending)
        "confirmed" -> stringResource(R.string.chat_meeting_status_confirmed)
        "cancelled" -> stringResource(R.string.chat_meeting_status_cancelled)
        else -> status.trim().replaceFirstChar { c -> c.uppercaseChar() }
    }
}

@Composable
private fun orderStatusLabelString(status: String): String = when (status.lowercase()) {
    "payment_pending" -> stringResource(R.string.order_status_payment_pending)
    "payment_held" -> stringResource(R.string.order_status_payment_held)
    "in_transit" -> stringResource(R.string.order_status_in_transit)
    "delivered_confirmed" -> stringResource(R.string.order_status_delivered_confirmed)
    "cancelled" -> stringResource(R.string.order_status_cancelled)
    "disputed" -> stringResource(R.string.order_status_disputed)
    "cash_meetup_open" -> stringResource(R.string.order_status_cash_meetup_open)
    else -> status.ifBlank { stringResource(R.string.order_status_unknown) }
}

internal data class TimelineStepUi(
    val title: String,
    val subtitle: String,
    val state: TimelineStepState,
)

internal enum class TimelineStepState { Done, Current, Pending }

@Composable
private fun TimelineRow(
    step: TimelineStepUi,
    isLast: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    val (icon, tint) = when (step.state) {
        TimelineStepState.Done -> Icons.Default.CheckCircle to FashColors.Primary
        TimelineStepState.Current -> Icons.Default.LocalShipping to FashColors.Primary
        TimelineStepState.Pending -> Icons.Default.RadioButtonUnchecked to scheme.outline.copy(alpha = 0.5f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(28.dp)
                        .padding(top = 4.dp)
                        .background(scheme.outlineVariant.copy(alpha = 0.5f)),
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (step.state == TimelineStepState.Current) FontWeight.Bold else FontWeight.Normal,
                ),
                color = when (step.state) {
                    TimelineStepState.Current -> FashColors.Primary
                    else -> scheme.onSurface
                },
            )
            if (step.subtitle.isNotBlank()) {
                Text(
                    text = step.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun BuyerShippingAddressCard(
    d: OrderDetail,
    selectedLocal: ShippingAddress?,
    onChangeClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.order_detail_shipping_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onChangeClick) {
                    Text(
                        text = if (
                            selectedLocal != null ||
                            d.recipientName.isNotBlank() ||
                            d.shippingAddressFormatted.isNotBlank()
                        ) {
                            stringResource(R.string.order_detail_shipping_change)
                        } else {
                            stringResource(R.string.order_detail_shipping_choose)
                        },
                        color = FashColors.Primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            when {
                selectedLocal != null -> {
                    val nameOrLabel = selectedLocal.recipientName.trim()
                        .ifBlank { selectedLocal.label.trim() }
                        .ifBlank { stringResource(R.string.shipping_address_row_untitled) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = nameOrLabel,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.weight(1f),
                        )
                        if (selectedLocal.isDefault) {
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
                    if (selectedLocal.label.isNotBlank() && selectedLocal.recipientName.isNotBlank()) {
                        Text(
                            text = selectedLocal.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (selectedLocal.phone.isNotBlank()) {
                        Text(
                            text = selectedLocal.phone,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = selectedLocal.formattedAddressLine().ifBlank { selectedLocal.formattedSingleLine() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                d.recipientName.isNotBlank() || d.recipientPhone.isNotBlank() ||
                    d.shippingAddressFormatted.isNotBlank() -> {
                    val name = d.recipientName.ifBlank { "—" }
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    )
                    if (d.recipientPhone.isNotBlank()) {
                        Text(
                            text = d.recipientPhone,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    val line = d.shippingAddressFormatted.trim()
                    if (line.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                else -> {
                    Text(
                        text = stringResource(R.string.order_detail_shipping_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
internal fun OrderProductCard(
    d: OrderDetail,
    showSellerHandle: Boolean,
    onClick: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val imageUrl = d.listingImageUrl.takeIf { it.isNotBlank() }?.let { orderDetailResolveImageUrl(it) }.orEmpty()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null && d.listingId.isNotBlank()) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            ),
        shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
        color = scheme.surface,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.order_detail_product),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(scheme.surfaceContainerHigh),
                ) {
                    if (imageUrl.isNotEmpty()) {
                        FashAsyncImage(
                            model = imageUrl,
                            contentDescription = d.listingTitle,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    if (showSellerHandle && d.sellerUsername.isNotBlank()) {
                        Text(
                            text = "@${d.sellerUsername}",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = d.listingTitle.ifBlank { "—" },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (d.listingVariantLabel.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.order_detail_variant_line, d.listingVariantLabel),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun CounterpartyCard(
    title: String,
    displayName: String,
    username: String,
    avatarUrl: String,
    onClick: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val url = avatarUrl.takeIf { it.isNotBlank() }?.let { orderDetailResolveImageUrl(it) }.orEmpty()
    val avatarForUi = url.takeIf { it.isNotEmpty() }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null && username.isNotBlank()) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            ),
        shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
        color = scheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(scheme.surfaceContainerHigh),
                    ) {
                        FashProfileAvatarImage(
                            imageUrl = avatarForUi,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = displayName.ifBlank { "@$username" },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (username.isNotBlank()) {
                            Text(
                                text = "@$username",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun OrderBuyerReviewSection(
    role: OrderViewerRole,
    review: OrderBuyerReview,
    formatDate: (String) -> String,
) {
    val scheme = MaterialTheme.colorScheme
    val heading = when (role) {
        OrderViewerRole.Buyer -> stringResource(R.string.order_detail_buyer_review_heading_buyer)
        OrderViewerRole.Seller -> stringResource(R.string.order_detail_buyer_review_heading_seller)
        OrderViewerRole.Viewer -> stringResource(R.string.order_detail_buyer_review_heading_viewer)
    }
    val stars = review.rating.coerceIn(0, 5)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
        color = scheme.surface,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = heading,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(5) { i ->
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = if (i < stars) FashColors.Primary else scheme.outlineVariant.copy(alpha = 0.45f),
                    )
                }
                if (stars in 1..5) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$stars/5",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onSurface,
                    )
                }
            }
            val comment = review.comment.trim()
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = comment.ifBlank { stringResource(R.string.order_detail_buyer_review_no_comment) },
                style = MaterialTheme.typography.bodyMedium,
                color = if (comment.isNotBlank()) scheme.onSurface else scheme.onSurfaceVariant,
            )
            val at = review.createdAt.trim()
            if (at.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.order_detail_buyer_review_submitted_at, formatDate(at)),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun OrderTrackingCard(d: OrderDetail) {
    if (d.trackingNumber.isBlank() && d.carrier.isBlank()) return
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
        color = scheme.surface,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.order_detail_tracking),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = d.carrier.ifBlank { "—" },
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                text = d.trackingNumber.ifBlank { stringResource(R.string.order_detail_tracking_empty) },
                style = MaterialTheme.typography.bodySmall,
                color = FashColors.Primary,
            )
            if (d.trackingStatusSummary.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = d.trackingStatusSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun OrderStickyBottomBar(
    d: OrderDetail,
    role: OrderViewerRole,
    busy: OrderDetailBusyAction,
    onPay: () -> Unit,
    onCancelOrder: () -> Unit = {},
    onConfirmReceipt: () -> Unit,
    onReview: () -> Unit,
    onShip: () -> Unit,
    /** Seller meetup / in-person handoff — mutually exclusive with [onShip] in normal flows. */
    onConfirmHandoff: () -> Unit = {},
    onChat: () -> Unit,
    onOpenDispute: () -> Unit = {},
    onSubmitDisputeEvidence: () -> Unit = {},
) {
    val st = d.status.trim().lowercase()
    val showPay = role == OrderViewerRole.Buyer && st == "payment_pending"
    val showConfirm = role == OrderViewerRole.Buyer && d.canConfirm
    val showReview = role == OrderViewerRole.Buyer && d.canReview
    val showConfirmHandoff = role == OrderViewerRole.Seller && d.sellerShowsConfirmHandoffCta()
    val showShip = role == OrderViewerRole.Seller && st == "payment_held" && d.canShip && !showConfirmHandoff
    val showChat =
        d.conversationId.isNotBlank() &&
            (role == OrderViewerRole.Buyer || role == OrderViewerRole.Seller) &&
            st !in listOf("cancelled")
    val showOpenDispute =
        role != OrderViewerRole.Viewer &&
            st in listOf("in_transit", "delivered_confirmed")
    val showDisputeEvidence =
        role != OrderViewerRole.Viewer && st == "disputed"

    val idle = busy == OrderDetailBusyAction.None
    val hasPrimary = showConfirmHandoff || showShip || showPay || showConfirm
    val hasSecondary = showOpenDispute || showDisputeEvidence || showReview || showChat

    if (!hasPrimary && !hasSecondary) return

    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(FashTheme.spacing.radiusSoftMin)
    val rowGap = FashTheme.spacing.spacing2
    val stickyOutline = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.45f))
    val labelStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
    val flatElevation = ButtonDefaults.buttonElevation(
        defaultElevation = 0.dp,
        pressedElevation = 0.dp,
        focusedElevation = 0.dp,
        hoveredElevation = 0.dp,
        disabledElevation = 0.dp,
    )
    val compactPad = PaddingValues(horizontal = 18.dp, vertical = 10.dp)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = scheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = FashTheme.spacing.editorialStart)
                .padding(top = FashTheme.spacing.spacing3, bottom = FashTheme.spacing.spacing2),
            verticalArrangement = Arrangement.spacedBy(rowGap),
        ) {
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = scheme.outlineVariant.copy(alpha = 0.35f),
            )

            // ── Primary (checkout / fulfilment) ─────────────────────────────────
            if (showConfirmHandoff) {
                val handoffBusy = busy == OrderDetailBusyAction.ConfirmHandoff
                val handoffOn = FashColors.Primary.fashReadableOn()
                Button(
                    onClick = onConfirmHandoff,
                    enabled = idle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(FashTheme.spacing.buttonHeight - 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FashColors.Primary,
                        contentColor = handoffOn,
                    ),
                    shape = shape,
                    elevation = flatElevation,
                    contentPadding = compactPad,
                ) {
                    if (handoffBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = handoffOn,
                        )
                    } else {
                        Icon(Icons.Filled.Handshake, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.order_detail_confirm_handoff), style = labelStyle)
                    }
                }
            }
            if (showShip) {
                val shipBusy = busy == OrderDetailBusyAction.Ship
                val shipOn = FashColors.Primary.fashReadableOn()
                Button(
                    onClick = onShip,
                    enabled = idle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(FashTheme.spacing.buttonHeight - 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FashColors.Primary,
                        contentColor = shipOn,
                    ),
                    shape = shape,
                    elevation = flatElevation,
                    contentPadding = compactPad,
                ) {
                    if (shipBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = shipOn,
                        )
                    } else {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.order_detail_action_ship), style = labelStyle)
                    }
                }
            }
            if (showPay) {
                Button(
                    onClick = onPay,
                    enabled = idle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(FashTheme.spacing.buttonHeight - 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FashColors.Primary,
                        contentColor = FashColors.Primary.fashReadableOn(),
                    ),
                    shape = shape,
                    elevation = flatElevation,
                    contentPadding = compactPad,
                ) {
                    Text(stringResource(R.string.order_detail_pay), style = labelStyle)
                }
                val cancelBusy = busy == OrderDetailBusyAction.CancelOrder
                OutlinedButton(
                    onClick = onCancelOrder,
                    enabled = idle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(FashTheme.spacing.buttonHeight - 4.dp),
                    shape = shape,
                    border = BorderStroke(1.dp, scheme.error.copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.error),
                    contentPadding = compactPad,
                ) {
                    if (cancelBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = scheme.error,
                        )
                    } else {
                        Text(stringResource(R.string.order_cancel_order), style = labelStyle)
                    }
                }
            }
            if (showConfirm) {
                val confirmRecvBusy = busy == OrderDetailBusyAction.ConfirmReceipt
                val confirmOn = FashColors.Primary.fashReadableOn()
                Button(
                    onClick = onConfirmReceipt,
                    enabled = idle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(FashTheme.spacing.buttonHeight - 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FashColors.Primary,
                        contentColor = confirmOn,
                    ),
                    shape = shape,
                    elevation = flatElevation,
                    contentPadding = compactPad,
                ) {
                    if (confirmRecvBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = confirmOn,
                        )
                    } else {
                        Text(stringResource(R.string.orders_confirm_received), style = labelStyle)
                    }
                }
            }

            if (hasPrimary && hasSecondary) {
                Spacer(modifier = Modifier.height(FashTheme.spacing.spacing1))
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = scheme.outlineVariant.copy(alpha = 0.22f),
                )
                Spacer(modifier = Modifier.height(FashTheme.spacing.spacing1))
            }

            // ── Secondary (post-purchase / support) — even rhythm, softer chrome ──
            if (showReview) {
                val reviewBusy = busy == OrderDetailBusyAction.SubmitReview
                val reviewOn = FashColors.Primary.fashReadableOn()
                Button(
                    onClick = onReview,
                    enabled = idle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(FashTheme.spacing.buttonHeight - 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FashColors.Primary,
                        contentColor = reviewOn,
                    ),
                    shape = shape,
                    elevation = flatElevation,
                    contentPadding = compactPad,
                ) {
                    if (reviewBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = reviewOn,
                        )
                    } else {
                        Text(stringResource(R.string.orders_review), style = labelStyle)
                    }
                }
            }
            if (showOpenDispute) {
                val disputeOpenBusy = busy == OrderDetailBusyAction.OpenDispute
                OutlinedButton(
                    onClick = onOpenDispute,
                    enabled = idle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(FashTheme.spacing.buttonHeight - 4.dp),
                    shape = shape,
                    border = stickyOutline,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface),
                    contentPadding = compactPad,
                ) {
                    if (disputeOpenBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = scheme.primary,
                        )
                    } else {
                        Text(stringResource(R.string.order_detail_dispute_open), style = labelStyle)
                    }
                }
            }
            if (showDisputeEvidence) {
                val evidenceBusy = busy == OrderDetailBusyAction.SubmitEvidence
                OutlinedButton(
                    onClick = onSubmitDisputeEvidence,
                    enabled = idle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(FashTheme.spacing.buttonHeight - 4.dp),
                    shape = shape,
                    border = stickyOutline,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface),
                    contentPadding = compactPad,
                ) {
                    if (evidenceBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = scheme.primary,
                        )
                    } else {
                        Text(stringResource(R.string.order_detail_dispute_evidence), style = labelStyle)
                    }
                }
            }
            if (showChat) {
                OutlinedButton(
                    onClick = onChat,
                    enabled = idle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(FashTheme.spacing.buttonHeight - 4.dp),
                    shape = shape,
                    border = stickyOutline,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface),
                    contentPadding = compactPad,
                ) {
                    Icon(
                        Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = scheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (role) {
                            OrderViewerRole.Seller -> stringResource(R.string.order_detail_chat_buyer)
                            else -> stringResource(R.string.order_detail_chat_seller)
                        },
                        style = labelStyle,
                        color = scheme.onSurface,
                    )
                }
            }
        }
    }
}

internal fun orderDetailResolveImageUrl(path: String): String {
    if (path.startsWith("http")) return path
    val base = AppEnvironment.apiBaseUrl.trimEnd('/')
    return if (path.startsWith("/")) "$base$path" else "$base/$path"
}

internal fun formatOrderDateTime(iso: String): String {
    if (iso.isBlank()) return ""
    return try {
        val s = iso.trim().let { t ->
            when {
                t.contains('T') -> t
                t.contains(' ') -> t.replaceFirst(" ", "T")
                else -> "${t}T00:00:00Z"
            }
        }
        val instant = java.time.Instant.parse(
            if (s.endsWith("Z") || s.contains('+') || s.length > 25) s else "${s}Z",
        )
        val z = java.time.ZoneId.systemDefault()
        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(instant.atZone(z))
    } catch (_: Exception) {
        iso
    }
}

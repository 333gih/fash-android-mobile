package com.pc.fash_android_mobile.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import com.pc.fash_android_mobile.data.chat.MyConversationReport
import com.pc.fash_android_mobile.data.order.sellerConfirmHandoffCtaVisible
import com.pc.fash_android_mobile.ui.common.stableLazyKey
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.config.BusinessFlowConfig
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.chat.ChatMapsUrlRules
import com.pc.fash_android_mobile.data.chat.ChatMessage
import com.pc.fash_android_mobile.data.chat.OutboundSendState
import com.pc.fash_android_mobile.data.deal.DealRecord
import com.pc.fash_android_mobile.data.chat.parseOrderCancelledEmbeddedMessage
import com.pc.fash_android_mobile.data.chat.ProductCard
import com.pc.fash_android_mobile.data.chat.PriceOffer
import com.pc.fash_android_mobile.ui.components.FashDefaultProfileAvatar
import com.pc.fash_android_mobile.ui.components.FashEmptyBulletTipLine
import com.pc.fash_android_mobile.ui.components.FashSnackbarHost
import com.pc.fash_android_mobile.ui.components.rememberSerialSnackbarChannel
import com.pc.fash_android_mobile.ui.main.ChatComposerBarOverlayInset
import com.pc.fash_android_mobile.ui.components.FashEmptyState
import com.pc.fash_android_mobile.ui.address.AddressBookViewModel
import com.pc.fash_android_mobile.ui.orders.OrderDetailViewModel
import com.pc.fash_android_mobile.ui.orders.formatOrderDateTime
import com.pc.fash_android_mobile.ui.orders.normalizeOrderStatus
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.fashReadableOn
import com.pc.fash_android_mobile.ui.theme.fashShimmer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Screen entry point
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatDetailScreen(
    conversationId: String,
    viewModel: ChatDetailViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onProductClick: (listingId: String) -> Unit = {},
    /**
     * Called when the buyer continues in-app checkout from the deal banner (`payment_pending` escrow order).
     * Carries [orderId], [listingId], and the last accepted offer amount for [CheckoutScreen].
     */
    onPayNow: (orderId: String, listingId: String, acceptedAmountVnd: Long) -> Unit = { _, _, _ -> },
    /**
     * Buyer/seller chose **Ship** in the fulfillment sheet — host should push [ChatShipFulfillmentScreen]
     * (or equivalent) for address + checkout handoff.
     */
    onStartShipFulfillment: (orderId: String, listingId: String, agreedAmountVnd: Long) -> Unit = { _, _, _ -> },
    /** Called when any party taps the banner area (view order details). */
    onOrderDetails: (orderId: String) -> Unit = {},
    /** Legacy callback kept for backward compat; not triggered by the offer→order flow. */
    onCheckout: (listingId: String, acceptedAmountVnd: Long) -> Unit = { _, _ -> },
    /**
     * Unread count in **other** conversations (global inbox minus this thread’s unread row).
     * Shown on the back control and as a subtitle in the title area when non-zero.
     */
    otherInboxUnreadCount: Int = 0,
    /** Open the counterparty’s public seller profile (by username). */
    onOtherUserProfileClick: (username: String) -> Unit = {},
    /** Opens the orders list (same as main app bar). */
    onOrdersClick: () -> Unit = {},
    /**
     * When non-null, shows [ChatOrderDetailOverlay] as a **child** of this screen so chat stays
     * composed under the sheet (stable with main nav / bottom bar; avoids overlay–sibling ordering issues).
     */
    orderDetailOverlayOrderId: String? = null,
    onDismissOrderDetailOverlay: () -> Unit = {},
    orderDetailViewModel: OrderDetailViewModel? = null,
    addressBookViewModel: AddressBookViewModel? = null,
    onOrderOverlayPayment: (listingId: String, amountVnd: Long, orderId: String) -> Unit = { _, _, _ -> },
    onOrderOverlayNavigateToChat: (conversationId: String) -> Unit = {},
    onOrderOverlayOpenShippingList: () -> Unit = {},
    onOrderOverlayOpenAddShipping: () -> Unit = {},
    /** From order sheet: open counterparty profile (dismisses overlay first in caller). */
    onOrderOverlayOpenUserProfile: (username: String) -> Unit = {},
    /** From order sheet: open listing PDP or seller edit for own listing. */
    onOrderOverlayOpenListing: (listingId: String, sellerUserId: String) -> Unit = { _, _ -> },
    /** Seller: after a successful sale, shortcut to create another listing (e.g. Post tab). */
    onSellerSuggestNewListing: () -> Unit = {},
) {
    val detail by viewModel.detail.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isMessagesLoading by viewModel.isMessagesLoading.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val isRespondingToOffer by viewModel.isRespondingToOffer.collectAsState()
    val isCreatingOffer by viewModel.isCreatingOffer.collectAsState()
    val showOfferDialog by viewModel.showOfferDialog.collectAsState()
    val orderId by viewModel.orderId.collectAsState()
    val orderStatus by viewModel.orderStatus.collectAsState()
    val orderMeetupDeadlineAt by viewModel.orderMeetupDeadlineAt.collectAsState()
    val orderCanConfirmHandoff by viewModel.orderCanConfirmHandoff.collectAsState()
    val orderMeetupBothPartiesCheckedIn by viewModel.orderMeetupBothPartiesCheckedIn.collectAsState()
    val confirmHandoffInFlight by viewModel.confirmHandoffInFlight.collectAsState()
    val orderMeetingAppointmentStatus by viewModel.orderMeetingAppointmentStatus.collectAsState()
    val orderMeetingScheduledAt by viewModel.orderMeetingScheduledAt.collectAsState()
    val isOtherTyping by viewModel.isOtherTyping.collectAsState()
    val meetingMutationInFlight by viewModel.meetingMutationInFlight.collectAsState()
    val isProposingMeeting by viewModel.isProposingMeeting.collectAsState()
    val showMeetingIdentityReverify by viewModel.showMeetingIdentityReverifyDialog.collectAsState()
    val ackMeetingReverifyInFlight by viewModel.ackMeetingReverifyInFlight.collectAsState()
    val isCreatingCounterOffer by viewModel.isCreatingCounterOffer.collectAsState()
    val showReportDialog by viewModel.showReportDialog.collectAsState()
    val isReporting by viewModel.isReporting.collectAsState()
    val reportBannerPulseAt by viewModel.reportBannerPulseAt.collectAsState()
    val counterOfferSheet by viewModel.counterOfferSheet.collectAsState()
    val activeDeal by viewModel.activeDeal.collectAsState()
    val isDealWorking by viewModel.isDealWorking.collectAsState()
    val pendingDealReviewDealId by viewModel.pendingDealReviewDealId.collectAsState()

    /** Escrow order linked to this thread — prefer VM state, fall back to [ConversationDetail.orderId] from API. */
    val conversationOrderId = orderId?.trim()?.takeIf { it.isNotEmpty() }
        ?: detail?.orderId?.trim()?.takeIf { it.isNotEmpty() }

    val snackbarHostState = remember { SnackbarHostState() }
    val enqueueSnackbarSerial = rememberSerialSnackbarChannel(snackbarHostState)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { msg ->
            enqueueSnackbarSerial { showSnackbar(msg) }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.suggestReopenListing.collect {
            enqueueSnackbarSerial {
                showSnackbar(context.getString(R.string.chat_meeting_cancel_suggest_reopen))
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigateToOrderDetail.collectLatest { oid ->
            if (oid.isNotBlank()) onOrderDetails(oid.trim())
        }
    }

    // Always invoke [loadConversation]: when [detail] already matches this thread it still runs [silentPoll]
    // to load messages. Skipping when ids matched left history empty (e.g. open chat from PDP while the VM
    // still held this conversation from a quick inbox back) until send/offer called [refreshMessages].
    LaunchedEffect(conversationId) {
        viewModel.loadConversation(conversationId)
    }

    MeetingIdentityReverifyDialog(
        visible = showMeetingIdentityReverify,
        openVerificationUrl = AppEnvironment.identityReverifyUrl.takeIf { it.isNotEmpty() },
        isAckInFlight = ackMeetingReverifyInFlight,
        onDismiss = viewModel::dismissMeetingIdentityReverifyDialog,
        onAckCompleted = viewModel::ackMeetingIdentityReverifyFromChat,
    )

    Box(modifier = modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = {
            FashSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                additionalBottomInset = ChatComposerBarOverlayInset,
            )
        },
        topBar = {
            TopAppBar(
                title = {
                    if (detail != null) {
                        val d0 = detail!!
                        val profileUsername = d0.otherUser.username.trim()
                        ChatDetailHeader(
                            displayName = d0.otherUser.displayName
                                .ifBlank { "@${d0.otherUser.username}" },
                            avatarUrl = d0.otherUser.avatarUrl,
                            otherInboxUnreadCount = otherInboxUnreadCount,
                            profileUsername = profileUsername,
                            onProfileClick = {
                                if (profileUsername.isNotEmpty()) {
                                    onOtherUserProfileClick(profileUsername)
                                }
                            },
                        )
                    }
                },
                navigationIcon = {
                    val backCd = if (otherInboxUnreadCount > 0) {
                        stringResource(R.string.chat_detail_back_inbox_unread_cd, otherInboxUnreadCount)
                    } else {
                        stringResource(R.string.chat_detail_back_cd)
                    }
                    BadgedBox(
                        badge = {
                            if (otherInboxUnreadCount > 0) {
                                Badge(containerColor = FashColors.Primary) {
                                    Text(
                                        text = if (otherInboxUnreadCount > 99) "99+" else otherInboxUnreadCount.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                        },
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = backCd,
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOrdersClick) {
                        Icon(
                            imageVector = Icons.Default.LocalMall,
                            contentDescription = stringResource(R.string.orders_icon_cd),
                            tint = FashColors.Primary,
                        )
                    }
                    // Report: hide flow when this thread already has the viewer's report (server `my_report`).
                    if (detail?.myReport == null) {
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { expanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = stringResource(R.string.chat_report_menu_cd),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            androidx.compose.material3.DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                            ) {
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(stringResource(R.string.chat_report_menu_item)) },
                                    onClick = {
                                        expanded = false
                                        viewModel.openReportDialog()
                                    },
                                )
                            }
                        }
                    } else {
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                            tooltip = {
                                PlainTooltip {
                                    Text(stringResource(R.string.chat_report_already_submitted_cd))
                                }
                            },
                            state = rememberTooltipState(),
                        ) {
                            Box(
                                modifier = Modifier.size(48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = stringResource(R.string.chat_report_already_submitted_cd),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        when {
            isLoading && detail == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = FashColors.Primary)
                }
            }

            detail == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.chat_load_error),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            else -> {
                val d = detail!!
                var showMeetingSheet by remember { mutableStateOf(false) }
                var showFulfillmentChoiceSheet by remember { mutableStateOf(false) }
                val maxOffers = BusinessFlowConfig.maxOffersPerConversation
                val orderStatusNorm = orderStatus?.trim()?.lowercase().orEmpty()
                /**
                 * Aligns with server rules: no new offers while an order is in pipeline (e.g. payment_pending);
                 * after cancel/expiry, [orderId] clears / status cancelled and offers can resume (subject to
                 * is_closed, offer_count, pending offer — see [OfferLimitPolicyBanner] and [offerBlocked]).
                 */
                val hasOrderBlockingOffer =
                    conversationOrderId != null && orderStatusNorm != "cancelled"
                val hasLinkedOrder = conversationOrderId != null
                val composerReadOnly =
                    d.isClosed || d.product?.listingStatus == "sold"
                val offerBlocked =
                    d.isClosed ||
                        d.product?.listingStatus == "sold" ||
                        (d.product?.listingStatus == "reserved" && orderStatusNorm != "cancelled")
                val offerLimitReached = d.offerCount >= maxOffers
                val sortedMessages = messages.sortedBy { it.timestamp }

                val buyerOfferBlockedByMeetupPolicy = remember(
                    sortedMessages,
                    orderStatus,
                    orderMeetingAppointmentStatus,
                    orderMeetingScheduledAt,
                ) {
                    shouldBlockBuyerNewPriceOffer(
                        messages = sortedMessages,
                        viewerIsBuyer = d.isBuyer,
                        orderStatus = orderStatus,
                        orderApptStatus = orderMeetingAppointmentStatus,
                        orderApptScheduledAt = orderMeetingScheduledAt,
                    )
                }
                val hasActiveMeetupBlockingSchedule = remember(
                    sortedMessages,
                    orderMeetingAppointmentStatus,
                    orderMeetingScheduledAt,
                    orderStatus,
                ) {
                    dealBannerShouldHideScheduleMeetup(
                        orderStatus = orderStatus,
                        orderApptStatus = orderMeetingAppointmentStatus,
                        orderApptScheduledAt = orderMeetingScheduledAt,
                        messages = sortedMessages,
                    )
                }
                val orderStatusSubtitle = chatOrderStatusSubtitleForChat(
                    isBuyer = d.isBuyer,
                    orderStatusRaw = orderStatus,
                )

                val sellerSeesConfirmHandoff = remember(
                    d.isBuyer,
                    orderCanConfirmHandoff,
                    orderMeetupBothPartiesCheckedIn,
                    orderStatus,
                ) {
                    !d.isBuyer &&
                        sellerConfirmHandoffCtaVisible(
                            canConfirmHandoff = orderCanConfirmHandoff,
                            meetupBothPartiesCheckedIn = orderMeetupBothPartiesCheckedIn,
                            orderStatusRaw = orderStatus,
                        )
                }

                val acceptedOfferAmount = remember(sortedMessages) {
                    sortedMessages
                        .filter {
                            isNegotiationMessageType(it.messageType) &&
                                it.offerStatus.equals("accepted", ignoreCase = true)
                        }
                        .maxByOrNull { it.timestamp }
                        ?.offerAmountVnd ?: 0L
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                ) {
                    // STATE B: Deal banner (slides in from top when order_id becomes non-null)
                    AnimatedVisibility(
                        visible = hasLinkedOrder,
                        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                    ) {
                        DealBanner(
                            isBuyer = d.isBuyer,
                            orderStatus = orderStatus,
                            statusSubtitle = orderStatusSubtitle,
                            meetupPayByFormatted = orderMeetupDeadlineAt?.takeIf { it.isNotBlank() }?.let { raw ->
                                formatOrderDateTime(raw)
                            },
                            meetingSosUnlocked = orderMeetupBothPartiesCheckedIn,
                            onTap = { conversationOrderId?.let { onOrderDetails(it) } },
                            onPayNow = {
                                conversationOrderId?.let { oid ->
                                    onPayNow(
                                        oid,
                                        d.product?.listingId.orEmpty(),
                                        acceptedOfferAmount,
                                    )
                                }
                            },
                            onConfirmHandoff = if (sellerSeesConfirmHandoff) {
                                { viewModel.confirmHandoff() }
                            } else {
                                null
                            },
                            confirmHandoffInProgress = confirmHandoffInFlight,
                            onOpenFulfillmentChoice = if (
                                hasLinkedOrder &&
                                orderStatusNorm != "cancelled" &&
                                orderStatusNorm != "delivered_confirmed" &&
                                orderStatusNorm != "disputed" &&
                                !hasActiveMeetupBlockingSchedule
                            ) {
                                { showFulfillmentChoiceSheet = true }
                            } else {
                                null
                            },
                            onSellerSuggestNewListing = if (!d.isBuyer && orderStatusNorm == "delivered_confirmed") {
                                onSellerSuggestNewListing
                            } else {
                                null
                            },
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))

                    // Product reference card
                    d.product?.let { product ->
                        ProductReferenceCard(
                            product = product,
                            onClick = { onProductClick(product.listingId) },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.52f))
                    }

                    val myReport = d.myReport
                    AnimatedVisibility(
                        visible = myReport != null,
                        enter = fadeIn(tween(240, easing = FastOutSlowInEasing)) +
                            expandVertically(
                                expandFrom = Alignment.Top,
                                animationSpec = tween(260, easing = FastOutSlowInEasing),
                            ),
                        exit = fadeOut(tween(160)) + shrinkVertically(shrinkTowards = Alignment.Top),
                    ) {
                        if (myReport != null) {
                            ConversationReportStatusBanner(
                                report = myReport,
                                pulseAt = reportBannerPulseAt,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.52f))
                        }
                    }

                    if (d.isBuyer && !hasOrderBlockingOffer && !offerBlocked) {
                        OfferLimitPolicyBanner(
                            usedCount = d.offerCount,
                            maxOffers = maxOffers,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.52f))
                    }

                    activeDeal?.let { deal ->
                        OfflineDealRecordBanner(
                            deal = deal,
                            isWorking = isDealWorking,
                            formatPrice = ::formatPrice,
                            onComplete = { viewModel.completeOfflineDeal() },
                            onCancel = { viewModel.cancelActiveOfflineDeal() },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.52f))
                    }

                    // Messages
                    val listState = rememberLazyListState()

                    // Only snap to newest when the user is already at the bottom (index 0 in reverse list).
                    // Scrolling on every list update was fighting the user when reading older messages.
                    LaunchedEffect(
                        sortedMessages.size,
                        sortedMessages.lastOrNull()?.messageId,
                        isCreatingOffer,
                        isCreatingCounterOffer,
                    ) {
                        if (sortedMessages.isEmpty() && !isCreatingOffer && !isCreatingCounterOffer) {
                            return@LaunchedEffect
                        }
                        if (listState.firstVisibleItemIndex == 0) {
                            listState.animateScrollToItem(0)
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (isMessagesLoading && sortedMessages.isEmpty()) {
                            MessageSkeletonList(
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else if (sortedMessages.isEmpty() && !isMessagesLoading) {
                            val scheme = MaterialTheme.colorScheme
                            FashEmptyState(
                                icon = Icons.Outlined.ChatBubbleOutline,
                                title = stringResource(R.string.chat_empty_messages_title),
                                subtitle = stringResource(R.string.chat_empty_messages_subtitle),
                                footer = {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(R.string.chat_empty_suggestions_title),
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.SemiBold,
                                        ),
                                        color = scheme.onSurface,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        FashEmptyBulletTipLine(
                                            text = stringResource(R.string.chat_empty_messages_tip_1),
                                        )
                                        FashEmptyBulletTipLine(
                                            text = stringResource(R.string.chat_empty_messages_tip_2),
                                        )
                                    }
                                },
                            )
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                reverseLayout = true,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                when {
                                    isCreatingOffer || isCreatingCounterOffer -> {
                                        item(key = "offer_sending_placeholder") {
                                            Box(
                                                modifier = Modifier
                                                    .animateItem(
                                                        fadeInSpec = tween(220, easing = FastOutSlowInEasing),
                                                        fadeOutSpec = tween(160),
                                                        placementSpec = spring(
                                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                                            stiffness = Spring.StiffnessMediumLow,
                                                        ),
                                                    )
                                                    .fillMaxWidth(),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                OfferSendingPlaceholder()
                                            }
                                        }
                                    }
                                    isMessagesLoading -> {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .animateItem(
                                                        fadeInSpec = tween(200, easing = FastOutSlowInEasing),
                                                        fadeOutSpec = tween(160),
                                                        placementSpec = spring(
                                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                                            stiffness = Spring.StiffnessMediumLow,
                                                        ),
                                                    )
                                                    .fillMaxWidth(),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                LoadingMessageIndicator()
                                            }
                                        }
                                    }
                                }
                                itemsIndexed(
                                    items = sortedMessages.reversed(),
                                    key = { index, msg ->
                                        stableLazyKey(msg.messageId, index, "msg")
                                    },
                                ) { index, msg ->
                                    val isNewestRow = index == 0
                                    var revealed by remember(msg.messageId, index) {
                                        mutableStateOf(!isNewestRow)
                                    }
                                    LaunchedEffect(msg.messageId, index) {
                                        if (isNewestRow) {
                                            revealed = true
                                        }
                                    }
                                    val newestScale by animateFloatAsState(
                                        targetValue = if (!isNewestRow || revealed) 1f else 0.94f,
                                        animationSpec = tween(280, easing = FastOutSlowInEasing),
                                        label = "chat_newest_scale",
                                    )
                                    Box(
                                        modifier = Modifier
                                            .animateItem(
                                                fadeInSpec = tween(
                                                    280,
                                                    easing = FastOutSlowInEasing,
                                                ),
                                                fadeOutSpec = tween(180),
                                                placementSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessMediumLow,
                                                ),
                                            )
                                            .fillMaxWidth()
                                            .then(
                                                if (isNewestRow) {
                                                    Modifier.graphicsLayer {
                                                        scaleX = newestScale
                                                        scaleY = newestScale
                                                        transformOrigin = TransformOrigin(0.5f, 1f)
                                                    }
                                                } else {
                                                    Modifier
                                                },
                                            ),
                                    ) {
                                        when (msg.messageType) {
                                            "meeting_proposal" -> {
                                                msg.meetingAppointment?.let { mtg ->
                                                    MeetingProposalMessageCard(
                                                        message = msg,
                                                        meeting = mtg,
                                                        isViewerBuyer = d.isBuyer,
                                                        hasLinkedEscrowOrder = hasLinkedOrder,
                                                        formatTime = viewModel::formatTime,
                                                        mutationInFlight = meetingMutationInFlight,
                                                        onConfirm = { viewModel.confirmMeeting(mtg.id) },
                                                        onWithdrawOrReject = { viewModel.cancelMeeting(mtg.id) },
                                                        onCheckIn = if (mtg.status.equals("confirmed", ignoreCase = true)) {
                                                            { viewModel.checkInMeeting(mtg.id) }
                                                        } else {
                                                            null
                                                        },
                                                        onRecordOfflineDeal = if (
                                                            !hasLinkedOrder &&
                                                            mtg.status.equals("confirmed", ignoreCase = true) &&
                                                            ChatMapsUrlRules.isLenientMeetingMapsUrl(mtg.locationUrl) &&
                                                            mtg.scheduledAt.isNotBlank()
                                                        ) {
                                                            {
                                                                viewModel.createOfflineDeal(
                                                                    meetingAppointmentId = mtg.id,
                                                                    meetingLocationUrl = mtg.locationUrl,
                                                                    meetingAtRfc3339 = mtg.scheduledAt,
                                                                    agreedPriceVnd = null,
                                                                )
                                                            }
                                                        } else {
                                                            null
                                                        },
                                                    )
                                                }
                                            }
                                            "offer", "counter_offer" -> OfferMessageBubble(
                                                message = msg,
                                                isBuyer = d.isBuyer,
                                                hasOrder = hasLinkedOrder,
                                                isResponding = isRespondingToOffer,
                                                onAccept = {
                                                    viewModel.acceptOffer(
                                                        PriceOffer(
                                                            offerId = msg.messageId,
                                                            amountVnd = msg.offerAmountVnd,
                                                            proposedByMe = msg.isFromMe,
                                                            status = msg.offerStatus,
                                                        ),
                                                    )
                                                },
                                                onDecline = {
                                                    viewModel.declineOffer(
                                                        PriceOffer(
                                                            offerId = msg.messageId,
                                                            amountVnd = msg.offerAmountVnd,
                                                            proposedByMe = msg.isFromMe,
                                                            status = msg.offerStatus,
                                                        ),
                                                    )
                                                },
                                                onCounter = if (
                                                    !d.isBuyer && !hasLinkedOrder &&
                                                    msg.messageType == "offer" &&
                                                    !msg.isFromMe
                                                ) {
                                                    {
                                                        viewModel.openCounterOfferSheet(
                                                            msg.messageId,
                                                            msg.offerAmountVnd,
                                                        )
                                                    }
                                                } else {
                                                    null
                                                },
                                                formatTime = viewModel::formatTime,
                                            )
                                            "system" -> SystemMessageBubble(
                                                message = msg,
                                                formatTime = viewModel::formatTime,
                                            )
                                            else -> if (msg.text.isNotBlank()) {
                                                val cancelledPair = remember(msg.messageId, msg.text) {
                                                    parseOrderCancelledEmbeddedMessage(msg.text)
                                                }
                                                if (cancelledPair != null) {
                                                    OrderCancelledNoticeBubble(
                                                        message = msg,
                                                        displayText = cancelledPair.second,
                                                        formatTime = viewModel::formatTime,
                                                        onViewOrder = { onOrderDetails(cancelledPair.first) },
                                                        onDeleteRequest = { viewModel.deleteMessage(msg) },
                                                    )
                                                } else {
                                                    MessageBubble(
                                                        message = msg,
                                                        formatTime = viewModel::formatTime,
                                                        onDeleteRequest = { viewModel.deleteMessage(msg) },
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Typing indicator — use expand/shrink so the bar animates smoothly
                    AnimatedVisibility(
                        visible = isOtherTyping,
                        enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(tween(220)),
                        exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(tween(180)),
                    ) {
                        TypingIndicator(name = d.otherUser.displayName.ifBlank { d.otherUser.username })
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.52f))

                    if (composerReadOnly) {
                        ClosedConversationComposerBar(
                            soldOnly = d.product?.listingStatus == "sold",
                        )
                    } else {
                        ChatInputBar(
                            text = inputText,
                            onTextChange = viewModel::onInputChange,
                            onSend = { viewModel.sendMessage() },
                            isSending = isSending,
                            showOfferButton = d.isBuyer && !hasOrderBlockingOffer,
                            offerButtonEnabled = d.pendingOffer == null &&
                                !offerLimitReached &&
                                !offerBlocked &&
                                !buyerOfferBlockedByMeetupPolicy,
                            offerShowLimitTooltip = d.isBuyer && !hasOrderBlockingOffer && offerLimitReached && !offerBlocked,
                            offerShowMeetupLockTooltip = d.isBuyer &&
                                !hasOrderBlockingOffer &&
                                !offerLimitReached &&
                                !offerBlocked &&
                                d.pendingOffer == null &&
                                buyerOfferBlockedByMeetupPolicy,
                            offerLimitMax = maxOffers,
                            onOfferClick = { viewModel.onSetPriceClick() },
                        )
                    }

                    // Modal bottom sheets: must be composed *after* the weighted message [Box] so flex layout
                    // keeps the list visible; placing sheets above [Modifier.weight(1f)] can collapse the list
                    // when a sheet opens (e.g. schedule meetup).
                    if (showOfferDialog) {
                        OfferPriceBottomSheet(
                            priceVnd = d.product?.priceVnd ?: 0L,
                            isLoading = isCreatingOffer,
                            onDismiss = { viewModel.dismissOfferDialog() },
                            onSubmit = { amt -> viewModel.createOffer(amt) },
                        )
                    }

                    counterOfferSheet?.let { args ->
                        CounterOfferBottomSheet(
                            buyerOfferAmountVnd = args.buyerOfferAmountVnd,
                            isLoading = isCreatingCounterOffer,
                            onDismiss = { viewModel.dismissCounterOfferSheet() },
                            onSubmit = { viewModel.submitCounterOffer(it) },
                        )
                    }

                    pendingDealReviewDealId?.let { rid ->
                        DealReviewBottomSheet(
                            dealId = rid,
                            isLoading = isDealWorking,
                            onDismiss = { viewModel.skipOfflineDealReviewPrompt() },
                            onSubmit = { rating, comment ->
                                viewModel.submitOfflineDealReview(rid, rating, comment)
                            },
                        )
                    }

                    if (showFulfillmentChoiceSheet) {
                        FulfillmentChoiceBottomSheet(
                            onDismiss = { showFulfillmentChoiceSheet = false },
                            onChooseMeetup = {
                                showFulfillmentChoiceSheet = false
                                showMeetingSheet = true
                            },
                            onChooseShip = {
                                showFulfillmentChoiceSheet = false
                                val oid = conversationOrderId
                                if (oid != null) {
                                    onStartShipFulfillment(
                                        oid,
                                        d.product?.listingId.orEmpty(),
                                        acceptedOfferAmount,
                                    )
                                }
                            },
                            shipFulfillmentEnabled = BusinessFlowConfig.c2cShipFulfillmentEnabled,
                        )
                    }
                    if (showMeetingSheet) {
                        val sheetLinkedOrder = conversationOrderId
                        MeetingProposalBottomSheet(
                            isLoading = isProposingMeeting,
                            linkedOrderId = sheetLinkedOrder,
                            onViewOrder = if (sheetLinkedOrder != null) {
                                { id: String ->
                                    showMeetingSheet = false
                                    onOrderDetails(id)
                                }
                            } else {
                                null
                            },
                            onDismiss = { if (!isProposingMeeting) showMeetingSheet = false },
                            onSubmit = { url, iso, en, off ->
                                viewModel.proposeMeeting(
                                    conversationId = conversationId,
                                    locationUrl = url,
                                    scheduledAtIso = iso,
                                    reminderEnabled = en,
                                    reminderOffsetMinutes = off,
                                ) {
                                    showMeetingSheet = false
                                }
                            },
                        )
                    }
                    // Report dialog
                    if (showReportDialog) {
                        ReportConversationDialog(
                            isLoading = isReporting,
                            onDismiss = { viewModel.dismissReportDialog() },
                            onSubmit = { category, description ->
                                viewModel.submitReport(category, description)
                            },
                        )
                    }
                }
            }
        }
    }
    val overlayOid = orderDetailOverlayOrderId?.trim()?.takeIf { it.isNotEmpty() }
    val ovm = orderDetailViewModel
    val avm = addressBookViewModel
    if (overlayOid != null && ovm != null && avm != null) {
        ChatOrderDetailOverlay(
            orderId = overlayOid,
            onDismiss = onDismissOrderDetailOverlay,
            viewModel = ovm,
            addressBookViewModel = avm,
            onNavigateToPayment = onOrderOverlayPayment,
            onNavigateToChat = onOrderOverlayNavigateToChat,
            onOpenShippingAddressList = onOrderOverlayOpenShippingList,
            onOpenAddShippingAddress = onOrderOverlayOpenAddShipping,
            onOpenUserProfile = onOrderOverlayOpenUserProfile,
            onOpenListing = onOrderOverlayOpenListing,
        )
    }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Conversation report (GET `my_report` + POST success feedback)
// ─────────────────────────────────────────────────────────────────────────────

private fun formatChatReportSubmittedAt(iso: String): String {
    if (iso.isBlank()) return ""
    return try {
        val toParse = when {
            iso.contains("T") -> iso
            iso.contains(" ") -> iso.replace(" ", "T")
            else -> iso
        }
        val instant = java.time.Instant.parse(toParse)
        java.time.format.DateTimeFormatter.ofLocalizedDateTime(java.time.format.FormatStyle.SHORT)
            .format(instant.atZone(java.time.ZoneId.systemDefault()))
    } catch (_: Exception) {
        iso
    }
}

@Composable
private fun reportCategoryLabel(category: String): String {
    val c = category.trim().lowercase()
    return when (c) {
        "spam" -> stringResource(R.string.chat_report_category_spam)
        "harassment" -> stringResource(R.string.chat_report_category_harassment)
        "scam" -> stringResource(R.string.chat_report_category_scam)
        "inappropriate" -> stringResource(R.string.chat_report_category_inappropriate)
        "other" -> stringResource(R.string.chat_report_category_other)
        "" -> ""
        else -> c.replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase(java.util.Locale.getDefault()) else ch.toString()
        }
    }
}

@Composable
private fun ConversationReportStatusBanner(
    report: MyConversationReport,
    pulseAt: Long,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val norm = report.status.trim().lowercase()
    val (container, onContainer, icon) = when (norm) {
        "pending" -> Triple(scheme.secondaryContainer, scheme.onSecondaryContainer, Icons.Outlined.Info)
        "dismissed" -> Triple(scheme.surfaceContainerHighest, scheme.onSurfaceVariant, Icons.Outlined.CheckCircle)
        "warned" -> Triple(scheme.tertiaryContainer, scheme.onTertiaryContainer, Icons.Filled.Warning)
        "suspended" -> Triple(scheme.errorContainer, scheme.onErrorContainer, Icons.Filled.ErrorOutline)
        else -> Triple(scheme.surfaceContainerHighest, scheme.onSurfaceVariant, Icons.Outlined.Info)
    }
    val statusLine = when (norm) {
        "pending" -> stringResource(R.string.chat_report_status_pending)
        "dismissed" -> stringResource(R.string.chat_report_status_dismissed)
        "warned" -> stringResource(R.string.chat_report_status_warned)
        "suspended" -> stringResource(R.string.chat_report_status_suspended)
        else -> stringResource(R.string.chat_report_status_unknown, report.status)
    }
    val scale = remember { Animatable(1f) }
    LaunchedEffect(pulseAt) {
        if (pulseAt == 0L) return@LaunchedEffect
        scale.snapTo(1f)
        scale.animateTo(1.04f, tween(140, easing = FastOutSlowInEasing))
        scale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        )
    }
    Surface(
        modifier = modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            transformOrigin = TransformOrigin.Center
        },
        color = container,
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.chat_report_banner_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = onContainer,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = onContainer,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = statusLine,
                    style = MaterialTheme.typography.bodyMedium,
                    color = onContainer,
                )
            }
            val submitted = formatChatReportSubmittedAt(report.createdAt)
            if (submitted.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.chat_report_banner_submitted_at, submitted),
                    style = MaterialTheme.typography.bodySmall,
                    color = onContainer.copy(alpha = 0.92f),
                )
            }
            val catLabel = reportCategoryLabel(report.category)
            if (catLabel.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.chat_report_banner_category, catLabel),
                    style = MaterialTheme.typography.bodySmall,
                    color = onContainer.copy(alpha = 0.92f),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Offer limit (buyer — max from [BusinessFlowConfig.maxOffersPerConversation])
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OfferLimitPolicyBanner(
    usedCount: Int,
    maxOffers: Int,
    modifier: Modifier = Modifier,
) {
    if (maxOffers < 1) return
    val scheme = MaterialTheme.colorScheme
    val used = usedCount.coerceAtLeast(0)
    val atLimit = used >= maxOffers
    val lastRemaining = !atLimit && maxOffers - used == 1 && maxOffers > 1
    val introOnly = used == 0
    val (container, onContainer, icon) = when {
        atLimit ->
            Triple(scheme.errorContainer, scheme.onErrorContainer, Icons.Filled.ErrorOutline)
        lastRemaining ->
            Triple(scheme.tertiaryContainer, scheme.onTertiaryContainer, Icons.Filled.Warning)
        else ->
            Triple(
                scheme.surfaceContainerHighest,
                scheme.onSurfaceVariant,
                Icons.Outlined.Info,
            )
    }
    val text = when {
        atLimit ->
            stringResource(R.string.chat_offer_policy_at_limit, used, maxOffers)
        lastRemaining ->
            stringResource(R.string.chat_offer_policy_last, maxOffers)
        introOnly ->
            stringResource(R.string.chat_offer_policy_intro, maxOffers)
        else ->
            stringResource(R.string.chat_offer_policy_progress, used, maxOffers)
    }
    Surface(
        modifier = modifier,
        color = container,
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = onContainer,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = onContainer,
            )
        }
    }
}

@Composable
private fun chatOrderStatusSubtitleForChat(isBuyer: Boolean, orderStatusRaw: String?): String? {
    val raw = orderStatusRaw?.trim().orEmpty()
    if (raw.isBlank()) return null
    val norm = normalizeOrderStatus(raw)
    return if (isBuyer) {
        when (norm) {
            "payment_pending" -> stringResource(R.string.chat_order_state_buyer_payment_pending)
            "payment_held" -> stringResource(R.string.chat_order_state_buyer_payment_held)
            "in_transit" -> stringResource(R.string.chat_order_state_buyer_in_transit)
            "delivered_confirmed" -> stringResource(R.string.chat_order_state_buyer_delivered_confirmed)
            "cancelled" -> stringResource(R.string.chat_order_state_buyer_cancelled)
            "disputed" -> stringResource(R.string.chat_order_state_buyer_disputed)
            "cash_meetup_open" -> stringResource(R.string.chat_order_state_buyer_cash_meetup_open)
            else -> stringResource(R.string.chat_order_state_buyer_unknown, raw)
        }
    } else {
        when (norm) {
            "payment_pending" -> stringResource(R.string.chat_order_state_seller_payment_pending)
            "payment_held" -> stringResource(R.string.chat_order_state_seller_payment_held)
            "in_transit" -> stringResource(R.string.chat_order_state_seller_in_transit)
            "delivered_confirmed" -> stringResource(R.string.chat_order_state_seller_delivered_confirmed)
            "cancelled" -> stringResource(R.string.chat_order_state_seller_cancelled)
            "disputed" -> stringResource(R.string.chat_order_state_seller_disputed)
            "cash_meetup_open" -> stringResource(R.string.chat_order_state_seller_cash_meetup_open)
            else -> stringResource(R.string.chat_order_state_seller_unknown, raw)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Deal banner (STATE B — escrow order exists)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DealBanner(
    isBuyer: Boolean,
    orderStatus: String?,
    /** Role-specific line so both parties see what the order state means for them. */
    statusSubtitle: String? = null,
    /** When set, shows meetup-linked payment cutoff (server `meetup_deadline_at`). */
    meetupPayByFormatted: String? = null,
    /** Both parties checked in (`sos_unlocked` and/or both grace check-in timestamps on order). */
    meetingSosUnlocked: Boolean = false,
    onTap: () -> Unit,
    onPayNow: () -> Unit = {},
    /** Seller meetup handoff — `POST .../confirm-handoff`. */
    onConfirmHandoff: (() -> Unit)? = null,
    confirmHandoffInProgress: Boolean = false,
    /** Opens meetup vs ship chooser (replaces legacy meetup-only CTA). */
    onOpenFulfillmentChoice: (() -> Unit)? = null,
    /** Seller-only: successful order — suggest listing another product. */
    onSellerSuggestNewListing: (() -> Unit)? = null,
) {
    val s = orderStatus?.trim()?.lowercase().orEmpty()
    val buyerNeedsToPay = isBuyer && s == "payment_pending"
    val sellerWaitingForPayment = !isBuyer && s == "payment_pending"
    val showPaymentDeadlineWarning = buyerNeedsToPay || sellerWaitingForPayment
    val showMeetupPayBy =
        showPaymentDeadlineWarning &&
            !meetupPayByFormatted.isNullOrBlank()

    val appearance = dealBannerAppearance(
        isBuyer = isBuyer,
        statusNorm = s,
        buyerNeedsToPay = buyerNeedsToPay,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(appearance.background),
    ) {
        // Status row — always visible
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onTap)
                .padding(horizontal = 16.dp, vertical = if (buyerNeedsToPay) 10.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = appearance.leadingIcon,
                contentDescription = null,
                tint = appearance.accent,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = when {
                    buyerNeedsToPay ->
                        stringResource(R.string.chat_deal_banner_buyer_pending)
                    !isBuyer && s == "payment_pending" ->
                        stringResource(R.string.chat_deal_banner_seller_pending)
                    s == "cancelled" ->
                        stringResource(R.string.chat_deal_banner_cancelled)
                    s == "disputed" ->
                        stringResource(R.string.chat_deal_banner_disputed)
                    s == "delivered_confirmed" ->
                        stringResource(R.string.chat_deal_banner_delivered)
                    s == "cash_meetup_open" ->
                        stringResource(R.string.chat_deal_banner_cash_meetup)
                    s in listOf("payment_held", "in_transit") ->
                        stringResource(R.string.chat_deal_banner_in_progress)
                    s.isBlank() ->
                        stringResource(R.string.chat_deal_banner_status_pending)
                    else ->
                        stringResource(R.string.chat_deal_banner_done)
                },
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = appearance.primaryText,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.chat_deal_banner_view_order),
                style = MaterialTheme.typography.labelSmall,
                color = appearance.accent,
            )
        }

        statusSubtitle?.let { sub ->
            Text(
                text = sub,
                style = MaterialTheme.typography.bodySmall,
                color = appearance.primaryText.copy(alpha = 0.88f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 10.dp),
            )
        }

        if (showMeetupPayBy) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 8.dp),
                color = Color(0xFFE8F5E9),
                shape = RoundedCornerShape(10.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(R.string.chat_deal_meetup_pay_deadline, meetupPayByFormatted!!),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp,
                        ),
                        color = Color(0xFF1B5E20),
                    )
                }
            }
        }

        if (meetingSosUnlocked && s != "cancelled") {
            val bothCheckedInMainRes = when {
                s == "in_transit" && isBuyer -> R.string.chat_deal_meetup_both_checked_in_buyer_in_transit
                s == "in_transit" && !isBuyer -> R.string.chat_deal_meetup_both_checked_in_seller_in_transit
                isBuyer -> R.string.chat_deal_meetup_both_checked_in_buyer
                else -> R.string.chat_deal_meetup_both_checked_in_seller
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 8.dp),
                color = Color(0xFFE8F5E9),
                shape = RoundedCornerShape(10.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(22.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(bothCheckedInMainRes),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                lineHeight = 18.sp,
                            ),
                            color = Color(0xFF1B5E20),
                        )
                        Text(
                            text = stringResource(R.string.chat_deal_sos_unlocked_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF1B5E20).copy(alpha = 0.72f),
                            lineHeight = 16.sp,
                        )
                    }
                }
            }
        }

        // Urgent: pay soon or order may be cancelled (buyer + seller while payment_pending)
        if (showPaymentDeadlineWarning) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = if (buyerNeedsToPay) 10.dp else 12.dp),
                color = Color(0xFFFFF5E6),
                shape = RoundedCornerShape(10.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = if (buyerNeedsToPay) {
                            stringResource(R.string.chat_deal_payment_deadline_warning_buyer)
                        } else {
                            stringResource(R.string.chat_deal_payment_deadline_warning_seller)
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp,
                        ),
                        color = Color(0xFFBF360C),
                    )
                }
            }
        }

        // Buyer CTA — view order details + continue checkout when order is payment_pending
        if (buyerNeedsToPay) {
            OutlinedButton(
                onClick = onTap,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, appearance.accent.copy(alpha = 0.55f)),
            ) {
                Text(
                    text = stringResource(R.string.chat_deal_banner_view_order),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = appearance.accent,
                )
            }
            Button(
                onClick = onPayNow,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = if (onOpenFulfillmentChoice != null) 8.dp else 12.dp)
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FashColors.Primary),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.ShoppingBag,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.chat_deal_escrow_continue_cta),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
            }
            if (onOpenFulfillmentChoice != null) {
                OutlinedButton(
                    onClick = onOpenFulfillmentChoice,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, appearance.accent.copy(alpha = 0.55f)),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocalShipping,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = appearance.accent,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.chat_fulfillment_banner_cta),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = appearance.accent,
                    )
                }
            }
        }
        if (onOpenFulfillmentChoice != null && !buyerNeedsToPay && s != "cancelled") {
            OutlinedButton(
                onClick = onOpenFulfillmentChoice,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, appearance.accent.copy(alpha = 0.55f)),
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocalShipping,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = appearance.accent,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.chat_fulfillment_banner_cta),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = appearance.accent,
                )
            }
        }
        if (onConfirmHandoff != null && !isBuyer) {
            val handoffOn = FashColors.Primary.fashReadableOn()
            Button(
                onClick = onConfirmHandoff,
                enabled = !confirmHandoffInProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp)
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FashColors.Primary),
            ) {
                if (confirmHandoffInProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = handoffOn,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.order_detail_confirm_handoff),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = handoffOn,
                    )
                }
            }
        }
        if (onSellerSuggestNewListing != null) {
            OutlinedButton(
                onClick = onSellerSuggestNewListing,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, appearance.accent.copy(alpha = 0.55f)),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = appearance.accent,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.chat_seller_suggest_new_listing),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = appearance.accent,
                )
            }
        }
    }
}

private data class DealBannerAppearance(
    val background: Color,
    val primaryText: Color,
    val accent: Color,
    val leadingIcon: ImageVector,
)

private fun dealBannerAppearance(
    isBuyer: Boolean,
    statusNorm: String,
    buyerNeedsToPay: Boolean,
): DealBannerAppearance = when {
    buyerNeedsToPay || (!isBuyer && statusNorm == "payment_pending") ->
        DealBannerAppearance(
            background = Color(0xFFE8F5E9),
            primaryText = Color(0xFF1B5E20),
            accent = Color(0xFF388E3C),
            leadingIcon = if (buyerNeedsToPay) Icons.Filled.ShoppingBag else Icons.Filled.CheckCircle,
        )
    statusNorm == "cancelled" ->
        DealBannerAppearance(
            background = Color(0xFFFFEBEE),
            primaryText = Color(0xFFB71C1C),
            accent = Color(0xFFC62828),
            leadingIcon = Icons.Filled.Cancel,
        )
    statusNorm == "disputed" ->
        DealBannerAppearance(
            background = Color(0xFFFFF8E1),
            primaryText = Color(0xFFBF360C),
            accent = Color(0xFFE65100),
            leadingIcon = Icons.Filled.Warning,
        )
    statusNorm in listOf("payment_held", "in_transit", "cash_meetup_open") ->
        DealBannerAppearance(
            background = Color(0xFFE3F2FD),
            primaryText = Color(0xFF0D47A1),
            accent = Color(0xFF1565C0),
            leadingIcon = Icons.Filled.ShoppingBag,
        )
    statusNorm == "delivered_confirmed" ->
        DealBannerAppearance(
            background = Color(0xFFE8F5E9),
            primaryText = Color(0xFF1B5E20),
            accent = Color(0xFF2E7D32),
            leadingIcon = Icons.Filled.CheckCircle,
        )
    statusNorm.isBlank() ->
        DealBannerAppearance(
            background = Color(0xFFF5F5F5),
            primaryText = Color(0xFF424242),
            accent = Color(0xFF616161),
            leadingIcon = Icons.Filled.ErrorOutline,
        )
    else ->
        DealBannerAppearance(
            background = Color(0xFFE8F5E9),
            primaryText = Color(0xFF1B5E20),
            accent = Color(0xFF388E3C),
            leadingIcon = Icons.Filled.CheckCircle,
        )
}

// ─────────────────────────────────────────────────────────────────────────────
// Coil: same image path with rotating presigned query params — stable cache keys + no crossfade
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StableChatImage(
    imageUrl: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    if (imageUrl.isBlank()) return
    val context = LocalContext.current
    val request = remember(imageUrl) {
        val key = imageUrl.substringBefore('?').ifBlank { imageUrl }
        ImageRequest.Builder(context)
            .data(imageUrl)
            .memoryCacheKey(key)
            .diskCacheKey(key)
            .crossfade(false)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = contentScale,
        alignment = Alignment.Center,
        modifier = modifier,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Product reference card (with SOLD badge)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProductReferenceCard(
    product: ProductCard,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val isSold = product.listingStatus in listOf("sold", "reserved")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Thumbnail with optional SOLD overlay
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp)),
        ) {
            StableChatImage(
                imageUrl = product.imageUrl,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (isSold) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x88000000)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.chat_listing_sold_label),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatPrice(product.priceVnd),
                style = MaterialTheme.typography.bodySmall,
                color = FashColors.Primary,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Offer sending placeholder (matches offer card chrome while POST /chat/offers is in flight)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OfferSendingPlaceholder() {
    val scheme = MaterialTheme.colorScheme
    val pulse = rememberInfiniteTransition(label = "offer_send_border")
    val borderAlpha by pulse.animateFloat(
        initialValue = 0.38f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "offer_border_pulse",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .background(scheme.surfaceContainerLow)
                .border(
                    width = 1.5.dp,
                    color = FashColors.Primary.copy(alpha = borderAlpha),
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalOffer,
                    contentDescription = null,
                    tint = FashColors.Primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.chat_offer_label),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = FashColors.Primary,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.62f)
                    .height(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(scheme.surfaceContainerHighest)
                    .fashShimmer(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = FashColors.Primary,
                )
                Text(
                    text = stringResource(R.string.chat_offer_sending),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Offer message bubble (full-width card with status badge)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OfferMessageBubble(
    message: ChatMessage,
    isBuyer: Boolean,
    hasOrder: Boolean,
    isResponding: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    /** Seller: counter a pending buyer offer (initial `offer` only). */
    onCounter: (() -> Unit)? = null,
    formatTime: (String) -> String,
) {
    val scheme = MaterialTheme.colorScheme
    val isCounterOffer = message.messageType.equals("counter_offer", ignoreCase = true)

    // Seller responds to buyer's first offer
    val showSellerActionsOnBuyerOffer = !isBuyer &&
        message.offerStatus == "pending" &&
        !message.isFromMe &&
        !hasOrder &&
        !isCounterOffer

    // Buyer responds to seller's counter
    val showBuyerActionsOnSellerCounter = isBuyer &&
        message.offerStatus == "pending" &&
        !message.isFromMe &&
        !hasOrder &&
        isCounterOffer

    // Buyer waiting on seller (initial offer)
    val showBuyerWaiting = isBuyer &&
        message.offerStatus == "pending" &&
        message.isFromMe &&
        !isCounterOffer

    // Seller waiting on buyer after counter
    val showSellerWaitingOnBuyer = !isBuyer &&
        message.offerStatus == "pending" &&
        message.isFromMe &&
        isCounterOffer

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Card
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .background(scheme.surfaceContainerLow)
                .border(
                    width = 1.dp,
                    color = scheme.outlineVariant.copy(alpha = 0.68f),
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalOffer,
                    contentDescription = null,
                    tint = FashColors.Primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(
                        if (isCounterOffer) R.string.chat_counter_offer_label else R.string.chat_offer_label,
                    ),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = FashColors.Primary,
                )
            }

            // Amount
            Text(
                text = formatPrice(message.offerAmountVnd),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = scheme.onSurface,
            )

            // Status badge
            OfferStatusBadge(status = message.offerStatus)

            // Conditional sub-content
            when {
                showBuyerWaiting -> {
                    Text(
                        text = stringResource(R.string.chat_offer_waiting_seller),
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        color = scheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }

                showSellerWaitingOnBuyer -> {
                    Text(
                        text = stringResource(R.string.chat_counter_waiting_buyer),
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        color = scheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }

                showBuyerActionsOnSellerCounter || showSellerActionsOnBuyerOffer -> {
                    if (isResponding) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = FashColors.Primary,
                            )
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                OutlinedButton(
                                    onClick = onDecline,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error,
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                    ),
                                ) {
                                    Text(
                                        stringResource(R.string.chat_offer_decline),
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                                Button(
                                    onClick = onAccept,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                ) {
                                    Text(
                                        stringResource(R.string.chat_offer_accept),
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                            if (showSellerActionsOnBuyerOffer && onCounter != null) {
                                OutlinedButton(
                                    onClick = onCounter,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, FashColors.Primary.copy(alpha = 0.5f)),
                                ) {
                                    Text(
                                        stringResource(R.string.chat_offer_counter_cta),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = FashColors.Primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Timestamp
            Text(
                text = formatTime(message.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

@Composable
private fun OfferStatusBadge(status: String) {
    val (bgColor, textColor, label) = when (status.lowercase()) {
        "pending" -> Triple(
            Color(0xFFFFF3CD),
            Color(0xFF856404),
            stringResource(R.string.chat_offer_status_waiting),
        )
        "accepted" -> Triple(
            Color(0xFFD1E7DD),
            Color(0xFF155724),
            stringResource(R.string.chat_offer_status_accepted),
        )
        "declined" -> Triple(
            Color(0xFFF8D7DA),
            Color(0xFF842029),
            stringResource(R.string.chat_offer_status_declined),
        )
        "expired" -> Triple(
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.onSurfaceVariant,
            stringResource(R.string.chat_offer_status_expired),
        )
        else -> Triple(
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.onSurfaceVariant,
            stringResource(R.string.chat_offer_status_cancelled),
        )
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = textColor,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// System message bubble
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SystemMessageBubble(
    message: ChatMessage,
    formatTime: (String) -> String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = message.text,
            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
            textAlign = TextAlign.Center,
        )
        Text(
            text = formatTime(message.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Buyer-cancel notice (machine-readable first line stripped for display + view order)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OrderCancelledNoticeBubble(
    message: ChatMessage,
    displayText: String,
    formatTime: (String) -> String,
    onViewOrder: () -> Unit,
    onDeleteRequest: () -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.chat_delete_message_title)) },
            text = { Text(stringResource(R.string.chat_delete_message_confirm)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteRequest()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = scheme.error),
                ) {
                    Text(stringResource(R.string.chat_delete_message_confirm))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.create_listing_cancel))
                }
            },
        )
    }

    val isMe = message.isFromMe
    val textOnPrimary = FashColors.Primary.fashReadableOn()
    val bubbleShape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (isMe) 18.dp else 4.dp,
        bottomEnd = if (isMe) 4.dp else 18.dp,
    )
    val incomingUnread = !isMe && !message.isRead
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .then(
                    if (incomingUnread) {
                        Modifier.border(2.dp, FashColors.Primary.copy(alpha = 0.5f), bubbleShape)
                    } else {
                        Modifier
                    },
                )
                .clip(bubbleShape)
                .background(if (isMe) FashColors.Primary else scheme.surfaceContainerHigh)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            if (isMe && !message.messageId.startsWith("local-")) showDeleteDialog = true
                        },
                    )
                }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isMe) textOnPrimary else scheme.onSurface,
            )
            TextButton(
                onClick = onViewOrder,
                modifier = Modifier.align(if (isMe) Alignment.End else Alignment.Start),
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
            ) {
                Text(
                    text = stringResource(R.string.chat_deal_banner_view_order),
                    color = if (isMe) textOnPrimary else FashColors.Primary,
                )
            }
            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (isMe && message.outboundState != OutboundSendState.NONE) {
                    when (message.outboundState) {
                        OutboundSendState.SENDING -> CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = textOnPrimary.copy(alpha = 0.75f),
                            strokeWidth = 2.dp,
                        )
                        OutboundSendState.FAILED -> Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = textOnPrimary.copy(alpha = 0.85f),
                        )
                        OutboundSendState.NONE -> Unit
                    }
                }
                Text(
                    text = formatTime(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isMe) textOnPrimary.copy(alpha = 0.65f) else scheme.onSurfaceVariant.copy(alpha = 0.55f),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Normal text message bubble
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MessageBubble(
    message: ChatMessage,
    formatTime: (String) -> String,
    onDeleteRequest: () -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.chat_delete_message_title)) },
            text = { Text(stringResource(R.string.chat_delete_message_confirm)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteRequest()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = scheme.error),
                ) {
                    Text(stringResource(R.string.chat_delete_message_confirm))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.create_listing_cancel))
                }
            },
        )
    }

    val isMe = message.isFromMe
    val textOnPrimary = FashColors.Primary.fashReadableOn()
    val bubbleShape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (isMe) 18.dp else 4.dp,
        bottomEnd = if (isMe) 4.dp else 18.dp,
    )
    val incomingUnread = !isMe && !message.isRead
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .then(
                    if (incomingUnread) {
                        Modifier.border(2.dp, FashColors.Primary.copy(alpha = 0.5f), bubbleShape)
                    } else {
                        Modifier
                    },
                )
                .clip(bubbleShape)
                .background(if (isMe) FashColors.Primary else scheme.surfaceContainerHigh)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            if (isMe && !message.messageId.startsWith("local-")) showDeleteDialog = true
                        },
                    )
                }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isMe) textOnPrimary else scheme.onSurface,
            )
            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (isMe && message.outboundState != OutboundSendState.NONE) {
                    when (message.outboundState) {
                        OutboundSendState.SENDING -> CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = textOnPrimary.copy(alpha = 0.75f),
                            strokeWidth = 2.dp,
                        )
                        OutboundSendState.FAILED -> Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = textOnPrimary.copy(alpha = 0.85f),
                        )
                        OutboundSendState.NONE -> Unit
                    }
                }
                Text(
                    text = formatTime(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isMe) textOnPrimary.copy(alpha = 0.65f) else scheme.onSurfaceVariant.copy(alpha = 0.55f),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Chat header (avatar + name inside TopAppBar title)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatDetailHeader(
    displayName: String,
    avatarUrl: String,
    otherInboxUnreadCount: Int = 0,
    profileUsername: String = "",
    onProfileClick: () -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    val canOpenProfile = profileUsername.isNotBlank()
    val profileClickLabel = stringResource(R.string.chat_detail_header_profile_cd)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (canOpenProfile) {
                    Modifier.clickable(
                        onClick = onProfileClick,
                        onClickLabel = profileClickLabel,
                    )
                } else {
                    Modifier
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(FashColors.Primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            if (avatarUrl.isNotBlank()) {
                StableChatImage(
                    imageUrl = avatarUrl,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                FashDefaultProfileAvatar(
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (otherInboxUnreadCount > 0) {
                Text(
                    text = stringResource(R.string.chat_detail_other_inbox_unread, otherInboxUnreadCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Read-only composer (reservation / sold)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ClosedConversationComposerBar(
    soldOnly: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    val text = if (soldOnly) {
        stringResource(R.string.chat_conversation_sold_readonly)
    } else {
        stringResource(R.string.chat_conversation_ended_readonly)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant.copy(alpha = 0.65f),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Chat input bar
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isSending: Boolean,
    /** True when current user is buyer AND no order exists yet. */
    showOfferButton: Boolean,
    /** Disables the offer button (pending offer, limit, closed listing, etc.). */
    offerButtonEnabled: Boolean,
    /** When true, offer button stays disabled and shows a plain tooltip (offer limit). */
    offerShowLimitTooltip: Boolean = false,
    /** When true (and limit tooltip off), disabled offer button explains meetup / negotiation lock. */
    offerShowMeetupLockTooltip: Boolean = false,
    /** Max offers from env (shown in limit tooltip). */
    offerLimitMax: Int,
    onOfferClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val tooltipState = rememberTooltipState()
    val onPrimaryText = FashColors.Primary.fashReadableOn()
    val sendBgPrimary = text.isNotBlank() && !isSending

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Offer button (STATE A, buyer only)
        if (showOfferButton) {
            if (offerShowLimitTooltip || offerShowMeetupLockTooltip) {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = {
                        PlainTooltip {
                            Text(
                                if (offerShowLimitTooltip) {
                                    stringResource(
                                        R.string.chat_offer_limit_tooltip,
                                        offerLimitMax,
                                    )
                                } else {
                                    stringResource(R.string.chat_offer_meetup_lock_tooltip)
                                },
                            )
                        }
                    },
                    state = tooltipState,
                ) {
                    OutlinedButton(
                        onClick = { },
                        enabled = false,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(40.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = scheme.outline.copy(alpha = 0.5f),
                            disabledContentColor = scheme.outline.copy(alpha = 0.5f),
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            scheme.outline.copy(alpha = 0.4f),
                        ),
                    ) {
                        Icon(
                            Icons.Filled.LocalOffer,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.chat_set_price),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onOfferClick,
                    enabled = offerButtonEnabled,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(40.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FashColors.Primary),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (offerButtonEnabled) FashColors.Primary else scheme.outline.copy(alpha = 0.4f),
                    ),
                ) {
                    Icon(
                        Icons.Filled.LocalOffer,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (offerButtonEnabled) {
                            stringResource(R.string.chat_set_price)
                        } else {
                            stringResource(R.string.chat_pending_offer_button_waiting)
                        },
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }

        // Text field
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    text = stringResource(R.string.chat_input_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Send,
                keyboardType = KeyboardType.Text,
            ),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            shape = RoundedCornerShape(24.dp),
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FashColors.Primary.copy(alpha = 0.7f),
                unfocusedBorderColor = scheme.outline.copy(alpha = 0.4f),
                focusedContainerColor = scheme.surfaceContainerLow,
                unfocusedContainerColor = scheme.surfaceContainerLow,
            ),
            textStyle = MaterialTheme.typography.bodyMedium,
        )

        // Send button
        IconButton(
            onClick = onSend,
            enabled = text.isNotBlank() && !isSending,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (sendBgPrimary) FashColors.Primary
                    else scheme.surfaceContainerHigh,
                ),
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = if (sendBgPrimary) onPrimaryText else scheme.primary,
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.chat_send),
                    tint = if (text.isNotBlank()) onPrimaryText else scheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Offer price bottom sheet
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Hides the IME and clears focus before sheet content is removed — reduces framework warnings such as
 * `requestCursorUpdates on inactive InputConnection` when dismissing sheets that contain [OutlinedTextField].
 */
@Composable
private fun rememberSheetDismiss(onDismiss: () -> Unit): () -> Unit {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    return remember(onDismiss) {
        {
            keyboard?.hide()
            focusManager.clearFocus(force = true)
            onDismiss()
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfferPriceBottomSheet(
    priceVnd: Long,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (Long) -> Unit,
) {
    var rawInput by remember { mutableStateOf("") }
    val parsedAmount = rawInput.filter { it.isDigit() }.toLongOrNull() ?: 0L
    val dismissSheet = rememberSheetDismiss(onDismiss)

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = dismissSheet,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(),
        dragHandle = { androidx.compose.material3.BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.chat_offer_dialog_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (priceVnd > 0L) {
                Text(
                    text = stringResource(R.string.chat_offer_dialog_listed_price, formatPrice(priceVnd)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "₫",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = FashColors.Primary,
                    modifier = Modifier.padding(end = 6.dp),
                )
                OutlinedTextField(
                    value = rawInput,
                    onValueChange = { rawInput = it.filter { c -> c.isDigit() } },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            stringResource(R.string.chat_offer_dialog_placeholder),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FashColors.Primary,
                    ),
                )
            }

            if (parsedAmount > 0L) {
                Text(
                    text = formatPrice(parsedAmount),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = FashColors.Primary,
                )
            }

            Button(
                onClick = { if (parsedAmount > 0L) onSubmit(parsedAmount) },
                enabled = parsedAmount > 0L && !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FashColors.Primary,
                    contentColor = FashColors.Primary.fashReadableOn(),
                ),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = FashColors.Primary.fashReadableOn(),
                    )
                } else {
                    Text(
                        text = stringResource(R.string.chat_offer_dialog_submit),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Typing indicator
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Typing indicator displayed when the other participant is composing a message.
 *
 * Design-system compliance:
 * - Bubble shape and color match the LEFT (incoming) message bubble.
 * - Mini avatar uses the same brand default as [ChatDetailHeader].
 * - Three dots use a **scale pulse** (not vertical offset) so motion is never clipped by the
 *   bubble’s [RoundedCornerShape] clip. Stagger uses [StartOffset] on [infiniteRepeatable].
 * - Each dot’s [animateFloat] is declared separately (never inside a loop).
 */
@Composable
private fun TypingIndicator(name: String) {
    val scheme = MaterialTheme.colorScheme

    val transition = rememberInfiniteTransition(label = "typing_indicator")

    // Staggered wave: same pulse, different phase (StartOffset — not tween delayMillis)
    val scaleDot0 by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(0, StartOffsetType.FastForward),
        ),
        label = "typing_dot0",
    )
    val scaleDot1 by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(110, StartOffsetType.FastForward),
        ),
        label = "typing_dot1",
    )
    val scaleDot2 by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(220, StartOffsetType.FastForward),
        ),
        label = "typing_dot2",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 64.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Mini avatar — mirrors ChatDetailHeader style
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(FashColors.Primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            FashDefaultProfileAvatar(
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            // Sender name label — matches caption style from the design system
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.padding(start = 2.dp),
            )

            // Bubble — same shape token as the incoming (left-side) message bubble
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomEnd = 18.dp,
                            bottomStart = 4.dp,
                        ),
                    )
                    .background(scheme.surfaceContainerHigh)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            ) {
                Row(
                    modifier = Modifier.height(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val dotColor = FashColors.OnSurfaceVariant.copy(alpha = 0.55f)
                    val dotSize = 8.dp

                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .scale(scaleDot0)
                            .clip(CircleShape)
                            .background(dotColor),
                    )
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .scale(scaleDot1)
                            .clip(CircleShape)
                            .background(dotColor),
                    )
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .scale(scaleDot2)
                            .clip(CircleShape)
                            .background(dotColor),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Message loading skeleton
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MessageSkeletonList(modifier: Modifier = Modifier) {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceContainerHigh,
        MaterialTheme.colorScheme.surfaceContainerHighest,
        MaterialTheme.colorScheme.surfaceContainerHigh,
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmer_translate",
    )
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 300f, 0f),
        end = Offset(translateAnim, 0f),
    )
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(6) { idx ->
            val isRight = idx % 2 == 0
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isRight) Arrangement.End else Arrangement.Start,
            ) {
                Box(
                    modifier = Modifier
                        .width(if (idx % 3 == 0) 220.dp else 160.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(brush),
                )
            }
        }
    }
}

@Composable
private fun LoadingMessageIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = FashColors.Primary.copy(alpha = 0.6f),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun isNegotiationMessageType(t: String): Boolean =
    t == "offer" || t.equals("counter_offer", ignoreCase = true)

@Composable
private fun OfflineDealRecordBanner(
    deal: DealRecord,
    isWorking: Boolean,
    formatPrice: (Long) -> String,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val st = deal.status.lowercase()
    if (st == "cancelled") return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = scheme.tertiaryContainer.copy(alpha = 0.45f),
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.chat_offline_deal_banner_title),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onTertiaryContainer,
            )
            when (st) {
                "scheduled" -> {
                    Text(
                        text = stringResource(R.string.chat_offline_deal_banner_scheduled),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurface,
                    )
                    if (deal.agreedPriceVnd > 0L) {
                        Text(
                            text = formatPrice(deal.agreedPriceVnd),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = scheme.onSurface,
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OutlinedButton(
                            onClick = onCancel,
                            enabled = !isWorking,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text(stringResource(R.string.chat_offline_deal_cancel_record))
                        }
                        Button(
                            onClick = onComplete,
                            enabled = !isWorking,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FashColors.Primary),
                        ) {
                            if (isWorking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = FashColors.Primary.fashReadableOn(),
                                )
                            } else {
                                Text(stringResource(R.string.chat_offline_deal_complete))
                            }
                        }
                    }
                }
                "completed" -> {
                    Text(
                        text = stringResource(R.string.chat_offline_deal_banner_completed),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurface,
                    )
                }
                else -> {
                    Text(
                        text = deal.status,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CounterOfferBottomSheet(
    buyerOfferAmountVnd: Long,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (Long) -> Unit,
) {
    var rawInput by remember { mutableStateOf("") }
    val parsedAmount = rawInput.filter { it.isDigit() }.toLongOrNull() ?: 0L
    val dismissSheet = rememberSheetDismiss(onDismiss)

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = dismissSheet,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(),
        dragHandle = { androidx.compose.material3.BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.chat_counter_sheet_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (buyerOfferAmountVnd > 0L) {
                Text(
                    text = stringResource(
                        R.string.chat_counter_sheet_buyer_offer,
                        formatPrice(buyerOfferAmountVnd),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "₫",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = FashColors.Primary,
                    modifier = Modifier.padding(end = 6.dp),
                )
                OutlinedTextField(
                    value = rawInput,
                    onValueChange = { rawInput = it.filter { c -> c.isDigit() } },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            stringResource(R.string.chat_offer_dialog_placeholder),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FashColors.Primary,
                    ),
                )
            }

            if (parsedAmount > 0L) {
                Text(
                    text = formatPrice(parsedAmount),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = FashColors.Primary,
                )
            }

            Button(
                onClick = { if (parsedAmount >= 1000L) onSubmit(parsedAmount) },
                enabled = parsedAmount >= 1000L && !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FashColors.Primary,
                    contentColor = FashColors.Primary.fashReadableOn(),
                ),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = FashColors.Primary.fashReadableOn(),
                    )
                } else {
                    Text(
                        text = stringResource(R.string.chat_counter_sheet_submit),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
            }
        }
    }
}

@Composable
private fun DealReviewBottomSheet(
    dealId: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (Int, String?) -> Unit,
) {
    var rating by remember(dealId) { mutableIntStateOf(5) }
    var comment by remember(dealId) { mutableStateOf("") }
    val dismissSheet = rememberSheetDismiss(onDismiss)
    val scheme = MaterialTheme.colorScheme

    Dialog(
        onDismissRequest = { if (!isLoading) dismissSheet() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = !isLoading,
            dismissOnClickOutside = false,
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scheme.scrim.copy(alpha = 0.52f))
                    .clickable(
                        enabled = !isLoading,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = dismissSheet,
                    ),
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* consume — taps stay on the card */ },
                    ),
                shape = RoundedCornerShape(20.dp),
                color = scheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 20.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.chat_offline_deal_review_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        for (star in 1..5) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(
                                        enabled = !isLoading,
                                        onClick = { rating = star },
                                    )
                                    .background(
                                        if (rating == star) {
                                            FashColors.Primary.copy(alpha = 0.16f)
                                        } else {
                                            Color.Transparent
                                        },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = star.toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = if (rating == star) FontWeight.Bold else FontWeight.Medium,
                                    color = if (rating >= star) {
                                        FashColors.Primary
                                    } else {
                                        scheme.outline
                                    },
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it.take(2000) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.chat_offline_deal_review_hint)) },
                        maxLines = 4,
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FashColors.Primary),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = dismissSheet,
                            enabled = !isLoading,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.chat_offline_deal_review_skip))
                        }
                        Button(
                            onClick = {
                                onSubmit(
                                    rating,
                                    comment.trim().takeIf { it.isNotEmpty() },
                                )
                            },
                            enabled = !isLoading,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = FashColors.Primary),
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = FashColors.Primary.fashReadableOn(),
                                )
                            } else {
                                Text(stringResource(R.string.chat_offline_deal_review_submit))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Formats VND amount as ₫80.000 (spec format with dot separators). */
private fun formatPrice(vnd: Long): String =
    "₫${"%,d".format(vnd).replace(',', '.')}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportConversationDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (category: String, description: String?) -> Unit,
) {
    var selectedCategory by remember { mutableStateOf("spam") }
    var description by remember { mutableStateOf("") }
    val categories = listOf(
        "spam" to R.string.chat_report_category_spam,
        "harassment" to R.string.chat_report_category_harassment,
        "scam" to R.string.chat_report_category_scam,
        "inappropriate" to R.string.chat_report_category_inappropriate,
        "other" to R.string.chat_report_category_other,
    )
    val isFormValid = selectedCategory.isNotBlank()

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() }, // ✅ fixed lambda
        title = { Text(stringResource(R.string.chat_report_dialog_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.chat_report_dialog_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                categories.forEach { (cat, labelRes) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isLoading) { selectedCategory = cat }
                            .padding(vertical = 4.dp),
                    ) {
                        RadioButton(
                            selected = selectedCategory == cat,
                            onClick = null,
                            enabled = !isLoading,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(labelRes),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 2000) description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.chat_report_description_label)) },
                    placeholder = { Text(stringResource(R.string.chat_report_description_hint)) },
                    supportingText = {
                        Text(
                            text = "${description.length}/2000",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    enabled = !isLoading,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FashColors.Primary,
                    ),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedCategory, description.trim().takeIf { it.isNotEmpty() }) },
                enabled = isFormValid && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = FashColors.Primary),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = FashColors.Primary.fashReadableOn(),
                    )
                } else {
                    Text(stringResource(R.string.chat_report_submit))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading,
            ) {
                Text(stringResource(R.string.create_listing_cancel))
            }
        },
    )
}
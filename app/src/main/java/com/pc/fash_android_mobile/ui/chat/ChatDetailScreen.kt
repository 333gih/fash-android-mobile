package com.pc.fash_android_mobile.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.chat.ChatMessage
import com.pc.fash_android_mobile.data.chat.OutboundSendState
import com.pc.fash_android_mobile.data.chat.ProductCard
import com.pc.fash_android_mobile.data.chat.PriceOffer
import com.pc.fash_android_mobile.ui.theme.FashColors
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Screen entry point
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    conversationId: String,
    viewModel: ChatDetailViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onProductClick: (listingId: String) -> Unit = {},
    /**
     * Called when the buyer taps "Pay Now" in the deal banner.
     * Carries the orderId, listingId and the accepted offer amount so the caller can open
     * CheckoutScreen with the correct price.
     */
    onPayNow: (orderId: String, listingId: String, acceptedAmountVnd: Long) -> Unit = { _, _, _ -> },
    /** Called when any party taps the banner area (view order details). */
    onOrderDetails: (orderId: String) -> Unit = {},
    /** Legacy callback kept for backward compat; not triggered by the offer→order flow. */
    onCheckout: (listingId: String, acceptedAmountVnd: Long) -> Unit = { _, _ -> },
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
    val isOtherTyping by viewModel.isOtherTyping.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Loads full detail when [conversationId] changes; same id + cached detail is a no-op in the ViewModel.
    LaunchedEffect(conversationId) {
        viewModel.loadConversation(conversationId)
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (detail != null) {
                        ChatDetailHeader(
                            displayName = detail!!.otherUser.displayName
                                .ifBlank { "@${detail!!.otherUser.username}" },
                            avatarUrl = detail!!.otherUser.avatarUrl,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
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
                val hasPendingOfferFromMe = d.pendingOffer?.proposedByMe == true
                val hasOrder = orderId != null
                val composerReadOnly =
                    d.isClosed || d.product?.listingStatus == "sold"
                val offerBlocked =
                    d.isClosed ||
                        d.product?.listingStatus == "sold" ||
                        d.product?.listingStatus == "reserved"
                val offerLimitReached = d.offerCount >= 3
                val sortedMessages = messages.sortedBy { it.timestamp }

                // The amount from the most-recently accepted offer (used for Pay Now)
                val acceptedOfferAmount = remember(sortedMessages) {
                    sortedMessages
                        .filter { it.messageType == "offer" && it.offerStatus == "accepted" }
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
                        visible = hasOrder,
                        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                    ) {
                        DealBanner(
                            isBuyer = d.isBuyer,
                            orderStatus = orderStatus,
                            onTap = { orderId?.let { onOrderDetails(it) } },
                            onPayNow = {
                                orderId?.let { oid ->
                                    onPayNow(
                                        oid,
                                        d.product?.listingId.orEmpty(),
                                        acceptedOfferAmount,
                                    )
                                }
                            },
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Product reference card
                    d.product?.let { product ->
                        ProductReferenceCard(
                            product = product,
                            onClick = { onProductClick(product.listingId) },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }

                    // Offer price bottom sheet
                    if (showOfferDialog) {
                        OfferPriceBottomSheet(
                            priceVnd = d.product?.priceVnd ?: 0L,
                            isLoading = isCreatingOffer,
                            onDismiss = { viewModel.dismissOfferDialog() },
                            onSubmit = { amt -> viewModel.createOffer(amt) },
                        )
                    }

                    // Messages
                    val listState = rememberLazyListState()

                    LaunchedEffect(sortedMessages.size) {
                        if (sortedMessages.isNotEmpty()) {
                            // reverseLayout=true → index 0 is the newest message (bottom)
                            listState.animateScrollToItem(0)
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (isMessagesLoading && sortedMessages.isEmpty()) {
                            MessageSkeletonList(
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else if (sortedMessages.isEmpty() && !isMessagesLoading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(R.string.chat_empty_messages),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    ),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                reverseLayout = true,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                if (isMessagesLoading) {
                                    item { LoadingMessageIndicator() }
                                }
                                itemsIndexed(
                                    items = sortedMessages.reversed(),
                                    key = { _, msg -> msg.messageId },
                                ) { _, msg ->
                                    when (msg.messageType) {
                                        "offer" -> OfferMessageBubble(
                                            message = msg,
                                            isBuyer = d.isBuyer,
                                            hasOrder = hasOrder,
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
                                            formatTime = viewModel::formatTime,
                                        )
                                        "system" -> SystemMessageBubble(
                                            message = msg,
                                            formatTime = viewModel::formatTime,
                                        )
                                        else -> if (msg.text.isNotBlank()) {
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

                    // Typing indicator — use expand/shrink so the bar animates smoothly
                    AnimatedVisibility(
                        visible = isOtherTyping,
                        enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(tween(220)),
                        exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(tween(180)),
                    ) {
                        TypingIndicator(name = d.otherUser.displayName.ifBlank { d.otherUser.username })
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

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
                            showOfferButton = d.isBuyer && !hasOrder,
                            offerButtonEnabled = !hasPendingOfferFromMe && !offerLimitReached && !offerBlocked,
                            offerShowLimitTooltip = d.isBuyer && !hasOrder && offerLimitReached && !offerBlocked,
                            onOfferClick = { viewModel.onSetPriceClick() },
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Deal banner (STATE B — order exists)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DealBanner(
    isBuyer: Boolean,
    orderStatus: String?,
    onTap: () -> Unit,
    onPayNow: () -> Unit = {},
) {
    val s = orderStatus?.trim()?.lowercase().orEmpty()
    val buyerNeedsToPay = isBuyer && s == "payment_pending"

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
            if (!buyerNeedsToPay) {
                Text(
                    text = stringResource(R.string.chat_deal_banner_view_order),
                    style = MaterialTheme.typography.labelSmall,
                    color = appearance.accent,
                )
            }
        }

        // Prominent "Pay Now" button — only for buyer awaiting payment
        if (buyerNeedsToPay) {
            Button(
                onClick = onPayNow,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp)
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
                    text = stringResource(R.string.chat_deal_pay_now),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
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
    statusNorm in listOf("payment_held", "in_transit") ->
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
    formatTime: (String) -> String,
) {
    val scheme = MaterialTheme.colorScheme

    // Show Accept/Decline only when: seller + offer pending + offer is from the other person
    val showSellerActions = !isBuyer &&
        message.offerStatus == "pending" &&
        !message.isFromMe &&
        !hasOrder

    // Buyer sees "Waiting..." subtext when their own pending offer exists
    val showBuyerWaiting = isBuyer &&
        message.offerStatus == "pending" &&
        message.isFromMe

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
                    color = scheme.outlineVariant.copy(alpha = 0.4f),
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
                    text = stringResource(R.string.chat_offer_label),
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

                showSellerActions -> {
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(
                                onClick = onDecline,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isMe) 18.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 18.dp,
                    ),
                )
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
                color = if (isMe) Color.White else scheme.onSurface,
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
                            color = Color.White.copy(alpha = 0.75f),
                            strokeWidth = 2.dp,
                        )
                        OutboundSendState.FAILED -> Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White.copy(alpha = 0.85f),
                        )
                        OutboundSendState.NONE -> Unit
                    }
                }
                Text(
                    text = formatTime(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isMe) Color.White.copy(alpha = 0.65f) else scheme.onSurfaceVariant.copy(alpha = 0.55f),
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
) {
    val scheme = MaterialTheme.colorScheme
    Row(
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
                Text(
                    text = (displayName.firstOrNull() ?: '?').uppercaseChar().toString(),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = FashColors.Primary,
                )
            }
        }
        Text(
            text = displayName,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
    onOfferClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val tooltipState = rememberTooltipState()

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
            if (offerShowLimitTooltip) {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = {
                        PlainTooltip {
                            Text(stringResource(R.string.chat_offer_limit_tooltip))
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
                    if (text.isNotBlank() && !isSending) FashColors.Primary
                    else scheme.surfaceContainerHigh,
                ),
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White,
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.chat_send),
                    tint = if (text.isNotBlank()) Color.White else scheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Offer price bottom sheet
// ─────────────────────────────────────────────────────────────────────────────

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

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                colors = ButtonDefaults.buttonColors(containerColor = FashColors.Primary),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
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
 * - Mini avatar uses [FashColors.Primary] tint with a letter fallback — identical to
 *   [ChatDetailHeader].
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
            Text(
                text = (name.firstOrNull() ?: '?').uppercaseChar().toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                ),
                color = FashColors.Primary,
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

/** Formats VND amount as ₫80.000 (spec format with dot separators). */
private fun formatPrice(vnd: Long): String =
    "₫${"%,d".format(vnd).replace(',', '.')}"


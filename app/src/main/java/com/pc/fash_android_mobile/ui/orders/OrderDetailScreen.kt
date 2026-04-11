package com.pc.fash_android_mobile.ui.orders

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.BuildConfig
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.components.FashSnackbarHost
import com.pc.fash_android_mobile.data.address.ShippingAddress
import com.pc.fash_android_mobile.ui.address.AddressBookViewModel
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.data.order.OrderMeetingGrace
import com.pc.fash_android_mobile.ui.theme.FashTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: String,
    viewModel: OrderDetailViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onNavigateToPayment: (listingId: String, amountVnd: Long, orderId: String) -> Unit,
    /** Opens chat when [OrderDetail.conversationId] is set. */
    onNavigateToChat: (conversationId: String) -> Unit = {},
    /** Opens the buyer/seller public profile by @username (from order counterparty card). */
    onOpenUserProfile: (username: String) -> Unit = {},
    /** Opens the listing (PDP); [sellerUserId] routes the current user’s own listing to edit when it matches. */
    onOpenListing: (listingId: String, sellerUserId: String) -> Unit = { _, _ -> },
    addressBookViewModel: AddressBookViewModel? = null,
    onOpenShippingAddressList: () -> Unit = {},
    onOpenAddShippingAddress: () -> Unit = {},
) {
    val detail by viewModel.detail.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val busy by viewModel.busyAction.collectAsState()
    val isBlocking = busy != OrderDetailBusyAction.None
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showReviewDialog by remember { mutableStateOf(false) }
    var reviewRating by remember { mutableFloatStateOf(5f) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showShipDialog by remember { mutableStateOf(false) }
    var trackingInput by remember { mutableStateOf("") }
    var carrierInput by remember { mutableStateOf("") }
    var showCancelConfirm by remember { mutableStateOf(false) }
    var showOpenDisputeDialog by remember { mutableStateOf(false) }
    var showEvidenceDialog by remember { mutableStateOf(false) }
    var openDisputeDesc by remember { mutableStateOf("") }
    var evidenceDesc by remember { mutableStateOf("") }
    val openDisputePhotoUrls = remember { mutableStateListOf<String>() }
    val evidencePhotoUrls = remember { mutableStateListOf<String>() }
    val scrollState = rememberScrollState()
    var pendingCheckInAppointmentId by remember { mutableStateOf<String?>(null) }
    var reviewComment by remember { mutableStateOf("") }
    var showEmptyAddressAlert by remember { mutableStateOf(false) }
    val emptyAddrList = remember { emptyList<ShippingAddress>() }
    val addresses by addressBookViewModel?.addresses?.collectAsState(initial = emptyAddrList)
        ?: remember { mutableStateOf(emptyAddrList) }

    fun readImageBytes(uri: Uri): Pair<ByteArray, String>? {
        val mime = context.contentResolver.getType(uri)?.takeIf { !it.contains('*') } ?: "image/jpeg"
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            Pair(stream.readBytes(), mime)
        }
    }

    val pickOpenDisputeImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        if (openDisputePhotoUrls.size >= 10) {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.order_detail_dispute_photo_limit))
            }
            return@rememberLauncherForActivityResult
        }
        val data = readImageBytes(uri) ?: return@rememberLauncherForActivityResult
        viewModel.uploadDisputePhoto(
            data.first,
            "dispute_${System.currentTimeMillis()}.jpg",
            data.second,
        ) { r ->
            r.onSuccess { url -> openDisputePhotoUrls.add(url) }
            r.onFailure { e ->
                scope.launch {
                    snackbarHostState.showSnackbar(
                        e.message ?: context.getString(R.string.create_listing_upload_error),
                    )
                }
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val pending = pendingCheckInAppointmentId ?: return@rememberLauncherForActivityResult
        pendingCheckInAppointmentId = null
        scope.launch {
            val loc = if (granted) MeetupCheckInLocation.peekLastKnownLatLng(context) else null
            viewModel.checkInAtMeeting(pending, loc?.first, loc?.second)
        }
    }

    val pickEvidenceImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        if (evidencePhotoUrls.size >= 10) {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.order_detail_dispute_photo_limit))
            }
            return@rememberLauncherForActivityResult
        }
        val data = readImageBytes(uri) ?: return@rememberLauncherForActivityResult
        viewModel.uploadDisputePhoto(
            data.first,
            "evidence_${System.currentTimeMillis()}.jpg",
            data.second,
        ) { r ->
            r.onSuccess { url -> evidencePhotoUrls.add(url) }
            r.onFailure { e ->
                scope.launch {
                    snackbarHostState.showSnackbar(
                        e.message ?: context.getString(R.string.create_listing_upload_error),
                    )
                }
            }
        }
    }

    LaunchedEffect(showOpenDisputeDialog) {
        if (showOpenDisputeDialog) {
            openDisputeDesc = ""
            openDisputePhotoUrls.clear()
        }
    }
    LaunchedEffect(showEvidenceDialog) {
        if (showEvidenceDialog) {
            evidenceDesc = ""
            evidencePhotoUrls.clear()
        }
    }

    LaunchedEffect(showReviewDialog) {
        if (showReviewDialog) reviewComment = ""
    }

    LaunchedEffect(orderId) {
        viewModel.load(orderId)
        scrollState.scrollTo(0)
    }
    LaunchedEffect(orderId) {
        val vm = addressBookViewModel ?: return@LaunchedEffect
        vm.refresh()
        // refresh() merges from API asynchronously; wait until it finishes before treating "no addresses".
        vm.loading.first { !it }
        if (vm.addresses.value.isEmpty()) {
            showEmptyAddressAlert = true
        }
    }
    LaunchedEffect(Unit) {
        viewModel.events.collect { msg -> snackbarHostState.showSnackbar(msg) }
    }
    LaunchedEffect(detail?.status, showOpenDisputeDialog) {
        if (showOpenDisputeDialog && detail?.status?.trim()?.lowercase() == "disputed") {
            showOpenDisputeDialog = false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { FashSnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.order_detail_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.orders_back),
                                tint = FashColors.Primary,
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showHelpDialog = true }) {
                            Icon(
                                Icons.Outlined.HelpOutline,
                                contentDescription = stringResource(R.string.order_detail_help_cd),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
                if (isRefreshing) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = FashColors.Primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        when {
            isLoading && detail == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(color = FashColors.Primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.order_detail_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            loadError != null && detail == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = loadError!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = { viewModel.load(orderId) }) {
                        Text(stringResource(R.string.feed_retry))
                    }
                }
            }
            detail != null && detail!!.orderId.equals(orderId.trim(), ignoreCase = true) -> {
                val d = detail!!
                val isBuyer = viewModel.isCurrentUserBuyer()
                val isSeller = viewModel.isCurrentUserSeller()
                val role = when {
                    isBuyer -> OrderViewerRole.Buyer
                    isSeller -> OrderViewerRole.Seller
                    else -> OrderViewerRole.Viewer
                }
                val formatDate: (String) -> String = { formatOrderDateTime(it) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = FashTheme.spacing.editorialStart)
                            .padding(top = FashTheme.spacing.spacing4)
                            .padding(bottom = OrderDetailStickyScrollBottomInset),
                    ) {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(280)) + slideInVertically(tween(280)) { it / 4 },
                        ) {
                            OrderHeroCard(d = d, role = role, formatDate = formatDate)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OrderTimelineSection(d = d, formatDate = formatDate)
                        Spacer(modifier = Modifier.height(12.dp))
                        if (d.meetupDeadlineAt.isNotBlank() && d.status.trim().lowercase() == "payment_pending") {
                            OrderMeetupDeadlineStrip(
                                meetupDeadlineAt = d.meetupDeadlineAt,
                                formatDate = formatDate,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        d.meetingAppointment?.let { meet ->
                            OrderMeetingSection(meeting = meet, formatDate = formatDate)
                            Spacer(modifier = Modifier.height(12.dp))
                            val grace = d.meetingGrace ?: OrderMeetingGrace()
                            val apptId = meet.id.trim().takeIf { it.isNotEmpty() }
                            OrderMeetingGraceSection(
                                grace = grace,
                                appointmentId = apptId,
                                busy = busy,
                                onCheckIn = { appointmentId ->
                                    when {
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.ACCESS_COARSE_LOCATION,
                                        ) == PackageManager.PERMISSION_GRANTED -> {
                                            scope.launch {
                                                val loc = MeetupCheckInLocation.peekLastKnownLatLng(context)
                                                viewModel.checkInAtMeeting(
                                                    appointmentId,
                                                    loc?.first,
                                                    loc?.second,
                                                )
                                            }
                                        }
                                        else -> {
                                            pendingCheckInAppointmentId = appointmentId
                                            locationPermissionLauncher.launch(
                                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                            )
                                        }
                                    }
                                },
                                onReportNoShow = { reason, note ->
                                    viewModel.reportMeetingNoShow(d.orderId, reason, note)
                                },
                                showAcknowledgeCash = d.canAcknowledgeOfflineCash && role == OrderViewerRole.Seller,
                                onAcknowledgeCash = { viewModel.acknowledgeOfflineCash(d.orderId) },
                                formatDate = formatDate,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        if (role == OrderViewerRole.Buyer && d.status.trim().lowercase() == "cancelled") {
                            BuyerCancelledNegotiationHint(
                                listingStatus = d.listingStatus,
                                maxOffersPerConversation = BuildConfig.CHAT_MAX_OFFERS_PER_CONVERSATION,
                                conversationId = d.conversationId,
                                onOpenChat = {
                                    val cid = d.conversationId.trim()
                                    if (cid.isNotEmpty()) onNavigateToChat(cid)
                                },
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        when (role) {
                            OrderViewerRole.Buyer -> {
                                val selectedLocal = addressBookViewModel?.getSelectionForOrder(d.orderId)
                                BuyerShippingAddressCard(
                                    d = d,
                                    selectedLocal = selectedLocal,
                                    onChangeClick = {
                                        if (addresses.isEmpty()) {
                                            showEmptyAddressAlert = true
                                        } else {
                                            onOpenShippingAddressList()
                                        }
                                    },
                                )
                                if (
                                    selectedLocal != null ||
                                        d.shippingAddressFormatted.isNotBlank() ||
                                        d.recipientName.isNotBlank()
                                ) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                                val sellerHandle = d.sellerUsername.trim()
                                CounterpartyCard(
                                    title = stringResource(R.string.order_detail_counterparty_seller),
                                    displayName = d.sellerDisplayName,
                                    username = d.sellerUsername,
                                    avatarUrl = d.sellerAvatarUrl,
                                    onClick = if (sellerHandle.isNotEmpty()) {
                                        { onOpenUserProfile(sellerHandle) }
                                    } else {
                                        null
                                    },
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OrderProductCard(
                                    d = d,
                                    showSellerHandle = true,
                                    onClick = if (d.listingId.isNotBlank()) {
                                        {
                                            onOpenListing(
                                                d.listingId.trim(),
                                                d.sellerUserId.trim(),
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                )
                            }
                            OrderViewerRole.Seller -> {
                                val buyerHandle = d.buyerUsername.trim()
                                CounterpartyCard(
                                    title = stringResource(R.string.order_detail_counterparty_buyer),
                                    displayName = d.buyerDisplayName,
                                    username = d.buyerUsername,
                                    avatarUrl = d.buyerAvatarUrl,
                                    onClick = if (buyerHandle.isNotEmpty()) {
                                        { onOpenUserProfile(buyerHandle) }
                                    } else {
                                        null
                                    },
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OrderProductCard(
                                    d = d,
                                    showSellerHandle = false,
                                    onClick = if (d.listingId.isNotBlank()) {
                                        {
                                            onOpenListing(
                                                d.listingId.trim(),
                                                d.sellerUserId.trim(),
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                )
                            }
                            OrderViewerRole.Viewer -> {
                                OrderProductCard(
                                    d = d,
                                    showSellerHandle = true,
                                    onClick = if (d.listingId.isNotBlank()) {
                                        {
                                            onOpenListing(
                                                d.listingId.trim(),
                                                d.sellerUserId.trim(),
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                )
                            }
                        }

                        d.buyerReview?.let { review ->
                            Spacer(modifier = Modifier.height(12.dp))
                            OrderBuyerReviewSection(
                                role = role,
                                review = review,
                                formatDate = formatDate,
                            )
                        }

                        if (d.trackingNumber.isNotBlank() || d.carrier.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OrderTrackingCard(d = d)
                        }
                    }

                    AnimatedVisibility(
                        visible = true,
                        modifier = Modifier.align(Alignment.BottomCenter),
                        enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 2 },
                        exit = fadeOut() + slideOutVertically { it / 2 },
                    ) {
                        OrderStickyBottomBar(
                            d = d,
                            role = role,
                            busy = busy,
                            onPay = {
                                onNavigateToPayment(d.listingId, d.amountVnd, d.orderId)
                            },
                            onCancelOrder = { showCancelConfirm = true },
                            onConfirmReceipt = { viewModel.confirmReceipt(d.orderId) },
                            onReview = { showReviewDialog = true },
                            onShip = {
                                trackingInput = d.trackingNumber
                                carrierInput = d.carrier.ifBlank { "" }
                                showShipDialog = true
                            },
                            onConfirmHandoff = { viewModel.confirmHandoff(d.orderId) },
                            onChat = {
                                val cid = d.conversationId.trim()
                                if (cid.isNotEmpty()) {
                                    onNavigateToChat(cid)
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            context.getString(R.string.order_detail_chat_unavailable),
                                        )
                                    }
                                }
                            },
                            onOpenDispute = { showOpenDisputeDialog = true },
                            onSubmitDisputeEvidence = { showEvidenceDialog = true },
                        )
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(color = FashColors.Primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.order_detail_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showEmptyAddressAlert) {
        AlertDialog(
            onDismissRequest = { showEmptyAddressAlert = false },
            title = { Text(stringResource(R.string.address_empty_alert_title)) },
            text = { Text(stringResource(R.string.address_empty_alert_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEmptyAddressAlert = false
                        onOpenAddShippingAddress()
                    },
                ) {
                    Text(stringResource(R.string.address_empty_alert_create))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyAddressAlert = false }) {
                    Text(stringResource(R.string.create_listing_cancel))
                }
            },
        )
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text(stringResource(R.string.order_detail_help_cd)) },
            text = { Text(stringResource(R.string.order_detail_help_body)) },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text(stringResource(R.string.order_detail_help_close))
                }
            },
        )
    }

    if (showCancelConfirm && detail != null) {
        val d = detail!!
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title = { Text(stringResource(R.string.order_cancel_confirm_title)) },
            text = { Text(stringResource(R.string.order_cancel_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelConfirm = false
                        viewModel.cancelOrder(d.orderId)
                    },
                    enabled = !isBlocking,
                ) {
                    Text(stringResource(R.string.order_cancel_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirm = false }) {
                    Text(stringResource(R.string.order_cancel_confirm_dismiss))
                }
            },
        )
    }

    if (showShipDialog && detail != null) {
        val d = detail!!
        AlertDialog(
            onDismissRequest = { showShipDialog = false },
            title = { Text(stringResource(R.string.order_detail_ship_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = trackingInput,
                        onValueChange = { trackingInput = it },
                        label = { Text(stringResource(R.string.order_detail_ship_tracking_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = carrierInput,
                        onValueChange = { carrierInput = it },
                        label = { Text(stringResource(R.string.order_detail_ship_carrier_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showShipDialog = false
                        viewModel.shipOrder(d.orderId, trackingInput, carrierInput)
                    },
                    enabled = !isBlocking && trackingInput.isNotBlank(),
                ) {
                    Text(stringResource(R.string.order_detail_ship_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showShipDialog = false }) {
                    Text(stringResource(R.string.create_listing_cancel))
                }
            },
        )
    }

    if (
        showReviewDialog &&
        detail != null &&
        detail!!.orderId.equals(orderId.trim(), ignoreCase = true) &&
        detail!!.canReview &&
        detail!!.buyerReview == null
    ) {
        val d = detail!!
        AlertDialog(
            onDismissRequest = { showReviewDialog = false },
            title = { Text(stringResource(R.string.order_detail_review_title)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.order_detail_review_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Slider(
                        value = reviewRating,
                        onValueChange = { reviewRating = it },
                        valueRange = 1f..5f,
                        steps = 3,
                    )
                    Text(
                        text = "${reviewRating.roundToInt()} / 5",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = FashColors.Primary,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = reviewComment,
                        onValueChange = { if (it.length <= 500) reviewComment = it },
                        label = { Text(stringResource(R.string.order_detail_review_comment_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 5,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showReviewDialog = false
                        val c = reviewComment.trim().ifBlank { null }
                        viewModel.submitReview(d.orderId, reviewRating.roundToInt(), c)
                    },
                    enabled = !isBlocking,
                ) {
                    Text(stringResource(R.string.order_detail_review_submit))
                }
            },
            dismissButton = {
                TextButton(onClick = { showReviewDialog = false }) {
                    Text(stringResource(R.string.create_listing_cancel))
                }
            },
        )
    }

    if (showOpenDisputeDialog && detail != null) {
        val d = detail!!
        AlertDialog(
            onDismissRequest = { showOpenDisputeDialog = false },
            title = { Text(stringResource(R.string.order_detail_dispute_dialog_title_open)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = openDisputeDesc,
                        onValueChange = { openDisputeDesc = it },
                        label = { Text(stringResource(R.string.order_detail_dispute_description_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 8,
                        enabled = !isBlocking,
                    )
                    OutlinedButton(
                        onClick = { pickOpenDisputeImage.launch("image/*") },
                        enabled = !isBlocking && openDisputePhotoUrls.size < 10,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.order_detail_dispute_add_photo, openDisputePhotoUrls.size))
                    }
                }
            },
            confirmButton = {
                val openDisputeBusy = busy == OrderDetailBusyAction.OpenDispute
                TextButton(
                    onClick = {
                        viewModel.openDispute(d.orderId, openDisputeDesc, openDisputePhotoUrls.toList())
                    },
                    enabled = openDisputeDesc.isNotBlank() && busy == OrderDetailBusyAction.None,
                ) {
                    if (openDisputeBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = FashColors.Primary,
                        )
                    } else {
                        Text(stringResource(R.string.order_detail_dispute_submit))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showOpenDisputeDialog = false }) {
                    Text(stringResource(R.string.create_listing_cancel))
                }
            },
        )
    }

    if (showEvidenceDialog && detail != null) {
        val d = detail!!
        AlertDialog(
            onDismissRequest = { showEvidenceDialog = false },
            title = { Text(stringResource(R.string.order_detail_dispute_dialog_title_evidence)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = evidenceDesc,
                        onValueChange = { evidenceDesc = it },
                        label = { Text(stringResource(R.string.order_detail_dispute_description_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 8,
                        enabled = !isBlocking,
                    )
                    OutlinedButton(
                        onClick = { pickEvidenceImage.launch("image/*") },
                        enabled = !isBlocking && evidencePhotoUrls.size < 10,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.order_detail_dispute_add_photo, evidencePhotoUrls.size))
                    }
                }
            },
            confirmButton = {
                val evidenceBusy = busy == OrderDetailBusyAction.SubmitEvidence
                TextButton(
                    onClick = {
                        viewModel.submitDisputeEvidence(d.orderId, evidenceDesc, evidencePhotoUrls.toList())
                    },
                    enabled = evidenceDesc.isNotBlank() && busy == OrderDetailBusyAction.None,
                ) {
                    if (evidenceBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = FashColors.Primary,
                        )
                    } else {
                        Text(stringResource(R.string.order_detail_dispute_submit))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showEvidenceDialog = false }) {
                    Text(stringResource(R.string.create_listing_cancel))
                }
            },
        )
    }
}

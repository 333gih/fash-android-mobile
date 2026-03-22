package com.pc.fash_android_mobile.ui.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.chat.ChatMessage
import com.pc.fash_android_mobile.data.chat.PriceOffer
import com.pc.fash_android_mobile.data.chat.ProductCard
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

private val AvatarSize = 40.dp
private val ProductThumbSize = 48.dp
private val BubbleCorner = RoundedCornerShape(16.dp)
private val ProductCardCorner = RoundedCornerShape(12.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    modifier: Modifier = Modifier,
    conversationId: String,
    viewModel: ChatDetailViewModel,
    onBack: () -> Unit,
    onProductClick: (String) -> Unit = {},
) {
    val detail by viewModel.detail.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val showOfferDialog by viewModel.showOfferDialog.collectAsState()
    val scheme = MaterialTheme.colorScheme

    LaunchedEffect(conversationId) {
        viewModel.loadConversation(conversationId)
    }

    BackHandler { onBack() }

    when {
        isLoading && detail == null -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = FashColors.Primary)
        }
        loadError != null && detail == null -> Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.orders_back),
                                tint = FashColors.Primary,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = scheme.surface,
                        navigationIconContentColor = FashColors.Primary,
                    ),
                )
            },
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(loadError!!, color = scheme.onSurfaceVariant)
                    OutlinedButton(
                        onClick = { viewModel.loadConversation(conversationId) },
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = FashColors.Primary,
                        ),
                    ) {
                        Text(stringResource(R.string.chat_retry))
                    }
                }
            }
        }
        detail != null -> {
            val d = detail!!
            Scaffold(
                modifier = modifier.fillMaxSize(),
                topBar = {
                    ChatDetailHeader(
                        otherName = d.otherUser.displayName.ifBlank { d.otherUser.username },
                        username = d.otherUser.username,
                        avatarUrl = d.otherUser.avatarUrl,
                        isOnline = d.otherUser.isOnline,
                        productThumbUrl = d.product?.imageUrl,
                        onBack = onBack,
                        onProductThumbClick = { d.product?.listingId?.let { onProductClick(it) } },
                    )
                },
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                ) {
                    d.product?.let { product ->
                        ProductReferenceCard(
                            product = product,
                            onViewProduct = { onProductClick(product.listingId) },
                        )
                    }
                    val pendingOffer = d.pendingOffer?.takeIf { it.status == "pending" && !it.proposedByMe }
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        state = rememberLazyListState(),
                        reverseLayout = true,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = FashTheme.spacing.editorialStart,
                            vertical = 12.dp,
                        ),
                    ) {
                        if (pendingOffer != null) {
                            item {
                                PriceOfferCard(
                                    offer = pendingOffer,
                                    formatPrice = ::formatPrice,
                                    onAccept = { viewModel.acceptOffer(pendingOffer) },
                                    onDecline = { viewModel.declineOffer(pendingOffer) },
                                    isResponding = viewModel.isRespondingToOffer.collectAsState().value,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                        items(messages.reversed(), key = { it.messageId }) { msg ->
                            MessageBubble(
                                message = msg,
                                formatTime = viewModel::formatTime,
                            )
                        }
                    }
                    ChatInputBar(
                        inputText = inputText,
                        onInputChange = viewModel::onInputChange,
                        onSetPrice = viewModel::onSetPriceClick,
                        onSend = viewModel::sendMessage,
                        isSending = viewModel.isSending.collectAsState().value,
                        product = d.product,
                    )
                }
            }
        }
    }

    var offerAmount by remember { mutableStateOf("") }
    if (showOfferDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissOfferDialog() },
            title = { Text(stringResource(R.string.chat_offer_dialog_title)) },
            text = {
                BasicTextField(
                    value = offerAmount,
                    onValueChange = { offerAmount = it.filter { c -> c.isDigit() } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .border(1.dp, scheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                    singleLine = true,
                    decorationBox = { inner ->
                        Box {
                            if (offerAmount.isEmpty()) {
                                Text(
                                    stringResource(R.string.chat_offer_dialog_hint),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                                )
                            }
                            inner()
                        }
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        offerAmount.toLongOrNull()?.let { viewModel.createOffer(it) }
                        offerAmount = ""
                    },
                ) {
                    Text(stringResource(R.string.chat_offer_dialog_submit), color = FashColors.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissOfferDialog(); offerAmount = "" }) {
                    Text(stringResource(R.string.create_listing_cancel), color = FashColors.Primary)
                }
            },
        )
    }
}

@Composable
private fun ChatDetailHeader(
    otherName: String,
    username: String,
    avatarUrl: String,
    isOnline: Boolean,
    productThumbUrl: String?,
    onBack: () -> Unit,
    onProductThumbClick: (() -> Unit)?,
) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val resolvedAvatar = avatarUrl.takeIf { it.isNotBlank() }?.let { resolveImageUrl(it) }
    val resolvedThumb = productThumbUrl?.takeIf { it.isNotBlank() }?.let { resolveImageUrl(it) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surface)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = scheme.onSurface,
            )
        }
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(AvatarSize)) {
                Box(
                    modifier = Modifier
                        .size(AvatarSize)
                        .clip(CircleShape)
                        .background(scheme.surfaceContainerHigh),
                ) {
                    if (resolvedAvatar != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(resolvedAvatar).build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
                if (isOnline) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(10.dp)
                            .background(FashColors.Success, CircleShape)
                            .padding(1.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = otherName.ifBlank { "@$username" },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = scheme.onSurface,
                )
                Text(
                    text = "@$username • ${stringResource(R.string.chat_detail_active)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        if (resolvedThumb != null && onProductThumbClick != null) {
            Box(
                modifier = Modifier
                    .size(ProductThumbSize)
                    .clip(RoundedCornerShape(8.dp))
                    .background(scheme.surfaceContainerHigh)
                    .clickable(onClick = onProductThumbClick),
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(resolvedThumb).build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

@Composable
private fun ProductReferenceCard(
    product: ProductCard,
    onViewProduct: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val imageUrl = product.imageUrl.takeIf { it.isNotBlank() }?.let { resolveImageUrl(it) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart, vertical = 8.dp),
        shape = ProductCardCorner,
        color = scheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onViewProduct)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(scheme.surfaceContainerHigh),
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(imageUrl).build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.title.ifBlank { "Sản phẩm" },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = scheme.onSurface,
                )
                Text(
                    text = formatPrice(product.priceVnd),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = FashColors.Primary,
                )
            }
            Text(
                text = stringResource(R.string.chat_detail_view_product),
                style = MaterialTheme.typography.labelMedium,
                color = FashColors.Primary,
            )
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    formatTime: (String) -> String,
) {
    val scheme = MaterialTheme.colorScheme
    val align = if (message.isFromMe) Alignment.End else Alignment.Start
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align,
    ) {
        Surface(
            shape = BubbleCorner,
            color = if (message.isFromMe) FashColors.Primary else scheme.surfaceContainerHigh,
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (message.isFromMe) FashColors.OnPrimary else scheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
        Row(
            modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = if (message.isFromMe) Arrangement.End else Arrangement.Start,
        ) {
            Text(
                text = formatTime(message.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
            if (message.isFromMe && message.isRead) {
                Text(
                    text = " • ${stringResource(R.string.chat_detail_read)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PriceOfferCard(
    offer: PriceOffer,
    formatPrice: (Long) -> String,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    isResponding: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = scheme.surface,
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AttachMoney,
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .background(FashColors.Primary.copy(alpha = 0.2f), CircleShape)
                        .padding(6.dp),
                    tint = FashColors.Primary,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.chat_detail_price_proposal),
                        style = MaterialTheme.typography.labelMedium,
                        color = FashColors.Primary,
                    )
                    Text(
                        text = formatPrice(offer.amountVnd),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onSurface,
                    )
                }
            }
            Text(
                text = stringResource(R.string.chat_detail_offer_prompt),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    enabled = !isResponding,
                    modifier = Modifier.weight(1f),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = FashColors.Primary,
                    ),
                ) {
                    Text(stringResource(R.string.chat_detail_decline))
                }
                androidx.compose.material3.Button(
                    onClick = onAccept,
                    enabled = !isResponding,
                    modifier = Modifier.weight(1f),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = FashColors.Success,
                        contentColor = FashColors.OnPrimary,
                    ),
                ) {
                    if (isResponding) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = FashColors.OnPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(stringResource(R.string.chat_detail_accept))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSetPrice: () -> Unit,
    onSend: () -> Unit,
    isSending: Boolean,
    product: ProductCard?,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = scheme.surface,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FashTheme.spacing.editorialStart, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (product != null) {
                OutlinedButton(
                    onClick = onSetPrice,
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = FashColors.Primary,
                    ),
                ) {
                    Text(
                        stringResource(R.string.chat_detail_set_price),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                color = scheme.surfaceContainerHighest,
            ) {
                BasicTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = scheme.onSurface),
                    singleLine = true,
                    decorationBox = { inner ->
                        Box {
                            if (inputText.isEmpty()) {
                                Text(
                                    stringResource(R.string.chat_detail_message_placeholder),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                                )
                            }
                            inner()
                        }
                    },
                )
            }
            IconButton(
                onClick = onSend,
                enabled = inputText.isNotBlank() && !isSending,
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = FashColors.Primary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(FashColors.Primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = FashColors.OnPrimary,
                        )
                    }
                }
            }
        }
    }
}

private fun resolveImageUrl(path: String): String {
    if (path.startsWith("http")) return path
    val base = com.pc.fash_android_mobile.config.AppEnvironment.apiBaseUrl.trimEnd('/')
    return if (path.startsWith("/")) "$base$path" else "$base/$path"
}

private fun formatPrice(vnd: Long): String =
    "đ ${"%,d".format(vnd).replace(',', '.')}"

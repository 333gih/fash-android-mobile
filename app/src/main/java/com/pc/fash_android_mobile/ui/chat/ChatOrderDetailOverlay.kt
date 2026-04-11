package com.pc.fash_android_mobile.ui.chat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.ui.address.AddressBookViewModel
import com.pc.fash_android_mobile.ui.orders.OrderDetailScreen
import com.pc.fash_android_mobile.ui.orders.OrderDetailViewModel
import kotlinx.coroutines.delay

private const val OVERLAY_HEIGHT_FRACTION = 0.58f
private const val EXIT_ANIM_MS = 260

/**
 * Full-width, medium-height order detail over chat: dimmed scrim, slide+fade from bottom,
 * rounded top surface (20dp) and Material drag handle (matches [ModalBottomSheet] styling).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatOrderDetailOverlay(
    orderId: String,
    onDismiss: () -> Unit,
    viewModel: OrderDetailViewModel,
    addressBookViewModel: AddressBookViewModel,
    onNavigateToPayment: (listingId: String, amountVnd: Long, orderId: String) -> Unit,
    onNavigateToChat: (conversationId: String) -> Unit,
    onOpenShippingAddressList: () -> Unit,
    onOpenAddShippingAddress: () -> Unit,
    onOpenUserProfile: (username: String) -> Unit = {},
    onOpenListing: (listingId: String, sellerUserId: String) -> Unit = { _, _ -> },
) {
    var visible by remember(orderId) { mutableStateOf(true) }

    fun requestClose() {
        if (visible) visible = false
    }

    LaunchedEffect(visible) {
        if (!visible) {
            delay(EXIT_ANIM_MS.toLong())
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = { requestClose() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            // Default true: the same tap that opened the sheet (or the first frame before the sheet
            // hit-tests) is often treated as "outside", so the dialog closes immediately.
            dismissOnClickOutside = false,
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.52f)),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(1f - OVERLAY_HEIGHT_FRACTION)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { requestClose() },
                    )
                    .semantics { role = Role.Button },
            )

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(220)) + slideInVertically(
                    animationSpec = tween(320),
                    initialOffsetY = { it },
                ),
                exit = fadeOut(tween(180)) + slideOutVertically(
                    animationSpec = tween(220),
                    targetOffsetY = { it },
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(OVERLAY_HEIGHT_FRACTION),
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            BottomSheetDefaults.DragHandle()
                        }
                        OrderDetailScreen(
                            orderId = orderId,
                            viewModel = viewModel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            onBack = { requestClose() },
                            onNavigateToPayment = onNavigateToPayment,
                            onNavigateToChat = onNavigateToChat,
                            onOpenUserProfile = onOpenUserProfile,
                            onOpenListing = onOpenListing,
                            addressBookViewModel = addressBookViewModel,
                            onOpenShippingAddressList = onOpenShippingAddressList,
                            onOpenAddShippingAddress = onOpenAddShippingAddress,
                        )
                    }
                }
            }
        }
    }

    BackHandler(enabled = visible) {
        requestClose()
    }
}

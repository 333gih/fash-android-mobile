package com.pc.fash_android_mobile.ui.orders

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.order.OrderRepository
import com.pc.fash_android_mobile.data.realtime.RealtimeEvent
import com.pc.fash_android_mobile.data.realtime.RealtimeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Global pending-payment banner: [OrderRepository.getPendingPaymentOrders] on a short poll + realtime.
 * Countdown is driven by [PendingPaymentOrderRow.remainingSecondsAtFetch] and [PendingPaymentBannerData.fetchedAtEpochMs]
 * (see UI tick); API [expired] / local remaining ≤ 0 show the expired state.
 */
class PendingPaymentViewModel(application: Application) : AndroidViewModel(application) {

    private val orderRepository: OrderRepository =
        (application as FashApplication).orderRepository
    private val orderCancelCoordinator =
        (application as FashApplication).orderCancelCoordinator
    private val realtimeManager: RealtimeManager =
        (application as FashApplication).realtimeManager

    /** Snackbar once per order when local countdown hits zero. */
    private val expiredSnackbarEmitted = mutableSetOf<String>()

    private val _banner = MutableStateFlow<PendingPaymentBannerData?>(null)
    val banner: StateFlow<PendingPaymentBannerData?> = _banner.asStateFlow()

    private val _events = MutableSharedFlow<PendingPaymentEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<PendingPaymentEvent> = _events.asSharedFlow()

    private var pollJob: Job? = null
    private var realtimeJob: Job? = null

    fun startMonitoring() {
        if (pollJob?.isActive == true) return
        realtimeJob = viewModelScope.launch {
            realtimeManager.events.collect { event ->
                if (event is RealtimeEvent.OrderStatusChanged) {
                    refresh()
                }
            }
        }
        pollJob = viewModelScope.launch {
            refresh()
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                refresh()
            }
        }
    }

    fun stopMonitoring() {
        pollJob?.cancel()
        pollJob = null
        realtimeJob?.cancel()
        realtimeJob = null
    }

    /** When local countdown reaches zero (UI tick). Idempotent per [orderId] for snackbar. */
    fun onDeadlineElapsed(orderId: String) {
        if (orderId.isBlank() || orderId in expiredSnackbarEmitted) return
        expiredSnackbarEmitted.add(orderId)
        viewModelScope.launch {
            _events.emit(PendingPaymentEvent.Expired(orderId))
        }
    }

    /** Buyer cancels a pending-payment order from the global banner. */
    fun cancelOrder(orderId: String) {
        if (orderId.isBlank()) return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { orderRepository.cancelOrder(orderId) }
            val app = getApplication<Application>()
            result.fold(
                onSuccess = {
                    withContext(Dispatchers.IO) {
                        orderCancelCoordinator.notifyBuyerCancelledOrderByOrderId(
                            orderId,
                            app.getString(R.string.chat_message_order_cancelled_by_buyer),
                        )
                    }
                    refresh()
                    _events.emit(PendingPaymentEvent.CancelSuccess)
                },
                onFailure = { e ->
                    _events.emit(PendingPaymentEvent.CancelFailed(mapCancelOrderError(e)))
                },
            )
        }
    }

    private fun mapCancelOrderError(e: Throwable): String {
        val app = getApplication<Application>()
        return when (e.message) {
            "ORDER_NOT_CANCELLABLE" -> app.getString(R.string.order_cancel_error_not_cancellable)
            "FORBIDDEN" -> app.getString(R.string.order_cancel_error_forbidden)
            "NOT_FOUND" -> app.getString(R.string.order_cancel_error_not_found)
            else -> e.message?.takeIf { it.isNotBlank() } ?: app.getString(R.string.order_cancel_error_generic)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val response = withContext(Dispatchers.IO) {
                orderRepository.getPendingPaymentOrders().getOrNull()
            } ?: return@launch

            if (response.orders.isEmpty()) {
                if (_banner.value != null) _banner.value = null
                return@launch
            }

            val now = System.currentTimeMillis()
            val rows = response.orders
                .asSequence()
                .filter { it.orderId.isNotBlank() && it.listingId.isNotBlank() }
                .sortedWith(compareBy({ it.remainingSeconds }, { it.paymentDeadlineAt }))
                .map { dto ->
                    PendingPaymentOrderRow(
                        orderId = dto.orderId,
                        listingId = dto.listingId,
                        title = dto.listingTitle,
                        imageUrl = dto.coverImageUrl,
                        amountVnd = dto.amountVnd,
                        remainingSecondsAtFetch = dto.remainingSeconds.coerceAtLeast(0),
                        expiredFromApi = dto.expired,
                    )
                }
                .toList()

            if (rows.isEmpty()) {
                if (_banner.value != null) _banner.value = null
                return@launch
            }

            _banner.value = PendingPaymentBannerData(
                fetchedAtEpochMs = now,
                paymentWindowMinutes = response.paymentWindowMinutes.coerceAtLeast(1),
                orders = rows,
            )
        }
    }

    companion object {
        private const val POLL_INTERVAL_MS = 30_000L
    }
}

data class PendingPaymentBannerData(
    val fetchedAtEpochMs: Long,
    val paymentWindowMinutes: Int,
    val orders: List<PendingPaymentOrderRow>,
)

data class PendingPaymentOrderRow(
    val orderId: String,
    val listingId: String,
    val title: String,
    val imageUrl: String,
    val amountVnd: Long,
    val remainingSecondsAtFetch: Int,
    val expiredFromApi: Boolean,
)

sealed interface PendingPaymentEvent {
    data class Expired(val orderId: String) : PendingPaymentEvent
    data object CancelSuccess : PendingPaymentEvent
    data class CancelFailed(val message: String) : PendingPaymentEvent
}

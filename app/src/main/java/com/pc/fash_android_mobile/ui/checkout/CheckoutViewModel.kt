package com.pc.fash_android_mobile.ui.checkout

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.BuildConfig
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.data.address.ShippingAddress
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.listing.ListingDetail
import com.pc.fash_android_mobile.data.listing.ListingRepository
import com.pc.fash_android_mobile.data.order.OrderDetail
import com.pc.fash_android_mobile.data.order.OrderRepository
import com.pc.fash_android_mobile.data.order.effectiveBuyerTotal
import com.pc.fash_android_mobile.data.payment.CheckoutAddress
import com.pc.fash_android_mobile.data.payment.CorePaymentRepository
import com.pc.fash_android_mobile.data.user.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One-shot UI actions (e.g. open gateway URL in Custom Tabs). */
sealed interface PaymentUiEvent {
    data class OpenPaymentUrl(val url: String) : PaymentUiEvent
}

data class PaymentMethodOption(
    val id: String,
    val name: String,
)

class CheckoutViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val listingRepository: ListingRepository =
        (application as FashApplication).listingRepository
    private val orderRepository: OrderRepository =
        (application as FashApplication).orderRepository
    private val corePaymentRepository: CorePaymentRepository =
        (application as FashApplication).corePaymentRepository
    private val userRepository: UserRepository =
        (application as FashApplication).userRepository
    private val orderCancelCoordinator =
        (application as FashApplication).orderCancelCoordinator

    private val _detail = MutableStateFlow<ListingDetail?>(null)
    val detail: StateFlow<ListingDetail?> = _detail.asStateFlow()

    private val _fullName = MutableStateFlow("")
    val fullName: StateFlow<String> = _fullName.asStateFlow()

    private val _phone = MutableStateFlow("")
    val phone: StateFlow<String> = _phone.asStateFlow()

    private val _address = MutableStateFlow("")
    val address: StateFlow<String> = _address.asStateFlow()

    private val _district = MutableStateFlow("")
    val district: StateFlow<String> = _district.asStateFlow()

    private val _city = MutableStateFlow("")
    val city: StateFlow<String> = _city.asStateFlow()

    /** When checkout was prefilled from the saved-address book; used for label/default badge in UI. */
    private val _shippingAddressForDisplay = MutableStateFlow<ShippingAddress?>(null)
    val shippingAddressForDisplay: StateFlow<ShippingAddress?> = _shippingAddressForDisplay.asStateFlow()

    private val _selectedPaymentIndex = MutableStateFlow(0)
    val selectedPaymentIndex: StateFlow<Int> = _selectedPaymentIndex.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _isCancelling = MutableStateFlow(false)
    val isCancelling: StateFlow<Boolean> = _isCancelling.asStateFlow()

    /** True after gateway URL opened until paid, cancelled, or poll timeout. */
    private val _awaitingGatewayReturn = MutableStateFlow(false)
    val awaitingGatewayReturn: StateFlow<Boolean> = _awaitingGatewayReturn.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    /** When non-zero, overrides the listing price (e.g. an accepted offer price). */
    private val _overridePriceVnd = MutableStateFlow(0L)
    val overridePriceVnd: StateFlow<Long> = _overridePriceVnd.asStateFlow()

    /** When set, [submitPayment] skips `POST /orders` (order already exists — e.g. from order detail). */
    private val _existingOrderId = MutableStateFlow<String?>(null)
    val existingOrderId: StateFlow<String?> = _existingOrderId.asStateFlow()

    /** Populated when [existingOrderId] is set — full order row for checkout UI. */
    private val _orderDetail = MutableStateFlow<OrderDetail?>(null)
    val orderDetail: StateFlow<OrderDetail?> = _orderDetail.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _paymentUiEvents = MutableSharedFlow<PaymentUiEvent>(extraBufferCapacity = 4)
    val paymentUiEvents: SharedFlow<PaymentUiEvent> = _paymentUiEvents.asSharedFlow()

    private var pollingJob: Job? = null
    private var pendingSuccess: ((String) -> Unit)? = null

    /**
     * Channel ids must match backend / payment-service enabled gateways (lowercase).
     * See ADDING_PAYMENT_PROVIDER.md — e.g. momo, vnpay, tpbank.
     */
    val paymentMethods = listOf(
        PaymentMethodOption("momo", "Ví MoMo"),
        PaymentMethodOption("vnpay", "VNPay"),
        PaymentMethodOption("tpbank", "Chuyển khoản TPBank"),
    )

    fun loadListing(listingId: String, overridePriceVnd: Long = 0L, existingOrderId: String? = null) {
        _overridePriceVnd.value = overridePriceVnd
        val oid = existingOrderId?.takeIf { it.isNotBlank() }
        _existingOrderId.value = oid
        if (listingId.isBlank()) {
            _loadError.value = getApplication<Application>().getString(R.string.checkout_load_error)
            _isLoading.value = false
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = null
            _detail.value = null
            _orderDetail.value = null
            _shippingAddressForDisplay.value = null
            coroutineScope {
                val listingAsync = async(Dispatchers.IO) { listingRepository.getListingDetail(listingId) }
                val orderAsync = oid?.let { id ->
                    async(Dispatchers.IO) { orderRepository.getOrderDetail(id) }
                }
                val listingResult = listingAsync.await()
                val orderResult = orderAsync?.await()
                _isLoading.value = false
                listingResult.fold(
                    onSuccess = { _detail.value = it },
                    onFailure = {
                        _loadError.value = it.message
                            ?: getApplication<Application>().getString(R.string.checkout_load_error)
                        _events.tryEmit(_loadError.value!!)
                    },
                )
                orderResult?.getOrNull()?.let { od ->
                    _orderDetail.value = od
                    if (od.recipientName.isNotBlank()) _fullName.value = od.recipientName
                    if (od.recipientPhone.isNotBlank()) _phone.value = od.recipientPhone
                    if (od.shippingAddressFormatted.isNotBlank()) {
                        _address.value = od.shippingAddressFormatted
                    }
                }
                applySavedAddressPrefill()
                applyProfileNamePrefill()
            }
        }
    }

    /** Fills display name from GET /users/me when address book left name empty. */
    private suspend fun applyProfileNamePrefill() {
        if (_fullName.value.isNotBlank()) return
        val profile = withContext(Dispatchers.IO) {
            userRepository.getMeProfile()
        }.getOrNull() ?: return
        val name = profile.displayName.trim()
        if (name.isNotEmpty()) _fullName.value = name
    }

    /**
     * Fills checkout fields from the local address book (order-linked or default).
     * When the user linked a saved address to this order on [OrderDetailScreen], that mapping wins
     * over partial shipping text from the order API row.
     */
    private fun applySavedAddressPrefill() {
        val app = getApplication<Application>() as FashApplication
        val uid = app.authManager.sessionStore.read()?.userId?.trim()?.takeIf { it.isNotEmpty() }
            ?: return
        val store = app.addressLocalStore
        val orderId = _existingOrderId.value?.trim()?.takeIf { it.isNotEmpty() }
        val list = store.listAddresses(uid)
        val mappedId = orderId?.let { store.getOrderAddressId(uid, it) }
        val picked: ShippingAddress? = if (orderId != null) {
            val mapped = mappedId?.let { id -> list.find { it.id == id } }
            mapped ?: list.firstOrNull { it.isDefault } ?: list.firstOrNull()
        } else {
            store.getDefaultOrFirst(uid)
        }
        _shippingAddressForDisplay.value = picked
        val c = picked?.toCheckoutAddress() ?: return
        if (orderId != null && mappedId != null) {
            _fullName.value = c.fullName
            _phone.value = c.phone
            _address.value = c.address
            _district.value = c.district
            _city.value = c.city
            return
        }
        if (_fullName.value.isBlank()) _fullName.value = c.fullName
        if (_phone.value.isBlank()) _phone.value = c.phone
        if (_address.value.isBlank()) _address.value = c.address
        if (_district.value.isBlank()) _district.value = c.district
        if (_city.value.isBlank()) _city.value = c.city
    }

    fun onFullNameChange(value: String) { _fullName.value = value }
    fun onPhoneChange(value: String) { _phone.value = value }
    fun onAddressChange(value: String) { _address.value = value }
    fun onDistrictChange(value: String) { _district.value = value }
    fun onCityChange(value: String) { _city.value = value }
    fun selectPaymentMethod(index: Int) { _selectedPaymentIndex.value = index }

    /** Product line (before shipping/discount). */
    val productPriceVnd: Long
        get() {
            val od = _orderDetail.value
            if (od != null && od.amountVnd > 0L) return od.amountVnd
            return _overridePriceVnd.value.takeIf { it > 0 } ?: (_detail.value?.priceVnd ?: 0L)
        }

    val shippingFeeVnd: Long
        get() {
            val od = _orderDetail.value
            if (od != null && od.shippingFeeVnd > 0L) return od.shippingFeeVnd
            return DEFAULT_SHIPPING_FEE_VND
        }

    val discountVnd: Long
        get() = _orderDetail.value?.discountVnd?.takeIf { it > 0L } ?: 0L

    /** Total the buyer pays (matches POST /orders amount_vnd for new orders). */
    val grandTotalVnd: Long
        get() {
            val od = _orderDetail.value
            if (od != null) {
                val t = od.effectiveBuyerTotal()
                if (t > 0L) return t
            }
            return (productPriceVnd + shippingFeeVnd - discountVnd).coerceAtLeast(1000L)
        }

    /** Legacy: platform fee for transparency (not added to buyer total in summary). */
    val platformFeeVnd: Long
        get() {
            val od = _orderDetail.value
            if (od != null && od.platformFeeVnd > 0L) return od.platformFeeVnd
            return (productPriceVnd * PLATFORM_FEE_PERCENT).toLong()
        }

    val platformFeeFromOrder: Boolean
        get() = _orderDetail.value?.platformFeeVnd?.let { it > 0L } == true

    val sellerPayoutVnd: Long
        get() = _orderDetail.value?.sellerPayoutVnd?.takeIf { it > 0L } ?: 0L

    fun canSubmit(): Boolean {
        if (_detail.value == null) return false
        if (_fullName.value.trim().isBlank()) return false
        if (_phone.value.trim().isBlank()) return false
        if (_address.value.trim().isBlank()) return false
        if (_district.value.trim().isBlank()) return false
        if (_city.value.trim().isBlank()) return false
        return true
    }

    /**
     * Creates or reuses order, calls core **proxied** payment initiate (returns gateway URL), opens URL in UI,
     * then polls order status until **payment_held** or terminal state.
     */
    /** Cancels an existing `payment_pending` order (buyer); used when resuming checkout for an unpaid order. */
    fun cancelPendingOrder(onSuccess: () -> Unit) {
        val oid = _existingOrderId.value?.trim()?.takeIf { it.isNotEmpty() } ?: return
        val st = _orderDetail.value?.status?.trim()?.lowercase().orEmpty()
        if (st != "payment_pending") return
        if (_isCancelling.value || _isSubmitting.value) return
        viewModelScope.launch {
            _isCancelling.value = true
            val result = withContext(Dispatchers.IO) { orderRepository.cancelOrder(oid) }
            _isCancelling.value = false
            val app = getApplication<Application>()
            result.fold(
                onSuccess = {
                    withContext(Dispatchers.IO) {
                        orderCancelCoordinator.notifyBuyerCancelledOrderByOrderId(
                            oid,
                            app.getString(R.string.chat_message_order_cancelled_by_buyer),
                        )
                    }
                    _events.tryEmit(app.getString(R.string.order_cancel_success))
                    onSuccess()
                },
                onFailure = { e ->
                    _events.tryEmit(mapCancelOrderError(e))
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

    fun submitPayment(onSuccess: (orderId: String) -> Unit) {
        if (!canSubmit() || _isSubmitting.value) return
        val d = _detail.value ?: return
        pendingSuccess = onSuccess
        viewModelScope.launch {
            _isSubmitting.value = true
            val existing = _existingOrderId.value
            val orderIdResult: Result<String> = if (existing != null) {
                Result.success(existing)
            } else {
                withContext(Dispatchers.IO) {
                    orderRepository.createOrder(d.id, grandTotalVnd)
                }
            }
            val orderId = orderIdResult.getOrElse { e ->
                _isSubmitting.value = false
                pendingSuccess = null
                _events.tryEmit(
                    e.message ?: getApplication<Application>().getString(R.string.checkout_payment_error),
                )
                return@launch
            }
            withContext(Dispatchers.IO) { orderRepository.getOrderDetail(orderId) }.getOrNull()?.let {
                _orderDetail.value = it
            }
            val idx = _selectedPaymentIndex.value.coerceIn(0, paymentMethods.lastIndex)
            val method = paymentMethods[idx]
            val initResult = withContext(Dispatchers.IO) {
                corePaymentRepository.initiatePayment(
                    orderId = orderId,
                    paymentMethod = method.id,
                    redirectUrl = BuildConfig.PAYMENT_REDIRECT_URL,
                    shipping = CheckoutAddress(
                        fullName = _fullName.value,
                        phone = _phone.value,
                        address = _address.value,
                        district = _district.value,
                        city = _city.value,
                    ),
                )
            }
            initResult.fold(
                onSuccess = { result ->
                    _isSubmitting.value = false
                    _awaitingGatewayReturn.value = true
                    _paymentUiEvents.tryEmit(PaymentUiEvent.OpenPaymentUrl(result.paymentUrl))
                    startPollingForPaid(orderId)
                },
                onFailure = { e ->
                    _isSubmitting.value = false
                    pendingSuccess = null
                    _events.tryEmit(
                        e.message
                            ?: getApplication<Application>().getString(R.string.checkout_payment_init_failed),
                    )
                },
            )
        }
    }

    private fun startPollingForPaid(orderId: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            repeat(POLL_ATTEMPTS) { attempt ->
                if (attempt > 0) delay(POLL_INTERVAL_MS)
                val od = withContext(Dispatchers.IO) {
                    orderRepository.getOrderDetail(orderId).getOrNull()
                }
                if (od != null) {
                    _orderDetail.value = od
                    when (od.status.lowercase()) {
                        "payment_held", "in_transit", "delivered_confirmed" -> {
                            finishPaidFlow(orderId)
                            return@launch
                        }
                        "cancelled", "disputed" -> {
                            _awaitingGatewayReturn.value = false
                            pendingSuccess = null
                            _events.tryEmit(
                                getApplication<Application>().getString(R.string.checkout_payment_cancelled_or_dispute),
                            )
                            return@launch
                        }
                    }
                }
                val paySt = withContext(Dispatchers.IO) {
                    corePaymentRepository.getPaymentStatus(orderId).getOrNull()
                }
                if (paySt != null) {
                    val esc = paySt.escrowStatus.lowercase()
                    val looksPaid = paySt.paidAt.isNotBlank() ||
                        esc.contains("held") || esc.contains("paid") ||
                        esc.contains("complete") || esc.contains("success")
                    if (looksPaid && !esc.contains("cancel")) {
                        val refreshed = withContext(Dispatchers.IO) {
                            orderRepository.getOrderDetail(orderId).getOrNull()
                        }
                        refreshed?.let { _orderDetail.value = it }
                        finishPaidFlow(orderId)
                        return@launch
                    }
                }
            }
            _awaitingGatewayReturn.value = false
            pendingSuccess = null
            _events.tryEmit(getApplication<Application>().getString(R.string.checkout_payment_poll_timeout))
        }
    }

    private fun finishPaidFlow(orderId: String) {
        _awaitingGatewayReturn.value = false
        _events.tryEmit(getApplication<Application>().getString(R.string.checkout_success))
        pendingSuccess?.invoke(orderId)
        pendingSuccess = null
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }

    private companion object {
        const val PLATFORM_FEE_PERCENT = 0.10
        const val DEFAULT_SHIPPING_FEE_VND = 30_000L
        const val POLL_INTERVAL_MS = 3_000L
        const val POLL_ATTEMPTS = 60
    }
}

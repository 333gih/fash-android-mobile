package com.pc.fash_android_mobile.ui.checkout

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.listing.ListingDetail
import com.pc.fash_android_mobile.data.listing.ListingRepository
import com.pc.fash_android_mobile.data.order.OrderDetail
import com.pc.fash_android_mobile.data.order.OrderRepository
import com.pc.fash_android_mobile.data.payment.CheckoutAddress
import com.pc.fash_android_mobile.data.payment.PaymentRequest
import com.pc.fash_android_mobile.data.payment.PaymentService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PLATFORM_FEE_PERCENT = 0.10

data class PaymentMethodOption(
    val id: String,
    val name: String,
    val logoRes: Int? = null,
)

class CheckoutViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val listingRepository: ListingRepository =
        (application as FashApplication).listingRepository
    private val orderRepository: OrderRepository =
        (application as FashApplication).orderRepository
    private val paymentService: PaymentService =
        (application as FashApplication).paymentService

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

    private val _selectedPaymentIndex = MutableStateFlow(0)
    val selectedPaymentIndex: StateFlow<Int> = _selectedPaymentIndex.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

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

    val paymentMethods = listOf(
        PaymentMethodOption("momo", "Ví MoMo"),
        PaymentMethodOption("zalopay", "ZaloPay"),
        PaymentMethodOption("vnpay", "VNPay"),
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
                orderResult?.getOrNull()?.let { _orderDetail.value = it }
            }
        }
    }

    fun onFullNameChange(value: String) { _fullName.value = value }
    fun onPhoneChange(value: String) { _phone.value = value }
    fun onAddressChange(value: String) { _address.value = value }
    fun onDistrictChange(value: String) { _district.value = value }
    fun onCityChange(value: String) { _city.value = value }
    fun selectPaymentMethod(index: Int) { _selectedPaymentIndex.value = index }

    val productPriceVnd: Long
        get() {
            val od = _orderDetail.value
            if (od != null && od.amountVnd > 0L) return od.amountVnd
            return _overridePriceVnd.value.takeIf { it > 0 } ?: (_detail.value?.priceVnd ?: 0L)
        }

    val platformFeeVnd: Long
        get() {
            val od = _orderDetail.value
            if (od != null && od.platformFeeVnd > 0L) return od.platformFeeVnd
            return (productPriceVnd * PLATFORM_FEE_PERCENT).toLong()
        }

    /** True when platform fee comes from the order API (not the 10%% estimate). */
    val platformFeeFromOrder: Boolean
        get() = _orderDetail.value?.platformFeeVnd?.let { it > 0L } == true

    val sellerPayoutVnd: Long
        get() = _orderDetail.value?.sellerPayoutVnd?.takeIf { it > 0L } ?: 0L

    val totalAmountVnd: Long
        get() = productPriceVnd + platformFeeVnd

    fun canSubmit(): Boolean {
        if (_detail.value == null) return false
        if (_fullName.value.trim().isBlank()) return false
        if (_phone.value.trim().isBlank()) return false
        if (_address.value.trim().isBlank()) return false
        if (_district.value.trim().isBlank()) return false
        if (_city.value.trim().isBlank()) return false
        return true
    }

    fun submitPayment(onSuccess: () -> Unit) {
        if (!canSubmit() || _isSubmitting.value) return
        val d = _detail.value ?: return
        viewModelScope.launch {
            _isSubmitting.value = true
            val existing = _existingOrderId.value
            val orderIdResult: Result<String> = if (existing != null) {
                Result.success(existing)
            } else {
                withContext(Dispatchers.IO) {
                    orderRepository.createOrder(d.id, productPriceVnd)
                }
            }
            _isSubmitting.value = false
            orderIdResult.fold(
                onSuccess = {
                    _events.tryEmit(getApplication<Application>().getString(R.string.checkout_success))
                    onSuccess()
                },
                onFailure = { e ->
                    _events.tryEmit(
                        e.message ?: getApplication<Application>().getString(R.string.checkout_payment_error),
                    )
                },
            )
        }
    }
}

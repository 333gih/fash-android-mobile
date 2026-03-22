package com.pc.fash_android_mobile.ui.checkout

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.listing.ListingDetail
import com.pc.fash_android_mobile.data.listing.ListingRepository
import com.pc.fash_android_mobile.data.payment.CheckoutAddress
import com.pc.fash_android_mobile.data.payment.PaymentRequest
import com.pc.fash_android_mobile.data.payment.PaymentService
import kotlinx.coroutines.Dispatchers
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

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    val paymentMethods = listOf(
        PaymentMethodOption("momo", "Ví MoMo"),
        PaymentMethodOption("zalopay", "ZaloPay"),
        PaymentMethodOption("vnpay", "VNPay"),
    )

    fun loadListing(listingId: String) {
        if (listingId.isBlank()) {
            _loadError.value = getApplication<Application>().getString(R.string.checkout_load_error)
            _isLoading.value = false
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = null
            _detail.value = null
            val result = withContext(Dispatchers.IO) {
                listingRepository.getListingDetail(listingId)
            }
            _isLoading.value = false
            result.fold(
                onSuccess = { _detail.value = it },
                onFailure = {
                    _loadError.value = it.message ?: getApplication<Application>().getString(R.string.checkout_load_error)
                    _events.tryEmit(_loadError.value!!)
                },
            )
        }
    }

    fun onFullNameChange(value: String) { _fullName.value = value }
    fun onPhoneChange(value: String) { _phone.value = value }
    fun onAddressChange(value: String) { _address.value = value }
    fun onDistrictChange(value: String) { _district.value = value }
    fun onCityChange(value: String) { _city.value = value }
    fun selectPaymentMethod(index: Int) { _selectedPaymentIndex.value = index }

    val productPriceVnd: Long
        get() = _detail.value?.priceVnd ?: 0L

    val platformFeeVnd: Long
        get() = (productPriceVnd * PLATFORM_FEE_PERCENT).toLong()

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
        val method = paymentMethods.getOrNull(_selectedPaymentIndex.value) ?: paymentMethods[0]
        viewModelScope.launch {
            _isSubmitting.value = true
            val request = PaymentRequest(
                listingId = d.id,
                productTitle = d.title,
                productPriceVnd = productPriceVnd,
                platformFeeVnd = platformFeeVnd,
                totalAmountVnd = totalAmountVnd,
                address = CheckoutAddress(
                    fullName = _fullName.value.trim(),
                    phone = _phone.value.trim(),
                    address = _address.value.trim(),
                    district = _district.value.trim(),
                    city = _city.value.trim(),
                ),
                paymentMethodId = method.id,
            )
            val result = withContext(Dispatchers.IO) {
                paymentService.processPayment(request)
            }
            _isSubmitting.value = false
            result.fold(
                onSuccess = {
                    _events.tryEmit(it.message ?: getApplication<Application>().getString(R.string.checkout_success))
                    onSuccess()
                },
                onFailure = {
                    _events.tryEmit(it.message ?: getApplication<Application>().getString(R.string.checkout_payment_error))
                },
            )
        }
    }
}

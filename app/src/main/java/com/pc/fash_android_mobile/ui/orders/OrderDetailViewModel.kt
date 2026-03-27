package com.pc.fash_android_mobile.ui.orders

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.listing.ListingRepository
import com.pc.fash_android_mobile.data.order.OrderDetail
import com.pc.fash_android_mobile.data.order.OrderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OrderDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val orderRepository: OrderRepository =
        (application as FashApplication).orderRepository
    private val listingRepository: ListingRepository =
        (application as FashApplication).listingRepository
    private val sessionStore = (application as FashApplication).authManager.sessionStore

    /**
     * Last order id the user asked to load. Used to drop stale HTTP responses when the user
     * switches orders quickly or navigates away before a slow request finishes.
     */
    private val _requestedOrderId = MutableStateFlow<String?>(null)

    private val _detail = MutableStateFlow<OrderDetail?>(null)
    val detail: StateFlow<OrderDetail?> = _detail.asStateFlow()

    /** True while the initial / switched order has no data yet (full-screen loader). */
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** True while re-fetching the same order (keep showing previous detail, top progress). */
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    private val _isWorking = MutableStateFlow(false)
    val isWorking: StateFlow<Boolean> = _isWorking.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    /**
     * True when the logged-in user is the buyer. Matches [AuthSession.userId] to the order's
     * buyer id **or** buyer username — some backends return UUID in the order while the session
     * stores username (or the reverse), so id-only checks hide the Pay button incorrectly.
     */
    fun isCurrentUserBuyer(): Boolean {
        val my = sessionStore.read()?.userId?.trim().orEmpty()
        if (my.isBlank()) return false
        val d = _detail.value ?: return false
        val buyerId = d.buyerUserId.trim()
        val buyerName = d.buyerUsername.trim()
        if (buyerId.isNotBlank() && my.equals(buyerId, ignoreCase = true)) return true
        if (buyerName.isNotBlank() && my.equals(buyerName, ignoreCase = true)) return true
        return false
    }

    /** True when the logged-in user is the seller (matches id or username). */
    fun isCurrentUserSeller(): Boolean {
        val my = sessionStore.read()?.userId?.trim().orEmpty()
        if (my.isBlank()) return false
        val d = _detail.value ?: return false
        val sellerId = d.sellerUserId.trim()
        val sellerName = d.sellerUsername.trim()
        if (sellerId.isNotBlank() && my.equals(sellerId, ignoreCase = true)) return true
        if (sellerName.isNotBlank() && my.equals(sellerName, ignoreCase = true)) return true
        return false
    }

    /**
     * Marks the order as shipped (seller). Refreshes detail on success.
     */
    fun shipOrder(orderId: String, trackingNumber: String, carrier: String) {
        val oid = orderId.trim()
        if (oid.isBlank()) return
        val tn = trackingNumber.trim()
        val c = carrier.trim().ifBlank { "—" }
        if (tn.isBlank()) {
            _events.tryEmit(getApplication<Application>().getString(R.string.order_detail_ship_tracking_required))
            return
        }
        viewModelScope.launch {
            _isWorking.value = true
            val result = withContext(Dispatchers.IO) {
                orderRepository.shipOrder(oid, tn, c)
            }
            _isWorking.value = false
            result.fold(
                onSuccess = {
                    _events.tryEmit(getApplication<Application>().getString(R.string.order_detail_ship_success))
                    load(oid)
                },
                onFailure = {
                    _events.tryEmit(
                        it.message ?: getApplication<Application>().getString(R.string.order_detail_ship_error),
                    )
                },
            )
        }
    }

    /**
     * Loads order detail for [orderId]. Clears cached detail when switching to a different order
     * so the UI never shows another order’s data while waiting.
     */
    fun load(orderId: String) {
        val clean = orderId.trim()
        if (clean.isBlank()) {
            _loadError.value = getApplication<Application>().getString(R.string.order_detail_load_error)
            _isLoading.value = false
            _isRefreshing.value = false
            return
        }
        val prior = _requestedOrderId.value
        _requestedOrderId.value = clean
        _loadError.value = null
        val switchedOrder = prior == null || !prior.equals(clean, ignoreCase = true)
        if (switchedOrder) {
            _detail.value = null
            _isLoading.value = true
            _isRefreshing.value = false
        } else {
            _isRefreshing.value = true
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { orderRepository.getOrderDetail(clean) }
            if (!_requestedOrderId.value.equals(clean, ignoreCase = true)) {
                return@launch
            }
            _isLoading.value = false
            _isRefreshing.value = false
            result.fold(
                onSuccess = { order ->
                    if (!_requestedOrderId.value.equals(clean, ignoreCase = true)) return@fold
                    _detail.value = order
                    _loadError.value = null
                },
                onFailure = { e ->
                    if (!_requestedOrderId.value.equals(clean, ignoreCase = true)) return@fold
                    val msg = e.message
                        ?: getApplication<Application>().getString(R.string.order_detail_load_error)
                    if (_detail.value == null) {
                        _loadError.value = msg
                    } else {
                        _events.tryEmit(msg)
                    }
                },
            )
        }
    }

    fun confirmReceipt(orderId: String) {
        if (orderId.isBlank()) return
        viewModelScope.launch {
            _isWorking.value = true
            val result = withContext(Dispatchers.IO) { orderRepository.confirmReceipt(orderId) }
            _isWorking.value = false
            result.fold(
                onSuccess = {
                    _events.tryEmit(getApplication<Application>().getString(R.string.orders_confirm_success))
                    load(orderId)
                },
                onFailure = {
                    _events.tryEmit(
                        it.message ?: getApplication<Application>().getString(R.string.orders_confirm_error),
                    )
                },
            )
        }
    }

    fun submitReview(orderId: String, rating: Int, comment: String?) {
        if (orderId.isBlank()) return
        viewModelScope.launch {
            _isWorking.value = true
            val result = withContext(Dispatchers.IO) {
                orderRepository.submitReview(orderId, rating, comment)
            }
            _isWorking.value = false
            result.fold(
                onSuccess = {
                    _events.tryEmit(getApplication<Application>().getString(R.string.order_detail_review_sent))
                    load(orderId)
                },
                onFailure = {
                    _events.tryEmit(
                        it.message ?: getApplication<Application>().getString(R.string.order_detail_review_error),
                    )
                },
            )
        }
    }

    /** Upload a photo for dispute / evidence (uses listing image endpoint → signed URL). */
    fun uploadDisputePhoto(
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
        onResult: (Result<String>) -> Unit,
    ) {
        if (bytes.isEmpty()) {
            onResult(Result.failure(IllegalArgumentException("empty image")))
            return
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                listingRepository.uploadListingImage(bytes, fileName, mimeType)
            }
            onResult(result)
        }
    }

    fun openDispute(orderId: String, description: String, photoUrls: List<String>) {
        val oid = orderId.trim()
        if (oid.isBlank()) return
        if (description.trim().isEmpty()) {
            _events.tryEmit(getApplication<Application>().getString(R.string.order_detail_dispute_description_required))
            return
        }
        viewModelScope.launch {
            _isWorking.value = true
            val result = withContext(Dispatchers.IO) {
                orderRepository.openDispute(oid, description, photoUrls)
            }
            _isWorking.value = false
            result.fold(
                onSuccess = {
                    _events.tryEmit(getApplication<Application>().getString(R.string.order_detail_dispute_open_success))
                    load(oid)
                },
                onFailure = {
                    _events.tryEmit(
                        it.message ?: getApplication<Application>().getString(R.string.order_detail_dispute_error),
                    )
                },
            )
        }
    }

    fun submitDisputeEvidence(orderId: String, description: String, photoUrls: List<String>) {
        val oid = orderId.trim()
        if (oid.isBlank()) return
        if (description.trim().isEmpty()) {
            _events.tryEmit(getApplication<Application>().getString(R.string.order_detail_dispute_description_required))
            return
        }
        viewModelScope.launch {
            _isWorking.value = true
            val result = withContext(Dispatchers.IO) {
                orderRepository.submitDisputeEvidence(oid, description, photoUrls)
            }
            _isWorking.value = false
            result.fold(
                onSuccess = {
                    _events.tryEmit(getApplication<Application>().getString(R.string.order_detail_dispute_evidence_success))
                    load(oid)
                },
                onFailure = {
                    _events.tryEmit(
                        it.message ?: getApplication<Application>().getString(R.string.order_detail_dispute_error),
                    )
                },
            )
        }
    }
}

package com.pc.fash_android_mobile.ui.orders

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.order.OrderItem
import com.pc.fash_android_mobile.data.order.OrderRepository
import com.pc.fash_android_mobile.data.realtime.RealtimeEvent
import com.pc.fash_android_mobile.data.realtime.RealtimeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OrdersViewModel(application: Application) : AndroidViewModel(application) {

    private val orderRepository: OrderRepository =
        (application as FashApplication).orderRepository
    private val realtimeManager: RealtimeManager =
        (application as FashApplication).realtimeManager

    private val _selectedTab = MutableStateFlow(0) // 0: Buying, 1: Selling
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _buyingOrders = MutableStateFlow<List<OrderItem>>(emptyList())
    val buyingOrders: StateFlow<List<OrderItem>> = _buyingOrders.asStateFlow()

    private val _sellingOrders = MutableStateFlow<List<OrderItem>>(emptyList())
    val sellingOrders: StateFlow<List<OrderItem>> = _sellingOrders.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    private val _confirmingOrderId = MutableStateFlow<String?>(null)
    val confirmingOrderId: StateFlow<String?> = _confirmingOrderId.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        // INTEGRATION.md §5 order.status_changed: payment/shipping updates arrive via WS —
        // silently refresh the list so the buyer/seller sees the new status immediately
        viewModelScope.launch {
            realtimeManager.events.collect { event ->
                if (event is RealtimeEvent.OrderStatusChanged) {
                    silentRefreshOrders()
                }
            }
        }
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    /** Quietly re-fetches both buying and selling orders without showing a loading indicator. */
    private suspend fun silentRefreshOrders() {
        val buyingResult = withContext(Dispatchers.IO) { orderRepository.getBuyingOrders() }
        val sellingResult = withContext(Dispatchers.IO) { orderRepository.getSellingOrders() }
        buyingResult.getOrNull()?.let { _buyingOrders.value = it }
        sellingResult.getOrNull()?.let { _sellingOrders.value = it }
    }

    fun loadOrders() {
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = null
            val buyingResult = withContext(Dispatchers.IO) { orderRepository.getBuyingOrders() }
            val sellingResult = withContext(Dispatchers.IO) { orderRepository.getSellingOrders() }
            buyingResult.onSuccess { _buyingOrders.value = it }
                .onFailure {
                    _loadError.value = it.message
                    _buyingOrders.value = emptyList()
                }
            sellingResult.onSuccess { _sellingOrders.value = it }
                .onFailure {
                    if (_loadError.value == null) _loadError.value = it.message
                    _sellingOrders.value = emptyList()
                }
            _isLoading.value = false
        }
    }

    fun confirmReceipt(orderId: String) {
        if (orderId.isBlank()) return
        viewModelScope.launch {
            _confirmingOrderId.value = orderId
            val result = withContext(Dispatchers.IO) {
                orderRepository.confirmReceipt(orderId)
            }
            _confirmingOrderId.value = null
            result.fold(
                onSuccess = {
                    _events.tryEmit(getApplication<Application>().getString(R.string.orders_confirm_success))
                    loadOrders()
                },
                onFailure = {
                    _events.tryEmit(it.message ?: getApplication<Application>().getString(R.string.orders_confirm_error))
                },
            )
        }
    }

    fun retryLoad() {
        loadOrders()
    }
}

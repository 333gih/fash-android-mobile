package com.pc.fash_android_mobile.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.data.order.OrderItem
import com.pc.fash_android_mobile.data.order.OrderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val BuyerDeliveringStatuses = setOf(
    "payment_held",
    "in_transit",
    "delivering",
    "shipped",
    "shipping",
)

class HomeDeliveringViewModel(application: Application) : AndroidViewModel(application) {

    private val orderRepository: OrderRepository =
        (application as FashApplication).orderRepository

    private val _orders = MutableStateFlow<List<OrderItem>>(emptyList())
    val orders: StateFlow<List<OrderItem>> = _orders.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    private val _confirmingOrderId = MutableStateFlow<String?>(null)
    val confirmingOrderId: StateFlow<String?> = _confirmingOrderId.asStateFlow()

    fun clearCachesForSignedOutUser() {
        _orders.value = emptyList()
        _isLoading.value = false
        _isRefreshing.value = false
        _loadError.value = null
        _confirmingOrderId.value = null
    }

    fun loadIfShippingEnabled(shippingEnabled: Boolean) {
        if (!shippingEnabled) return
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = null
            fetchDelivering()
            _isLoading.value = false
        }
    }

    fun refresh(shippingEnabled: Boolean) {
        if (!shippingEnabled) return
        viewModelScope.launch {
            _isRefreshing.value = true
            _loadError.value = null
            fetchDelivering()
            _isRefreshing.value = false
        }
    }

    private suspend fun fetchDelivering() {
        val result = withContext(Dispatchers.IO) {
            orderRepository.getBuyingOrders(limit = 50, offset = 0)
        }
        result.fold(
            onSuccess = { list ->
                _orders.value = list.filter { it.status.trim() in BuyerDeliveringStatuses }
            },
            onFailure = { e ->
                _loadError.value = e.message?.takeIf { it.isNotBlank() }
                _orders.value = emptyList()
            },
        )
    }

    fun confirmReceipt(orderId: String) {
        if (orderId.isBlank() || _confirmingOrderId.value != null) return
        viewModelScope.launch {
            _confirmingOrderId.value = orderId
            val result = withContext(Dispatchers.IO) { orderRepository.confirmReceipt(orderId) }
            result.fold(
                onSuccess = {
                    fetchDelivering()
                },
                onFailure = { },
            )
            _confirmingOrderId.value = null
        }
    }
}

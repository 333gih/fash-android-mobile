package com.pc.fash_android_mobile.ui.address

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.address.AddressLocalStore
import com.pc.fash_android_mobile.data.address.ShippingAddress
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class AddressBookViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val store: AddressLocalStore = (application as FashApplication).addressLocalStore
    private val sessionStore = (application as FashApplication).authManager.sessionStore

    private val _addresses = MutableStateFlow<List<ShippingAddress>>(emptyList())
    val addresses: StateFlow<List<ShippingAddress>> = _addresses.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private fun userId(): String? = sessionStore.read()?.userId?.trim()?.takeIf { it.isNotEmpty() }

    fun refresh() {
        val uid = userId() ?: return
        _addresses.value = store.listAddresses(uid)
    }

    fun getSelectionForOrder(orderId: String): ShippingAddress? {
        val uid = userId() ?: return null
        val oid = orderId.trim().lowercase()
        val list = store.listAddresses(uid)
        val mappedId = store.getOrderAddressId(uid, oid)
        if (mappedId != null) {
            list.find { it.id == mappedId }?.let { return it }
        }
        return list.firstOrNull { it.isDefault } ?: list.firstOrNull()
    }

    fun setOrderShipping(orderId: String, address: ShippingAddress) {
        val uid = userId() ?: return
        store.setOrderAddressId(uid, orderId, address.id)
        refresh()
    }

    fun addAddress(
        recipientName: String,
        phone: String,
        city: String,
        district: String,
        ward: String,
        line1: String,
        isDefault: Boolean,
    ): String? {
        val uid = userId() ?: return null
        val list = store.listAddresses(uid)
        val mustDefault = list.isEmpty()
        val addr = ShippingAddress(
            id = UUID.randomUUID().toString(),
            recipientName = recipientName.trim(),
            phone = phone.trim(),
            city = city.trim(),
            district = district.trim(),
            ward = ward.trim(),
            line1 = line1.trim(),
            isDefault = isDefault || mustDefault,
        )
        store.upsertAddress(uid, addr)
        refresh()
        _events.tryEmit(getApplication<Application>().getString(R.string.address_saved_success))
        return addr.id
    }

    fun addressById(id: String): ShippingAddress? =
        _addresses.value.find { it.id == id }

    fun setDefaultAddress(addressId: String) {
        val uid = userId() ?: return
        store.setDefault(uid, addressId)
        refresh()
    }

    fun applyDefaultToCheckoutIfNeeded(): ShippingAddress? {
        val uid = userId() ?: return null
        return store.getDefaultOrFirst(uid)
    }

    /** Pre-select in list: order-linked id, else default, else first. */
    fun initialSelectionIdForOrder(orderId: String): String? {
        val uid = userId() ?: return null
        val oid = orderId.trim().lowercase()
        store.getOrderAddressId(uid, oid)?.let { return it }
        val list = store.listAddresses(uid)
        return list.firstOrNull { it.isDefault }?.id ?: list.firstOrNull()?.id
    }
}

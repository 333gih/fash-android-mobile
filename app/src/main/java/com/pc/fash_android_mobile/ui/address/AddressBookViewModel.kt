package com.pc.fash_android_mobile.ui.address

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.address.AddressLocalStore
import com.pc.fash_android_mobile.data.address.CreateUserShippingAddressRequest
import com.pc.fash_android_mobile.data.address.ShippingAddress
import com.pc.fash_android_mobile.data.address.UserShippingAddressRepository
import com.pc.fash_android_mobile.data.address.mergeShippingAddressesWithLocal
import com.pc.fash_android_mobile.data.common.CommonAddressDto
import com.pc.fash_android_mobile.data.common.CommonServiceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddressBookViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val app = application as FashApplication
    private val store: AddressLocalStore = app.addressLocalStore
    private val shippingRepo: UserShippingAddressRepository = app.userShippingAddressRepository
    private val commonRepo: CommonServiceRepository = app.commonServiceRepository
    private val sessionStore = app.authManager.sessionStore

    private val _addresses = MutableStateFlow<List<ShippingAddress>>(emptyList())
    val addresses: StateFlow<List<ShippingAddress>> = _addresses.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _provincesLoading = MutableStateFlow(false)
    val provincesLoading: StateFlow<Boolean> = _provincesLoading.asStateFlow()

    private val _provinces = MutableStateFlow<List<CommonAddressDto>>(emptyList())
    val provinces: StateFlow<List<CommonAddressDto>> = _provinces.asStateFlow()

    private val _districts = MutableStateFlow<List<CommonAddressDto>>(emptyList())
    val districts: StateFlow<List<CommonAddressDto>> = _districts.asStateFlow()

    private val _wards = MutableStateFlow<List<CommonAddressDto>>(emptyList())
    val wards: StateFlow<List<CommonAddressDto>> = _wards.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private fun userId(): String? = sessionStore.read()?.userId?.trim()?.takeIf { it.isNotEmpty() }

    /**
     * Loads from core API and merges [phone] from local rows with matching ids; falls back to local on failure.
     */
    fun refresh() {
        val uid = userId() ?: return
        // Show cached addresses immediately so callers (e.g. order detail) don't race empty in-memory state.
        _addresses.value = store.listAddresses(uid)
        _loading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                shippingRepo.listShippingAddresses().fold(
                    onSuccess = { api ->
                        val local = store.listAddresses(uid)
                        val merged = mergeShippingAddressesWithLocal(api, local)
                        store.saveAddresses(uid, merged)
                        _addresses.value = merged
                    },
                    onFailure = {
                        _addresses.value = store.listAddresses(uid)
                        _events.tryEmit(
                            app.getString(R.string.address_sync_failed),
                        )
                    },
                )
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearCachesForSignedOutUser() {
        _addresses.value = emptyList()
        _loading.value = false
    }

    fun loadProvincesIfNeeded() {
        if (_provinces.value.isNotEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            _provincesLoading.value = true
            try {
                commonRepo.getProvincesCatalog().fold(
                    onSuccess = { _provinces.value = it },
                    onFailure = {
                        _events.tryEmit(app.getString(R.string.address_catalog_load_failed))
                    },
                )
            } finally {
                _provincesLoading.value = false
            }
        }
    }

    fun onProvinceSelected(provinceId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            if (provinceId.isNullOrBlank()) {
                _districts.value = emptyList()
                _wards.value = emptyList()
                return@launch
            }
            commonRepo.getAdministrativeChildren(parentId = provinceId, childLevel = 2).fold(
                onSuccess = { _districts.value = it },
                onFailure = {
                    _districts.value = emptyList()
                    _events.tryEmit(app.getString(R.string.address_catalog_load_failed))
                },
            )
            _wards.value = emptyList()
        }
    }

    fun onDistrictSelected(districtId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            if (districtId.isNullOrBlank()) {
                _wards.value = emptyList()
                return@launch
            }
            commonRepo.getAdministrativeChildren(parentId = districtId, childLevel = 3).fold(
                onSuccess = { _wards.value = it },
                onFailure = {
                    _wards.value = emptyList()
                    _events.tryEmit(app.getString(R.string.address_catalog_load_failed))
                },
            )
        }
    }

    fun resetAdministrativeDropdowns() {
        _districts.value = emptyList()
        _wards.value = emptyList()
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

    /**
     * Creates a saved address on core-service and syncs local store; [phone] is kept only locally.
     */
    fun createShippingAddress(
        label: String,
        recipientName: String,
        phone: String,
        line1: String,
        line2: String,
        city: String,
        region: String,
        postalCode: String,
        countryCode: String,
        provinceId: String?,
        provinceName: String,
        districtId: String?,
        districtName: String,
        wardId: String?,
        wardName: String,
        isDefault: Boolean,
        onResult: (Result<String>) -> Unit,
    ) {
        val uid = userId()
        if (uid == null) {
            onResult(Result.failure(IllegalStateException("not signed in")))
            return
        }
        val list = store.listAddresses(uid)
        val mustDefault = list.isEmpty()
        val req = CreateUserShippingAddressRequest(
            line1 = line1.trim(),
            countryCode = countryCode.trim().uppercase().take(2).ifBlank { "VN" },
            label = label.trim(),
            recipientName = recipientName.trim(),
            line2 = line2.trim(),
            city = city.trim(),
            region = region.trim(),
            postalCode = postalCode.trim(),
            isDefault = isDefault || mustDefault,
            provinceId = provinceId?.takeIf { it.isNotBlank() },
            provinceName = provinceName.trim(),
            districtId = districtId?.takeIf { it.isNotBlank() },
            districtName = districtName.trim(),
            wardId = wardId?.takeIf { it.isNotBlank() },
            wardName = wardName.trim(),
        )
        viewModelScope.launch(Dispatchers.IO) {
            shippingRepo.createShippingAddress(req).fold(
                onSuccess = { created ->
                    val withPhone = created.copy(phone = phone.trim())
                    val merged = mergeShippingAddressesWithLocal(listOf(withPhone), store.listAddresses(uid))
                    val final = merged.first()
                    store.upsertAddress(uid, final)
                    _addresses.value = store.listAddresses(uid)
                    withContext(Dispatchers.Main) {
                        onResult(Result.success(final.id))
                    }
                    _events.tryEmit(getApplication<Application>().getString(R.string.address_saved_success))
                },
                onFailure = { e ->
                    withContext(Dispatchers.Main) {
                        onResult(Result.failure(e))
                    }
                },
            )
        }
    }

    fun addressById(id: String): ShippingAddress? =
        _addresses.value.find { it.id == id }

    fun setDefaultAddress(addressId: String) {
        val uid = userId() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            shippingRepo.setDefaultShippingAddress(addressId).fold(
                onSuccess = {
                    shippingRepo.listShippingAddresses().onSuccess { api ->
                        val local = store.listAddresses(uid)
                        val merged = mergeShippingAddressesWithLocal(api, local)
                        store.saveAddresses(uid, merged)
                        _addresses.value = merged
                    }
                },
                onFailure = {
                    _events.tryEmit(it.message ?: getApplication<Application>().getString(R.string.address_default_failed))
                },
            )
        }
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

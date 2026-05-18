package com.pc.fash_android_mobile.ui.sellerpackages

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.data.sellerpackages.SellerProductPackage
import com.pc.fash_android_mobile.data.sellerpackages.SellerProductPackageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SellerProductPackagesViewModel(application: Application) : AndroidViewModel(application) {

    private val repo: SellerProductPackageRepository =
        (application as FashApplication).sellerProductPackageRepository

    private val _packages = MutableStateFlow<List<SellerProductPackage>>(emptyList())
    val packages: StateFlow<List<SellerProductPackage>> = _packages.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _loadError.value = null
            repo.listPackages(activeOnly = true).fold(
                onSuccess = { res ->
                    _packages.value = res.packages
                },
                onFailure = { e ->
                    _packages.value = emptyList()
                    _loadError.value = e.message
                },
            )
            _isLoading.value = false
        }
    }
}

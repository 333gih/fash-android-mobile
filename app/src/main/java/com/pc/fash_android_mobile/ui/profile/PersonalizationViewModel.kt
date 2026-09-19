package com.pc.fash_android_mobile.ui.profile

import android.app.Application
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.data.user.ProfileInfo
import com.pc.fash_android_mobile.data.user.ProfilePatch
import com.pc.fash_android_mobile.data.user.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class PersonalizationViewModel(application: Application) : AndroidViewModel(application) {

    private val fashApp = application as FashApplication
    private val userRepository: UserRepository = fashApp.userRepository

    private val _profile = MutableStateFlow<ProfileInfo?>(null)
    val profile: StateFlow<ProfileInfo?> = _profile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isUploadingPhoto = MutableStateFlow(false)
    val isUploadingPhoto: StateFlow<Boolean> = _isUploadingPhoto.asStateFlow()

    private val _photoError = MutableStateFlow<String?>(null)
    val photoError: StateFlow<String?> = _photoError.asStateFlow()

    fun load() {
        // Fast path: use canonical profile from app-level store to avoid a redundant network call.
        val cached = fashApp.userProfileStore.profile.value
        if (cached != null) {
            _profile.value = cached
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val result = withContext(Dispatchers.IO) { userRepository.getMeProfile() }
            _isLoading.value = false
            result.onSuccess {
                _profile.value = it
                fashApp.userProfileStore.update(it)
            }
        }
    }

    fun handlePhotoSelection(
        uri: Uri,
        contentResolver: ContentResolver,
        uploadErrorMessage: String,
    ) {
        viewModelScope.launch {
            _isUploadingPhoto.value = true
            _photoError.value = null
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                    val rawBytes = contentResolver.openInputStream(uri)?.readBytes()
                        ?: error("Cannot read image")
                    // Compress to max 1200px / 82% JPEG before upload.
                    val bytes = compressImageBytes(rawBytes, maxDimension = 1200, jpegQuality = 82)
                        ?: rawBytes
                    val uploadResult = userRepository.uploadProfileImage(
                        bytes = bytes,
                        filename = "avatar.jpg",
                        type = "avatar",
                        mimeType = "image/jpeg",
                    )
                    val url = uploadResult.getOrThrow()
                    // Persist URL to server in background — no getMeProfile reload needed.
                    userRepository.updateProfile(ProfilePatch(avatarUrl = url)).getOrThrow()
                    url
                }
            }
            result.onSuccess { url ->
                // Optimistic local update: patch profile in memory without a full reload.
                val current = _profile.value
                if (current != null) {
                    val updated = current.copy(avatarUrl = url)
                    _profile.value = updated
                    fashApp.userProfileStore.update(updated)
                }
            }.onFailure {
                _photoError.value = uploadErrorMessage
            }
            _isUploadingPhoto.value = false
        }
    }

    private fun compressImageBytes(bytes: ByteArray, maxDimension: Int, jpegQuality: Int): ByteArray? {
        return runCatching {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            val origMax = maxOf(opts.outWidth, opts.outHeight)
            if (origMax <= maxDimension && bytes.size <= 400_000) return bytes
            val scale = if (origMax > maxDimension) maxDimension.toFloat() / origMax else 1f
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(opts, maxDimension, maxDimension)
            }
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
                ?: return null
            val newW = (bmp.width * scale).toInt().coerceAtLeast(1)
            val newH = (bmp.height * scale).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(bmp, newW, newH, true)
            if (scaled !== bmp) bmp.recycle()
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, jpegQuality, out)
            scaled.recycle()
            out.toByteArray()
        }.getOrNull()
    }

    private fun calculateInSampleSize(opts: BitmapFactory.Options, reqW: Int, reqH: Int): Int {
        val (h, w) = opts.outHeight to opts.outWidth
        var inSampleSize = 1
        if (h > reqH || w > reqW) {
            val halfH = h / 2
            val halfW = w / 2
            while ((halfH / inSampleSize) >= reqH && (halfW / inSampleSize) >= reqW) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}

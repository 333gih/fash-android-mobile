package com.pc.fash_android_mobile.data.listing

import android.graphics.BitmapFactory
import android.net.Uri
import android.content.Context

object ListingImagePixelSize {
    private const val MAX_DIMENSION = 32_000

    fun fromBytes(bytes: ByteArray): Pair<Int, Int>? {
        if (bytes.isEmpty()) return null
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        val w = opts.outWidth
        val h = opts.outHeight
        return if (w in 1..MAX_DIMENSION && h in 1..MAX_DIMENSION) w to h else null
    }

    fun fromUri(context: Context, uri: Uri): Pair<Int, Int>? =
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(stream, null, opts)
            val w = opts.outWidth
            val h = opts.outHeight
            if (w in 1..MAX_DIMENSION && h in 1..MAX_DIMENSION) w to h else null
        }
}

data class ListingImageUploadResult(
    val url: String,
    val width: Int? = null,
    val height: Int? = null,
)

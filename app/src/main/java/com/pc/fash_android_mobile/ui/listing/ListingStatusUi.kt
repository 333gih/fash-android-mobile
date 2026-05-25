package com.pc.fash_android_mobile.ui.listing

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.pc.fash_android_mobile.R
import java.util.Locale

@Composable
fun listingStatusOverlayLabel(wire: String?): String? {
    if (wire.isNullOrBlank()) return null
    return when (wire.trim().lowercase(Locale.ROOT)) {
        "in_review" -> stringResource(R.string.listing_status_in_review)
        "rejected" -> stringResource(R.string.listing_status_rejected)
        "active" -> stringResource(R.string.listing_status_active)
        "inactive" -> stringResource(R.string.listing_status_inactive)
        "sold" -> stringResource(R.string.listing_status_sold)
        "reserved" -> stringResource(R.string.listing_status_reserved)
        "deleted" -> stringResource(R.string.listing_status_deleted)
        else -> wire
    }
}

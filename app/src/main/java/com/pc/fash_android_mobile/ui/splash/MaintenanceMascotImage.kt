package com.pc.fash_android_mobile.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R

@Composable
fun MaintenanceMascotImage(
    modifier: Modifier = Modifier,
    maxHeightDp: Int = 220,
) {
    Image(
        painter = painterResource(R.drawable.maintenance_mascot),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .height(maxHeightDp.dp)
            .widthIn(max = maxHeightDp.dp),
    )
}

package com.pc.fash_android_mobile.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun FashMascotGuideImage(
    resId: Int,
    modifier: Modifier = Modifier,
    sizeDp: Int = 72,
) {
    Image(
        painter = painterResource(resId),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(sizeDp.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp), clip = false),
    )
}

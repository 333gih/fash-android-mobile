package com.pc.fash_android_mobile.ui.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.onboarding.PreLoginMascotGuideStore
import com.pc.fash_android_mobile.ui.components.FashMascotGuideImage
import com.pc.fash_android_mobile.ui.components.FashPrimaryButton
import com.pc.fash_android_mobile.ui.theme.FashColors

enum class PreLoginMascotGuideContext {
    LoginScreen,
    GuestShell,
}

private data class PreLoginSlide(
    val titleRes: Int,
    val bodyRes: Int,
    val mascotRes: Int,
)

@Composable
fun PreLoginMascotGuideOverlay(
    context: PreLoginMascotGuideContext,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appContext = LocalContext.current.applicationContext
    val slides = remember(context) {
        when (context) {
            PreLoginMascotGuideContext.LoginScreen -> listOf(
                PreLoginSlide(R.string.app_tour_intro_title, R.string.app_tour_intro_body, R.drawable.fash_mascot_point_up),
                PreLoginSlide(R.string.pre_login_login_email_title, R.string.pre_login_login_email_body, R.drawable.fash_mascot_point_down),
                PreLoginSlide(R.string.pre_login_login_social_title, R.string.pre_login_login_social_body, R.drawable.fash_mascot_point_left),
                PreLoginSlide(R.string.pre_login_login_guest_title, R.string.pre_login_login_guest_body, R.drawable.fash_mascot_point_up),
            )
            PreLoginMascotGuideContext.GuestShell -> listOf(
                PreLoginSlide(R.string.app_tour_intro_title, R.string.app_tour_intro_body, R.drawable.fash_mascot_point_up),
                PreLoginSlide(R.string.app_tour_nav_home_title, R.string.app_tour_nav_home_body, R.drawable.fash_mascot_point_left),
                PreLoginSlide(R.string.pre_login_guest_sign_in_title, R.string.pre_login_guest_sign_in_body, R.drawable.fash_mascot_point_down),
            )
        }
    }
    var index by remember { mutableIntStateOf(0) }
    val isLast = index >= slides.lastIndex
    val slide = slides[index]

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.58f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = {
                    PreLoginMascotGuideStore.markCompleted(appContext)
                    onFinish()
                }) {
                    Text(
                        text = stringResource(R.string.app_tour_skip),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                    FashMascotGuideImage(resId = slide.mascotRes, sizeDp = 88)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(slide.titleRes),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(slide.bodyRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            GuidePageIndicator(
                pageCount = slides.size,
                currentPage = index,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            FashPrimaryButton(
                onClick = {
                    if (isLast) {
                        PreLoginMascotGuideStore.markCompleted(appContext)
                        onFinish()
                    } else {
                        index += 1
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                label = stringResource(if (isLast) R.string.app_tour_done else R.string.welcome_intro_next),
            )
        }
    }
}

@Composable
private fun GuidePageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { page ->
            val selected = page == currentPage
            val width by animateDpAsState(
                targetValue = if (selected) 18.dp else 6.dp,
                animationSpec = tween(durationMillis = 220),
                label = "guideDot",
            )
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(width)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (selected) FashColors.Primary else scheme.outlineVariant.copy(alpha = 0.55f),
                    ),
            )
        }
    }
}

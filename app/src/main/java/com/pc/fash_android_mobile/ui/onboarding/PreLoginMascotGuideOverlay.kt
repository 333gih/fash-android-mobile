package com.pc.fash_android_mobile.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.onboarding.PreLoginMascotGuideStore

enum class PreLoginMascotGuideContext {
    LoginScreen,
    GuestShell,
}

private data class PreLoginGuideSlide(
    val titleRes: Int,
    val bodyRes: Int,
    val anchor: FeatureTourAnchor?,
)

@Composable
fun PreLoginMascotGuideOverlay(
    context: PreLoginMascotGuideContext,
    anchors: Map<FeatureTourAnchor, LayoutCoordinates>,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appContext = LocalContext.current.applicationContext
    val slides = remember(context) {
        when (context) {
            PreLoginMascotGuideContext.LoginScreen -> listOf(
                PreLoginGuideSlide(R.string.app_tour_intro_title, R.string.app_tour_intro_body, null),
                PreLoginGuideSlide(R.string.pre_login_login_email_title, R.string.pre_login_login_email_body, FeatureTourAnchor.LoginEmailForm),
                PreLoginGuideSlide(R.string.pre_login_login_social_title, R.string.pre_login_login_social_body, FeatureTourAnchor.LoginSocialRow),
                PreLoginGuideSlide(R.string.pre_login_login_guest_title, R.string.pre_login_login_guest_body, FeatureTourAnchor.LoginGuestBrowse),
            )
            PreLoginMascotGuideContext.GuestShell -> listOf(
                PreLoginGuideSlide(R.string.app_tour_intro_title, R.string.app_tour_intro_body, null),
                PreLoginGuideSlide(R.string.app_tour_nav_home_title, R.string.app_tour_nav_home_body, FeatureTourAnchor.BottomHome),
                PreLoginGuideSlide(R.string.pre_login_guest_sign_in_title, R.string.pre_login_guest_sign_in_body, FeatureTourAnchor.BottomProfile),
            )
        }
    }
    var index by remember { mutableIntStateOf(0) }
    val slide = slides[index]
    val isLast = index >= slides.lastIndex

    MascotSpotlightOverlay(
        title = stringResource(slide.titleRes),
        bodyText = stringResource(slide.bodyRes),
        anchor = slide.anchor,
        anchors = anchors,
        stepIndex = index,
        stepCount = slides.size,
        showBack = index > 0,
        isLast = isLast,
        onBack = { index = (index - 1).coerceAtLeast(0) },
        onNext = {
            if (isLast) {
                PreLoginMascotGuideStore.markCompleted(appContext)
                onFinish()
            } else {
                index += 1
            }
        },
        onSkip = {
            PreLoginMascotGuideStore.markCompleted(appContext)
            onFinish()
        },
        modifier = modifier,
    )
}

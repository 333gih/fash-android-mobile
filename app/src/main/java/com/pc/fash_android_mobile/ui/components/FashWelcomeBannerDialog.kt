package com.pc.fash_android_mobile.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.data.promo.AppPromoCampaign
import com.pc.fash_android_mobile.data.promo.AppPromoCampaignKind
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.welcome.WelcomeDialogStore

/**
 * @deprecated Use [FashAppPromoOverlayDialog] with [com.pc.fash_android_mobile.data.promo.AppPromoCampaignResolver].
 */
@Deprecated("Use FashAppPromoOverlayDialog")
@Composable
fun FashWelcomeBannerDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    bottomOverlayInset: Dp = 0.dp,
) {
    if (!visible) return
    val welcome = AppPromoCampaign(
        id = WelcomeDialogStore.CAMPAIGN_ID,
        version = WelcomeDialogStore.CURRENT_WELCOME_VERSION,
        kind = AppPromoCampaignKind.Welcome,
        titleRes = R.string.welcome_banner_dialog_title,
        messageRes = R.string.welcome_banner_dialog_message,
        primaryActionRes = R.string.welcome_banner_dialog_action,
    )
    FashAppPromoOverlayDialog(
        campaign = welcome,
        onDismiss = onDismiss,
        onPrimaryClick = { onDismiss() },
    )
}

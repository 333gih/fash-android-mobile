package com.pc.fash_android_mobile.data.promo

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.pc.fash_android_mobile.ui.main.MainTab

/** Applies admin-configured button actions for [AppPromoCampaignKind.Remote]. */
object AppPromoNavigation {
    fun applyPrimary(
        activity: Activity,
        campaign: AppPromoCampaign,
        onTab: (MainTab) -> Unit,
        onOpenOrders: () -> Unit = {},
        onOpenExplore: () -> Unit = {},
        onOpenSellerPackages: () -> Unit = {},
    ) {
        if (campaign.kind == AppPromoCampaignKind.SellerPackage) {
            onOpenSellerPackages()
            return
        }
        campaign.primaryAction?.let { apply(activity, it, onTab, onOpenOrders, onOpenExplore, onOpenSellerPackages) }
    }

    fun applySecondary(
        activity: Activity,
        campaign: AppPromoCampaign,
        onTab: (MainTab) -> Unit,
        onOpenOrders: () -> Unit = {},
        onOpenExplore: () -> Unit = {},
        onOpenSellerPackages: () -> Unit = {},
    ) {
        campaign.secondaryAction?.let { apply(activity, it, onTab, onOpenOrders, onOpenExplore, onOpenSellerPackages) }
    }

    private fun apply(
        activity: Activity,
        action: AppPromoButtonAction,
        onTab: (MainTab) -> Unit,
        onOpenOrders: () -> Unit,
        onOpenExplore: () -> Unit,
        onOpenSellerPackages: () -> Unit,
    ) {
        when (action.type.trim().lowercase()) {
            "external_url", "deeplink" -> {
                val url = action.payload.trim()
                if (url.isNotEmpty()) {
                    runCatching {
                        CustomTabsIntent.Builder().setShowTitle(true).build()
                            .launchUrl(activity, Uri.parse(url))
                    }.onFailure {
                        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                }
            }
            "in_app_explore" -> onOpenExplore()
            "in_app_orders" -> onOpenOrders()
            "in_app_chat" -> onTab(MainTab.Chat)
            "in_app_post_tab" -> onTab(MainTab.Post)
            "in_app_product_packages" -> onOpenSellerPackages()
            "in_app_listing" -> {
                val listingId = action.payload.trim()
                if (listingId.isNotEmpty()) {
                    onOpenExplore()
                }
            }
            else -> Unit
        }
    }
}

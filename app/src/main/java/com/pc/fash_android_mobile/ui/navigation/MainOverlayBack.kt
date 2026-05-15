package com.pc.fash_android_mobile.ui.navigation

import com.pc.fash_android_mobile.ui.explore.ExplorePrimarySection

/**
 * Describes where the seller shop overlay was opened from so [dismissSellerShopOverlay]
 * can restore the screen underneath.
 */
enum class SellerShopEntrySource {
    None,
    ProductDetail,
    Explore,
    /** Chat header / order overlay in chat; restores [SellerShopRestoreContext.chatConversationId]. */
    Chat,
    FollowConnections,
    FeaturedSellers,
    Orders,
    HomeDelivering,
    /** Order detail full screen; keeps [SellerShopRestoreContext.orderId] composed under the shop. */
    OrderDetail,
}

/**
 * Optional payloads saved when opening seller shop (and cleared on dismiss).
 */
data class SellerShopRestoreContext(
    val exploreSection: ExplorePrimarySection? = null,
    val chatConversationId: String? = null,
    val followConnectionsTab: Int = 0,
    val orderId: String? = null,
    val reopenOrders: Boolean = false,
    val reopenHomeDelivering: Boolean = false,
    val reopenFeaturedSellers: Boolean = false,
)

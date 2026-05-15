package com.pc.fash_android_mobile.config

import com.pc.fash_android_mobile.BuildConfig

/**
 * Compile-time **business-flow** settings from `env/{dev,prod}.env` → [BuildConfig].
 *
 * Keep values aligned with core-service and the product flows described in
 * **`ANDROID_END_TO_END_BUSINESS_FLOW.md`** (§3 flows, §7 Android UI/UX).
 * These are **not** fetched at runtime; rebuild when switching environments.
 */
object BusinessFlowConfig {

    /**
     * Max buyer offers per conversation (server enforces the same cap).
     *
     * Env: `CHAT_MAX_OFFERS_PER_CONVERSATION` → [BuildConfig.CHAT_MAX_OFFERS_PER_CONVERSATION].
     */
    val maxOffersPerConversation: Int
        get() = BuildConfig.CHAT_MAX_OFFERS_PER_CONVERSATION

    /**
     * Env: `C2C_SHIP_FULFILLMENT_ENABLED` — ship path in chat fulfillment chooser (meetup vs ship).
     */
    val c2cShipFulfillmentEnabled: Boolean
        get() = BuildConfig.C2C_SHIP_FULFILLMENT_ENABLED

    /**
     * Env: `C2C_SHIP_ONLINE_PAYMENT_ENABLED` — in-app checkout / payment step for the ship path.
     */
    val c2cShipOnlinePaymentEnabled: Boolean
        get() = BuildConfig.C2C_SHIP_ONLINE_PAYMENT_ENABLED
}

package com.pc.fash_android_mobile.data.promo

/**
 * In-memory guard so the same promo is not shown twice in one app process.
 * Persisted cooldown / max-shows live in [AppPromoCampaignStore].
 */
object AppPromoSessionStore {
    private val consumedDialogKeys = mutableSetOf<String>()

    private fun sessionKey(campaignId: String, version: Int): String = "${campaignId}_v$version"

    fun isDialogConsumed(campaign: AppPromoCampaign): Boolean =
        sessionKey(campaign.id, campaign.version) in consumedDialogKeys

    fun markDialogConsumed(campaign: AppPromoCampaign) {
        consumedDialogKeys.add(sessionKey(campaign.id, campaign.version))
    }
}

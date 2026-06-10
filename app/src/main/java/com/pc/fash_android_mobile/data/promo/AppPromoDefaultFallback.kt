package com.pc.fash_android_mobile.data.promo

import android.content.Context
import com.pc.fash_android_mobile.R
import java.util.Calendar
import java.util.TimeZone

/**
 * Guaranteed app-open promo when remote catalog and carousel slides are unavailable.
 * Rotates by day-of-month (12 themes, aligned with admin seed calendar).
 */
object AppPromoDefaultFallback {

    private const val VERSION = 1
    private const val COOLDOWN_HOURS = 4
    private const val MAX_SHOWS_PER_DAY = 3

    private data class DefaultPromo(
        val idSuffix: String,
        val titleRes: Int,
        val messageRes: Int,
        val primaryRes: Int,
        val badgeRes: Int? = null,
    )

    private val promos = listOf(
        DefaultPromo("m01", R.string.app_promo_default_m01_title, R.string.app_promo_default_m01_body, R.string.app_promo_default_primary),
        DefaultPromo("m02", R.string.app_promo_default_m02_title, R.string.app_promo_default_m02_body, R.string.app_promo_default_primary),
        DefaultPromo("m03", R.string.app_promo_default_m03_title, R.string.app_promo_default_m03_body, R.string.app_promo_default_primary),
        DefaultPromo("m04", R.string.app_promo_default_m04_title, R.string.app_promo_default_m04_body, R.string.app_promo_default_primary),
        DefaultPromo("m05", R.string.app_promo_default_m05_title, R.string.app_promo_default_m05_body, R.string.app_promo_default_primary),
        DefaultPromo("m06", R.string.app_promo_default_m06_title, R.string.app_promo_default_m06_body, R.string.app_promo_default_primary),
        DefaultPromo("m07", R.string.app_promo_default_m07_title, R.string.app_promo_default_m07_body, R.string.app_promo_default_primary),
        DefaultPromo("m08", R.string.app_promo_default_m08_title, R.string.app_promo_default_m08_body, R.string.app_promo_default_primary),
        DefaultPromo("m09", R.string.app_promo_default_m09_title, R.string.app_promo_default_m09_body, R.string.app_promo_default_primary),
        DefaultPromo("m10", R.string.app_promo_default_m10_title, R.string.app_promo_default_m10_body, R.string.app_promo_default_primary),
        DefaultPromo("m11", R.string.app_promo_default_m11_title, R.string.app_promo_default_m11_body, R.string.app_promo_default_primary),
        DefaultPromo("m12", R.string.app_promo_default_m12_title, R.string.app_promo_default_m12_body, R.string.app_promo_default_primary, R.string.app_promo_default_esg_badge),
    )

    fun resolve(context: Context): AppPromoCampaign? {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"))
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val pick = promos[(month - 1) % promos.size]
        val campaign = AppPromoCampaign(
            id = "default_app_open_${pick.idSuffix}_${month}_$day",
            version = VERSION,
            kind = AppPromoCampaignKind.Remote,
            titleRes = pick.titleRes,
            messageRes = pick.messageRes,
            primaryActionRes = pick.primaryRes,
            secondaryActionRes = R.string.app_promo_secondary_later,
            badgeRes = pick.badgeRes,
            primaryAction = AppPromoButtonAction(type = "in_app_explore", payload = ""),
            priority = 40,
            scheduleType = "on_app_open",
            maxShowsPerUser = MAX_SHOWS_PER_DAY,
            cooldownHours = COOLDOWN_HOURS,
        )
        return if (AppPromoCampaignStore.canShow(context, campaign)) campaign else null
    }
}

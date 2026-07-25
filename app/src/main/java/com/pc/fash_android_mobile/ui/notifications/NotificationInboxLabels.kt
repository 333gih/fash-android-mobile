package com.pc.fash_android_mobile.ui.notifications

import androidx.annotation.StringRes
import com.pc.fash_android_mobile.R

/** Maps inbox `payload_type` (`data["type"]`) to a user-facing string resource. */
@StringRes
fun inboxPayloadTypeStringRes(payloadType: String): Int? =
    when (payloadType) {
        "marketplace.follower.new" -> R.string.notification_pt_marketplace_follower_new
        "marketplace.follower.batch" -> R.string.notification_pt_marketplace_follower_batch
        "marketplace.listing.liked" -> R.string.notification_pt_marketplace_listing_liked
        "marketplace.listing.liked.batch" -> R.string.notification_pt_marketplace_listing_liked_batch
        "marketplace.listing.approved_for_followers" -> R.string.notification_pt_marketplace_listing_approved_for_followers
        "marketplace.listing.approved" -> R.string.notification_pt_marketplace_listing_approved
        "marketplace.chat.message" -> R.string.notification_pt_marketplace_chat_message
        "marketplace.chat.offer_received" -> R.string.notification_pt_marketplace_chat_offer_received
        "marketplace.chat.offer_accepted" -> R.string.notification_pt_marketplace_chat_offer_accepted
        "marketplace.chat.offer_declined" -> R.string.notification_pt_marketplace_chat_offer_declined
        "marketplace.order.created" -> R.string.notification_pt_marketplace_order_created
        "marketplace.order.shipped" -> R.string.notification_pt_marketplace_order_shipped
        "marketplace.order.cancelled" -> R.string.notification_pt_marketplace_order_cancelled
        "marketplace.order.meetup_aborted" -> R.string.notification_pt_marketplace_order_meetup_aborted
        "marketplace.order.funds_released" -> R.string.notification_pt_marketplace_order_funds_released
        "marketplace.order.dispute_opened" -> R.string.notification_pt_marketplace_order_dispute_opened
        "marketplace.review.received" -> R.string.notification_pt_marketplace_review_received
        "marketplace.referral.invite_rewarded" -> R.string.notification_pt_marketplace_referral_invite_rewarded
        "marketplace.recommendation.daily_digest" -> R.string.notification_pt_marketplace_recommendation_daily_digest
        "marketplace.recommendation.style_fresh" -> R.string.notification_pt_marketplace_recommendation_style_fresh
        "marketplace.recommendation.similar_saved" -> R.string.notification_pt_marketplace_recommendation_similar_saved
        "marketplace.recommendation.continue_browsing" -> R.string.notification_pt_marketplace_recommendation_continue_browsing
        "marketplace.recommendation.inactive_nudge" -> R.string.notification_pt_marketplace_recommendation_inactive_nudge
        "marketplace.recommendation.inactive_ladder" -> R.string.notification_pt_marketplace_recommendation_inactive_ladder
        "marketplace.recommendation.daily_comeback" -> R.string.notification_pt_marketplace_recommendation_daily_comeback
        "marketplace.recommendation.ai_re_engagement" -> R.string.notification_pt_marketplace_recommendation_ai_re_engagement
        "marketplace.recommendation.sustainable_impact" -> R.string.notification_pt_marketplace_recommendation_sustainable_impact
        "marketplace.recommendation.community_quiet" -> R.string.notification_pt_marketplace_recommendation_community_quiet
        "marketplace.recommendation.style_drought" -> R.string.notification_pt_marketplace_recommendation_style_drought
        "marketplace.recommendation.taste_neighbor" -> R.string.notification_pt_marketplace_recommendation_taste_neighbor
        "marketplace.recommendation.hunt_today" -> R.string.notification_pt_marketplace_recommendation_hunt_today
        "marketplace.recommendation.social_style_match" -> R.string.notification_pt_marketplace_recommendation_social_style_match
        "admin.mobile_push" -> R.string.notification_pt_admin_mobile_push
        "admin.mobile_push.announcement" -> R.string.notification_pt_admin_mobile_push_announcement
        "admin.mobile_push.promo" -> R.string.notification_pt_admin_mobile_push_promo
        "admin.mobile_push.transactional" -> R.string.notification_pt_admin_mobile_push_transactional
        "admin.mobile_push.ops" -> R.string.notification_pt_admin_mobile_push_ops
        "admin.app_promo_interstitial" -> R.string.notification_pt_admin_app_promo_interstitial
        else -> null
    }

/** Maps inbox ledger `source` to a user-facing string resource. */
@StringRes
fun inboxSourceStringRes(source: String): Int? =
    when (source) {
        "kafka" -> R.string.notification_src_kafka
        "core_local" -> R.string.notification_src_core_local
        else -> null
    }

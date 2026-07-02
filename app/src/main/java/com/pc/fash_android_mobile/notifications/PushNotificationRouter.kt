package com.pc.fash_android_mobile.notifications

import android.content.Intent
import android.net.Uri
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.deeplink.InboxDeepLinks
import com.pc.fash_android_mobile.deeplink.InviteDeepLinks
import com.pc.fash_android_mobile.deeplink.ListingDeepLinks
import com.pc.fash_android_mobile.deeplink.ProfileDeepLinks
import com.pc.fash_android_mobile.ui.chat.ChatInAppNotificationPolicy

/**
 * Routes system-tray notification taps (MainActivity intent) — mirrors iOS
 * [FashFirebaseMessagingService.routeFromPushData] with Android in-app chat-first parity.
 */
object PushNotificationRouter {

    private const val FCM_EXTRA_PREFIX = "fcm."

    /** Copies FCM data map onto the tray [Intent] so tap routing has conversation/order context. */
    fun attachFcmDataToIntent(intent: Intent, data: Map<String, String>) {
        for ((key, value) in data) {
            if (key.isBlank() || value.isBlank()) continue
            intent.putExtra("$FCM_EXTRA_PREFIX$key", value)
        }
    }

    fun fcmDataFromIntent(intent: Intent?): Map<String, String> {
        intent ?: return emptyMap()
        val extras = intent.extras ?: return emptyMap()
        val out = linkedMapOf<String, String>()
        for (key in extras.keySet()) {
            if (!key.startsWith(FCM_EXTRA_PREFIX)) continue
            val value = extras.getString(key)?.trim().orEmpty()
            if (value.isEmpty()) continue
            out[key.removePrefix(FCM_EXTRA_PREFIX)] = value
        }
        mergeLegacyTrayExtras(intent, out)
        return out
    }

    /**
     * Applies navigation pending state from a notification tray tap or deep-link VIEW intent.
     * Call after account-switch parsing; engagement is reported separately.
     */
    fun routeFromTrayTap(fashApp: FashApplication, intent: Intent?) {
        val data = fcmDataFromIntent(intent)
        if (data.isEmpty() && intent == null) return

        if (data["inbox_refresh"] == "1") {
            fashApp.requestInboxUnreadRefreshDebounced()
        }

        InAppNotificationNavigation.chatConversationId(data)?.let { conversationId ->
            fashApp.pendingOpenChatConversationId.value = conversationId.trim()
            return
        }

        val deepLink = data["deep_link"]?.trim()?.takeIf { it.isNotEmpty() }
            ?: intent?.getStringExtra("deep_link")?.trim()?.takeIf { it.isNotEmpty() }
        if (!deepLink.isNullOrEmpty()) {
            if (routeDeepLink(fashApp, deepLink)) return
        }

        val nav = normalized(data["nav_target"] ?: data["navTarget"])
        if (nav == "in_app_invite_friends") {
            fashApp.pendingOpenInviteFriends.value = true
            return
        }
        if (nav == "order") {
            InAppNotificationNavigation.orderId(data)?.let { orderId ->
                fashApp.pendingOpenOrderId.value = orderId
                return
            }
        }

        val inboxId = data["user_notification_id"]?.trim()?.takeIf { it.isNotEmpty() }
            ?: InboxDeepLinks.parseNotificationIdFromIntent(intent)
        if (!inboxId.isNullOrEmpty()) {
            fashApp.pendingInboxNotificationId.value = inboxId
            fashApp.requestOpenNotificationInbox()
        }
    }

    private fun routeDeepLink(fashApp: FashApplication, deepLink: String): Boolean {
        InboxDeepLinks.parseNotificationIdFromDeepLinkString(deepLink)?.let { nid ->
            fashApp.pendingInboxNotificationId.value = nid
            fashApp.requestOpenNotificationInbox()
            return true
        }
        runCatching {
            val uri = Uri.parse(deepLink)
            ListingDeepLinks.parseListingId(uri)?.let { listingId ->
                fashApp.pendingDeepLinkListingId.value = listingId
                return true
            }
            ProfileDeepLinks.parseUsername(uri)?.let { username ->
                fashApp.pendingDeepLinkSellerUsername.value = username
                return true
            }
            if (uri.host.equals("invite", ignoreCase = true)) {
                InviteDeepLinks.parseReferralTokenFromIntent(
                    Intent(Intent.ACTION_VIEW, uri),
                )?.let { token ->
                    fashApp.pendingReferralToken.value = token
                }
                InviteDeepLinks.parseReferrerFromIntent(
                    Intent(Intent.ACTION_VIEW, uri),
                )?.let { ref ->
                    fashApp.pendingReferrerUsername.value = ref
                }
                fashApp.pendingOpenInviteFriends.value = true
                return true
            }
        }
        return false
    }

    private fun mergeLegacyTrayExtras(intent: Intent, out: MutableMap<String, String>) {
        intent.getStringExtra("user_notification_id")?.trim()?.takeIf { it.isNotEmpty() }?.let {
            out.putIfAbsent("user_notification_id", it)
        }
        intent.getStringExtra("deep_link")?.trim()?.takeIf { it.isNotEmpty() }?.let {
            out.putIfAbsent("deep_link", it)
        }
        for (key in listOf("conversation_id", "conversationId", "order_id", "marketplace_order_id", "nav_target", "type", "event")) {
            intent.getStringExtra(key)?.trim()?.takeIf { it.isNotEmpty() }?.let { value ->
                out.putIfAbsent(key, value)
            }
        }
        if (out.containsKey("conversation_id") || out.containsKey("conversationId")) return
        ChatInAppNotificationPolicy.conversationId(out)?.let { cid ->
            out["conversation_id"] = cid
        }
    }

    private fun normalized(value: String?): String = value?.trim()?.lowercase().orEmpty()
}

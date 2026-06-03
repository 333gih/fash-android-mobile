package com.pc.fash_android_mobile.data.promo

import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.FashInAppNotificationSession
import com.pc.fash_android_mobile.ui.chat.InboxNotificationSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Admin app-promo while foreground: dialog + inbox read, or in-app only in chat (no dialog on exit).
 */
object AppPromoPresentationPolicy {

    fun isInChatDetail(openConversationId: String?, activeChatConversationId: String?): Boolean {
        val open = openConversationId?.trim().orEmpty()
        if (open.isNotEmpty()) return true
        val active = activeChatConversationId?.trim().orEmpty()
        return active.isNotEmpty()
    }

    fun shouldSuppressInAppToast(context: android.content.Context, campaign: AppPromoCampaign): Boolean {
        AppPromoCampaignStore.isDismissed(context, campaign) ||
            AppPromoCampaignStore.hasRecordedShow(context, campaign)
    }

    fun promoInAppData(
        campaign: AppPromoCampaign,
        userNotificationId: String?,
    ): Map<String, String> = buildMap {
        put("type", ADMIN_APP_PROMO_PAYLOAD_TYPE)
        put("campaign_id", campaign.id)
        put("campaign_version", campaign.version.toString())
        userNotificationId?.trim()?.takeIf { it.isNotEmpty() }?.let { put("user_notification_id", it) }
    }

    fun handleIncoming(
        app: FashApplication,
        campaign: AppPromoCampaign,
        openConversationId: String?,
        userNotificationId: String?,
        presentDialog: (AppPromoCampaign) -> Unit,
    ) {
        AppPromoPendingQueue.enqueue(campaign)
        app.requestInboxUnreadRefreshDebounced()

        if (isInChatDetail(openConversationId, app.activeChatConversationId)) {
            if (shouldSuppressInAppToast(app, campaign)) return
            val title = campaign.remoteTitle?.trim().orEmpty()
            val body = campaign.remoteMessage?.trim().orEmpty()
            if (title.isEmpty() && body.isEmpty()) return
            app.showInAppNotificationFromRealtime(
                title = title.ifEmpty { body },
                body = body,
                data = promoInAppData(campaign, userNotificationId),
                userNotificationId = userNotificationId,
            )
            return
        }

        presentDialog(campaign)
    }

    fun markInboxReadAfterDialogShown(
        scope: CoroutineScope,
        app: FashApplication,
        campaign: AppPromoCampaign,
    ) {
        scope.launch(Dispatchers.IO) {
            InboxNotificationSync.markAppPromoNotificationsRead(
                campaignId = campaign.id,
                version = campaign.version,
                userRepository = app.userRepository,
            )
            app.requestInboxUnreadRefreshDebounced()
        }
    }
}

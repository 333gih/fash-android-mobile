package com.pc.fash_android_mobile.notifications

import android.content.Context
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.data.promo.AppPromoPresentationPolicy
import com.pc.fash_android_mobile.data.promo.isAppPromoPushData
import com.pc.fash_android_mobile.data.promo.parseAppPromoFromPushData
import com.pc.fash_android_mobile.data.realtime.RealtimeEvent
import com.pc.fash_android_mobile.ui.chat.ChatNotificationPresence
import com.pc.fash_android_mobile.ui.chat.ChatInAppNotificationPolicy
import com.pc.fash_android_mobile.ui.chat.ChatViewModel

/** Routes global realtime notification signals — parity with iOS [RealtimeNotificationRouter]. */
object RealtimeNotificationRouter {

    fun handleNotificationShow(
        event: RealtimeEvent.NotificationShow,
        app: FashApplication,
        context: Context,
        openConversationId: String?,
        chatViewModel: ChatViewModel?,
        presentAdminPromo: (com.pc.fash_android_mobile.data.promo.AppPromoCampaign) -> Unit,
    ) {
        val pushData = event.data ?: emptyMap()
        if (ChatInAppNotificationPolicy.shouldSuppressInApp(pushData, openConversationId)) {
            ChatNotificationPresence.handleSuppressedChatNotification(app, pushData)
            return
        }
        if (isAppPromoPushData(pushData)) {
            parseAppPromoFromPushData(
                data = pushData,
                fallbackTitle = event.title,
                fallbackBody = event.body,
            )?.let { promo ->
                AppPromoPresentationPolicy.handleIncoming(
                    app = app,
                    campaign = promo,
                    openConversationId = openConversationId,
                    userNotificationId = event.userNotificationId,
                    presentDialog = presentAdminPromo,
                )
            }
            return
        }
        app.showInAppNotificationFromRealtime(
            title = event.title,
            body = event.body,
            data = pushData,
            userNotificationId = event.userNotificationId,
            openConversationId = openConversationId,
            chatViewModel = chatViewModel,
        )
        app.requestInboxUnreadRefreshDebounced()
    }

    fun handleMessageNew(
        event: RealtimeEvent.MessageNew,
        app: FashApplication,
        context: Context,
        openConversationId: String?,
        chatViewModel: ChatViewModel,
        isGuestMode: Boolean,
    ) {
        chatViewModel.refreshInboxFromRealtimeSignal()
        if (isGuestMode) return

        val myId = app.authManager.sessionStore.read()?.userId?.trim().orEmpty()
        val cid = event.conversationId.trim()
        if (ChatInAppNotificationPolicy.isOpenConversation(cid, openConversationId)) {
            ChatNotificationPresence.handleSuppressedChatNotification(
                app,
                mapOf("conversation_id" to cid),
            )
            return
        }
        if (!ChatInAppNotificationPolicy.shouldShowMessageNewInApp(
                conversationId = event.conversationId,
                senderId = event.senderId,
                recipientId = event.recipientId,
                messageType = event.messageType,
                systemSubtype = event.systemSubtype,
                myUserId = myId,
                openConversationId = openConversationId,
            )
        ) {
            return
        }

        val preview = event.preview.trim()
        val data = if (cid.isEmpty()) emptyMap() else mapOf("conversation_id" to cid)
        val title = InAppNotificationPresentation.chatMessageNewTitle(
            context = context,
            conversationId = cid,
            senderId = event.senderId,
            chatViewModel = chatViewModel,
        )
        val body = preview.ifEmpty {
            context.getString(com.pc.fash_android_mobile.R.string.notification_pt_marketplace_chat_message)
        }
        app.showInAppNotificationFromRealtime(
            title = title,
            body = body,
            data = data,
            userNotificationId = null,
            openConversationId = openConversationId,
            chatViewModel = chatViewModel,
        )
    }
}

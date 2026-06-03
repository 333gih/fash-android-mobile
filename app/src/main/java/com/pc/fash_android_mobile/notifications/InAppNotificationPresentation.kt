package com.pc.fash_android_mobile.notifications

import android.content.Context
import com.pc.fash_android_mobile.FashInAppNotificationSession
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.chat.ChatInAppNotificationPolicy
import com.pc.fash_android_mobile.ui.chat.ChatViewModel

/** Builds in-app banner copy — prefer peer username/display name over generic "User". */
object InAppNotificationPresentation {

    private val genericActorTitles = setOf(
        "user", "buyer", "người dùng", "người mua",
    )

    fun enrich(
        context: Context,
        session: FashInAppNotificationSession,
        chatViewModel: ChatViewModel?,
    ): FashInAppNotificationSession {
        var title = session.title.trim()
        val body = session.body.trim()
        val data = session.data

        if (isGenericActorTitle(title)) {
            resolveActorTitle(context, data, chatViewModel)?.takeIf { it.isNotEmpty() }?.let { title = it }
        } else if (title.isEmpty()) {
            resolveActorTitle(context, data, chatViewModel)?.takeIf { it.isNotEmpty() }?.let { title = it }
        }

        return FashInAppNotificationSession(
            title = title,
            body = body,
            data = data,
            userNotificationId = session.userNotificationId,
        )
    }

    fun chatMessageNewTitle(
        context: Context,
        conversationId: String,
        senderId: String,
        chatViewModel: ChatViewModel?,
    ): String {
        chatViewModel?.peerLabelForConversationId(conversationId)?.takeIf { it.isNotEmpty() }?.let { return it }
        chatViewModel?.peerLabelForOtherUserId(senderId)?.takeIf { it.isNotEmpty() }?.let { return it }
        return context.getString(R.string.notification_pt_marketplace_chat_message)
    }

    fun resolveActorTitle(
        context: Context,
        data: Map<String, String>?,
        chatViewModel: ChatViewModel?,
    ): String? {
        if (data.isNullOrEmpty()) return null
        for (key in listOf(
            "sender_display_name", "sender_username", "sender_name",
            "buyer_display_name", "buyer_username",
            "seller_display_name", "seller_username",
            "actor_display_name", "display_name", "username",
        )) {
            val value = data[key]?.trim().orEmpty()
            if (value.isNotEmpty() && !isGenericActorTitle(value)) {
                return formatHandle(value)
            }
        }
        ChatInAppNotificationPolicy.conversationId(data)?.let { cid ->
            chatViewModel?.peerLabelForConversationId(cid)?.takeIf { it.isNotEmpty() }?.let { return it }
        }
        return null
    }

    private fun formatHandle(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return trimmed
        if (trimmed.startsWith("@")) return trimmed
        if (trimmed.contains(' ')) return trimmed
        return "@$trimmed"
    }

    fun isGenericActorTitle(title: String): Boolean =
        genericActorTitles.contains(title.trim().lowercase())
}

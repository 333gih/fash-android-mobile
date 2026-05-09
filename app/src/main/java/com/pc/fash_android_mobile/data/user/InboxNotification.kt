package com.pc.fash_android_mobile.data.user

/** Page from `GET …/users/me/notifications`. */
data class InboxNotificationsPage(
    val items: List<InboxNotificationItem>,
    val serviceRef: String?,
)

/** One inbox row (core proxy → notification ledger). */
data class InboxNotificationItem(
    val id: String,
    val title: String,
    val body: String,
    val dataMap: Map<String, Any?>?,
    val payloadType: String?,
    val source: String?,
    val sourceEventId: String?,
    val readAtIso: String?,
    val createdAtIso: String,
) {
    val isUnread: Boolean get() = readAtIso.isNullOrBlank()
}

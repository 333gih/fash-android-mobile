package com.pc.fash_android_mobile.data.promo

/**
 * In-memory queue when multiple promos arrive at once (realtime + FCM + pull).
 * Higher [AppPromoCampaign.priority] wins; ties broken by enqueue order (FIFO).
 */
object AppPromoPendingQueue {
    private val lock = Any()
    private val pending = LinkedHashMap<String, AppPromoCampaign>()

    fun enqueue(campaign: AppPromoCampaign) {
        synchronized(lock) {
            val existing = pending[campaign.id]
            if (existing == null || campaign.priority >= existing.priority) {
                pending[campaign.id] = campaign
            }
        }
    }

    fun peekHighest(): AppPromoCampaign? = synchronized(lock) {
        pending.values.maxWithOrNull(
            compareBy<AppPromoCampaign> { it.priority }
                .thenBy { it.id },
        )
    }

    fun pollHighest(): AppPromoCampaign? = synchronized(lock) {
        val best = peekHighest() ?: return null
        pending.remove(best.id)
        best
    }

    fun remove(id: String) {
        synchronized(lock) { pending.remove(id) }
    }

    fun clear() {
        synchronized(lock) { pending.clear() }
    }

    fun size(): Int = synchronized(lock) { pending.size }
}

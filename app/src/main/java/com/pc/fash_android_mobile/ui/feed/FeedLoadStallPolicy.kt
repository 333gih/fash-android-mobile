package com.pc.fash_android_mobile.ui.feed

/** Shared stall window before showing "try again later" on feed grids (iOS parity). */
object FeedLoadStallPolicy {
    /** Must exceed typical cold-start explore-listings latency so the grid does not flash the hard-error CTA. */
    const val TIMEOUT_MS = 12_000L
}

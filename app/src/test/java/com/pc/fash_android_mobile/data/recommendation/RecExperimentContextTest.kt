package com.pc.fash_android_mobile.data.recommendation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecExperimentContextTest {
    @Test
    fun isRecommendationSurface_includesForYou() {
        assertTrue(RecExperimentContext.isRecommendationSurface("for_you"))
        assertTrue(RecExperimentContext.isRecommendationSurface("explore"))
    }

    @Test
    fun isRecommendationSurface_excludesNotificationOpen() {
        assertFalse(RecExperimentContext.isRecommendationSurface(FeedSurfaces.NOTIFICATION_OPEN))
        assertFalse(RecExperimentContext.isRecommendationSurface(FeedSurfaces.APP_OPEN))
    }

    @Test
    fun experimentIdFromHeader() {
        RecExperimentContext.clear()
        RecExperimentContext.update("exp_preloved:variant")
        assertEquals("exp_preloved:variant", RecExperimentContext.experimentIdForFeedEvents())
    }
}

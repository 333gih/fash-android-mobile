package com.pc.fash_android_mobile.data.appstatus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppMaintenanceStatusTest {

    @Test
    fun warningIsNotLocked() {
        val status = AppMaintenanceStatus.Open.copy(phase = "warning", countdownSeconds = 60)
        assertTrue(status.isWarning)
        assertFalse(status.isLocked)
        assertTrue(status.sawRestricted)
        assertTrue(status.isEffectivelyWarning())
        assertFalse(status.isEffectivelyLocked())
    }

    @Test
    fun inferResume_backOnlineWhenLeavingLock() {
        val prev = AppMaintenanceStatus.Open.copy(maintenance = true, phase = "maintenance")
        val next = AppMaintenanceStatus.Open
        assertEquals("back_online", next.inferredResumeMoment(prev))
    }

    @Test
    fun inferResume_warningClearedWhenLeavingWarning() {
        val prev = AppMaintenanceStatus.Open.copy(phase = "warning")
        val next = AppMaintenanceStatus.Open
        assertEquals("warning_cleared", next.inferredResumeMoment(prev))
    }

    @Test
    fun inferResume_prefersServerMoment() {
        val prev = AppMaintenanceStatus.Open.copy(maintenance = true, phase = "maintenance")
        val next = AppMaintenanceStatus.Open.copy(resumeMoment = "back_online")
        assertEquals("back_online", next.inferredResumeMoment(prev))
    }

    @Test
    fun inferResume_nullWhenStillRestricted() {
        val prev = AppMaintenanceStatus.Open.copy(phase = "warning")
        val next = AppMaintenanceStatus.Open.copy(maintenance = true, phase = "maintenance")
        assertNull(next.inferredResumeMoment(prev))
    }

    @Test
    fun resumeToken_fallsBackWhenUpdatedAtMissing() {
        val prev = AppMaintenanceStatus.Open.copy(maintenance = true, phase = "maintenance")
        val next = AppMaintenanceStatus.Open
        assertEquals("local:back_online:maintenance", next.resumeDedupeToken(prev))
    }
}

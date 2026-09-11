package com.mani.controlcentre.companion

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionPolicyTest {
    @Test fun quickPanelNeedsASamsungQuickPanelId() {
        assertTrue(CompanionPolicy.isQuickPanel(listOf("legacy_window_root", "sec_quick_panel_compose_root")))
        // Observed on the S25 Ultra: these two ids are present while the notifications panel is open.
        assertFalse(CompanionPolicy.isQuickPanel(listOf("qs_frame", "quick_settings_container", "legacy_window_root")))
        assertFalse(CompanionPolicy.isQuickPanel(listOf("legacy_window_root", "notification_stack_scroller")))
        assertFalse(CompanionPolicy.isQuickPanel(emptyList()))
    }

    @Test fun handleOnlyWhileUnlockedInteractiveAndOpen() {
        assertTrue(CompanionPolicy.shouldShowHandle(enabled = true, interactive = true, keyguardLocked = false, quickPanelOpen = true))
        assertFalse(CompanionPolicy.shouldShowHandle(enabled = false, interactive = true, keyguardLocked = false, quickPanelOpen = true))
        assertFalse(CompanionPolicy.shouldShowHandle(enabled = true, interactive = false, keyguardLocked = false, quickPanelOpen = true))
        assertFalse(CompanionPolicy.shouldShowHandle(enabled = true, interactive = true, keyguardLocked = true, quickPanelOpen = true))
        assertFalse(CompanionPolicy.shouldShowHandle(enabled = true, interactive = true, keyguardLocked = false, quickPanelOpen = false))
    }

    @Test fun halfwayDragSettlesOpen() {
        assertTrue(CompanionPolicy.settlesOpen(translation = 400f, width = 1440f, velocityX = 0f))
        assertFalse(CompanionPolicy.settlesOpen(translation = 1000f, width = 1440f, velocityX = 0f))
    }

    @Test fun flingWinsOverPosition() {
        assertTrue(CompanionPolicy.settlesOpen(translation = 1300f, width = 1440f, velocityX = -2000f))
        assertFalse(CompanionPolicy.settlesOpen(translation = 100f, width = 1440f, velocityX = 2000f))
    }

    @Test fun degenerateInputNeverOpens() {
        assertFalse(CompanionPolicy.settlesOpen(translation = 0f, width = 0f, velocityX = 0f))
        assertFalse(CompanionPolicy.settlesOpen(translation = Float.NaN, width = 1440f, velocityX = 0f))
        assertFalse(CompanionPolicy.settlesOpen(translation = 0f, width = 1440f, velocityX = Float.NaN))
    }
}

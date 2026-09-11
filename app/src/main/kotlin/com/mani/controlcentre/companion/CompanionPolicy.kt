package com.mani.controlcentre.companion

/** Pure decisions for the companion-page experiment; unit-tested, no Android dependencies. */
object CompanionPolicy {
    /**
     * View ids (without the package prefix) that only exist while Samsung's Quick Panel content is showing.
     * Device finding: `qs_frame` and `quick_settings_container` are also present while the notifications
     * panel is open, so only the Samsung compose root is exclusive.
     */
    val QUICK_PANEL_IDS = setOf("sec_quick_panel_compose_root")

    /** Ids that are also looked up for diagnostics only; they do not decide anything. */
    val DIAGNOSTIC_IDS = setOf("quick_settings_container", "qs_frame", "notification_stack_scroller")

    /** Ids that identify the shade window's content root on this firmware. */
    val SHADE_ROOT_IDS = setOf("legacy_window_root", "notification_panel")

    const val SHADE_WINDOW_TITLE = "NotificationShade"

    fun isQuickPanel(foundIds: Collection<String>): Boolean = foundIds.any { it in QUICK_PANEL_IDS }

    fun shouldShowHandle(enabled: Boolean, interactive: Boolean, keyguardLocked: Boolean, quickPanelOpen: Boolean): Boolean =
        enabled && interactive && !keyguardLocked && quickPanelOpen

    /**
     * Whether a drag settles with the page shown. [translation] is the page offset in px (0 = fully shown,
     * [width] = fully hidden); [velocityX] is px/s, negative when moving towards "shown".
     */
    fun settlesOpen(translation: Float, width: Float, velocityX: Float, flingPxPerSecond: Float = 1500f): Boolean = when {
        width <= 0f || !translation.isFinite() || !velocityX.isFinite() -> false
        velocityX <= -flingPxPerSecond -> true
        velocityX >= flingPxPerSecond -> false
        else -> translation < width * 0.5f
    }
}

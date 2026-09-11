package com.mani.controlcentre.access

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import com.mani.controlcentre.core.Page
import com.mani.controlcentre.core.Route
import com.mani.controlcentre.data.Prefs
import com.mani.controlcentre.data.TriggerSettings
import com.mani.controlcentre.diagnostics.GestureLog
import com.mani.controlcentre.gesture.TriggerWindowController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Owns the trigger windows and launches the panel with the page chosen during the gesture.
 * It reads no screen content; the single event type in its configuration is ignored.
 */
class PanelAccessibilityService : AccessibilityService(), TriggerWindowController.Listener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var windows: TriggerWindowController? = null
    private var settings: TriggerSettings? = null
    private var receiverRegistered = false

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            GestureLog.log("Service: ${intent.action?.substringAfterLast('.')}")
            refresh()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        GestureLog.log("Service connected")
        windows = TriggerWindowController(this, this)
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenReceiver, filter, RECEIVER_NOT_EXPORTED)
        receiverRegistered = true
        scope.launch {
            Prefs(this@PanelAccessibilityService).triggers.collect { latest ->
                settings = latest
                refresh()
            }
        }
    }

    /** Rebuilds the trigger windows for the current settings and lock state, never during a gesture. */
    private fun refresh() {
        val controller = windows ?: return
        val latest = settings ?: return
        if (controller.gestureActive) return
        if (isInputAllowed()) controller.apply(latest.configs()) else controller.removeAll()
    }

    override fun isInputAllowed(): Boolean {
        val power = getSystemService(PowerManager::class.java)
        val keyguard = getSystemService(KeyguardManager::class.java)
        return power.isInteractive && !keyguard.isKeyguardLocked && settings?.paused != true
    }

    override fun onPageSelected(page: Page, route: Route) {
        GestureLog.log("Launch PanelActivity page=${page.id} route=${route.name}")
        runCatching { startActivity(PageIntents.panel(this, page)) }
            .onFailure { GestureLog.log("startActivity failed: $it") }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        refresh()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        tearDown("unbound")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        tearDown("destroyed")
        super.onDestroy()
    }

    private fun tearDown(reason: String) {
        scope.cancel()
        if (receiverRegistered) {
            runCatching { unregisterReceiver(screenReceiver) }
            receiverRegistered = false
        }
        windows?.removeAll()
        windows = null
        GestureLog.log("Service $reason")
    }

    companion object {
        /** True when the user has enabled this service in system Accessibility settings. */
        fun isEnabled(context: Context): Boolean {
            val manager = context.getSystemService(AccessibilityManager::class.java)
            return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any { info ->
                val service = info.resolveInfo.serviceInfo
                service.packageName == context.packageName && service.name == PanelAccessibilityService::class.java.name
            }
        }
    }
}

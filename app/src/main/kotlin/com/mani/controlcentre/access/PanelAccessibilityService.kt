package com.mani.controlcentre.access

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import com.mani.controlcentre.companion.CompanionController
import com.mani.controlcentre.companion.CompanionPolicy
import com.mani.controlcentre.companion.QuickPanelDetector
import com.mani.controlcentre.companion.ShadeObservation
import com.mani.controlcentre.data.Prefs
import com.mani.controlcentre.diagnostics.GestureLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Companion-page experiment. Creates no windows until Samsung's Quick Panel is observed open; then shows one
 * small edge handle. Never intercepts the opening gesture. The superseded top-right/side triggers are gone.
 */
class PanelAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var companion: CompanionController? = null
    private var detector: QuickPanelDetector? = null
    private var enabled = true
    private var lastObservation: ShadeObservation? = null
    private var receiverRegistered = false

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            GestureLog.log("Service: ${intent.action?.substringAfterLast('.')}")
            if (intent.action == Intent.ACTION_SCREEN_OFF) companion?.removeAll("screen off") else evaluate("screen state")
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        GestureLog.log("Service connected (companion experiment)")
        companion = CompanionController(this)
        detector = QuickPanelDetector(this)
        registerReceiver(
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            },
            RECEIVER_NOT_EXPORTED,
        )
        receiverRegistered = true
        scope.launch {
            Prefs(this@PanelAccessibilityService).companionEnabled.collect { value ->
                enabled = value
                GestureLog.log("Companion experiment enabled=$value")
                evaluate("preference")
            }
        }
        evaluate("connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED && event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val summary = "${AccessibilityEvent.eventTypeToString(event.eventType).removePrefix("TYPE_")} pkg=${event.packageName} cls=${event.className?.toString()?.substringAfterLast('.')} changes=0x${Integer.toHexString(event.windowChanges)}"
        evaluate(summary)
    }

    /** Re-reads the shade state and reconciles the windows. Never tears windows down mid-gesture. */
    private fun evaluate(reason: String) {
        val detector = detector ?: return
        val companion = companion ?: return
        val observation = detector.observe()
        if (observation != lastObservation) {
            GestureLog.log("Shade: visible=${observation.shadeVisible} quickPanel=${observation.quickPanel} ids=${observation.foundIds} title=${observation.shadeTitle} <- $reason")
            if (observation.shadeVisible && !observation.quickPanel && lastObservation?.shadeVisible != true) {
                GestureLog.log("Shade ids (diagnostic): ${detector.describeShade()}")
            }
            lastObservation = observation
        }
        val power = getSystemService(PowerManager::class.java)
        val keyguard = getSystemService(KeyguardManager::class.java)
        val show = CompanionPolicy.shouldShowHandle(enabled, power.isInteractive, keyguard.isKeyguardLocked, observation.quickPanel)
        if (show) {
            companion.showHandle()
        } else if (!companion.gestureActive) {
            companion.removeAll("quick panel not open (${if (keyguard.isKeyguardLocked) "locked" else reason})")
        }
    }

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
        companion?.removeAll("service $reason")
        companion = null
        detector = null
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

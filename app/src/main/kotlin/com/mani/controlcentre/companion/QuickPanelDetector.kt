package com.mani.controlcentre.companion

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.mani.controlcentre.diagnostics.GestureLog

/** What the service can currently see of Samsung's shade. */
data class ShadeObservation(
    val shadeVisible: Boolean,
    val quickPanel: Boolean,
    val foundIds: List<String>,
    val shadeTitle: String?,
)

/**
 * Recognises the open Quick Panel from the System UI shade window and a few of its view ids.
 * Read-only: inspects windows owned by com.android.systemui only, never reads text.
 */
class QuickPanelDetector(private val service: AccessibilityService) {
    private val screenHeight = service.resources.displayMetrics.heightPixels

    fun observe(): ShadeObservation {
        val windows = runCatching { service.windows }.getOrElse { emptyList() }
        val shade = windows.firstOrNull { it.type == AccessibilityWindowInfo.TYPE_SYSTEM && isShade(it) }
            ?: return ShadeObservation(shadeVisible = false, quickPanel = false, foundIds = emptyList(), shadeTitle = null)
        val root = shade.root ?: return ShadeObservation(true, false, emptyList(), shade.title?.toString())
        val found = (CompanionPolicy.QUICK_PANEL_IDS + CompanionPolicy.DIAGNOSTIC_IDS + CompanionPolicy.SHADE_ROOT_IDS).filter { id ->
            root.findAccessibilityNodeInfosByViewId("$SYSTEM_UI:id/$id").isNotEmpty()
        }
        return ShadeObservation(true, CompanionPolicy.isQuickPanel(found), found, shade.title?.toString())
    }

    /** Diagnostic only: distinct view ids near the top of the shade tree, never text. */
    fun describeShade(limit: Int = 40): List<String> {
        val windows = runCatching { service.windows }.getOrElse { return emptyList() }
        val root = windows.firstOrNull { it.type == AccessibilityWindowInfo.TYPE_SYSTEM && isShade(it) }?.root ?: return emptyList()
        val ids = LinkedHashSet<String>()
        val queue = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>().apply { add(root to 0) }
        var visited = 0
        while (queue.isNotEmpty() && ids.size < limit && visited < 400) {
            val (node, depth) = queue.removeFirst()
            visited++
            node.viewIdResourceName?.removePrefix("$SYSTEM_UI:id/")?.let { ids.add(it) }
            if (depth < 6) for (i in 0 until node.childCount) node.getChild(i)?.let { queue.add(it to depth + 1) }
        }
        return ids.toList()
    }

    private fun isShade(window: AccessibilityWindowInfo): Boolean {
        if (window.title?.toString() == CompanionPolicy.SHADE_WINDOW_TITLE) return true
        val bounds = Rect().also { window.getBoundsInScreen(it) }
        val root = window.root ?: return false
        val rootId = root.viewIdResourceName?.removePrefix("$SYSTEM_UI:id/")
        return root.packageName == SYSTEM_UI && bounds.height() > screenHeight / 2 && rootId in CompanionPolicy.SHADE_ROOT_IDS
    }

    fun windowSummary(): String = runCatching { service.windows }.getOrElse { emptyList() }.joinToString(" | ") { w ->
        val b = Rect().also { w.getBoundsInScreen(it) }
        "${typeName(w.type)}:${w.title ?: "-"}:${w.root?.packageName ?: "?"}:${b.width()}x${b.height()}"
    }

    private fun typeName(type: Int) = when (type) {
        AccessibilityWindowInfo.TYPE_APPLICATION -> "app"
        AccessibilityWindowInfo.TYPE_SYSTEM -> "sys"
        AccessibilityWindowInfo.TYPE_INPUT_METHOD -> "ime"
        AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY -> "a11y"
        AccessibilityWindowInfo.TYPE_SPLIT_SCREEN_DIVIDER -> "divider"
        else -> "t$type"
    }

    private companion object {
        const val SYSTEM_UI = "com.android.systemui"
    }

    init {
        GestureLog.log("Detector ready; screenHeight=$screenHeight")
    }
}

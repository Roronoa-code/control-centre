package com.mani.controlcentre.gesture

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import com.mani.controlcentre.core.GestureRouter
import com.mani.controlcentre.core.Page
import com.mani.controlcentre.core.Route
import com.mani.controlcentre.core.TriggerConfig
import com.mani.controlcentre.diagnostics.GestureLog
import kotlin.math.roundToInt

/** Adds and removes the small trigger windows. Must be driven from the connected accessibility service's own context. */
class TriggerWindowController(private val context: Context, private val listener: Listener) {

    interface Listener {
        /** False while locked, screen off or paused: the touch is logged and dropped. */
        fun isInputAllowed(): Boolean
        fun onPageSelected(page: Page, route: Route)
    }

    private val windowManager: WindowManager = context.getSystemService(WindowManager::class.java)
    private val views = mutableMapOf<Route, TriggerView>()

    val gestureActive: Boolean get() = views.values.any { it.tracking }

    fun apply(configs: List<TriggerConfig>) {
        removeAll()
        configs.filter { it.enabled && it.route != Route.EXTERNAL }.forEach { add(it) }
        GestureLog.log("Trigger windows: ${views.keys.joinToString().ifEmpty { "none" }}")
    }

    fun removeAll() {
        views.values.forEach { view -> runCatching { windowManager.removeViewImmediate(view) } }
        views.clear()
    }

    private fun add(config: TriggerConfig) {
        val metrics = context.resources.displayMetrics
        val density = metrics.density
        val view = TriggerView(context, config, density, listener)
        val horizontal = if (config.left) Gravity.START else Gravity.END
        val params = when (config.route) {
            Route.TOP -> WindowManager.LayoutParams(
                (config.topWidthDp * density).roundToInt(),
                (TOP_HEIGHT_DP * density).roundToInt(),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                FLAGS,
                PixelFormat.TRANSLUCENT,
            ).apply { gravity = Gravity.TOP or horizontal }
            else -> WindowManager.LayoutParams(
                (config.widthDp * density).roundToInt(),
                (config.heightDp * density).roundToInt(),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                FLAGS,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or horizontal
                y = (metrics.heightPixels * config.position - config.heightDp * density / 2).roundToInt()
            }
        }
        params.fitInsetsTypes = 0
        params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        params.title = "ControlCentre:${config.route.name}"
        runCatching { windowManager.addView(view, params) }
            .onSuccess { views[config.route] = view }
            .onFailure { GestureLog.log("addView ${config.route} failed: $it") }
    }

    private companion object {
        const val TOP_HEIGHT_DP = 48f
        const val FLAGS = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
    }
}

/** One trigger region. Tracks the original pointer from DOWN to UP/CANCEL using raw screen coordinates. */
private class TriggerView(
    context: Context,
    private val config: TriggerConfig,
    private val density: Float,
    private val listener: TriggerWindowController.Listener,
) : View(context) {
    private val slopDp = ViewConfiguration.get(context).scaledTouchSlop / density
    private val tag = config.route.name
    private var router: GestureRouter? = null
    private var downX = 0f
    private var downY = 0f
    private var lastPage: Page? = null

    val tracking: Boolean get() = router != null

    init {
        if (config.showHandle) setBackgroundColor(if (config.route == Route.TOP) 0x40FFFFFF else 0x60FFFFFF)
        contentDescription = "Control Centre ${config.route.name.lowercase()} trigger"
    }

    /** Keeps the system Back gesture from stealing edge swipes that start on the handle (within the 200 dp per-edge budget). */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        systemGestureExclusionRects = listOf(Rect(0, 0, w, h))
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (!listener.isInputAllowed()) {
                    GestureLog.log("$tag DOWN ignored: input not allowed")
                    return false
                }
                downX = event.rawX
                downY = event.rawY
                lastPage = null
                router = GestureRouter(config.route, config.left, slopDp)
                GestureLog.log("$tag DOWN raw=(${event.rawX.roundToInt()},${event.rawY.roundToInt()})")
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val active = router ?: return false
                val sample = active.move((event.rawX - downX) / density, (event.rawY - downY) / density)
                if (sample.recognised && sample.page != lastPage) {
                    lastPage = sample.page
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    GestureLog.log("$tag page=${sample.page.id} progress=${(sample.progress * 100).roundToInt()}%")
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                val active = router ?: return false
                router = null
                val page = active.finish()
                val dx = ((event.rawX - downX) / density).roundToInt()
                val dy = ((event.rawY - downY) / density).roundToInt()
                GestureLog.log("$tag UP dx=${dx}dp dy=${dy}dp commit=${page?.id ?: "none"}")
                if (page != null) listener.onPageSelected(page, config.route)
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                router?.cancel()
                router = null
                GestureLog.log("$tag CANCEL")
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}

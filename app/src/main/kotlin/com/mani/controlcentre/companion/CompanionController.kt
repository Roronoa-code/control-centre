package com.mani.controlcentre.companion

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.mani.controlcentre.diagnostics.GestureLog
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Owns the two experiment windows: a small right-edge handle (only while the Quick Panel is open) and the
 * blank companion page it drags in. Both are TYPE_ACCESSIBILITY_OVERLAY windows created from the service context.
 */
class CompanionController(private val context: Context) {
    private val windowManager: WindowManager = context.getSystemService(WindowManager::class.java)
    private val density = context.resources.displayMetrics.density
    private val screenWidth = context.resources.displayMetrics.widthPixels
    private val screenHeight = context.resources.displayMetrics.heightPixels
    private var handle: HandleView? = null
    private var page: CompanionPageView? = null

    val handleShown: Boolean get() = handle != null
    val pageShown: Boolean get() = page != null
    val gestureActive: Boolean get() = handle?.tracking == true || page?.tracking == true

    fun showHandle() {
        if (handle != null) return
        val view = HandleView(context, this)
        val params = overlayParams(dp(HANDLE_WIDTH_DP), dp(HANDLE_HEIGHT_DP)).apply {
            gravity = Gravity.TOP or Gravity.END
            y = (screenHeight * HANDLE_TOP_FRACTION).roundToInt()
            title = "ControlCentre:CompanionHandle"
        }
        if (add(view, params)) {
            handle = view
            GestureLog.log("Companion handle shown at y=${params.y}px (${(params.y / density).roundToInt()}dp), ${HANDLE_WIDTH_DP}x${HANDLE_HEIGHT_DP}dp")
        }
    }

    fun removeAll(reason: String) {
        val had = handle != null || page != null
        page?.let { it.cancelAnimation(); remove(it) }
        page = null
        handle?.let { remove(it) }
        handle = null
        if (had) GestureLog.log("Companion windows removed: $reason")
    }

    /** Called by the handle when a leftward drag starts. Creates the page off-screen so it can follow the finger. */
    fun beginPage(): CompanionPageView {
        page?.let { return it }
        val view = CompanionPageView(context, this)
        val params = overlayParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT).apply {
            title = "ControlCentre:CompanionPage"
        }
        view.offset = screenWidth.toFloat()
        if (add(view, params)) {
            page = view
            GestureLog.log("Companion page window added")
        }
        return view
    }

    fun dragPage(offsetPx: Float) {
        page?.offset = offsetPx.coerceIn(0f, screenWidth.toFloat())
    }

    /** Settles the page fully shown or removes it. The handle stays until the Quick Panel closes. */
    fun settlePage(open: Boolean, why: String) {
        val current = page ?: return
        GestureLog.log("Companion page settle open=$open ($why)")
        if (open) {
            current.animateOffset(0f) { }
        } else {
            current.animateOffset(screenWidth.toFloat()) {
                if (page === current) {
                    remove(current)
                    page = null
                    GestureLog.log("Companion page removed")
                }
            }
        }
    }

    val width: Float get() = screenWidth.toFloat()

    private fun overlayParams(width: Int, height: Int) = WindowManager.LayoutParams(
        width,
        height,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply {
        fitInsetsTypes = 0
        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
    }

    private fun add(view: View, params: WindowManager.LayoutParams): Boolean =
        runCatching { windowManager.addView(view, params) }
            .onFailure { GestureLog.log("addView ${params.title} failed: $it") }
            .isSuccess

    private fun remove(view: View) {
        runCatching { windowManager.removeViewImmediate(view) }
            .onFailure { GestureLog.log("removeView failed: $it") }
    }

    fun dp(value: Float): Int = (value * density).roundToInt()

    companion object {
        const val HANDLE_WIDTH_DP = 16f
        const val HANDLE_HEIGHT_DP = 120f
        /** Right margin beside Samsung's tile grid on the S25 Ultra Quick Panel; nothing interactive sits under it. */
        const val HANDLE_TOP_FRACTION = 0.66f
    }
}

/** The narrow edge handle. Owns the pointer from DOWN to UP; only a leftward horizontal drag does anything. */
private class HandleView(context: Context, private val controller: CompanionController) : View(context) {
    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var dragging = false
    private var velocity: VelocityTracker? = null
    var tracking = false
        private set

    init {
        background = GradientDrawable().apply {
            setColor(0x88FFFFFF.toInt())
            cornerRadius = controller.dp(4f).toFloat()
        }
        contentDescription = "Companion page handle"
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        systemGestureExclusionRects = listOf(Rect(0, 0, w, h))
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                dragging = false
                tracking = true
                velocity = VelocityTracker.obtain().also { it.addMovement(event) }
                GestureLog.log("Handle DOWN raw=(${event.rawX.roundToInt()},${event.rawY.roundToInt()})")
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!tracking) return false
                velocity?.addMovement(event)
                val dx = event.rawX - downX
                val dy = event.rawY - downY
                if (!dragging && -dx > slop && abs(dx) > abs(dy)) {
                    dragging = true
                    controller.beginPage()
                    GestureLog.log("Handle drag started")
                }
                if (dragging) controller.dragPage(controller.width + dx)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!tracking) return false
                tracking = false
                val tracker = velocity
                var vx = 0f
                if (tracker != null) {
                    tracker.addMovement(event)
                    tracker.computeCurrentVelocity(1000)
                    vx = tracker.xVelocity
                    tracker.recycle()
                }
                velocity = null
                val dx = event.rawX - downX
                val action = if (event.actionMasked == MotionEvent.ACTION_UP) "UP" else "CANCEL"
                GestureLog.log("Handle $action dx=${(dx / resources.displayMetrics.density).roundToInt()}dp vx=${vx.roundToInt()} dragging=$dragging")
                if (dragging) {
                    val open = event.actionMasked == MotionEvent.ACTION_UP &&
                        CompanionPolicy.settlesOpen(controller.width + dx, controller.width, vx)
                    controller.settlePage(open, "handle $action")
                }
                dragging = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}

/**
 * Full-screen window whose opaque content slides horizontally. Touches never reach Samsung's panel underneath.
 * A rightward drag anywhere on the page removes it; the button is the emergency exit.
 */
class CompanionPageView(context: Context, private val controller: CompanionController) : FrameLayout(context) {
    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private val content: LinearLayout
    private var downX = 0f
    private var downY = 0f
    private var dragging = false
    private var velocity: VelocityTracker? = null
    var tracking = false
        private set

    var offset: Float
        get() = content.translationX
        set(value) { content.translationX = value }

    init {
        setBackgroundColor(Color.TRANSPARENT)
        content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(0xFF141416.toInt())
            val pad = controller.dp(24f)
            setPadding(pad, pad, pad, pad)
            addView(TextView(context).apply {
                text = "Companion — test page"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            })
            addView(TextView(context).apply {
                text = "Blank overlay above Samsung's open Quick Panel.\nSwipe right anywhere to return to the panel."
                setTextColor(0xFF9A9AA0.toInt())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                gravity = Gravity.CENTER
                setPadding(0, controller.dp(12f), 0, controller.dp(32f))
            })
            addView(Button(context).apply {
                text = "Close companion (emergency)"
                setOnClickListener {
                    GestureLog.log("Companion emergency close tapped")
                    controller.settlePage(false, "emergency button")
                }
            })
        }
        addView(content, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun animateOffset(target: Float, end: () -> Unit) {
        content.animate().cancel()
        content.animate()
            .translationX(target)
            .setDuration(180)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction { end() }
            .start()
    }

    fun cancelAnimation() {
        content.animate().cancel()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.rawX
                downY = ev.rawY
                dragging = false
                velocity = VelocityTracker.obtain().also { it.addMovement(ev) }
            }
            MotionEvent.ACTION_MOVE -> {
                velocity?.addMovement(ev)
                val dx = ev.rawX - downX
                val dy = ev.rawY - downY
                if (dx > slop && dx > abs(dy)) {
                    dragging = true
                    tracking = true
                    content.animate().cancel()
                    return true
                }
            }
        }
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                dragging = false
                velocity = VelocityTracker.obtain().also { it.addMovement(event) }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                velocity?.addMovement(event)
                val dx = event.rawX - downX
                if (!dragging && dx > slop && dx > abs(event.rawY - downY)) {
                    dragging = true
                    tracking = true
                    content.animate().cancel()
                }
                if (dragging) offset = dx.coerceAtLeast(0f)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val tracker = velocity
                var vx = 0f
                if (tracker != null) {
                    tracker.addMovement(event)
                    tracker.computeCurrentVelocity(1000)
                    vx = tracker.xVelocity
                    tracker.recycle()
                }
                velocity = null
                if (dragging) {
                    val stayOpen = event.actionMasked == MotionEvent.ACTION_UP &&
                        CompanionPolicy.settlesOpen(offset, controller.width, vx)
                    GestureLog.log("Page ${if (event.actionMasked == MotionEvent.ACTION_UP) "UP" else "CANCEL"} offset=${offset.roundToInt()}px vx=${vx.roundToInt()} stayOpen=$stayOpen")
                    controller.settlePage(stayOpen, "page drag")
                }
                dragging = false
                tracking = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}

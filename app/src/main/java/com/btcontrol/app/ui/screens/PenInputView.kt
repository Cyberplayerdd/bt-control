package com.btcontrol.app.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import com.btcontrol.app.BtControlApp
import com.btcontrol.app.data.FingerMode
import com.btcontrol.app.data.PenButtonAction
import com.btcontrol.app.data.Settings
import com.btcontrol.app.hid.HidDescriptor
import com.btcontrol.app.hid.Key
import com.btcontrol.app.hid.Mod
import com.btcontrol.app.hid.PenState
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

/**
 * Full-screen pen surface. Converts stylus MotionEvents (hover, pressure, tilt, side button,
 * eraser) into HID pen digitizer reports, mapped onto an active area with the PC screen's
 * aspect ratio. Fingers can be ignored (palm rejection), draw, or do gestures.
 */
@SuppressLint("ViewConstructor")
class PenInputView(context: Context) : View(context) {
    private val hid get() = BtControlApp.instance.hid
    private val d = resources.displayMetrics.density
    private val main = Handler(Looper.getMainLooper())

    var settings: Settings = BtControlApp.instance.settings.value
        set(value) {
            val aspectChanged = field.tabletAspect != value.tabletAspect
            field = value
            if (aspectChanged) computeArea()
        }
    var eraserMode = false
    var colors = Colors()
        set(value) { field = value; invalidate() }

    data class Colors(
        val background: Int = Color.rgb(24, 26, 30),
        val area: Int = Color.rgb(36, 39, 45),
        val border: Int = Color.rgb(90, 140, 255),
        val ink: Int = Color.rgb(170, 199, 255),
        val text: Int = Color.rgb(140, 145, 155),
    )

    private val area = RectF()
    private var penInRange = false
    private var lastPenTime = 0L
    private var lastStylusTime = 0L
    private var stylusInRange = false
    private var lastInvert = false
    private var hoverX = -1f
    private var hoverY = -1f

    // Local ink preview (fades out) so you can see what you draw on the tablet itself.
    private class P(val x: Float, val y: Float, val w: Float, val t: Long, val start: Boolean)
    private val trail = ArrayDeque<P>()
    private val inkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND; style = Paint.Style.STROKE }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private var hintLayout: StaticLayout? = null
    private var hintKey = ""

    private val outOfRange = Runnable {
        penInRange = false
        stylusInRange = false
        hoverX = -1f
        hid.penOutOfRange()
        invalidate()
    }

    init {
        isFocusable = true
        isClickable = true
    }

    /** Aspect ratio width/height of the active area, or null to use the whole view. */
    private fun aspect(): Float? {
        val a = settings.tabletAspect
        if (a == "fill") return null
        val parts = a.split(':')
        val w = parts.getOrNull(0)?.toFloatOrNull() ?: return null
        val h = parts.getOrNull(1)?.toFloatOrNull() ?: return null
        return if (h > 0f) w / h else null
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) = computeArea()

    private fun computeArea() {
        val m = 0f
        val w = width - 2 * m
        val h = height - 2 * m
        if (w <= 0 || h <= 0) return
        val a = aspect()
        if (a == null) area.set(m, m, width - m, height - m)
        else if (w / h > a) {
            val aw = h * a
            area.set((width - aw) / 2, m, (width + aw) / 2, height - m)
        } else {
            val ah = w / a
            area.set(m, (height - ah) / 2, width - m, (height + ah) / 2)
        }
        invalidate()
    }

    private fun isPen(tool: Int) = tool == MotionEvent.TOOL_TYPE_STYLUS || tool == MotionEvent.TOOL_TYPE_ERASER

    // ------------------------------------------------------------------ hover

    override fun onHoverEvent(e: MotionEvent): Boolean {
        if (!isPen(e.getToolType(0))) return super.onHoverEvent(e)
        when (e.actionMasked) {
            MotionEvent.ACTION_HOVER_ENTER, MotionEvent.ACTION_HOVER_MOVE -> {
                if (e.actionMasked == MotionEvent.ACTION_HOVER_ENTER) requestUnbufferedDispatch(e)
                main.removeCallbacks(outOfRange)
                stylusInRange = true
                lastStylusTime = SystemClock.uptimeMillis()
                sendPen(e, 0, tip = false)
                hoverX = e.x; hoverY = e.y
                // Watchdog in case HOVER_EXIT never comes.
                main.postDelayed(outOfRange, 3000)
                invalidate()
            }
            MotionEvent.ACTION_HOVER_EXIT -> {
                // Android sends HOVER_EXIT right before the pen touches: wait before leaving range.
                main.removeCallbacks(outOfRange)
                main.postDelayed(outOfRange, 90)
            }
        }
        return true
    }

    // ------------------------------------------------------------------ touch

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        val penIdx = (0 until e.pointerCount).firstOrNull { isPen(e.getToolType(it)) }
        if (penIdx != null) {
            handlePen(e, penIdx)
            return true
        }
        handleFingers(e)
        return true
    }

    private fun handlePen(e: MotionEvent, idx: Int) {
        stylusInRange = true
        lastStylusTime = SystemClock.uptimeMillis()
        if (fingerDrawing) endFingerPen(e, click = false)
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                requestUnbufferedDispatch(e)
                main.removeCallbacks(outOfRange)
                hoverX = -1f
                sendPen(e, idx, tip = true)
                addTrail(e.getX(idx), e.getY(idx), e.getPressure(idx), true)
            }
            MotionEvent.ACTION_MOVE -> {
                for (h in 0 until e.historySize) {
                    addTrail(e.getHistoricalX(idx, h), e.getHistoricalY(idx, h), e.getHistoricalPressure(idx, h), false)
                }
                addTrail(e.getX(idx), e.getY(idx), e.getPressure(idx), false)
                sendPen(e, idx, tip = true)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (e.actionMasked == MotionEvent.ACTION_POINTER_UP && e.actionIndex != idx) return
                sendPen(e, idx, tip = false)
                // Still hovering? Hover events will cancel this.
                main.removeCallbacks(outOfRange)
                main.postDelayed(outOfRange, 350)
            }
            MotionEvent.ACTION_CANCEL -> {
                main.removeCallbacks(outOfRange)
                outOfRange.run()
            }
        }
        invalidate()
    }

    private fun curve(p: Float): Int {
        val s = settings
        val x = p.coerceIn(0f, 1f).pow(s.pressureCurve.coerceIn(0.2f, 4f))
        val v = s.minPressure + (1f - s.minPressure) * x
        return (v * HidDescriptor.PEN_MAX_PRESSURE).roundToInt().coerceIn(1, HidDescriptor.PEN_MAX_PRESSURE)
    }

    private fun sendPen(e: MotionEvent, idx: Int, tip: Boolean, fixedPressure: Float? = null) {
        val s = settings
        val nx = ((e.getX(idx) - area.left) / area.width()).coerceIn(0f, 1f)
        val ny = ((e.getY(idx) - area.top) / area.height()).coerceIn(0f, 1f)
        val btn = e.buttonState
        val primary = btn and (MotionEvent.BUTTON_STYLUS_PRIMARY or MotionEvent.BUTTON_SECONDARY) != 0
        val secondary = btn and (MotionEvent.BUTTON_STYLUS_SECONDARY or MotionEvent.BUTTON_TERTIARY) != 0
        var barrel = false
        var erasing = eraserMode || e.getToolType(idx) == MotionEvent.TOOL_TYPE_ERASER || secondary
        when (s.penButtonAction) {
            PenButtonAction.RIGHT_CLICK -> barrel = primary
            PenButtonAction.ERASER -> if (primary) erasing = true
            PenButtonAction.NONE -> Unit
        }
        val pressure = if (tip) curve(fixedPressure ?: e.getPressure(idx)) else 0

        var tiltX = 0
        var tiltY = 0
        if (s.sendTilt && fixedPressure == null) {
            val tilt = e.getAxisValue(MotionEvent.AXIS_TILT, idx).coerceIn(0f, 1.48f)
            val orient = e.getAxisValue(MotionEvent.AXIS_ORIENTATION, idx)
            if (tilt > 0f) {
                val t = tan(tilt)
                tiltX = Math.toDegrees(atan(t * sin(orient)).toDouble()).roundToInt()
                tiltY = Math.toDegrees(atan(-t * cos(orient)).toDouble()).roundToInt()
            }
        }

        // Windows expects the pen to leave range when switching between tip and eraser end.
        if (penInRange && erasing != lastInvert) {
            hid.penOutOfRange()
            penInRange = false
        }
        lastInvert = erasing
        lastPenTime = SystemClock.uptimeMillis()

        val x = (nx * HidDescriptor.PEN_MAX_XY).roundToInt()
        val y = (ny * HidDescriptor.PEN_MAX_XY).roundToInt()
        if (!penInRange && tip) {
            // Touching without hovering first (finger mode, very fast pens): report a hover first.
            hid.pen(PenState(inRange = true, invert = erasing, x = x, y = y, tiltX = tiltX, tiltY = tiltY))
        }
        penInRange = true
        hid.pen(
            if (erasing) PenState(inRange = true, tip = false, barrel = false, invert = true, eraser = tip,
                x = x, y = y, pressure = pressure, tiltX = tiltX, tiltY = tiltY)
            else PenState(inRange = true, tip = tip, barrel = barrel, invert = false, eraser = false,
                x = x, y = y, pressure = pressure, tiltX = tiltX, tiltY = tiltY)
        )
    }

    // ------------------------------------------------------------------ fingers

    private var gMode = 0 // 0 = undecided, 1 = pan, 2 = zoom
    private var gStartSpan = 0f
    private var gLastSpan = 0f
    private var gStartX = 0f
    private var gStartY = 0f
    private var gLastX = 0f
    private var gLastY = 0f
    private var gMaxPointers = 0
    private var gDownTime = 0L
    private var gIgnored = false
    private var fingerDrawing = false

    private var fingerDownTime = 0L
    private var fingerDownX = 0f
    private var fingerDownY = 0f
    private var fingerMoved = false

    /** Palm rejection: ignore fingers while a real stylus is near the screen. */
    private fun stylusNear() = stylusInRange || SystemClock.uptimeMillis() - lastStylusTime < 500

    /**
     * Fingers (drawing without a stylus):
     *  - DRAW: one finger draws (fixed pressure)
     *  - HOVER: one finger moves the pen cursor without drawing, a tap clicks
     *  - GESTURES: one finger does nothing
     * In every mode except IGNORE, 2 fingers scroll / pinch-zoom, 2-finger tap = undo, 3 = redo.
     */
    private fun handleFingers(e: MotionEvent) {
        val s = settings
        if (s.fingerMode == FingerMode.IGNORE) return
        val palm = stylusNear()
        gestures(e, palm)
        val singleMode = s.fingerMode == FingerMode.DRAW || s.fingerMode == FingerMode.HOVER
        if (!singleMode) return
        if (palm || e.pointerCount > 1 || gMaxPointers > 1) {
            if (fingerDrawing) endFingerPen(e, click = false)
            return
        }
        val draw = s.fingerMode == FingerMode.DRAW
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                main.removeCallbacks(outOfRange)
                fingerDrawing = true
                fingerDownTime = e.eventTime
                fingerDownX = e.x; fingerDownY = e.y
                fingerMoved = false
                // Wait for the first move before touching so a 2-finger gesture doesn't leave a dot.
                sendPen(e, 0, tip = false, fixedPressure = 0f)
                hoverX = e.x; hoverY = e.y
            }
            MotionEvent.ACTION_MOVE -> if (fingerDrawing) {
                if (!fingerMoved && hypot(e.x - fingerDownX, e.y - fingerDownY) > 6 * d) {
                    fingerMoved = true
                    if (draw) addTrail(fingerDownX, fingerDownY, 0.6f, true)
                }
                if (fingerMoved) {
                    sendPen(e, 0, tip = draw, fixedPressure = if (draw) 0.6f else 0f)
                    if (draw) addTrail(e.x, e.y, 0.6f, false) else { hoverX = e.x; hoverY = e.y }
                }
            }
            MotionEvent.ACTION_UP -> if (fingerDrawing) {
                val tap = !fingerMoved && e.eventTime - fingerDownTime < 300
                endFingerPen(e, click = tap)
            }
            MotionEvent.ACTION_CANCEL -> if (fingerDrawing) endFingerPen(e, click = false)
        }
        invalidate()
    }

    private fun endFingerPen(e: MotionEvent, click: Boolean) {
        if (click) {
            // Tap = pen tap (click) at that spot.
            sendPen(e, 0, tip = true, fixedPressure = 0.6f)
        }
        sendPen(e, 0, tip = false, fixedPressure = 0f)
        fingerDrawing = false
        main.removeCallbacks(outOfRange)
        // Move mode keeps the cursor visible a little longer.
        main.postDelayed(outOfRange, if (settings.fingerMode == FingerMode.HOVER) 1500 else 120)
    }

    private fun centroid(e: MotionEvent): Pair<Float, Float> {
        var x = 0f; var y = 0f
        val n = e.pointerCount
        for (i in 0 until n) { x += e.getX(i); y += e.getY(i) }
        return x / n to y / n
    }

    private fun span(e: MotionEvent): Float =
        if (e.pointerCount < 2) 0f else hypot(e.getX(0) - e.getX(1), e.getY(0) - e.getY(1))

    /** 2 fingers: scroll / pinch-zoom (Ctrl+wheel) · 2-finger tap: undo · 3-finger tap: redo. */
    private fun gestures(e: MotionEvent, palm: Boolean) {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                gIgnored = palm
                gMode = 0
                gMaxPointers = 1
                gDownTime = e.eventTime
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                gMaxPointers = maxOf(gMaxPointers, e.pointerCount)
                val (cx, cy) = centroid(e)
                gStartX = cx; gStartY = cy; gLastX = cx; gLastY = cy
                gStartSpan = span(e); gLastSpan = gStartSpan
            }
            MotionEvent.ACTION_MOVE -> {
                if (gIgnored || palm || e.pointerCount < 2) return
                val (cx, cy) = centroid(e)
                val sp = span(e)
                if (gMode == 0) {
                    if (abs(sp - gStartSpan) > 40 * d) {
                        gMode = 2
                        hid.keyDown(Key.LEFT_CTRL)
                    } else if (hypot(cx - gStartX, cy - gStartY) > 20 * d) gMode = 1
                }
                when (gMode) {
                    1 -> {
                        val k = settings.scrollSpeed / (10f * d)
                        val dy = cy - gLastY
                        val dx = cx - gLastX
                        if (abs(dy) >= abs(dx)) hid.scroll(dy * k, 0f) else hid.scroll(0f, -dx * k)
                    }
                    2 -> if (gLastSpan > 0f && sp > 0f) hid.scroll(ln(sp / gLastSpan) * 6f, 0f)
                }
                gLastX = cx; gLastY = cy; gLastSpan = sp
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (gMode == 2) { hid.keyUp(Key.LEFT_CTRL); gMode = 3 }
                if (gMode == 1) gMode = 3
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (gMode == 2) hid.keyUp(Key.LEFT_CTRL)
                val quick = e.eventTime - gDownTime < 300
                if (!gIgnored && !palm && gMode == 0 && quick && e.actionMasked == MotionEvent.ACTION_UP) {
                    when (gMaxPointers) {
                        2 -> hid.shortcut(Key.Z, Mod.LCTRL)
                        3 -> hid.shortcut(Key.Z, Mod.LCTRL or Mod.LSHIFT)
                    }
                }
                gMode = 0
            }
        }
    }

    // ------------------------------------------------------------------ drawing

    private fun addTrail(x: Float, y: Float, p: Float, start: Boolean) {
        if (!settings.showInkTrail) return
        trail.addLast(P(x, y, p.coerceIn(0f, 1f), SystemClock.uptimeMillis(), start))
        while (trail.size > 4000) trail.removeFirst()
    }

    override fun onDraw(canvas: Canvas) {
        val c = colors
        canvas.clipRect(0, 0, width, height)
        canvas.drawColor(c.background)
        fill.color = c.area
        canvas.drawRoundRect(area, 6 * d, 6 * d, fill)
        stroke.color = c.border
        stroke.alpha = 90
        stroke.strokeWidth = 1.5f * d
        val inset = stroke.strokeWidth / 2
        canvas.drawRoundRect(area.left + inset, area.top + inset, area.right - inset, area.bottom - inset, 6 * d, 6 * d, stroke)

        val now = SystemClock.uptimeMillis()
        val life = 1400L
        while (trail.isNotEmpty() && now - trail.first().t > life) trail.removeFirst()
        if (trail.isEmpty() && hoverX < 0) {
            // Wrapped to the area width so nothing is cut off in portrait.
            val hint = "Pen area → PC screen" + (if (settings.tabletAspect == "fill") "" else " (${settings.tabletAspect})") +
                "\nHover to move · touch to draw" +
                "\n\nNo stylus? Tap the notch at the top › Fingers: draw (or move cursor)" +
                "\nArea shape, eraser and toolbar are in the notch too"
            val maxW = (width - 48 * d).toInt().coerceAtLeast(80)
            val key = "$hint|$maxW|${c.text}"
            if (key != hintKey) {
                textPaint.color = c.text
                textPaint.textSize = 15 * d
                hintLayout = StaticLayout.Builder.obtain(hint, 0, hint.length, textPaint, maxW)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setLineSpacing(0f, 1.2f)
                    .build()
                hintKey = key
            }
            hintLayout?.let { l ->
                canvas.save()
                canvas.translate(width / 2f - maxW / 2f, height / 2f - l.height / 2f)
                l.draw(canvas)
                canvas.restore()
            }
        }
        var prev: P? = null
        for (p in trail) {
            val pr = prev
            if (pr != null && !p.start) {
                val age = (now - p.t).toFloat() / life
                inkPaint.color = c.ink
                inkPaint.alpha = ((1f - age) * 255).toInt().coerceIn(0, 255)
                inkPaint.strokeWidth = (1.2f + p.w * 5f) * d
                canvas.drawLine(pr.x, pr.y, p.x, p.y, inkPaint)
            }
            prev = p
        }
        if (hoverX >= 0) {
            stroke.color = c.ink
            stroke.alpha = 200
            stroke.strokeWidth = 1.5f * d
            canvas.drawCircle(hoverX, hoverY, 6 * d, stroke)
        }
        if (trail.isNotEmpty()) postInvalidateOnAnimation()
    }

    override fun onDetachedFromWindow() {
        main.removeCallbacksAndMessages(null)
        if (penInRange) hid.penOutOfRange()
        if (gMode == 2) hid.keyUp(Key.LEFT_CTRL)
        super.onDetachedFromWindow()
    }
}

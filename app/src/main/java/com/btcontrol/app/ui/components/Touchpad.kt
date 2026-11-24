package com.btcontrol.app.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.btcontrol.app.BtControlApp
import com.btcontrol.app.hid.MouseButton
import kotlin.math.abs
import kotlin.math.min

/**
 * Laptop-style touchpad:
 *  - 1 finger: move · tap: left click · tap then drag: drag (left button held)
 *  - 2 fingers: scroll (vertical + horizontal) · tap: right click
 *  - 3 fingers tap: middle click
 */
@Composable
fun Touchpad(modifier: Modifier = Modifier, hint: String? = "Touchpad") {
    val app = BtControlApp.instance
    val hid = app.hid
    val settings by app.settings.flow.collectAsStateWithLifecycle()
    val s by rememberUpdatedState(settings)
    val lastTapUp = remember { LongArray(1) }
    val tick = rememberHaptic()

    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .pointerInput(Unit) {
                val slop = viewConfiguration.touchSlop
                val pxPerDp = density
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downTime = down.uptimeMillis
                    var maxPointers = 1
                    var moved = false
                    var travel = 0f
                    val dragging = s.tapToClick && downTime - lastTapUp[0] < 300
                    if (dragging) {
                        hid.mouseDown(MouseButton.LEFT)
                        lastTapUp[0] = 0
                    }
                    var prevCentroid: Offset? = null
                    var prevCount = 0
                    var upTime = downTime
                    try {
                        while (true) {
                            val ev = awaitPointerEvent()
                            val pressed = ev.changes.filter { it.pressed }
                            ev.changes.forEach { it.consume() }
                            if (pressed.isEmpty()) {
                                upTime = ev.changes.maxOfOrNull { it.uptimeMillis } ?: upTime
                                break
                            }
                            if (pressed.size > maxPointers) maxPointers = pressed.size

                            if (pressed.size == 1 && (maxPointers == 1 || dragging)) {
                                val c = pressed[0]
                                val d = c.position - c.previousPosition
                                travel += d.getDistance()
                                if (travel > slop) moved = true
                                if (moved) {
                                    val dt = (c.uptimeMillis - c.previousUptimeMillis).coerceAtLeast(1)
                                    val speedDp = d.getDistance() / pxPerDp / dt // dp per ms
                                    val gain = s.pointerSpeed * 1.4f / pxPerDp *
                                        (1f + s.pointerAcceleration * 0.6f * min(speedDp, 4f))
                                    hid.mouseMove(d.x * gain, d.y * gain)
                                }
                                prevCentroid = null
                            } else if (pressed.size >= 2) {
                                var cx = 0f
                                var cy = 0f
                                pressed.forEach { cx += it.position.x; cy += it.position.y }
                                val centroid = Offset(cx / pressed.size, cy / pressed.size)
                                val prev = prevCentroid
                                if (prev != null && prevCount == pressed.size) {
                                    val d = centroid - prev
                                    travel += d.getDistance()
                                    if (travel > slop) moved = true
                                    if (moved) {
                                        val notchesPerPx = s.scrollSpeed / (9f * pxPerDp)
                                        val dir = if (s.naturalScroll) 1f else -1f
                                        // Lock to the dominant axis for cleaner scrolling.
                                        if (abs(d.y) >= abs(d.x)) hid.scroll(d.y * notchesPerPx * dir, 0f)
                                        else hid.scroll(0f, -d.x * notchesPerPx * dir)
                                    }
                                }
                                prevCentroid = centroid
                                prevCount = pressed.size
                            }
                        }
                    } finally {
                        if (dragging) hid.mouseUp(MouseButton.LEFT)
                    }
                    if (!dragging && !moved && upTime - downTime < 350 && s.tapToClick) {
                        tick()
                        when (maxPointers) {
                            1 -> { hid.click(MouseButton.LEFT); lastTapUp[0] = upTime }
                            2 -> hid.click(MouseButton.RIGHT)
                            else -> hid.click(MouseButton.MIDDLE)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (hint != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    hint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Vertical strip: drag with one finger to scroll. */
@Composable
fun ScrollStrip(modifier: Modifier = Modifier) {
    val app = BtControlApp.instance
    val settings by app.settings.flow.collectAsStateWithLifecycle()
    val s by rememberUpdatedState(settings)
    Box(
        modifier
            .width(44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    while (true) {
                        val ev = awaitPointerEvent()
                        val c = ev.changes.firstOrNull() ?: break
                        c.consume()
                        if (!c.pressed) break
                        val dy = c.position.y - c.previousPosition.y
                        val dir = if (s.naturalScroll) 1f else -1f
                        app.hid.scroll(dy * s.scrollSpeed / (7f * density) * dir, 0f)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            repeat(3) {
                Box(
                    Modifier.padding(vertical = 3.dp).width(16.dp).height(2.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
            }
        }
    }
}

/**
 * Turns gyroscope rotation into pointer motion, independent of how the device is held and of the
 * screen rotation: horizontal motion = turning around the real-world vertical axis (gravity),
 * vertical motion = tilting around the screen's horizontal axis.
 */
class MotionPointer(private val context: Context, private val onMove: (dx: Float, dy: Float) -> Unit) : SensorEventListener {
    private val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyro = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val gravitySensor = sm.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val up = floatArrayOf(0f, 0f, 1f)
    private var last = 0L
    @Volatile var speed = 1f

    val available get() = gyro != null

    fun start() {
        last = 0L
        gyro?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        gravitySensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    fun stop() = sm.unregisterListener(this)

    private val displays = context.getSystemService(Context.DISPLAY_SERVICE) as android.hardware.display.DisplayManager

    private fun rotation(): Int = displays.getDisplay(android.view.Display.DEFAULT_DISPLAY)?.rotation ?: 0

    override fun onSensorChanged(e: SensorEvent) {
        if (e.sensor.type != Sensor.TYPE_GYROSCOPE) {
            // Low-pass the gravity / accelerometer vector ("up" in device coordinates).
            val a = if (e.sensor.type == Sensor.TYPE_GRAVITY) 1f else 0.1f
            for (i in 0..2) up[i] += a * (e.values[i] - up[i])
            return
        }
        val t = e.timestamp
        if (last == 0L) { last = t; return }
        val dt = (t - last) / 1e9f
        last = t
        val b = GyroCalibration.bias
        val wx = e.values[0] - b[0]; val wy = e.values[1] - b[1]; val wz = e.values[2] - b[2]

        val gl = kotlin.math.sqrt(up[0] * up[0] + up[1] * up[1] + up[2] * up[2]).takeIf { it > 1f } ?: return
        val ux = up[0] / gl; val uy = up[1] / gl; val uz = up[2] / gl

        // Screen "right" axis in device coordinates for the current display rotation.
        var rx = 1f; var ry = 0f
        when (rotation()) {
            android.view.Surface.ROTATION_90 -> { rx = 0f; ry = -1f }
            android.view.Surface.ROTATION_180 -> { rx = -1f; ry = 0f }
            android.view.Surface.ROTATION_270 -> { rx = 0f; ry = 1f }
        }
        // Make it horizontal (remove its vertical component).
        val d = rx * ux + ry * uy
        var hx = rx - d * ux; var hy = ry - d * uy; var hz = -d * uz
        val hl = kotlin.math.sqrt(hx * hx + hy * hy + hz * hz)
        if (hl < 0.2f) { hx = rx; hy = ry; hz = 0f } else { hx /= hl; hy /= hl; hz /= hl }

        val yaw = wx * ux + wy * uy + wz * uz       // turning left/right
        val pitch = wx * hx + wy * hy + wz * hz     // tilting up/down
        val dead = if (GyroCalibration.isCalibrated) 0.012f else 0.02f
        val k = 1500f * speed * dt
        val dx = if (abs(yaw) > dead) -yaw * k else 0f
        val dy = if (abs(pitch) > dead) -pitch * k else 0f
        if (dx != 0f || dy != 0f) onMove(dx, dy)
    }

    override fun onAccuracyChanged(s: Sensor?, a: Int) {}
}

/**
 * "Air mouse": while [enabled], moving the device moves the mouse cursor, whatever the screen
 * rotation and however the device is held.
 */
@Composable
fun AirMouse(enabled: Boolean) {
    val context = LocalContext.current
    val app = BtControlApp.instance
    val settings by app.settings.flow.collectAsStateWithLifecycle()
    val pointer = remember { MotionPointer(context.applicationContext) { dx, dy -> app.hid.mouseMove(dx, dy) } }
    pointer.speed = settings.airMouseSpeed
    DisposableEffect(enabled) {
        if (enabled) pointer.start()
        onDispose { pointer.stop() }
    }
}

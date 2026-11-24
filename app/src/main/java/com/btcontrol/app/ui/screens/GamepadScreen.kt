package com.btcontrol.app.ui.screens

import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.btcontrol.app.BtControlApp
import com.btcontrol.app.hid.Hat
import com.btcontrol.app.hid.PadButton
import com.btcontrol.app.ui.components.HoldButton
import com.btcontrol.app.ui.components.rememberHaptic
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

/**
 * Virtual gamepad (Xbox-style layout). Windows sees it as a standard HID game controller
 * ("Set up USB game controllers" / joy.cpl). Steam Input maps it for XInput games.
 */
@Composable
fun GamepadScreen() {
    val app = BtControlApp.instance
    val hid = app.hid
    val settings by app.settings.flow.collectAsStateWithLifecycle()

    var leftStickActive by remember { mutableStateOf(false) }
    TiltSteering(settings.gamepadTiltSteering && !leftStickActive, settings.gamepadTiltSensitivity)

    DisposableEffect(Unit) {
        hid.padReset()
        onDispose { hid.padReset() }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        val portrait = w < h

        fun Modifier.at(cx: Dp, cy: Dp, size: Dp) = this.offset(cx - size / 2, cy - size / 2).size(size)
        fun Modifier.atRect(x: Dp, y: Dp, width: Dp, height: Dp) = this.offset(x, y).size(width, height)

        val leftStick: @Composable (Modifier) -> Unit = { m ->
            Stick(right = false, floating = settings.gamepadFloatingSticks, modifier = m, onActive = { leftStickActive = it })
        }
        val rightStick: @Composable (Modifier) -> Unit = { m ->
            Stick(right = true, floating = settings.gamepadFloatingSticks, modifier = m)
        }
        val faceButtons: @Composable (Dp, Dp, Dp) -> Unit = { fx, fy, face ->
            val d = face * 1.05f
            FaceKey("Y", Color(0xFFF9A825), Modifier.at(fx, fy - d, face), PadButton.Y)
            FaceKey("A", Color(0xFF2E7D32), Modifier.at(fx, fy + d, face), PadButton.A)
            FaceKey("X", Color(0xFF1565C0), Modifier.at(fx - d, fy, face), PadButton.X)
            FaceKey("B", Color(0xFFC62828), Modifier.at(fx + d, fy, face), PadButton.B)
        }
        val shoulders: @Composable (Dp, Dp, Dp) -> Unit = { top, shW, shH ->
            PadKey("LT", Modifier.atRect(w * 0.02f, top, shW, shH), onPress = { hid.padTrigger(false, 1f) }, onRelease = { hid.padTrigger(false, 0f) })
            PadKey("LB", Modifier.atRect(w * 0.02f, top + shH + 6.dp, shW, shH), PadButton.LB)
            PadKey("RT", Modifier.atRect(w * 0.98f - shW, top, shW, shH), onPress = { hid.padTrigger(true, 1f) }, onRelease = { hid.padTrigger(true, 0f) })
            PadKey("RB", Modifier.atRect(w * 0.98f - shW, top + shH + 6.dp, shW, shH), PadButton.RB)
        }
        val center: @Composable (Dp, Dp, Dp) -> Unit = { cy, cw, ch ->
            PadKey("Back", Modifier.atRect(w * 0.5f - cw * 1.7f, cy, cw, ch), PadButton.BACK, small = true)
            PadKey("\u2302", Modifier.atRect(w * 0.5f - cw / 2, cy, cw, ch), PadButton.HOME)
            PadKey("Start", Modifier.atRect(w * 0.5f + cw * 0.7f, cy, cw, ch), PadButton.START, small = true)
            PadKey("L3", Modifier.atRect(w * 0.5f - cw * 1.2f, cy + ch + 8.dp, cw, ch), PadButton.L3, small = true)
            PadKey("R3", Modifier.atRect(w * 0.5f + cw * 0.2f, cy + ch + 8.dp, cw, ch), PadButton.R3, small = true)
        }

        if (portrait) {
            // Tall screen: left stick + face buttons on top, D-pad + right stick below.
            val stick = min(w.value * 0.44f, h.value * 0.27f).dp
            val face = min(w.value * 0.13f, h.value * 0.08f).dp
            shoulders(h * 0.03f, w * 0.3f, h * 0.06f)
            leftStick(Modifier.at(w * 0.27f, h * 0.36f, stick))
            faceButtons(w * 0.74f, h * 0.36f, face)
            center(h * 0.53f, w * 0.15f, h * 0.055f)
            DPad(Modifier.at(w * 0.27f, h * 0.80f, stick * 0.9f))
            rightStick(Modifier.at(w * 0.73f, h * 0.80f, stick * 0.9f))
        } else {
            val stick = min(h.value * 0.40f, w.value * 0.24f).dp
            val face = min(h.value * 0.13f, w.value * 0.075f).dp
            shoulders(h * 0.04f, w * 0.17f, h * 0.12f)
            leftStick(Modifier.at(w * 0.17f, h * 0.55f, stick))
            rightStick(Modifier.at(w * 0.66f, h * 0.76f, stick * 0.85f))
            DPad(Modifier.at(w * 0.34f, h * 0.76f, stick * 0.85f))
            faceButtons(w * 0.85f, h * 0.50f, face)
            center(h * 0.30f, w * 0.085f, h * 0.10f)
        }
    }
}

@Composable
private fun PadKey(
    label: String,
    modifier: Modifier,
    button: Int = -1,
    small: Boolean = false,
    onPress: (() -> Unit)? = null,
    onRelease: (() -> Unit)? = null,
) {
    val hid = BtControlApp.instance.hid
    HoldButton(
        modifier,
        onPress = { onPress?.invoke() ?: hid.padButton(button, true) },
        onRelease = { onRelease?.invoke() ?: hid.padButton(button, false) },
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(label, fontSize = if (small) 12.sp else 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FaceKey(label: String, color: Color, modifier: Modifier, button: Int) {
    val hid = BtControlApp.instance.hid
    HoldButton(
        modifier,
        onPress = { hid.padButton(button, true) },
        onRelease = { hid.padButton(button, false) },
        shape = CircleShape,
        containerColor = color.copy(alpha = 0.22f),
        pressedColor = color.copy(alpha = 0.6f),
    ) {
        Text(label, color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

/** Analog stick. Double-tap and hold = stick click (L3/R3). */
@Composable
internal fun Stick(right: Boolean, floating: Boolean, modifier: Modifier, onActive: (Boolean) -> Unit = {}) {
    val hid = BtControlApp.instance.hid
    var knob by remember { mutableStateOf(Offset.Zero) }
    var origin by remember { mutableStateOf<Offset?>(null) }
    val lastUp = remember { LongArray(1) }
    val tick = rememberHaptic()
    val active by rememberUpdatedState(onActive)
    val baseColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val ringColor = MaterialTheme.colorScheme.outlineVariant
    val knobColor = MaterialTheme.colorScheme.primary

    Box(
        modifier.pointerInput(floating) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                down.consume()
                val center = if (floating) down.position else Offset(size.width / 2f, size.height / 2f)
                origin = center
                val radius = min(size.width, size.height) / 2f * 0.75f
                val click = down.uptimeMillis - lastUp[0] < 280
                if (click) {
                    tick(); hid.padButton(if (right) PadButton.R3 else PadButton.L3, true)
                }
                active(true)
                fun update(p: Offset) {
                    var v = (p - center) / radius
                    val len = v.getDistance()
                    if (len > 1f) v /= len
                    if (len < 0.06f) v = Offset.Zero
                    knob = v
                    hid.padStick(right, v.x, v.y)
                }
                update(down.position)
                try {
                    while (true) {
                        val ev = awaitPointerEvent()
                        val c = ev.changes.firstOrNull { it.id == down.id } ?: break
                        c.consume()
                        if (!c.pressed) {
                            lastUp[0] = c.uptimeMillis
                            break
                        }
                        update(c.position)
                    }
                } finally {
                    knob = Offset.Zero
                    origin = null
                    hid.padStick(right, 0f, 0f)
                    if (click) hid.padButton(if (right) PadButton.R3 else PadButton.L3, false)
                    active(false)
                }
            }
        }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val c = origin ?: Offset(size.width / 2f, size.height / 2f)
            val r = min(size.width, size.height) / 2f * 0.75f
            drawCircle(baseColor, radius = r + r * 0.25f, center = c)
            drawCircle(ringColor, radius = r, center = c, style = Stroke(width = 3f))
            drawCircle(knobColor.copy(alpha = 0.85f), radius = r * 0.42f, center = c + knob * r)
        }
    }
}

/** 8-way D-pad; slide between directions without lifting. */
@Composable
internal fun DPad(modifier: Modifier) {
    val hid = BtControlApp.instance.hid
    var dir by remember { mutableIntStateOf(Hat.CENTER) }
    val tick = rememberHaptic()
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val on = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    val arrow = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                down.consume()
                val center = Offset(size.width / 2f, size.height / 2f)
                val dead = min(size.width, size.height) * 0.12f
                fun update(p: Offset) {
                    val v = p - center
                    val nd = if (hypot(v.x, v.y) < dead) Hat.CENTER else {
                        // angle 0 = up, clockwise
                        val a = Math.toDegrees(atan2(v.x.toDouble(), -v.y.toDouble()))
                        (((a + 360 + 22.5) % 360) / 45).toInt().coerceIn(0, 7)
                    }
                    if (nd != dir) {
                        dir = nd
                        if (nd != Hat.CENTER) tick()
                        hid.padHat(nd)
                    }
                }
                update(down.position)
                try {
                    while (true) {
                        val ev = awaitPointerEvent()
                        val c = ev.changes.firstOrNull { it.id == down.id } ?: break
                        c.consume()
                        if (!c.pressed) break
                        update(c.position)
                    }
                } finally {
                    dir = Hat.CENTER
                    hid.padHat(Hat.CENTER)
                }
            }
        }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val s = min(size.width, size.height)
            val arm = s / 3f
            val cx = size.width / 2f
            val cy = size.height / 2f
            fun armRect(dx: Int, dy: Int, lit: Boolean) {
                drawRoundRect(
                    if (lit) on else base,
                    topLeft = Offset(cx - arm / 2 + dx * arm, cy - arm / 2 + dy * arm),
                    size = androidx.compose.ui.geometry.Size(arm, arm),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(arm * 0.2f),
                )
            }
            val up = dir in listOf(Hat.UP, Hat.UP_RIGHT, Hat.UP_LEFT)
            val down = dir in listOf(Hat.DOWN, Hat.DOWN_RIGHT, Hat.DOWN_LEFT)
            val left = dir in listOf(Hat.LEFT, Hat.UP_LEFT, Hat.DOWN_LEFT)
            val rightLit = dir in listOf(Hat.RIGHT, Hat.UP_RIGHT, Hat.DOWN_RIGHT)
            armRect(0, -1, up); armRect(0, 1, down); armRect(-1, 0, left); armRect(1, 0, rightLit)
            armRect(0, 0, false)
            drawCircle(arrow.copy(alpha = 0.4f), radius = arm * 0.12f, center = Offset(cx, cy))
        }
    }
}

/** Tilt the phone like a steering wheel to move the left stick horizontally. */
@Composable
private fun TiltSteering(enabled: Boolean, sensitivity: Float) {
    val context = LocalContext.current
    val hid = BtControlApp.instance.hid
    val sens by rememberUpdatedState(sensitivity)
    DisposableEffect(enabled) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        @Suppress("DEPRECATION")
        val rotation = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val ax = e.values[0]
                val ay = e.values[1]
                val steer = when (rotation) {
                    Surface.ROTATION_90 -> ay
                    Surface.ROTATION_270 -> -ay
                    Surface.ROTATION_180 -> ax
                    else -> -ax
                } / SensorManager.GRAVITY_EARTH
                val v = (steer * 2.2f * sens).coerceIn(-1f, 1f)
                hid.padStick(false, if (kotlin.math.abs(v) < 0.04f) 0f else v, 0f)
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (enabled && sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose {
            sm.unregisterListener(listener)
            if (enabled) hid.padStick(false, 0f, 0f)
        }
    }
}

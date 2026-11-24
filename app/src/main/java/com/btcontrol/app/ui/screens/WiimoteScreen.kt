package com.btcontrol.app.ui.screens

import android.content.Context
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Display
import android.view.Surface
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.btcontrol.app.BtControlApp
import com.btcontrol.app.hid.PadButton
import com.btcontrol.app.ui.components.AutoText
import com.btcontrol.app.ui.components.HoldButton
import com.btcontrol.app.ui.components.MotionPointer
import kotlin.math.sqrt

/** Wiimote → gamepad mapping (configure the same in Dolphin's "Emulated Wii Remote"). */
object WiiMap {
    const val A = PadButton.A
    const val B = PadButton.B
    const val ONE = PadButton.X
    const val TWO = PadButton.Y
    const val MINUS = PadButton.BACK
    const val PLUS = PadButton.START
    const val HOME = PadButton.HOME
    const val C = PadButton.LB
    const val Z = PadButton.RB
    const val SHAKE = 13
}

/**
 * Wii Remote for the Dolphin emulator: buttons, D-pad, IR pointer (gyroscope → Rx/Ry axes),
 * tilt (gravity → right stick), shake (button 14) and an optional Nunchuk (left stick + C/Z).
 * Portrait = remote pointed at the screen; landscape = sideways remote (NES-style).
 */
@Composable
fun WiimoteScreen() {
    val app = BtControlApp.instance
    val hid = app.hid
    val settings by app.settings.flow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pointerOn by rememberSaveable { mutableStateOf(settings.wiimotePointer) }
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    DisposableEffect(Unit) {
        hid.padReset()
        hid.padPointer(0f, 0f)
        onDispose { hid.padReset() }
    }

    // IR pointer: the gyroscope moves an absolute pointer sent on the Rx / Ry axes (Dolphin's IR),
    // so it never fights the real PC mouse. Optionally the PC mouse can follow as well.
    val pointerPos = remember { floatArrayOf(0f, 0f) }
    val alsoMouse by rememberUpdatedState(settings.wiimoteMouse)
    val pointer = remember {
        MotionPointer(context.applicationContext) { dx, dy ->
            pointerPos[0] = (pointerPos[0] + dx / 500f).coerceIn(-1f, 1f)
            pointerPos[1] = (pointerPos[1] + dy / 500f).coerceIn(-1f, 1f)
            hid.padPointer(pointerPos[0], pointerPos[1])
            if (alsoMouse) hid.mouseMove(dx, dy)
        }
    }
    pointer.speed = settings.airMouseSpeed
    DisposableEffect(pointerOn) {
        if (pointerOn) pointer.start() else { pointerPos[0] = 0f; pointerPos[1] = 0f; hid.padPointer(0f, 0f) }
        onDispose { pointer.stop() }
    }

    // Tilt (right stick) and shake (button) from the accelerometer. The neutral tilt is the angle
    // the device is held at when the tab opens (or when "Recenter" is tapped).
    val tiltOn by rememberUpdatedState(settings.wiimoteTilt)
    val neutral = remember { floatArrayOf(Float.NaN, 0f) }
    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val displays = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val grav = FloatArray(3)
        val main = Handler(Looper.getMainLooper())
        var shakeUntil = 0L
        val startTime = SystemClock.uptimeMillis()
        val releaseShake = Runnable { hid.padButton(WiiMap.SHAKE, false) }
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                for (i in 0..2) grav[i] += 0.15f * (e.values[i] - grav[i])
                val lx = e.values[0] - grav[0]; val ly = e.values[1] - grav[1]; val lz = e.values[2] - grav[2]
                val shake = sqrt(lx * lx + ly * ly + lz * lz)
                val now = SystemClock.uptimeMillis()
                if (shake > 14f && now > shakeUntil) {
                    shakeUntil = now + 250
                    hid.padButton(WiiMap.SHAKE, true)
                    main.removeCallbacks(releaseShake)
                    main.postDelayed(releaseShake, 180)
                }
                if (!tiltOn) return
                // Gravity in screen coordinates.
                var sx = grav[0]; var sy = grav[1]
                when (displays.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: 0) {
                    Surface.ROTATION_90 -> { sx = -grav[1]; sy = grav[0] }
                    Surface.ROTATION_180 -> { sx = -grav[0]; sy = -grav[1] }
                    Surface.ROTATION_270 -> { sx = grav[1]; sy = -grav[0] }
                }
                val g = SensorManager.GRAVITY_EARTH
                if (neutral[0].isNaN()) {
                    if (now < startTime + 400) return // let the filter settle
                    neutral[0] = sx; neutral[1] = sy
                }
                val tx = (-(sx - neutral[0]) / g * 1.6f).coerceIn(-1f, 1f)
                val ty = (-(sy - neutral[1]) / g * 1.6f).coerceIn(-1f, 1f)
                hid.padStick(true, if (kotlin.math.abs(tx) < 0.04f) 0f else tx, if (kotlin.math.abs(ty) < 0.04f) 0f else ty)
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        acc?.let { sm.registerListener(l, it, SensorManager.SENSOR_DELAY_GAME) }
        onDispose {
            sm.unregisterListener(l)
            main.removeCallbacks(releaseShake)
            hid.padStick(true, 0f, 0f)
        }
    }

    val bar: @Composable () -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(selected = pointerOn, onClick = { pointerOn = !pointerOn }, label = { Text("Pointer") })
            FilterChip(selected = false, onClick = {
                neutral[0] = Float.NaN
                pointerPos[0] = 0f; pointerPos[1] = 0f
                hid.padPointer(0f, 0f)
            }, label = { Text("Recenter") })
            FilterChip(
                selected = settings.wiimoteNunchuk,
                onClick = { app.settings.update { it.copy(wiimoteNunchuk = !it.wiimoteNunchuk) } },
                label = { Text("Nunchuk") },
            )
            WiiKey("Shake", WiiMap.SHAKE, Modifier.height(36.dp).width(72.dp), small = true)
        }
    }

    if (landscape) {
        // Sideways remote: D-pad on the left, 1 / 2 on the right.
        BoxWithConstraints(Modifier.fillMaxSize().padding(16.dp).navigationBarsPadding()) {
            val h = maxHeight
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                DPad(Modifier.size(h * 0.75f))
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        WiiKey("−", WiiMap.MINUS, Modifier.size(52.dp), round = true)
                        WiiKey("⌂", WiiMap.HOME, Modifier.size(52.dp), round = true)
                        WiiKey("+", WiiMap.PLUS, Modifier.size(52.dp), round = true)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        WiiKey("A", WiiMap.A, Modifier.size(52.dp), round = true)
                        WiiKey("B", WiiMap.B, Modifier.size(52.dp), round = true)
                    }
                    bar()
                }
                Spacer(Modifier.weight(1f))
                WiiKey("1", WiiMap.ONE, Modifier.size(h * 0.32f), round = true, big = true)
                Spacer(Modifier.width(16.dp))
                WiiKey("2", WiiMap.TWO, Modifier.size(h * 0.32f), round = true, big = true)
            }
        }
        return
    }

    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(top = 22.dp, bottom = 8.dp).navigationBarsPadding()) {
        val w = maxWidth
        val nunchuk = settings.wiimoteNunchuk
        val boxH = maxHeight
        val unit: Dp = minOf(w, maxHeight * (if (nunchuk) 0.42f else 0.55f))
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                // B trigger along the left edge (it is underneath on a real remote).
                WiiKey("B", WiiMap.B, Modifier.width(w * 0.2f).fillMaxHeight(0.85f), big = true)
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxHeight()) {
                    DPad(Modifier.size(unit * 0.5f))
                    WiiKey("A", WiiMap.A, Modifier.size(unit * 0.26f), round = true, big = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(unit * 0.06f), verticalAlignment = Alignment.CenterVertically) {
                        WiiKey("−", WiiMap.MINUS, Modifier.size(unit * 0.13f), round = true)
                        WiiKey("⌂", WiiMap.HOME, Modifier.size(unit * 0.13f), round = true)
                        WiiKey("+", WiiMap.PLUS, Modifier.size(unit * 0.13f), round = true)
                    }
                    WiiKey("1", WiiMap.ONE, Modifier.size(unit * 0.16f), round = true)
                    WiiKey("2", WiiMap.TWO, Modifier.size(unit * 0.16f), round = true)
                }
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(w * 0.2f))
            }
            if (nunchuk) NunchukRow(Modifier.fillMaxWidth().height(boxH * 0.3f))
            bar()
        }
    }
}

@Composable
private fun NunchukRow(modifier: Modifier) {
    Row(modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Stick(right = false, floating = false, modifier = Modifier.fillMaxHeight().weight(1f))
        Column(Modifier.width(110.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WiiKey("C", WiiMap.C, Modifier.fillMaxWidth().weight(1f))
            WiiKey("Z", WiiMap.Z, Modifier.fillMaxWidth().weight(1.4f))
        }
    }
}

@Composable
private fun WiiKey(label: String, button: Int, modifier: Modifier, round: Boolean = false, big: Boolean = false, small: Boolean = false) {
    val hid = BtControlApp.instance.hid
    HoldButton(
        modifier,
        onPress = { hid.padButton(button, true) },
        onRelease = { hid.padButton(button, false) },
        shape = if (round) CircleShape else RoundedCornerShape(18.dp),
        containerColor = if (big) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        AutoText(label, maxSize = if (big) 30.sp else if (small) 13.sp else 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
    }
}

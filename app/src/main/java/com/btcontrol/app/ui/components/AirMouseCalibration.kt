package com.btcontrol.app.ui.components

import com.btcontrol.app.R
import com.btcontrol.app.t
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.Mouse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.btcontrol.app.BtControlApp
import com.btcontrol.app.data.Settings
import kotlin.math.abs

/**
 * Gyroscope offset ("bias"). Every gyroscope reports a small rotation even when it lies still,
 * which makes an air-mouse cursor creep; calibration measures it once and it is subtracted.
 */
object GyroCalibration {
    @Volatile var bias = floatArrayOf(0f, 0f, 0f)
        private set

    fun load(s: Settings) {
        bias = floatArrayOf(s.gyroBiasX, s.gyroBiasY, s.gyroBiasZ)
    }

    /**
     * Samples the gyroscope for 2 s (after a short settle delay). Fails if the device moved.
     * Callbacks run on the main thread.
     */
    fun calibrate(context: Context, onProgress: (Float) -> Unit, onDone: (Boolean) -> Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val gyro = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val main = Handler(Looper.getMainLooper())
        if (gyro == null) { onDone(false); return }
        val settle = 600L
        val duration = 2000L
        val start = SystemClock.uptimeMillis()
        val sum = DoubleArray(3)
        val min = floatArrayOf(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE)
        val max = floatArrayOf(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE)
        var n = 0
        var finished = false
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                if (finished) return
                val t = SystemClock.uptimeMillis() - start
                if (t < settle) return
                for (i in 0..2) {
                    sum[i] += e.values[i]
                    if (e.values[i] < min[i]) min[i] = e.values[i]
                    if (e.values[i] > max[i]) max[i] = e.values[i]
                }
                n++
                val p = ((t - settle).toFloat() / duration).coerceIn(0f, 1f)
                main.post { onProgress(p) }
                if (t >= settle + duration) {
                    finished = true
                    sm.unregisterListener(this)
                    // Still enough? (a hand-held device wobbles far more than this)
                    val still = n > 20 && (0..2).all { max[it] - min[it] < 0.08f }
                    if (still) {
                        val b = FloatArray(3) { (sum[it] / n).toFloat() }
                        bias = b
                        BtControlApp.instance.settings.update { it.copy(gyroBiasX = b[0], gyroBiasY = b[1], gyroBiasZ = b[2]) }
                    }
                    main.post { onDone(still) }
                }
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        sm.registerListener(listener, gyro, SensorManager.SENSOR_DELAY_GAME)
    }

    /** True when a calibration has been stored. */
    val isCalibrated get() = bias.any { abs(it) > 0f }
}

/** Dialog that walks through the air-mouse calibration. */
@Composable
fun CalibrateAirMouseDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var state by remember { mutableIntStateOf(0) } // 0 ready, 1 running, 2 done, 3 moved
    var progress by remember { mutableFloatStateOf(0f) }
    AlertDialog(
        onDismissRequest = { if (state != 1) onDismiss() },
        icon = { Icon(Icons.Outlined.GpsFixed, null) },
        title = { Text(t(R.string.calibrate_air_mouse)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    when (state) {
                        0 -> t(R.string.put_the_device_down_flat_on_a_table_tap_st)
                        1 -> t(R.string.calibrating_keep_the_device_still)
                        2 -> t(R.string.done_the_cursor_should_now_stay_put_when_y)
                        else -> t(R.string.the_device_moved_during_the_measurement_pu)
                    }
                )
                if (state == 1) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            when (state) {
                0, 3 -> Button(onClick = {
                    state = 1
                    progress = 0f
                    GyroCalibration.calibrate(context.applicationContext, { progress = it }) { ok -> state = if (ok) 2 else 3 }
                }) { Text(if (state == 3) t(R.string.try_again) else t(R.string.start)) }
                2 -> Button(onClick = onDismiss) { Text(t(R.string.done)) }
                else -> Unit
            }
        },
        dismissButton = { if (state == 0 || state == 3) TextButton(onClick = onDismiss) { Text(t(R.string.cancel)) } },
    )
}

/** Clear, labelled air-mouse switch + calibrate button. */
@Composable
fun AirMouseBar(on: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    var calibrate by remember { mutableStateOf(false) }
    if (calibrate) CalibrateAirMouseDialog { calibrate = false }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (on) {
            Button(onClick = onToggle, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.Mouse, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                Text(t(R.string.air_mouse_on_move_the_device))
            }
        } else {
            FilledTonalButton(onClick = onToggle, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.Mouse, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                Text(t(R.string.air_mouse_off))
            }
        }
        OutlinedButton(onClick = { calibrate = true }) {
            Icon(Icons.Outlined.GpsFixed, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
            Text(t(R.string.calibrate))
        }
    }
}

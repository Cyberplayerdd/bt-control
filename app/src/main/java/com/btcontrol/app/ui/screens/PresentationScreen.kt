package com.btcontrol.app.ui.screens

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.btcontrol.app.BtControlApp
import com.btcontrol.app.data.PresentationKeys
import com.btcontrol.app.hid.Key
import com.btcontrol.app.hid.Mod
import com.btcontrol.app.ui.MainActivity
import com.btcontrol.app.ui.components.AirMouse
import com.btcontrol.app.ui.components.AutoText
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import com.btcontrol.app.ui.components.HoldButton
import com.btcontrol.app.ui.components.KeyCap
import kotlinx.coroutines.delay

/**
 * Presentation remote: big next/previous buttons, slideshow shortcuts (PowerPoint, Google
 * Slides, Keynote, LibreOffice Impress), a laser pointer driven by the gyroscope, a timer, and
 * volume buttons for next/previous slide.
 */
@Composable
fun PresentationScreen() {
    val app = BtControlApp.instance
    val hid = app.hid
    val settings by app.settings.flow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var laser by remember { mutableStateOf(false) }
    var pptShortcuts by rememberSaveable { mutableStateOf(true) }
    var calibrate by remember { mutableStateOf(false) }
    if (calibrate) com.btcontrol.app.ui.components.CalibrateAirMouseDialog { calibrate = false }

    val (nextKey, prevKey) = when (settings.presentationKeys) {
        PresentationKeys.ARROWS -> Key.RIGHT to Key.LEFT
        PresentationKeys.PAGE_UP_DOWN -> Key.PAGE_DOWN to Key.PAGE_UP
        PresentationKeys.SPACE_BACKSPACE -> Key.SPACE to Key.BACKSPACE
    }
    val next by rememberUpdatedState({ hid.tap(nextKey) })
    val prev by rememberUpdatedState({ hid.tap(prevKey) })

    AirMouse(laser)

    // Volume buttons → next / previous slide.
    DisposableEffect(settings.presentationVolumeKeys) {
        if (settings.presentationVolumeKeys) {
            MainActivity.keyInterceptor = { e ->
                when (e.keyCode) {
                    KeyEvent.KEYCODE_VOLUME_UP -> { if (e.action == KeyEvent.ACTION_DOWN && e.repeatCount == 0) next(); true }
                    KeyEvent.KEYCODE_VOLUME_DOWN -> { if (e.action == KeyEvent.ACTION_DOWN && e.repeatCount == 0) prev(); true }
                    else -> false
                }
            }
        }
        onDispose { MainActivity.keyInterceptor = null }
    }

    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val prevButton: @Composable (Modifier) -> Unit = { m ->
        HoldButton(m, onRelease = { prev() }, shape = RoundedCornerShape(24.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, Modifier.size(40.dp))
                Text("Previous")
            }
        }
    }
    val nextButton: @Composable (Modifier) -> Unit = { m ->
        HoldButton(m, onRelease = { next() }, shape = RoundedCornerShape(24.dp), containerColor = MaterialTheme.colorScheme.primaryContainer) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(56.dp))
                Text("Next", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
    val shortcuts: @Composable (Modifier) -> Unit = { m ->
        Row(m, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Shortcut("Start", "F5") { hid.tap(Key.F5) }
            Shortcut("From here", "⇧F5") { hid.tap(Key.F5, Mod.LSHIFT) }
            Shortcut("Black", "B") { hid.shortcut(Key.B) }
            Shortcut("White", "W") { hid.shortcut(Key.W) }
            Shortcut("End", "Esc") { hid.tap(Key.ESC) }
        }
    }
    val laserRow: @Composable (Modifier) -> Unit = { m ->
        Row(m, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HoldButton(
                Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(20.dp),
                containerColor = Color(0xFFC62828).copy(alpha = 0.18f),
                pressedColor = Color(0xFFC62828).copy(alpha = 0.45f),
                onPress = {
                    if (pptShortcuts) hid.shortcut(Key.L, Mod.LCTRL)
                    laser = true
                },
                onRelease = {
                    laser = false
                    if (pptShortcuts) hid.shortcut(Key.H, Mod.LCTRL)
                },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(6.dp)) {
                    AutoText("● Laser pointer", maxSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFC62828))
                    AutoText("Hold & point the device at the screen", maxSize = 11.sp)
                }
            }
            Column(Modifier.weight(0.6f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                KeyCap("Pen (Ctrl+P)", Modifier.weight(1f).fillMaxWidth(), fontSize = 13.sp, onRelease = { hid.shortcut(Key.P, Mod.LCTRL) })
                KeyCap("Erase ink (E)", Modifier.weight(1f).fillMaxWidth(), fontSize = 13.sp, onRelease = { hid.shortcut(Key.E) })
            }
        }
    }
    val options: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilterChip(selected = pptShortcuts, onClick = { pptShortcuts = !pptShortcuts }, label = { Text("PowerPoint laser keys") })
            Spacer(Modifier.width(8.dp))
            FilterChip(selected = false, onClick = { calibrate = true }, label = { Text("Calibrate laser") })
            Spacer(Modifier.width(8.dp))
            if (settings.presentationVolumeKeys) {
                Text("Volume keys = slides", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (landscape) {
        Row(Modifier.fillMaxSize().padding(12.dp).navigationBarsPadding(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            prevButton(Modifier.weight(0.8f).fillMaxHeight())
            Column(Modifier.weight(1.3f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PresentationTimer(settings.presentationAlertMinutes, context)
                shortcuts(Modifier.fillMaxWidth().weight(0.8f))
                laserRow(Modifier.fillMaxWidth().weight(1f))
                options()
            }
            nextButton(Modifier.weight(1f).fillMaxHeight())
        }
    } else {
        Column(Modifier.fillMaxSize().padding(12.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PresentationTimer(settings.presentationAlertMinutes, context)
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                prevButton(Modifier.weight(1f).fillMaxHeight())
                nextButton(Modifier.weight(1.6f).fillMaxHeight())
            }
            shortcuts(Modifier.fillMaxWidth().height(56.dp))
            laserRow(Modifier.fillMaxWidth().height(88.dp))
            options()
        }
    }
}

@Composable
private fun RowScope.Shortcut(label: String, key: String, onClick: () -> Unit) {
    HoldButton(Modifier.weight(1f).fillMaxHeight(), onRelease = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 3.dp)) {
            AutoText(label, maxSize = 13.sp)
            AutoText(key, maxSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PresentationTimer(alertMinutes: Int, context: Context) {
    var running by rememberSaveable { mutableStateOf(false) }
    var accumulated by rememberSaveable { mutableLongStateOf(0L) }
    var startedAt by rememberSaveable { mutableLongStateOf(0L) }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var alerted by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(running) {
        while (running) {
            now = SystemClock.elapsedRealtime()
            delay(250)
        }
    }
    val elapsed = accumulated + if (running) now - startedAt else 0L
    val totalSec = elapsed / 1000
    if (alertMinutes > 0 && !alerted && totalSec >= alertMinutes * 60L) {
        alerted = true
        vibrate(context)
    }
    val over = alertMinutes > 0 && totalSec >= alertMinutes * 60L

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "%02d:%02d".format(totalSec / 60, totalSec % 60),
                    fontSize = 34.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                    color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    if (alertMinutes > 0) "Vibrates at $alertMinutes min" else "Timer (set an alert in Settings)",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalIconButton(onClick = {
                if (running) {
                    accumulated += SystemClock.elapsedRealtime() - startedAt
                    running = false
                } else {
                    startedAt = SystemClock.elapsedRealtime()
                    now = startedAt
                    running = true
                }
            }) { Icon(if (running) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, "Start/pause") }
            Spacer(Modifier.width(8.dp))
            FilledTonalIconButton(onClick = {
                running = false; accumulated = 0; alerted = false
            }) { Icon(Icons.Outlined.RestartAlt, "Reset") }
        }
    }
}

@Suppress("DEPRECATION")
private fun vibrate(context: Context) {
    val v: Vibrator? = if (Build.VERSION.SDK_INT >= 31)
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    else context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    v?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400, 200, 400), -1))
}

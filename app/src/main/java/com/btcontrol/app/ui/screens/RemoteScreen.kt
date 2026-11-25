package com.btcontrol.app.ui.screens

import com.btcontrol.app.R
import com.btcontrol.app.t
import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.btcontrol.app.BtControlApp
import com.btcontrol.app.hid.Consumer
import com.btcontrol.app.hid.Key
import com.btcontrol.app.hid.MouseButton
import com.btcontrol.app.hid.SystemKey
import com.btcontrol.app.ui.ScreenActionEntry
import com.btcontrol.app.ui.components.AirMouse
import com.btcontrol.app.ui.components.AirMouseBar
import com.btcontrol.app.ui.components.AutoText
import com.btcontrol.app.ui.components.HoldButton

/**
 * TV-style remote for watching media on the PC: navigation pad with OK, back / home / menu,
 * volume and track rockers, playback keys, fullscreen, sleep. "Pointer" turns the device into a
 * motion remote (like a smart-TV pointer): move it to move the cursor, OK clicks.
 */
@Composable
fun RemoteScreen() {
    val hid = BtControlApp.instance.hid
    var pointer by rememberSaveable { mutableStateOf(false) }
    var confirmSleep by remember { mutableStateOf(false) }
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    AirMouse(pointer)

    if (confirmSleep) {
        AlertDialog(
            onDismissRequest = { confirmSleep = false },
            title = { Text(t(R.string.put_the_computer_to_sleep)) },
            confirmButton = { TextButton(onClick = { hid.systemTap(SystemKey.SLEEP); confirmSleep = false }) { Text(t(R.string.sleep)) } },
            dismissButton = { TextButton(onClick = { confirmSleep = false }) { Text(t(R.string.cancel)) } },
        )
    }

    val topRow: @Composable (Modifier) -> Unit = { m ->
        Row(m, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            RoundKey(Icons.Outlined.PowerSettingsNew, t(R.string.sleep), tint = Color(0xFFC62828)) { confirmSleep = true }
            RoundKey(Icons.Outlined.Fullscreen, t(R.string.fullscreen_f)) { hid.shortcut(Key.F) }
            RoundKey(Icons.AutoMirrored.Outlined.VolumeOff, t(R.string.mute), consumer = Consumer.MUTE)
        }
    }
    val navRow: @Composable (Modifier) -> Unit = { m ->
        Row(m, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            RoundKey(Icons.AutoMirrored.Outlined.ArrowBack, t(R.string.back_esc), key = Key.ESC)
            RoundKey(Icons.Outlined.Home, t(R.string.home_start_menu), key = Key.LEFT_GUI)
            RoundKey(Icons.Outlined.Menu, "Menu", key = Key.APPLICATION)
        }
    }
    val rockers: @Composable (Modifier) -> Unit = { m ->
        Row(m, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Rocker("VOL", Consumer.VOLUME_UP, Consumer.VOLUME_DOWN, Icons.Outlined.Add, Icons.Outlined.Remove)
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PillKey(Icons.Outlined.PlayArrow, t(R.string.play_pause), Consumer.PLAY_PAUSE, Modifier.weight(1.3f), primary = true)
                PillKey(Icons.Outlined.Stop, t(R.string.stop), Consumer.STOP, Modifier.weight(1f))
            }
            Rocker("CH", Consumer.NEXT_TRACK, Consumer.PREV_TRACK, Icons.Outlined.KeyboardArrowUp, Icons.Outlined.KeyboardArrowDown)
        }
    }
    val transport: @Composable (Modifier) -> Unit = { m ->
        Row(m, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillKey(Icons.Outlined.SkipPrevious, t(R.string.previous), Consumer.PREV_TRACK, Modifier.weight(1f))
            PillKey(Icons.Outlined.FastRewind, t(R.string.rewind), Consumer.REWIND, Modifier.weight(1f))
            PillKey(Icons.Outlined.FastForward, t(R.string.forward), Consumer.FAST_FORWARD, Modifier.weight(1f))
            PillKey(Icons.Outlined.SkipNext, t(R.string.next), Consumer.NEXT_TRACK, Modifier.weight(1f))
        }
    }

    if (landscape) {
        Row(Modifier.fillMaxSize().padding(16.dp).navigationBarsPadding(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            BoxWithConstraints(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                NavPad(minOf(maxWidth, maxHeight) * 0.95f, pointer)
            }
            Column(Modifier.weight(1.2f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                topRow(Modifier.fillMaxWidth())
                AirMouseBar(pointer, { pointer = !pointer }, Modifier.fillMaxWidth())
                navRow(Modifier.fillMaxWidth())
                rockers(Modifier.fillMaxWidth().weight(1f))
                transport(Modifier.fillMaxWidth().weight(0.4f))
            }
        }
    } else {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            topRow(Modifier.fillMaxWidth())
            AirMouseBar(pointer, { pointer = !pointer }, Modifier.fillMaxWidth())
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1.25f), contentAlignment = Alignment.Center) {
                NavPad(minOf(maxWidth, maxHeight), pointer)
            }
            navRow(Modifier.fillMaxWidth())
            rockers(Modifier.fillMaxWidth().weight(1f))
            transport(Modifier.fillMaxWidth().weight(0.35f))
        }
    }
}

/** Round navigation pad: arrows around a big OK button (OK = click when the pointer is on). */
@Composable
private fun NavPad(size: Dp, pointer: Boolean) {
    val hid = BtControlApp.instance.hid
    val ring = MaterialTheme.colorScheme.surfaceContainerHigh
    Box(Modifier.size(size)) {
        Canvas(Modifier.fillMaxSize()) { drawCircle(ring) }
        val arrow = size * 0.27f
        val off = size * 0.36f
        @Composable
        fun Arrow(icon: ImageVector, usage: Int, dx: Dp, dy: Dp) {
            HoldButton(
                Modifier.align(Alignment.Center).offset(dx, dy).size(arrow),
                onPress = { hid.keyDown(usage) }, onRelease = { hid.keyUp(usage) },
                shape = CircleShape, containerColor = Color.Transparent,
            ) { Icon(icon, null, Modifier.size(arrow * 0.6f)) }
        }
        Arrow(Icons.Outlined.KeyboardArrowUp, Key.UP, 0.dp, -off)
        Arrow(Icons.Outlined.KeyboardArrowDown, Key.DOWN, 0.dp, off)
        Arrow(Icons.Outlined.KeyboardArrowLeft, Key.LEFT, -off, 0.dp)
        Arrow(Icons.Outlined.KeyboardArrowRight, Key.RIGHT, off, 0.dp)
        HoldButton(
            Modifier.align(Alignment.Center).size(size * 0.38f),
            onPress = { if (pointer) hid.mouseDown(MouseButton.LEFT) else hid.keyDown(Key.ENTER) },
            onRelease = { if (pointer) hid.mouseUp(MouseButton.LEFT) else hid.keyUp(Key.ENTER) },
            shape = CircleShape, containerColor = MaterialTheme.colorScheme.primaryContainer,
        ) { AutoText(if (pointer) t(R.string.click) else "OK", maxSize = 22.sp) }
    }
}

@Composable
private fun RoundKey(
    icon: ImageVector,
    label: String,
    key: Int? = null,
    consumer: Int? = null,
    tint: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
) {
    val hid = BtControlApp.instance.hid
    HoldButton(
        Modifier.size(60.dp),
        onPress = { key?.let { hid.keyDown(it) }; consumer?.let { hid.consumerDown(it) } },
        onRelease = { key?.let { hid.keyUp(it) }; consumer?.let { hid.consumerUp() }; onClick?.invoke() },
        shape = CircleShape,
    ) { Icon(icon, label, tint = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurface else tint) }
}

@Composable
private fun ColumnScope.PillKey(icon: ImageVector, label: String, consumer: Int, modifier: Modifier, primary: Boolean = false) =
    PillKeyImpl(icon, label, consumer, modifier.fillMaxWidth(), primary)

@Composable
private fun RowScope.PillKey(icon: ImageVector, label: String, consumer: Int, modifier: Modifier) =
    PillKeyImpl(icon, label, consumer, modifier.fillMaxHeight(), false)

@Composable
private fun PillKeyImpl(icon: ImageVector, label: String, consumer: Int, modifier: Modifier, primary: Boolean) {
    val hid = BtControlApp.instance.hid
    HoldButton(
        modifier,
        onPress = { hid.consumerDown(consumer) }, onRelease = { hid.consumerUp() },
        shape = RoundedCornerShape(24.dp),
        containerColor = if (primary) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
    ) { Icon(icon, label, Modifier.size(if (primary) 40.dp else 28.dp)) }
}

/** Vertical rocker like a TV remote's volume / channel key. */
@Composable
private fun RowScope.Rocker(label: String, upUsage: Int, downUsage: Int, upIcon: ImageVector, downIcon: ImageVector) {
    val hid = BtControlApp.instance.hid
    Column(
        Modifier.weight(0.8f).fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HoldButton(
            Modifier.fillMaxWidth().weight(1f),
            onPress = { hid.consumerDown(upUsage) }, onRelease = { hid.consumerUp() },
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomStart = 6.dp, bottomEnd = 6.dp),
        ) { Icon(upIcon, "$label up", Modifier.size(30.dp)) }
        AutoText(label, Modifier.padding(vertical = 4.dp), maxSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HoldButton(
            Modifier.fillMaxWidth().weight(1f),
            onPress = { hid.consumerDown(downUsage) }, onRelease = { hid.consumerUp() },
            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 30.dp, bottomEnd = 30.dp),
        ) { Icon(downIcon, "$label down", Modifier.size(30.dp)) }
    }
}

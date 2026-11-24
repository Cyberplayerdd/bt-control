package com.btcontrol.app.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.VolumeDown
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.BrightnessHigh
import androidx.compose.material.icons.outlined.BrightnessLow
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.btcontrol.app.BtControlApp
import com.btcontrol.app.hid.Consumer
import com.btcontrol.app.hid.SystemKey
import com.btcontrol.app.ui.components.AutoText
import com.btcontrol.app.ui.ScreenActionEntry
import com.btcontrol.app.ui.components.HoldButton

@Composable
fun MediaScreen() {
    val app = BtControlApp.instance
    val hid = app.hid
    val settings by app.settings.flow.collectAsStateWithLifecycle()
    val labels = settings.mediaShowLabels
    var confirmSystem by remember { mutableIntStateOf(0) }
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    // "Show names" lives in the top bar with the other options.
    ScreenActionEntry("Show button names", Icons.AutoMirrored.Outlined.Label, checked = labels) {
        app.settings.update { it.copy(mediaShowLabels = !it.mediaShowLabels) }
    }

    if (confirmSystem != 0) {
        AlertDialog(
            onDismissRequest = { confirmSystem = 0 },
            title = { Text(if (confirmSystem == SystemKey.SLEEP) "Put the computer to sleep?" else "Press the computer's power button?") },
            text = { Text(if (confirmSystem == SystemKey.SLEEP) "The connection drops while it sleeps." else "Windows does whatever the power button is set to (often shut down or sleep).") },
            confirmButton = { TextButton(onClick = { hid.systemTap(confirmSystem); confirmSystem = 0 }) { Text("Yes") } },
            dismissButton = { TextButton(onClick = { confirmSystem = 0 }) { Text("Cancel") } },
        )
    }

    val playback: @Composable ColumnScope.() -> Unit = {
        MediaRow(Modifier.weight(1.3f)) {
            CButton(Consumer.PREV_TRACK, Icons.Outlined.SkipPrevious, "Previous", labels)
            CButton(Consumer.PLAY_PAUSE, Icons.Outlined.PlayArrow, "Play / Pause", labels, weight = 1.4f, primary = true)
            CButton(Consumer.NEXT_TRACK, Icons.Outlined.SkipNext, "Next", labels)
        }
        MediaRow(Modifier.weight(1f)) {
            CButton(Consumer.REWIND, Icons.Outlined.FastRewind, "Rewind", labels)
            CButton(Consumer.STOP, Icons.Outlined.Stop, "Stop", labels)
            CButton(Consumer.FAST_FORWARD, Icons.Outlined.FastForward, "Forward", labels)
        }
        MediaRow(Modifier.weight(1.2f)) {
            CButton(Consumer.VOLUME_DOWN, Icons.AutoMirrored.Outlined.VolumeDown, "Volume −", labels)
            CButton(Consumer.MUTE, Icons.AutoMirrored.Outlined.VolumeOff, "Mute", labels)
            CButton(Consumer.VOLUME_UP, Icons.AutoMirrored.Outlined.VolumeUp, "Volume +", labels)
        }
    }
    val others: @Composable ColumnScope.() -> Unit = {
        MediaRow(Modifier.weight(1f)) {
            CButton(Consumer.BRIGHTNESS_DOWN, Icons.Outlined.BrightnessLow, "Brightness −", labels)
            CButton(Consumer.BRIGHTNESS_UP, Icons.Outlined.BrightnessHigh, "Brightness +", labels)
            AButton(Icons.Outlined.Bedtime, "Sleep", labels) { confirmSystem = SystemKey.SLEEP }
            AButton(Icons.Outlined.PowerSettingsNew, "Power", labels, tint = Color(0xFFC62828)) { confirmSystem = SystemKey.POWER }
        }
        MediaRow(Modifier.weight(1f)) {
            CButton(Consumer.AC_BACK, Icons.AutoMirrored.Outlined.ArrowBack, "Back", labels)
            CButton(Consumer.AC_FORWARD, Icons.AutoMirrored.Outlined.ArrowForward, "Forward", labels)
            CButton(Consumer.AC_REFRESH, Icons.Outlined.Refresh, "Refresh", labels)
            CButton(Consumer.AC_HOME, Icons.Outlined.Home, "Home", labels)
        }
        MediaRow(Modifier.weight(1f)) {
            CButton(Consumer.AC_SEARCH, Icons.Outlined.Search, "Search", labels)
            CButton(Consumer.BROWSER, Icons.Outlined.Public, "Browser", labels)
            CButton(Consumer.EMAIL, Icons.Outlined.Mail, "Mail", labels)
            CButton(Consumer.CALCULATOR, Icons.Outlined.Calculate, "Calculator", labels)
            CButton(Consumer.FILE_EXPLORER, Icons.Outlined.Folder, "Files", labels)
        }
    }

    Box(Modifier.fillMaxSize().padding(10.dp).navigationBarsPadding()) {
        if (landscape) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp), content = playback)
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp), content = others)
            }
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                playback()
                others()
            }
        }
    }
}

@Composable
private fun MediaRow(modifier: Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

@Composable
private fun RowScope.CButton(usage: Int, icon: ImageVector, label: String, labels: Boolean, weight: Float = 1f, primary: Boolean = false) {
    val hid = BtControlApp.instance.hid
    HoldButton(
        Modifier.weight(weight).fillMaxHeight(),
        onPress = { hid.consumerDown(usage) },
        onRelease = { hid.consumerUp() },
        containerColor = if (primary) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp),
    ) { IconLabel(icon, label, labels, if (primary) 44.dp else 30.dp) }
}

@Composable
private fun RowScope.AButton(icon: ImageVector, label: String, labels: Boolean, tint: Color = Color.Unspecified, onClick: () -> Unit) {
    HoldButton(Modifier.weight(1f).fillMaxHeight(), onRelease = onClick, shape = RoundedCornerShape(16.dp)) {
        IconLabel(icon, label, labels, 30.dp, tint)
    }
}

@Composable
private fun IconLabel(icon: ImageVector, label: String, showLabel: Boolean, size: Dp, tint: Color = Color.Unspecified) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(4.dp)) {
        Icon(icon, label, Modifier.size(size), tint = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurface else tint)
        if (showLabel) AutoText(label, maxSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

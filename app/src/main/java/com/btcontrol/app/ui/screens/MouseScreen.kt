package com.btcontrol.app.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.outlined.KeyboardReturn
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.KeyboardHide
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Mouse
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewCompact
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.btcontrol.app.BtControlApp
import com.btcontrol.app.data.Settings
import com.btcontrol.app.hid.Key
import com.btcontrol.app.hid.KeyboardLayout
import com.btcontrol.app.hid.Mod
import com.btcontrol.app.hid.MouseButton
import com.btcontrol.app.ui.components.AirMouse
import com.btcontrol.app.ui.components.HoldButton
import com.btcontrol.app.ui.components.KeyCap
import com.btcontrol.app.ui.components.KeyCapture
import com.btcontrol.app.ui.components.KeyboardSizes
import com.btcontrol.app.ui.components.ModifierKey
import com.btcontrol.app.ui.components.ScrollStrip
import com.btcontrol.app.ui.components.SendTextDialog
import com.btcontrol.app.ui.components.SliderRow
import com.btcontrol.app.ui.components.SpecKeyboard
import com.btcontrol.app.ui.components.SwitchRow
import com.btcontrol.app.ui.components.Touchpad
import com.btcontrol.app.ui.components.UsageKey
import com.btcontrol.app.ui.components.rememberKeyCapture

private const val HINT = "1 finger: move · tap: click\n2 fingers: scroll · tap: right click\n3 fingers tap: middle click\ntap then drag: select / drag"

@Composable
fun MouseScreen() {
    val app = BtControlApp.instance
    val hid = app.hid
    val s by app.settings.flow.collectAsStateWithLifecycle()
    val capture = rememberKeyCapture()
    var sendText by remember { mutableStateOf(false) }
    var options by remember { mutableStateOf(false) }
    var airMouse by remember { mutableStateOf(false) }
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val layout = KeyboardLayout.byId(s.keyboardLayout)
    val kb = s.compactKeyboard

    AirMouse(airMouse)
    if (sendText) SendTextDialog { sendText = false }
    if (options) MouseOptionsDialog(s) { options = false }

    val pad: @Composable (Modifier) -> Unit = { m ->
        Box(m) {
            Row(Modifier.fillMaxSize()) {
                Touchpad(Modifier.weight(1f).fillMaxHeight(), hint = if (airMouse) "Air mouse on — move the device" else if (s.showTouchpadHint) HINT else null)
                if (s.showScrollStrip) {
                    Spacer(Modifier.width(6.dp))
                    ScrollStrip(Modifier.fillMaxHeight())
                }
            }
            if (s.touchpadOnly) {
                SmallFloatingActionButton(
                    onClick = { app.settings.update { it.copy(touchpadOnly = false) } },
                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f),
                ) { Icon(Icons.Outlined.CropFree, "Show controls") }
            }
        }
    }

    if (s.touchpadOnly) {
        Column(Modifier.fillMaxSize().padding(8.dp).navigationBarsPadding()) {
            KeyCapture(capture)
            pad(Modifier.fillMaxSize())
        }
        return
    }

    val buttons: @Composable (Modifier) -> Unit = { m ->
        Row(m, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            HoldButton(Modifier.weight(2f).fillMaxHeight(), onPress = { hid.mouseDown(MouseButton.LEFT) }, onRelease = { hid.mouseUp(MouseButton.LEFT) }) {}
            HoldButton(Modifier.weight(0.8f).fillMaxHeight(), onPress = { hid.mouseDown(MouseButton.MIDDLE) }, onRelease = { hid.mouseUp(MouseButton.MIDDLE) }) {}
            HoldButton(Modifier.weight(2f).fillMaxHeight(), onPress = { hid.mouseDown(MouseButton.RIGHT) }, onRelease = { hid.mouseUp(MouseButton.RIGHT) }) {}
        }
    }

    val keyboard: @Composable (Modifier) -> Unit = { m ->
        when (kb) {
            "off" -> Unit
            "row" -> QuickKeysRow(m)
            else -> SpecKeyboard(KeyboardSizes.rows(kb, layout.iso), layout, m, gap = 3.dp, maxFont = 16f)
        }
    }

    val toolbar: @Composable () -> Unit = {
        // Evenly spread so it fits any width (phones in portrait included).
        val b = Modifier.size(42.dp)
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledIconToggleButton(checked = imeVisible, onCheckedChange = { if (imeVisible) capture.hide() else capture.show() }, modifier = b) {
                Icon(if (imeVisible) Icons.Outlined.KeyboardHide else Icons.Outlined.Keyboard, "Phone keyboard")
            }
            KeyboardSizeButton(kb, b) { v -> app.settings.update { it.copy(compactKeyboard = v) } }
            FilledTonalIconButton(onClick = { sendText = true }, modifier = b) { Icon(Icons.AutoMirrored.Outlined.Send, "Send text") }
            FilledIconToggleButton(checked = airMouse, onCheckedChange = { airMouse = it }, modifier = b) { Icon(Icons.Outlined.NearMe, "Air mouse") }
            FilledIconToggleButton(checked = s.showMouseButtons, onCheckedChange = { v -> app.settings.update { it.copy(showMouseButtons = v) } }, modifier = b) {
                Icon(Icons.Outlined.Mouse, "Mouse buttons")
            }
            FilledTonalIconButton(onClick = { app.settings.update { it.copy(touchpadOnly = true) } }, modifier = b) { Icon(Icons.Outlined.CropFree, "Touchpad only") }
            FilledTonalIconButton(onClick = { options = true }, modifier = b) { Icon(Icons.Outlined.Tune, "Touchpad options") }
            LayoutChip(compact = true)
        }
    }

    val kbHeight = when (kb) {
        "off" -> 0.dp
        "row" -> 46.dp
        "40" -> 184.dp
        "60", "65" -> 220.dp
        else -> 252.dp
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 10.dp).padding(top = 6.dp).let { if (!imeVisible) it.navigationBarsPadding() else it }) {
        KeyCapture(capture)
        if (landscape && kb != "off" && kb != "row") {
            // Side by side: touchpad + buttons on the left, keyboard on the right.
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(0.42f).fillMaxHeight()) {
                    pad(Modifier.weight(1f).fillMaxWidth())
                    if (s.showMouseButtons) buttons(Modifier.padding(top = 6.dp).fillMaxWidth().height(52.dp))
                }
                keyboard(Modifier.weight(0.58f).fillMaxHeight())
            }
        } else {
            if (kb == "row") keyboard(Modifier.padding(bottom = 6.dp))
            pad(Modifier.weight(1f).fillMaxWidth())
            if (s.showMouseButtons) buttons(Modifier.padding(top = 6.dp).fillMaxWidth().height(if (landscape) 44.dp else 56.dp))
            if (kb != "off" && kb != "row") {
                keyboard(Modifier.padding(top = 6.dp).fillMaxWidth().heightIn(max = kbHeight).height(kbHeight))
            }
        }
        toolbar()
    }
}

@Composable
private fun KeyboardSizeButton(current: String, modifier: Modifier = Modifier, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        FilledIconToggleButton(checked = current != "off", onCheckedChange = { open = true }, modifier = modifier) {
            Icon(Icons.Outlined.ViewCompact, "Built-in keyboard")
        }
        DropdownMenu(open, onDismissRequest = { open = false }) {
            Text("Built-in keyboard", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            val options = listOf("off" to "Off", "row" to "Keys row (Esc, Ctrl, arrows…)") + KeyboardSizes.ALL.take(4)
            for ((v, label) in options) {
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = { onSelect(v); open = false },
                    trailingIcon = { if (v == current) Text("✓") },
                )
            }
        }
    }
}

@Composable
private fun MouseOptionsDialog(s: Settings, onDismiss: () -> Unit) {
    val app = BtControlApp.instance
    var calibrate by remember { mutableStateOf(false) }
    if (calibrate) com.btcontrol.app.ui.components.CalibrateAirMouseDialog { calibrate = false }
    fun set(b: (Settings) -> Settings) = app.settings.update(b)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Touchpad") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                SliderRow("Sensitivity", s.pointerSpeed, 0.2f..5f, { "%.1f×".format(it) }) { v -> set { it.copy(pointerSpeed = v) } }
                SliderRow("Acceleration", s.pointerAcceleration, 0f..3f, { "%.1f".format(it) }) { v -> set { it.copy(pointerAcceleration = v) } }
                SliderRow("Scroll amount", s.scrollSpeed, 0.2f..5f, { "%.1f×".format(it) }) { v -> set { it.copy(scrollSpeed = v) } }
                SliderRow("Air mouse speed", s.airMouseSpeed, 0.2f..4f, { "%.1f×".format(it) }) { v -> set { it.copy(airMouseSpeed = v) } }
                TextButton(onClick = { calibrate = true }) { Text("Calibrate air mouse") }
                SwitchRow("Natural scrolling", s.naturalScroll) { v -> set { it.copy(naturalScroll = v) } }
                SwitchRow("Tap to click", s.tapToClick) { v -> set { it.copy(tapToClick = v) } }
                SwitchRow("Show help text", s.showTouchpadHint) { v -> set { it.copy(showTouchpadHint = v) } }
                SwitchRow("Scroll strip", s.showScrollStrip) { v -> set { it.copy(showScrollStrip = v) } }
                SwitchRow("Mouse buttons", s.showMouseButtons) { v -> set { it.copy(showMouseButtons = v) } }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

/** Compact chip that shows / switches the PC keyboard layout used for typing. */
@Composable
fun LayoutChip(compact: Boolean = false) {
    val app = BtControlApp.instance
    val settings by app.settings.flow.collectAsStateWithLifecycle()
    var open by remember { mutableStateOf(false) }
    Box {
        if (compact) {
            FilledTonalIconButton(onClick = { open = true }, modifier = Modifier.size(42.dp)) {
                Text(settings.keyboardLayout.uppercase(), style = MaterialTheme.typography.labelLarge)
            }
        } else FilterChip(
            selected = false,
            onClick = { open = true },
            label = { Text(settings.keyboardLayout.uppercase()) },
            leadingIcon = { Icon(Icons.Outlined.Language, null, Modifier.size(18.dp)) },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Text("Keyboard layout of the PC", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            for (l in KeyboardLayout.ALL) {
                DropdownMenuItem(
                    text = { Text(l.name) },
                    onClick = { app.settings.update { it.copy(keyboardLayout = l.id) }; open = false },
                    trailingIcon = { if (l.id == settings.keyboardLayout) Text("✓") },
                )
            }
        }
    }
}

/** Horizontally scrolling row of keys a phone keyboard doesn't have. */
@Composable
fun QuickKeysRow(modifier: Modifier = Modifier) {
    val keyMod = Modifier.width(52.dp).fillMaxHeight()
    Row(
        modifier.fillMaxWidth().height(46.dp).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        UsageKey(Key.ESC, "Esc", keyMod, fontSize = 14.sp)
        UsageKey(Key.TAB, "Tab", keyMod, fontSize = 14.sp)
        ModifierKey(Key.LEFT_CTRL, "Ctrl", keyMod)
        ModifierKey(Key.LEFT_ALT, "Alt", keyMod)
        ModifierKey(Key.LEFT_SHIFT, "Shift", keyMod)
        ModifierKey(Key.LEFT_GUI, "Win", keyMod)
        UsageKey(Key.LEFT, "←", keyMod)
        UsageKey(Key.UP, "↑", keyMod)
        UsageKey(Key.DOWN, "↓", keyMod)
        UsageKey(Key.RIGHT, "→", keyMod)
        UsageKey(Key.BACKSPACE, "Bksp", keyMod, icon = Icons.AutoMirrored.Outlined.Backspace)
        UsageKey(Key.ENTER, "Enter", keyMod, icon = Icons.AutoMirrored.Outlined.KeyboardReturn)
        UsageKey(Key.DELETE, "Del", keyMod, fontSize = 14.sp)
        UsageKey(Key.HOME, "Home", keyMod, fontSize = 13.sp)
        UsageKey(Key.END, "End", keyMod, fontSize = 13.sp)
        UsageKey(Key.PAGE_UP, "PgUp", keyMod, fontSize = 13.sp)
        UsageKey(Key.PAGE_DOWN, "PgDn", keyMod, fontSize = 13.sp)
        for (i in 0 until 12) UsageKey(Key.F1 + i, "F${i + 1}", keyMod, fontSize = 14.sp)
        UsageKey(Key.PRINT_SCREEN, "PrtSc", keyMod, fontSize = 12.sp)
        UsageKey(Key.INSERT, "Ins", keyMod, fontSize = 14.sp)
        val hid = BtControlApp.instance.hid
        KeyCap("Ctrl+C", Modifier.width(64.dp).fillMaxHeight(), fontSize = 13.sp, onRelease = { hid.shortcut(Key.C, Mod.LCTRL) })
        KeyCap("Ctrl+V", Modifier.width(64.dp).fillMaxHeight(), fontSize = 13.sp, onRelease = { hid.shortcut(Key.V, Mod.LCTRL) })
        KeyCap("Ctrl+Z", Modifier.width(64.dp).fillMaxHeight(), fontSize = 13.sp, onRelease = { hid.shortcut(Key.Z, Mod.LCTRL) })
    }
}

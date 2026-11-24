package com.btcontrol.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.btcontrol.app.BtControlApp
import com.btcontrol.app.bt.HidService
import com.btcontrol.app.data.FingerMode
import com.btcontrol.app.data.PenButtonAction
import com.btcontrol.app.data.PresentationKeys
import com.btcontrol.app.data.ThemeMode
import com.btcontrol.app.data.UnknownCharMode
import com.btcontrol.app.hid.HidDescriptor
import com.btcontrol.app.hid.KeyboardLayout
import com.btcontrol.app.ui.components.ChoiceRow
import com.btcontrol.app.ui.components.SectionHeader
import com.btcontrol.app.ui.components.SliderRow
import com.btcontrol.app.ui.components.SwitchRow
import androidx.compose.ui.platform.LocalContext
import kotlin.math.roundToInt

@Composable
fun SettingsScreen() {
    val app = BtControlApp.instance
    val s by app.settings.flow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    fun set(block: (com.btcontrol.app.data.Settings) -> com.btcontrol.app.data.Settings) = app.settings.update(block)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 24.dp)) {
        SectionHeader("Computer keyboard")
        ChoiceRow(
            "Keyboard layout of the computer", s.keyboardLayout,
            KeyboardLayout.ALL.map { it.id to it.name },
            subtitle = "Must match the layout selected on the PC, otherwise typed text comes out wrong.",
        ) { v -> set { it.copy(keyboardLayout = v) } }
        ChoiceRow(
            "Characters missing from that layout", s.unknownCharMode,
            listOf(
                UnknownCharMode.WINDOWS_ALT_CODES to "Windows Alt codes (Alt + numpad)",
                UnknownCharMode.WINDOWS_HEX to "Windows Alt + hex (needs EnableHexNumpad)",
                UnknownCharMode.LINUX_CTRL_SHIFT_U to "Linux Ctrl+Shift+U",
                UnknownCharMode.SKIP to "Skip them",
            ),
        ) { v -> set { it.copy(unknownCharMode = v) } }
        SliderRow("Delay between key reports", s.typingDelayMs.toFloat(), 0f..30f, { "${it.roundToInt()} ms" }, steps = 29,
            subtitle = "Increase if some characters are lost when sending long texts.") { v -> set { it.copy(typingDelayMs = v.roundToInt()) } }
        ChoiceRow(
            "Computer keyboard size", s.keyboardSize, com.btcontrol.app.ui.components.KeyboardSizes.ALL,
        ) { v -> set { it.copy(keyboardSize = v) } }
        ChoiceRow(
            "Built-in keyboard on the mouse tab", s.compactKeyboard,
            listOf("off" to "Off", "row" to "Keys row") + com.btcontrol.app.ui.components.KeyboardSizes.ALL.take(4),
        ) { v -> set { it.copy(compactKeyboard = v) } }

        SectionHeader("Mouse & touchpad")
        SliderRow("Pointer speed", s.pointerSpeed, 0.2f..4f, { "%.1f×".format(it) }) { v -> set { it.copy(pointerSpeed = v) } }
        SliderRow("Acceleration", s.pointerAcceleration, 0f..3f, { "%.1f".format(it) }) { v -> set { it.copy(pointerAcceleration = v) } }
        SliderRow("Scroll speed", s.scrollSpeed, 0.2f..4f, { "%.1f×".format(it) }) { v -> set { it.copy(scrollSpeed = v) } }
        SwitchRow("Natural scrolling", s.naturalScroll, "Content follows your fingers, like on the phone") { v -> set { it.copy(naturalScroll = v) } }
        SwitchRow("Tap to click", s.tapToClick) { v -> set { it.copy(tapToClick = v) } }
        SwitchRow("Show scroll strip", s.showScrollStrip) { v -> set { it.copy(showScrollStrip = v) } }
        SliderRow("Air mouse / laser speed", s.airMouseSpeed, 0.2f..4f, { "%.1f×".format(it) }) { v -> set { it.copy(airMouseSpeed = v) } }

        SectionHeader("Presentation")
        ChoiceRow(
            "Next / previous keys", s.presentationKeys,
            listOf(
                PresentationKeys.ARROWS to "→ / ←  (PowerPoint, Google Slides, Keynote)",
                PresentationKeys.PAGE_UP_DOWN to "Page Down / Page Up  (most clickers)",
                PresentationKeys.SPACE_BACKSPACE to "Space / Backspace",
            ),
        ) { v -> set { it.copy(presentationKeys = v) } }
        SwitchRow("Volume buttons change slides", s.presentationVolumeKeys) { v -> set { it.copy(presentationVolumeKeys = v) } }
        SliderRow("Vibrate after", s.presentationAlertMinutes.toFloat(), 0f..90f, { if (it < 1f) "off" else "${it.roundToInt()} min" }, steps = 17) { v ->
            set { it.copy(presentationAlertMinutes = v.roundToInt()) }
        }

        SectionHeader("Drawing tablet")
        ChoiceRow(
            "Pen area shape (match your PC screen)", s.tabletAspect,
            listOf("16:9" to "16:9 (most monitors)", "16:10" to "16:10", "21:9" to "21:9 ultrawide", "32:9" to "32:9 super ultrawide",
                "3:2" to "3:2 (Surface)", "4:3" to "4:3", "fill" to "Whole tablet screen (biggest)"),
        ) { v -> set { it.copy(tabletAspect = v) } }
        SliderRow("Pressure curve", s.pressureCurve, 0.3f..3f, {
            when {
                it < 0.8f -> "soft (%.1f)".format(it)
                it > 1.25f -> "firm (%.1f)".format(it)
                else -> "linear (%.1f)".format(it)
            }
        }, subtitle = "Soft = light strokes get thick quickly. Firm = you need to press harder.") { v -> set { it.copy(pressureCurve = v) } }
        SliderRow("Minimum pressure", s.minPressure, 0f..0.5f, { "${(it * 100).roundToInt()} %" }) { v -> set { it.copy(minPressure = v) } }
        ChoiceRow(
            "Pen side button", s.penButtonAction,
            listOf(PenButtonAction.RIGHT_CLICK to "Barrel button (right click)", PenButtonAction.ERASER to "Eraser while held", PenButtonAction.NONE to "Nothing"),
        ) { v -> set { it.copy(penButtonAction = v) } }
        ChoiceRow(
            "Fingers on the pen area", s.fingerMode,
            listOf(
                FingerMode.GESTURES to "Gestures: 2-finger scroll / pinch zoom, 2-finger tap undo, 3-finger tap redo",
                FingerMode.IGNORE to "Ignore (palm rejection)",
                FingerMode.DRAW to "Draw with a finger (no stylus needed)",
                FingerMode.HOVER to "Move the cursor with a finger, tap to click",
            ),
        ) { v -> set { it.copy(fingerMode = v) } }
        SwitchRow("Send pen tilt", s.sendTilt) { v -> set { it.copy(sendTilt = v) } }
        SwitchRow("Show ink preview on the tablet", s.showInkTrail) { v -> set { it.copy(showInkTrail = v) } }
        SwitchRow("Show shortcut toolbar", s.showTabletToolbar) { v -> set { it.copy(showTabletToolbar = v) } }

        SectionHeader("Multimedia")
        SwitchRow("Show button names", s.mediaShowLabels) { v -> set { it.copy(mediaShowLabels = v) } }

        SectionHeader("Gamepad")
        SwitchRow("Tilt to steer (left stick)", s.gamepadTiltSteering) { v -> set { it.copy(gamepadTiltSteering = v) } }
        SliderRow("Tilt sensitivity", s.gamepadTiltSensitivity, 0.3f..3f, { "%.1f×".format(it) }) { v -> set { it.copy(gamepadTiltSensitivity = v) } }
        SwitchRow("Floating sticks", s.gamepadFloatingSticks, "Stick centre is wherever your thumb lands") { v -> set { it.copy(gamepadFloatingSticks = v) } }

        SectionHeader("Rotation")
        Text(
            "Each tab follows the device's rotation sensor by default (even when Android's auto-rotate is off). " +
                "Use the rotation button in the top bar (or the notch menu) to lock a tab in landscape or portrait.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        androidx.compose.material3.TextButton(onClick = { set { it.copy(orientations = emptyMap()) } }, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text("Reset all tabs to default rotation")
        }

        SectionHeader("General")
        ChoiceRow("Theme", s.theme, listOf(ThemeMode.SYSTEM to "System", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark")) { v -> set { it.copy(theme = v) } }
        SwitchRow("Dynamic colours", s.dynamicColor) { v -> set { it.copy(dynamicColor = v) } }
        SwitchRow("Vibrate on key press", s.haptics) { v -> set { it.copy(haptics = v) } }
        SwitchRow("Keep screen on", s.keepScreenOn) { v -> set { it.copy(keepScreenOn = v) } }
        SwitchRow("Stay connected in background", s.keepInBackground, "Shows a notification while active") { v ->
            set { it.copy(keepInBackground = v) }
            if (v) HidService.start(context) else HidService.stop(context)
        }
        SwitchRow("Reconnect to the last computer on start", s.autoConnect) { v -> set { it.copy(autoConnect = v) } }
        SliderRow(
            "Report interval", s.reportIntervalMs.toFloat(), 4f..20f, { "${it.roundToInt()} ms (${(1000f / it).roundToInt()} Hz)" }, steps = 15,
            subtitle = "Mouse, pen and gamepad update rate. Lower = smoother but needs a good Bluetooth link.",
        ) { v -> set { it.copy(reportIntervalMs = v.roundToInt()) } }

        SectionHeader("About")
        Text(
            "BT Control 1.0 — Bluetooth HID keyboard, mouse, media keys, gamepad and pen digitizer.\n" +
                "HID report descriptor: ${HidDescriptor.DESCRIPTOR.size} bytes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

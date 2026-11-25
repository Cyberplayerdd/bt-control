package com.btcontrol.app.ui.screens

import com.btcontrol.app.R
import com.btcontrol.app.t
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.PanTool
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.btcontrol.app.BtControlApp
import com.btcontrol.app.data.FingerMode
import com.btcontrol.app.hid.Key
import com.btcontrol.app.hid.Mod
import com.btcontrol.app.ui.NotchEntry
import com.btcontrol.app.ui.components.AutoText
import com.btcontrol.app.ui.components.HoldButton

private val ASPECTS = listOf("fill", "16:9", "16:10", "21:9", "32:9", "3:2", "4:3")

/**
 * Drawing tablet: the pen area maps to the PC screen (absolute positioning, like a Wacom tablet)
 * with pressure, tilt, hover, side button and eraser — Windows Ink compatible. By default the
 * whole screen is the pen area; options live in the notch at the top.
 */
@Composable
fun TabletScreen() {
    val app = BtControlApp.instance
    val hid = app.hid
    val settings by app.settings.flow.collectAsStateWithLifecycle()
    var eraser by rememberSaveable { mutableStateOf(false) }
    val toolbar = settings.showTabletToolbar

    NotchEntry(t(R.string.pen_area_x, if (settings.tabletAspect == "fill") t(R.string.whole_screen) else settings.tabletAspect)) {
        val i = ASPECTS.indexOf(app.settings.value.tabletAspect)
        app.settings.update { it.copy(tabletAspect = ASPECTS[(i + 1) % ASPECTS.size]) }
    }
    NotchEntry(if (toolbar) t(R.string.hide_shortcut_toolbar) else t(R.string.show_shortcut_toolbar)) {
        app.settings.update { it.copy(showTabletToolbar = !it.showTabletToolbar) }
    }
    NotchEntry(if (eraser) t(R.string.eraser_mode_on) else t(R.string.eraser_mode_off)) { eraser = !eraser }
    NotchEntry(
        t(R.string.fingers_x, when (settings.fingerMode) {
            FingerMode.DRAW -> t(R.string.finger_draw)
            FingerMode.HOVER -> t(R.string.move_cursor_tap_click)
            FingerMode.GESTURES -> t(R.string.gestures_only)
            FingerMode.IGNORE -> t(R.string.finger_ignored)
        })
    ) {
        val order = listOf(FingerMode.GESTURES, FingerMode.DRAW, FingerMode.HOVER, FingerMode.IGNORE)
        val i = order.indexOf(app.settings.value.fingerMode)
        app.settings.update { it.copy(fingerMode = order[(i + 1) % order.size]) }
    }

    val cs = MaterialTheme.colorScheme
    val colors = PenInputView.Colors(
        background = cs.background.toArgb(),
        area = cs.surfaceContainer.toArgb(),
        border = cs.primary.toArgb(),
        ink = cs.primary.toArgb(),
        text = cs.onSurfaceVariant.toArgb(),
    )

    Row(Modifier.fillMaxSize().background(cs.background)) {
        if (toolbar) {
            Column(
                Modifier.width(64.dp).fillMaxHeight().padding(start = 4.dp, top = 24.dp, bottom = 4.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ToolKey(t(R.string.undo), Icons.AutoMirrored.Outlined.Undo, onClick = { hid.shortcut(Key.Z, Mod.LCTRL) })
                ToolKey(t(R.string.redo), Icons.AutoMirrored.Outlined.Redo, onClick = { hid.shortcut(Key.Z, Mod.LCTRL or Mod.LSHIFT) })
                ToolToggle(if (eraser) t(R.string.eraser) else t(R.string.pen), eraser) { eraser = !eraser }
                ToolKey(t(R.string.pan), Icons.Outlined.PanTool, hold = Key.SPACE)
                ToolKey("Ctrl", hold = Key.LEFT_CTRL)
                ToolKey("Shift", hold = Key.LEFT_SHIFT)
                ToolKey("Alt", hold = Key.LEFT_ALT)
                ToolKey(t(R.string.size), text = "[", onClick = { hid.tap(Key.LEFT_BRACKET) })
                ToolKey(t(R.string.size_2), text = "]", onClick = { hid.tap(Key.RIGHT_BRACKET) })
                ToolKey(t(R.string.zoom_2), Icons.Outlined.ZoomIn, onClick = { hid.tap(Key.KP_PLUS, Mod.LCTRL) })
                ToolKey(t(R.string.zoom), Icons.Outlined.ZoomOut, onClick = { hid.tap(Key.KP_MINUS, Mod.LCTRL) })
            }
        }
        Box(Modifier.weight(1f).fillMaxHeight()) {
            AndroidView(
                factory = { ctx -> PenInputView(ctx) },
                update = { v ->
                    v.settings = settings
                    v.eraserMode = eraser
                    v.colors = colors
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ColumnScope.ToolKey(
    label: String,
    icon: ImageVector? = null,
    text: String? = null,
    hold: Int? = null,
    onClick: (() -> Unit)? = null,
) {
    val hid = BtControlApp.instance.hid
    HoldButton(
        Modifier.fillMaxWidth().height(50.dp),
        onPress = { if (hold != null) hid.keyDown(hold) },
        onRelease = { if (hold != null) hid.keyUp(hold) else onClick?.invoke() },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            when {
                icon != null -> Icon(icon, label)
                text != null -> AutoText(text, maxSize = 18.sp)
            }
            AutoText(label, maxSize = 10.sp)
        }
    }
}

@Composable
private fun ColumnScope.ToolToggle(label: String, on: Boolean, onClick: () -> Unit) {
    HoldButton(Modifier.fillMaxWidth().height(50.dp), onRelease = onClick, active = on) {
        AutoText(label, maxSize = 12.sp)
    }
}

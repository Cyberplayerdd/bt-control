package com.btcontrol.app.ui.components

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.btcontrol.app.BtControlApp
import com.btcontrol.app.hid.Key
import com.btcontrol.app.hid.Latch

/** Returns a function that performs a short haptic tick (if enabled in settings). */
@Composable
fun rememberHaptic(): () -> Unit {
    val view = LocalView.current
    val settings by BtControlApp.instance.settings.flow.collectAsStateWithLifecycle()
    val enabled by rememberUpdatedState(settings.haptics)
    return remember(view) {
        { if (enabled) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }
    }
}

/**
 * A button that reports finger down / finger up separately (so keys can be held), works with
 * multi-touch (each finger on a different button) and keeps tracking a finger that slides
 * off the button until it is lifted.
 */
@Composable
fun HoldButton(
    modifier: Modifier = Modifier,
    onPress: () -> Unit = {},
    onRelease: () -> Unit = {},
    active: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    activeColor: Color = MaterialTheme.colorScheme.primaryContainer,
    pressedColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
    shape: Shape = RoundedCornerShape(12.dp),
    haptic: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val press by rememberUpdatedState(onPress)
    val release by rememberUpdatedState(onRelease)
    val tick = rememberHaptic()
    val bg = when {
        pressed -> pressedColor
        active -> activeColor
        else -> containerColor
    }
    Box(
        modifier
            .clip(shape)
            .background(bg)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    pressed = true
                    if (haptic) tick()
                    press()
                    try {
                        while (true) {
                            val ev = awaitPointerEvent()
                            val c = ev.changes.firstOrNull { it.id == down.id } ?: break
                            c.consume()
                            if (!c.pressed) break
                        }
                    } finally {
                        // Also runs if the button leaves the screen while held.
                        pressed = false
                        release()
                    }
                }
            },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** A labelled key: big main label, optional small secondary label (e.g. shifted character). */
@Composable
fun KeyCap(
    label: String,
    modifier: Modifier = Modifier,
    sub: String? = null,
    icon: ImageVector? = null,
    active: Boolean = false,
    fontSize: TextUnit = 16.sp,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    alt: String? = null,
    onPress: () -> Unit = {},
    onRelease: () -> Unit = {},
) {
    HoldButton(modifier, onPress, onRelease, active = active, containerColor = containerColor, shape = RoundedCornerShape(8.dp)) {
        if (sub != null) {
            Text(
                sub,
                fontSize = (fontSize.value * 0.7f).sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.TopStart).padding(start = 4.dp, top = 1.dp),
                maxLines = 1,
            )
        }
        if (alt != null) {
            Text(
                alt,
                fontSize = (fontSize.value * 0.65f).sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 4.dp, bottom = 1.dp),
                maxLines = 1,
            )
        }
        if (icon != null) Icon(icon, label)
        else AutoText(
            label,
            maxSize = fontSize,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 3.dp),
        )
    }
}

/** Single-line text that shrinks to fit its box instead of being cropped. */
@Composable
fun AutoText(
    text: String,
    modifier: Modifier = Modifier,
    maxSize: TextUnit = 16.sp,
    minSize: TextUnit = 7.sp,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    maxLines: Int = 1,
) {
    val c = if (color == Color.Unspecified) LocalContentColor.current else color
    BasicText(
        text,
        modifier = modifier,
        style = TextStyle(color = c, fontWeight = fontWeight, textAlign = TextAlign.Center, fontSize = maxSize),
        maxLines = maxLines,
        softWrap = maxLines > 1,
        overflow = TextOverflow.Clip,
        autoSize = TextAutoSize.StepBased(minFontSize = minSize, maxFontSize = maxSize, stepSize = 0.5.sp),
    )
}

/** Key that sends a HID usage: held while pressed. */
@Composable
fun UsageKey(
    usage: Int,
    label: String,
    modifier: Modifier = Modifier,
    sub: String? = null,
    icon: ImageVector? = null,
    fontSize: TextUnit = 16.sp,
    active: Boolean = false,
    alt: String? = null,
) {
    val hid = BtControlApp.instance.hid
    KeyCap(label, modifier, sub, icon, active = active, fontSize = fontSize, alt = alt,
        onPress = { hid.keyDown(usage) }, onRelease = { hid.keyUp(usage) })
}

/** Sticky modifier key (tap = once, tap twice = lock, hold = hold). */
@Composable
fun ModifierKey(usage: Int, label: String, modifier: Modifier = Modifier, fontSize: TextUnit = 14.sp) {
    val hid = BtControlApp.instance.hid
    val latches by hid.latches.collectAsStateWithLifecycle()
    val state = latches[usage] ?: Latch.OFF
    Box(modifier) {
        KeyCap(
            label, Modifier.fillMaxSize(), active = state != Latch.OFF, fontSize = fontSize,
            onPress = { hid.modifierPress(usage) }, onRelease = { hid.modifierRelease(usage) },
        )
        if (state == Latch.LOCKED) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(4.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                    .padding(3.dp)
            )
        }
    }
}

/** Locks the activity orientation while the composable is shown. */
@Composable
fun LockOrientation(orientation: Int) {
    val view = LocalView.current
    DisposableEffect(orientation) {
        val activity = view.context as? Activity
        activity?.requestedOrientation = orientation
        onDispose { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }
}

/** Small caption used above groups of controls. */
@Composable
fun GroupLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 4.dp, bottom = 4.dp, top = 8.dp),
    )
}

/** Outline highlight used to show a key is "on" (e.g. Num Lock LED). */
fun Modifier.ledOutline(on: Boolean, color: Color) =
    if (on) this.border(2.dp, color, RoundedCornerShape(8.dp)) else this

/** Short label of modifier usages for compact UIs. */
fun modifierLabel(usage: Int): String = when (usage) {
    Key.LEFT_CTRL, Key.RIGHT_CTRL -> "Ctrl"
    Key.LEFT_SHIFT, Key.RIGHT_SHIFT -> "Shift"
    Key.LEFT_ALT -> "Alt"
    Key.RIGHT_ALT -> "AltGr"
    Key.LEFT_GUI, Key.RIGHT_GUI -> "Win"
    else -> "?"
}

@Composable
fun CenteredHint(text: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

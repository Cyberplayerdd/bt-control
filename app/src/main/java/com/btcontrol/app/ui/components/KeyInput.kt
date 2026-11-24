package com.btcontrol.app.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.btcontrol.app.BtControlApp
import com.btcontrol.app.hid.AndroidKeyMap
import com.btcontrol.app.hid.Key
import com.btcontrol.app.hid.Mod

/**
 * Invisible view that receives input from the phone's soft keyboard (and hardware keyboards)
 * and forwards it to the computer as HID key presses.
 *
 * The input type is "visible password" so keyboards don't autocorrect or compose words; the
 * composing-text diff below still handles keyboards that compose anyway.
 */
@SuppressLint("ViewConstructor")
class KeyInputView(context: Context) : View(context) {
    private val hid get() = BtControlApp.instance.hid

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun onCheckIsTextEditor() = true

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or
            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or
            EditorInfo.IME_FLAG_NO_FULLSCREEN or
            EditorInfo.IME_ACTION_NONE
        outAttrs.initialSelStart = 0
        outAttrs.initialSelEnd = 0
        return Connection()
    }

    private fun typed(text: String) {
        if (text.isNotEmpty()) hid.typeText(text)
    }

    private fun backspace(n: Int) = repeat(n) { hid.tap(Key.BACKSPACE) }

    private fun metaMods(e: KeyEvent): Int {
        var m = 0
        if (e.isCtrlPressed) m = m or Mod.LCTRL
        if (e.isAltPressed) m = m or Mod.LALT
        if (e.isMetaPressed) m = m or Mod.LGUI
        if (e.isShiftPressed) m = m or Mod.LSHIFT
        return m
    }

    /** Key events coming from the IME (soft keys like Backspace / Enter / arrows). */
    private fun softKey(e: KeyEvent) {
        if (e.action != KeyEvent.ACTION_DOWN) return
        val usage = AndroidKeyMap.fromKeyCode(e.keyCode)
        if (usage != null) {
            if (!Key.isModifier(usage)) hid.tap(usage, metaMods(e))
            return
        }
        val ch = e.unicodeChar
        if (ch != 0) {
            val mods = metaMods(e) and Mod.LSHIFT.inv()
            if (mods != 0 && Character.isLetterOrDigit(ch)) {
                val c = Character.toLowerCase(ch.toChar())
                val stroke = hid.layout.strokesFor(c)?.firstOrNull()
                if (stroke != null) hid.tap(stroke.usage, mods or stroke.mods)
            } else typed(String(Character.toChars(ch)))
        }
    }

    // Hardware keyboard (e.g. a tablet keyboard cover): forward physical keys 1:1.
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.scanCode != 0) {
            val u = AndroidKeyMap.fromScanCode(event.scanCode)
            if (u != null) {
                if (event.repeatCount == 0) hid.keyDown(u)
                return true
            }
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) return super.onKeyDown(keyCode, event)
        softKey(event)
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (event.scanCode != 0) {
            val u = AndroidKeyMap.fromScanCode(event.scanCode)
            if (u != null) {
                hid.keyUp(u)
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    private inner class Connection : BaseInputConnection(this, false) {
        private var composing = ""

        private fun replaceComposing(next: String) {
            val common = composing.commonPrefixWith(next).length
            val del = composing.length - common
            if (del > 0) backspace(del)
            typed(next.substring(common))
        }

        override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
            replaceComposing(text?.toString() ?: "")
            composing = ""
            return true
        }

        override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
            val t = text?.toString() ?: ""
            replaceComposing(t)
            composing = t
            return true
        }

        override fun finishComposingText(): Boolean {
            composing = ""
            return true
        }

        override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
            if (composing.isNotEmpty()) {
                // Keyboards sometimes delete inside the composing region.
                val cut = minOf(beforeLength, composing.length)
                composing = composing.dropLast(cut)
            }
            if (beforeLength > 0) backspace(beforeLength)
            repeat(afterLength) { hid.tap(Key.DELETE) }
            return true
        }

        override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean =
            deleteSurroundingText(beforeLength, afterLength)

        override fun sendKeyEvent(event: KeyEvent): Boolean {
            softKey(event)
            return true
        }

        override fun performEditorAction(actionCode: Int): Boolean {
            hid.tap(Key.ENTER)
            return true
        }

        override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence = ""
        override fun getTextAfterCursor(n: Int, flags: Int): CharSequence = ""
    }
}

/** Lets a screen show / hide the soft keyboard bound to a [KeyInputView]. */
class KeyCaptureController {
    internal var view: KeyInputView? = null

    fun show() {
        val v = view ?: return
        v.requestFocus()
        val imm = v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        v.post { imm.showSoftInput(v, 0) }
    }

    fun hide() {
        val v = view ?: return
        val imm = v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(v.windowToken, 0)
    }
}

@Composable
fun rememberKeyCapture() = remember { KeyCaptureController() }

/** Place once in a screen (1 dp, invisible) to receive keyboard input. */
@Composable
fun KeyCapture(controller: KeyCaptureController, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { ctx -> KeyInputView(ctx).also { controller.view = it } },
        modifier = modifier.size(1.dp),
        onRelease = { if (controller.view === it) controller.view = null },
    )
}

/** Dialog to compose a longer text on the phone and send it in one go. */
@Composable
fun SendTextDialog(onDismiss: () -> Unit) {
    val hid = BtControlApp.instance.hid
    var text by rememberSaveable { mutableStateOf("") }
    var enter by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send text") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    placeholder = { Text("Type or paste text…") },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = enter, onCheckedChange = { enter = it })
                    Text("Press Enter after sending")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                hid.typeText(if (enter) text + "\n" else text)
                text = ""
                onDismiss()
            }) { Text("Send") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

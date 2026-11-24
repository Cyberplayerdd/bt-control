package com.btcontrol.app.hid

import com.btcontrol.app.data.AppSettings
import com.btcontrol.app.data.UnknownCharMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.Charset
import kotlin.math.roundToInt

enum class Latch { OFF, ONCE, LOCKED }

data class PenState(
    val inRange: Boolean = false,
    val tip: Boolean = false,
    val barrel: Boolean = false,
    val invert: Boolean = false,
    val eraser: Boolean = false,
    val x: Int = 0,
    val y: Int = 0,
    val pressure: Int = 0,
    val tiltX: Int = 0,
    val tiltY: Int = 0,
)

/**
 * High-level HID API used by every screen. Keeps the current keyboard / mouse / gamepad state
 * and turns it into reports for [ReportSender].
 */
class HidController(
    private val sender: ReportSender,
    private val settings: AppSettings,
) {
    // ---------------------------------------------------------------- keyboard

    private val pressedKeys = LinkedHashSet<Int>()
    private var heldMods = 0

    private val _leds = MutableStateFlow(0)
    /** Host keyboard LEDs (Num/Caps/Scroll lock) as reported by the computer. */
    val leds: StateFlow<Int> = _leds.asStateFlow()
    @Volatile private var ledsKnown = false

    private val _latches = MutableStateFlow<Map<Int, Latch>>(emptyMap())
    /** Sticky modifier state shown by the on-screen keyboards. */
    val latches: StateFlow<Map<Int, Latch>> = _latches.asStateFlow()
    private val modsPhysicallyDown = HashSet<Int>()
    private val modsUsedWhileDown = HashSet<Int>()

    val layout: KeyboardLayout get() = KeyboardLayout.byId(settings.value.keyboardLayout)

    /**
     * Shortcuts are defined with letter usages named after US QWERTY (Ctrl+Z…). Windows resolves
     * shortcuts through the active layout, so on AZERTY "Ctrl+Z" must be sent on the key that
     * carries the Z — this moves letter keys to their position on the selected PC layout.
     */
    fun resolve(usage: Int): Int {
        if (usage !in Key.A..Key.Z) return usage
        val c = 'a' + (usage - Key.A)
        return layout.strokesFor(c)?.firstOrNull()?.takeIf { it.mods == 0 }?.usage ?: usage
    }

    /** Layout-aware shortcut, e.g. `shortcut(Key.Z, Mod.LCTRL)` for undo. */
    fun shortcut(usage: Int, mods: Int = 0) = tap(resolve(usage), mods)

    fun onHostLeds(value: Int) {
        ledsKnown = true
        _leds.value = value
    }

    private fun keyboardReport(mods: Int, keys: Collection<Int>): ByteArray {
        val r = ByteArray(HidDescriptor.KEYBOARD_LEN)
        r[0] = mods.toByte()
        keys.take(6).forEachIndexed { i, k -> r[2 + i] = k.toByte() }
        return r
    }

    private val _mods = MutableStateFlow(0)
    /** Modifier keys currently down on the host (held or latched) — used to preview what keys will type. */
    val mods: StateFlow<Int> = _mods.asStateFlow()

    private fun sendKeyboard(mods: Int = heldMods, keys: Collection<Int> = pressedKeys, delay: Int = 0) {
        if (_mods.value != heldMods) _mods.value = heldMods
        sender.sendDiscrete(HidDescriptor.ID_KEYBOARD, keyboardReport(mods, keys), delay)
    }

    /** Physical key down (held until [keyUp]). Modifier usages update the modifier byte. */
    @Synchronized
    fun keyDown(usage: Int) {
        if (Key.isModifier(usage)) heldMods = heldMods or Key.modifierBit(usage)
        else {
            if (pressedKeys.size >= 6) return
            pressedKeys.add(usage)
            markModifiersUsed()
        }
        sendKeyboard()
    }

    @Synchronized
    fun keyUp(usage: Int) {
        if (Key.isModifier(usage)) heldMods = heldMods and Key.modifierBit(usage).inv()
        else if (!pressedKeys.remove(usage)) return
        sendKeyboard()
        if (!Key.isModifier(usage)) releaseOneShotModifiers()
    }

    /** Press and release a key, optionally with extra modifiers (e.g. Ctrl+C). */
    @Synchronized
    fun tap(usage: Int, mods: Int = 0) {
        markModifiersUsed()
        if (Key.isModifier(usage)) {
            val bit = Key.modifierBit(usage)
            sendKeyboard(heldMods or mods or bit, pressedKeys)
        } else {
            sendKeyboard(heldMods or mods, pressedKeys + usage)
        }
        sendKeyboard(heldMods, pressedKeys)
        releaseOneShotModifiers()
    }

    @Synchronized
    fun releaseAllKeys() {
        pressedKeys.clear()
        heldMods = 0
        modsPhysicallyDown.clear()
        modsUsedWhileDown.clear()
        _latches.value = emptyMap()
        sendKeyboard(0, emptyList())
    }

    // Sticky modifiers: tap = one-shot, tap twice = locked, hold + other key = classic hold.

    @Synchronized
    fun modifierPress(usage: Int) {
        modsPhysicallyDown += usage
        modsUsedWhileDown -= usage
        if ((_latches.value[usage] ?: Latch.OFF) == Latch.OFF) keyDown(usage)
    }

    @Synchronized
    fun modifierRelease(usage: Int) {
        modsPhysicallyDown -= usage
        val used = modsUsedWhileDown.remove(usage)
        val state = _latches.value[usage] ?: Latch.OFF
        val next = when {
            used -> Latch.OFF
            state == Latch.OFF -> Latch.ONCE
            state == Latch.ONCE -> Latch.LOCKED
            else -> Latch.OFF
        }
        setLatch(usage, next)
        if (next == Latch.OFF) keyUp(usage)
    }

    private fun setLatch(usage: Int, latch: Latch) {
        _latches.value = _latches.value.toMutableMap().apply {
            if (latch == Latch.OFF) remove(usage) else put(usage, latch)
        }
    }

    private fun markModifiersUsed() {
        modsUsedWhileDown.addAll(modsPhysicallyDown)
    }

    private fun releaseOneShotModifiers() {
        val once = _latches.value.filter { it.value == Latch.ONCE && it.key !in modsPhysicallyDown }.keys
        if (once.isEmpty()) return
        for (u in once) {
            setLatch(u, Latch.OFF)
            heldMods = heldMods and Key.modifierBit(u).inv()
        }
        sendKeyboard()
    }

    /** Type a string using the PC keyboard layout selected in the settings. */
    @Synchronized
    fun typeText(text: String) {
        if (text.isEmpty()) return
        val s = settings.value
        val layout = KeyboardLayout.byId(s.keyboardLayout)
        val delay = s.typingDelayMs
        val capsWasOn = ledsKnown && (_leds.value and Led.CAPS_LOCK) != 0 && text.any { it.isLetter() }
        if (capsWasOn) tapRaw(Key.CAPS_LOCK, 0, delay)

        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            i += Character.charCount(cp)
            if (cp == '\r'.code) continue
            val strokes = if (cp <= 0xFFFF) layout.strokesFor(cp.toChar()) else null
            if (strokes != null) {
                for (st in strokes) tapRaw(st.usage, st.mods, delay)
            } else {
                typeUnknown(cp, s.unknownCharMode, layout, delay)
            }
            releaseOneShotModifiers()
        }
        if (capsWasOn) tapRaw(Key.CAPS_LOCK, 0, delay)
    }

    private fun tapRaw(usage: Int, mods: Int, delay: Int) {
        sendKeyboard(heldMods or mods, pressedKeys + usage, delay)
        sendKeyboard(heldMods, pressedKeys, delay)
    }

    private val cp1252: Charset? = runCatching { Charset.forName("windows-1252") }.getOrNull()

    private fun typeUnknown(cp: Int, mode: UnknownCharMode, layout: KeyboardLayout, delay: Int) {
        when (mode) {
            UnknownCharMode.SKIP -> Unit
            UnknownCharMode.WINDOWS_ALT_CODES -> {
                if (cp > 0xFFFF) return
                val ansi = cp1252?.newEncoder()?.takeIf { it.canEncode(cp.toChar()) }?.encode(java.nio.CharBuffer.wrap(charArrayOf(cp.toChar())))
                val digits = if (ansi != null && ansi.remaining() == 1) {
                    "0" + (ansi.get(0).toInt() and 0xFF).toString().padStart(3, '0')
                } else cp.toString()
                withNumLock(delay) {
                    val alt = heldMods or Mod.LALT
                    sendKeyboard(alt, pressedKeys, delay)
                    for (d in digits) {
                        sendKeyboard(alt, pressedKeys + Key.KEYPAD_DIGITS[d - '0'], delay)
                        sendKeyboard(alt, pressedKeys, delay)
                    }
                    sendKeyboard(heldMods, pressedKeys, delay)
                }
            }
            UnknownCharMode.WINDOWS_HEX -> {
                if (cp > 0xFFFF) return
                val hex = Integer.toHexString(cp)
                withNumLock(delay) {
                    val alt = heldMods or Mod.LALT
                    sendKeyboard(alt, pressedKeys, delay)
                    sendKeyboard(alt, pressedKeys + Key.KP_PLUS, delay)
                    sendKeyboard(alt, pressedKeys, delay)
                    for (h in hex) {
                        val usage = if (h.isDigit()) Key.KEYPAD_DIGITS[h - '0']
                        else layout.strokesFor(h)?.firstOrNull()?.usage ?: continue
                        sendKeyboard(alt, pressedKeys + usage, delay)
                        sendKeyboard(alt, pressedKeys, delay)
                    }
                    sendKeyboard(heldMods, pressedKeys, delay)
                }
            }
            UnknownCharMode.LINUX_CTRL_SHIFT_U -> {
                tapRaw(Key.U, Mod.LCTRL or Mod.LSHIFT, delay)
                for (h in Integer.toHexString(cp)) {
                    layout.strokesFor(h)?.forEach { tapRaw(it.usage, it.mods, delay) }
                }
                tapRaw(Key.SPACE, 0, delay)
            }
        }
    }

    private inline fun withNumLock(delay: Int, block: () -> Unit) {
        val toggle = ledsKnown && (_leds.value and Led.NUM_LOCK) == 0
        if (toggle) tapRaw(Key.NUM_LOCK, 0, delay)
        block()
        if (toggle) tapRaw(Key.NUM_LOCK, 0, delay)
    }

    // ---------------------------------------------------------------- mouse

    private var mouseButtons = 0
    private var remX = 0f
    private var remY = 0f
    private var remWheel = 0f
    private var remPan = 0f

    @Synchronized
    fun mouseMove(dx: Float, dy: Float) {
        val tx = remX + dx
        val ty = remY + dy
        val ix = tx.toInt().coerceIn(-32767, 32767)
        val iy = ty.toInt().coerceIn(-32767, 32767)
        remX = tx - ix
        remY = ty - iy
        if (ix != 0 || iy != 0) sender.sendMouse(mouseButtons, ix, iy, 0, 0)
    }

    /** Scroll by (fractional) wheel notches. Positive [vertical] scrolls up, positive [horizontal] right. */
    @Synchronized
    fun scroll(vertical: Float, horizontal: Float = 0f) {
        val tw = remWheel + vertical
        val tp = remPan + horizontal
        val iw = tw.toInt().coerceIn(-127, 127)
        val ip = tp.toInt().coerceIn(-127, 127)
        remWheel = tw - iw
        remPan = tp - ip
        if (iw != 0 || ip != 0) sender.sendMouse(mouseButtons, 0, 0, iw, ip)
    }

    @Synchronized
    fun mouseDown(button: Int) {
        mouseButtons = mouseButtons or button
        sender.sendMouse(mouseButtons, 0, 0, 0, 0)
    }

    @Synchronized
    fun mouseUp(button: Int) {
        mouseButtons = mouseButtons and button.inv()
        sender.sendMouse(mouseButtons, 0, 0, 0, 0)
    }

    fun click(button: Int = MouseButton.LEFT) {
        mouseDown(button)
        mouseUp(button)
    }

    fun isMouseButtonDown(button: Int) = mouseButtons and button != 0

    // ---------------------------------------------------------------- consumer / system

    fun consumerDown(usage: Int) = sender.sendDiscrete(
        HidDescriptor.ID_CONSUMER, byteArrayOf((usage and 0xFF).toByte(), (usage shr 8).toByte())
    )

    fun consumerUp() = sender.sendDiscrete(HidDescriptor.ID_CONSUMER, byteArrayOf(0, 0))

    fun consumerTap(usage: Int) {
        consumerDown(usage)
        consumerUp()
    }

    fun systemTap(bit: Int) {
        sender.sendDiscrete(HidDescriptor.ID_SYSTEM, byteArrayOf(bit.toByte()))
        sender.sendDiscrete(HidDescriptor.ID_SYSTEM, byteArrayOf(0))
    }

    // ---------------------------------------------------------------- gamepad

    private var padButtons = 0
    private var padHat = Hat.CENTER
    private val padAxes = intArrayOf(128, 128, 128, 128, 0, 0) // LX LY RX RY LT RT

    @Synchronized
    fun padButton(index: Int, down: Boolean) {
        val next = if (down) padButtons or (1 shl index) else padButtons and (1 shl index).inv()
        if (next == padButtons) return
        padButtons = next
        sendPad()
    }

    @Synchronized
    fun padHat(value: Int) {
        if (value == padHat) return
        padHat = value
        sendPad()
    }

    /** x, y in -1..1 (y positive = down). */
    @Synchronized
    fun padStick(right: Boolean, x: Float, y: Float) {
        val o = if (right) 2 else 0
        val ax = axis(x)
        val ay = axis(y)
        if (padAxes[o] == ax && padAxes[o + 1] == ay) return
        padAxes[o] = ax
        padAxes[o + 1] = ay
        sendPad()
    }

    /** value 0..1 */
    @Synchronized
    fun padTrigger(right: Boolean, value: Float) {
        val i = if (right) 5 else 4
        val v = (value.coerceIn(0f, 1f) * 255).roundToInt()
        if (padAxes[i] == v) return
        padAxes[i] = v
        // Also expose triggers as digital buttons for games that expect them.
        val bit = 1 shl (if (right) PadButton.RT_BTN else PadButton.LT_BTN)
        padButtons = if (v > 64) padButtons or bit else padButtons and bit.inv()
        sendPad()
    }

    /**
     * Wiimote IR pointer on the Rx / Ry axes (x, y in -1..1, centre = 0). Unlike [padTrigger] it
     * does not touch the trigger buttons.
     */
    @Synchronized
    fun padPointer(x: Float, y: Float) {
        val ax = axis(x)
        val ay = axis(y)
        if (padAxes[4] == ax && padAxes[5] == ay) return
        padAxes[4] = ax
        padAxes[5] = ay
        sendPad()
    }

    @Synchronized
    fun padReset() {
        padButtons = 0
        padHat = Hat.CENTER
        padAxes[0] = 128; padAxes[1] = 128; padAxes[2] = 128; padAxes[3] = 128
        padAxes[4] = 0; padAxes[5] = 0
        sendPad()
    }

    private fun axis(v: Float) = ((v.coerceIn(-1f, 1f) + 1f) * 127.5f).roundToInt().coerceIn(0, 255)

    private fun sendPad() {
        val r = byteArrayOf(
            (padButtons and 0xFF).toByte(), (padButtons shr 8 and 0xFF).toByte(),
            (padHat and 0x0F).toByte(),
            padAxes[0].toByte(), padAxes[1].toByte(), padAxes[2].toByte(), padAxes[3].toByte(),
            padAxes[4].toByte(), padAxes[5].toByte(),
        )
        sender.sendState(HidDescriptor.ID_GAMEPAD, r, padButtons or (padHat shl 16))
    }

    // ---------------------------------------------------------------- pen

    @Volatile var lastPen = PenState()
        private set

    fun pen(s: PenState) {
        lastPen = s
        var bits = 0
        if (s.tip) bits = bits or 0x01
        if (s.barrel) bits = bits or 0x02
        if (s.invert) bits = bits or 0x04
        if (s.eraser) bits = bits or 0x08
        if (s.inRange) bits = bits or 0x10
        val x = s.x.coerceIn(0, HidDescriptor.PEN_MAX_XY)
        val y = s.y.coerceIn(0, HidDescriptor.PEN_MAX_XY)
        val p = s.pressure.coerceIn(0, HidDescriptor.PEN_MAX_PRESSURE)
        val r = byteArrayOf(
            bits.toByte(),
            (x and 0xFF).toByte(), (x shr 8).toByte(),
            (y and 0xFF).toByte(), (y shr 8).toByte(),
            (p and 0xFF).toByte(), (p shr 8).toByte(),
            s.tiltX.coerceIn(-90, 90).toByte(), s.tiltY.coerceIn(-90, 90).toByte(),
        )
        sender.sendState(HidDescriptor.ID_PEN, r, bits)
    }

    fun penOutOfRange() = pen(lastPen.copy(inRange = false, tip = false, barrel = false, invert = false, eraser = false, pressure = 0))

    // ---------------------------------------------------------------- lifecycle

    /** Called on (dis)connection: forget everything that might be "stuck" on the host. */
    @Synchronized
    fun resetState() {
        pressedKeys.clear()
        heldMods = 0
        modsPhysicallyDown.clear()
        modsUsedWhileDown.clear()
        _latches.value = emptyMap()
        mouseButtons = 0
        padButtons = 0
        padHat = Hat.CENTER
        ledsKnown = false
        lastPen = PenState()
        _mods.value = 0
    }

    /** Raw gamepad button set (used by the Wiimote tab for extra buttons such as Shake). */
    fun padButtonsState() = padButtons
}

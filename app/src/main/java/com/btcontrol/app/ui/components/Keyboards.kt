package com.btcontrol.app.ui.components

import com.btcontrol.app.R
import com.btcontrol.app.t
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.outlined.KeyboardReturn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.btcontrol.app.BtControlApp
import com.btcontrol.app.hid.Key
import com.btcontrol.app.hid.KeyboardLayout
import com.btcontrol.app.hid.Latch
import com.btcontrol.app.hid.Led
import com.btcontrol.app.hid.Mod

/** One key of a keyboard spec: HID usage, width in key units, optional Fn-layer usage. */
data class KS(val usage: Int, val w: Float = 1f, val fn: Int? = null)

/** Keyboard sizes, from the fewest keys to the most. */
object KeyboardSizes {
    const val FN = -1
    const val GAP = 0

    val ALL get() = listOf("40" to "40%", "60" to "60%", "65" to "65%", "75" to "75%", "tkl" to "TKL", "full" to t(R.string.full))

    private val NUM_FN = mapOf(
        Key.N1 to Key.F1, Key.N2 to Key.F2, Key.N3 to Key.F3, Key.N4 to Key.F4, Key.N5 to Key.F5,
        Key.N6 to Key.F6, Key.N7 to Key.F7, Key.N8 to Key.F8, Key.N9 to Key.F9, Key.N0 to Key.F10,
        Key.MINUS to Key.F11, Key.EQUAL to Key.F12,
    )
    private val NAV_FN = mapOf(
        Key.I to Key.UP, Key.J to Key.LEFT, Key.K to Key.DOWN, Key.L to Key.RIGHT,
        Key.U to Key.PAGE_UP, Key.O to Key.PAGE_DOWN, Key.Y to Key.HOME, Key.H to Key.END,
        Key.P to Key.PRINT_SCREEN, Key.LEFT_BRACKET to Key.INSERT,
    )

    private fun gap(w: Float) = KS(GAP, w)

    private fun numberRow(escFirst: Boolean): List<KS> = buildList {
        add(if (escFirst) KS(Key.ESC, 1f, Key.GRAVE) else KS(Key.GRAVE))
        for (u in KeyboardLayout.ROW1.drop(1)) add(KS(u, 1f, if (escFirst) NUM_FN[u] else null))
    }

    private fun qRow(iso: Boolean, fn: Map<Int, Int>) =
        listOf(KS(Key.TAB, 1.5f)) + KeyboardLayout.ROW2_ISO.map { KS(it, 1f, fn[it]) } +
            (if (iso) KS(Key.ENTER, 1.5f) else KS(Key.BACKSLASH, 1.5f))

    private fun aRow(iso: Boolean, fn: Map<Int, Int>) =
        listOf(KS(Key.CAPS_LOCK, 1.75f)) + (if (iso) KeyboardLayout.ROW3_ISO else KeyboardLayout.ROW3_ANSI).map { KS(it, 1f, fn[it]) } +
            KS(Key.ENTER, if (iso) 1.25f else 2.25f)

    private fun zRow(iso: Boolean, rightShift: Float) =
        listOf(KS(Key.LEFT_SHIFT, if (iso) 1.25f else 2.25f)) + (if (iso) KeyboardLayout.ROW4_ISO else KeyboardLayout.ROW4_ANSI).map { KS(it) } +
            KS(Key.RIGHT_SHIFT, rightShift)

    private fun fRowTkl() = buildList {
        add(KS(Key.ESC)); add(gap(1f))
        for (i in 0..3) add(KS(Key.F1 + i)); add(gap(0.5f))
        for (i in 4..7) add(KS(Key.F1 + i)); add(gap(0.5f))
        for (i in 8..11) add(KS(Key.F1 + i))
    }

    private fun tklRows(iso: Boolean): List<List<KS>> = listOf(
        fRowTkl() + listOf(gap(0.25f), KS(Key.PRINT_SCREEN), KS(Key.SCROLL_LOCK), KS(Key.PAUSE)),
        numberRow(false) + KS(Key.BACKSPACE, 2f) + listOf(gap(0.25f), KS(Key.INSERT), KS(Key.HOME), KS(Key.PAGE_UP)),
        qRow(iso, emptyMap()) + listOf(gap(0.25f), KS(Key.DELETE), KS(Key.END), KS(Key.PAGE_DOWN)),
        aRow(iso, emptyMap()) + listOf(gap(3.25f)),
        zRow(iso, 2.75f) + listOf(gap(1.25f), KS(Key.UP), gap(1f)),
        listOf(
            KS(Key.LEFT_CTRL, 1.25f), KS(Key.LEFT_GUI, 1.25f), KS(Key.LEFT_ALT, 1.25f), KS(Key.SPACE, 6.25f),
            KS(Key.RIGHT_ALT, 1.25f), KS(Key.RIGHT_GUI, 1.25f), KS(Key.APPLICATION, 1.25f), KS(Key.RIGHT_CTRL, 1.25f),
            gap(0.25f), KS(Key.LEFT), KS(Key.DOWN), KS(Key.RIGHT),
        ),
    )

    fun rows(size: String, iso: Boolean): List<List<KS>> = when (size) {
        "40" -> listOf(
            listOf(KS(Key.TAB, 1f, Key.GRAVE)) +
                KeyboardLayout.ROW2_ANSI.take(10).mapIndexed { i, u -> KS(u, 1f, KeyboardLayout.ROW1[i + 1]) } +
                KS(Key.BACKSPACE, 1f, Key.DELETE),
            listOf(KS(Key.ESC, 1f, Key.F12)) + listOf(
                KS(Key.A, 1f, Key.MINUS), KS(Key.S, 1f, Key.EQUAL), KS(Key.D, 1f, Key.LEFT_BRACKET),
                KS(Key.F, 1f, Key.RIGHT_BRACKET), KS(Key.G, 1f, if (iso) Key.NON_US_HASH else Key.BACKSLASH),
                KS(Key.H, 1f, if (iso) Key.NON_US_BACKSLASH else null), KS(Key.J, 1f, Key.HOME),
                KS(Key.K, 1f, Key.PAGE_UP), KS(Key.L, 1f, Key.PAGE_DOWN), KS(Key.SEMICOLON, 1f, Key.END),
                KS(Key.APOSTROPHE, 1f, Key.INSERT),
            ),
            listOf(KS(Key.LEFT_SHIFT)) +
                KeyboardLayout.ROW4_ANSI.mapIndexed { i, u -> KS(u, 1f, Key.F1 + i) } +
                KS(Key.ENTER, 1f, Key.F11),
            listOf(
                KS(Key.LEFT_CTRL), KS(Key.LEFT_GUI), KS(Key.LEFT_ALT), KS(FN), KS(Key.SPACE, 3f), KS(Key.RIGHT_ALT),
                KS(Key.LEFT, 1f, Key.HOME), KS(Key.UP, 1f, Key.PAGE_UP), KS(Key.DOWN, 1f, Key.PAGE_DOWN), KS(Key.RIGHT, 1f, Key.END),
            ),
        )
        "60" -> listOf(
            numberRow(true) + KS(Key.BACKSPACE, 2f, Key.DELETE),
            qRow(iso, NAV_FN),
            aRow(iso, NAV_FN),
            zRow(iso, 2.75f),
            listOf(
                KS(Key.LEFT_CTRL, 1.25f), KS(Key.LEFT_GUI, 1.25f), KS(Key.LEFT_ALT, 1.25f), KS(Key.SPACE, 6.25f),
                KS(Key.RIGHT_ALT, 1.25f), KS(FN, 1.25f), KS(Key.APPLICATION, 1.25f), KS(Key.RIGHT_CTRL, 1.25f),
            ),
        )
        "65" -> listOf(
            numberRow(true) + KS(Key.BACKSPACE, 2f, Key.DELETE) + KS(Key.DELETE, 1f, Key.INSERT),
            qRow(iso, NAV_FN) + KS(Key.PAGE_UP, 1f, Key.HOME),
            aRow(iso, NAV_FN) + KS(Key.PAGE_DOWN, 1f, Key.END),
            zRow(iso, 1.75f) + KS(Key.UP) + KS(Key.END, 1f, Key.PRINT_SCREEN),
            listOf(
                KS(Key.LEFT_CTRL, 1.25f), KS(Key.LEFT_GUI, 1.25f), KS(Key.LEFT_ALT, 1.25f), KS(Key.SPACE, 6.25f),
                KS(Key.RIGHT_ALT), KS(FN), KS(Key.RIGHT_CTRL), KS(Key.LEFT), KS(Key.DOWN), KS(Key.RIGHT),
            ),
        )
        "75" -> listOf(
            listOf(KS(Key.ESC)) + (0..11).map { KS(Key.F1 + it) } + listOf(KS(Key.PRINT_SCREEN), KS(Key.INSERT), KS(Key.DELETE)),
            numberRow(false) + KS(Key.BACKSPACE, 2f) + KS(Key.HOME),
            qRow(iso, emptyMap()) + KS(Key.PAGE_UP),
            aRow(iso, emptyMap()) + KS(Key.PAGE_DOWN),
            zRow(iso, 1.75f) + KS(Key.UP) + KS(Key.END),
            listOf(
                KS(Key.LEFT_CTRL, 1.25f), KS(Key.LEFT_GUI, 1.25f), KS(Key.LEFT_ALT, 1.25f), KS(Key.SPACE, 6.25f),
                KS(Key.RIGHT_ALT), KS(Key.APPLICATION), KS(Key.RIGHT_CTRL), KS(Key.LEFT), KS(Key.DOWN), KS(Key.RIGHT),
            ),
        )
        "full" -> {
            val t = tklRows(iso)
            val pad = listOf(
                listOf(gap(4f)),
                listOf(KS(Key.NUM_LOCK), KS(Key.KP_SLASH), KS(Key.KP_ASTERISK), KS(Key.KP_MINUS)),
                listOf(KS(Key.KP_7), KS(Key.KP_8), KS(Key.KP_9), KS(Key.KP_PLUS)),
                listOf(KS(Key.KP_4), KS(Key.KP_5), KS(Key.KP_6), KS(Key.KP_PLUS)),
                listOf(KS(Key.KP_1), KS(Key.KP_2), KS(Key.KP_3), KS(Key.KP_ENTER)),
                listOf(KS(Key.KP_0, 2f), KS(Key.KP_DOT), KS(Key.KP_ENTER)),
            )
            t.mapIndexed { i, r -> r + gap(0.25f) + pad[i] }
        }
        else -> tklRows(iso)
    }
}

/** Short caption for a non-character key. */
fun namedKeyLabel(usage: Int): String = when (usage) {
    Key.LEFT -> "←"; Key.RIGHT -> "→"; Key.UP -> "↑"; Key.DOWN -> "↓"
    Key.PAGE_UP -> "PgUp"; Key.PAGE_DOWN -> "PgDn"; Key.DELETE -> "Del"; Key.INSERT -> "Ins"
    Key.PRINT_SCREEN -> "PrtSc"; Key.SCROLL_LOCK -> "ScrLk"; Key.NUM_LOCK -> "Num"; Key.CAPS_LOCK -> "Caps"
    Key.APPLICATION -> "Menu"; Key.SPACE -> ""; Key.ESC -> "Esc"; Key.TAB -> "Tab"
    Key.KP_SLASH -> "/"; Key.KP_ASTERISK -> "*"; Key.KP_MINUS -> "−"; Key.KP_PLUS -> "+"; Key.KP_DOT -> "."
    Key.KP_ENTER -> "Enter"
    in Key.KP_1..Key.KP_9 -> ('1' + (usage - Key.KP_1)).toString()
    Key.KP_0 -> "0"
    Key.LEFT_CTRL, Key.RIGHT_CTRL -> "Ctrl"
    Key.LEFT_SHIFT, Key.RIGHT_SHIFT -> "Shift"
    Key.LEFT_ALT -> "Alt"; Key.RIGHT_ALT -> "AltGr"
    Key.LEFT_GUI, Key.RIGHT_GUI -> "Win"
    else -> Key.NAMES[usage] ?: ""
}

/**
 * Renders a keyboard spec. Character keys are captioned with the PC layout and show — in the
 * accent colour — the character the key will produce with the modifiers currently active
 * (Shift, AltGr, Caps Lock); keys of the Fn layer are shown when Fn is on.
 */
@Composable
fun SpecKeyboard(rows: List<List<KS>>, layout: KeyboardLayout, modifier: Modifier = Modifier, gap: Dp = 4.dp, maxFont: Float = 18f) {
    val hid = BtControlApp.instance.hid
    val mods by hid.mods.collectAsStateWithLifecycle()
    val leds by hid.leds.collectAsStateWithLifecycle()
    var fn by remember { mutableStateOf(Latch.OFF) }
    val fnOn = fn != Latch.OFF
    val onKeyReleased = { if (fn == Latch.ONCE) fn = Latch.OFF }

    Column(modifier) {
        for (row in rows) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                for (k in row) {
                    if (k.usage == KeyboardSizes.GAP) {
                        Spacer(Modifier.weight(k.w))
                        continue
                    }
                    Box(Modifier.weight(k.w).fillMaxHeight().padding(gap / 2)) {
                        when {
                            k.usage == KeyboardSizes.FN -> {
                                KeyCap(
                                    "Fn", Modifier.fillMaxSize(), active = fnOn, fontSize = (maxFont * 0.8f).sp,
                                    onRelease = { fn = when (fn) { Latch.OFF -> Latch.ONCE; Latch.ONCE -> Latch.LOCKED; Latch.LOCKED -> Latch.OFF } },
                                )
                                if (fn == Latch.LOCKED) LockDot(Modifier.align(Alignment.TopEnd))
                            }
                            else -> {
                                val fromFn = fnOn && k.fn != null
                                val usage = if (fromFn) k.fn!! else k.usage
                                SpecKey(usage, layout, mods, leds, fromFn, maxFont, onKeyReleased)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LockDot(modifier: Modifier) {
    Box(
        modifier.padding(4.dp)
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
            .padding(3.dp)
    )
}

@Composable
private fun SpecKey(usage: Int, layout: KeyboardLayout, mods: Int, leds: Int, fromFn: Boolean, maxFont: Float, onReleased: () -> Unit) {
    val hid = BtControlApp.instance.hid
    if (Key.isModifier(usage)) {
        ModifierKey(usage, namedKeyLabel(usage), Modifier.fillMaxSize(), fontSize = (maxFont * 0.75f).sp)
        return
    }
    val led = when (usage) {
        Key.CAPS_LOCK -> leds and Led.CAPS_LOCK != 0
        Key.NUM_LOCK -> leds and Led.NUM_LOCK != 0
        Key.SCROLL_LOCK -> leds and Led.SCROLL_LOCK != 0
        else -> false
    }
    val fnColor = MaterialTheme.colorScheme.tertiary
    val label = layout.label(usage)
    val m = Modifier.fillMaxSize().ledOutline(led, MaterialTheme.colorScheme.primary)
    val press = { hid.keyDown(usage) }
    val release = { hid.keyUp(usage); onReleased() }

    if (label == null || usage == Key.SPACE) {
        val icon = when (usage) {
            Key.BACKSPACE -> Icons.AutoMirrored.Outlined.Backspace
            Key.ENTER, Key.KP_ENTER -> if (usage == Key.ENTER) Icons.AutoMirrored.Outlined.KeyboardReturn else null
            else -> null
        }
        val text = if (usage == Key.SPACE) layout.name.substringBefore(" (") else namedKeyLabel(usage)
        HoldButton(m, press, release, shape = RoundedCornerShape(8.dp)) {
            if (icon != null) Icon(icon, text, tint = if (fromFn) fnColor else MaterialTheme.colorScheme.onSurface)
            else AutoText(
                text, Modifier.padding(horizontal = 3.dp), maxSize = (maxFont * if (text.length > 2) 0.72f else 0.9f).sp,
                color = if (fromFn) fnColor else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium,
            )
        }
        return
    }

    // Character key: work out which glyph the PC will produce right now.
    val n = label.normal
    val s = label.shift
    val a = label.altGr
    val shift = mods and (Mod.LSHIFT or Mod.RSHIFT) != 0
    val altGr = mods and Mod.RALT != 0 || (mods and (Mod.LCTRL or Mod.RCTRL) != 0 && mods and Mod.LALT != 0)
    val letter = n != null && s != null && n.length == 1 && n[0].isLetter() && n.uppercase() == s
    val caps = leds and Led.CAPS_LOCK != 0
    val effShift = if (letter && caps) !shift else shift
    val active: String? = when {
        altGr -> a
        effShift -> s
        else -> n
    }
    val modified = altGr || effShift
    val accent = when {
        fromFn -> fnColor
        modified -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val dim = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

    HoldButton(
        m.alpha(if (active == null) 0.45f else 1f), press, release,
        shape = RoundedCornerShape(8.dp),
        containerColor = if (modified && active != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        else MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        val small = (maxFont * 0.62f).sp
        if (!letter && s != null && active != s && active != null) {
            AutoText(s, Modifier.align(Alignment.TopStart).padding(start = 4.dp, top = 1.dp), maxSize = small, color = dim)
        }
        if (!letter && n != null && active != n && active != null) {
            AutoText(n, Modifier.align(Alignment.BottomStart).padding(start = 4.dp, bottom = 1.dp), maxSize = small, color = dim)
        }
        if (a != null && active != a) {
            AutoText(
                a, Modifier.align(Alignment.BottomEnd).padding(end = 4.dp, bottom = 1.dp), maxSize = small,
                color = if (altGr) dim else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            )
        }
        AutoText(
            active ?: (n ?: s ?: ""),
            Modifier.padding(horizontal = 3.dp),
            maxSize = maxFont.sp,
            color = if (active == null) dim else accent,
            fontWeight = if (modified) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

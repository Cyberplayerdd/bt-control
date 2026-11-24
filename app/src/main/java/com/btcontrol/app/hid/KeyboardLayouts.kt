package com.btcontrol.app.hid

import java.text.Normalizer

/** One key press: modifier bits + key usage. */
data class KeyStroke(val mods: Int, val usage: Int)

/** What is printed on a physical key for a given PC layout (used by the on-screen keyboard). */
data class KeyLabel(val normal: String?, val shift: String?, val altGr: String?)

/**
 * A PC keyboard layout. HID transmits *physical key positions*; the computer turns them into
 * characters using its own layout. To type text we therefore need, for the layout selected on
 * the PC, the key (and modifiers) that produces each character — including AltGr levels and
 * dead keys (accent key followed by a letter).
 *
 * Each layout is described row by row in US-QWERTY position order. Each key spec is a string of
 * up to 4 characters: normal, Shift, AltGr, Shift+AltGr. A space means "nothing". A combining
 * accent (U+0300…U+036F) marks a dead key.
 */
class KeyboardLayout(
    val id: String,
    val name: String,
    val iso: Boolean,
    row1: List<String>,
    row2: List<String>,
    row3: List<String>,
    row4: List<String>,
) {
    private val strokes = HashMap<Char, List<KeyStroke>>()
    private val labels = HashMap<Int, KeyLabel>()

    init {
        val rows = listOf(
            ROW1 to row1,
            (if (iso) ROW2_ISO else ROW2_ANSI) to row2,
            (if (iso) ROW3_ISO else ROW3_ANSI) to row3,
            (if (iso) ROW4_ISO else ROW4_ANSI) to row4,
        )
        val levels = intArrayOf(Mod.NONE, Mod.LSHIFT, Mod.RALT, Mod.RALT or Mod.LSHIFT)
        val dead = ArrayList<Pair<Char, KeyStroke>>()

        for (level in 0..3) {
            for ((usages, specs) in rows) {
                require(usages.size == specs.size) { "Layout $id: row size ${specs.size} != ${usages.size}" }
                for (i in usages.indices) {
                    val spec = specs[i]
                    if (spec.length <= level) continue
                    val c = spec[level]
                    if (c == ' ') continue
                    val stroke = KeyStroke(levels[level], usages[i])
                    if (isCombining(c)) dead += c to stroke
                    else strokes.putIfAbsent(c, listOf(stroke))
                }
            }
        }
        // Labels for the on-screen keyboard
        for ((usages, specs) in rows) {
            for (i in usages.indices) {
                val spec = specs[i]
                fun at(l: Int): String? = spec.getOrNull(l)?.takeIf { it != ' ' }?.let { display(it) }
                labels[usages[i]] = KeyLabel(at(0), at(1), at(2))
            }
        }

        strokes.putIfAbsent(' ', listOf(KeyStroke(0, Key.SPACE)))
        strokes.putIfAbsent('\n', listOf(KeyStroke(0, Key.ENTER)))
        strokes.putIfAbsent('\t', listOf(KeyStroke(0, Key.TAB)))
        strokes.putIfAbsent(' ', listOf(KeyStroke(0, Key.SPACE)))

        // Dead keys: the accent alone (accent + space) and accented letters (accent + letter).
        val space = KeyStroke(0, Key.SPACE)
        for ((comb, deadStroke) in dead) {
            SPACING[comb]?.let { strokes.putIfAbsent(it, listOf(deadStroke, space)) }
            for (base in COMPOSE_BASES[comb].orEmpty()) {
                val baseStroke = strokes[base]?.singleOrNull() ?: continue
                val composed = Normalizer.normalize("$base$comb", Normalizer.Form.NFC)
                if (composed.length == 1 && composed[0] != base && composed[0].isLetter()) {
                    strokes.putIfAbsent(composed[0], listOf(deadStroke, baseStroke))
                }
            }
        }
    }

    /** Key strokes that produce [c] on this layout, or null if the character can't be typed. */
    fun strokesFor(c: Char): List<KeyStroke>? = strokes[c]

    fun label(usage: Int): KeyLabel? = labels[usage]

    companion object {
        private fun isCombining(c: Char) = c.code in 0x0300..0x036F
        private fun display(c: Char): String = if (isCombining(c)) (SPACING[c]?.toString() ?: "◌$c") else c.toString()

        private val SPACING = mapOf(
            '̀' to '`', '́' to '´', '̂' to '^', '̃' to '~', '̈' to '¨',
        )
        private const val GREEK_VOWELS = "αεηιουωΑΕΗΙΟΥΩ"
        private val COMPOSE_BASES = mapOf(
            '̀' to "aeiouAEIOU",
            '́' to "aeiouyAEIOUY$GREEK_VOWELS",
            '̂' to "aeiouAEIOU",
            '̃' to "aonAON",
            '̈' to "aeiouyAEIOUYιυΙΥ",
        )

        // Physical positions, named after US QWERTY.
        val ROW1 = intArrayOf(Key.GRAVE, Key.N1, Key.N2, Key.N3, Key.N4, Key.N5, Key.N6, Key.N7, Key.N8, Key.N9, Key.N0, Key.MINUS, Key.EQUAL)
        val ROW2_ANSI = intArrayOf(Key.Q, Key.W, Key.E, Key.R, Key.T, Key.Y, Key.U, Key.I, Key.O, Key.P, Key.LEFT_BRACKET, Key.RIGHT_BRACKET, Key.BACKSLASH)
        val ROW2_ISO = ROW2_ANSI.copyOf(12)
        val ROW3_ANSI = intArrayOf(Key.A, Key.S, Key.D, Key.F, Key.G, Key.H, Key.J, Key.K, Key.L, Key.SEMICOLON, Key.APOSTROPHE)
        val ROW3_ISO = ROW3_ANSI + Key.NON_US_HASH
        val ROW4_ANSI = intArrayOf(Key.Z, Key.X, Key.C, Key.V, Key.B, Key.N, Key.M, Key.COMMA, Key.PERIOD, Key.SLASH)
        val ROW4_ISO = intArrayOf(Key.NON_US_BACKSLASH) + ROW4_ANSI

        private fun r(vararg s: String) = s.toList()

        val FRENCH = KeyboardLayout(
            "fr", "Français (AZERTY)", iso = true,
            r("² ", "&1", "é2̃", "\"3#", "'4{", "(5[", "-6|", "è7̀", "_8\\", "ç9", "à0@", ")°]", "=+}"),
            r("aA", "zZ", "eE€", "rR", "tT", "yY", "uU", "iI", "oO", "pP", "̂̈", "$£¤"),
            r("qQ", "sS", "dD", "fF", "gG", "hH", "jJ", "kK", "lL", "mM", "ù%", "*µ"),
            r("<>", "wW", "xX", "cC", "vV", "bB", "nN", ",?", ";.", ":/", "!§"),
        )

        val BELGIAN = KeyboardLayout(
            "be", "Belge (AZERTY)", iso = true,
            r("²³", "&1|", "é2@", "\"3#", "'4", "(5", "§6", "è7", "!8", "ç9{", "à0}", ")°", "-_"),
            r("aA", "zZ", "eE€", "rR", "tT", "yY", "uU", "iI", "oO", "pP", "̂̈[", "$*]"),
            r("qQ", "sS", "dD", "fF", "gG", "hH", "jJ", "kK", "lL", "mM", "ù%́", "µ£̀"),
            r("<>\\", "wW", "xX", "cC", "vV", "bB", "nN", ",?", ";.", ":/", "=+̃"),
        )

        val SWISS_FRENCH = KeyboardLayout(
            "ch", "Suisse romand (QWERTZ)", iso = true,
            r("§°", "1+¦", "2\"@", "3*#", "4ç", "5%", "6&¬", "7/|", "8(¢", "9)", "0=", "'?́", "̂̀̃"),
            r("qQ", "wW", "eE€", "rR", "tT", "zZ", "uU", "iI", "oO", "pP", "èü[", "̈!]"),
            r("aA", "sS", "dD", "fF", "gG", "hH", "jJ", "kK", "lL", "éö", "àä{", "$£}"),
            r("<>\\", "yY", "xX", "cC", "vV", "bB", "nN", "mM", ",;", ".:", "-_"),
        )

        val US = KeyboardLayout(
            "us", "English US (QWERTY)", iso = false,
            r("`~", "1!", "2@", "3#", "4$", "5%", "6^", "7&", "8*", "9(", "0)", "-_", "=+"),
            r("qQ", "wW", "eE", "rR", "tT", "yY", "uU", "iI", "oO", "pP", "[{", "]}", "\\|"),
            r("aA", "sS", "dD", "fF", "gG", "hH", "jJ", "kK", "lL", ";:", "'\""),
            r("zZ", "xX", "cC", "vV", "bB", "nN", "mM", ",<", ".>", "/?"),
        )

        val UK = KeyboardLayout(
            "uk", "English UK (QWERTY)", iso = true,
            r("`¬¦", "1!", "2\"", "3£", "4$€", "5%", "6^", "7&", "8*", "9(", "0)", "-_", "=+"),
            r("qQ", "wW", "eEéÉ", "rR", "tT", "yY", "uUúÚ", "iIíÍ", "oOóÓ", "pP", "[{", "]}"),
            r("aAáÁ", "sS", "dD", "fF", "gG", "hH", "jJ", "kK", "lL", ";:", "'@", "#~"),
            r("\\|", "zZ", "xX", "cC", "vV", "bB", "nN", "mM", ",<", ".>", "/?"),
        )

        val GERMAN = KeyboardLayout(
            "de", "Deutsch (QWERTZ)", iso = true,
            r("̂°", "1!", "2\"²", "3§³", "4$", "5%", "6&", "7/{", "8([", "9)]", "0=}", "ß?\\", "́̀"),
            r("qQ@", "wW", "eE€", "rR", "tT", "zZ", "uU", "iI", "oO", "pP", "üÜ", "+*~"),
            r("aA", "sS", "dD", "fF", "gG", "hH", "jJ", "kK", "lL", "öÖ", "äÄ", "#'"),
            r("<>|", "yY", "xX", "cC", "vV", "bB", "nN", "mMµ", ",;", ".:", "-_"),
        )

        val SPANISH = KeyboardLayout(
            "es", "Español (QWERTY)", iso = true,
            r("ºª\\", "1!|", "2\"@", "3·#", "4$", "5%", "6&¬", "7/", "8(", "9)", "0=", "'?", "¡¿"),
            r("qQ", "wW", "eE€", "rR", "tT", "yY", "uU", "iI", "oO", "pP", "̀̂[", "+*]"),
            r("aA", "sS", "dD", "fF", "gG", "hH", "jJ", "kK", "lL", "ñÑ", "́̈{", "çÇ}"),
            r("<>", "zZ", "xX", "cC", "vV", "bB", "nN", "mM", ",;", ".:", "-_"),
        )

        val ITALIAN = KeyboardLayout(
            "it", "Italiano (QWERTY)", iso = true,
            r("\\|", "1!", "2\"", "3£", "4$", "5%€", "6&", "7/", "8(", "9)", "0=", "'?", "ì^"),
            r("qQ", "wW", "eE€", "rR", "tT", "yY", "uU", "iI", "oO", "pP", "èé[{", "+*]}"),
            r("aA", "sS", "dD", "fF", "gG", "hH", "jJ", "kK", "lL", "òç@", "à°#", "ù§"),
            r("<>", "zZ", "xX", "cC", "vV", "bB", "nN", "mM", ",;", ".:", "-_"),
        )

        val GREEK = KeyboardLayout(
            "el", "Ελληνικά (Greek)", iso = false,
            r("`~", "1!", "2@", "3#", "4$", "5%", "6^", "7&", "8*", "9(", "0)", "-_", "=+"),
            r(";:", "ς", "εΕ", "ρΡ", "τΤ", "υΥ", "θΘ", "ιΙ", "οΟ", "πΠ", "[{", "]}", "\\|"),
            r("αΑ", "σΣ", "δΔ", "φΦ", "γΓ", "ηΗ", "ξΞ", "κΚ", "λΛ", "́̈", "'\""),
            r("ζΖ", "χΧ", "ψΨ", "ωΩ", "βΒ", "νΝ", "μΜ", ",<", ".>", "/?"),
        )

        val ALL: List<KeyboardLayout> = listOf(FRENCH, BELGIAN, SWISS_FRENCH, US, UK, GERMAN, SPANISH, ITALIAN, GREEK)

        fun byId(id: String): KeyboardLayout = ALL.firstOrNull { it.id == id } ?: FRENCH
    }
}

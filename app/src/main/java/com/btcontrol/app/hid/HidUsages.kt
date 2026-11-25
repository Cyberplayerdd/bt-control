package com.btcontrol.app.hid

import com.btcontrol.app.R
import com.btcontrol.app.t

/** HID usage IDs (USB HID Usage Tables 1.4). */
object Key {
    // Letters (positions on a US QWERTY keyboard)
    const val A = 0x04; const val B = 0x05; const val C = 0x06; const val D = 0x07
    const val E = 0x08; const val F = 0x09; const val G = 0x0A; const val H = 0x0B
    const val I = 0x0C; const val J = 0x0D; const val K = 0x0E; const val L = 0x0F
    const val M = 0x10; const val N = 0x11; const val O = 0x12; const val P = 0x13
    const val Q = 0x14; const val R = 0x15; const val S = 0x16; const val T = 0x17
    const val U = 0x18; const val V = 0x19; const val W = 0x1A; const val X = 0x1B
    const val Y = 0x1C; const val Z = 0x1D

    const val N1 = 0x1E; const val N2 = 0x1F; const val N3 = 0x20; const val N4 = 0x21
    const val N5 = 0x22; const val N6 = 0x23; const val N7 = 0x24; const val N8 = 0x25
    const val N9 = 0x26; const val N0 = 0x27

    const val ENTER = 0x28
    const val ESC = 0x29
    const val BACKSPACE = 0x2A
    const val TAB = 0x2B
    const val SPACE = 0x2C
    const val MINUS = 0x2D
    const val EQUAL = 0x2E
    const val LEFT_BRACKET = 0x2F
    const val RIGHT_BRACKET = 0x30
    const val BACKSLASH = 0x31
    const val NON_US_HASH = 0x32
    const val SEMICOLON = 0x33
    const val APOSTROPHE = 0x34
    const val GRAVE = 0x35
    const val COMMA = 0x36
    const val PERIOD = 0x37
    const val SLASH = 0x38
    const val CAPS_LOCK = 0x39

    const val F1 = 0x3A; const val F2 = 0x3B; const val F3 = 0x3C; const val F4 = 0x3D
    const val F5 = 0x3E; const val F6 = 0x3F; const val F7 = 0x40; const val F8 = 0x41
    const val F9 = 0x42; const val F10 = 0x43; const val F11 = 0x44; const val F12 = 0x45

    const val PRINT_SCREEN = 0x46
    const val SCROLL_LOCK = 0x47
    const val PAUSE = 0x48
    const val INSERT = 0x49
    const val HOME = 0x4A
    const val PAGE_UP = 0x4B
    const val DELETE = 0x4C
    const val END = 0x4D
    const val PAGE_DOWN = 0x4E
    const val RIGHT = 0x4F
    const val LEFT = 0x50
    const val DOWN = 0x51
    const val UP = 0x52

    const val NUM_LOCK = 0x53
    const val KP_SLASH = 0x54
    const val KP_ASTERISK = 0x55
    const val KP_MINUS = 0x56
    const val KP_PLUS = 0x57
    const val KP_ENTER = 0x58
    const val KP_1 = 0x59; const val KP_2 = 0x5A; const val KP_3 = 0x5B
    const val KP_4 = 0x5C; const val KP_5 = 0x5D; const val KP_6 = 0x5E
    const val KP_7 = 0x5F; const val KP_8 = 0x60; const val KP_9 = 0x61
    const val KP_0 = 0x62
    const val KP_DOT = 0x63
    const val NON_US_BACKSLASH = 0x64
    const val APPLICATION = 0x65
    const val KP_EQUAL = 0x67

    const val F13 = 0x68; const val F14 = 0x69; const val F15 = 0x6A; const val F16 = 0x6B
    const val F17 = 0x6C; const val F18 = 0x6D; const val F19 = 0x6E; const val F20 = 0x6F
    const val F21 = 0x70; const val F22 = 0x71; const val F23 = 0x72; const val F24 = 0x73

    const val LEFT_CTRL = 0xE0
    const val LEFT_SHIFT = 0xE1
    const val LEFT_ALT = 0xE2
    const val LEFT_GUI = 0xE3
    const val RIGHT_CTRL = 0xE4
    const val RIGHT_SHIFT = 0xE5
    const val RIGHT_ALT = 0xE6
    const val RIGHT_GUI = 0xE7

    fun isModifier(usage: Int) = usage in 0xE0..0xE7
    fun modifierBit(usage: Int) = if (isModifier(usage)) 1 shl (usage - 0xE0) else 0

    val KEYPAD_DIGITS = intArrayOf(KP_0, KP_1, KP_2, KP_3, KP_4, KP_5, KP_6, KP_7, KP_8, KP_9)
    val F_KEYS = intArrayOf(F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12)

    /** Human readable name for non-character keys (used by the layout editor & key picker). */
    val NAMES: Map<Int, String> = linkedMapOf(
        ENTER to "Enter", ESC to "Esc", BACKSPACE to "Backspace", TAB to "Tab", SPACE to "Space",
        CAPS_LOCK to "Caps Lock", PRINT_SCREEN to "Print Screen", SCROLL_LOCK to "Scroll Lock",
        PAUSE to "Pause", INSERT to "Insert", HOME to "Home", PAGE_UP to "Page Up", DELETE to "Delete",
        END to "End", PAGE_DOWN to "Page Down", RIGHT to "→", LEFT to "←", DOWN to "↓", UP to "↑",
        NUM_LOCK to "Num Lock", KP_SLASH to "Num /", KP_ASTERISK to "Num *", KP_MINUS to "Num -",
        KP_PLUS to "Num +", KP_ENTER to "Num Enter", KP_1 to "Num 1", KP_2 to "Num 2", KP_3 to "Num 3",
        KP_4 to "Num 4", KP_5 to "Num 5", KP_6 to "Num 6", KP_7 to "Num 7", KP_8 to "Num 8",
        KP_9 to "Num 9", KP_0 to "Num 0", KP_DOT to "Num .", KP_EQUAL to "Num =",
        APPLICATION to "Menu",
        F1 to "F1", F2 to "F2", F3 to "F3", F4 to "F4", F5 to "F5", F6 to "F6", F7 to "F7",
        F8 to "F8", F9 to "F9", F10 to "F10", F11 to "F11", F12 to "F12", F13 to "F13", F14 to "F14",
        F15 to "F15", F16 to "F16", F17 to "F17", F18 to "F18", F19 to "F19", F20 to "F20",
        F21 to "F21", F22 to "F22", F23 to "F23", F24 to "F24",
        LEFT_CTRL to "Ctrl", LEFT_SHIFT to "Shift", LEFT_ALT to "Alt", LEFT_GUI to "Win",
        RIGHT_CTRL to "Right Ctrl", RIGHT_SHIFT to "Right Shift", RIGHT_ALT to "AltGr", RIGHT_GUI to "Right Win",
    )
}

/** Modifier bit masks for byte 0 of the keyboard report. */
object Mod {
    const val NONE = 0
    const val LCTRL = 0x01
    const val LSHIFT = 0x02
    const val LALT = 0x04
    const val LGUI = 0x08
    const val RCTRL = 0x10
    const val RSHIFT = 0x20
    const val RALT = 0x40 // AltGr
    const val RGUI = 0x80

    fun describe(mods: Int): String = buildList {
        if (mods and (LCTRL or RCTRL) != 0) add("Ctrl")
        if (mods and (LGUI or RGUI) != 0) add("Win")
        if (mods and LALT != 0) add("Alt")
        if (mods and RALT != 0) add("AltGr")
        if (mods and (LSHIFT or RSHIFT) != 0) add("Shift")
    }.joinToString("+")
}

/** Keyboard LED bits (output report). */
object Led {
    const val NUM_LOCK = 0x01
    const val CAPS_LOCK = 0x02
    const val SCROLL_LOCK = 0x04
}

/** Consumer page (0x0C) usages. */
object Consumer {
    const val BRIGHTNESS_UP = 0x6F
    const val BRIGHTNESS_DOWN = 0x70
    const val RECORD = 0xB2
    const val FAST_FORWARD = 0xB3
    const val REWIND = 0xB4
    const val NEXT_TRACK = 0xB5
    const val PREV_TRACK = 0xB6
    const val STOP = 0xB7
    const val EJECT = 0xB8
    const val PLAY_PAUSE = 0xCD
    const val MUTE = 0xE2
    const val VOLUME_UP = 0xE9
    const val VOLUME_DOWN = 0xEA
    const val MEDIA_PLAYER = 0x183
    const val EMAIL = 0x18A
    const val CALCULATOR = 0x192
    const val FILE_EXPLORER = 0x194
    const val BROWSER = 0x196
    const val AC_SEARCH = 0x221
    const val AC_HOME = 0x223
    const val AC_BACK = 0x224
    const val AC_FORWARD = 0x225
    const val AC_STOP = 0x226
    const val AC_REFRESH = 0x227
    const val AC_BOOKMARKS = 0x22A
    const val AC_ZOOM_IN = 0x22D
    const val AC_ZOOM_OUT = 0x22E

    val NAMES: Map<Int, String> get() = linkedMapOf(
        PLAY_PAUSE to t(R.string.play_pause), NEXT_TRACK to t(R.string.next_track), PREV_TRACK to t(R.string.previous_track),
        STOP to t(R.string.stop), FAST_FORWARD to t(R.string.fast_forward), REWIND to t(R.string.rewind), RECORD to t(R.string.record),
        EJECT to t(R.string.eject), MUTE to t(R.string.mute), VOLUME_UP to t(R.string.volume), VOLUME_DOWN to t(R.string.volume_2),
        BRIGHTNESS_UP to t(R.string.brightness), BRIGHTNESS_DOWN to t(R.string.brightness_2),
        MEDIA_PLAYER to t(R.string.media_player), EMAIL to t(R.string.mail), CALCULATOR to t(R.string.calculator),
        FILE_EXPLORER to t(R.string.file_explorer), BROWSER to t(R.string.web_browser), AC_SEARCH to t(R.string.search),
        AC_HOME to t(R.string.browser_home), AC_BACK to t(R.string.browser_back), AC_FORWARD to t(R.string.browser_forward),
        AC_STOP to t(R.string.browser_stop), AC_REFRESH to t(R.string.refresh), AC_BOOKMARKS to t(R.string.bookmarks),
        AC_ZOOM_IN to t(R.string.zoom_in), AC_ZOOM_OUT to t(R.string.zoom_out),
    )
}

/** Generic Desktop / System Control bits (report 6). */
object SystemKey {
    const val POWER = 0x01
    const val SLEEP = 0x02
    const val WAKE = 0x04
}

/** Mouse button bits (report 2). */
object MouseButton {
    const val LEFT = 0x01
    const val RIGHT = 0x02
    const val MIDDLE = 0x04
    const val BACK = 0x08
    const val FORWARD = 0x10

    val NAMES get() = linkedMapOf(LEFT to t(R.string.left_click), RIGHT to t(R.string.right_click), MIDDLE to t(R.string.middle_click),
        BACK to t(R.string.back_button), FORWARD to t(R.string.forward_button))
}

/** Gamepad button indexes (bit positions in report 4). */
object PadButton {
    const val A = 0; const val B = 1; const val X = 2; const val Y = 3
    const val LB = 4; const val RB = 5; const val BACK = 6; const val START = 7
    const val L3 = 8; const val R3 = 9; const val HOME = 10
    const val LT_BTN = 11; const val RT_BTN = 12
}

/** Hat switch values (report 4). */
object Hat {
    const val UP = 0; const val UP_RIGHT = 1; const val RIGHT = 2; const val DOWN_RIGHT = 3
    const val DOWN = 4; const val DOWN_LEFT = 5; const val LEFT = 6; const val UP_LEFT = 7
    const val CENTER = 8 // out of logical range → "null" state
}

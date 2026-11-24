package com.btcontrol.app.hid

import android.view.KeyEvent

/** Maps Android key events to HID usages. */
object AndroidKeyMap {

    /** Linux input scan codes (physical position) → HID usage. Used for hardware keyboard pass-through. */
    private val SCAN = HashMap<Int, Int>().apply {
        put(1, Key.ESC)
        for (i in 0..9) put(2 + i, Key.N1 + i) // KEY_1..KEY_0
        put(12, Key.MINUS); put(13, Key.EQUAL); put(14, Key.BACKSPACE); put(15, Key.TAB)
        val qRow = intArrayOf(Key.Q, Key.W, Key.E, Key.R, Key.T, Key.Y, Key.U, Key.I, Key.O, Key.P)
        qRow.forEachIndexed { i, u -> put(16 + i, u) }
        put(26, Key.LEFT_BRACKET); put(27, Key.RIGHT_BRACKET); put(28, Key.ENTER); put(29, Key.LEFT_CTRL)
        val aRow = intArrayOf(Key.A, Key.S, Key.D, Key.F, Key.G, Key.H, Key.J, Key.K, Key.L)
        aRow.forEachIndexed { i, u -> put(30 + i, u) }
        put(39, Key.SEMICOLON); put(40, Key.APOSTROPHE); put(41, Key.GRAVE); put(42, Key.LEFT_SHIFT)
        put(43, Key.NON_US_HASH)
        val zRow = intArrayOf(Key.Z, Key.X, Key.C, Key.V, Key.B, Key.N, Key.M)
        zRow.forEachIndexed { i, u -> put(44 + i, u) }
        put(51, Key.COMMA); put(52, Key.PERIOD); put(53, Key.SLASH); put(54, Key.RIGHT_SHIFT)
        put(55, Key.KP_ASTERISK); put(56, Key.LEFT_ALT); put(57, Key.SPACE); put(58, Key.CAPS_LOCK)
        for (i in 0..9) put(59 + i, Key.F1 + i) // F1..F10
        put(69, Key.NUM_LOCK); put(70, Key.SCROLL_LOCK)
        put(71, Key.KP_7); put(72, Key.KP_8); put(73, Key.KP_9); put(74, Key.KP_MINUS)
        put(75, Key.KP_4); put(76, Key.KP_5); put(77, Key.KP_6); put(78, Key.KP_PLUS)
        put(79, Key.KP_1); put(80, Key.KP_2); put(81, Key.KP_3); put(82, Key.KP_0); put(83, Key.KP_DOT)
        put(86, Key.NON_US_BACKSLASH); put(87, Key.F11); put(88, Key.F12)
        put(96, Key.KP_ENTER); put(97, Key.RIGHT_CTRL); put(98, Key.KP_SLASH); put(99, Key.PRINT_SCREEN)
        put(100, Key.RIGHT_ALT); put(102, Key.HOME); put(103, Key.UP); put(104, Key.PAGE_UP)
        put(105, Key.LEFT); put(106, Key.RIGHT); put(107, Key.END); put(108, Key.DOWN)
        put(109, Key.PAGE_DOWN); put(110, Key.INSERT); put(111, Key.DELETE); put(117, Key.KP_EQUAL)
        put(119, Key.PAUSE); put(125, Key.LEFT_GUI); put(126, Key.RIGHT_GUI); put(127, Key.APPLICATION)
    }

    /** Android key codes of non-character keys → HID usage (for soft keyboards / IME events). */
    private val KEYCODES = HashMap<Int, Int>().apply {
        put(KeyEvent.KEYCODE_DEL, Key.BACKSPACE)
        put(KeyEvent.KEYCODE_FORWARD_DEL, Key.DELETE)
        put(KeyEvent.KEYCODE_ENTER, Key.ENTER)
        put(KeyEvent.KEYCODE_NUMPAD_ENTER, Key.KP_ENTER)
        put(KeyEvent.KEYCODE_TAB, Key.TAB)
        put(KeyEvent.KEYCODE_ESCAPE, Key.ESC)
        put(KeyEvent.KEYCODE_SPACE, Key.SPACE)
        put(KeyEvent.KEYCODE_DPAD_UP, Key.UP)
        put(KeyEvent.KEYCODE_DPAD_DOWN, Key.DOWN)
        put(KeyEvent.KEYCODE_DPAD_LEFT, Key.LEFT)
        put(KeyEvent.KEYCODE_DPAD_RIGHT, Key.RIGHT)
        put(KeyEvent.KEYCODE_MOVE_HOME, Key.HOME)
        put(KeyEvent.KEYCODE_MOVE_END, Key.END)
        put(KeyEvent.KEYCODE_PAGE_UP, Key.PAGE_UP)
        put(KeyEvent.KEYCODE_PAGE_DOWN, Key.PAGE_DOWN)
        put(KeyEvent.KEYCODE_INSERT, Key.INSERT)
        put(KeyEvent.KEYCODE_CAPS_LOCK, Key.CAPS_LOCK)
        put(KeyEvent.KEYCODE_NUM_LOCK, Key.NUM_LOCK)
        put(KeyEvent.KEYCODE_SCROLL_LOCK, Key.SCROLL_LOCK)
        put(KeyEvent.KEYCODE_SYSRQ, Key.PRINT_SCREEN)
        put(KeyEvent.KEYCODE_BREAK, Key.PAUSE)
        put(KeyEvent.KEYCODE_MENU, Key.APPLICATION)
        for (i in 0..11) put(KeyEvent.KEYCODE_F1 + i, Key.F1 + i)
        put(KeyEvent.KEYCODE_CTRL_LEFT, Key.LEFT_CTRL)
        put(KeyEvent.KEYCODE_CTRL_RIGHT, Key.RIGHT_CTRL)
        put(KeyEvent.KEYCODE_SHIFT_LEFT, Key.LEFT_SHIFT)
        put(KeyEvent.KEYCODE_SHIFT_RIGHT, Key.RIGHT_SHIFT)
        put(KeyEvent.KEYCODE_ALT_LEFT, Key.LEFT_ALT)
        put(KeyEvent.KEYCODE_ALT_RIGHT, Key.RIGHT_ALT)
        put(KeyEvent.KEYCODE_META_LEFT, Key.LEFT_GUI)
        put(KeyEvent.KEYCODE_META_RIGHT, Key.RIGHT_GUI)
    }

    /** Physical key → HID usage (hardware keyboards report a Linux scan code). */
    fun fromScanCode(scanCode: Int): Int? = SCAN[scanCode]

    /** Non-character Android key code → HID usage. */
    fun fromKeyCode(keyCode: Int): Int? = KEYCODES[keyCode]
}

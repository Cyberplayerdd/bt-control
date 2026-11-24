package com.btcontrol.app.hid

/**
 * One composite HID report descriptor. Every function of the app is a separate top-level
 * collection selected by its report ID, so the PC creates a keyboard, a mouse, a consumer
 * control, a system control, a game controller and a pen digitizer from a single pairing.
 *
 * IMPORTANT: hosts (Windows especially) cache this descriptor at pairing time. If it is ever
 * changed, the computer must "remove device" and pair again.
 */
object HidDescriptor {
    const val ID_KEYBOARD = 1
    const val ID_MOUSE = 2
    const val ID_CONSUMER = 3
    const val ID_SYSTEM = 4
    const val ID_GAMEPAD = 5
    const val ID_PEN = 6

    const val KEYBOARD_LEN = 8
    const val MOUSE_LEN = 7
    const val CONSUMER_LEN = 2
    const val SYSTEM_LEN = 1
    const val GAMEPAD_LEN = 9
    const val PEN_LEN = 9

    const val PEN_MAX_XY = 32767
    const val PEN_MAX_PRESSURE = 4095
    const val PEN_MAX_TILT = 90

    fun inputReportLength(id: Int) = when (id) {
        ID_KEYBOARD -> KEYBOARD_LEN
        ID_MOUSE -> MOUSE_LEN
        ID_CONSUMER -> CONSUMER_LEN
        ID_SYSTEM -> SYSTEM_LEN
        ID_GAMEPAD -> GAMEPAD_LEN
        ID_PEN -> PEN_LEN
        else -> 0
    }

    private fun b(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }

    private val KEYBOARD = b(
        0x05, 0x01,        // Usage Page (Generic Desktop)
        0x09, 0x06,        // Usage (Keyboard)
        0xA1, 0x01,        // Collection (Application)
        0x85, ID_KEYBOARD, //   Report ID
        0x05, 0x07,        //   Usage Page (Keyboard/Keypad)
        0x19, 0xE0,        //   Usage Minimum (Left Control)
        0x29, 0xE7,        //   Usage Maximum (Right GUI)
        0x15, 0x00,        //   Logical Minimum (0)
        0x25, 0x01,        //   Logical Maximum (1)
        0x75, 0x01,        //   Report Size (1)
        0x95, 0x08,        //   Report Count (8)
        0x81, 0x02,        //   Input (Data,Var,Abs)        modifiers
        0x95, 0x01,        //   Report Count (1)
        0x75, 0x08,        //   Report Size (8)
        0x81, 0x01,        //   Input (Const)               reserved
        0x95, 0x05,        //   Report Count (5)
        0x75, 0x01,        //   Report Size (1)
        0x05, 0x08,        //   Usage Page (LEDs)
        0x19, 0x01,        //   Usage Minimum (Num Lock)
        0x29, 0x05,        //   Usage Maximum (Kana)
        0x91, 0x02,        //   Output (Data,Var,Abs)       LEDs
        0x95, 0x01,        //   Report Count (1)
        0x75, 0x03,        //   Report Size (3)
        0x91, 0x01,        //   Output (Const)              padding
        0x95, 0x06,        //   Report Count (6)
        0x75, 0x08,        //   Report Size (8)
        0x15, 0x00,        //   Logical Minimum (0)
        0x26, 0xFF, 0x00,  //   Logical Maximum (255)
        0x05, 0x07,        //   Usage Page (Keyboard/Keypad)
        0x19, 0x00,        //   Usage Minimum (0)
        0x2A, 0xFF, 0x00,  //   Usage Maximum (255)
        0x81, 0x00,        //   Input (Data,Array,Abs)      6 key slots
        0xC0,              // End Collection
    )

    private val MOUSE = b(
        0x05, 0x01,        // Usage Page (Generic Desktop)
        0x09, 0x02,        // Usage (Mouse)
        0xA1, 0x01,        // Collection (Application)
        0x85, ID_MOUSE,    //   Report ID
        0x09, 0x01,        //   Usage (Pointer)
        0xA1, 0x00,        //   Collection (Physical)
        0x05, 0x09,        //     Usage Page (Button)
        0x19, 0x01,        //     Usage Minimum (1)
        0x29, 0x05,        //     Usage Maximum (5)
        0x15, 0x00,        //     Logical Minimum (0)
        0x25, 0x01,        //     Logical Maximum (1)
        0x95, 0x05,        //     Report Count (5)
        0x75, 0x01,        //     Report Size (1)
        0x81, 0x02,        //     Input (Data,Var,Abs)
        0x95, 0x01,        //     Report Count (1)
        0x75, 0x03,        //     Report Size (3)
        0x81, 0x01,        //     Input (Const)
        0x05, 0x01,        //     Usage Page (Generic Desktop)
        0x09, 0x30,        //     Usage (X)
        0x09, 0x31,        //     Usage (Y)
        0x16, 0x01, 0x80,  //     Logical Minimum (-32767)
        0x26, 0xFF, 0x7F,  //     Logical Maximum (32767)
        0x75, 0x10,        //     Report Size (16)
        0x95, 0x02,        //     Report Count (2)
        0x81, 0x06,        //     Input (Data,Var,Rel)
        0x09, 0x38,        //     Usage (Wheel)
        0x15, 0x81,        //     Logical Minimum (-127)
        0x25, 0x7F,        //     Logical Maximum (127)
        0x75, 0x08,        //     Report Size (8)
        0x95, 0x01,        //     Report Count (1)
        0x81, 0x06,        //     Input (Data,Var,Rel)
        0x05, 0x0C,        //     Usage Page (Consumer)
        0x0A, 0x38, 0x02,  //     Usage (AC Pan)
        0x95, 0x01,        //     Report Count (1)
        0x81, 0x06,        //     Input (Data,Var,Rel)
        0xC0,              //   End Collection
        0xC0,              // End Collection
    )

    private val CONSUMER = b(
        0x05, 0x0C,        // Usage Page (Consumer)
        0x09, 0x01,        // Usage (Consumer Control)
        0xA1, 0x01,        // Collection (Application)
        0x85, ID_CONSUMER, //   Report ID
        0x15, 0x00,        //   Logical Minimum (0)
        0x26, 0xFF, 0x03,  //   Logical Maximum (0x3FF)
        0x19, 0x00,        //   Usage Minimum (0)
        0x2A, 0xFF, 0x03,  //   Usage Maximum (0x3FF)
        0x75, 0x10,        //   Report Size (16)
        0x95, 0x01,        //   Report Count (1)
        0x81, 0x00,        //   Input (Data,Array,Abs)
        0xC0,              // End Collection
    )

    private val SYSTEM = b(
        0x05, 0x01,        // Usage Page (Generic Desktop)
        0x09, 0x80,        // Usage (System Control)
        0xA1, 0x01,        // Collection (Application)
        0x85, ID_SYSTEM,   //   Report ID
        0x19, 0x81,        //   Usage Minimum (System Power Down)
        0x29, 0x83,        //   Usage Maximum (System Wake Up)
        0x15, 0x00,        //   Logical Minimum (0)
        0x25, 0x01,        //   Logical Maximum (1)
        0x75, 0x01,        //   Report Size (1)
        0x95, 0x03,        //   Report Count (3)
        0x81, 0x02,        //   Input (Data,Var,Abs)
        0x95, 0x05,        //   Report Count (5)
        0x81, 0x01,        //   Input (Const)
        0xC0,              // End Collection
    )

    private val GAMEPAD = b(
        0x05, 0x01,        // Usage Page (Generic Desktop)
        0x09, 0x05,        // Usage (Game Pad)
        0xA1, 0x01,        // Collection (Application)
        0x85, ID_GAMEPAD,  //   Report ID
        0x05, 0x09,        //   Usage Page (Button)
        0x19, 0x01,        //   Usage Minimum (1)
        0x29, 0x10,        //   Usage Maximum (16)
        0x15, 0x00,        //   Logical Minimum (0)
        0x25, 0x01,        //   Logical Maximum (1)
        0x75, 0x01,        //   Report Size (1)
        0x95, 0x10,        //   Report Count (16)
        0x81, 0x02,        //   Input (Data,Var,Abs)
        0x05, 0x01,        //   Usage Page (Generic Desktop)
        0x09, 0x39,        //   Usage (Hat switch)
        0x15, 0x00,        //   Logical Minimum (0)
        0x25, 0x07,        //   Logical Maximum (7)
        0x35, 0x00,        //   Physical Minimum (0)
        0x46, 0x3B, 0x01,  //   Physical Maximum (315)
        0x65, 0x14,        //   Unit (Degrees)
        0x75, 0x04,        //   Report Size (4)
        0x95, 0x01,        //   Report Count (1)
        0x81, 0x42,        //   Input (Data,Var,Abs,Null)
        0x65, 0x00,        //   Unit (None)
        0x45, 0x00,        //   Physical Maximum (0)
        0x75, 0x04,        //   Report Size (4)
        0x95, 0x01,        //   Report Count (1)
        0x81, 0x01,        //   Input (Const)               padding
        0x09, 0x30,        //   Usage (X)                   left stick
        0x09, 0x31,        //   Usage (Y)
        0x09, 0x32,        //   Usage (Z)                   right stick
        0x09, 0x35,        //   Usage (Rz)
        0x15, 0x00,        //   Logical Minimum (0)
        0x26, 0xFF, 0x00,  //   Logical Maximum (255)
        0x75, 0x08,        //   Report Size (8)
        0x95, 0x04,        //   Report Count (4)
        0x81, 0x02,        //   Input (Data,Var,Abs)
        0x09, 0x33,        //   Usage (Rx)                  left trigger
        0x09, 0x34,        //   Usage (Ry)                  right trigger
        0x95, 0x02,        //   Report Count (2)
        0x81, 0x02,        //   Input (Data,Var,Abs)
        0xC0,              // End Collection
    )

    private val PEN = b(
        0x05, 0x0D,        // Usage Page (Digitizer)
        0x09, 0x02,        // Usage (Pen)
        0xA1, 0x01,        // Collection (Application)
        0x85, ID_PEN,      //   Report ID
        0x09, 0x20,        //   Usage (Stylus)
        0xA1, 0x00,        //   Collection (Physical)
        0x09, 0x42,        //     Usage (Tip Switch)
        0x09, 0x44,        //     Usage (Barrel Switch)
        0x09, 0x3C,        //     Usage (Invert)
        0x09, 0x45,        //     Usage (Eraser)
        0x09, 0x32,        //     Usage (In Range)
        0x15, 0x00,        //     Logical Minimum (0)
        0x25, 0x01,        //     Logical Maximum (1)
        0x75, 0x01,        //     Report Size (1)
        0x95, 0x05,        //     Report Count (5)
        0x81, 0x02,        //     Input (Data,Var,Abs)
        0x95, 0x03,        //     Report Count (3)
        0x81, 0x03,        //     Input (Const,Var,Abs)     padding
        0x05, 0x01,        //     Usage Page (Generic Desktop)
        0x09, 0x30,        //     Usage (X)
        0x75, 0x10,        //     Report Size (16)
        0x95, 0x01,        //     Report Count (1)
        0x55, 0x0D,        //     Unit Exponent (-3)
        0x65, 0x13,        //     Unit (Inch)
        0x15, 0x00,        //     Logical Minimum (0)
        0x26, 0xFF, 0x7F,  //     Logical Maximum (32767)
        0x35, 0x00,        //     Physical Minimum (0)
        0x46, 0x10, 0x27,  //     Physical Maximum (10000 = 10.000 in)
        0x81, 0x02,        //     Input (Data,Var,Abs)
        0x09, 0x31,        //     Usage (Y)
        0x46, 0x6A, 0x18,  //     Physical Maximum (6250 = 6.250 in)
        0x81, 0x02,        //     Input (Data,Var,Abs)
        0x45, 0x00,        //     Physical Maximum (0)
        0x65, 0x00,        //     Unit (None)
        0x55, 0x00,        //     Unit Exponent (0)
        0x05, 0x0D,        //     Usage Page (Digitizer)
        0x09, 0x30,        //     Usage (Tip Pressure)
        0x26, 0xFF, 0x0F,  //     Logical Maximum (4095)
        0x75, 0x10,        //     Report Size (16)
        0x95, 0x01,        //     Report Count (1)
        0x81, 0x02,        //     Input (Data,Var,Abs)
        0x09, 0x3D,        //     Usage (X Tilt)
        0x09, 0x3E,        //     Usage (Y Tilt)
        0x15, 0xA6,        //     Logical Minimum (-90)
        0x25, 0x5A,        //     Logical Maximum (90)
        0x35, 0xA6,        //     Physical Minimum (-90)
        0x45, 0x5A,        //     Physical Maximum (90)
        0x65, 0x14,        //     Unit (Degrees)
        0x75, 0x08,        //     Report Size (8)
        0x95, 0x02,        //     Report Count (2)
        0x81, 0x02,        //     Input (Data,Var,Abs)
        0x35, 0x00,        //     Physical Minimum (0)
        0x45, 0x00,        //     Physical Maximum (0)
        0x65, 0x00,        //     Unit (None)
        0xC0,              //   End Collection
        0xC0,              // End Collection
    )

    val DESCRIPTOR: ByteArray = KEYBOARD + MOUSE + CONSUMER + SYSTEM + GAMEPAD + PEN
}

# BT Control

**Use your Android phone or tablet as a Bluetooth keyboard, mouse, remote, gamepad or drawing
tablet for your computer.**

- Nothing to install on the computer: it sees a normal Bluetooth device.
- Works with Windows, Linux, ChromeOS and Android TV (macOS too, except the pen).
- Needs Android 9 or newer. Works offline. 18 languages.

## Install

1. On your phone, download the APK from the [latest release](https://github.com/Cyberplayerdd/bt-control/releases/latest).
2. Open it and allow installing from that source when Android asks.

## Connect to your computer

1. In the app: **☰ › Bluetooth devices › Make visible for 2 minutes**.
2. On the PC: **Settings › Bluetooth & devices › Add device › Bluetooth**, then pick your phone.
3. Confirm the code on both screens. The status turns green.

Next time, just open the app: it reconnects by itself.

> Already paired your phone with this PC before (for file transfer…)? Remove it on the PC first,
> then pair again. Windows only learns that it's a keyboard/mouse when pairing.

## Main features

- 🖱️ **Mouse & keyboard**: touchpad, your phone's keyboard, air mouse (move the phone).
- ⌨️ **Computer keyboard**: from 40 % to full size, in your PC's layout (AZERTY, QWERTZ…).
- ✏️ **Drawing tablet**: pen pressure, tilt and hover with a stylus (Windows Ink), or draw with a finger.
- 🎮 **Gamepad**: two sticks, D-pad, buttons and triggers.
- 📺 **Remote & multimedia**: TV-style remote and media keys for watching things on the PC.
- 🎞️ **Presentation**: next/previous, laser pointer, timer.

## Also

- Numeric keypad
- Code scanner: reads barcodes and QR codes and types them on the PC
- Custom layouts: your own buttons, shortcuts and macros (import/export)
- Every screen works in portrait and landscape, and full screen
- Light/dark theme, reconnects automatically, stays connected in the background

## Tips

- **Wrong characters?** In **Settings › Computer keyboard**, pick the same keyboard layout as the PC.
- **Drawing apps:** works with any Windows Ink app (Krita, Photoshop, Clip Studio, OneNote…). In
  Krita, choose *Settings › Tablet › Windows 8+ Pointer Input*. In the app, set the pen area shape
  to your monitor's (e.g. 16:9) so circles stay round.
- **Games:** Windows sees a standard game controller (test it with *Win+R › joy.cpl*). For games
  that only accept Xbox controllers, add the game to Steam and turn on *Steam Input*.

## Problems?

| Problem | Fix |
|---|---|
| "HID registration failed" | Close other Bluetooth keyboard/mouse apps, turn Bluetooth off and on, tap *Retry*. |
| Paired but won't connect | Remove the phone on the PC and pair again with the app open. |
| Lag | Move closer to the PC, or raise *Report interval* in Settings. |

To report a bug, attach the log from **Settings › Diagnostic log › Share log**.

## Privacy

The app has no Internet permission: nothing leaves your phone unless you share it yourself. The
diagnostic log is anonymised (no device names, no Bluetooth addresses, no typed text or keys, only
events and counts) and small (last 3 sessions, 256 KB max each). You can view, delete or turn it off
in Settings.

## Languages

English, Français, Español, Deutsch, Italiano, Português, Nederlands, Polski, Русский, Українська,
Türkçe, Ελληνικά, 日本語, 한국어, 简体中文, हिन्दी, Bahasa Indonesia, Tiếng Việt.

Most translations were machine-assisted, so corrections are welcome: edit
`app/src/main/res/values-<language>/strings.xml`.

## Build from source

```
gradlew assembleRelease
```

The APK is in `app/build/outputs/apk/release/`. Without your own `keystore.properties` (see
[the example](keystore.properties.example)) it's signed with the debug key, so uninstall the
published version before installing yours. Technical notes: [PLAN.md](PLAN.md).

## License

[GPL-3.0](LICENSE)

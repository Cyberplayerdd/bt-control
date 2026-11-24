# BT Control — research notes & plan

Goal: an Android app that turns a phone/tablet into a **Bluetooth HID peripheral** so a PC
(Windows / Linux / macOS / Android TV …) sees it as a real keyboard, mouse, media remote,
gamepad and **pressure-sensitive pen tablet** — no software needed on the PC.

## 1. Research findings

| Question | Finding | Consequence |
|---|---|---|
| How can a phone act as a BT keyboard/mouse? | `android.bluetooth.BluetoothHidDevice` (API 28+). App registers an SDP record + HID report descriptor with `registerApp()`, then `sendReport(device, id, bytes)`. | `minSdk 28`. Only **one** HID-device app can be registered at a time on the phone. |
| Is the profile enabled on the test devices (Samsung tablet, Android 16; Samsung phone, Android 17)? | `dumpsys bluetooth_manager` → `Enabled Profile Services: HID_DEVICE`. | ✅ Works on this device. |
| What the PC caches | Windows reads the report descriptor from SDP **at pairing time** and keeps it. | Ship one complete composite descriptor from day 1. If it ever changes → remove the device on the PC and pair again. |
| Many functions over one link | A single descriptor can declare several top-level collections distinguished by **Report IDs**. Windows creates one HID device per collection (keyboard, mouse, consumer control, game controller, pen). | One registration gives every feature at once; switching screens needs no re-pairing. |
| Pen with pressure on Windows | Windows Ink accepts a HID **Digitizer / Pen** collection (usage page 0x0D, usage 0x02) with Tip Switch, Barrel, Invert, Eraser, **In Range** (mandatory), X/Y absolute with physical units, Tip Pressure, X/Y Tilt. Only specific button-state combinations are valid (e.g. eraser hovering = InRange+Invert; erasing = InRange+Invert+Eraser, Tip=0). | Pen report follows Microsoft's sample layout; state machine only emits valid states. Works with Krita, Photoshop, Clip Studio, OneNote, etc. |
| Pen hover on Android | Stylus sends `ACTION_HOVER_*`; Android emits `HOVER_EXIT` *right before* `ACTION_DOWN`. | Out-of-range is sent after a short delay and cancelled if the pen touches, otherwise Windows sees the pen "leave" at every stroke start. |
| Tablet's S Pen | `sec_e-pen`: pressure 0–4095, tilt ±63, `BTN_STYLUS`. | 12-bit pressure (0–4095) and tilt are forwarded; S Pen button → barrel (right-click) or eraser. |
| Typing text | HID sends **key positions**, the PC applies its own layout. This PC uses **French AZERTY** (+ US, Greek). | Character→keystroke tables per PC layout (FR, BE, US, UK, DE, ES, IT, CH-FR, GR) incl. AltGr and dead keys; Alt-code fallback for unmapped characters. |
| Gamepad on Windows | A generic HID gamepad shows up as a DirectInput "game controller" (joy.cpl). XInput-only games need Steam Input / x360ce / DS4Windows-style remapping. | DualShock-4-like axis layout (X,Y,Z,Rz sticks; Rx,Ry triggers; hat; 16 buttons), which SDL/Steam handle well. |
| Throughput | `sendReport()` returns false when the L2CAP channel is congested; flooding causes lag. | Report queue with coalescing (mouse deltas summed, pen/gamepad moves replaced) and pacing (~125 Hz), retries on congestion, button transitions never dropped. |
| Background | Registration dies with the process. | Foreground service (`connectedDevice` type) keeps the link while the app is in the background. |

## 2. HID report map (single composite descriptor)

| ID | Collection | Payload |
|---|---|---|
| 1 | Keyboard (+ LED output) | modifiers, reserved, 6 key slots / LEDs (Num, Caps, Scroll…) |
| 2 | Mouse | 5 buttons, X/Y 16-bit relative, wheel, horizontal pan |
| 3 | Consumer control | one 16-bit usage (media, volume, brightness, browser, app launch) |
| 4 | System control | power down, sleep, wake |
| 5 | Gamepad | 16 buttons, hat, X/Y/Z/Rz sticks, Rx/Ry analog triggers |
| 6 | Pen digitizer | tip/barrel/invert/eraser/in-range, X/Y 0–32767, pressure 0–4095, tilt X/Y |

## 3. App architecture (Kotlin + Jetpack Compose, Material 3)

```
hid/   HidDescriptor, HidUsages, ReportSender (queue/coalescing), HidController (keyboard/mouse/
       consumer/gamepad/pen state), KeyboardLayouts (char → keystrokes), AndroidKeyMap
bt/    BluetoothHidManager (proxy, registerApp, connect, pairing, discovery), HidService (FGS)
data/  AppSettings (SharedPreferences → StateFlow), CustomLayouts (JSON, import/export)
ui/    MainActivity + navigation drawer (same structure as the reference app), components
       (HoldButton, Touchpad, KeyInputView for the system keyboard, modifiers), screens
```

Screens (drawer): **Controls** → Mouse & keyboard · Multimedia · Computer keyboard · Numpad ·
Presentation · Code scanner · Gamepad · Drawing tablet — **Custom layouts** → your layouts ·
New layout · Manage layouts — **Configuration** → Bluetooth devices · Settings.

## 4. Build order
1. Gradle project (AGP 8.13, Kotlin 2.2, Compose BOM 2025.10, CameraX + ML Kit barcode).
2. HID core + Bluetooth manager + foreground service.
3. Screens: mouse/keyboard, media, full keyboard, numpad, presentation, scanner.
4. Gamepad + drawing tablet (custom View for raw MotionEvents: hover, pressure, tilt, buttons).
5. Custom layouts editor/runner, device management, settings.
6. Build, install on the tablet over adb, smoke-test UI; pair with the PC and verify the HID
   devices appear in Windows.

## 5. Known limits
- Only one HID-device app can be active on the phone at a time.
- After changing the descriptor (app update that touches it) → un-pair and re-pair on the PC.
- macOS has no generic pen-digitizer driver (pen acts as nothing/absolute pointer there).
- XInput games need Steam Input or a remapper for the gamepad.

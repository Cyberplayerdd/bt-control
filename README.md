# BT Control

**Turn an Android phone or tablet into a Bluetooth keyboard, mouse, remote, gamepad and
pressure-sensitive drawing tablet for your computer.**

No software or driver is needed on the PC: the phone presents itself as a standard Bluetooth HID
device, exactly like a real keyboard, mouse, game controller or Wacom-style pen. Pair it once and
it works on Windows, Linux, ChromeOS and Android TV (macOS works except for the pen).

Requires Android 9 or newer with the Bluetooth *HID Device* profile (most phones and tablets).

## Main features

- **🖱️ Mouse & keyboard** — a laptop-style touchpad (tap to click, two fingers to scroll and
  right-click, tap-and-drag), your phone's keyboard typing straight onto the PC, and an
  **air mouse**: move the phone to move the cursor.
- **⌨️ Full computer keyboard** — choose the size from **40 % to full size with numpad**, captioned
  in your PC's layout (AZERTY, QWERTZ, QWERTY…). Keys **light up with the character they will type**
  when Shift, AltGr or Fn is active.
- **✏️ Drawing tablet** — use a tablet with a stylus (e.g. Samsung S Pen) as a Wacom-style pen
  tablet: **4096 pressure levels, tilt, hover**, side button and eraser, through Windows Ink.
  Works with Krita, Photoshop, Clip Studio Paint, OneNote… Also works **without a stylus**.
- **🎮 Gamepad** — two analog sticks, D-pad, A/B/X/Y, bumpers and triggers; Windows sees a real
  game controller.
- **📺 Remote & multimedia** — a TV-style remote for watching things on the PC (navigation pad, OK,
  back, volume and channel rockers, play/pause) plus a full media-key panel.
- **🎞️ Presentation remote** — big next/previous buttons, **laser pointer** by pointing the phone,
  volume keys to change slides, timer.

## All features

**Mouse & keyboard tab**
- Touchpad: 1 finger moves, tap = left click, 2-finger tap = right click, 3-finger tap = middle
  click, tap then drag = select/drag, 2-finger scroll (vertical and horizontal)
- Optional scroll strip and left/middle/right buttons; *touchpad only* mode
- Touchpad options: sensitivity, acceleration, scroll amount, natural scrolling, tap to click
- Phone keyboard input (with Backspace, Enter, arrows…), *Send text* dialog for long texts
- Built-in compact keyboard: keys row, 40 %, 60 %, 65 % or 75 %
- Air mouse (gyroscope) with **calibration** to remove drift
- Hardware keyboards plugged into the phone/tablet are passed through key for key

**Computer keyboard tab**
- Sizes 40 % · 60 % · 65 % · 75 % · TKL · Full (with numpad)
- Captions in the PC layout: Français (AZERTY), Belge, Suisse romand, English US/UK, Deutsch,
  Español, Italiano, Ελληνικά
- Live preview of what each key types with Shift / AltGr / Caps Lock / Fn
- Sticky modifiers (tap = once, tap twice = lock, hold = hold), Fn layer (F-keys, arrows, nav)
- Num / Caps / Scroll Lock lights reported by the PC

**Numeric keypad tab** — real keypad keys (Alt codes, spreadsheets, calculators), Num Lock state

**Multimedia tab** — play/pause, previous/next, stop, rewind/forward, volume ±, mute,
screen brightness ±, sleep, power button, browser back/forward/refresh/home/search, launch the
browser, mail, calculator and file explorer; icons only or with names

**Remote tab** — navigation pad with OK, back, home (Start menu), menu, VOL and CH rockers,
play/pause, stop, previous/next, rewind/forward, fullscreen, mute, sleep; air-mouse pointer mode
(OK clicks) with calibration

**Presentation tab** — next/previous (arrows, Page Up/Down or Space/Backspace), start (F5 /
Shift+F5), black/white screen, end; laser pointer (PowerPoint Ctrl+L) moved by pointing the phone;
pen / erase ink; volume buttons change slides; timer with vibration alert

**Code scanner tab** — reads barcodes and QR codes with the camera (offline) and types them on the
PC like a USB scanner, followed by Enter / Tab / Space; history, resend, copy, torch

**Gamepad tab** — 2 analog sticks (double-tap = L3/R3), 8-way D-pad, A/B/X/Y, LB/RB, LT/RT,
Back/Start/Home; tilt-to-steer; floating sticks; landscape and portrait layouts

**Drawing tablet tab** — pressure, tilt, hover, side button (right click or eraser), eraser tip;
pen area = whole screen or a chosen aspect ratio (16:9, 16:10, 21:9, 32:9, 3:2, 4:3); pressure curve
and minimum pressure; palm rejection; finger modes (draw, move cursor + tap to click, gestures);
finger gestures (2-finger scroll, pinch = zoom, 2-finger tap = undo, 3-finger tap = redo); optional
shortcut toolbar (undo/redo, pan, Ctrl/Shift/Alt, brush size, zoom); fading ink preview

**Custom layouts** — build your own button grids: key shortcuts, text snippets, media keys, mouse
buttons, sleep/power, **macros** (sequences with delays), touchpads and scroll strips; colours,
sizes, landscape mode; import/export (JSON), share, duplicate; three examples included

**Everywhere**
- Typing in any PC layout, including accented letters (dead keys) and Alt codes for the rest
- Shortcuts are layout-aware (Ctrl+Z is always *undo*, even on AZERTY)
- Every tab rotates (auto / lock landscape / lock portrait per tab) and can go full screen
- Bluetooth devices screen: make visible, search and pair, connect/disconnect, forget
- Reconnects automatically to the last computer; stays connected in the background
- Light/dark theme, dynamic colours, haptic feedback, keep-screen-on, adjustable report rate

## Rotation and full screen

Every tab has a portrait and a landscape layout and follows the screen by default. The rotation
button in the top bar opens **Auto-rotate · Lock landscape · Lock portrait** for the current tab
(remembered per tab). On large tablets Android may *letterbox* a locked tab instead of rotating
the display — auto-rotate is usually the better choice there. The ⛶ button makes any tab full
screen. Full-screen tabs (gamepad, drawing tablet) only show a small **notch** at the top:
tap it for the menu, connection status, rotation and that tab's options. Other tabs show their
options in the top bar (e.g. *Show button names* on Multimedia).

## Pairing with a Windows PC

1. **If the phone/tablet was already paired with this PC** (for file transfer, hotspot…), remove it
   first in *Settings › Bluetooth & devices* on the PC. Windows only discovers that the device is a
   keyboard/mouse/pen at pairing time.
2. Open BT Control → ☰ → **Bluetooth devices** → **Make visible for 2 minutes**.
3. On the PC: **Settings › Bluetooth & devices › Add device › Bluetooth** → pick your phone/tablet.
4. Confirm the pairing code on both screens. The status chip turns green: *Connected*.

Next time, just open the app: it reconnects to the last computer automatically (or tap *Connect*
in *Bluetooth devices*). Keep it connected in the background via the notification; *Quit* in the
notification releases Bluetooth.

> If the HID report descriptor ever changes (app update that says so), remove the device on the
> PC and pair again — Windows caches the descriptor from the first pairing.

## Keyboard layout matters

Bluetooth keyboards send **key positions**, not characters. The PC turns them into characters using
*its* layout. Choose the PC's layout in Settings (or with the **FR** chip on the Mouse screen):
Français (AZERTY), Belge, Suisse romand, English US/UK, Deutsch, Español, Italiano, Ελληνικά.
Accented letters use the layout's dead keys (ê = ^ then e). Characters that don't exist on the
layout (É on French AZERTY, emoji…) are typed with **Alt codes** on Windows (Settings lets you pick
Alt+hex or Linux Ctrl+Shift+U instead).

Shortcuts in custom layouts and in the app (Ctrl+Z, Ctrl+C…) are layout-aware: on AZERTY, Ctrl+Z is
sent on the key that carries the Z, so it is always *undo*.

## Drawing tablet tips (Windows)

- Works with any Windows Ink app: Krita (*Settings › Tablet › Windows 8+ Pointer Input*), Photoshop,
  Clip Studio Paint, MediBang, OneNote, Whiteboard, Paint…
- The pen area maps to the **whole primary screen**. With several monitors, choose which one the pen
  uses in *Control Panel › Tablet PC Settings › Setup › Pen input*. Set the area shape to your
  monitor's aspect ratio in the app's settings so circles stay round.
- To stop Windows from treating a long press as right-click: *Settings › Bluetooth & devices › Pen
  & Windows Ink* → turn off *press and hold to right-click*, and turn off the pen visual feedback /
  "show cursor" options as you like.
- Samsung S Pen: if pressing the side button while hovering opens *Air command*, disable it in
  *Settings › Advanced features › S Pen*.
- Finger gestures on the pen area are ignored while the pen is near the screen (palm rejection).

## Gamepad tips

Windows sees a standard HID game controller (*Win+R → joy.cpl* to test). Games that only support
Xbox (XInput) controllers: add the game to **Steam** and enable *Steam Input* for generic gamepads,
or use a remapper such as x360ce.

## Troubleshooting

| Problem | Fix |
|---|---|
| "HID registration failed" | Another Bluetooth keyboard/mouse app holds the HID profile. Close/force-stop it, toggle Bluetooth, tap *Retry*. |
| Paired but never connects | Remove the device on the PC, pair again while the app is open. |
| Wrong characters typed | The PC layout in Settings doesn't match the layout active on the PC. |
| Lost characters in long texts | Raise *Delay between key reports* (Settings › Computer keyboard). |
| Laggy mouse/pen | Move closer, avoid Wi-Fi 2.4 GHz congestion, or raise *Report interval* to 10–12 ms. |

## Building

```
gradlew assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
gradlew assembleRelease    # app/build/outputs/apk/release/app-release.apk (minified, debug-signed)
```

JDK 17+ (tested with JDK 21), Android SDK 36. See [PLAN.md](PLAN.md) for the research notes and the
HID report map.

### Project layout

```
app/src/main/java/com/btcontrol/app/
  hid/   HidDescriptor (composite report descriptor), ReportSender (queue, coalescing, pacing),
         HidController (keyboard/mouse/consumer/gamepad/pen state), KeyboardLayouts, AndroidKeyMap
  bt/    BluetoothHidManager (profile proxy, registerApp, connect, pairing), HidService (foreground)
  data/  AppSettings, CustomLayouts (models, repository, action runner, examples)
  ui/    MainActivity, AppNav (drawer), components (HoldButton, Touchpad, KeyInputView, forms),
         screens (one file per screen; PenInputView = raw stylus handling)
```

## License

BT Control is free software: you can redistribute it and/or modify it under the terms of the
[GNU General Public License v3.0](LICENSE). Apps built from this code must also be released under
the GPL.

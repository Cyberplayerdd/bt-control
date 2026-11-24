package com.btcontrol.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class UnknownCharMode { SKIP, WINDOWS_ALT_CODES, WINDOWS_HEX, LINUX_CTRL_SHIFT_U }
enum class PresentationKeys { ARROWS, PAGE_UP_DOWN, SPACE_BACKSPACE }
enum class PenButtonAction { RIGHT_CLICK, ERASER, NONE }
enum class FingerMode { IGNORE, GESTURES, DRAW, HOVER }
enum class ScanSuffix { NONE, ENTER, TAB, SPACE }

@Serializable
data class Settings(
    // General
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val haptics: Boolean = true,
    val keepScreenOn: Boolean = true,
    val keepInBackground: Boolean = true,
    val autoConnect: Boolean = true,
    val lastDeviceAddress: String? = null,
    val reportIntervalMs: Int = 8,
    val lastScreen: String = "mouse",
    /** Saved-settings format version (see AppSettings.migrate). */
    val settingsVersion: Int = 0,
    /** Per-tab orientation: auto (follows the sensor, like the gamepad), landscape, portrait. */
    val orientations: Map<String, String> = emptyMap(),
    val mediaShowLabels: Boolean = false,
    val welcomeSeen: Boolean = false,

    // Keyboard
    val keyboardLayout: String = "fr",
    val unknownCharMode: UnknownCharMode = UnknownCharMode.WINDOWS_ALT_CODES,
    val typingDelayMs: Int = 2,
    /** Size of the "Computer keyboard" tab: 40, 60, 65, 75, tkl, full. */
    val keyboardSize: String = "75",
    /** Built-in keyboard on the mouse tab: off, row, 40, 60, 65, 75. */
    val compactKeyboard: String = "off",

    // Mouse
    val pointerSpeed: Float = 1.0f,
    val pointerAcceleration: Float = 1.0f,
    val scrollSpeed: Float = 1.0f,
    val naturalScroll: Boolean = true,
    val tapToClick: Boolean = true,
    val showScrollStrip: Boolean = true,
    val showTouchpadHint: Boolean = false,
    val showMouseButtons: Boolean = true,
    val touchpadOnly: Boolean = false,
    val airMouseSpeed: Float = 1.0f,
    /** Gyroscope offset measured by "Calibrate air mouse" (rad/s). */
    val gyroBiasX: Float = 0f,
    val gyroBiasY: Float = 0f,
    val gyroBiasZ: Float = 0f,

    // Presentation
    val presentationKeys: PresentationKeys = PresentationKeys.ARROWS,
    val presentationVolumeKeys: Boolean = true,
    val presentationAlertMinutes: Int = 0,

    // Drawing tablet
    val tabletAspect: String = "fill",
    val pressureCurve: Float = 1.0f,
    val minPressure: Float = 0.0f,
    val penButtonAction: PenButtonAction = PenButtonAction.RIGHT_CLICK,
    val fingerMode: FingerMode = FingerMode.GESTURES,
    val showInkTrail: Boolean = true,
    val sendTilt: Boolean = true,
    val showTabletToolbar: Boolean = false,

    // Gamepad
    val gamepadTiltSteering: Boolean = false,
    val gamepadFloatingSticks: Boolean = false,
    val gamepadTiltSensitivity: Float = 1.0f,

    // Wiimote
    val wiimoteNunchuk: Boolean = false,
    val wiimotePointer: Boolean = true,
    val wiimoteTilt: Boolean = true,
    /** Also move the PC mouse with the Wiimote pointer (the pointer is always sent on Rx/Ry). */
    val wiimoteMouse: Boolean = false,

    // Code scanner
    val scanSuffix: ScanSuffix = ScanSuffix.ENTER,
    val scanAutoSend: Boolean = true,
)

/** Settings persisted as one JSON blob in SharedPreferences and exposed as a StateFlow. */
class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _flow = MutableStateFlow(load())
    val flow: StateFlow<Settings> = _flow.asStateFlow()
    val value: Settings get() = _flow.value

    private fun load(): Settings {
        val loaded = try {
            prefs.getString(KEY, null)?.let { json.decodeFromString(Settings.serializer(), it) } ?: Settings()
        } catch (_: Exception) {
            Settings()
        }
        return migrate(loaded)
    }

    /** One-time changes of defaults for settings saved by older versions. */
    private fun migrate(s: Settings): Settings {
        var m = s
        if (m.settingsVersion < 2) {
            // v2: pen area uses the whole screen, toolbar off by default, simpler mouse tab.
            m = m.copy(tabletAspect = "fill", showTabletToolbar = false, showTouchpadHint = false)
        }
        if (m.settingsVersion < 3) {
            // v3: all tabs follow the screen rotation by default; drop locks set with the old button.
            m = m.copy(orientations = emptyMap())
        }
        if (m.settingsVersion != CURRENT_VERSION) {
            m = m.copy(settingsVersion = CURRENT_VERSION)
            prefs.edit().putString(KEY, json.encodeToString(Settings.serializer(), m)).apply()
        }
        return m
    }

    fun update(block: (Settings) -> Settings) {
        val next = block(_flow.value)
        if (next == _flow.value) return
        _flow.value = next
        prefs.edit().putString(KEY, json.encodeToString(Settings.serializer(), next)).apply()
    }

    private companion object {
        const val KEY = "settings_json"
        const val CURRENT_VERSION = 3
    }
}

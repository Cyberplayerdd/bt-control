package com.btcontrol.app.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.clickable
import com.btcontrol.app.R
import com.btcontrol.app.t
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.btcontrol.app.BtControlApp
import com.btcontrol.app.bt.HidService
import com.btcontrol.app.data.FingerMode
import com.btcontrol.app.data.PenButtonAction
import com.btcontrol.app.data.PresentationKeys
import com.btcontrol.app.data.ThemeMode
import com.btcontrol.app.data.UnknownCharMode
import com.btcontrol.app.hid.HidDescriptor
import com.btcontrol.app.hid.KeyboardLayout
import com.btcontrol.app.ui.components.ChoiceRow
import com.btcontrol.app.ui.components.SectionHeader
import com.btcontrol.app.ui.components.SliderRow
import com.btcontrol.app.ui.components.SwitchRow
import androidx.compose.ui.platform.LocalContext
import kotlin.math.roundToInt

@Composable
fun SettingsScreen() {
    val app = BtControlApp.instance
    val s by app.settings.flow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    fun set(block: (com.btcontrol.app.data.Settings) -> com.btcontrol.app.data.Settings) = app.settings.update(block)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 24.dp)) {
        SectionHeader(t(R.string.nav_keyboard))
        ChoiceRow(
            t(R.string.keyboard_layout_of_the_computer), s.keyboardLayout,
            KeyboardLayout.ALL.map { it.id to it.name },
            subtitle = t(R.string.must_match_the_layout_selected_on_the_pc_o),
        ) { v -> set { it.copy(keyboardLayout = v) } }
        ChoiceRow(
            t(R.string.characters_missing_from_that_layout), s.unknownCharMode,
            listOf(
                UnknownCharMode.WINDOWS_ALT_CODES to t(R.string.windows_alt_codes_alt_numpad),
                UnknownCharMode.WINDOWS_HEX to t(R.string.windows_alt_hex_needs_enablehexnumpad),
                UnknownCharMode.LINUX_CTRL_SHIFT_U to t(R.string.linux_ctrl_shift_u),
                UnknownCharMode.SKIP to t(R.string.skip_them),
            ),
        ) { v -> set { it.copy(unknownCharMode = v) } }
        SliderRow(t(R.string.delay_between_key_reports), s.typingDelayMs.toFloat(), 0f..30f, { "${it.roundToInt()} ms" }, steps = 29,
            subtitle = t(R.string.increase_if_some_characters_are_lost_when)) { v -> set { it.copy(typingDelayMs = v.roundToInt()) } }
        ChoiceRow(
            t(R.string.computer_keyboard_size), s.keyboardSize, com.btcontrol.app.ui.components.KeyboardSizes.ALL,
        ) { v -> set { it.copy(keyboardSize = v) } }
        ChoiceRow(
            t(R.string.built_in_keyboard_on_the_mouse_tab), s.compactKeyboard,
            listOf("off" to t(R.string.off), "row" to t(R.string.keys_row)) + com.btcontrol.app.ui.components.KeyboardSizes.ALL.take(4),
        ) { v -> set { it.copy(compactKeyboard = v) } }

        SectionHeader(t(R.string.mouse_touchpad))
        SliderRow(t(R.string.pointer_speed), s.pointerSpeed, 0.2f..4f, { "%.1f×".format(it) }) { v -> set { it.copy(pointerSpeed = v) } }
        SliderRow(t(R.string.acceleration), s.pointerAcceleration, 0f..3f, { "%.1f".format(it) }) { v -> set { it.copy(pointerAcceleration = v) } }
        SliderRow(t(R.string.scroll_speed), s.scrollSpeed, 0.2f..4f, { "%.1f×".format(it) }) { v -> set { it.copy(scrollSpeed = v) } }
        SwitchRow(t(R.string.natural_scrolling), s.naturalScroll, t(R.string.content_follows_your_fingers_like_on_the_p)) { v -> set { it.copy(naturalScroll = v) } }
        SwitchRow(t(R.string.tap_to_click), s.tapToClick) { v -> set { it.copy(tapToClick = v) } }
        SwitchRow(t(R.string.show_scroll_strip), s.showScrollStrip) { v -> set { it.copy(showScrollStrip = v) } }
        SliderRow(t(R.string.air_mouse_laser_speed), s.airMouseSpeed, 0.2f..4f, { "%.1f×".format(it) }) { v -> set { it.copy(airMouseSpeed = v) } }

        SectionHeader(t(R.string.nav_presentation))
        ChoiceRow(
            t(R.string.next_previous_keys), s.presentationKeys,
            listOf(
                PresentationKeys.ARROWS to t(R.string.powerpoint_google_slides_keynote),
                PresentationKeys.PAGE_UP_DOWN to t(R.string.page_down_page_up_most_clickers),
                PresentationKeys.SPACE_BACKSPACE to t(R.string.space_backspace),
            ),
        ) { v -> set { it.copy(presentationKeys = v) } }
        SwitchRow(t(R.string.volume_buttons_change_slides), s.presentationVolumeKeys) { v -> set { it.copy(presentationVolumeKeys = v) } }
        SliderRow(t(R.string.vibrate_after), s.presentationAlertMinutes.toFloat(), 0f..90f, { if (it < 1f) "off" else "${it.roundToInt()} min" }, steps = 17) { v ->
            set { it.copy(presentationAlertMinutes = v.roundToInt()) }
        }

        SectionHeader(t(R.string.nav_tablet))
        ChoiceRow(
            t(R.string.pen_area_shape_match_your_pc_screen), s.tabletAspect,
            listOf("16:9" to t(R.string.n16_9_most_monitors), "16:10" to "16:10", "21:9" to t(R.string.n21_9_ultrawide), "32:9" to t(R.string.n32_9_super_ultrawide),
                "3:2" to t(R.string.n3_2_surface), "4:3" to "4:3", "fill" to t(R.string.whole_tablet_screen_biggest)),
        ) { v -> set { it.copy(tabletAspect = v) } }
        SliderRow(t(R.string.pressure_curve), s.pressureCurve, 0.3f..3f, {
            when {
                it < 0.8f -> t(R.string.soft_1f).format(it)
                it > 1.25f -> t(R.string.firm_1f).format(it)
                else -> t(R.string.linear_1f).format(it)
            }
        }, subtitle = t(R.string.soft_light_strokes_get_thick_quickly_firm)) { v -> set { it.copy(pressureCurve = v) } }
        SliderRow(t(R.string.minimum_pressure), s.minPressure, 0f..0.5f, { "${(it * 100).roundToInt()} %" }) { v -> set { it.copy(minPressure = v) } }
        ChoiceRow(
            t(R.string.pen_side_button), s.penButtonAction,
            listOf(PenButtonAction.RIGHT_CLICK to t(R.string.barrel_button_right_click), PenButtonAction.ERASER to t(R.string.eraser_while_held), PenButtonAction.NONE to t(R.string.nothing)),
        ) { v -> set { it.copy(penButtonAction = v) } }
        ChoiceRow(
            t(R.string.fingers_on_the_pen_area), s.fingerMode,
            listOf(
                FingerMode.GESTURES to t(R.string.gestures_2_finger_scroll_pinch_zoom_2_fing),
                FingerMode.IGNORE to t(R.string.ignore_palm_rejection),
                FingerMode.DRAW to t(R.string.draw_with_a_finger_no_stylus_needed),
                FingerMode.HOVER to t(R.string.move_the_cursor_with_a_finger_tap_to_click),
            ),
        ) { v -> set { it.copy(fingerMode = v) } }
        SwitchRow(t(R.string.send_pen_tilt), s.sendTilt) { v -> set { it.copy(sendTilt = v) } }
        SwitchRow(t(R.string.show_ink_preview_on_the_tablet), s.showInkTrail) { v -> set { it.copy(showInkTrail = v) } }
        SwitchRow(t(R.string.show_shortcut_toolbar), s.showTabletToolbar) { v -> set { it.copy(showTabletToolbar = v) } }

        SectionHeader(t(R.string.nav_media))
        SwitchRow(t(R.string.show_button_names), s.mediaShowLabels) { v -> set { it.copy(mediaShowLabels = v) } }

        SectionHeader(t(R.string.nav_gamepad))
        SwitchRow(t(R.string.tilt_to_steer_left_stick), s.gamepadTiltSteering) { v -> set { it.copy(gamepadTiltSteering = v) } }
        SliderRow(t(R.string.tilt_sensitivity), s.gamepadTiltSensitivity, 0.3f..3f, { "%.1f×".format(it) }) { v -> set { it.copy(gamepadTiltSensitivity = v) } }
        SwitchRow(t(R.string.floating_sticks), s.gamepadFloatingSticks, t(R.string.stick_centre_is_wherever_your_thumb_lands)) { v -> set { it.copy(gamepadFloatingSticks = v) } }

        SectionHeader(t(R.string.rotation))
        Text(
            t(R.string.each_tab_follows_the_device_s_rotation_sen),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        androidx.compose.material3.TextButton(onClick = { set { it.copy(orientations = emptyMap()) } }, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(t(R.string.reset_all_tabs_to_default_rotation))
        }

        SectionHeader(t(R.string.general))
        LanguageRow()
        ChoiceRow(t(R.string.theme), s.theme, listOf(ThemeMode.SYSTEM to t(R.string.system), ThemeMode.LIGHT to t(R.string.light), ThemeMode.DARK to t(R.string.dark))) { v -> set { it.copy(theme = v) } }
        SwitchRow(t(R.string.dynamic_colours), s.dynamicColor) { v -> set { it.copy(dynamicColor = v) } }
        SwitchRow(t(R.string.vibrate_on_key_press), s.haptics) { v -> set { it.copy(haptics = v) } }
        SwitchRow(t(R.string.keep_screen_on), s.keepScreenOn) { v -> set { it.copy(keepScreenOn = v) } }
        SwitchRow(t(R.string.stay_connected_in_background), s.keepInBackground, t(R.string.shows_a_notification_while_active)) { v ->
            set { it.copy(keepInBackground = v) }
            if (v) HidService.start(context) else HidService.stop(context)
        }
        SwitchRow(t(R.string.reconnect_to_the_last_computer_on_start), s.autoConnect) { v -> set { it.copy(autoConnect = v) } }
        SliderRow(
            t(R.string.report_interval), s.reportIntervalMs.toFloat(), 4f..20f, { "${it.roundToInt()} ms (${(1000f / it).roundToInt()} Hz)" }, steps = 15,
            subtitle = t(R.string.mouse_pen_and_gamepad_update_rate_lower_sm),
        ) { v -> set { it.copy(reportIntervalMs = v.roundToInt()) } }

        SectionHeader(t(R.string.about))
        Text(
            t(R.string.bt_control_1_0_bluetooth_hid_keyboard_mous) +
                t(R.string.descriptor_bytes, HidDescriptor.DESCRIPTOR.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

/** Opens Android's per-app language page (Android 13+); older versions follow the phone's language. */
@Composable
private fun LanguageRow() {
    val context = LocalContext.current
    val modern = android.os.Build.VERSION.SDK_INT >= 33
    val current = java.util.Locale.getDefault().let { it.getDisplayName(it).replaceFirstChar { c -> c.titlecase(it) } }
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth()
            .clickable(enabled = modern) {
                runCatching {
                    context.startActivity(
                        android.content.Intent(
                            android.provider.Settings.ACTION_APP_LOCALE_SETTINGS,
                            android.net.Uri.fromParts("package", context.packageName, null),
                        )
                    )
                }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column {
            Text(t(R.string.language), style = MaterialTheme.typography.bodyLarge)
            Text(current, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            Text(
                t(if (modern) R.string.language_summary_system else R.string.language_summary_old),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

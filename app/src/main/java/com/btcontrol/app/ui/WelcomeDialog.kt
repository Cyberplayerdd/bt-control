package com.btcontrol.app.ui

import com.btcontrol.app.R
import com.btcontrol.app.t
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Mouse
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.SettingsRemote
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** First-launch explanation of what the app does (also opened from the menu: "About & help"). */
@Composable
fun WelcomeDialog(onPair: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(t(R.string.welcome_to_bt_control)) },
        text = {
            Column(
                Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    t(R.string.bt_control_turns_this_phone_or_tablet_into),
                )
                Feature(Icons.Outlined.Mouse, t(R.string.nav_mouse), t(R.string.touchpad_phone_keyboard_air_mouse))
                Feature(Icons.Outlined.Keyboard, t(R.string.nav_keyboard), t(R.string.from_40_to_full_size_in_your_pc_s_layout))
                Feature(Icons.Outlined.SettingsRemote, t(R.string.remote_multimedia), t(R.string.play_pause_volume_navigation_sleep))
                Feature(Icons.Outlined.Brush, t(R.string.nav_tablet), t(R.string.pen_pressure_tilt_and_hover_windows_ink))
                Feature(Icons.Outlined.SportsEsports, t(R.string.nav_gamepad), t(R.string.two_sticks_d_pad_buttons_and_triggers))
                Feature(Icons.Outlined.Slideshow, t(R.string.nav_presentation), t(R.string.next_previous_laser_pointer_timer))
                Feature(Icons.Outlined.QrCodeScanner, t(R.string.nav_scanner), t(R.string.types_barcodes_and_qr_codes_on_the_pc))
                Feature(Icons.Outlined.Widgets, t(R.string.custom_layouts), t(R.string.your_own_buttons_shortcuts_and_macros))
                Text(t(R.string.getting_started), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    t(R.string.n1_tap_pair_a_computer_and_make_this_device),
                )
            }
        },
        confirmButton = { Button(onClick = onPair) { Text(t(R.string.pair_a_computer)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(t(R.string.got_it)) } },
    )
}

@Composable
private fun Feature(icon: ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}


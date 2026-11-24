package com.btcontrol.app.ui

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
        title = { Text("Welcome to BT Control") },
        text = {
            Column(
                Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "BT Control turns this phone or tablet into a Bluetooth keyboard, mouse, remote, " +
                        "gamepad and drawing tablet for your computer. Nothing to install on the PC: " +
                        "it simply sees a real Bluetooth device.",
                )
                Feature(Icons.Outlined.Mouse, "Mouse & keyboard", "Touchpad, phone keyboard, air mouse")
                Feature(Icons.Outlined.Keyboard, "Computer keyboard", "From 40% to full size, in your PC's layout")
                Feature(Icons.Outlined.SettingsRemote, "Remote & multimedia", "Play/pause, volume, navigation, sleep")
                Feature(Icons.Outlined.Brush, "Drawing tablet", "Pen pressure, tilt and hover (Windows Ink)")
                Feature(Icons.Outlined.SportsEsports, "Gamepad", "Two sticks, D-pad, buttons and triggers")
                Feature(Icons.Outlined.Slideshow, "Presentation", "Next/previous, laser pointer, timer")
                Feature(Icons.Outlined.QrCodeScanner, "Code scanner", "Types barcodes and QR codes on the PC")
                Feature(Icons.Outlined.Widgets, "Custom layouts", "Your own buttons, shortcuts and macros")
                Text("Getting started", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "1. Tap “Pair a computer” and make this device visible.\n" +
                        "2. On the PC: Bluetooth settings › Add device › choose this device.\n" +
                        "3. Open the menu (☰) and pick what you want to control.",
                )
            }
        },
        confirmButton = { Button(onClick = onPair) { Text("Pair a computer") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Got it") } },
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


package com.btcontrol.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BluetoothSearching
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.btcontrol.app.BtControlApp
import com.btcontrol.app.bt.BtDeviceInfo
import com.btcontrol.app.bt.HidStatus
import com.btcontrol.app.ui.AppActions
import com.btcontrol.app.ui.StatusDot
import com.btcontrol.app.ui.statusText

@Composable
fun DevicesScreen(actions: AppActions) {
    val bt = BtControlApp.instance.bt
    val state by bt.state.collectAsStateWithLifecycle()
    val bonded by bt.bondedDevices.collectAsStateWithLifecycle()
    val discovered by bt.discoveredDevices.collectAsStateWithLifecycle()
    val discovering by bt.discovering.collectAsStateWithLifecycle()
    var showAll by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { bt.refreshBonded() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ---------------- status
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(state)
                    Spacer(Modifier.width(10.dp))
                    Text(statusText(state), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                state.message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Text(
                    "This device appears to computers as “${bt.adapterName}”.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (state.status) {
                        HidStatus.CONNECTED -> OutlinedButton(onClick = { bt.disconnect() }) { Text("Disconnect") }
                        HidStatus.BLUETOOTH_OFF -> Button(onClick = actions.enableBluetooth) { Text("Turn on Bluetooth") }
                        HidStatus.NO_PERMISSION -> Button(onClick = actions.requestPermissions) { Text("Grant permission") }
                        HidStatus.REGISTRATION_FAILED, HidStatus.UNSUPPORTED -> Button(onClick = { bt.reRegister() }) { Text("Retry") }
                        else -> Unit
                    }
                    if (state.status == HidStatus.READY || state.status == HidStatus.CONNECTED) {
                        TextButton(onClick = { bt.reRegister() }) { Text("Restart HID") }
                    }
                }
            }
        }

        // ---------------- pairing
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Pair a new computer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "1. If this device is already paired with the computer (for example for file transfer), remove it in the computer's Bluetooth settings first — the computer only learns that this is a keyboard/mouse/pen when it pairs.\n" +
                        "2. Make this device visible:",
                    style = MaterialTheme.typography.bodyMedium,
                )
                FilledTonalButton(onClick = actions.makeDiscoverable) {
                    Icon(Icons.Outlined.Visibility, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Make visible for 2 minutes")
                }
                Text(
                    "3. Windows: Settings › Bluetooth & devices › Add device › Bluetooth, then choose “${bt.adapterName}”. " +
                        "macOS: System Settings › Bluetooth. Linux: your Bluetooth settings.\n" +
                        "4. Confirm the code on both screens. The status above turns green when connected.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("…or search for the computer from here", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    if (discovering) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        TextButton(onClick = { bt.cancelDiscovery() }) { Text("Stop") }
                    } else {
                        TextButton(onClick = { if (!bt.startDiscovery()) actions.requestPermissions() }) {
                            Icon(Icons.Outlined.BluetoothSearching, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Search")
                        }
                    }
                }
                val found = discovered.filter { !it.bonded && (showAll || it.isComputer) }
                for (d in found) {
                    DeviceRow(d, connected = false, onConnect = { bt.pairAndConnect(d.device) }, connectLabel = "Pair")
                }
                if (discovered.isNotEmpty()) {
                    TextButton(onClick = { showAll = !showAll }) {
                        Text(if (showAll) "Show computers only" else "Show all devices (${discovered.count { !it.bonded }})")
                    }
                }
            }
        }

        // ---------------- paired
        Text("Paired devices", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (bonded.isEmpty()) Text("No paired devices.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        val computers = bonded.filter { it.isComputer }
        val others = bonded.filterNot { it.isComputer }
        for (d in computers) {
            DeviceRow(
                d,
                connected = state.connected && state.deviceAddress == d.address,
                onConnect = { bt.connect(d.device) },
                onDisconnect = { bt.disconnect() },
                onUnpair = { bt.unpair(d.device) },
            )
        }
        if (others.isNotEmpty()) {
            Text(
                "Other devices (phones, headphones…) — they usually can't use a keyboard/mouse, but you can try:",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            for (d in others) {
                DeviceRow(
                    d,
                    connected = state.connected && state.deviceAddress == d.address,
                    onConnect = { bt.connect(d.device) },
                    onDisconnect = { bt.disconnect() },
                    onUnpair = { bt.unpair(d.device) },
                )
            }
        }
        Text(
            "Tips: only one app at a time can act as a Bluetooth keyboard on this device — close other keyboard/mouse apps. " +
                "After an app update that changes the device description, remove the pairing on the computer and pair again.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DeviceRow(
    d: BtDeviceInfo,
    connected: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit = {},
    onUnpair: (() -> Unit)? = null,
    connectLabel: String = "Connect",
) {
    var menu by remember { mutableStateOf(false) }
    ListItem(
        leadingContent = { Icon(if (d.isComputer) Icons.Outlined.Computer else Icons.Outlined.Devices, null) },
        headlineContent = { Text(d.name) },
        supportingContent = { Text(if (connected) "Connected" else d.address) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (connected) OutlinedButton(onClick = onDisconnect) { Text("Disconnect") }
                else FilledTonalButton(onClick = onConnect) { Text(connectLabel) }
                if (onUnpair != null) {
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "More") }
                        DropdownMenu(menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("Forget (unpair)") }, onClick = { menu = false; onUnpair() })
                        }
                    }
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

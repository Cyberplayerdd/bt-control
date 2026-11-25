package com.btcontrol.app.ui.screens

import com.btcontrol.app.R
import com.btcontrol.app.t
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
                    t(R.string.device_appears_as, bt.adapterName),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (state.status) {
                        HidStatus.CONNECTED -> OutlinedButton(onClick = { bt.disconnect() }) { Text(t(R.string.disconnect)) }
                        HidStatus.BLUETOOTH_OFF -> Button(onClick = actions.enableBluetooth) { Text(t(R.string.turn_on_bluetooth)) }
                        HidStatus.NO_PERMISSION -> Button(onClick = actions.requestPermissions) { Text(t(R.string.grant_permission)) }
                        HidStatus.REGISTRATION_FAILED, HidStatus.UNSUPPORTED -> Button(onClick = { bt.reRegister() }) { Text(t(R.string.retry)) }
                        else -> Unit
                    }
                    if (state.status == HidStatus.READY || state.status == HidStatus.CONNECTED) {
                        TextButton(onClick = { bt.reRegister() }) { Text(t(R.string.restart_hid)) }
                    }
                }
            }
        }

        // ---------------- pairing
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(t(R.string.pair_a_new_computer), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    t(R.string.n1_if_this_device_is_already_paired_with_th),
                    style = MaterialTheme.typography.bodyMedium,
                )
                FilledTonalButton(onClick = actions.makeDiscoverable) {
                    Icon(Icons.Outlined.Visibility, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(t(R.string.make_visible_for_2_minutes))
                }
                Text(
                    t(R.string.pair_step3, bt.adapterName) +
                        t(R.string.macos_system_settings_bluetooth_linux_your),
                    style = MaterialTheme.typography.bodyMedium,
                )
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t(R.string.or_search_for_the_computer_from_here), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    if (discovering) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        TextButton(onClick = { bt.cancelDiscovery() }) { Text(t(R.string.stop)) }
                    } else {
                        TextButton(onClick = { if (!bt.startDiscovery()) actions.requestPermissions() }) {
                            Icon(Icons.Outlined.BluetoothSearching, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(t(R.string.search))
                        }
                    }
                }
                val found = discovered.filter { !it.bonded && (showAll || it.isComputer) }
                for (d in found) {
                    DeviceRow(d, connected = false, onConnect = { bt.pairAndConnect(d.device) }, connectLabel = t(R.string.pair))
                }
                if (discovered.isNotEmpty()) {
                    TextButton(onClick = { showAll = !showAll }) {
                        Text(if (showAll) t(R.string.show_computers_only) else t(R.string.show_all_devices, discovered.count { !it.bonded }))
                    }
                }
            }
        }

        // ---------------- paired
        Text(t(R.string.paired_devices), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (bonded.isEmpty()) Text(t(R.string.no_paired_devices), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                t(R.string.other_devices_phones_headphones_they_usual),
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
            t(R.string.tips_only_one_app_at_a_time_can_act_as_a_b),
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
    connectLabel: String = t(R.string.connect),
) {
    var menu by remember { mutableStateOf(false) }
    ListItem(
        leadingContent = { Icon(if (d.isComputer) Icons.Outlined.Computer else Icons.Outlined.Devices, null) },
        headlineContent = { Text(d.name) },
        supportingContent = { Text(if (connected) t(R.string.connected) else d.address) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (connected) OutlinedButton(onClick = onDisconnect) { Text(t(R.string.disconnect)) }
                else FilledTonalButton(onClick = onConnect) { Text(connectLabel) }
                if (onUnpair != null) {
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, t(R.string.more)) }
                        DropdownMenu(menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text(t(R.string.forget_unpair)) }, onClick = { menu = false; onUnpair() })
                        }
                    }
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

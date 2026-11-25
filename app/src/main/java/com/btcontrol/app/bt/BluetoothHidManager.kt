package com.btcontrol.app.bt

import com.btcontrol.app.R
import com.btcontrol.app.t
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.btcontrol.app.data.AppSettings
import com.btcontrol.app.hid.HidController
import com.btcontrol.app.hid.HidDescriptor
import com.btcontrol.app.hid.ReportSender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors

enum class HidStatus {
    NO_BLUETOOTH,      // device has no Bluetooth adapter
    NO_PERMISSION,     // BLUETOOTH_CONNECT not granted
    BLUETOOTH_OFF,
    STARTING,          // waiting for the HID profile proxy / registration
    UNSUPPORTED,       // HID device profile not available on this phone
    REGISTRATION_FAILED,
    READY,             // registered, waiting for a computer
    CONNECTING,
    CONNECTED,
}

data class HidState(
    val status: HidStatus,
    val deviceName: String? = null,
    val deviceAddress: String? = null,
    val message: String? = null,
) {
    val connected get() = status == HidStatus.CONNECTED
}

data class BtDeviceInfo(
    val address: String,
    val name: String,
    val isComputer: Boolean,
    val bonded: Boolean,
    val device: BluetoothDevice,
)

/**
 * Owns the Bluetooth HID Device profile: registers the app as a HID peripheral, tracks the
 * connection to the computer, and exposes pairing / discovery helpers.
 */
@SuppressLint("MissingPermission")
class BluetoothHidManager(
    private val context: Context,
    private val settings: AppSettings,
) {
    private val main = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private val adapter: BluetoothAdapter? =
        context.getSystemService(BluetoothManager::class.java)?.adapter

    @Volatile private var hidDevice: BluetoothHidDevice? = null
    @Volatile private var connectedDevice: BluetoothDevice? = null
    @Volatile private var registered = false
    private var started = false
    private var receiversRegistered = false
    private var proxyRequested = false
    private var pendingConnectAddress: String? = null

    private val _state = MutableStateFlow(HidState(HidStatus.STARTING))
    val state: StateFlow<HidState> = _state.asStateFlow()

    private val _bonded = MutableStateFlow<List<BtDeviceInfo>>(emptyList())
    val bondedDevices: StateFlow<List<BtDeviceInfo>> = _bonded.asStateFlow()

    private val _discovered = MutableStateFlow<List<BtDeviceInfo>>(emptyList())
    val discoveredDevices: StateFlow<List<BtDeviceInfo>> = _discovered.asStateFlow()

    private val _discovering = MutableStateFlow(false)
    val discovering: StateFlow<Boolean> = _discovering.asStateFlow()

    val sender = ReportSender(
        transport = { id, data -> sendReport(id, data) },
        isConnected = { connectedDevice != null && hidDevice != null },
    )
    val hid = HidController(sender, settings)

    val adapterName: String get() = runCatching { adapter?.name }.getOrNull() ?: t(R.string.this_device)

    fun hasConnectPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    fun hasScanPermission(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
    else ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    val isBluetoothOn: Boolean get() = adapter?.isEnabled == true

    // ------------------------------------------------------------------ lifecycle

    /** Idempotent: call whenever the UI comes to the foreground or permissions change. */
    fun start() {
        sender.minIntervalMs = settings.value.reportIntervalMs
        if (adapter == null) {
            _state.value = HidState(HidStatus.NO_BLUETOOTH)
            return
        }
        if (!hasConnectPermission()) {
            _state.value = HidState(HidStatus.NO_PERMISSION)
            return
        }
        registerReceivers()
        started = true
        refreshBonded()
        if (!adapter.isEnabled) {
            _state.value = HidState(HidStatus.BLUETOOTH_OFF)
            return
        }
        if (hidDevice == null && !proxyRequested) {
            _state.value = HidState(HidStatus.STARTING)
            proxyRequested = adapter.getProfileProxy(context, serviceListener, BluetoothProfile.HID_DEVICE)
            if (!proxyRequested) {
                _state.value = HidState(HidStatus.UNSUPPORTED, message = t(R.string.this_phone_does_not_expose_the_bluetooth_h))
            }
        } else if (hidDevice != null && !registered) {
            registerApp()
        }
    }

    /** Unregister and release everything (used by "Quit"). */
    fun stop() {
        started = false
        runCatching { if (_discovering.value) adapter?.cancelDiscovery() }
        disconnect()
        val h = hidDevice
        if (h != null) {
            runCatching { if (registered) h.unregisterApp() }
            runCatching { adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, h) }
        }
        hidDevice = null
        registered = false
        proxyRequested = false
        connectedDevice = null
        if (receiversRegistered) {
            runCatching { context.unregisterReceiver(receiver) }
            receiversRegistered = false
        }
        _state.value = HidState(HidStatus.STARTING, message = t(R.string.stopped))
    }

    private val serviceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            hidDevice = proxy as BluetoothHidDevice
            proxyRequested = false
            registerApp()
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            hidDevice = null
            registered = false
            proxyRequested = false
            setDisconnected()
            _state.value = HidState(if (isBluetoothOn) HidStatus.STARTING else HidStatus.BLUETOOTH_OFF)
        }
    }

    private fun registerApp() {
        val h = hidDevice ?: return
        val sdp = BluetoothHidDeviceAppSdpSettings(
            "BT Control",
            "Keyboard, mouse, gamepad and pen tablet",
            "BT Control",
            BluetoothHidDevice.SUBCLASS1_COMBO,
            HidDescriptor.DESCRIPTOR,
        )
        _state.value = HidState(HidStatus.STARTING, message = t(R.string.registering_hid_device))
        val ok = runCatching { h.registerApp(sdp, null, null, executor, callback) }.getOrElse {
            Log.e(TAG, "registerApp threw", it); false
        }
        if (!ok) {
            _state.value = HidState(
                HidStatus.REGISTRATION_FAILED,
                message = t(R.string.could_not_register_as_a_bluetooth_hid_devi),
            )
        }
    }

    private val callback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            Log.i(TAG, "onAppStatusChanged registered=$registered plugged=${pluggedDevice?.address}")
            this@BluetoothHidManager.registered = registered
            if (!registered) {
                setDisconnected()
                if (started && isBluetoothOn) {
                    _state.value = HidState(HidStatus.REGISTRATION_FAILED,
                        message = t(R.string.hid_registration_was_released_another_app))
                }
                return
            }
            val already = runCatching { hidDevice?.connectedDevices?.firstOrNull() }.getOrNull()
            if (already != null) {
                onConnected(already)
                return
            }
            _state.value = HidState(HidStatus.READY)
            val pending = pendingConnectAddress
            val target = when {
                pending != null -> adapter?.getRemoteDevice(pending)
                settings.value.autoConnect -> pluggedDevice
                    ?: settings.value.lastDeviceAddress?.let { addr -> runCatching { adapter?.getRemoteDevice(addr) }.getOrNull() }
                        ?.takeIf { it.bondState == BluetoothDevice.BOND_BONDED }
                else -> null
            }
            pendingConnectAddress = null
            // The computer usually reconnects by itself; only call it if it hasn't after a moment
            // (both sides connecting at once makes Android show "Can't connect").
            if (target != null) main.postDelayed({
                if (connectedDevice == null && _state.value.status == HidStatus.READY) connect(target)
            }, if (pending != null) 0L else 2500L)
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            Log.i(TAG, "onConnectionStateChanged ${device.address} state=$state")
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> onConnected(device)
                BluetoothProfile.STATE_CONNECTING ->
                    _state.value = HidState(HidStatus.CONNECTING, nameOf(device), device.address)
                BluetoothProfile.STATE_DISCONNECTED, BluetoothProfile.STATE_DISCONNECTING -> {
                    if (connectedDevice == null || connectedDevice?.address == device.address) {
                        setDisconnected()
                        if (registered) _state.value = HidState(HidStatus.READY)
                    }
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
            val h = hidDevice ?: return
            val len = HidDescriptor.inputReportLength(id.toInt())
            if (type == BluetoothHidDevice.REPORT_TYPE_INPUT && len > 0) {
                h.replyReport(device, type, id, ByteArray(len))
            } else {
                h.reportError(device, BluetoothHidDevice.ERROR_RSP_INVALID_RPT_ID)
            }
        }

        override fun onSetReport(device: BluetoothDevice, type: Byte, id: Byte, data: ByteArray) {
            if (type == BluetoothHidDevice.REPORT_TYPE_OUTPUT && id.toInt() == HidDescriptor.ID_KEYBOARD) {
                parseLeds(data)
            }
            hidDevice?.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS)
        }

        override fun onInterruptData(device: BluetoothDevice, reportId: Byte, data: ByteArray) {
            if (reportId.toInt() == HidDescriptor.ID_KEYBOARD) parseLeds(data)
        }

        override fun onVirtualCableUnplug(device: BluetoothDevice) {
            Log.i(TAG, "Virtual cable unplug from ${device.address}")
            setDisconnected()
            if (registered) _state.value = HidState(HidStatus.READY)
        }
    }

    private fun parseLeds(data: ByteArray) {
        val v = when {
            data.size >= 2 && data[0].toInt() == HidDescriptor.ID_KEYBOARD -> data[1].toInt()
            data.isNotEmpty() -> data[0].toInt()
            else -> return
        }
        hid.onHostLeds(v and 0xFF)
    }

    private fun onConnected(device: BluetoothDevice) {
        connectedDevice = device
        hid.resetState()
        sender.clear()
        sender.minIntervalMs = settings.value.reportIntervalMs
        _state.value = HidState(HidStatus.CONNECTED, nameOf(device), device.address)
        settings.update { it.copy(lastDeviceAddress = device.address) }
        refreshBonded()
    }

    private fun setDisconnected() {
        connectedDevice = null
        sender.clear()
        hid.resetState()
    }

    private fun sendReport(id: Int, data: ByteArray): Boolean {
        val d = connectedDevice ?: return false
        val h = hidDevice ?: return false
        return h.sendReport(d, id, data)
    }

    // ------------------------------------------------------------------ connect

    fun connect(device: BluetoothDevice) {
        val h = hidDevice
        if (h == null || !registered) {
            pendingConnectAddress = device.address
            start()
            return
        }
        connectedDevice?.let { if (it.address != device.address) runCatching { h.disconnect(it) } }
        _state.value = HidState(HidStatus.CONNECTING, nameOf(device), device.address)
        val ok = runCatching { h.connect(device) }.getOrDefault(false)
        if (!ok) {
            _state.value = HidState(HidStatus.READY, message = t(R.string.could_not_connect_to, nameOf(device)))
        }
    }

    fun connect(address: String) {
        val d = runCatching { adapter?.getRemoteDevice(address) }.getOrNull() ?: return
        connect(d)
    }

    fun disconnect() {
        val d = connectedDevice ?: return
        runCatching { hidDevice?.disconnect(d) }
    }

    /** Release HID registration and register again (fixes "stuck" states). */
    fun reRegister() {
        val h = hidDevice
        disconnect()
        if (h != null) {
            runCatching { if (registered) h.unregisterApp() }
            registered = false
            main.postDelayed({ registerApp() }, 600)
        } else start()
    }

    // ------------------------------------------------------------------ devices

    private fun nameOf(d: BluetoothDevice): String =
        runCatching { (if (Build.VERSION.SDK_INT >= 30) d.alias else null) ?: d.name }.getOrNull() ?: d.address

    private fun info(d: BluetoothDevice): BtDeviceInfo {
        val cls = runCatching { d.bluetoothClass?.majorDeviceClass }.getOrNull()
        return BtDeviceInfo(
            address = d.address,
            name = nameOf(d),
            isComputer = cls == BluetoothClass.Device.Major.COMPUTER,
            bonded = runCatching { d.bondState == BluetoothDevice.BOND_BONDED }.getOrDefault(false),
            device = d,
        )
    }

    fun refreshBonded() {
        if (!hasConnectPermission()) return
        val list = runCatching { adapter?.bondedDevices?.map { info(it) } }.getOrNull().orEmpty()
        _bonded.value = list.sortedWith(compareByDescending<BtDeviceInfo> { it.isComputer }.thenBy { it.name.lowercase() })
    }

    fun startDiscovery(): Boolean {
        val a = adapter ?: return false
        if (!hasScanPermission()) return false
        _discovered.value = emptyList()
        if (a.isDiscovering) a.cancelDiscovery()
        val ok = runCatching { a.startDiscovery() }.getOrDefault(false)
        _discovering.value = ok
        return ok
    }

    fun cancelDiscovery() {
        runCatching { adapter?.cancelDiscovery() }
        _discovering.value = false
    }

    /** Pair with a computer from the phone, then connect the HID profile once bonded. */
    fun pairAndConnect(device: BluetoothDevice) {
        cancelDiscovery()
        if (device.bondState == BluetoothDevice.BOND_BONDED) {
            connect(device)
            return
        }
        pendingConnectAddress = device.address
        runCatching { device.createBond() }
    }

    /** Forget a pairing on the phone side (hidden API, best effort). */
    fun unpair(device: BluetoothDevice) {
        runCatching {
            device.javaClass.getMethod("removeBond").invoke(device)
        }
        main.postDelayed({ refreshBonded() }, 800)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            when (intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                        BluetoothAdapter.STATE_ON -> if (started) start()
                        BluetoothAdapter.STATE_TURNING_OFF, BluetoothAdapter.STATE_OFF -> {
                            setDisconnected()
                            registered = false
                            hidDevice = null
                            proxyRequested = false
                            _state.value = HidState(HidStatus.BLUETOOTH_OFF)
                        }
                    }
                }
                BluetoothDevice.ACTION_FOUND -> {
                    val d = deviceExtra(intent) ?: return
                    val i = info(d)
                    if (_discovered.value.none { it.address == i.address }) {
                        _discovered.value = (_discovered.value + i)
                            .sortedWith(compareByDescending<BtDeviceInfo> { it.isComputer }.thenBy { it.name.lowercase() })
                    }
                }
                BluetoothDevice.ACTION_NAME_CHANGED -> {
                    val d = deviceExtra(intent) ?: return
                    _discovered.value = _discovered.value.map { if (it.address == d.address) info(d) else it }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> _discovering.value = true
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> _discovering.value = false
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    refreshBonded()
                    val d = deviceExtra(intent) ?: return
                    val st = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)
                    if (st == BluetoothDevice.BOND_BONDED && d.address == pendingConnectAddress) {
                        pendingConnectAddress = null
                        // Give the computer a moment to finish service discovery.
                        main.postDelayed({ if (connectedDevice == null) connect(d) }, 1500)
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun deviceExtra(intent: Intent): BluetoothDevice? =
        if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        else intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

    private fun registerReceivers() {
        if (receiversRegistered) return
        val f = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_NAME_CHANGED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, f, ContextCompat.RECEIVER_EXPORTED)
        receiversRegistered = true
    }

    private companion object {
        const val TAG = "BtHid"
    }
}

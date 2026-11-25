package com.btcontrol.app.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.btcontrol.app.BtControlApp
import com.btcontrol.app.bt.HidService
import com.btcontrol.app.ui.theme.BtControlTheme
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class MainActivity : ComponentActivity() {
    private val app get() = BtControlApp.instance
    private val handler = Handler(Looper.getMainLooper())

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { onPermissionsResult() }

    private val enableBtLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { app.bt.start() }

    private val discoverableLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        current = WeakReference(this)
        enableEdgeToEdge()

        lifecycleScope.launch {
            app.settings.flow.collect { s ->
                if (s.keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                app.bt.sender.minIntervalMs = s.reportIntervalMs
            }
        }

        setContent {
            val settings by app.settings.flow.collectAsStateWithLifecycle()
            BtControlTheme(settings.theme, settings.dynamicColor) {
                AppRoot(
                    actions = AppActions(
                        requestPermissions = ::requestBtPermissions,
                        enableBluetooth = ::enableBluetooth,
                        makeDiscoverable = ::makeDiscoverable,
                    ),
                )
            }
        }
        requestBtPermissions()
    }

    override fun onStart() {
        super.onStart()
        com.btcontrol.app.log.AppLog.i("app", "in foreground")
        handler.removeCallbacks(stopRunnable)
        if (app.bt.hasConnectPermission()) {
            app.bt.start()
            if (app.settings.value.keepInBackground) HidService.start(this)
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) com.btcontrol.app.log.AppLog.i("app", "in background")
        if (!isChangingConfigurations && !app.settings.value.keepInBackground) {
            handler.postDelayed(stopRunnable, 4000)
        }
    }

    private val stopRunnable = Runnable {
        app.bt.stop()
        HidService.stop(this)
    }

    override fun onDestroy() {
        if (current?.get() === this) current = null
        super.onDestroy()
    }

    private fun requiredPermissions(): Array<String> = buildList {
        if (Build.VERSION.SDK_INT >= 31) {
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_ADVERTISE)
        } else {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray()

    fun requestBtPermissions() {
        permissionLauncher.launch(requiredPermissions())
    }

    private fun onPermissionsResult() {
        app.bt.start()
        if (app.bt.hasConnectPermission()) {
            if (app.settings.value.keepInBackground) HidService.start(this)
            if (!app.bt.isBluetoothOn) enableBluetooth()
        }
    }

    @Suppress("MissingPermission")
    fun enableBluetooth() {
        if (!app.bt.hasConnectPermission()) {
            requestBtPermissions(); return
        }
        runCatching { enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)) }
    }

    @Suppress("MissingPermission")
    fun makeDiscoverable() {
        val i = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120)
        runCatching { discoverableLauncher.launch(i) }
    }

    /** Screens (e.g. presentation remote) can intercept hardware keys such as volume buttons. */
    @android.annotation.SuppressLint("RestrictedApi") // false positive: this overrides Activity.dispatchKeyEvent
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        keyInterceptor?.let { if (it(event)) return true }
        return super.dispatchKeyEvent(event)
    }

    companion object {
        private var current: WeakReference<MainActivity>? = null
        var keyInterceptor: ((KeyEvent) -> Boolean)? = null

        fun finishIfRunning() {
            current?.get()?.finishAndRemoveTask()
        }
    }
}

class AppActions(
    val requestPermissions: () -> Unit,
    val enableBluetooth: () -> Unit,
    val makeDiscoverable: () -> Unit,
)

package com.btcontrol.app

import android.app.Application
import com.btcontrol.app.bt.BluetoothHidManager
import com.btcontrol.app.data.AppSettings
import com.btcontrol.app.data.LayoutRepository
import com.btcontrol.app.hid.HidController

class BtControlApp : Application() {
    lateinit var settings: AppSettings
        private set
    lateinit var bt: BluetoothHidManager
        private set
    lateinit var layouts: LayoutRepository
        private set

    val hid: HidController get() = bt.hid

    override fun onCreate() {
        super.onCreate()
        instance = this
        settings = AppSettings(this)
        com.btcontrol.app.log.AppLog.init(this, settings.value.diagnosticLog, settings.nonDefaultSummary())
        bt = BluetoothHidManager(this, settings)
        layouts = LayoutRepository(this)
        com.btcontrol.app.ui.components.GyroCalibration.load(settings.value)
    }

    companion object {
        lateinit var instance: BtControlApp
            private set
    }
}

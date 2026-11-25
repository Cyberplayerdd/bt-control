package com.btcontrol.app.bt

import com.btcontrol.app.t
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.btcontrol.app.BtControlApp
import com.btcontrol.app.R
import com.btcontrol.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps the process (and therefore the HID registration and the link
 * to the computer) alive while the app is in the background.
 */
class HidService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val bt = BtControlApp.instance.bt
        try {
            ServiceCompat.startForeground(
                this, NOTIFICATION_ID, buildNotification(bt.state.value),
                if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE else 0,
            )
        } catch (e: Exception) {
            stopSelf()
            return
        }
        scope.launch {
            bt.state.collect { st ->
                getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, buildNotification(st))
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISCONNECT -> BtControlApp.instance.bt.disconnect()
            ACTION_QUIT -> {
                BtControlApp.instance.bt.stop()
                stopSelf()
                MainActivity.finishIfRunning()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, t(R.string.bluetooth_connection), NotificationManager.IMPORTANCE_LOW).apply {
                description = t(R.string.shown_while_bt_control_is_acting_as_a_keyb)
                setShowBadge(false)
            }
        )
    }

    private fun buildNotification(st: HidState): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val text = when (st.status) {
            HidStatus.CONNECTED -> t(R.string.status_connected_to, st.deviceName ?: "")
            HidStatus.CONNECTING -> t(R.string.status_connecting_to, st.deviceName ?: t(R.string.computer))
            HidStatus.READY -> t(R.string.ready_waiting_for_a_computer)
            HidStatus.BLUETOOTH_OFF -> t(R.string.bluetooth_is_off)
            else -> t(R.string.starting)
        }
        val b = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_bt)
            .setContentTitle("BT Control")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        if (st.connected) b.addAction(0, t(R.string.disconnect), action(ACTION_DISCONNECT, 1))
        b.addAction(0, t(R.string.quit), action(ACTION_QUIT, 2))
        return b.build()
    }

    private fun action(a: String, code: Int) = PendingIntent.getService(
        this, code, Intent(this, HidService::class.java).setAction(a),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        private const val CHANNEL = "connection"
        private const val NOTIFICATION_ID = 7
        private const val ACTION_DISCONNECT = "disconnect"
        private const val ACTION_QUIT = "quit"

        fun start(context: Context) {
            runCatching { ContextCompat.startForegroundService(context, Intent(context, HidService::class.java)) }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, HidService::class.java))
        }
    }
}

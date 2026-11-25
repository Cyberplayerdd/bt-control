package com.btcontrol.app.hid

import android.os.SystemClock
import com.btcontrol.app.log.AppLog

/**
 * Serialises every HID input report onto one worker thread.
 *
 * - Discrete reports (key presses, button transitions) are never merged or dropped.
 * - Mouse movement reports with identical buttons are merged by summing deltas.
 * - "State" reports (pen, gamepad) with identical button bits replace the queued one, so only
 *   the latest position is sent while the link is busy.
 * - Mergeable reports are paced to at most one per [minIntervalMs] per report ID so the
 *   Bluetooth link never builds a backlog (which would show up as lag).
 * - `sendReport` returns false when L2CAP is congested; the report is retried briefly.
 */
class ReportSender(
    private val transport: (id: Int, data: ByteArray) -> Boolean,
    private val isConnected: () -> Boolean,
) {
    private enum class Kind { DISCRETE, MOUSE, STATE }

    private class Item(
        val id: Int,
        var data: ByteArray,
        val kind: Kind,
        val signature: Int,
        val delayAfterMs: Int,
    )

    private val lock = Object()
    private val queue = ArrayDeque<Item>()
    private val lastSent = LongArray(16)

    @Volatile var minIntervalMs: Int = 8

    private val worker = Thread({ loop() }, "hid-sender").apply {
        isDaemon = true
        priority = Thread.MAX_PRIORITY - 1
        start()
    }

    /** A report that must arrive exactly as given (keys, clicks, consumer keys...). */
    fun sendDiscrete(id: Int, data: ByteArray, delayAfterMs: Int = 0) =
        enqueue(Item(id, data, Kind.DISCRETE, 0, delayAfterMs))

    /** Latest-wins state report; queued entry is replaced while [signature] (button bits) is unchanged. */
    fun sendState(id: Int, data: ByteArray, signature: Int) =
        enqueue(Item(id, data, Kind.STATE, signature, 0))

    /** Relative mouse report; consecutive reports with the same buttons are summed. */
    fun sendMouse(buttons: Int, dx: Int, dy: Int, wheel: Int, pan: Int) {
        val data = mouseBytes(buttons, dx, dy, wheel, pan)
        synchronized(lock) {
            if (!isConnected()) return
            val tail = queue.lastOrNull()
            if (tail != null && tail.kind == Kind.MOUSE && tail.signature == buttons) {
                val t = tail.data
                val nx = s16(t, 1) + dx
                val ny = s16(t, 3) + dy
                val nw = t[5].toInt() + wheel
                val np = t[6].toInt() + pan
                if (nx in -32767..32767 && ny in -32767..32767 && nw in -127..127 && np in -127..127) {
                    tail.data = mouseBytes(buttons, nx, ny, nw, np)
                    return
                }
            }
            queue.addLast(Item(HidDescriptor.ID_MOUSE, data, Kind.MOUSE, buttons, 0))
            lock.notifyAll()
        }
    }

    fun clear() = synchronized(lock) { queue.clear() }

    private fun enqueue(item: Item) {
        synchronized(lock) {
            if (!isConnected()) return
            if (item.kind == Kind.STATE) {
                // Replace the queued tail report of the same id if the buttons didn't change.
                val tail = queue.lastOrNull()
                if (tail != null && tail.id == item.id && tail.kind == Kind.STATE && tail.signature == item.signature) {
                    tail.data = item.data
                    return
                }
            }
            queue.addLast(item)
            if (queue.size > 512) {
                // Something is very wrong with the link; avoid unbounded growth.
                AppLog.countDropped()
                queue.removeFirst()
            }
            lock.notifyAll()
        }
    }

    private fun loop() {
        while (true) {
            val item: Item = synchronized(lock) {
                while (queue.isEmpty()) lock.wait()
                val head = queue.first()
                if (head.kind != Kind.DISCRETE) {
                    val wait = lastSent[head.id] + minIntervalMs - SystemClock.uptimeMillis()
                    if (wait > 0) {
                        // Give producers a chance to merge into this report while we wait.
                        lock.wait(wait)
                        return@synchronized null
                    }
                }
                queue.removeFirst()
            } ?: continue

            var ok = false
            var attempts = 0
            while (!ok && attempts < 40) {
                if (!isConnected()) break
                ok = try {
                    transport(item.id, item.data)
                } catch (t: Throwable) {
                    AppLog.w("hid", "sendReport threw", t)
                    false
                }
                if (!ok) {
                    if (attempts == 0) AppLog.countRetried()
                    attempts++
                    SystemClock.sleep(3)
                }
            }
            if (!ok) {
                if (!isConnected()) clear()
                else AppLog.countDropped()
            } else AppLog.countReport(item.id, item.data)
            lastSent[item.id] = SystemClock.uptimeMillis()
            if (item.delayAfterMs > 0) SystemClock.sleep(item.delayAfterMs.toLong())
        }
    }

    companion object {
        fun mouseBytes(buttons: Int, dx: Int, dy: Int, wheel: Int, pan: Int) = byteArrayOf(
            buttons.toByte(),
            (dx and 0xFF).toByte(), ((dx shr 8) and 0xFF).toByte(),
            (dy and 0xFF).toByte(), ((dy shr 8) and 0xFF).toByte(),
            wheel.toByte(), pan.toByte(),
        )

        private fun s16(b: ByteArray, off: Int): Int =
            ((b[off].toInt() and 0xFF) or (b[off + 1].toInt() shl 8)).toShort().toInt()
    }
}

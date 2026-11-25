package com.btcontrol.app.log

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicIntegerArray

/**
 * Local, anonymised diagnostic log.
 *
 * - Stays on the device (app-private storage); it only leaves when the user taps *Share log*.
 * - One file per app start; only the [MAX_FILES] most recent are kept.
 * - Each file is capped at [MAX_FILE_BYTES]: when it is full, the older half of its entries is
 *   removed (the header stays), so the whole log never exceeds MAX_FILES × MAX_FILE_BYTES.
 *   Identical consecutive lines are collapsed into "repeated N times".
 * - Anonymised: device names are never written, Bluetooth addresses become a short id that
 *   can't be turned back into the address, and e-mail addresses are removed. What is typed,
 *   the keys, buttons and pen positions sent are never written: only how many reports of each
 *   kind were sent, summarised every [SUMMARY_PERIOD_S] seconds.
 */
object AppLog {
    const val MAX_FILES = 3
    const val MAX_FILE_BYTES = 256 * 1024L
    private const val SUMMARY_PERIOD_S = 30L
    private const val MAX_ENTRY_CHARS = 6000
    private const val THREAD_NAME = "app-log"

    /** When false nothing is recorded (the existing files are kept until deleted). */
    @Volatile var enabled = true

    private val io = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, THREAD_NAME).apply { isDaemon = true; priority = Thread.MIN_PRIORITY }
    }
    private var dir: File? = null
    private var header = ""
    private var salt = ""
    private val sensitive = CopyOnWriteArraySet<String>()

    // Only touched on the io thread.
    private var file: File? = null
    private var lastBody: String? = null
    private var repeats = 0
    private val timeFmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    // Activity counters (report ids 0..7), reset at every summary.
    private val reports = AtomicIntegerArray(8)
    private val keyPresses = AtomicInteger()
    private val retried = AtomicInteger()
    private val dropped = AtomicInteger()

    fun init(context: Context, enabled: Boolean, settingsSummary: String) {
        this.enabled = enabled
        dir = File(context.filesDir, "logs")
        val prefs = context.getSharedPreferences("log", Context.MODE_PRIVATE)
        salt = prefs.getString("salt", null) ?: ByteArray(16).also { SecureRandom().nextBytes(it) }
            .joinToString("") { "%02x".format(it) }
            .also { prefs.edit().putString("salt", it).apply() }
        val version = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "?"
        header = buildString {
            append("BT Control ").append(version).append('\n')
            append("Android ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n")
            append("Device: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n')
            append("App language: ").append(Locale.getDefault().language).append('\n')
            append("Settings changed from default: ").append(settingsSummary.ifEmpty { "none" }).append('\n')
            append("Anonymised log: no device names, no Bluetooth addresses (dev-xxxxxx ids instead),\n")
            append("no typed text or keys - only events and counts of reports sent.\n")
            append(HEADER_END).append('\n')
        }
        installCrashHandler()
        io.scheduleWithFixedDelay({ runCatching { flushActivity() } }, SUMMARY_PERIOD_S, SUMMARY_PERIOD_S, TimeUnit.SECONDS)
        i("app", "started")
    }

    fun i(tag: String, msg: String) = log(Log.INFO, tag, msg, null)
    fun w(tag: String, msg: String, t: Throwable? = null) = log(Log.WARN, tag, msg, t)
    fun e(tag: String, msg: String, t: Throwable? = null) = log(Log.ERROR, tag, msg, t)

    /** Short anonymous id for a Bluetooth address (salted hash, stable on this install only). */
    fun device(address: String?): String {
        if (address.isNullOrEmpty()) return "none"
        val d = MessageDigest.getInstance("SHA-256").digest((salt + address.uppercase(Locale.US)).toByteArray())
        return "dev-" + d.take(3).joinToString("") { "%02x".format(it) }
    }

    /** A name (Bluetooth device, phone name…) that must never appear in the log. */
    fun addSensitive(name: String?) {
        val n = name?.trim() ?: return
        if (n.length >= 3) sensitive.add(n)
    }

    /** Called for each HID report actually delivered. Only counts are kept, never the content. */
    fun countReport(id: Int, data: ByteArray) {
        if (!enabled) return
        if (id in 0 until reports.length()) reports.incrementAndGet(id)
        // Keyboard report: [modifiers, reserved, key1..key6]; any key byte set = a key is down.
        if (id == 1 && data.size >= 3 && (2 until data.size).any { data[it].toInt() != 0 }) keyPresses.incrementAndGet()
    }

    fun countRetried() { if (enabled) retried.incrementAndGet() }
    fun countDropped() { if (enabled) dropped.incrementAndGet() }

    // ------------------------------------------------------------------ reading / sharing

    /** Log files, newest first. */
    fun files(): List<File> =
        dir?.listFiles { f -> f.name.startsWith("log-") && f.name.endsWith(".txt") }
            ?.sortedByDescending { it.name }.orEmpty()

    /** The end of the current log, for the in-app viewer. */
    fun tail(maxChars: Int = 24_000): String {
        sync { flushActivity() }
        val f = files().firstOrNull() ?: return ""
        val text = runCatching { f.readText() }.getOrDefault("")
        return if (text.length <= maxChars) text else "…\n" + text.takeLast(maxChars).substringAfter('\n')
    }

    /** All logs (newest first) in one file under cache/exports, ready to share; null if empty. */
    fun exportForSharing(context: Context): File? {
        sync { flushActivity() }
        val list = files()
        if (list.isEmpty()) return null
        val out = File(context.cacheDir, "exports/btcontrol-log.txt")
        out.parentFile?.mkdirs()
        out.writeText(buildString {
            append("BT Control diagnostic log (last ").append(list.size).append(" sessions, newest first)\n\n")
            list.forEach { f ->
                append("===== ").append(f.name).append(" =====\n")
                append(runCatching { f.readText() }.getOrDefault("(unreadable)\n"))
                append('\n')
            }
        })
        return out
    }

    fun deleteAll() {
        sync {
            files().forEach { it.delete() }
            file = null
            lastBody = null
            repeats = 0
        }
    }

    // ------------------------------------------------------------------ writing

    private fun log(priority: Int, tag: String, msg: String, t: Throwable?) {
        val text = scrub(msg) + (t?.let { "\n" + trace(it) } ?: "")
        Log.println(priority, "BtControl/$tag", text)
        if (!enabled || dir == null) return
        val level = when (priority) { Log.ERROR -> 'E'; Log.WARN -> 'W'; else -> 'I' }
        val time = System.currentTimeMillis()
        if (Thread.currentThread().name == THREAD_NAME) append(time, level, tag, text)
        else io.execute { append(time, level, tag, text) }
    }

    /** io thread only. */
    private fun append(time: Long, level: Char, tag: String, text: String) {
        val body = "$level/$tag: " + text.take(MAX_ENTRY_CHARS)
        if (body == lastBody) {
            repeats++
            return
        }
        val f = file ?: startFile() ?: return
        val sb = StringBuilder()
        if (repeats > 0) sb.append("    (previous line repeated ").append(repeats).append(" more times)\n")
        repeats = 0
        lastBody = body
        sb.append(timeFmt.format(Date(time))).append(' ').append(body).append('\n')
        runCatching {
            f.appendText(sb.toString())
            if (f.length() > MAX_FILE_BYTES) trim(f)
        }
    }

    /** Starts this session's file and deletes the oldest ones beyond [MAX_FILES]. io thread only. */
    private fun startFile(): File? {
        val d = dir ?: return null
        return runCatching {
            d.mkdirs()
            files().drop(MAX_FILES - 1).forEach { it.delete() }
            val name = "log-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()) + ".txt"
            File(d, name).also {
                it.writeText(header)
                file = it
            }
        }.getOrNull()
    }

    /** Keeps the header and the newer half of the entries. */
    private fun trim(f: File) {
        val lines = f.readLines()
        val split = lines.indexOf(HEADER_END) + 1
        val head = lines.take(split)
        val body = lines.drop(split).filterNot { it.startsWith(TRIM_MARK) }
        val keep = body.takeLast(body.size / 2)
        f.writeText(
            (head + "$TRIM_MARK older entries removed to keep the log small …" + keep)
                .joinToString("\n", postfix = "\n")
        )
    }

    private fun flushActivity() {
        val counts = IntArray(reports.length()) { reports.getAndSet(it, 0) }
        val keys = keyPresses.getAndSet(0)
        val re = retried.getAndSet(0)
        val dr = dropped.getAndSet(0)
        if (counts.sum() == 0 && re == 0 && dr == 0) {
            if (repeats > 0) append(System.currentTimeMillis(), 'I', "activity", "idle")
            return
        }
        val parts = buildList {
            if (counts[1] > 0) add("keyboard $keys key presses (${counts[1]} reports)")
            if (counts[2] > 0) add("mouse ${counts[2]}")
            if (counts[3] > 0) add("media keys ${counts[3]}")
            if (counts[4] > 0) add("power/sleep ${counts[4]}")
            if (counts[5] > 0) add("gamepad ${counts[5]}")
            if (counts[6] > 0) add("pen ${counts[6]}")
            if (re > 0) add("$re retried (link busy)")
            if (dr > 0) add("$dr DROPPED")
        }
        append(System.currentTimeMillis(), 'I', "activity", "reports sent in the last ${SUMMARY_PERIOD_S}s: " + parts.joinToString(", "))
    }

    /** Runs [block] on the log thread and waits for it (also flushes pending writes). */
    private fun sync(block: () -> Unit) {
        if (Thread.currentThread().name == THREAD_NAME) { block(); return }
        runCatching { io.submit(block).get(3, TimeUnit.SECONDS) }
    }

    private fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, ex ->
            runCatching {
                val text = "app crashed (thread ${thread.name})\n" + trace(ex)
                Log.e("BtControl/crash", text)
                if (enabled && dir != null) {
                    val time = System.currentTimeMillis()
                    sync { append(time, 'E', "crash", text) }
                }
            }
            previous?.uncaughtException(thread, ex)
        }
    }

    // ------------------------------------------------------------------ anonymising

    private val MAC = Regex("(?i)\\b[0-9a-f]{2}(?::[0-9a-f]{2}){5}\\b")
    private val EMAIL = Regex("[\\w.+-]+@[\\w-]+(?:\\.[\\w-]+)+")
    private const val HEADER_END = "----"
    private const val TRIM_MARK = "…"

    fun scrub(s: String): String {
        var r = MAC.replace(s) { device(it.value) }
        r = EMAIL.replace(r, "<email>")
        for (n in sensitive) r = r.replace(n, "<name>")
        return r
    }

    private fun trace(t: Throwable): String = buildString {
        var c: Throwable? = t
        var depth = 0
        while (c != null && depth < 4) {
            if (depth > 0) append("Caused by: ")
            append(c.javaClass.name)
            c.message?.let { append(": ").append(scrub(it)) }
            append('\n')
            c.stackTrace.take(20).forEach { append("    at ").append(it).append('\n') }
            c = c.cause.takeIf { it !== c }
            depth++
        }
    }.trimEnd()
}

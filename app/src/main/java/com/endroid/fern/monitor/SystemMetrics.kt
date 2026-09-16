package com.endroid.fern.monitor

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.os.SystemClock
import java.io.File
import java.io.RandomAccessFile

data class SystemSnapshot(
    val ramUsedMb: Long,
    val ramTotalMb: Long,
    val ramPercent: Float,
    val storageUsedGb: Float,
    val storageTotalGb: Float,
    val storagePercent: Float,
    val batteryPercent: Int,
    val batteryCharging: Boolean,
    val batteryTempC: Float?,
    val batteryHealth: String,
    val cpuPercent: Float,
    val cpuAvailable: Boolean,
    val loadAvg1: Float?,
    val loadAvg5: Float?,
    val loadAvg15: Float?,
    val cpuCores: Int,
    val cpuMaxMhz: Int?,
    val cpuHardware: String,
    val cpuBoard: String,
    val cpuAbi: String,
    val networkLabel: String,
    /** Instantaneous total interface throughput in KB/s (rx+tx). */
    val networkKBps: Float,
    val deviceModel: String,
    val androidVersion: String,
    val sdkInt: Int,
    val uptimeHours: Float,
    val appHeapUsedMb: Long,
    val appHeapMaxMb: Long,
    val thermalLabel: String
)

object SystemMetrics {

    @Volatile private var prevIdle: Long = -1
    @Volatile private var prevTotal: Long = -1
    @Volatile private var prevNetBytes: Long = -1
    @Volatile private var prevNetAt: Long = 0

    fun capture(context: Context): SystemSnapshot {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mem = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mem)
        val totalRam = mem.totalMem
        val availRam = mem.availMem
        val usedRam = (totalRam - availRam).coerceAtLeast(0)
        val ramPct = if (totalRam > 0) usedRam.toFloat() / totalRam * 100f else 0f

        val stat = StatFs(Environment.getDataDirectory().path)
        val totalStore = stat.totalBytes
        val availStore = stat.availableBytes
        val usedStore = (totalStore - availStore).coerceAtLeast(0)
        val storePct = if (totalStore > 0) usedStore.toFloat() / totalStore * 100f else 0f

        val battery = readBattery(context)
        val load = readLoadAvg()
        val cores = Runtime.getRuntime().availableProcessors()
        val (cpuPct, cpuOk) = readCpuPercentAccurate(cores, load)
        val maxMhz = readMaxCpuMhz()
        val net = readNetwork(context)
        val netKBps = readNetworkKBps()
        val uptime = SystemClock.elapsedRealtime() / 3_600_000f
        val runtime = Runtime.getRuntime()
        val heapUsed = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val heapMax = runtime.maxMemory() / (1024 * 1024)
        val thermal = readThermal(context)

        return SystemSnapshot(
            ramUsedMb = usedRam / (1024 * 1024),
            ramTotalMb = totalRam / (1024 * 1024),
            ramPercent = ramPct,
            storageUsedGb = usedStore / 1e9f,
            storageTotalGb = totalStore / 1e9f,
            storagePercent = storePct,
            batteryPercent = battery.pct,
            batteryCharging = battery.charging,
            batteryTempC = battery.tempC,
            batteryHealth = battery.health,
            cpuPercent = cpuPct,
            cpuAvailable = cpuOk,
            loadAvg1 = load?.getOrNull(0),
            loadAvg5 = load?.getOrNull(1),
            loadAvg15 = load?.getOrNull(2),
            cpuCores = cores,
            cpuMaxMhz = maxMhz,
            cpuHardware = Build.HARDWARE ?: "?",
            cpuBoard = Build.BOARD ?: "?",
            cpuAbi = (Build.SUPPORTED_ABIS.firstOrNull() ?: "?"),
            networkLabel = net,
            networkKBps = netKBps,
            deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}".trim(),
            androidVersion = Build.VERSION.RELEASE ?: "?",
            sdkInt = Build.VERSION.SDK_INT,
            uptimeHours = uptime,
            appHeapUsedMb = heapUsed,
            appHeapMaxMb = heapMax,
            thermalLabel = thermal
        )
    }

    private data class BatteryInfo(
        val pct: Int,
        val charging: Boolean,
        val tempC: Float?,
        val health: String
    )

    private fun readBattery(context: Context): BatteryInfo {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return BatteryInfo(-1, false, null, "Unknown")
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val pct = (level * 100) / scale
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val temp = if (tempTenths == Int.MIN_VALUE) null else tempTenths / 10f
        val health = when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failed"
            else -> "Unknown"
        }
        return BatteryInfo(pct, charging, temp, health)
    }

    private fun parseProcStatLine(line: String?): Pair<Long, Long>? {
        if (line.isNullOrBlank()) return null
        val trimmed = line.trim()
        if (!trimmed.startsWith("cpu")) return null
        // Aggregate line is "cpu " (with space); per-core is "cpu0" etc. — only use aggregate.
        if (trimmed.length > 3 && trimmed[3].isDigit()) return null
        val parts = trimmed.split(Regex("\\s+")).drop(1).mapNotNull { it.toLongOrNull() }
        if (parts.size < 4) return null
        val idle = parts[3] + parts.getOrElse(4) { 0L } // idle + iowait
        val total = parts.sum()
        if (total <= 0L) return null
        return idle to total
    }

    /** Several open paths — SELinux often blocks one style of access on phones. */
    private fun readProcStat(): Pair<Long, Long>? {
        try {
            parseProcStatLine(File("/proc/stat").bufferedReader().use { it.readLine() })?.let { return it }
        } catch (_: Exception) { }
        try {
            RandomAccessFile("/proc/stat", "r").use { raf ->
                parseProcStatLine(raf.readLine())?.let { return it }
            }
        } catch (_: Exception) { }
        try {
            File("/proc/stat").inputStream().bufferedReader().use { br ->
                parseProcStatLine(br.readLine())?.let { return it }
            }
        } catch (_: Exception) { }
        try {
            val proc = Runtime.getRuntime().exec(arrayOf("cat", "/proc/stat"))
            try {
                parseProcStatLine(proc.inputStream.bufferedReader().use { it.readLine() })?.let { return it }
            } finally {
                proc.destroy()
            }
        } catch (_: Exception) { }
        return null
    }

    private fun cpuFromLoad(cores: Int, load: FloatArray?): Pair<Float, Boolean> {
        val load1 = load?.getOrNull(0) ?: return 0f to false
        val c = cores.coerceAtLeast(1)
        return (load1 / c * 100f).coerceIn(0f, 100f) to false
    }

    /**
     * Dual-sample /proc/stat in this call; if the delta is zero or /proc is blocked,
     * use the cross-tick prevIdle/prevTotal from the last poll; finally loadavg.
     */
    private fun readCpuPercentAccurate(cores: Int, load: FloatArray?): Pair<Float, Boolean> {
        val first = readProcStat()
        if (first != null) {
            try {
                Thread.sleep(180)
            } catch (_: InterruptedException) { }
            val second = readProcStat()
            if (second != null) {
                val (idle1, total1) = first
                val (idle2, total2) = second
                val dTotal = total2 - total1
                val dIdle = idle2 - idle1
                if (dTotal > 0L) {
                    prevIdle = idle2
                    prevTotal = total2
                    val busy = ((dTotal - dIdle).toFloat() / dTotal * 100f).coerceIn(0f, 100f)
                    return busy to true
                }
            }
        }

        // Cross-tick against values stored on the previous successful sample
        val now = readProcStat() ?: first
        if (now != null) {
            val (idle2, total2) = now
            val pIdle = prevIdle
            val pTotal = prevTotal
            if (pIdle >= 0L && pTotal > 0L && total2 >= pTotal) {
                val dTotal = total2 - pTotal
                val dIdle = idle2 - pIdle
                prevIdle = idle2
                prevTotal = total2
                if (dTotal > 0L) {
                    val busy = ((dTotal - dIdle).toFloat() / dTotal * 100f).coerceIn(0f, 100f)
                    return busy to true
                }
            } else {
                prevIdle = idle2
                prevTotal = total2
            }
        }

        return cpuFromLoad(cores, load)
    }

    private fun readLoadAvg(): FloatArray? {
        fun parse(line: String?): FloatArray? {
            if (line.isNullOrBlank()) return null
            val p = line.trim().split(Regex("\\s+"))
            if (p.size < 3) return null
            val a = p[0].toFloatOrNull() ?: return null
            val b = p[1].toFloatOrNull() ?: return null
            val c = p[2].toFloatOrNull() ?: return null
            return floatArrayOf(a, b, c)
        }
        try {
            parse(File("/proc/loadavg").bufferedReader().use { it.readLine() })?.let { return it }
        } catch (_: Exception) { }
        try {
            RandomAccessFile("/proc/loadavg", "r").use { raf ->
                parse(raf.readLine())?.let { return it }
            }
        } catch (_: Exception) { }
        try {
            val proc = Runtime.getRuntime().exec(arrayOf("cat", "/proc/loadavg"))
            try {
                parse(proc.inputStream.bufferedReader().use { it.readLine() })?.let { return it }
            } finally {
                proc.destroy()
            }
        } catch (_: Exception) { }
        return null
    }

    private fun readMaxCpuMhz(): Int? {
        return try {
            val path = "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq"
            val khz = File(path).readText().trim().toLongOrNull() ?: return null
            (khz / 1000).toInt()
        } catch (_: Exception) {
            null
        }
    }

    private fun readNetwork(context: Context): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return "Unknown"
        val network = cm.activeNetwork ?: return "Offline"
        val caps = cm.getNetworkCapabilities(network) ?: return "Offline"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi‑Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Connected"
        }
    }

    private fun readNetworkKBps(): Float {
        val bytes = try {
            val rx = TrafficStats.getTotalRxBytes()
            val tx = TrafficStats.getTotalTxBytes()
            if (rx < 0 || tx < 0) return 0f
            rx + tx
        } catch (_: Exception) {
            return 0f
        }
        val now = SystemClock.elapsedRealtime()
        val prevB = prevNetBytes
        val prevT = prevNetAt
        prevNetBytes = bytes
        prevNetAt = now
        if (prevB < 0 || prevT <= 0L || now <= prevT) return 0f
        val dtSec = (now - prevT) / 1000f
        if (dtSec < 0.05f) return 0f
        val dBytes = (bytes - prevB).coerceAtLeast(0L)
        return (dBytes / 1024f) / dtSec
    }

    private fun readThermal(context: Context): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return "—"
        return try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            when (pm.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> "None"
                PowerManager.THERMAL_STATUS_LIGHT -> "Light"
                PowerManager.THERMAL_STATUS_MODERATE -> "Moderate"
                PowerManager.THERMAL_STATUS_SEVERE -> "Severe"
                PowerManager.THERMAL_STATUS_CRITICAL -> "Critical"
                PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency"
                PowerManager.THERMAL_STATUS_SHUTDOWN -> "Shutdown"
                else -> "—"
            }
        } catch (_: Exception) {
            "—"
        }
    }
}

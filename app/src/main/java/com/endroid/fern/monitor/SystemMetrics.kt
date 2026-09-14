package com.endroid.fern.monitor

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
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
    val cpuPercent: Float,
    val networkLabel: String,
    val deviceModel: String,
    val androidVersion: String,
    val sdkInt: Int,
    val uptimeHours: Float
)

object SystemMetrics {

    private var prevIdle: Long = 0
    private var prevTotal: Long = 0

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
        val cpu = readCpuPercent()
        val net = readNetwork(context)
        val uptime = android.os.SystemClock.elapsedRealtime() / 3_600_000f

        return SystemSnapshot(
            ramUsedMb = usedRam / (1024 * 1024),
            ramTotalMb = totalRam / (1024 * 1024),
            ramPercent = ramPct,
            storageUsedGb = usedStore / 1e9f,
            storageTotalGb = totalStore / 1e9f,
            storagePercent = storePct,
            batteryPercent = battery.first,
            batteryCharging = battery.second,
            batteryTempC = battery.third,
            cpuPercent = cpu,
            networkLabel = net,
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
            androidVersion = Build.VERSION.RELEASE ?: "?",
            sdkInt = Build.VERSION.SDK_INT,
            uptimeHours = uptime
        )
    }

    private fun readBattery(context: Context): Triple<Int, Boolean, Float?> {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return Triple(-1, false, null)
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val pct = (level * 100) / scale
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val temp = if (tempTenths == Int.MIN_VALUE) null else tempTenths / 10f
        return Triple(pct, charging, temp)
    }

    /** Best-effort CPU load from /proc/stat (may be restricted on some devices). */
    private fun readCpuPercent(): Float {
        return try {
            RandomAccessFile("/proc/stat", "r").use { reader ->
                val line = reader.readLine() ?: return 0f
                val parts = line.split(Regex("\\s+")).drop(1).mapNotNull { it.toLongOrNull() }
                if (parts.size < 4) return 0f
                val idle = parts[3]
                val total = parts.sum()
                val dIdle = idle - prevIdle
                val dTotal = total - prevTotal
                prevIdle = idle
                prevTotal = total
                if (dTotal <= 0L) return 0f
                ((dTotal - dIdle).toFloat() / dTotal * 100f).coerceIn(0f, 100f)
            }
        } catch (_: Exception) {
            0f
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
}

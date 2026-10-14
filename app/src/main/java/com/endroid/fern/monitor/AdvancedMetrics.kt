package com.endroid.fern.monitor

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

data class ThermalZone(val name: String, val tempC: Float)

enum class ElevatedProfile {
    /** Thermals + light CPU only — lowest impact. */
    Lite,
    /** Rotate heavy per-app work across ticks. */
    Balanced,
    /** More frequent per-app, still not all-at-once. */
    Full
}

data class AdvancedSnapshot(
    val thermalZones: List<ThermalZone>,
    val maxTempC: Float?,
    val rssKbByPackage: Map<String, Long>,
    val cpuTop: List<Pair<String, Float>> = emptyList(),
    val accurateCpuPercent: Float? = null,
    val shellOk: Boolean = false,
    val netKbByPackage: Map<String, Long> = emptyMap(),
    val processCount: Int = 0,
    val zramKb: Long? = null,
    val lastPhase: String = ""
)

/**
 * Elevated metrics with staggered work so Shizuku/root does not hammer the device.
 * Never runs dumpsys cpuinfo + meminfo + top + net in the same tick.
 */
class AdvancedMetrics(private val access: AdvancedAccess) {

    private val mutex = Mutex()
    private val lastShellOkAt = AtomicLong(0L)
    private val shellOkCached = AtomicBoolean(false)
    private val lastRss = AtomicReference<Map<String, Long>>(emptyMap())
    private val lastCpu = AtomicReference<List<Pair<String, Float>>>(emptyList())
    private val lastZones = AtomicReference<List<ThermalZone>>(emptyList())
    private val lastAccCpu = AtomicReference<Float?>(null)
    private val lastNet = AtomicReference<Map<String, Long>>(emptyMap())
    private val lastProcs = AtomicLong(0L)
    private val lastZram = AtomicReference<Long?>(null)
    private val tick = AtomicLong(0L)

    suspend fun collect(
        advancedEnabled: Boolean,
        profile: ElevatedProfile = ElevatedProfile.Balanced,
        forceHeavy: Boolean = false
    ): AdvancedSnapshot = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (!access.isElevatedLive(advancedEnabled)) {
                return@withContext AdvancedSnapshot(emptyList(), null, emptyMap())
            }

            val n = tick.incrementAndGet()
            val shellOk = ensureShell()
            if (!shellOk) {
                return@withContext AdvancedSnapshot(
                    thermalZones = lastZones.get(),
                    maxTempC = lastZones.get().maxOfOrNull { it.tempC },
                    rssKbByPackage = lastRss.get(),
                    cpuTop = lastCpu.get(),
                    accurateCpuPercent = lastAccCpu.get(),
                    shellOk = false,
                    netKbByPackage = lastNet.get(),
                    processCount = lastProcs.get().toInt(),
                    zramKb = lastZram.get(),
                    lastPhase = "shell-wait"
                )
            }

            // Phase plan depends on profile — never all heavy dumpsys in one pass.
            val phase = when {
                forceHeavy -> "heavy"
                profile == ElevatedProfile.Lite -> when ((n % 4).toInt()) {
                    0 -> "thermal"
                    1 -> "cpu-acc"
                    2 -> "thermal"
                    else -> "idle"
                }
                profile == ElevatedProfile.Full -> when ((n % 4).toInt()) {
                    0 -> "thermal+cpu-acc"
                    1 -> "rss"
                    2 -> "cpu-top"
                    else -> "procs+zram"
                }
                else -> when ((n % 5).toInt()) { // Balanced
                    0 -> "thermal"
                    1 -> "cpu-acc"
                    2 -> "rss"
                    3 -> "cpu-top"
                    else -> "procs"
                }
            }

            when {
                phase.contains("thermal") -> {
                    val z = readThermalZones()
                    if (z.isNotEmpty()) lastZones.set(z)
                }
                phase == "idle" -> { /* keep cache */ }
            }
            if (phase.contains("cpu-acc")) {
                // Single /proc/stat sample pair is enough; shorter dwell than before.
                access.readCpuPercentFast()?.let { lastAccCpu.set(it.first) }
            }
            if (phase == "rss" || phase == "heavy") {
                val rss = readPackageRss()
                if (rss.isNotEmpty()) lastRss.set(rss)
            }
            if (phase == "cpu-top" || phase == "heavy") {
                val cpu = readCpuTop()
                if (cpu.isNotEmpty()) lastCpu.set(cpu)
            }
            if (phase.contains("procs") || phase == "heavy") {
                val c = countProcesses()
                if (c > 0) lastProcs.set(c.toLong())
            }
            if (phase.contains("zram") || phase == "heavy") {
                readZramKb()?.let { lastZram.set(it) }
            }

            val zones = lastZones.get()
            AdvancedSnapshot(
                thermalZones = zones,
                maxTempC = zones.maxOfOrNull { it.tempC },
                rssKbByPackage = lastRss.get(),
                cpuTop = lastCpu.get(),
                accurateCpuPercent = lastAccCpu.get(),
                shellOk = true,
                netKbByPackage = lastNet.get(),
                processCount = lastProcs.get().toInt(),
                zramKb = lastZram.get(),
                lastPhase = phase
            )
        }
    }

    private suspend fun ensureShell(): Boolean {
        val now = System.currentTimeMillis()
        if (shellOkCached.get() && now - lastShellOkAt.get() < 20_000L) return true
        access.ensureUserService()
        var ok = access.probeShell()
        if (!ok) {
            access.ensureUserService()
            kotlinx.coroutines.delay(200)
            ok = access.probeShell()
        }
        shellOkCached.set(ok)
        if (ok) lastShellOkAt.set(now)
        return ok
    }

    private suspend fun readThermalZones(): List<ThermalZone> {
        // Cap zones read — max 12 hottest after sort
        val out = access.exec(
            "c=0; for z in /sys/class/thermal/thermal_zone*; do " +
                "[ \$c -ge 16 ] && break; " +
                "n=\$(cat \"\$z/type\" 2>/dev/null); " +
                "t=\$(cat \"\$z/temp\" 2>/dev/null); " +
                "[ -n \"\$t\" ] && echo \"\$n|\$t\"; c=\$((c+1)); done",
            2_500
        ) ?: return emptyList()
        val list = ArrayList<ThermalZone>(16)
        for (line in out.lineSequence()) {
            val parts = line.split('|', limit = 2)
            if (parts.size < 2) continue
            val milli = parts[1].trim().toLongOrNull() ?: continue
            val c = if (milli > 200) milli / 1000f else milli.toFloat()
            if (c < -50f || c > 120f) continue
            list.add(ThermalZone(parts[0].trim().ifEmpty { "zone" }, c))
        }
        return list.sortedByDescending { it.tempC }.take(12)
    }

    private suspend fun readPackageRss(): Map<String, Long> {
        // One command only — prefer compact dumpsys -s
        val raw = access.exec("dumpsys meminfo -s 2>/dev/null | head -n 80", 4_000)
            ?: access.exec("ps -A -o NAME,RSS 2>/dev/null | head -n 120", 3_000)
            ?: return emptyMap()
        val fromDump = parseDumpsysMeminfo(raw)
        if (fromDump.isNotEmpty()) return fromDump
        return parsePsRss(raw)
    }

    private suspend fun readCpuTop(): List<Pair<String, Float>> {
        val dump = access.exec("dumpsys cpuinfo 2>/dev/null | head -n 40", 4_000)
        val fromDump = parseDumpsysCpuinfo(dump)
        if (fromDump.isNotEmpty()) return fromDump
        val top = access.exec("top -b -n 1 -m 25 2>/dev/null", 3_500)
        return parseTopCpu(top)
    }

    private fun parseDumpsysCpuinfo(raw: String?): List<Pair<String, Float>> {
        if (raw.isNullOrBlank()) return emptyList()
        val acc = LinkedHashMap<String, Float>()
        val re = Regex("""^\s*([\d.]+)%\s+\d+/\S*?([a-zA-Z][a-zA-Z0-9_]*(?:\.[a-zA-Z0-9_]+){1,})""")
        for (line in raw.lineSequence()) {
            val m = re.find(line) ?: continue
            val pct = m.groupValues[1].toFloatOrNull() ?: continue
            var name = m.groupValues[2].substringBefore(':')
            if (!name.contains('.')) continue
            acc[name] = (acc[name] ?: 0f) + pct
        }
        return acc.entries.sortedByDescending { it.value }.take(30).map { it.key to it.value }
    }

    private fun parseTopCpu(raw: String?): List<Pair<String, Float>> {
        if (raw.isNullOrBlank()) return emptyList()
        val acc = LinkedHashMap<String, Float>()
        for (line in raw.lineSequence()) {
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size < 9) continue
            val pkg = parts.lastOrNull { it.contains('.') && it.matches(Regex("[a-zA-Z0-9._:]+")) } ?: continue
            val pct = parts.getOrNull(8)?.replace("%", "")?.toFloatOrNull()
                ?: parts.getOrNull(7)?.toFloatOrNull() ?: continue
            if (pct <= 0f) continue
            val name = pkg.substringBefore(':')
            if (!name.contains('.')) continue
            acc[name] = (acc[name] ?: 0f) + pct
        }
        return acc.entries.sortedByDescending { it.value }.take(30).map { it.key to it.value }
    }

    private fun parseDumpsysMeminfo(raw: String?): Map<String, Long> {
        if (raw.isNullOrBlank()) return emptyMap()
        val map = HashMap<String, Long>()
        val re = Regex("""([\d,]+)\s*kB:\s*([a-zA-Z0-9._:]+)""", RegexOption.IGNORE_CASE)
        for (line in raw.lineSequence()) {
            val m = re.find(line) ?: continue
            val kb = m.groupValues[1].replace(",", "").toLongOrNull() ?: continue
            val pkg = m.groupValues[2].substringBefore(':')
            if (!pkg.contains('.')) continue
            map[pkg] = maxOf(map[pkg] ?: 0L, kb)
        }
        return map
    }

    private fun parsePsRss(raw: String?): Map<String, Long> {
        if (raw.isNullOrBlank()) return emptyMap()
        val map = HashMap<String, Long>()
        for (line in raw.lineSequence()) {
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size < 2) continue
            val name = parts[0].substringBefore(':')
            val rss = parts.lastOrNull()?.toLongOrNull() ?: continue
            if (!name.contains('.')) continue
            map[name] = maxOf(map[name] ?: 0L, rss)
        }
        return map
    }

    private suspend fun countProcesses(): Int {
        val out = access.exec("ps -A 2>/dev/null | wc -l", 1_500) ?: return 0
        return out.trim().toIntOrNull()?.coerceAtLeast(0) ?: 0
    }

    private suspend fun readZramKb(): Long? {
        val out = access.exec(
            "cat /sys/block/zram0/mm_stat 2>/dev/null || cat /proc/swaps 2>/dev/null | head -n 3",
            1_500
        ) ?: return null
        // mm_stat: orig_data_size compr_data_size ...
        val first = out.lineSequence().firstOrNull()?.trim() ?: return null
        val nums = first.split(Regex("\\s+")).mapNotNull { it.toLongOrNull() }
        if (nums.isNotEmpty() && !first.contains("Filename")) {
            return nums[0] / 1024L // bytes → approx kb for orig size
        }
        // swaps line: /dev/block/zram0 partition size used priority
        for (line in out.lineSequence().drop(1)) {
            val p = line.trim().split(Regex("\\s+"))
            if (p.size >= 4 && p[0].contains("zram")) {
                return p[3].toLongOrNull() // already KB on many kernels
            }
        }
        return null
    }

    suspend fun forceStop(packageName: String, advancedEnabled: Boolean): Boolean {
        if (!access.isElevatedLive(advancedEnabled)) return false
        if (packageName.isBlank() || packageName.contains(' ')) return false
        if (access.forceStopPackage(packageName)) return true
        val out = access.exec("am force-stop ${packageName.trim()}; echo FERN_STOPPED", 3_000)
        return out != null && out.contains("FERN_STOPPED")
    }

    suspend fun clearAppCache(packageName: String, advancedEnabled: Boolean): Boolean {
        if (!access.isElevatedLive(advancedEnabled)) return false
        if (packageName.isBlank() || packageName.contains(' ')) return false
        val out = access.exec("pm clear --cache-only ${packageName.trim()} 2>/dev/null; echo FERN_CACHE", 4_000)
        return out != null && (out.contains("FERN_CACHE") || out.contains("Success"))
    }

    fun invalidateShellCache() {
        shellOkCached.set(false)
        lastShellOkAt.set(0L)
    }
}

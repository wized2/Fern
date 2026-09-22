package com.endroid.fern.monitor

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ThermalZone(val name: String, val tempC: Float)

data class AdvancedSnapshot(
    val thermalZones: List<ThermalZone>,
    val maxTempC: Float?,
    val rssKbByPackage: Map<String, Long>,
    val cpuTop: List<Pair<String, Float>> = emptyList(),
    val accurateCpuPercent: Float? = null,
    val shellOk: Boolean = false
)

class AdvancedMetrics(private val access: AdvancedAccess) {

    suspend fun collect(advancedEnabled: Boolean): AdvancedSnapshot = withContext(Dispatchers.IO) {
        if (!access.isElevatedLive(advancedEnabled)) {
            return@withContext AdvancedSnapshot(emptyList(), null, emptyMap())
        }

        // Binder path first (no shell) — most reliable for RAM
        var rss = access.runningAppPssKb()

        // Shell path for dumpsys / ps / thermal / cpu
        val shellOk = access.probeShell()
        if (rss.isEmpty() && shellOk) {
            rss = readPackageRss()
        }
        val zones = if (shellOk) readThermalZones() else emptyList()
        val cpu = if (shellOk) readCpuTop() else emptyList()
        val accurate = if (shellOk) access.readCpuPercent()?.first else null

        Log.i(
            "FernAdvanced",
            "collect rss=${rss.size} cpuTop=${cpu.size} zones=${zones.size} shell=$shellOk accurate=$accurate"
        )

        AdvancedSnapshot(
            thermalZones = zones,
            maxTempC = zones.maxOfOrNull { it.tempC },
            rssKbByPackage = rss,
            cpuTop = cpu,
            accurateCpuPercent = accurate,
            shellOk = shellOk
        )
    }

    private suspend fun readThermalZones(): List<ThermalZone> {
        val out = access.exec(
            "for z in /sys/class/thermal/thermal_zone*; do " +
                "n=\$(cat \"\$z/type\" 2>/dev/null); " +
                "t=\$(cat \"\$z/temp\" 2>/dev/null); " +
                "[ -n \"\$t\" ] && echo \"\$n|\$t\"; done",
            2_500
        ) ?: return emptyList()
        val list = ArrayList<ThermalZone>()
        for (line in out.lineSequence()) {
            val parts = line.split('|', limit = 2)
            if (parts.size < 2) continue
            val milli = parts[1].trim().toLongOrNull() ?: continue
            val c = if (milli > 200) milli / 1000f else milli.toFloat()
            if (c < -50f || c > 120f) continue
            list.add(ThermalZone(parts[0].trim().ifEmpty { "zone" }, c))
        }
        return list.sortedByDescending { it.tempC }
    }

    private suspend fun readPackageRss(): Map<String, Long> {
        val dumpsys = access.exec("dumpsys meminfo -s 2>/dev/null | head -n 100", 3_500)
        val fromDump = parseDumpsysMeminfo(dumpsys)
        if (fromDump.isNotEmpty()) return fromDump
        val ps = access.exec("ps -A -o NAME,RSS 2>/dev/null | head -n 250", 2_500)
        return parsePsRss(ps)
    }

    private suspend fun readCpuTop(): List<Pair<String, Float>> {
        val raw = access.exec("dumpsys cpuinfo 2>/dev/null | head -n 50", 3_500) ?: return emptyList()
        val list = ArrayList<Pair<String, Float>>()
        val re = Regex("""^\s*([\d.]+)%\s+\d+/(?:[\w.]+:)?([a-zA-Z0-9._]+)""")
        for (line in raw.lineSequence()) {
            val m = re.find(line) ?: continue
            val pct = m.groupValues[1].toFloatOrNull() ?: continue
            val name = m.groupValues[2]
            if (!name.contains('.')) continue
            list.add(name to pct)
            if (list.size >= 20) break
        }
        return list
    }

    private fun parseDumpsysMeminfo(raw: String?): Map<String, Long> {
        if (raw.isNullOrBlank()) return emptyMap()
        val map = HashMap<String, Long>()
        val re = Regex("""([\d,]+)\s*kB:\s*([a-zA-Z0-9._]+)""")
        for (line in raw.lineSequence()) {
            val m = re.find(line) ?: continue
            val kb = m.groupValues[1].replace(",", "").toLongOrNull() ?: continue
            val pkg = m.groupValues[2]
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
            val name = parts[0]
            val rss = parts[1].toLongOrNull() ?: continue
            if (!name.contains('.')) continue
            map[name] = maxOf(map[name] ?: 0L, rss)
        }
        return map
    }

    suspend fun forceStop(packageName: String, advancedEnabled: Boolean): Boolean {
        if (!access.isElevatedLive(advancedEnabled)) return false
        if (packageName.isBlank() || packageName.contains(' ')) return false
        if (access.forceStopPackage(packageName)) return true
        val out = access.exec("am force-stop ${packageName.trim()}; echo DONE", 3_000)
        return out != null && out.contains("DONE")
    }
}

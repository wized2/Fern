package com.endroid.fern.monitor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ThermalZone(val name: String, val tempC: Float)

data class AdvancedSnapshot(
    val thermalZones: List<ThermalZone>,
    val maxTempC: Float?,
    val rssKbByPackage: Map<String, Long>,
    val cpuTop: List<Pair<String, Float>> = emptyList()
)

/**
 * Elevated-only metrics via Shizuku/root shell.
 */
class AdvancedMetrics(private val access: AdvancedAccess) {

    suspend fun collect(advancedEnabled: Boolean): AdvancedSnapshot = withContext(Dispatchers.IO) {
        if (!access.isElevated(advancedEnabled)) {
            return@withContext AdvancedSnapshot(emptyList(), null, emptyMap())
        }
        val zones = readThermalZones()
        val maxT = zones.maxOfOrNull { it.tempC }
        val rss = readPackageRss()
        val cpu = readCpuTop()
        AdvancedSnapshot(zones, maxT, rss, cpu)
    }

    private suspend fun readThermalZones(): List<ThermalZone> {
        val out = access.exec(
            "for z in /sys/class/thermal/thermal_zone*; do " +
                "n=\$(cat \"\$z/type\" 2>/dev/null); " +
                "t=\$(cat \"\$z/temp\" 2>/dev/null); " +
                "[ -n \"\$t\" ] && echo \"\$n|\$t\"; done",
            4_000
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
        val dumpsys = access.exec("dumpsys meminfo -s 2>/dev/null | head -n 100", 5_000)
        val fromDump = parseDumpsysMeminfo(dumpsys)
        if (fromDump.isNotEmpty()) return fromDump
        val ps = access.exec("ps -A -o NAME,RSS 2>/dev/null | head -n 250", 4_000)
        return parsePsRss(ps)
    }

    private suspend fun readCpuTop(): List<Pair<String, Float>> {
        // dumpsys cpuinfo first lines: "  12% 1234/com.example: 10% user + 2% kernel"
        val raw = access.exec("dumpsys cpuinfo 2>/dev/null | head -n 40", 5_000) ?: return emptyList()
        val list = ArrayList<Pair<String, Float>>()
        val re = Regex("""^\s*([\d.]+)%\s+\d+/(?:[\w.]+:)?([a-zA-Z0-9._]+)""")
        for (line in raw.lineSequence()) {
            val m = re.find(line) ?: continue
            val pct = m.groupValues[1].toFloatOrNull() ?: continue
            val name = m.groupValues[2]
            if (!name.contains('.')) continue
            list.add(name to pct)
            if (list.size >= 15) break
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
        if (!access.isElevated(advancedEnabled)) return false
        if (packageName.isBlank() || packageName.contains(' ')) return false
        access.exec("am force-stop ${packageName.trim()}", 4_000)
        return true
    }
}

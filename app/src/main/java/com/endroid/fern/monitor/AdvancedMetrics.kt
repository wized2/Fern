package com.endroid.fern.monitor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ThermalZone(
    val name: String,
    val tempC: Float
)

data class AdvancedSnapshot(
    val zones: List<ThermalZone>,
    /** Hottest zone temperature, if any. */
    val maxTempC: Float?,
    /** packageName → RSS KB from elevated `ps` / dumpsys. */
    val rssKbByPackage: Map<String, Long>
)

/**
 * Elevated-only metrics. Safe no-ops when [AdvancedAccess] cannot run a shell.
 */
class AdvancedMetrics(private val access: AdvancedAccess) {

    suspend fun collect(advancedEnabled: Boolean): AdvancedSnapshot =
        withContext(Dispatchers.IO) {
            if (!access.isElevated(advancedEnabled)) {
                return@withContext AdvancedSnapshot(emptyList(), null, emptyMap())
            }
            val zones = readThermalZones()
            val rss = readPackageRss()
            AdvancedSnapshot(
                zones = zones,
                maxTempC = zones.maxOfOrNull { it.tempC },
                rssKbByPackage = rss
            )
        }

    private suspend fun readThermalZones(): List<ThermalZone> {
        // One shell: type + temp for each zone (millidegree integers)
        val script = """
            for z in /sys/class/thermal/thermal_zone*; do
              [ -f "${'$'}z/temp" ] || continue
              t=${'$'}(cat "${'$'}z/temp" 2>/dev/null) || continue
              n=${'$'}(cat "${'$'}z/type" 2>/dev/null || basename "${'$'}z")
              echo "${'$'}n|${'$'}t"
            done
        """.trimIndent().replace("\n", " ")
        val out = access.exec(script, 4_000) ?: return emptyList()
        val list = ArrayList<ThermalZone>()
        for (line in out.lineSequence()) {
            val parts = line.split('|', limit = 2)
            if (parts.size < 2) continue
            val milli = parts[1].trim().toLongOrNull() ?: continue
            // Some kernels report already in °C (small integers); most use milli-°C
            val c = if (milli > 200) milli / 1000f else milli.toFloat()
            if (c < -50f || c > 120f) continue
            list.add(ThermalZone(parts[0].trim().ifEmpty { "zone" }, c))
        }
        return list.sortedByDescending { it.tempC }
    }

    private suspend fun readPackageRss(): Map<String, Long> {
        // Prefer dumpsys meminfo summary; fall back to ps
        val dumpsys = access.exec("dumpsys meminfo -s 2>/dev/null | head -n 80", 5_000)
        val fromDump = parseDumpsysMeminfo(dumpsys)
        if (fromDump.isNotEmpty()) return fromDump
        val ps = access.exec("ps -A -o NAME,RSS 2>/dev/null | head -n 200", 4_000)
        return parsePsRss(ps)
    }

    private fun parseDumpsysMeminfo(raw: String?): Map<String, Long> {
        if (raw.isNullOrBlank()) return emptyMap()
        val map = HashMap<String, Long>()
        // Lines often look like: "    12,345 kB: com.example.app (pid 1234 / activities)"
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

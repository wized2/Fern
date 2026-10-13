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
    val shellOk: Boolean = false,
    val netKbByPackage: Map<String, Long> = emptyMap(),
    val processCount: Int = 0
)

class AdvancedMetrics(private val access: AdvancedAccess) {

    suspend fun collect(advancedEnabled: Boolean): AdvancedSnapshot = withContext(Dispatchers.IO) {
        if (!access.isElevatedLive(advancedEnabled)) {
            return@withContext AdvancedSnapshot(emptyList(), null, emptyMap())
        }

        access.ensureUserService()
        var shellOk = access.probeShell()
        if (!shellOk) {
            // one more bind attempt
            access.ensureUserService()
            kotlinx.coroutines.delay(400)
            shellOk = access.probeShell()
        }

        val rss = if (shellOk) readPackageRss() else emptyMap()
        val zones = if (shellOk) readThermalZones() else emptyList()
        val cpu = if (shellOk) readCpuTop() else emptyList()
        val accurate = if (shellOk) access.readCpuPercent()?.first else null
        val net = if (shellOk) readNetKb() else emptyMap()
        val procs = if (shellOk) countProcesses() else 0

        Log.i(
            "FernAdvanced",
            "collect shell=$shellOk rss=${rss.size} cpu=${cpu.size} zones=${zones.size} net=${net.size} acc=$accurate"
        )

        AdvancedSnapshot(
            thermalZones = zones,
            maxTempC = zones.maxOfOrNull { it.tempC },
            rssKbByPackage = rss,
            cpuTop = cpu,
            accurateCpuPercent = accurate,
            shellOk = shellOk,
            netKbByPackage = net,
            processCount = procs
        )
    }

    private suspend fun readThermalZones(): List<ThermalZone> {
        val out = access.exec(
            "for z in /sys/class/thermal/thermal_zone*; do " +
                "n=\$(cat \"\$z/type\" 2>/dev/null); " +
                "t=\$(cat \"\$z/temp\" 2>/dev/null); " +
                "[ -n \"\$t\" ] && echo \"\$n|\$t\"; done",
            3_000
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
        // Prefer dumpsys meminfo --package (modern) then -s, then ps
        val attempts = listOf(
            "dumpsys meminfo -s 2>/dev/null | head -n 200",
            "dumpsys meminfo 2>/dev/null | grep -E 'kB:|K:|TOTAL' | head -n 250",
            "ps -A -o NAME,RSS 2>/dev/null | head -n 400",
            "ps -A -o ARGS,RSS 2>/dev/null | head -n 400"
        )
        for (cmd in attempts) {
            val raw = access.exec(cmd, 6_000) ?: continue
            val map = when {
                cmd.startsWith("ps") -> parsePsRss(raw)
                else -> parseDumpsysMeminfo(raw).ifEmpty { parsePsRss(raw) }
            }
            if (map.isNotEmpty()) return map
        }
        return emptyMap()
    }

    private suspend fun readCpuTop(): List<Pair<String, Float>> {
        // 1) dumpsys cpuinfo (most accurate process attribution on Android)
        val dump = access.exec("dumpsys cpuinfo 2>/dev/null | head -n 80", 6_000)
        val fromDump = parseDumpsysCpuinfo(dump)
        if (fromDump.isNotEmpty()) return fromDump

        // 2) toybox top
        val top = access.exec("top -b -n 1 -m 40 2>/dev/null", 5_000)
        val fromTop = parseTopCpu(top)
        if (fromTop.isNotEmpty()) return fromTop

        // 3) ps with %CPU when available
        val ps = access.exec("ps -A -o NAME,%CPU 2>/dev/null | head -n 200", 4_000)
        return parsePsCpu(ps)
    }

    private fun parseDumpsysCpuinfo(raw: String?): List<Pair<String, Float>> {
        if (raw.isNullOrBlank()) return emptyMap<String, Float>().toList()
        val acc = LinkedHashMap<String, Float>()
        // Formats seen across OEMs:
        // "  12% 1000/com.example.app: 8% user + 4% kernel"
        // "  3.5% 1234/com.foo:bar: 2% user + 1% kernel / faults ..."
        // "  0.1% 567/system_server: ..."
        val patterns = listOf(
            Regex("""^\s*([\d.]+)%\s+\d+/(?:u\d+_a\d+:)?([a-zA-Z0-9._]+(?::[a-zA-Z0-9._]+)?)"""),
            Regex("""^\s*([\d.]+)%\s+\d+/\S*?([a-zA-Z][a-zA-Z0-9_]*(?:\.[a-zA-Z0-9_]+){1,})""")
        )
        for (line in raw.lineSequence()) {
            var matched = false
            for (re in patterns) {
                val m = re.find(line) ?: continue
                val pct = m.groupValues[1].toFloatOrNull() ?: continue
                var name = m.groupValues[2]
                // strip process suffix after colon for grouping (com.app:service → com.app)
                if (name.contains(':') && name.contains('.')) {
                    name = name.substringBefore(':')
                }
                if (!name.contains('.')) continue
                acc[name] = (acc[name] ?: 0f) + pct
                matched = true
                break
            }
            if (!matched) continue
        }
        return acc.entries
            .sortedByDescending { it.value }
            .take(40)
            .map { it.key to it.value }
    }

    private fun parseTopCpu(raw: String?): List<Pair<String, Float>> {
        if (raw.isNullOrBlank()) return emptyList()
        val acc = LinkedHashMap<String, Float>()
        // PID USER PR NI VIRT RES SHR S [%CPU] %MEM TIME+ ARGS
        for (line in raw.lineSequence()) {
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size < 9) continue
            val cpuIdx = parts.indexOfFirst { it.contains('%') && it.replace("%", "").toFloatOrNull() != null }
            // find package-like token
            val pkg = parts.lastOrNull { it.contains('.') && it.matches(Regex("[a-zA-Z0-9._:]+")) } ?: continue
            val pct = when {
                cpuIdx >= 0 -> parts[cpuIdx].replace("%", "").toFloatOrNull()
                else -> parts.getOrNull(8)?.replace("%", "")?.toFloatOrNull()
                    ?: parts.getOrNull(7)?.toFloatOrNull()
            } ?: continue
            if (pct <= 0f) continue
            val name = pkg.substringBefore(':')
            if (!name.contains('.')) continue
            acc[name] = (acc[name] ?: 0f) + pct
        }
        return acc.entries.sortedByDescending { it.value }.take(40).map { it.key to it.value }
    }

    private fun parsePsCpu(raw: String?): List<Pair<String, Float>> {
        if (raw.isNullOrBlank()) return emptyList()
        val acc = LinkedHashMap<String, Float>()
        for (line in raw.lineSequence()) {
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size < 2) continue
            val name = parts[0].substringBefore(':')
            val pct = parts[1].replace("%", "").toFloatOrNull() ?: continue
            if (!name.contains('.') || pct <= 0f) continue
            acc[name] = (acc[name] ?: 0f) + pct
        }
        return acc.entries.sortedByDescending { it.value }.take(40).map { it.key to it.value }
    }

    private fun parseDumpsysMeminfo(raw: String?): Map<String, Long> {
        if (raw.isNullOrBlank()) return emptyMap()
        val map = HashMap<String, Long>()
        val patterns = listOf(
            Regex("""([\d,]+)\s*kB:\s*([a-zA-Z0-9._:]+)""", RegexOption.IGNORE_CASE),
            Regex("""([\d,]+)\s*K:\s*([a-zA-Z0-9._:]+)""", RegexOption.IGNORE_CASE),
            Regex("""^\s*([\d,]+)\s+([\d,]+)\s+([\d,]+)\s+([\d,]+)\s+([a-zA-Z0-9._:]+)""")
        )
        for (line in raw.lineSequence()) {
            var hit = false
            for ((idx, re) in patterns.withIndex()) {
                val m = re.find(line) ?: continue
                val kb = when (idx) {
                    2 -> m.groupValues[1].replace(",", "").toLongOrNull() // pss-like
                    else -> m.groupValues[1].replace(",", "").toLongOrNull()
                } ?: continue
                val pkgRaw = when (idx) {
                    2 -> m.groupValues[5]
                    else -> m.groupValues[2]
                }
                val pkg = pkgRaw.substringBefore(':')
                if (!pkg.contains('.')) continue
                map[pkg] = maxOf(map[pkg] ?: 0L, kb)
                hit = true
                break
            }
            if (!hit) continue
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
            val rss = parts.lastOrNull()?.toLongOrNull()
                ?: parts.getOrNull(1)?.toLongOrNull()
                ?: continue
            if (!name.contains('.')) continue
            map[name] = maxOf(map[name] ?: 0L, rss)
        }
        return map
    }

    private suspend fun readNetKb(): Map<String, Long> {
        // Aggregate /proc/net/xt_qtaguid/stats or /proc/net/xt_qtaguid if present (older)
        // On modern Android, try dumpsys netstats detail (heavy) — use lighter uid_stat if available
        val raw = access.exec(
            "if [ -d /proc/uid_stat ]; then " +
                "for d in /proc/uid_stat/*; do " +
                "u=\$(basename \"\$d\"); " +
                "t=\$(( \$(cat \"\$d/tcp_rcv\" 2>/dev/null || echo 0) + \$(cat \"\$d/tcp_snd\" 2>/dev/null || echo 0) )); " +
                "echo \"\$u \$t\"; done; " +
                "fi",
            4_000
        ) ?: return emptyMap()
        // Map uid → bytes; resolve package via pm path is slow — store as uid string keys u:12345
        val map = HashMap<String, Long>()
        for (line in raw.lineSequence()) {
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size < 2) continue
            val uid = parts[0]
            val bytes = parts[1].toLongOrNull() ?: continue
            if (bytes <= 0) continue
            map["uid:$uid"] = bytes / 1024L
        }
        return map
    }

    private suspend fun countProcesses(): Int {
        val out = access.exec("ps -A 2>/dev/null | wc -l", 2_000) ?: return 0
        return out.trim().toIntOrNull()?.coerceAtLeast(0) ?: 0
    }

    suspend fun forceStop(packageName: String, advancedEnabled: Boolean): Boolean {
        if (!access.isElevatedLive(advancedEnabled)) return false
        if (packageName.isBlank() || packageName.contains(' ')) return false
        if (access.forceStopPackage(packageName)) return true
        val out = access.exec("am force-stop ${packageName.trim()}; echo FERN_STOPPED", 4_000)
        return out != null && out.contains("FERN_STOPPED")
    }

    suspend fun clearAppCache(packageName: String, advancedEnabled: Boolean): Boolean {
        if (!access.isElevatedLive(advancedEnabled)) return false
        if (packageName.isBlank() || packageName.contains(' ')) return false
        val out = access.exec("pm clear --cache-only ${packageName.trim()} 2>/dev/null; echo FERN_CACHE", 5_000)
        if (out != null && out.contains("FERN_CACHE")) return true
        val out2 = access.exec("pm clear ${packageName.trim()} 2>/dev/null; echo FERN_CLEAR", 5_000)
        return out2 != null && out2.contains("Success")
    }
}

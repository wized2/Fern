package com.endroid.fern.monitor

/**
 * Pure parsing of kernel/sys text formats. No Android APIs — unit-testable.
 */
object MetricsParse {

    /**
     * Parse the aggregate `cpu ` line from /proc/stat.
     * @return idle (idle+iowait) to total jiffies, or null if not usable
     */
    fun parseProcStatLine(line: String?): Pair<Long, Long>? {
        if (line.isNullOrBlank()) return null
        val trimmed = line.trim()
        if (!trimmed.startsWith("cpu")) return null
        // Reject per-core lines: cpu0, cpu12, …
        if (trimmed.length > 3 && trimmed[3].isDigit()) return null
        val parts = trimmed.split(Regex("\\s+")).drop(1).mapNotNull { it.toLongOrNull() }
        if (parts.size < 4) return null
        val idle = parts[3] + parts.getOrElse(4) { 0L }
        val total = parts.sum()
        if (total <= 0L) return null
        return idle to total
    }

    /** MemTotal / MemAvailable in kB from /proc/meminfo text. */
    fun parseMeminfoKb(text: String): Pair<Long, Long>? {
        val total = Regex("""MemTotal:\s+(\d+)""").find(text)?.groupValues?.get(1)?.toLongOrNull()
        val avail = Regex("""MemAvailable:\s+(\d+)""").find(text)?.groupValues?.get(1)?.toLongOrNull()
        if (total == null || avail == null || total <= 0L) return null
        return total to avail
    }

    /** loadavg 1/5/15 from first three fields of /proc/loadavg. */
    fun parseLoadavg(line: String?): FloatArray? {
        if (line.isNullOrBlank()) return null
        val p = line.trim().split(Regex("\\s+"))
        if (p.size < 3) return null
        val a = p[0].toFloatOrNull() ?: return null
        val b = p[1].toFloatOrNull() ?: return null
        val c = p[2].toFloatOrNull() ?: return null
        return floatArrayOf(a, b, c)
    }

    /**
     * CPU usage percent from two successive (idle, total) samples.
     * @return null if samples are unusable or total did not advance
     */
    fun cpuPercentFromDelta(idle1: Long, total1: Long, idle2: Long, total2: Long): Float? {
        val dTotal = total2 - total1
        val dIdle = idle2 - idle1
        if (dTotal <= 0L) return null
        val busy = (dTotal - dIdle).coerceAtLeast(0L)
        return (busy.toDouble() / dTotal.toDouble() * 100.0).toFloat().coerceIn(0f, 100f)
    }
}

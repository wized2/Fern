package com.endroid.fern

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure parsing helpers — keep offline and deterministic for CI.
 * Mirrors the /proc and sysfs formats Fern reads on device.
 */
class SystemMetricsParseTest {

    @Test
    fun parseMeminfoUsedTotal() {
        val sample = """
            MemTotal:        8000000 kB
            MemFree:         1000000 kB
            MemAvailable:    3000000 kB
            Buffers:          200000 kB
            Cached:          1500000 kB
        """.trimIndent()
        val totalKb = Regex("""MemTotal:\s+(\d+)""").find(sample)!!.groupValues[1].toLong()
        val availKb = Regex("""MemAvailable:\s+(\d+)""").find(sample)!!.groupValues[1].toLong()
        val usedMb = (totalKb - availKb) / 1024.0
        val totalMb = totalKb / 1024.0
        assertEquals(8000000L, totalKb)
        assertTrue(usedMb > 0)
        assertTrue(totalMb > usedMb)
    }

    @Test
    fun parseLoadavg() {
        val line = "0.52 0.58 0.59 2/980 12345"
        val parts = line.trim().split(Regex("\\s+"))
        val l1 = parts[0].toFloat()
        val l5 = parts[1].toFloat()
        val l15 = parts[2].toFloat()
        assertEquals(0.52f, l1, 0.001f)
        assertEquals(0.58f, l5, 0.001f)
        assertEquals(0.59f, l15, 0.001f)
    }

    @Test
    fun parseStatCpuDelta() {
        // cpu  user nice system idle iowait irq softirq
        fun parse(line: String): LongArray {
            val p = line.removePrefix("cpu").trim().split(Regex("\\s+")).map { it.toLong() }
            return longArrayOf(p[0], p[1], p[2], p[3])
        }
        val a = parse("cpu  100 10 50 1000 0 0 0")
        val b = parse("cpu  140 10 70 1100 0 0 0")
        val idle = (b[3] - a[3]).toDouble()
        val total = (b.sum() - a.sum()).toDouble()
        val usage = ((total - idle) / total * 100.0)
        assertTrue(usage in 1.0..100.0)
    }

    @Test
    fun parseStatCpuIgnoresTrailingFields() {
        // Mirrors parseProcStatLine: idle = user-facing idle + iowait (parts[3] + parts[4]);
        // total sums every numeric field. Extra columns (steal, guest, guest_nice, …) must not throw.
        fun parse(line: String): Pair<Long, Long>? {
            val trimmed = line.trim()
            if (!trimmed.startsWith("cpu")) return null
            if (trimmed.length > 3 && trimmed[3].isDigit()) return null
            val parts = trimmed.split(Regex("\\s+")).drop(1).mapNotNull { it.toLongOrNull() }
            if (parts.size < 4) return null
            val idle = parts[3] + parts.getOrElse(4) { 0L }
            val total = parts.sum()
            if (total <= 0L) return null
            return idle to total
        }
        val seven = parse("cpu  100 10 50 1000 20 0 0")
        val eightPlus = parse("cpu  100 10 50 1000 20 0 0 5 3")
        assertEquals(seven, eightPlus)
        val withSteal = parse("cpu  100 10 50 1000 20 0 0 40")
        assertEquals(seven!!.first, withSteal!!.first)
        assertTrue(withSteal.second > seven.second)
        assertEquals(null, parse("cpu0 1 2 3 4"))
        assertEquals(null, parse("cpu  1 2"))
    }

    @Test
    fun thermalLabelThresholds() {
        fun label(c: Float): String = when {
            c >= 50f -> "extreme"
            c >= 40f -> "hot"
            else -> "normal"
        }
        assertEquals("normal", label(36f))
        assertEquals("hot", label(42f))
        assertEquals("extreme", label(51f))
    }

    @Test
    fun csvLineEscapes() {
        fun csv(v: String) = "\"" + v.replace("\"", "\"\"") + "\""
        assertEquals("\"a,b\"", csv("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", csv("say \"hi\""))
    }
}

    @Test
    fun batteryCsvHeaderAndRows() {
        val csv = com.endroid.fern.monitor.HistoryExport.batteryCsv(listOf(80f, 79f), startEpochMs = 0L)
        assertTrue(csv.startsWith("index,epoch_ms,battery_percent"))
        assertTrue(csv.contains("0,0,80.0") || csv.contains("0,0,80"))
    }

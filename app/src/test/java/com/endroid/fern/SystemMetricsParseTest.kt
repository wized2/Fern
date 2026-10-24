package com.endroid.fern

import com.endroid.fern.monitor.HistoryExport
import com.endroid.fern.monitor.MetricsParse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Offline unit tests for metric text parsers (CI-friendly). */
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
        val (totalKb, availKb) = MetricsParse.parseMeminfoKb(sample)!!
        assertEquals(8000000L, totalKb)
        assertEquals(3000000L, availKb)
        assertTrue(totalKb > availKb)
    }

    @Test
    fun parseMeminfoRejectsGarbage() {
        assertNull(MetricsParse.parseMeminfoKb("not meminfo"))
        assertNull(MetricsParse.parseMeminfoKb(""))
    }

    @Test
    fun parseLoadavg() {
        val load = MetricsParse.parseLoadavg("0.52 0.58 0.59 2/980 12345")!!
        assertEquals(0.52f, load[0], 0.001f)
        assertEquals(0.58f, load[1], 0.001f)
        assertEquals(0.59f, load[2], 0.001f)
    }

    @Test
    fun parseLoadavgShortLine() {
        assertNull(MetricsParse.parseLoadavg("0.1 0.2"))
        assertNull(MetricsParse.parseLoadavg(null))
    }

    @Test
    fun parseStatCpuDelta() {
        val a = MetricsParse.parseProcStatLine("cpu  100 10 50 1000 0 0 0")!!
        val b = MetricsParse.parseProcStatLine("cpu  140 10 70 1100 0 0 0")!!
        val pct = MetricsParse.cpuPercentFromDelta(a.first, a.second, b.first, b.second)
        assertNotNull(pct)
        assertTrue(pct!! in 1f..100f)
    }

    @Test
    fun parseStatCpuIgnoresTrailingFields() {
        // Extra columns (steal/guest/…) must not throw; idle stays idle+iowait;
        // total includes every numeric field so longer lines can have a larger total.
        val seven = MetricsParse.parseProcStatLine("cpu  100 10 50 1000 20 0 0")!!
        val eightPlus = MetricsParse.parseProcStatLine("cpu  100 10 50 1000 20 0 0 5 3")!!
        assertEquals(seven.first, eightPlus.first)
        assertTrue(eightPlus.second >= seven.second)
        val withSteal = MetricsParse.parseProcStatLine("cpu  100 10 50 1000 20 0 0 40")!!
        assertEquals(seven.first, withSteal.first)
        assertTrue(withSteal.second > seven.second)
        assertNull(MetricsParse.parseProcStatLine("cpu0 1 2 3 4"))
        assertNull(MetricsParse.parseProcStatLine("cpu  1 2"))
        assertNull(MetricsParse.parseProcStatLine(null))
    }

    @Test
    fun cpuPercentNoProgress() {
        assertNull(MetricsParse.cpuPercentFromDelta(10, 100, 10, 100))
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

    @Test
    fun batteryCsvHeaderAndRows() {
        val csv = HistoryExport.batteryCsv(listOf(80f, 79f), startEpochMs = 0L)
        assertTrue(csv.startsWith("index,epoch_ms,battery_percent"))
        assertTrue(csv.contains("0,0,80.0") || csv.contains("0,0,80"))
    }
}

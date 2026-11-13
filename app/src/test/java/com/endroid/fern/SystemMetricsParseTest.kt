package com.endroid.fern

import com.endroid.fern.monitor.HistoryExport
import com.endroid.fern.monitor.MetricsParse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun parseMeminfoMissingAvailable() {
        val sample = """
            MemTotal:        8000000 kB
            MemFree:         1000000 kB
        """.trimIndent()
        assertNull(MetricsParse.parseMeminfoKb(sample))
    }

    @Test
    fun parseMeminfoZeroTotal() {
        val sample = """
            MemTotal:        0 kB
            MemAvailable:    3000000 kB
        """.trimIndent()
        assertNull(MetricsParse.parseMeminfoKb(sample))
    }

    @Test
    fun parseMeminfoExtraWhitespace() {
        val sample = "MemTotal: \t  8000000   kB\nMemAvailable:\t3000000 kB"
        val (totalKb, availKb) = MetricsParse.parseMeminfoKb(sample)!!
        assertEquals(8000000L, totalKb)
        assertEquals(3000000L, availKb)
    }

    @Test
    fun parseLoadavgFewerThanThreeFields() {
        assertNull(MetricsParse.parseLoadavg("0.1"))
        assertNull(MetricsParse.parseLoadavg("0.1 0.2"))
    }

    @Test
    fun parseLoadavgNonNumericFields() {
        assertNull(MetricsParse.parseLoadavg("abc 0.2 0.3"))
        assertNull(MetricsParse.parseLoadavg("0.1 xx 0.3"))
        assertNull(MetricsParse.parseLoadavg("0.1 0.2 0.3x"))
    }

    @Test
    fun parseLoadavgBlankInput() {
        assertNull(MetricsParse.parseLoadavg(""))
        assertNull(MetricsParse.parseLoadavg("   "))
    }

    @Test
    fun isValidPackageNameAccepts() {
        assertTrue(MetricsParse.isValidPackageName("com.example.app"))
        assertTrue(MetricsParse.isValidPackageName("com.endroid.fern.beta"))
        assertTrue(MetricsParse.isValidPackageName("com.example.app_2"))
    }

    @Test
    fun isValidPackageNameRejects() {
        assertFalse(MetricsParse.isValidPackageName("foo"))
        assertFalse(MetricsParse.isValidPackageName("a b"))
        assertFalse(MetricsParse.isValidPackageName("a;b.c"))
        assertFalse(MetricsParse.isValidPackageName("x.y\$(id)"))
        assertFalse(MetricsParse.isValidPackageName(""))
        assertFalse(MetricsParse.isValidPackageName(null))
        assertFalse(MetricsParse.isValidPackageName("com.example." + "a".repeat(244)))
        assertFalse(MetricsParse.isValidPackageName("1com.example"))
        assertFalse(MetricsParse.isValidPackageName("com..example"))
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

@Test
    fun historyExportSessionCsvHasHeaderAndRows() {
        val csv = HistoryExport.sessionCsv(
            cpu = listOf(10f, 20f),
            ram = listOf(30f, 40f),
            battery = listOf(90f, 89f),
            intervalMs = 1000,
            startEpochMs = 0L,
        )
        assertTrue(csv.startsWith("index,epoch_ms,cpu_percent"))
        assertTrue(csv.contains("0,0,10.00,30.00,90.00"))
        assertTrue(csv.contains("1,1000,20.00,40.00,89.00"))
    }

    @Test
    fun historyExportMarkdownHasTable() {
        val md = HistoryExport.snapshotMarkdown(
            deviceModel = "Pixel",
            androidVersion = "14",
            sdkInt = 34,
            cpuPercent = 12.5f,
            cpuCores = 8,
            cpuHardware = "tensor",
            ramUsedMb = 4000,
            ramTotalMb = 8000,
            ramPercent = 50f,
            batteryPercent = 80,
            batteryCharging = false,
            batteryHealth = "Good",
            batteryTempC = 31.2f,
            thermalLabel = "None",
            networkLabel = "Wi‑Fi",
            networkKBps = 12f,
            uptimeHours = 3.5f,
            peakCpu = 40f,
            peakRam = 60f,
            avgCpu = 15f,
            avgRam = 45f,
        )
        assertTrue(md.contains("# Fern snapshot"))
        assertTrue(md.contains("| Device | Pixel |"))
        assertTrue(md.contains("CPU"))
    }

    @Test
    fun parseProcStatNullAndJunk() {
        assertNull(MetricsParse.parseProcStatLine(null))
        assertNull(MetricsParse.parseProcStatLine(""))
        assertNull(MetricsParse.parseProcStatLine("not-a-cpu-line"))
        assertNull(MetricsParse.parseProcStatLine("cpu"))
    }

    @Test
    fun sessionCsvPadsMissingSeries() {
        val csv = HistoryExport.sessionCsv(
            cpu = listOf(1f),
            ram = emptyList(),
            battery = listOf(50f, 49f),
            intervalMs = 500,
            startEpochMs = 1000L,
        )
        assertTrue(csv.contains("index,epoch_ms"))
        assertTrue(csv.contains("0,1000,"))
        assertTrue(csv.contains("1,1500,"))
    }
}

package com.endroid.fern

import com.endroid.fern.monitor.HistoryExport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/** Exports must not change with the device language (a decimal comma would split CSV columns). */
class HistoryExportLocaleTest {

    private fun <T> withLocale(l: Locale, block: () -> T): T {
        val old = Locale.getDefault()
        Locale.setDefault(l)
        try { return block() } finally { Locale.setDefault(old) }
    }

    @Test
    fun sessionCsvUsesDotDecimalsInGerman() {
        val csv = withLocale(Locale.GERMANY) {
            HistoryExport.sessionCsv(listOf(10.5f), listOf(30.25f), listOf(90f), 1000, 0L)
        }
        val row = csv.lines()[1]
        assertEquals("0,0,10.50,30.25,90.00", row)
        assertEquals(5, row.split(',').size)
    }

    @Test
    fun markdownUsesDotDecimalsInSpanish() {
        val md = withLocale(Locale("es", "ES")) { snapshot("Pixel") }
        assertTrue(md.contains("12.5%"))
        assertFalse(md.contains("12,5%"))
    }

    @Test
    fun markdownEscapesPipesInTextFields() {
        val md = snapshot("Model | X\nline2")
        assertTrue(md.contains("| Device | Model \\| X line2 |"))
    }

    private fun snapshot(model: String) = HistoryExport.snapshotMarkdown(
        deviceModel = model, androidVersion = "14", sdkInt = 34,
        cpuPercent = 12.5f, cpuCores = 8, cpuHardware = "tensor",
        ramUsedMb = 4000, ramTotalMb = 8000, ramPercent = 50f,
        batteryPercent = 80, batteryCharging = false, batteryHealth = "Good", batteryTempC = 31.2f,
        thermalLabel = "None", networkLabel = "Wi-Fi", networkKBps = 12f,
        uptimeHours = 3.5f, peakCpu = 40f, peakRam = 60f, avgCpu = 15f, avgRam = 45f,
    )
}

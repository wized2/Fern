package com.endroid.fern

import com.endroid.fern.monitor.HistoryExport
import org.junit.Assert.assertEquals
import org.junit.Test

/** Edge cases for the CSV exports: empty input and series of different lengths. */
class HistoryExportEdgeCaseTest {
    private val header = "index,epoch_ms,cpu_percent,ram_percent,battery_percent"

    @Test
    fun sessionCsvWithNoSamplesIsHeaderOnly() {
        val csv = HistoryExport.sessionCsv(emptyList(), emptyList(), emptyList(), 1000, 0L)
        assertEquals(header + "\n", csv)
    }

    @Test
    fun sessionCsvPadsShorterSeriesWithEmptyCells() {
        val csv = HistoryExport.sessionCsv(
            cpu = listOf(1f, 2f, 3f),
            ram = listOf(10f),
            battery = listOf(50f, 49f),
            intervalMs = 500,
            startEpochMs = 1000L,
        )
        assertEquals(
            listOf(
                header,
                "0,1000,1.00,10.00,50.00",
                "1,1500,2.00,,49.00",
                "2,2000,3.00,,",
            ),
            csv.trimEnd().lines()
        )
    }

    @Test
    fun everySessionRowHasFiveColumns() {
        val csv = HistoryExport.sessionCsv(listOf(1f), emptyList(), listOf(2f, 3f, 4f), 1000, 0L)
        csv.trimEnd().lines().forEach { assertEquals(5, it.split(',').size) }
    }

    @Test
    fun batteryCsvWithNoSamplesIsHeaderOnly() {
        assertEquals("index,epoch_ms,battery_percent\n", HistoryExport.batteryCsv(emptyList(), 0L))
    }

    @Test
    fun batteryCsvStepsOneSecondPerSample() {
        val rows = HistoryExport.batteryCsv(listOf(80f, 79f), 5000L).trimEnd().lines()
        assertEquals("0,5000,80.0", rows[1])
        assertEquals("1,6000,79.0", rows[2])
    }
}

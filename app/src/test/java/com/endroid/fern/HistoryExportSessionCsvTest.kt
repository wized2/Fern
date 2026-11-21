package com.endroid.fern

import com.endroid.fern.monitor.HistoryExport
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

/**
 * Edge-case unit tests for [HistoryExport.sessionCsv] (issue #33).
 * Pure JVM/Kotlin — no Android APIs required.
 */
class HistoryExportSessionCsvTest {

    private companion object {
        const val HEADER = "index,epoch_ms,cpu_percent,ram_percent,battery_percent"

        /** Drop the trailing empty entry produced by the final line separator. */
        fun rows(csv: String) = csv.lines().filter { it.isNotEmpty() }
    }

    /** sessionCsv formats with the default locale, so pin it to get stable decimal dots. */
    private fun <T> withUsLocale(block: () -> T): T {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.US)
        try {
            return block()
        } finally {
            Locale.setDefault(previous)
        }
    }

    /** All three series empty: only the header must be emitted, with no data rows. */
    @Test
    fun allListsEmptyReturnsHeaderOnly() {
        val csv = withUsLocale {
            HistoryExport.sessionCsv(
                cpu = emptyList(),
                ram = emptyList(),
                battery = emptyList(),
                intervalMs = 1000,
                startEpochMs = 0L,
            )
        }
        assertEquals(listOf(HEADER), rows(csv))
    }

    /** Series of different lengths are right-padded with empty cells up to the longest series. */
    @Test
    fun differentLengthsArePaddedToLongest() {
        val csv = withUsLocale {
            HistoryExport.sessionCsv(
                cpu = listOf(10f, 20f, 30f),
                ram = listOf(40f),
                battery = listOf(50f, 49f),
                intervalMs = 1000,
                startEpochMs = 0L,
            )
        }
        val lines = rows(csv)
        assertEquals(4, lines.size) // header + one row per index of the longest series
        assertEquals(HEADER, lines[0])
        assertEquals("0,0,10.00,40.00,50.00", lines[1])
        assertEquals("1,1000,20.00,,49.00", lines[2]) // ram missing at index 1
        assertEquals("2,2000,30.00,,", lines[3]) // ram and battery missing at index 2
        lines.forEach { assertEquals("row must keep five columns: $it", 5, it.split(',').size) }
    }

    /** A custom intervalMs is used to advance the epoch_ms column. */
    @Test
    fun customIntervalMsIsApplied() {
        val csv = withUsLocale {
            HistoryExport.sessionCsv(
                cpu = listOf(1f, 2f, 3f),
                ram = emptyList(),
                battery = emptyList(),
                intervalMs = 250,
                startEpochMs = 10_000L,
            )
        }
        val lines = rows(csv)
        assertEquals("0,10000,1.00,,", lines[1])
        assertEquals("1,10250,2.00,,", lines[2])
        assertEquals("2,10500,3.00,,", lines[3])
    }

    /** A fixed startEpochMs makes the exported timestamps deterministic across calls. */
    @Test
    fun fixedStartEpochMsIsDeterministic() {
        fun export() = withUsLocale {
            HistoryExport.sessionCsv(
                cpu = listOf(5f),
                ram = listOf(6f),
                battery = listOf(7f),
                intervalMs = 1000,
                startEpochMs = 1_700_000_000_000L,
            )
        }
        val first = export()
        assertEquals(first, export())
        assertEquals("0,1700000000000,5.00,6.00,7.00", rows(first)[1])
    }
}

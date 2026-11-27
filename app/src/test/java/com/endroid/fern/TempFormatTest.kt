package com.endroid.fern

import com.endroid.fern.monitor.TempFormat
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class TempFormatTest {
    @Test
    fun celsiusIsUnchanged() {
        assertEquals("36.5°C", TempFormat.format(36.5f, false, 1, Locale.US))
    }

    @Test
    fun fahrenheitConvertsCorrectly() {
        assertEquals("98.6°F", TempFormat.format(37f, true, 1, Locale.US))
        assertEquals("32°F", TempFormat.format(0f, true, 0, Locale.US))
        assertEquals("212°F", TempFormat.format(100f, true, 0, Locale.US))
        assertEquals("-40°F", TempFormat.format(-40f, true, 0, Locale.US))
    }

    @Test
    fun usesTheLocaleDecimalMark() {
        assertEquals("36,5°C", TempFormat.format(36.5f, false, 1, Locale.GERMANY))
    }

    @Test
    fun decimalsAreClamped() {
        assertEquals("36.50°C", TempFormat.format(36.5f, false, 9, Locale.US))
        assertEquals("37°C", TempFormat.format(36.6f, false, -3, Locale.US))
    }
}

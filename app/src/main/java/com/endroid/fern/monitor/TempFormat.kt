package com.endroid.fern.monitor

import java.util.Locale

/** Temperature display helpers. Pure functions, so they are easy to unit test. */
object TempFormat {
    fun toFahrenheit(celsius: Float): Float = celsius * 9f / 5f + 32f

    /** "36.5°C" or "97.7°F". Uses the device locale for the decimal mark unless [locale] is given. */
    fun format(celsius: Float, fahrenheit: Boolean, decimals: Int = 1, locale: Locale = Locale.getDefault()): String {
        val v = if (fahrenheit) toFahrenheit(celsius) else celsius
        return String.format(locale, "%." + decimals.coerceIn(0, 2) + "f°%s", v, if (fahrenheit) "F" else "C")
    }
}

package com.endroid.fern.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.endroid.fern.monitor.TempFormat

/** Current temperature unit. Reading [fahrenheit] inside a composable makes it recompose when the setting changes. */
object TempUnit {
    var fahrenheit by mutableStateOf(false)

    fun fmt(celsius: Float, decimals: Int = 1): String = TempFormat.format(celsius, fahrenheit, decimals)
}

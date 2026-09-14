package com.endroid.fern

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.endroid.fern.data.Prefs
import com.endroid.fern.data.ThemeMode
import com.endroid.fern.monitor.SystemMetrics
import com.endroid.fern.monitor.SystemSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FernViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)

    private val _snapshot = MutableStateFlow<SystemSnapshot?>(null)
    val snapshot: StateFlow<SystemSnapshot?> = _snapshot.asStateFlow()

    private val _historyCpu = MutableStateFlow<List<Float>>(emptyList())
    val historyCpu: StateFlow<List<Float>> = _historyCpu.asStateFlow()

    private val _historyRam = MutableStateFlow<List<Float>>(emptyList())
    val historyRam: StateFlow<List<Float>> = _historyRam.asStateFlow()

    private val _historyBattery = MutableStateFlow<List<Float>>(emptyList())
    val historyBattery: StateFlow<List<Float>> = _historyBattery.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.themeMode)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _refreshMs = MutableStateFlow(prefs.refreshMs)
    val refreshMs: StateFlow<Int> = _refreshMs.asStateFlow()

    private val _keepScreenOn = MutableStateFlow(prefs.keepScreenOn)
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    private var loop: Job? = null
    private var running = false

    fun setThemeMode(mode: ThemeMode) {
        prefs.themeMode = mode
        _themeMode.value = mode
    }

    fun setRefreshMs(ms: Int) {
        prefs.refreshMs = ms
        _refreshMs.value = prefs.refreshMs
        if (running) restartLoop()
    }

    fun setKeepScreenOn(on: Boolean) {
        prefs.keepScreenOn = on
        _keepScreenOn.value = on
    }

    fun startSampling() {
        running = true
        if (loop?.isActive == true) return
        restartLoop()
    }

    fun stopSampling() {
        running = false
        loop?.cancel()
        loop = null
    }

    private fun restartLoop() {
        loop?.cancel()
        loop = viewModelScope.launch {
            withContext(Dispatchers.IO) {
                SystemMetrics.capture(getApplication())
            }
            delay(180)
            while (isActive && running) {
                val snap = withContext(Dispatchers.IO) {
                    SystemMetrics.capture(getApplication())
                }
                _snapshot.value = snap
                _historyCpu.value = (_historyCpu.value + snap.cpuPercent).takeLast(40)
                _historyRam.value = (_historyRam.value + snap.ramPercent).takeLast(40)
                if (snap.batteryPercent >= 0) {
                    _historyBattery.value =
                        (_historyBattery.value + snap.batteryPercent.toFloat()).takeLast(40)
                }
                delay(_refreshMs.value.toLong())
            }
        }
    }
}

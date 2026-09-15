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

    private val _historyNet = MutableStateFlow<List<Float>>(emptyList())
    val historyNet: StateFlow<List<Float>> = _historyNet.asStateFlow()

    private val _historyStorage = MutableStateFlow<List<Float>>(emptyList())
    val historyStorage: StateFlow<List<Float>> = _historyStorage.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.themeMode)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _refreshMs = MutableStateFlow(prefs.refreshMs)
    val refreshMs: StateFlow<Int> = _refreshMs.asStateFlow()

    private val _keepScreenOn = MutableStateFlow(prefs.keepScreenOn)
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    private val _haptics = MutableStateFlow(prefs.haptics)
    val haptics: StateFlow<Boolean> = _haptics.asStateFlow()

    private val _pauseInBackground = MutableStateFlow(prefs.pauseInBackground)
    val pauseInBackground: StateFlow<Boolean> = _pauseInBackground.asStateFlow()

    private val _peakCpu = MutableStateFlow(0f)
    val peakCpu: StateFlow<Float> = _peakCpu.asStateFlow()
    private val _peakRam = MutableStateFlow(0f)
    val peakRam: StateFlow<Float> = _peakRam.asStateFlow()
    private val _peakNet = MutableStateFlow(0f)
    val peakNet: StateFlow<Float> = _peakNet.asStateFlow()

    private val _lastUpdatedMs = MutableStateFlow(0L)
    val lastUpdatedMs: StateFlow<Long> = _lastUpdatedMs.asStateFlow()

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

    fun setHaptics(on: Boolean) {
        prefs.haptics = on
        _haptics.value = on
    }

    fun setPauseInBackground(on: Boolean) {
        prefs.pauseInBackground = on
        _pauseInBackground.value = on
    }

    fun clearHistory() {
        _historyCpu.value = emptyList()
        _historyRam.value = emptyList()
        _historyBattery.value = emptyList()
        _historyNet.value = emptyList()
        _historyStorage.value = emptyList()
        _peakCpu.value = 0f
        _peakRam.value = 0f
        _peakNet.value = 0f
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

    fun refreshNow() {
        viewModelScope.launch {
            val snap = withContext(Dispatchers.IO) {
                SystemMetrics.capture(getApplication())
            }
            applySnapshot(snap)
        }
    }

    private fun applySnapshot(snap: SystemSnapshot) {
        _snapshot.value = snap
        _lastUpdatedMs.value = System.currentTimeMillis()
        // Always append — even small changes should appear on the sparkline
        _historyCpu.value = (_historyCpu.value + snap.cpuPercent).takeLast(48)
        _historyRam.value = (_historyRam.value + snap.ramPercent).takeLast(48)
        if (snap.batteryPercent >= 0) {
            _historyBattery.value =
                (_historyBattery.value + snap.batteryPercent.toFloat()).takeLast(48)
        }
        _historyNet.value = (_historyNet.value + snap.networkKBps).takeLast(48)
        _historyStorage.value = (_historyStorage.value + snap.storagePercent).takeLast(48)
        if (snap.cpuPercent > _peakCpu.value) _peakCpu.value = snap.cpuPercent
        if (snap.ramPercent > _peakRam.value) _peakRam.value = snap.ramPercent
        if (snap.networkKBps > _peakNet.value) _peakNet.value = snap.networkKBps
    }

    fun metricsShareText(): String {
        val s = _snapshot.value ?: return "Fern: no sample yet"
        return buildString {
            appendLine("Fern system snapshot")
            appendLine("Device: ${s.deviceModel} · Android ${s.androidVersion} (API ${s.sdkInt})")
            appendLine(
                "CPU: ${"%.1f".format(s.cpuPercent)}% (peak ${"%.1f".format(_peakCpu.value)}%) · ${s.cpuCores} cores"
            )
            appendLine(
                "RAM: ${s.ramUsedMb}/${s.ramTotalMb} MB (${"%.1f".format(s.ramPercent)}%, peak ${"%.1f".format(_peakRam.value)}%)"
            )
            appendLine(
                "Battery: ${s.batteryPercent}% ${if (s.batteryCharging) "charging" else "discharging"} · ${s.batteryHealth}"
            )
            appendLine(
                "Storage: ${"%.1f".format(s.storageUsedGb)}/${"%.1f".format(s.storageTotalGb)} GB"
            )
            appendLine(
                "Network: ${s.networkLabel} · ${"%.1f".format(s.networkKBps)} KB/s (peak ${"%.1f".format(_peakNet.value)})"
            )
            appendLine("Thermal: ${s.thermalLabel} · Uptime ${"%.1f".format(s.uptimeHours)} h")
        }
    }

    private fun restartLoop() {
        loop?.cancel()
        loop = viewModelScope.launch {
            // Seed network baseline (first TrafficStats delta needs a prior point)
            withContext(Dispatchers.IO) {
                SystemMetrics.capture(getApplication())
            }
            delay(250)
            while (isActive && running) {
                val snap = withContext(Dispatchers.IO) {
                    SystemMetrics.capture(getApplication())
                }
                applySnapshot(snap)
                delay(_refreshMs.value.toLong())
            }
        }
    }
}

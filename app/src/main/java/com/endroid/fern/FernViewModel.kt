package com.endroid.fern

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.endroid.fern.data.Prefs
import com.endroid.fern.data.ThemeMode
import com.endroid.fern.monitor.SystemMetrics
import com.endroid.fern.monitor.SystemSnapshot
import com.endroid.fern.monitor.AdvancedAccess
import com.endroid.fern.monitor.AdvancedMetrics
import com.endroid.fern.monitor.AdvancedSnapshot
import com.endroid.fern.monitor.ElevatedProfile
import com.endroid.fern.monitor.ElevatedStatus
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
    private val advancedAccess = AdvancedAccess(app)
    private val advancedMetrics = AdvancedMetrics(advancedAccess)

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

    private val _seedColorArgb = MutableStateFlow(prefs.seedColorArgb)
    val seedColorArgb: StateFlow<Int> = _seedColorArgb.asStateFlow()
    private val _amoledDark = MutableStateFlow(prefs.amoledDark)
    val amoledDark: StateFlow<Boolean> = _amoledDark.asStateFlow()
    private val _compactUi = MutableStateFlow(prefs.compactUi)
    val compactUi: StateFlow<Boolean> = _compactUi.asStateFlow()
    private val _refreshMs = MutableStateFlow(prefs.refreshMs)
    val refreshMs: StateFlow<Int> = _refreshMs.asStateFlow()
    private val _keepScreenOn = MutableStateFlow(prefs.keepScreenOn)
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()
    private val _haptics = MutableStateFlow(prefs.haptics)
    val haptics: StateFlow<Boolean> = _haptics.asStateFlow()
    private val _pauseInBackground = MutableStateFlow(prefs.pauseInBackground)
    val pauseInBackground: StateFlow<Boolean> = _pauseInBackground.asStateFlow()
    private val _advancedMode = MutableStateFlow(prefs.advancedMode)
    private val _elevatedProfile = MutableStateFlow(prefs.elevatedProfile)
    val elevatedProfile: StateFlow<Int> = _elevatedProfile.asStateFlow()
    private val _elevatedIntervalSec = MutableStateFlow(prefs.elevatedIntervalSec)
    val elevatedIntervalSecFlow: StateFlow<Int> = _elevatedIntervalSec.asStateFlow()
    val advancedMode: StateFlow<Boolean> = _advancedMode.asStateFlow()
    private val _elevatedStatus = MutableStateFlow(advancedAccess.status(prefs.advancedMode))
    val elevatedStatus: StateFlow<ElevatedStatus> = _elevatedStatus.asStateFlow()
    private val _advancedSnapshot = MutableStateFlow(AdvancedSnapshot(emptyList(), null, emptyMap()))
    val advancedSnapshot: StateFlow<AdvancedSnapshot> = _advancedSnapshot.asStateFlow()

    private val _peakCpu = MutableStateFlow(0f)
    val peakCpu: StateFlow<Float> = _peakCpu.asStateFlow()
    private val _peakRam = MutableStateFlow(0f)
    val peakRam: StateFlow<Float> = _peakRam.asStateFlow()
    private val _peakNet = MutableStateFlow(0f)
    val peakNet: StateFlow<Float> = _peakNet.asStateFlow()
    private val _lastUpdatedMs = MutableStateFlow(0L)
    val lastUpdatedMs: StateFlow<Long> = _lastUpdatedMs.asStateFlow()

    private val _avgCpu = MutableStateFlow(0f)
    val avgCpu: StateFlow<Float> = _avgCpu.asStateFlow()
    private val _avgRam = MutableStateFlow(0f)
    val avgRam: StateFlow<Float> = _avgRam.asStateFlow()
    private val _alertMessage = MutableStateFlow<String?>(null)
    val alertMessage: StateFlow<String?> = _alertMessage.asStateFlow()
    private var lastAlertAt = 0L

    private var loop: Job? = null
    private var elevatedLoop: Job? = null
    private var running = false

    /** Last accurate CPU from elevated shell (null if unavailable). */
    @Volatile private var elevatedCpu: Float? = null

    fun setThemeMode(mode: ThemeMode) {
        prefs.themeMode = mode
        _themeMode.value = mode
    }

    fun setSeedColor(argb: Int) {
        prefs.seedColorArgb = argb
        _seedColorArgb.value = argb
    }

    fun setAmoledDark(on: Boolean) {
        prefs.amoledDark = on
        _amoledDark.value = on
    }

    fun setCompactUi(on: Boolean) {
        prefs.compactUi = on
        _compactUi.value = on
    }

    fun setRefreshMs(ms: Int) {
        val clamped = ms.coerceIn(500, 10_000)
        prefs.refreshMs = clamped
        _refreshMs.value = clamped
        // Always restart sampling so the new interval applies immediately
        if (running) {
            restartLoop()
        }
        Toast.makeText(
            getApplication(),
            "Refresh every ${"%.1f".format(clamped / 1000f)}s",
            Toast.LENGTH_SHORT
        ).show()
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

    fun setAlertCpu(v: Int) {
        prefs.alertCpuPercent = v
    }

    fun setAlertTemp(v: Int) {
        prefs.alertTempC = v
    }

    fun setShowTrends(on: Boolean) {
        prefs.showTrends = on
    }

    fun setCompactHome(on: Boolean) {
        prefs.compactHome = on
    }

    fun alertCpuPercent(): Int = prefs.alertCpuPercent
    fun alertTempC(): Int = prefs.alertTempC
    fun showTrendsPref(): Boolean = prefs.showTrends
    fun compactHomePref(): Boolean = prefs.compactHome

    fun setElevatedProfile(v: Int) {
        prefs.elevatedProfile = v
        _elevatedProfile.value = prefs.elevatedProfile
        if (_advancedMode.value) restartElevatedLoop()
    }

    fun setElevatedIntervalSec(v: Int) {
        prefs.elevatedIntervalSec = v
        _elevatedIntervalSec.value = prefs.elevatedIntervalSec
        if (_advancedMode.value) restartElevatedLoop()
    }

    fun elevatedIntervalSec(): Int = prefs.elevatedIntervalSec

    fun setAdvancedMode(on: Boolean) {
        prefs.advancedMode = on
        _advancedMode.value = on
        refreshElevatedStatus()
        if (on) {
            advancedAccess.ensureUserService()
            restartElevatedLoop()
        } else {
            elevatedLoop?.cancel()
            elevatedLoop = null
            elevatedCpu = null
            _advancedSnapshot.value = AdvancedSnapshot(emptyList(), null, emptyMap())
        }
    }

    fun refreshElevatedStatus() {
        _elevatedStatus.value = advancedAccess.status(_advancedMode.value)
    }

    fun refreshElevatedMetrics() {
        if (!_advancedMode.value) return
        viewModelScope.launch {
            advancedAccess.ensureUserService()
            refreshElevatedStatus()
            advancedMetrics.invalidateShellCache()
            val adv = withContext(Dispatchers.IO) {
                advancedMetrics.collect(true, ElevatedProfile.Full, forceHeavy = true)
            }
            _advancedSnapshot.value = adv
            adv.accurateCpuPercent?.let { elevatedCpu = it }
            Toast.makeText(
                getApplication(),
                if (adv.shellOk)
                    "Elevated: RAM ${adv.rssKbByPackage.size} · CPU ${adv.cpuTop.size}"
                else
                    "Shell not ready — wait and tap Refresh again",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    fun requestShizukuPermission() {
        val msg = advancedAccess.requestShizukuPermission()
        Toast.makeText(getApplication(), msg, Toast.LENGTH_LONG).show()
        refreshElevatedStatus()
        viewModelScope.launch {
            delay(400)
            refreshElevatedStatus()
            delay(800)
            refreshElevatedStatus()
            if (advancedAccess.hasShizukuPermission()) {
                restartElevatedLoop()
            }
        }
    }

    fun advancedAccess(): AdvancedAccess = advancedAccess

    suspend fun forceStopPackage(pkg: String): Boolean {
        val ok = advancedMetrics.forceStop(pkg, _advancedMode.value)
        withContext(Dispatchers.Main) {
            Toast.makeText(
                getApplication(),
                if (ok) "Stopped $pkg" else "Could not stop $pkg",
                Toast.LENGTH_SHORT
            ).show()
        }
        return ok
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
        _avgCpu.value = 0f
        _avgRam.value = 0f
    }

    fun startSampling() {
        running = true
        if (loop?.isActive != true) restartLoop()
        if (_advancedMode.value && elevatedLoop?.isActive != true) restartElevatedLoop()
    }

    fun stopSampling() {
        running = false
        loop?.cancel()
        loop = null
        elevatedLoop?.cancel()
        elevatedLoop = null
    }

    fun refreshNow() {
        viewModelScope.launch {
            val snap = withContext(Dispatchers.IO) {
                SystemMetrics.capture(getApplication())
            }
            applySnapshot(applyElevatedCpu(snap))
        }
    }

    private fun applyElevatedCpu(snap: SystemSnapshot): SystemSnapshot {
        val cpu = elevatedCpu ?: return snap
        return snap.copy(cpuPercent = cpu, cpuAvailable = true)
    }

    private fun applySnapshot(snap: SystemSnapshot) {
        _snapshot.value = snap
        _lastUpdatedMs.value = System.currentTimeMillis()
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
        val hc = _historyCpu.value
        val hr = _historyRam.value
        if (hc.isNotEmpty()) _avgCpu.value = hc.average().toFloat()
        if (hr.isNotEmpty()) _avgRam.value = hr.average().toFloat()
        maybeAlert(snap)
    }

    private fun maybeAlert(snap: SystemSnapshot) {
        val now = System.currentTimeMillis()
        if (now - lastAlertAt < 45_000L) return
        val cpuLim = prefs.alertCpuPercent
        val tempLim = prefs.alertTempC
        val ramLim = prefs.alertRamPercent
        val msg = when {
            cpuLim > 0 && snap.cpuPercent >= cpuLim ->
                "CPU ${"%.0f".format(snap.cpuPercent)}% ≥ alert $cpuLim%"
            ramLim > 0 && snap.ramPercent >= ramLim ->
                "RAM ${"%.0f".format(snap.ramPercent)}% ≥ alert $ramLim%"
            tempLim > 0 && (snap.batteryTempC ?: -1f) >= tempLim ->
                "Battery ${"%.1f".format(snap.batteryTempC)}°C ≥ alert ${tempLim}°C"
            snap.lowMemory -> "System reports low memory"
            else -> null
        }
        if (msg != null) {
            lastAlertAt = now
            _alertMessage.value = msg
            Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    fun clearAlert() { _alertMessage.value = null }

    fun metricsShareText(): String {
        val s = _snapshot.value ?: return "Fern: no sample yet"
        return buildString {
            appendLine("Fern system snapshot")
            appendLine("Device: ${s.deviceModel} · Android ${s.androidVersion} (API ${s.sdkInt})")
            appendLine(
                "CPU: ${"%.1f".format(s.cpuPercent)}% (peak ${"%.1f".format(_peakCpu.value)}%) · ${s.cpuCores} cores · ${s.cpuHardware}"
            )
            appendLine(
                "RAM: ${s.ramUsedMb}/${s.ramTotalMb} MB (${"%.1f".format(s.ramPercent)}%, peak ${"%.1f".format(_peakRam.value)}%)"
            )
            appendLine(
                "Battery: ${s.batteryPercent}% ${if (s.batteryCharging) "charging" else "discharging"} · ${s.batteryHealth}"
            )
            appendLine("Thermal: ${s.thermalLabel} · Uptime ${"%.1f".format(s.uptimeHours)} h")
            appendLine(
                "Session avg CPU ${"%.1f".format(_avgCpu.value)}% · RAM ${"%.1f".format(_avgRam.value)}%"
            )
            s.batteryTempC?.let { appendLine("Battery temp: ${"%.1f".format(it)}°C") }
            s.batteryPowerMw?.let {
                appendLine("Battery power: ${"%.0f".format(kotlin.math.abs(it))} mW")
            }
            if (s.lowMemory) appendLine("Memory pressure: low")
            appendLine("Net: ${s.networkLabel} · ${"%.1f".format(s.networkKBps)} KB/s (peak ${"%.1f".format(_peakNet.value)})")
            appendLine("Refresh: ${_refreshMs.value} ms · Advanced: ${_advancedMode.value} · ${_elevatedStatus.value.label}")
        }
    }

    /** Fast loop — only lightweight SystemMetrics, respects refreshMs. */
    private fun restartLoop() {
        loop?.cancel()
        loop = viewModelScope.launch {
            withContext(Dispatchers.IO) {
                SystemMetrics.capture(getApplication())
            }
            delay(150)
            while (isActive && running) {
                val snap = withContext(Dispatchers.IO) {
                    SystemMetrics.capture(getApplication())
                }
                applySnapshot(applyElevatedCpu(snap))
                val wait = _refreshMs.value.toLong().coerceIn(500L, 10_000L)
                delay(wait)
            }
        }
    }

    /** Slow staggered elevated loop — never stacks with itself; light phases by default. */
    private fun restartElevatedLoop() {
        elevatedLoop?.cancel()
        if (!_advancedMode.value) return
        elevatedLoop = viewModelScope.launch {
            var statusEvery = 0
            while (isActive && running && _advancedMode.value) {
                statusEvery++
                if (statusEvery % 3 == 1) refreshElevatedStatus()
                if (advancedAccess.isElevatedLive(true)) {
                    val profile = when (_elevatedProfile.value) {
                        0 -> ElevatedProfile.Lite
                        2 -> ElevatedProfile.Full
                        else -> ElevatedProfile.Balanced
                    }
                    val adv = withContext(Dispatchers.IO) {
                        advancedMetrics.collect(true, profile, forceHeavy = false)
                    }
                    _advancedSnapshot.value = adv
                    adv.accurateCpuPercent?.let { elevatedCpu = it }
                }
                val sec = prefs.elevatedIntervalSec.coerceIn(3, 20)
                delay(sec * 1000L)
            }
        }
    }
}

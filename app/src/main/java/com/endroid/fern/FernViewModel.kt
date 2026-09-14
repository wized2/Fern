package com.endroid.fern

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.endroid.fern.monitor.SystemMetrics
import com.endroid.fern.monitor.SystemSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FernViewModel(app: Application) : AndroidViewModel(app) {

    private val _snapshot = MutableStateFlow<SystemSnapshot?>(null)
    val snapshot: StateFlow<SystemSnapshot?> = _snapshot.asStateFlow()

    private val _historyCpu = MutableStateFlow<List<Float>>(emptyList())
    val historyCpu: StateFlow<List<Float>> = _historyCpu.asStateFlow()

    private val _historyRam = MutableStateFlow<List<Float>>(emptyList())
    val historyRam: StateFlow<List<Float>> = _historyRam.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                val snap = withContext(Dispatchers.IO) {
                    SystemMetrics.capture(getApplication())
                }
                _snapshot.value = snap
                _historyCpu.value = (_historyCpu.value + snap.cpuPercent).takeLast(40)
                _historyRam.value = (_historyRam.value + snap.ramPercent).takeLast(40)
                delay(1500)
            }
        }
    }
}

package com.endroid.fern

import android.os.Bundle
import android.content.Intent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.endroid.fern.ui.FernApp
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import com.endroid.fern.widget.BatteryWidgetProvider
import com.endroid.fern.widget.RamWidgetProvider
import com.endroid.fern.ui.theme.FernTheme

class MainActivity : ComponentActivity() {
    private val viewModel: FernViewModel by viewModels()

    override fun onResume() {
        super.onResume()
        refreshWidgets()
    }

    private fun refreshWidgets() {
        val mgr = AppWidgetManager.getInstance(this)
        val batteryIds = mgr.getAppWidgetIds(ComponentName(this, BatteryWidgetProvider::class.java))
        if (batteryIds.isNotEmpty()) {
            sendBroadcast(Intent(this, BatteryWidgetProvider::class.java).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, batteryIds))
        }
        val ramIds = mgr.getAppWidgetIds(ComponentName(this, RamWidgetProvider::class.java))
        if (ramIds.isNotEmpty()) {
            sendBroadcast(Intent(this, RamWidgetProvider::class.java).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ramIds))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val theme by viewModel.themeMode.collectAsState()
            val keepOn by viewModel.keepScreenOn.collectAsState()
            LaunchedEffect(keepOn) {
                if (keepOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
            FernTheme(mode = theme) {
                val snap by viewModel.snapshot.collectAsState()
                val cpu by viewModel.historyCpu.collectAsState()
                val ram by viewModel.historyRam.collectAsState()
                val bat by viewModel.historyBattery.collectAsState()
                val net by viewModel.historyNet.collectAsState()
                val storage by viewModel.historyStorage.collectAsState()
                val refresh by viewModel.refreshMs.collectAsState()
                val lastUpdated by viewModel.lastUpdatedMs.collectAsState()
                val haptics by viewModel.haptics.collectAsState()
                val pauseBg by viewModel.pauseInBackground.collectAsState()
                val peakCpu by viewModel.peakCpu.collectAsState()
                val peakRam by viewModel.peakRam.collectAsState()
                val peakNet by viewModel.peakNet.collectAsState()
                FernApp(
                    snapshot = snap,
                    cpuHistory = cpu,
                    ramHistory = ram,
                    batteryHistory = bat,
                    netHistory = net,
                    storageHistory = storage,
                    themeMode = theme,
                    refreshMs = refresh,
                    keepScreenOn = keepOn,
                    haptics = haptics,
                    pauseInBackground = pauseBg,
                    peakCpu = peakCpu,
                    peakRam = peakRam,
                    peakNet = peakNet,
                    lastUpdatedMs = lastUpdated,
                    onThemeMode = viewModel::setThemeMode,
                    onRefreshMs = viewModel::setRefreshMs,
                    onKeepScreenOn = viewModel::setKeepScreenOn,
                    onHaptics = viewModel::setHaptics,
                    onPauseInBackground = viewModel::setPauseInBackground,
                    onClearHistory = viewModel::clearHistory,
                    onShareMetrics = viewModel::metricsShareText,
                    onRefreshNow = viewModel::refreshNow
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.startSampling()
    }

    override fun onStop() {
        if (viewModel.pauseInBackground.value) {
            viewModel.stopSampling()
        }
        super.onStop()
    }
}

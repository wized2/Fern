package com.endroid.fern

import android.os.Bundle
import android.content.Intent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.endroid.fern.data.ThemeMode
import androidx.compose.runtime.getValue
import com.endroid.fern.ui.FernApp
import rikka.shizuku.Shizuku
import com.endroid.fern.monitor.AdvancedAccess
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.util.Log
import com.endroid.fern.widget.BatteryWidgetProvider
import com.endroid.fern.widget.RamWidgetProvider
import com.endroid.fern.ui.theme.FernTheme

class MainActivity : ComponentActivity() {

    private val viewModel: FernViewModel by viewModels()

    private val shizukuPermissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            Log.i(TAG, "Shizuku permission result code=$requestCode grant=$grantResult")
            viewModel.advancedAccess().onPermissionResult(requestCode, grantResult)
            viewModel.refreshElevatedStatus()
        }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        Log.i(TAG, "Shizuku binder received (ping=${runCatching { Shizuku.pingBinder() }.getOrDefault(false)})")
        viewModel.advancedAccess().onBinderReceived()
        viewModel.refreshElevatedStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        Log.i(TAG, "Shizuku binder dead")
        viewModel.advancedAccess().onBinderDead()
        viewModel.refreshElevatedStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshWidgets()
        viewModel.refreshElevatedStatus()
    }

    private fun refreshWidgets() {
        val mgr = AppWidgetManager.getInstance(this)
        val batteryIds = mgr.getAppWidgetIds(ComponentName(this, BatteryWidgetProvider::class.java))
        if (batteryIds.isNotEmpty()) {
            sendBroadcast(
                Intent(this, BatteryWidgetProvider::class.java)
                    .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, batteryIds)
            )
        }
        val ramIds = mgr.getAppWidgetIds(ComponentName(this, RamWidgetProvider::class.java))
        if (ramIds.isNotEmpty()) {
            sendBroadcast(
                Intent(this, RamWidgetProvider::class.java)
                    .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ramIds)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        try {
            // Sticky: fires immediately if binder is already available
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
        } catch (e: Exception) {
            Log.e(TAG, "Shizuku listener register failed", e)
        }
        setContent {
            val theme by viewModel.themeMode.collectAsStateWithLifecycle()
            val seedArgb by viewModel.seedColorArgb.collectAsStateWithLifecycle()
            val amoled by viewModel.amoledDark.collectAsStateWithLifecycle()
            val keepOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()
            val darkBars = theme == ThemeMode.DARK || (theme == ThemeMode.AUTO && isSystemInDarkTheme())
            SideEffect {
                val c = WindowCompat.getInsetsController(window, window.decorView)
                c.isAppearanceLightStatusBars = !darkBars
                c.isAppearanceLightNavigationBars = !darkBars
            }
            LaunchedEffect(keepOn) {
                if (keepOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
            FernTheme(mode = theme, seedColor = androidx.compose.ui.graphics.Color(seedArgb), amoledDark = amoled) {
                val snap by viewModel.snapshot.collectAsStateWithLifecycle()
                val cpu by viewModel.historyCpu.collectAsStateWithLifecycle()
                val ram by viewModel.historyRam.collectAsStateWithLifecycle()
                val bat by viewModel.historyBattery.collectAsStateWithLifecycle()
                val net by viewModel.historyNet.collectAsStateWithLifecycle()
                val storage by viewModel.historyStorage.collectAsStateWithLifecycle()
                val refresh by viewModel.refreshMs.collectAsStateWithLifecycle()
                val lastUpdated by viewModel.lastUpdatedMs.collectAsStateWithLifecycle()
                val haptics by viewModel.haptics.collectAsStateWithLifecycle()
                val pauseBg by viewModel.pauseInBackground.collectAsStateWithLifecycle()
                val peakCpu by viewModel.peakCpu.collectAsStateWithLifecycle()
                val peakRam by viewModel.peakRam.collectAsStateWithLifecycle()
                val peakNet by viewModel.peakNet.collectAsStateWithLifecycle()
                val advancedMode by viewModel.advancedMode.collectAsStateWithLifecycle()
                val elevatedStatus by viewModel.elevatedStatus.collectAsStateWithLifecycle()
                val advancedSnap by viewModel.advancedSnapshot.collectAsStateWithLifecycle()
                val avgCpu by viewModel.avgCpu.collectAsStateWithLifecycle()
                val avgRam by viewModel.avgRam.collectAsStateWithLifecycle()
                val elevatedProfile by viewModel.elevatedProfile.collectAsStateWithLifecycle()
                val elevatedIntervalSec by viewModel.elevatedIntervalSecFlow.collectAsStateWithLifecycle()
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
                    seedColorArgb = seedArgb,
                    onSeedColor = viewModel::setSeedColor,
                    onAmoledDark = viewModel::setAmoledDark,
                    onCompactHome = viewModel::setCompactUi,
                    onRefreshMs = viewModel::setRefreshMs,
                    onKeepScreenOn = viewModel::setKeepScreenOn,
                    onHaptics = viewModel::setHaptics,
                    onPauseInBackground = viewModel::setPauseInBackground,
                    onClearHistory = viewModel::clearHistory,
                    onShareMetrics = viewModel::metricsShareText,
                    onShareMarkdown = viewModel::metricsMarkdownText,
                    onShareHistoryCsv = viewModel::historyCsvText,
                    onRefreshNow = viewModel::refreshNow,
                    advancedMode = advancedMode,
                    elevatedStatus = elevatedStatus,
                    advancedSnapshot = advancedSnap,
                    onAdvancedMode = viewModel::setAdvancedMode,
                    onRequestShizuku = viewModel::requestShizukuPermission,
                    onRefreshElevated = viewModel::refreshElevatedStatus,
                    onRefreshElevatedMetrics = viewModel::refreshElevatedMetrics,
                    onForceStop = viewModel::forceStopPackage,
                    avgCpu = avgCpu,
                    avgRam = avgRam,
                    elevatedProfile = elevatedProfile,
                    onElevatedProfile = viewModel::setElevatedProfile,
                    elevatedIntervalSec = elevatedIntervalSec,
                    onElevatedInterval = viewModel::setElevatedIntervalSec
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.startSampling()
        viewModel.refreshElevatedStatus()
    }

    override fun onStop() {
        if (viewModel.pauseInBackground.value) {
            viewModel.stopSampling()
        }
        super.onStop()
    }

    override fun onDestroy() {
        try {
            Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
        } catch (_: Exception) {
        }
        super.onDestroy()
    }

    companion object {
        private const val TAG = "FernMain"
    }
}

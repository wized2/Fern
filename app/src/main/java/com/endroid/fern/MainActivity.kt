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
import androidx.compose.runtime.collectAsState
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
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = true
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
                val advancedMode by viewModel.advancedMode.collectAsState()
                val elevatedStatus by viewModel.elevatedStatus.collectAsState()
                val advancedSnap by viewModel.advancedSnapshot.collectAsState()
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
                    onRefreshNow = viewModel::refreshNow,
                    advancedMode = advancedMode,
                    elevatedStatus = elevatedStatus,
                    advancedSnapshot = advancedSnap,
                    onAdvancedMode = viewModel::setAdvancedMode,
                    onRequestShizuku = viewModel::requestShizukuPermission,
                    onRefreshElevated = viewModel::refreshElevatedStatus,
                    onForceStop = viewModel::forceStopPackage
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

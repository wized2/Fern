package com.endroid.fern

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.endroid.fern.ui.FernApp
import com.endroid.fern.ui.theme.FernTheme

class MainActivity : ComponentActivity() {
    private val viewModel: FernViewModel by viewModels()

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
                    lastUpdatedMs = lastUpdated,
                    onThemeMode = viewModel::setThemeMode,
                    onRefreshMs = viewModel::setRefreshMs,
                    onKeepScreenOn = viewModel::setKeepScreenOn,
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
        viewModel.stopSampling()
        super.onStop()
    }
}

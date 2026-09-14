package com.endroid.fern

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
            FernTheme(mode = theme) {
                val snap by viewModel.snapshot.collectAsState()
                val cpu by viewModel.historyCpu.collectAsState()
                val ram by viewModel.historyRam.collectAsState()
                val refresh by viewModel.refreshMs.collectAsState()
                FernApp(
                    snapshot = snap,
                    cpuHistory = cpu,
                    ramHistory = ram,
                    themeMode = theme,
                    refreshMs = refresh,
                    onThemeMode = viewModel::setThemeMode,
                    onRefreshMs = viewModel::setRefreshMs
                )
            }
        }
    }
}

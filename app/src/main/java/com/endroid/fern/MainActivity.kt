package com.endroid.fern

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.endroid.fern.ui.Dashboard
import com.endroid.fern.ui.theme.FernTheme

class MainActivity : ComponentActivity() {
    private val viewModel: FernViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FernTheme {
                val snap by viewModel.snapshot.collectAsState()
                val cpu by viewModel.historyCpu.collectAsState()
                val ram by viewModel.historyRam.collectAsState()
                Dashboard(snapshot = snap, cpuHistory = cpu, ramHistory = ram)
            }
        }
    }
}

package com.endroid.fern.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.clip
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.delay
import com.endroid.fern.BuildConfig
import com.endroid.fern.data.ThemeMode
import com.endroid.fern.monitor.SystemSnapshot
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.sp
import com.endroid.fern.R

private enum class Tab { Home, Details, Tests, Settings }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FernApp(
    snapshot: SystemSnapshot?,
    cpuHistory: List<Float>,
    ramHistory: List<Float>,
    batteryHistory: List<Float>,
    netHistory: List<Float>,
    storageHistory: List<Float>,
    themeMode: ThemeMode,
    refreshMs: Int,
    keepScreenOn: Boolean,
    haptics: Boolean,
    pauseInBackground: Boolean,
    peakCpu: Float,
    peakRam: Float,
    peakNet: Float,
    lastUpdatedMs: Long,
    onThemeMode: (ThemeMode) -> Unit,
    onRefreshMs: (Int) -> Unit,
    onKeepScreenOn: (Boolean) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onPauseInBackground: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
    onShareMetrics: () -> String,
    onRefreshNow: () -> Unit
) {
    var tab by remember { mutableStateOf(Tab.Home) }
    val navHaptic = LocalHapticFeedback.current
    fun selectTab(next: Tab) {
        if (next != tab && haptics) {
            navHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        tab = next
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Fern",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                windowInsets = WindowInsets.statusBars
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = tab == Tab.Home,
                    onClick = { selectTab(Tab.Home) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home", style = MaterialTheme.typography.labelMedium) },
                    alwaysShowLabel = false
                )
                NavigationBarItem(
                    selected = tab == Tab.Details,
                    onClick = { selectTab(Tab.Details) },
                    icon = { Icon(Icons.Default.Info, contentDescription = "Details") },
                    label = { Text("Details", style = MaterialTheme.typography.labelMedium) },
                    alwaysShowLabel = false
                )
                NavigationBarItem(
                    selected = tab == Tab.Tests,
                    onClick = { selectTab(Tab.Tests) },
                    icon = { Icon(Icons.Default.Science, contentDescription = "Tests") },
                    label = { Text("Tests", style = MaterialTheme.typography.labelMedium) },
                    alwaysShowLabel = false
                )
                NavigationBarItem(
                    selected = tab == Tab.Settings,
                    onClick = { selectTab(Tab.Settings) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", style = MaterialTheme.typography.labelMedium) },
                    alwaysShowLabel = false
                )
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = tab,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            modifier = Modifier.padding(padding).fillMaxSize(),
            label = "tab"
        ) { current ->
            when (current) {
                Tab.Home -> HomeContent(snapshot, cpuHistory, ramHistory, batteryHistory, netHistory, storageHistory, lastUpdatedMs, haptics, onRefreshNow)
                Tab.Details -> DetailsContent(snapshot, peakCpu, peakRam, peakNet, onShareMetrics)
                Tab.Tests -> TestsContent()
                Tab.Settings -> SettingsContent(
                    themeMode, refreshMs, keepScreenOn, haptics, pauseInBackground,
                    onThemeMode, onRefreshMs, onKeepScreenOn, onHaptics, onPauseInBackground, onClearHistory
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    s: SystemSnapshot?,
    cpuH: List<Float>,
    ramH: List<Float>,
    batH: List<Float>,
    netH: List<Float>,
    storageH: List<Float>,
    lastUpdatedMs: Long,
    haptics: Boolean,
    onRefreshNow: () -> Unit
) {
    var nowTick by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            nowTick = System.currentTimeMillis()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Live overview",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val age = if (lastUpdatedMs > 0L) {
                    val sec = ((nowTick - lastUpdatedMs) / 1000L).coerceAtLeast(0)
                    when {
                        sec < 3 -> "Live"
                        sec < 60 -> "Updated ${sec}s ago"
                        else -> "Updated ${sec / 60}m ago"
                    }
                } else {
                    "Connecting…"
                }
                val thermal = s?.thermalLabel?.takeIf { it.isNotBlank() && !it.equals("Unknown", true) }
                val thermalHot = thermal != null && (
                    thermal.contains("HOT", true) || thermal.contains("CRITICAL", true) ||
                        thermal.contains("EMERGENCY", true) || thermal.contains("SEVERE", true)
                )
                Text(
                    buildString {
                        append(age)
                        if (thermal != null) append(" · Thermal $thermal")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (thermalHot) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val haptic = LocalHapticFeedback.current
            IconButton(onClick = {
                if (haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onRefreshNow()
            }) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh now",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        if (s == null) {
            Text("Reading sensors…")
            return
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val cpuSub = if (s.cpuAvailable) {
                String.format("%.0f%%", s.cpuPercent)
            } else {
                s.loadAvg1?.let { String.format("~%.0f%% · load %.2f", s.cpuPercent, it) }
                    ?: String.format("~%.0f%% (est.)", s.cpuPercent)
            }
            Gauge("CPU", s.cpuPercent, cpuSub, Modifier.weight(1f))
            Gauge(
                "RAM",
                s.ramPercent,
                "${s.ramUsedMb}/${s.ramTotalMb} MB",
                Modifier.weight(1f)
            )
        }
        Bar(
            Icons.Default.Storage,
            "Storage",
            s.storagePercent,
            String.format("%.1f / %.1f GB", s.storageUsedGb, s.storageTotalGb)
        )
        Bar(
            if (s.batteryCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
            "Battery",
            s.batteryPercent.toFloat().coerceAtLeast(0f),
            buildString {
                append("${s.batteryPercent}%")
                if (s.batteryCharging) append(" · charging")
                s.batteryTempC?.let { append(String.format(" · %.1f°C", it)) }
            }
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Live trends",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spark(
                    if (cpuH.size >= 2) cpuH else listOf(0f, 0f),
                    MaterialTheme.colorScheme.primary,
                    "CPU",
                    fixedMax = 100f
                )
                Spark(
                    if (ramH.size >= 2) ramH else listOf(0f, 0f),
                    MaterialTheme.colorScheme.secondary,
                    "RAM",
                    fixedMax = 100f
                )
                val netLabel = s?.let {
                    if (it.networkKBps >= 1024f) String.format("Net · %.1f MB/s", it.networkKBps / 1024f)
                    else String.format("Net · %.0f KB/s · %s", it.networkKBps, it.networkLabel)
                } ?: "Net"
                Spark(
                    if (netH.size >= 2) netH else listOf(0f, 0f),
                    MaterialTheme.colorScheme.tertiary,
                    netLabel,
                    fixedMax = null
                )
            }
        }
    }
}

@Composable
private fun DetailsContent(s: SystemSnapshot?, peakCpu: Float, peakRam: Float, peakNet: Float, onShareMetrics: () -> String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Details",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        if (s == null) {
            Text("Waiting…")
            return
        }
        Detail("Processor") {
            Line("Cores", "${s.cpuCores}")
            Line("Chip", s.cpuHardware)
            Line("Board", s.cpuBoard)
            Line("ABI", s.cpuAbi)
            s.cpuCurMhz?.let { Line("Clock now", "$it MHz") }
            if (s.cpuCoreMhz.isNotEmpty()) {
                val cores = s.cpuCoreMhz
                val label = cores.mapIndexed { i, mhz -> "CPU$i $mhz" }.joinToString(" · ")
                Line("Per-core MHz", label.take(120) + if (label.length > 120) "…" else "")
            }
            s.cpuMaxMhz?.let { Line("Max clock", "$it MHz") }
            Line("Governor", s.cpuGovernor)
            Line(
                "CPU",
                if (s.cpuAvailable) String.format("%.1f%%", s.cpuPercent)
                else String.format("~%.1f%% (est.)", s.cpuPercent)
            )
            s.loadAvg1?.let {
                Line(
                    "Load 1/5/15",
                    String.format("%.2f / %.2f / %.2f", it, s.loadAvg5 ?: 0f, s.loadAvg15 ?: 0f)
                )
            }
            Line("Thermal", s.thermalLabel)
        }
        Detail("Memory") {
            Line("Used", "${s.ramUsedMb} MB")
            Line("Free", "${s.freeRamMb} MB")
            Line("Total", "${s.ramTotalMb} MB")
            Line("App heap", "${s.appHeapUsedMb} / ${s.appHeapMaxMb} MB")
            val extFree = s.externalStorageFreeGb
            val extTotal = s.externalStorageTotalGb
            if (extFree != null && extTotal != null) {
                Line(
                    "External",
                    String.format("%.1f / %.1f GB free", extFree, extTotal)
                )
            }
        }
        Detail("Power") {
            Line("Level", "${s.batteryPercent}%")
            Line("Status", if (s.batteryCharging) "Charging" else "Discharging")
            Line("Health", s.batteryHealth)
            Line("Technology", s.batteryTechnology)
            s.batteryTempC?.let { Line("Temp", String.format("%.1f °C", it)) }
            s.batteryVoltageMv?.let { Line("Voltage", "$it mV") }
            s.batteryCurrentUa?.let {
                val ma = it / 1000f
                Line("Current", String.format("%+.0f mA", ma))
            }
        }
        Detail("Display") {
            Line("Resolution", "${s.displayWidthPx} × ${s.displayHeightPx}")
            Line("Density", "${s.displayDensityDpi} dpi")
            Line("Refresh", String.format("%.0f Hz", s.displayRefreshHz))
        }
        Detail("Device") {
            Line("Model", s.deviceModel)
            Line("Android", "${s.androidVersion} (API ${s.sdkInt})")
            Line("Security patch", s.securityPatch)
            Line("Kernel", s.kernelVersion)
            Line("Network", s.networkLabel)
            Line("Throughput", if (s.networkKBps >= 1024f) String.format("%.2f MB/s", s.networkKBps / 1024f) else String.format("%.1f KB/s", s.networkKBps))
            Line("Locale", s.localeTag)
            Line("Time zone", s.timeZoneId)
            Line("Sensors", "${s.sensorCount}")
            Line(
                "Uptime",
                run {
                    val h = s.uptimeHours
                    val hours = h.toInt()
                    val mins = ((h - hours) * 60).toInt().coerceIn(0, 59)
                    if (hours >= 24) String.format("%dd %dh", hours / 24, hours % 24)
                    else String.format("%dh %02dm", hours, mins)
                }
            )
            Line("Peak CPU", String.format("%.1f%%", peakCpu))
            Line("Peak RAM", String.format("%.1f%%", peakRam))
            Line("Peak net", if (peakNet >= 1024f) String.format("%.2f MB/s", peakNet / 1024f) else String.format("%.1f KB/s", peakNet))
        }
        val context = LocalContext.current
        androidx.compose.material3.Button(
            onClick = {
                val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, onShareMetrics())
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Fern metrics")
                }
                context.startActivity(android.content.Intent.createChooser(send, "Share metrics"))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Share snapshot")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    themeMode: ThemeMode,
    refreshMs: Int,
    keepScreenOn: Boolean,
    haptics: Boolean,
    pauseInBackground: Boolean,
    onThemeMode: (ThemeMode) -> Unit,
    onRefreshMs: (Int) -> Unit,
    onKeepScreenOn: (Boolean) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onPauseInBackground: (Boolean) -> Unit,
    onClearHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Theme",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                var expanded by remember { mutableStateOf(false) }
                val label = when (themeMode) {
                    ThemeMode.AUTO -> "Auto"
                    ThemeMode.LIGHT -> "Light"
                    ThemeMode.DARK -> "Dark"
                }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Appearance") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (mode) {
                                            ThemeMode.AUTO -> "Auto"
                                            ThemeMode.LIGHT -> "Light"
                                            ThemeMode.DARK -> "Dark"
                                        }
                                    )
                                },
                                onClick = {
                                    onThemeMode(mode)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Refresh every ${"%.1f".format(refreshMs / 1000f)} s",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Slider(
                    value = refreshMs.toFloat(),
                    onValueChange = { onRefreshMs(it.toInt()) },
                    valueRange = 500f..5000f,
                    steps = 8
                )
            }
        }
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Keep screen on",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "While Fern is open",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = keepScreenOn, onCheckedChange = onKeepScreenOn)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(500 to "0.5s", 1000 to "1s", 1500 to "1.5s", 2000 to "2s", 5000 to "5s").forEach { (ms, label) ->
                val selected = refreshMs == ms
                androidx.compose.material3.FilterChip(
                    selected = selected,
                    onClick = { onRefreshMs(ms) },
                    label = { Text(label) }
                )
            }
        }
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Haptic feedback", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text("Vibrate on manual refresh", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = haptics, onCheckedChange = onHaptics)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Pause in background", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text("Stop sampling when app is not visible", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = pauseInBackground, onCheckedChange = onPauseInBackground)
                }
                androidx.compose.material3.OutlinedButton(onClick = onClearHistory, modifier = Modifier.fillMaxWidth()) {
                    Text("Clear sparkline history")
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = "Fern logo",
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHighest,
                                shape = MaterialTheme.shapes.medium
                            )
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Column {
                        Text(
                            "Fern",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    "Material 3 system monitor. Live CPU, RAM, storage, battery & thermal. Fully offline, no ads, no accounts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val ctx = LocalContext.current
                Text(
                    "github.com/wized2/Fern",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        runCatching {
                            ctx.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/wized2/Fern")
                                )
                            )
                        }
                    }
                )
                Text(
                    "MIT · endroid",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}



@Composable
private fun TestsContent() {
    val context = LocalContext.current
    var colorTest by remember { mutableStateOf<Color?>(null) }
    var touchHits by remember { mutableStateOf(0) }
    var accel by remember { mutableStateOf("—") }
    var gyro by remember { mutableStateOf("—") }
    var light by remember { mutableStateOf("—") }
    var mag by remember { mutableStateOf("—") }
    var sensorNames by remember { mutableStateOf<List<String>>(emptyList()) }

    DisposableEffect(Unit) {
        val sm = context.getSystemService(android.content.Context.SENSOR_SERVICE) as? SensorManager
        sensorNames = sm?.getSensorList(Sensor.TYPE_ALL)?.map { it.name }?.sorted()?.take(40) ?: emptyList()
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                val v = event.values
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        if (v.size >= 3) {
                            accel = String.format("x %.2f  y %.2f  z %.2f", v[0], v[1], v[2])
                        }
                    }
                    Sensor.TYPE_GYROSCOPE -> {
                        if (v.size >= 3) {
                            gyro = String.format("x %.2f  y %.2f  z %.2f", v[0], v[1], v[2])
                        }
                    }
                    Sensor.TYPE_LIGHT -> {
                        if (v.isNotEmpty()) {
                            light = String.format("%.1f lx", v[0])
                        }
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        if (v.size >= 3) {
                            mag = String.format("x %.1f  y %.1f  z %.1f µT", v[0], v[1], v[2])
                        }
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
        sm?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.let {
            sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
        sm?.getDefaultSensor(Sensor.TYPE_LIGHT)?.let {
            sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
        sm?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let {
            sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose {
            runCatching { sm?.unregisterListener(listener) }
        }
    }

    if (colorTest != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorTest!!)
                .clickable { colorTest = null },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Tap to exit",
                color = if (colorTest == Color.Black || colorTest == Color.Blue) Color.White else Color.Black,
                style = MaterialTheme.typography.titleMedium
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Hardware tests", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Text(
            "Quick checks for display, vibration, sensors and touch. Fully offline.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Detail("Display") {
            Text("Full-screen color — tap the screen to leave", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(
                    "White" to Color.White,
                    "Black" to Color.Black,
                    "Red" to Color.Red,
                    "Green" to Color(0xFF00C853),
                    "Blue" to Color.Blue
                ).forEach { (label, c) ->
                    OutlinedButton(onClick = { colorTest = c }, modifier = Modifier.weight(1f)) {
                        Text(label, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }

        Detail("Vibration") {
            Button(
                onClick = {
                    runCatching {
                        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val vm = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                            vm.defaultVibrator
                        } else {
                            @Suppress("DEPRECATION")
                            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(80)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Vibrate once")
            }
        }

        Detail("Motion sensors") {
            Line("Accelerometer", accel)
            Line("Gyroscope", gyro)
            Line("Light", light)
            Line("Magnetometer", mag)
        }

        Detail("Touch") {
            Text("Taps: $touchHits", style = MaterialTheme.typography.titleMedium)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
                    .pointerInput(Unit) {
                        detectTapGestures { touchHits += 1 }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("Tap here", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = { touchHits = 0 }, modifier = Modifier.fillMaxWidth()) {
                Text("Reset taps")
            }
        }

        Detail("Sensors on device (${sensorNames.size})") {
            if (sensorNames.isEmpty()) {
                Text("No sensors reported", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                sensorNames.forEach { name ->
                    Text(name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}


@Composable
private fun Detail(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun Line(k: String, v: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(k, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(v, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun LeafMark(modifier: Modifier = Modifier) {
    val leaf = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.08f)
            cubicTo(w * 0.15f, h * 0.35f, w * 0.1f, h * 0.6f, w * 0.5f, h * 0.92f)
            cubicTo(w * 0.9f, h * 0.6f, w * 0.85f, h * 0.35f, w * 0.5f, h * 0.08f)
            close()
        }
        drawPath(path, color = leaf)
        drawLine(
            Color.White.copy(alpha = 0.4f),
            Offset(w * 0.5f, h * 0.2f),
            Offset(w * 0.5f, h * 0.85f),
            strokeWidth = w * 0.055f,
            cap = StrokeCap.Round
        )
        drawLine(
            Color.White.copy(alpha = 0.25f),
            Offset(w * 0.5f, h * 0.38f),
            Offset(w * 0.32f, h * 0.5f),
            strokeWidth = w * 0.035f,
            cap = StrokeCap.Round
        )
        drawLine(
            Color.White.copy(alpha = 0.25f),
            Offset(w * 0.5f, h * 0.38f),
            Offset(w * 0.68f, h * 0.5f),
            strokeWidth = w * 0.035f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun Gauge(
    title: String,
    percent: Float,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val a by animateFloatAsState(
        percent.coerceIn(0f, 100f) / 100f,
        tween(700),
        label = "g"
    )
    val track = MaterialTheme.colorScheme.surfaceVariant
    val arc = MaterialTheme.colorScheme.primary
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = size.minDimension * 0.09f
                    val d = size.minDimension - stroke
                    val tl = Offset((size.width - d) / 2f, (size.height - d) / 2f)
                    drawArc(
                        track, 135f, 270f, false, tl, Size(d, d),
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        arc, 135f, 270f * a, false, tl, Size(d, d),
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
                Text(
                    String.format("%.0f%%", percent.coerceIn(0f, 100f)),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Bar(icon: ImageVector, title: String, percent: Float, detail: String) {
    val a by animateFloatAsState(
        (percent / 100f).coerceIn(0f, 1f),
        tween(700),
        label = "b"
    )
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$title $detail" }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text(title, style = MaterialTheme.typography.titleSmall)
            }
            LinearProgressIndicator(
                progress = { a },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Spark(
    values: List<Float>,
    color: Color,
    label: String,
    fixedMax: Float? = 100f
) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
                .padding(6.dp)
        ) {
            if (values.size < 2) return@Canvas
            val vmin = values.minOrNull() ?: 0f
            val vmaxRaw = values.maxOrNull() ?: 1f
            // Fixed 0..100 for % metrics so small changes are visible against full range.
            // For network, auto-scale with a floor so idle is not a flat top line.
            val lo: Float
            val hi: Float
            if (fixedMax != null) {
                lo = 0f
                hi = fixedMax.coerceAtLeast(1f)
            } else {
                lo = 0f
                hi = maxOf(vmaxRaw * 1.15f, 1f)
            }
            val range = (hi - lo).coerceAtLeast(0.001f)
            val step = size.width / (values.size - 1).coerceAtLeast(1)
            val pts = values.mapIndexed { i, v ->
                val y = size.height - ((v - lo) / range).coerceIn(0f, 1f) * size.height * 0.92f
                Offset(i * step, y)
            }
            // soft fill under the line
            val fill = Path().apply {
                moveTo(pts.first().x, size.height)
                pts.forEach { lineTo(it.x, it.y) }
                lineTo(pts.last().x, size.height)
                close()
            }
            drawPath(fill, color = color.copy(alpha = 0.14f))
            for (i in 0 until pts.lastIndex) {
                drawLine(color, pts[i], pts[i + 1], strokeWidth = 2.75f, cap = StrokeCap.Round)
            }
            // endpoint dots so movement is obvious
            drawCircle(color, radius = 3.5f, center = pts.last())
            drawCircle(color.copy(alpha = 0.5f), radius = 2.5f, center = pts.first())
        }
    }
}

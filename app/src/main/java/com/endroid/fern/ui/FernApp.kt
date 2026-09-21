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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.core.graphics.drawable.toBitmap
import android.app.AppOpsManager
import android.content.pm.PackageManager
import android.provider.Settings
import android.app.usage.UsageStatsManager
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.endroid.fern.monitor.ActiveApp
import com.endroid.fern.monitor.ActiveAppsRepository
import com.endroid.fern.monitor.ActiveWindow
import com.endroid.fern.monitor.AppState
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
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
import androidx.core.view.WindowCompat
import android.app.Activity
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.graphics.luminance
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

private enum class Tab { Home, Details, ActiveApps, More }

/** Nested destinations opened from the More tab. */
private enum class MoreSub { None, Tests, Settings, Sensors, About }

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
    var moreSub by remember { mutableStateOf(MoreSub.None) }
    val navHaptic = LocalHapticFeedback.current
    fun selectTab(next: Tab) {
        if (next != tab && haptics) {
            navHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        tab = next
        if (next != Tab.More) moreSub = MoreSub.None
    }
    fun openMore(sub: MoreSub) {
        if (haptics) navHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        moreSub = sub
    }
    fun backFromMore() {
        if (haptics) navHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        moreSub = MoreSub.None
    }
    val view = LocalView.current
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !dark
        controller.isAppearanceLightNavigationBars = !dark
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (android.os.Build.VERSION.SDK_INT >= 29) window.isStatusBarContrastEnforced = false
    }

    val onMoreChild = tab == Tab.More && moreSub != MoreSub.None
    val topTitle = when {
        onMoreChild && moreSub == MoreSub.Tests -> "Tests"
        onMoreChild && moreSub == MoreSub.Settings -> "Settings"
        onMoreChild && moreSub == MoreSub.Sensors -> "Sensors"
        onMoreChild && moreSub == MoreSub.About -> "About"
        else -> null
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    if (topTitle != null) {
                        Text(
                            topTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Fern",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "System monitor",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (onMoreChild) {
                        IconButton(onClick = { backFromMore() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                    alwaysShowLabel = true
                )
                NavigationBarItem(
                    selected = tab == Tab.Details,
                    onClick = { selectTab(Tab.Details) },
                    icon = { Icon(Icons.Default.Memory, contentDescription = "Details") },
                    label = { Text("Details", style = MaterialTheme.typography.labelMedium) },
                    alwaysShowLabel = true
                )
                NavigationBarItem(
                    selected = tab == Tab.ActiveApps,
                    onClick = { selectTab(Tab.ActiveApps) },
                    icon = { Icon(Icons.Default.Apps, contentDescription = "Active Apps") },
                    label = { Text("Apps", style = MaterialTheme.typography.labelMedium) },
                    alwaysShowLabel = true
                )
                NavigationBarItem(
                    selected = tab == Tab.More,
                    onClick = { selectTab(Tab.More) },
                    icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "More") },
                    label = { Text("More", style = MaterialTheme.typography.labelMedium) },
                    alwaysShowLabel = true
                )
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = when {
                tab == Tab.More && moreSub != MoreSub.None -> "more/${moreSub.name}"
                else -> tab.name
            },
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            modifier = Modifier.padding(padding).fillMaxSize(),
            label = "tab"
        ) { key ->
            when {
                key == Tab.Home.name -> HomeContent(
                    snapshot, cpuHistory, ramHistory, batteryHistory, netHistory, storageHistory,
                    lastUpdatedMs, haptics, onRefreshNow
                )
                key == Tab.Details.name -> DetailsContent(snapshot, peakCpu, peakRam, peakNet, onShareMetrics)
                key == Tab.ActiveApps.name -> ActiveAppsContent(haptics = haptics)
                key == Tab.More.name || key == "more/None" -> MoreContent(onOpen = { openMore(it) })
                key == "more/Tests" -> TestsContent()
                key == "more/Settings" -> SettingsContent(
                    themeMode, refreshMs, keepScreenOn, haptics, pauseInBackground,
                    onThemeMode, onRefreshMs, onKeepScreenOn, onHaptics, onPauseInBackground, onClearHistory
                )
                key == "more/Sensors" -> SensorsContent()
                key == "more/About" -> AboutContent()
                else -> MoreContent(onOpen = { openMore(it) })
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
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Overview",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
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
                val thermal = s?.thermalLabel?.takeIf { it.isNotBlank() && !it.equals("Unknown", true) && !it.equals("None", true) && it != "—" }
                val thermalHot = thermal != null && (
                    thermal.contains("HOT", true) || thermal.contains("CRITICAL", true) ||
                        thermal.contains("EMERGENCY", true) || thermal.contains("SEVERE", true)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (age == "Live") {
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                    RoundedCornerShape(50)
                                )
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "Live",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (thermal != null) {
                            Text(
                                "Thermal $thermal",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (thermalHot) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
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
                }
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
                "${formatMb(s.ramUsedMb)} / ${formatMb(s.ramTotalMb)}",
                Modifier.weight(1f)
            )
        }
        ThermalBar(
            tempC = s.batteryTempC,
            thermalLabel = s.thermalLabel
        )
        Bar(
            Icons.Default.Folder,
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
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Trends",
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
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "Hardware & system",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Readable snapshot of this device",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (s == null) {
            Text("Waiting…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return
        }

        // Quick peaks strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PeakChip("Peak CPU", String.format("%.0f%%", peakCpu), Modifier.weight(1f))
            PeakChip("Peak RAM", String.format("%.0f%%", peakRam), Modifier.weight(1f))
            val peakNetLabel = if (peakNet >= 1024f) String.format("%.1f MB/s", peakNet / 1024f)
            else String.format("%.0f KB/s", peakNet)
            PeakChip("Peak net", peakNetLabel, Modifier.weight(1f))
        }

        DetailSection(icon = Icons.Default.Memory, title = "Processor") {
            SpecRow("Cores", "${s.cpuCores}")
            SpecRow("Chip", s.cpuHardware)
            SpecRow("Board", s.cpuBoard)
            SpecRow("ABI", s.cpuAbi)
            s.cpuCurMhz?.let { SpecRow("Clock now", formatMhz(it)) }
            s.cpuMaxMhz?.let { SpecRow("Max clock", formatMhz(it)) }
            SpecRow("Governor", s.cpuGovernor.ifBlank { "—" })
            SpecRow(
                "CPU",
                if (s.cpuAvailable) String.format("%.1f%%", s.cpuPercent)
                else String.format("~%.1f%% (est.)", s.cpuPercent)
            )
            s.loadAvg1?.let {
                SpecRow(
                    "Load 1 / 5 / 15",
                    String.format("%.2f  ·  %.2f  ·  %.2f", it, s.loadAvg5 ?: 0f, s.loadAvg15 ?: 0f)
                )
            }
            SpecRow("Thermal", s.thermalLabel.ifBlank { "—" })
            if (s.cpuCoreMhz.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Per-core clock",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    s.cpuCoreMhz.mapIndexed { i, mhz -> "CPU$i ${formatMhz(mhz)}" }.joinToString("  ·  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        DetailSection(icon = Icons.Default.Folder, title = "Memory & storage") {
            SpecRow("RAM used", formatMb(s.ramUsedMb))
            SpecRow("RAM free", formatMb(s.freeRamMb))
            SpecRow("RAM total", formatMb(s.ramTotalMb))
            SpecRow("App heap", "${formatMb(s.appHeapUsedMb)} / ${formatMb(s.appHeapMaxMb)}")
            SpecRow("Storage", String.format("%.1f / %.1f GB", s.storageUsedGb, s.storageTotalGb))
            val extFree = s.externalStorageFreeGb
            val extTotal = s.externalStorageTotalGb
            if (extFree != null && extTotal != null) {
                SpecRow("External", String.format("%.1f / %.1f GB free", extFree, extTotal))
            }
        }

        DetailSection(icon = Icons.Default.BatteryFull, title = "Power") {
            SpecRow("Level", "${s.batteryPercent}%")
            SpecRow("Status", if (s.batteryCharging) "Charging" else "Discharging")
            SpecRow("Health", s.batteryHealth.ifBlank { "—" })
            SpecRow("Technology", s.batteryTechnology.ifBlank { "—" })
            s.batteryTempC?.let { SpecRow("Temperature", String.format("%.1f °C", it)) }
            s.batteryVoltageMv?.let { SpecRow("Voltage", String.format("%.2f V", it / 1000f)) }
            s.batteryCurrentUa?.let {
                SpecRow("Current", String.format("%+.0f mA", it / 1000f))
            }
        }

        DetailSection(icon = Icons.Default.PhoneAndroid, title = "Display & device") {
            SpecRow("Resolution", "${s.displayWidthPx} × ${s.displayHeightPx}")
            SpecRow("Density", "${s.displayDensityDpi} dpi · ${String.format("%.1f", s.displayDensityDpi / 160f)}×")
            SpecRow("Refresh", String.format("%.0f Hz", s.displayRefreshHz))
            SpecRow("Model", s.deviceModel)
            SpecRow("Android", "${s.androidVersion} (API ${s.sdkInt})")
            SpecRow("Security patch", s.securityPatch.ifBlank { "—" })
            SpecRow("Kernel", s.kernelVersion)
            SpecRow("Network", s.networkLabel)
            SpecRow("Throughput", formatRate(s.networkKBps))
            SpecRow("Locale", s.localeTag)
            SpecRow("Time zone", s.timeZoneId)
            SpecRow("Sensors", "${s.sensorCount}")
            SpecRow(
                "Uptime",
                run {
                    val h = s.uptimeHours
                    val hours = h.toInt()
                    val mins = ((h - hours) * 60).toInt().coerceIn(0, 59)
                    if (hours >= 24) String.format("%dd %dh", hours / 24, hours % 24)
                    else String.format("%dh %02dm", hours, mins)
                }
            )
        }

        val context = LocalContext.current
        OutlinedButton(
            onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, onShareMetrics())
                    putExtra(Intent.EXTRA_SUBJECT, "Fern metrics")
                }
                context.startActivity(Intent.createChooser(send, "Share metrics"))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text("Share snapshot")
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun PeakChip(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DetailSection(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            content()
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            value.ifBlank { "—" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.58f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
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
    }
}





@Composable
private fun AboutContent() {
    val ctx = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = "Fern logo",
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            shape = MaterialTheme.shapes.extraLarge
                        )
                        .padding(10.dp)
                )
                Text(
                    "Fern",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Live system pulse — CPU, RAM, storage, battery, thermal, active apps, and hardware tests. Fully offline, no ads, no accounts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Source", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    "github.com/wized2/Fern",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        runCatching {
                            ctx.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/wized2/Fern"))
                            )
                        }
                    }
                )
                Text("License", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    "MIT · endroid",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Permissions: network state for throughput, vibrate for tests/haptics, and optional Usage access for Active Apps. Nothing leaves the device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


private data class MoreItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val sub: MoreSub
)

@Composable
private fun MoreContent(onOpen: (MoreSub) -> Unit) {
    val items = listOf(
        MoreItem("Tests", "Run device tests", Icons.Default.Science, MoreSub.Tests),
        MoreItem("Sensors", "Sensors available on this device", Icons.Default.Sensors, MoreSub.Sensors),
        MoreItem("Settings", "Theme, refresh rate, screen", Icons.Default.Settings, MoreSub.Settings),
        MoreItem("About", "Version, license, source", Icons.Default.Info, MoreSub.About)
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "More",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            "Tools and preferences",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(item.sub) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                    MaterialTheme.shapes.medium
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                item.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.size(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                item.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (index < items.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 70.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Fern v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text(
            "Offline · no ads · no accounts",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun SensorsContent() {
    val context = LocalContext.current
    val sensors = remember {
        val sm = context.getSystemService(android.content.Context.SENSOR_SERVICE) as? SensorManager
        sm?.getSensorList(Sensor.TYPE_ALL)?.sortedBy { it.name.lowercase() } ?: emptyList()
    }
    var expanded by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Text(
            if (sensors.isEmpty()) "No sensors reported"
            else "${sensors.size} sensors",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        if (sensors.isEmpty()) {
            Text(
                "This device did not report any sensors.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(sensors, key = { "${it.name}|${it.type}|${it.vendor}" }) { sensor ->
                val key = "${sensor.name}|${sensor.type}|${sensor.vendor}"
                val open = expanded == key
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = MaterialTheme.shapes.large,
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = if (open) null else key }
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Text(
                            sensor.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            sensorTypeLabel(sensor.type) + " · " + (sensor.vendor.ifBlank { "unknown vendor" }),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (open) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Line("Type", sensorTypeLabel(sensor.type))
                            Line("Vendor", sensor.vendor.ifBlank { "—" })
                            Line("Max range", String.format("%.4g", sensor.maximumRange))
                            Line("Resolution", String.format("%.4g", sensor.resolution))
                            Line("Power", String.format("%.2f mA", sensor.power))
                            if (sensor.stringType != null) {
                                Line("String type", sensor.stringType)
                            }
                            SensorLiveBlock(sensor)
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun SensorLiveBlock(sensor: Sensor) {
    val context = LocalContext.current
    var values by remember(sensor.name, sensor.type) { mutableStateOf("Listening…") }
    DisposableEffect(sensor.name, sensor.type) {
        val sm = context.getSystemService(android.content.Context.SENSOR_SERVICE) as? SensorManager
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                values = event.values.take(4).joinToString("  ") { String.format("%.3f", it) }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sm?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { runCatching { sm?.unregisterListener(listener) } }
    }
    Text(
        "Live: $values",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp)
    )
}

private fun sensorTypeLabel(type: Int): String = when (type) {
    Sensor.TYPE_ACCELEROMETER -> "Accelerometer"
    Sensor.TYPE_MAGNETIC_FIELD -> "Magnetic field"
    Sensor.TYPE_ORIENTATION -> "Orientation"
    Sensor.TYPE_GYROSCOPE -> "Gyroscope"
    Sensor.TYPE_LIGHT -> "Light"
    Sensor.TYPE_PRESSURE -> "Pressure"
    Sensor.TYPE_TEMPERATURE -> "Temperature"
    Sensor.TYPE_PROXIMITY -> "Proximity"
    Sensor.TYPE_GRAVITY -> "Gravity"
    Sensor.TYPE_LINEAR_ACCELERATION -> "Linear acceleration"
    Sensor.TYPE_ROTATION_VECTOR -> "Rotation vector"
    Sensor.TYPE_RELATIVE_HUMIDITY -> "Humidity"
    Sensor.TYPE_AMBIENT_TEMPERATURE -> "Ambient temperature"
    Sensor.TYPE_GAME_ROTATION_VECTOR -> "Game rotation"
    Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> "Gyro uncalibrated"
    Sensor.TYPE_SIGNIFICANT_MOTION -> "Significant motion"
    Sensor.TYPE_STEP_DETECTOR -> "Step detector"
    Sensor.TYPE_STEP_COUNTER -> "Step counter"
    Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> "Geomagnetic rotation"
    Sensor.TYPE_HEART_RATE -> "Heart rate"
    Sensor.TYPE_POSE_6DOF -> "Pose 6DoF"
    Sensor.TYPE_STATIONARY_DETECT -> "Stationary detect"
    Sensor.TYPE_MOTION_DETECT -> "Motion detect"
    Sensor.TYPE_HEART_BEAT -> "Heart beat"
    Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT -> "Off-body detect"
    Sensor.TYPE_ACCELEROMETER_UNCALIBRATED -> "Accel uncalibrated"
    else -> "Type $type"
}

@Composable
private fun ActiveAppsContent(haptics: Boolean) {
    val context = LocalContext.current
    val repo = remember { ActiveAppsRepository(context) }
    var hasAccess by remember { mutableStateOf(repo.hasUsageAccess()) }
    var window by remember { mutableStateOf(ActiveWindow.HOUR_1) }
    var showSystem by remember { mutableStateOf(false) }
    var apps by remember { mutableStateOf<List<ActiveApp>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    fun refresh() {
        hasAccess = repo.hasUsageAccess()
        if (!hasAccess) {
            apps = emptyList()
            return
        }
        scope.launch {
            loading = true
            apps = runCatching { repo.load(window, showSystem) }.getOrElse { emptyList() }
            loading = false
        }
    }

    // Re-check permission when returning from system settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    LaunchedEffect(window, showSystem) { refresh() }

    // Auto-refresh while visible
    LaunchedEffect(hasAccess, window, showSystem) {
        if (!hasAccess) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(5_000)
            apps = runCatching { repo.load(window, showSystem) }.getOrElse { apps }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            "Active Apps",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            "Recently in the foreground",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (!hasAccess) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Usage access needed",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Fern can show which apps were recently active. Everything stays on this device — nothing is uploaded. Grant Usage access to enable this list.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Usage access")
                    }
                }
            }
            return
        }

        // Time window chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActiveWindow.entries.forEach { w ->
                FilterChip(
                    selected = window == w,
                    onClick = {
                        if (haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        window = w
                    },
                    label = { Text(w.label) }
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "System apps",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(checked = showSystem, onCheckedChange = {
                if (haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                showSystem = it
            })
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (apps.isEmpty() && !loading) {
            Text(
                "No recent activity in this window",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(apps, key = { it.packageName }) { app ->
                    ActiveAppRow(app) {
                        runCatching {
                            val uri = Uri.parse("package:${app.packageName}")
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
                            )
                        }
                    }
                }
                item {
                    Text(
                        "Android doesn't allow apps to see other apps' CPU or RAM. This shows which apps were recently active.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveAppRow(app: ActiveApp, onClick: () -> Unit) {
    val context = LocalContext.current
    val iconPainter = remember(app.packageName) {
        try {
            val d = context.packageManager.getApplicationIcon(app.packageName)
            val bmp = d.toBitmap(96, 96)
            BitmapPainter(bmp.asImageBitmap())
        } catch (_: Exception) {
            null
        }
    }
    val now = System.currentTimeMillis()
    val ago = relativeTime(now - app.lastUsedMs)
    val fg = formatDuration(app.foregroundTimeMs)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconPainter != null) {
                Image(
                    painter = iconPainter,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(MaterialTheme.shapes.medium)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHighest,
                            MaterialTheme.shapes.medium
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Apps, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        app.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (app.state == AppState.FOREGROUND) {
                        Spacer(modifier = Modifier.size(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                "Active now",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    "Used $ago · Foreground $fg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun relativeTime(deltaMs: Long): String {
    val s = (deltaMs / 1000).coerceAtLeast(0)
    return when {
        s < 60 -> "${s}s"
        s < 3600 -> "${s / 60} min"
        s < 86400 -> String.format("%.1f h", s / 3600.0)
        else -> "${s / 86400} d"
    }
}

private fun formatDuration(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return when {
        s < 60 -> "${s}s"
        s < 3600 -> "${s / 60} min"
        else -> String.format("%.1f h", s / 3600.0)
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

    DisposableEffect(Unit) {
        val sm = context.getSystemService(android.content.Context.SENSOR_SERVICE) as? SensorManager
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

private fun formatMb(mb: Long): String {
    return if (mb >= 1024) String.format("%.2f GB", mb / 1024.0) else "$mb MB"
}

private fun formatRate(kbps: Float): String {
    return when {
        kbps >= 1024f * 1024f -> String.format("%.2f GB/s", kbps / (1024f * 1024f))
        kbps >= 1024f -> String.format("%.1f MB/s", kbps / 1024f)
        kbps >= 1f -> String.format("%.0f KB/s", kbps)
        else -> String.format("%.1f KB/s", kbps)
    }
}

private fun formatMhz(mhz: Int): String {
    return if (mhz >= 1000) String.format("%.2f GHz", mhz / 1000.0) else "$mhz MHz"
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
        animationSpec = tween(900, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "g"
    )
    val track = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val arc = MaterialTheme.colorScheme.primary
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
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
                    val stroke = size.minDimension * 0.11f
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


/** Battery / device thermal strip for Home.
 *  < 40°C Normal · 40–50°C Overheating · 50°C+ Extreme
 */
private enum class ThermalLevel { Normal, Overheating, Extreme, Unknown }

private fun classifyThermal(tempC: Float?): ThermalLevel {
    if (tempC == null) return ThermalLevel.Unknown
    return when {
        tempC >= 50f -> ThermalLevel.Extreme
        tempC >= 40f -> ThermalLevel.Overheating
        else -> ThermalLevel.Normal
    }
}

@Composable
private fun ThermalBar(tempC: Float?, thermalLabel: String) {
    val level = classifyThermal(tempC)
    // Map ~25–60°C onto the progress track so the bar feels responsive
    val progressTarget = when {
        tempC == null -> 0f
        else -> ((tempC - 25f) / 35f).coerceIn(0.05f, 1f)
    }
    val a by animateFloatAsState(
        progressTarget,
        animationSpec = tween(900, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "thermal"
    )
    val barColor = when (level) {
        ThermalLevel.Normal -> MaterialTheme.colorScheme.primary
        ThermalLevel.Overheating -> Color(0xFFE6A817) // amber
        ThermalLevel.Extreme -> MaterialTheme.colorScheme.error
        ThermalLevel.Unknown -> MaterialTheme.colorScheme.outline
    }
    val statusText = when (level) {
        ThermalLevel.Normal -> "Normal"
        ThermalLevel.Overheating -> "Overheating"
        ThermalLevel.Extreme -> "Extreme"
        ThermalLevel.Unknown -> "Unavailable"
    }
    val detail = buildString {
        if (tempC != null) {
            append(String.format("%.1f°C", tempC))
            append(" · ")
            append(statusText)
        } else {
            append(statusText)
        }
        val sys = thermalLabel.takeIf {
            it.isNotBlank() && !it.equals("Unknown", true) &&
                !it.equals("None", true) && it != "—"
        }
        if (sys != null) append(" · System $sys")
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Thermal $detail" }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.DeviceThermostat,
                    contentDescription = null,
                    tint = barColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    "Thermal",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    statusText,
                    style = MaterialTheme.typography.labelMedium,
                    color = barColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
            LinearProgressIndicator(
                progress = { a },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
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
private fun Bar(icon: ImageVector, title: String, percent: Float, detail: String) {
    val a by animateFloatAsState(
        (percent / 100f).coerceIn(0f, 1f),
        animationSpec = tween(900, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "b"
    )
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$title $detail" }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
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
                .height(58.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.45f),
                    MaterialTheme.shapes.medium
                )
                .padding(8.dp)
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

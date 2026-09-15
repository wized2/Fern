package com.endroid.fern.ui

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Home
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
import kotlinx.coroutines.delay
import com.endroid.fern.BuildConfig
import com.endroid.fern.data.ThemeMode
import com.endroid.fern.monitor.SystemSnapshot

private enum class Tab { Home, Details, Settings }

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
    lastUpdatedMs: Long,
    onThemeMode: (ThemeMode) -> Unit,
    onRefreshMs: (Int) -> Unit,
    onKeepScreenOn: (Boolean) -> Unit,
    onRefreshNow: () -> Unit
) {
    var tab by remember { mutableStateOf(Tab.Home) }
    Scaffold(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == Tab.Home,
                    onClick = { tab = Tab.Home },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = tab == Tab.Details,
                    onClick = { tab = Tab.Details },
                    icon = { Icon(Icons.Default.Info, contentDescription = "Details") },
                    label = { Text("Details") }
                )
                NavigationBarItem(
                    selected = tab == Tab.Settings,
                    onClick = { tab = Tab.Settings },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                Tab.Home -> HomeContent(snapshot, cpuHistory, ramHistory, batteryHistory, netHistory, storageHistory, lastUpdatedMs, onRefreshNow)
                Tab.Details -> DetailsContent(snapshot)
                Tab.Settings -> SettingsContent(
                    themeMode, refreshMs, keepScreenOn,
                    onThemeMode, onRefreshMs, onKeepScreenOn
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            LeafMark(Modifier.size(36.dp))
            Spacer(modifier = Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Fern",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                val age = if (lastUpdatedMs > 0L) {
                    val sec = ((nowTick - lastUpdatedMs) / 1000L).coerceAtLeast(0)
                    if (sec < 5) "just now" else "${sec}s ago"
                } else {
                    "live"
                }
                val thermal = s?.thermalLabel?.takeIf { it.isNotBlank() && !it.equals("Unknown", true) }
                Text(
                    buildString {
                        append("System pulse · $age")
                        if (thermal != null) append(" · $thermal")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRefreshNow) {
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
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Activity",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
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
                Spark(
                    if (batH.size >= 2) batH else listOf(0f, 0f),
                    MaterialTheme.colorScheme.tertiary,
                    "Battery",
                    fixedMax = 100f
                )
                Spark(
                    if (storageH.size >= 2) storageH else listOf(0f, 0f),
                    MaterialTheme.colorScheme.secondary,
                    "Storage",
                    fixedMax = 100f
                )
                val netLabel = s?.let {
                    if (it.networkKBps >= 1024f) String.format("Net · %.1f MB/s", it.networkKBps / 1024f)
                    else String.format("Net · %.0f KB/s · %s", it.networkKBps, it.networkLabel)
                } ?: "Net"
                Spark(
                    if (netH.size >= 2) netH else listOf(0f, 0f),
                    MaterialTheme.colorScheme.primary,
                    netLabel,
                    fixedMax = null
                )
            }
        }
    }
}

@Composable
private fun DetailsContent(s: SystemSnapshot?) {
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
            s.cpuMaxMhz?.let { Line("Max clock", "$it MHz") }
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
            Line("Total", "${s.ramTotalMb} MB")
            Line("App heap", "${s.appHeapUsedMb} / ${s.appHeapMaxMb} MB")
        }
        Detail("Power") {
            Line("Level", "${s.batteryPercent}%")
            Line("Status", if (s.batteryCharging) "Charging" else "Discharging")
            Line("Health", s.batteryHealth)
            s.batteryTempC?.let { Line("Temp", String.format("%.1f °C", it)) }
        }
        Detail("Device") {
            Line("Model", s.deviceModel)
            Line("Android", "${s.androidVersion} (API ${s.sdkInt})")
            Line("Network", s.networkLabel)
            Line("Throughput", if (s.networkKBps >= 1024f) String.format("%.2f MB/s", s.networkKBps / 1024f) else String.format("%.1f KB/s", s.networkKBps))
            Line("Uptime", String.format("%.1f h", s.uptimeHours))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    themeMode: ThemeMode,
    refreshMs: Int,
    keepScreenOn: Boolean,
    onThemeMode: (ThemeMode) -> Unit,
    onRefreshMs: (Int) -> Unit,
    onKeepScreenOn: (Boolean) -> Unit
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
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
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
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
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
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
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
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LeafMark(modifier = Modifier.size(40.dp))
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
private fun Detail(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
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
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = size.minDimension * 0.1f
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
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
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
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$title $detail" }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.size(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            LinearProgressIndicator(
                progress = { a },
                modifier = Modifier.fillMaxWidth().height(10.dp),
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
                .height(56.dp)
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
            drawPath(fill, color = color.copy(alpha = 0.18f))
            for (i in 0 until pts.lastIndex) {
                drawLine(color, pts[i], pts[i + 1], strokeWidth = 3.5f, cap = StrokeCap.Round)
            }
            // endpoint dots so movement is obvious
            drawCircle(color, radius = 3.5f, center = pts.last())
            drawCircle(color.copy(alpha = 0.5f), radius = 2.5f, center = pts.first())
        }
    }
}

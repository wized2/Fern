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
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier.Modifier
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
import com.endroid.fern.data.ThemeMode
import com.endroid.fern.monitor.SystemSnapshot

private enum class Tab { Home, Details, Settings }

@Composable
fun FernApp(
    snapshot: SystemSnapshot?,
    cpuHistory: List<Float>,
    ramHistory: List<Float>,
    themeMode: ThemeMode,
    refreshMs: Int,
    onThemeMode: (ThemeMode) -> Unit,
    onRefreshMs: (Int) -> Unit
) {
    var tab by remember { mutableStateOf(Tab.Home) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
                NavigationBarItem(
                    selected = tab == Tab.Home,
                    onClick = { tab = Tab.Home },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = tab == Tab.Details,
                    onClick = { tab = Tab.Details },
                    icon = { Icon(Icons.Default.Speed, contentDescription = null) },
                    label = { Text("Details") }
                )
                NavigationBarItem(
                    selected = tab == Tab.Settings,
                    onClick = { tab = Tab.Settings },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") }
                )
            }
        }
    ) { padding ->
        Box(Modifier = Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                Tab.Home -> HomeTab(snapshot, cpuHistory, ramHistory)
                Tab.Details -> DetailsTab(snapshot)
                Tab.Settings -> SettingsTab(themeMode, refreshMs, onThemeMode, onRefreshMs)
            }
        }
    }
}

@Composable
private fun HomeTab(
    snapshot: SystemSnapshot?,
    cpuHistory: List<Float>,
    ramHistory: List<Float>
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LeafIcon(Modifier = Modifier.size(36.dp))
            Spacer(Modifier = Modifier.size(10.dp))
            Column {
                Text(
                    "Fern",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "System pulse · live",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (snapshot == null) {
            Text("Reading sensors…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val cpuLabel = if (snapshot.cpuAvailable) {
                String.format("%.0f%%", snapshot.cpuPercent)
            } else {
                snapshot.loadAvg1?.let { String.format("load %.2f", it) } ?: "N/A"
            }
            GaugeCard(
                title = "CPU",
                percent = if (snapshot.cpuAvailable) snapshot.cpuPercent else {
                    // Map load avg to a soft gauge scale
                    ((snapshot.loadAvg1 ?: 0f) / snapshot.cpuCores.coerceAtLeast(1) * 100f)
                        .coerceIn(0f, 100f)
                },
                subtitle = cpuLabel,
                modifier = Modifier.weight(1f)
            )
            GaugeCard(
                title = "RAM",
                percent = snapshot.ramPercent,
                subtitle = "${snapshot.ramUsedMb} / ${snapshot.ramTotalMb} MB",
                modifier = Modifier.weight(1f)
            )
        }

        MetricBarCard(
            icon = Icons.Default.Storage,
            title = "Storage",
            percent = snapshot.storagePercent,
            detail = String.format("%.1f / %.1f GB used", snapshot.storageUsedGb, snapshot.storageTotalGb)
        )

        MetricBarCard(
            icon = if (snapshot.batteryCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
            title = "Battery",
            percent = snapshot.batteryPercent.toFloat().coerceAtLeast(0f),
            detail = buildString {
                append("${snapshot.batteryPercent}%")
                if (snapshot.batteryCharging) append(" · charging")
                snapshot.batteryTempC?.let { append(String.format(" · %.1f°C", it)) }
            }
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Activity", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Sparkline(
                    if (cpuHistory.isNotEmpty()) cpuHistory else listOf(0f, 0f),
                    MaterialTheme.colorScheme.primary,
                    "CPU trend"
                )
                Sparkline(ramHistory, MaterialTheme.colorScheme.secondary, "RAM trend")
            }
        }

        Spacer(Modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun DetailsTab(snapshot: SystemSnapshot?) {
    Column(
        Modifier
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
        if (snapshot == null) {
            Text("Waiting for data…")
            return
        }

        DetailCard("Processor") {
            InfoRow(Icons.Default.Speed, "Cores", "${snapshot.cpuCores}")
            snapshot.cpuMaxMhz?.let { InfoRow(Icons.Default.Speed, "Max clock", "$it MHz") }
            InfoRow(
                Icons.Default.Speed,
                "CPU load",
                if (snapshot.cpuAvailable) String.format("%.1f%%", snapshot.cpuPercent) else "Restricted"
            )
            snapshot.loadAvg1?.let {
                InfoRow(Icons.Default.Speed, "Load 1 / 5 / 15",
                    String.format("%.2f · %.2f · %.2f", it, snapshot.loadAvg5 ?: 0f, snapshot.loadAvg15 ?: 0f))
            }
        }

        DetailCard("Memory") {
            InfoRow(Icons.Default.Memory, "Used", "${snapshot.ramUsedMb} MB")
            InfoRow(Icons.Default.Memory, "Total", "${snapshot.ramTotalMb} MB")
            InfoRow(Icons.Default.Memory, "App heap", "${snapshot.appHeapUsedMb} / ${snapshot.appHeapMaxMb} MB")
        }

        DetailCard("Power") {
            InfoRow(
                if (snapshot.batteryCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                "Level",
                "${snapshot.batteryPercent}%"
            )
            InfoRow(Icons.Default.BatteryFull, "Status", if (snapshot.batteryCharging) "Charging" else "Discharging")
            InfoRow(Icons.Default.BatteryFull, "Health", snapshot.batteryHealth)
            snapshot.batteryTempC?.let {
                InfoRow(Icons.Default.BatteryFull, "Temperature", String.format("%.1f °C", it))
            }
        }

        DetailCard("Device") {
            InfoRow(Icons.Default.Info, "Model", snapshot.deviceModel)
            InfoRow(Icons.Default.Info, "Android", "${snapshot.androidVersion} (API ${snapshot.sdkInt})")
            InfoRow(Icons.Default.Wifi, "Network", snapshot.networkLabel)
            InfoRow(Icons.Default.Speed, "Uptime", String.format("%.1f h", snapshot.uptimeHours))
            InfoRow(
                Icons.Default.Storage,
                "Storage",
                String.format("%.1f / %.1f GB", snapshot.storageUsedGb, snapshot.storageTotalGb)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTab(
    themeMode: ThemeMode,
    refreshMs: Int,
    onThemeMode: (ThemeMode) -> Unit,
    onRefreshMs: (Int) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Appearance", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                var expanded by remember { mutableStateOf(false) }
                val label = when (themeMode) {
                    ThemeMode.AUTO -> "Auto"
                    ThemeMode.LIGHT -> "Light"
                    ThemeMode.DARK -> "Dark"
                }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Theme") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Refresh", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text(
                    "Every ${"%.1f".format(refreshMs / 1000f)} s",
                    style = MaterialTheme.typography.bodyMedium
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LeafIcon(Modifier = Modifier.size(40.dp))
                    Spacer(Modifier = Modifier.size(12.dp))
                    Column {
                        Text("Fern", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        Text("v1.1.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(
                    "A lightweight Material 3 system monitor. Live gauges for CPU, memory, storage, and battery — no accounts, no ads, no tracking.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "CPU uses /proc/stat when the OS allows it; otherwise load average is shown. Battery temperature and health come from the system battery intent.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DetailCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun LeafIcon(modifier: Modifier = Modifier) {
    val leaf = MaterialTheme.colorScheme.primary
    val vein = MaterialTheme.colorScheme.onPrimaryContainer
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
            color = vein.copy(alpha = 0.5f),
            start = Offset(w * 0.5f, h * 0.2f),
            end = Offset(w * 0.5f, h * 0.85f),
            strokeWidth = w * 0.06f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun GaugeCard(title: String, percent: Float, subtitle: String, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(
        targetValue = percent.coerceIn(0f, 100f) / 100f,
        animationSpec = tween(700),
        label = "gauge"
    )
    val track = MaterialTheme.colorScheme.surfaceVariant
    val arc = MaterialTheme.colorScheme.primary
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier.semantics { contentDescription = "$title $subtitle" }
    ) {
        Column(Modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Box(
                Modifier.fillMaxWidth().aspectRatio(1f).padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = size.minDimension * 0.1f
                    val diameter = size.minDimension - stroke
                    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                    drawArc(
                        color = track, startAngle = 135f, sweepAngle = 270f, useCenter = false,
                        topLeft = topLeft, size = Size(diameter, diameter),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = arc, startAngle = 135f, sweepAngle = 270f * animated, useCenter = false,
                        topLeft = topLeft, size = Size(diameter, diameter),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                Text(
                    String.format("%.0f%%", percent.coerceIn(0f, 100f)),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MetricBarCard(icon: ImageVector, title: String, percent: Float, detail: String) {
    val animated by animateFloatAsState(
        targetValue = (percent / 100f).coerceIn(0f, 1f),
        animationSpec = tween(700),
        label = "bar"
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "$title $detail" }
    ) {
        Column(Modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier = Modifier.size(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            LinearProgressIndicator(
                progress = { animated },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Sparkline(values: List<Float>, color: Color, label: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
                .padding(4.dp)
        ) {
            if (values.size < 2) return@Canvas
            val max = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
            val step = size.width / (values.size - 1).coerceAtLeast(1)
            val pts = values.mapIndexed { i, v ->
                Offset(i * step, size.height - (v / max) * size.height * 0.9f)
            }
            for (i in 0 until pts.lastIndex) {
                drawLine(color, pts[i], pts[i + 1], strokeWidth = 3f, cap = StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier = Modifier.size(8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

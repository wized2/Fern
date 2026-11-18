package com.endroid.fern.monitor

import java.util.Locale

/**
 * Build CSV / markdown for session metric history and live snapshots.
 * Pure functions — safe for unit tests and UI share sheets.
 */
object HistoryExport {
    /** Always US-style numbers: a decimal comma would split CSV columns on de/es/fr devices. */
    private fun f(fmt: String, v: Number): String = String.format(Locale.US, fmt, v)

    /** Keep a value inside one markdown table cell. */
    private fun cell(v: String): String = v.replace("|", "\\|").replace("\n", " ").replace("\r", " ")

    fun batteryCsv(samples: List<Float>, startEpochMs: Long = System.currentTimeMillis()): String {
        val sb = StringBuilder()
        sb.appendLine("index,epoch_ms,battery_percent")
        samples.forEachIndexed { i, v ->
            sb.append(i).append(',')
                .append(startEpochMs + i * 1000L).append(',')
                .append(v).append('\n')
        }
        return sb.toString()
    }

    /**
     * Combined session history for share/export.
     * Rows align by index; missing series pad as empty.
     */
    fun sessionCsv(
        cpu: List<Float>,
        ram: List<Float>,
        battery: List<Float>,
        intervalMs: Int = 1000,
        startEpochMs: Long = System.currentTimeMillis(),
    ): String {
        val n = maxOf(cpu.size, ram.size, battery.size)
        val sb = StringBuilder()
        sb.appendLine("index,epoch_ms,cpu_percent,ram_percent,battery_percent")
        for (i in 0 until n) {
            sb.append(i).append(',')
                .append(startEpochMs + i.toLong() * intervalMs).append(',')
                .append(cpu.getOrNull(i)?.let { f("%.2f", it) } ?: "").append(',')
                .append(ram.getOrNull(i)?.let { f("%.2f", it) } ?: "").append(',')
                .append(battery.getOrNull(i)?.let { f("%.2f", it) } ?: "")
                .append('\n')
        }
        return sb.toString()
    }

    /** Markdown dashboard snapshot for chat/docs (issue #9). */
    fun snapshotMarkdown(
        deviceModel: String,
        androidVersion: String,
        sdkInt: Int,
        cpuPercent: Float,
        cpuCores: Int,
        cpuHardware: String,
        ramUsedMb: Long,
        ramTotalMb: Long,
        ramPercent: Float,
        batteryPercent: Int,
        batteryCharging: Boolean,
        batteryHealth: String,
        batteryTempC: Float?,
        thermalLabel: String,
        networkLabel: String,
        networkKBps: Float,
        uptimeHours: Float,
        peakCpu: Float,
        peakRam: Float,
        avgCpu: Float,
        avgRam: Float,
    ): String = buildString {
        appendLine("# Fern snapshot")
        appendLine()
        appendLine("| Field | Value |")
        appendLine("| --- | --- |")
        appendLine("| Device | ${cell(deviceModel)} |")
        appendLine("| Android | ${cell(androidVersion)} (API $sdkInt) |")
        appendLine("| CPU | ${f("%.1f", cpuPercent)}% · $cpuCores cores · ${cell(cpuHardware)} |")
        appendLine("| RAM | $ramUsedMb / $ramTotalMb MB (${f("%.1f", ramPercent)}%) |")
        appendLine(
            "| Battery | $batteryPercent% · ${if (batteryCharging) "charging" else "discharging"} · ${cell(batteryHealth)} |"
        )
        batteryTempC?.let { appendLine("| Battery temp | ${f("%.1f", it)}°C |") }
        appendLine("| Thermal | ${cell(thermalLabel)} |")
        appendLine("| Network | ${cell(networkLabel)} · ${f("%.1f", networkKBps)} KB/s |")
        appendLine("| Uptime | ${f("%.1f", uptimeHours)} h |")
        appendLine("| Session peak CPU / RAM | ${f("%.1f", peakCpu)}% / ${f("%.1f", peakRam)}% |")
        appendLine("| Session avg CPU / RAM | ${f("%.1f", avgCpu)}% / ${f("%.1f", avgRam)}% |")
        appendLine()
        appendLine("_Exported offline from Fern. No account required._")
    }
}

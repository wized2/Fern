package com.endroid.fern.monitor

/**
 * Build CSV / markdown for session metric history and live snapshots.
 * Pure functions — safe for unit tests and UI share sheets.
 */
object HistoryExport {
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
                .append(cpu.getOrNull(i)?.let { "%.2f".format(it) } ?: "").append(',')
                .append(ram.getOrNull(i)?.let { "%.2f".format(it) } ?: "").append(',')
                .append(battery.getOrNull(i)?.let { "%.2f".format(it) } ?: "")
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
        appendLine("| Device | $deviceModel |")
        appendLine("| Android | $androidVersion (API $sdkInt) |")
        appendLine("| CPU | ${"%.1f".format(cpuPercent)}% · $cpuCores cores · $cpuHardware |")
        appendLine("| RAM | $ramUsedMb / $ramTotalMb MB (${"%.1f".format(ramPercent)}%) |")
        appendLine(
            "| Battery | $batteryPercent% · ${if (batteryCharging) "charging" else "discharging"} · $batteryHealth |"
        )
        batteryTempC?.let { appendLine("| Battery temp | ${"%.1f".format(it)}°C |") }
        appendLine("| Thermal | $thermalLabel |")
        appendLine("| Network | $networkLabel · ${"%.1f".format(networkKBps)} KB/s |")
        appendLine("| Uptime | ${"%.1f".format(uptimeHours)} h |")
        appendLine("| Session peak CPU / RAM | ${"%.1f".format(peakCpu)}% / ${"%.1f".format(peakRam)}% |")
        appendLine("| Session avg CPU / RAM | ${"%.1f".format(avgCpu)}% / ${"%.1f".format(avgRam)}% |")
        appendLine()
        appendLine("_Exported offline from Fern. No account required._")
    }
}

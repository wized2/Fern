package com.endroid.fern.monitor

/**
 * Build CSV for session metric history (share/export). Pure — safe for unit tests.
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
}

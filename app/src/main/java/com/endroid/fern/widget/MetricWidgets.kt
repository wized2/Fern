package com.endroid.fern.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.endroid.fern.MainActivity
import com.endroid.fern.R
import com.endroid.fern.monitor.SystemMetrics

class BatteryWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(id, buildViews(context, MetricKind.BATTERY))
        }
    }
}

class RamWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(id, buildViews(context, MetricKind.RAM))
        }
    }
}

private enum class MetricKind { BATTERY, RAM }

private fun buildViews(context: Context, kind: MetricKind): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_metric)
    val snap = try {
        SystemMetrics.capture(context)
    } catch (_: Exception) {
        null
    }

    when (kind) {
        MetricKind.BATTERY -> {
            views.setTextViewText(R.id.widget_title, "Battery")
            if (snap != null) {
                val pct = snap.batteryPercent.coerceIn(0, 100)
                views.setTextViewText(R.id.widget_value, "$pct%")
                views.setProgressBar(R.id.widget_progress, 100, pct, false)
                val temp = snap.batteryTempC
                val status = if (snap.batteryCharging) "Charging" else "On battery"
                views.setTextViewText(
                    R.id.widget_subtitle,
                    if (temp > 0f) String.format("%s · %.0f°C", status, temp) else status
                )
            } else {
                views.setTextViewText(R.id.widget_value, "—")
                views.setProgressBar(R.id.widget_progress, 100, 0, false)
                views.setTextViewText(R.id.widget_subtitle, "Open Fern")
            }
        }
        MetricKind.RAM -> {
            views.setTextViewText(R.id.widget_title, "Memory")
            if (snap != null) {
                val pct = snap.ramPercent.toInt().coerceIn(0, 100)
                views.setTextViewText(R.id.widget_value, "$pct%")
                views.setProgressBar(R.id.widget_progress, 100, pct, false)
                views.setTextViewText(
                    R.id.widget_subtitle,
                    "${snap.ramUsedMb} / ${snap.ramTotalMb} MB"
                )
            } else {
                views.setTextViewText(R.id.widget_value, "—")
                views.setProgressBar(R.id.widget_progress, 100, 0, false)
                views.setTextViewText(R.id.widget_subtitle, "Open Fern")
            }
        }
    }

    val open = PendingIntent.getActivity(
        context,
        if (kind == MetricKind.BATTERY) 1 else 2,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.widget_root, open)
    return views
}

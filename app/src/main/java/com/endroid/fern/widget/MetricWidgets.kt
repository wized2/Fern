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
            appWidgetManager.updateAppWidget(id, buildViews(context, isBattery = true))
        }
    }
}

class RamWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(id, buildViews(context, isBattery = false))
        }
    }
}

private fun buildViews(context: Context, isBattery: Boolean): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_metric)
    val snap = try {
        SystemMetrics.capture(context)
    } catch (_: Exception) {
        null
    }
    if (isBattery) {
        views.setTextViewText(R.id.widget_title, "Battery")
        if (snap != null) {
            val pct = snap.batteryPercent.coerceIn(0, 100)
            views.setTextViewText(R.id.widget_value, "$pct%")
            views.setProgressBar(R.id.widget_progress, 100, pct, false)
            views.setTextViewText(
                R.id.widget_subtitle,
                if (snap.batteryCharging) "Charging" else "Discharging"
            )
        } else {
            views.setTextViewText(R.id.widget_value, "—")
            views.setProgressBar(R.id.widget_progress, 100, 0, false)
            views.setTextViewText(R.id.widget_subtitle, "Open Fern")
        }
    } else {
        views.setTextViewText(R.id.widget_title, "RAM")
        if (snap != null) {
            val pct = snap.ramPercent.toInt().coerceIn(0, 100)
            views.setTextViewText(R.id.widget_value, String.format("%d%%", pct))
            views.setProgressBar(R.id.widget_progress, 100, pct, false)
            views.setTextViewText(
                R.id.widget_subtitle,
                snap.ramUsedMb.toString() + " / " + snap.ramTotalMb + " MB"
            )
        } else {
            views.setTextViewText(R.id.widget_value, "—")
            views.setProgressBar(R.id.widget_progress, 100, 0, false)
            views.setTextViewText(R.id.widget_subtitle, "Open Fern")
        }
    }
    val open = PendingIntent.getActivity(
        context,
        if (isBattery) 1 else 2,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.widget_root, open)
    return views
}

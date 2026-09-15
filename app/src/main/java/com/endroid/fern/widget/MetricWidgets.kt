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
            views.setTextViewText(R.id.widget_value, snap.batteryPercent.toString() + "%")
            val sub = if (snap.batteryCharging) "Charging" else "Discharging"
            views.setTextViewText(R.id.widget_subtitle, sub)
        } else {
            views.setTextViewText(R.id.widget_value, "—")
            views.setTextViewText(R.id.widget_subtitle, "Open Fern")
        }
    } else {
        views.setTextViewText(R.id.widget_title, "RAM")
        if (snap != null) {
            views.setTextViewText(
                R.id.widget_value,
                String.format("%.0f%%", snap.ramPercent)
            )
            views.setTextViewText(
                R.id.widget_subtitle,
                snap.ramUsedMb.toString() + " / " + snap.ramTotalMb + " MB"
            )
        } else {
            views.setTextViewText(R.id.widget_value, "—")
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

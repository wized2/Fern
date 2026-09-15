package com.endroid.fern.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.endroid.fern.MainActivity
import com.endroid.fern.R
import com.endroid.fern.monitor.SystemMetrics

class BatteryWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAll(context, appWidgetManager, appWidgetIds, kind = Kind.BATTERY)
    }

    companion object {
        fun refresh(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, BatteryWidgetProvider::class.java))
            if (ids.isNotEmpty()) updateAll(context, mgr, ids, Kind.BATTERY)
        }
    }
}

class RamWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAll(context, appWidgetManager, appWidgetIds, kind = Kind.RAM)
    }

    companion object {
        fun refresh(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, RamWidgetProvider::class.java))
            if (ids.isNotEmpty()) updateAll(context, mgr, ids, Kind.RAM)
        }
    }
}

private enum class Kind { BATTERY, RAM }

private fun updateAll(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray,
    kind: Kind
) {
    val snap = try {
        SystemMetrics.capture(context)
    } catch (_: Exception) {
        null
    }
    val open = PendingIntent.getActivity(
        context,
        kind.ordinal,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    for (id in appWidgetIds) {
        val views = RemoteViews(context.packageName, R.layout.widget_metric)
        when (kind) {
            Kind.BATTERY -> {
                views.setTextViewText(R.id.widget_title, "Battery")
                if (snap != null) {
                    views.setTextViewText(R.id.widget_value, "${snap.batteryPercent}%")
                    val sub = buildString {
                        append(if (snap.batteryCharging) "Charging" else "Discharging")
                        snap.batteryTempC?.let { append(" · %.0f°C".format(it)) }
                    }
                    views.setTextViewText(R.id.widget_subtitle, sub)
                } else {
                    views.setTextViewText(R.id.widget_value, "—")
                    views.setTextViewText(R.id.widget_subtitle, "Open Fern")
                }
            }
            Kind.RAM -> {
                views.setTextViewText(R.id.widget_title, "RAM")
                if (snap != null) {
                    views.setTextViewText(R.id.widget_value, "%.0f%%".format(snap.ramPercent))
                    views.setTextViewText(
                        R.id.widget_subtitle,
                        "${snap.ramUsedMb} / ${snap.ramTotalMb} MB"
                    )
                } else {
                    views.setTextViewText(R.id.widget_value, "—")
                    views.setTextViewText(R.id.widget_subtitle, "Open Fern")
                }
            }
        }
        views.setOnClickPendingIntent(R.id.widget_root, open)
        appWidgetManager.updateAppWidget(id, views)
    }
}

package com.seanime.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.content.ComponentName
import android.view.View
import android.widget.RemoteViews
import com.seanime.app.R
import com.seanime.app.SeanimeService

class UpcomingAnimeWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH = "com.seanime.app.widget.ACTION_REFRESH"
        const val ACTION_DATA_UPDATED = "com.seanime.app.widget.ACTION_DATA_UPDATED"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val cache = WidgetCache(context)
        for (id in appWidgetIds) {
            if (!cache.hasValidCache() || cache.isLoading()) {
                refreshWidget(context, appWidgetManager, id)
            } else {
                updateWidget(context, appWidgetManager, id)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (ACTION_REFRESH == intent.action) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 
                AppWidgetManager.INVALID_APPWIDGET_ID)
            
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                // Clear cache to force refresh
                val cache = WidgetCache(context)
                cache.clearCache()
                
                // Start server and refresh
                refreshWidget(context, appWidgetManager, appWidgetId)
            }
        } else if (ACTION_DATA_UPDATED == intent.action) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, UpcomingAnimeWidget::class.java))
            for (id in ids) {
                updateWidget(context, appWidgetManager, id)
            }
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)
        val cache = WidgetCache(context)

        // Check for error first
        val error = cache.getError()
        if (error != null) {
            // Show error in loading text
            views.setTextViewText(R.id.loading_text, "Error: $error")
            views.setViewVisibility(R.id.loading_text, View.VISIBLE)
            views.setViewVisibility(R.id.anime_list_view, View.INVISIBLE)
        } else if (cache.isLoading() || !cache.hasValidCache()) {
            // Show loading state
            views.setTextViewText(R.id.loading_text, "Nothing here yet")
            views.setViewVisibility(R.id.loading_text, View.VISIBLE)
            // Keep the ListView around so RemoteViewsService still runs and can fetch.
            views.setViewVisibility(R.id.anime_list_view, View.INVISIBLE)
        } else {
            views.setViewVisibility(R.id.loading_text, View.GONE)
            views.setViewVisibility(R.id.anime_list_view, View.VISIBLE)
        }

        // Set up refresh button click
        val refreshIntent = Intent(context, UpcomingAnimeWidget::class.java).apply {
            action = ACTION_REFRESH
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val refreshPI = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            refreshIntent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.refresh_button, refreshPI)

        // Adapter for the list
        val serviceIntentList = Intent(context, AnimeListService::class.java)
        serviceIntentList.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        serviceIntentList.data = Uri.parse(serviceIntentList.toUri(Intent.URI_INTENT_SCHEME))
        views.setRemoteAdapter(R.id.anime_list_view, serviceIntentList)

        // Click Template (Opens the link in browser/app)
        val clickIntent = Intent(Intent.ACTION_VIEW)
        val clickPI = PendingIntent.getActivity(
            context,
            0,
            clickIntent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setPendingIntentTemplate(R.id.anime_list_view, clickPI)

        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.anime_list_view)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun refreshWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        // Start server binary
        val serviceIntent = Intent(context, SeanimeService::class.java)
        context.startForegroundService(serviceIntent)

        WidgetCache(context).setLoading(true)

        // Bind adapter immediately (so the factory can start working) while showing loading.
        updateWidget(context, appWidgetManager, appWidgetId)

        // Force a dataset refresh right away.
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.anime_list_view)

        // Also retry shortly after to cover slow server boot.
        Handler(Looper.getMainLooper()).postDelayed({
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.anime_list_view)
        }, 2000)
    }
}

package com.jamsmendez.cieldroid.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.jamsmendez.cieldroid.MainActivity
import com.jamsmendez.cieldroid.R
import com.jamsmendez.cieldroid.data.PreferencesSettingsRepository
import com.jamsmendez.cieldroid.data.settingsPreferences
import com.jamsmendez.cieldroid.domain.PreviewSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

private const val TAG = "CielWidget"

/**
 * Still snapshot of the WALLPAPER scene selection. No periodic updates: refreshed when a widget is
 * added or resized and when the wallpaper scene/FPS changes. The PNG cache stays in app-internal files.
 */
class SceneWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) =
        refresh(context, manager, appWidgetIds)

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) =
        refresh(context, manager, intArrayOf(appWidgetId))

    private fun refresh(context: Context, manager: AppWidgetManager, ids: IntArray) {
        if (ids.isEmpty()) return
        val pending = goAsync()
        val app = context.applicationContext
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope.launch {
            try {
                val settings = withTimeoutOrNull(SETTINGS_TIMEOUT_MS) { readSettings(app) } ?: PreviewSettings.WALLPAPER_DEFAULT
                val snapshot = withTimeoutOrNull(TOTAL_TIMEOUT_MS - SETTINGS_TIMEOUT_MS) {
                    renderLock.withLock { cachedOrRendered(app, manager, ids, settings) }
                }
                ids.forEach { manager.updateAppWidget(it, views(app, settings, snapshot)) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e(TAG, "widget refresh failed", e)
            } finally {
                pending.finish()
                scope.cancel()
            }
        }
    }

    private suspend fun readSettings(app: Context): PreviewSettings {
        val readScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        try {
            val state = PreferencesSettingsRepository(app.settingsPreferences, readScope).state
                .first { it.loaded || it.error != null }
            return if (state.loaded) state.wallpaper else PreviewSettings.WALLPAPER_DEFAULT
        } finally {
            readScope.cancel()
        }
    }

    /** Re-renders only when no PNG exists for this scene+fps; earlier keys' PNGs are removed. */
    private suspend fun cachedOrRendered(app: Context, manager: AppWidgetManager, ids: IntArray, settings: PreviewSettings): Bitmap? {
        val dir = app.filesDir
        val name = SnapshotCache.fileName(settings)
        val existing = dir.list()?.toList().orEmpty()
        if (!SnapshotCache.isStale(name, existing)) {
            BitmapFactory.decodeFile(File(dir, name).path)?.let { return it }
        }
        val size = targetSize(app, manager, ids)
        val bitmap = SceneSnapshotRenderer.render(app, settings, size.width, size.height, RENDER_TIMEOUT_MS) ?: return null
        val temp = File(dir, "$name.tmp")
        temp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        if (!temp.renameTo(File(dir, name))) temp.delete()
        SnapshotCache.obsolete(name, existing).forEach { File(dir, it).delete() }
        return bitmap
    }

    /** The largest requested widget size; portrait hosts report min width and max height. */
    private fun targetSize(app: Context, manager: AppWidgetManager, ids: IntArray): SnapshotSize {
        val density = app.resources.displayMetrics.density
        return ids.map {
            val options = manager.getAppWidgetOptions(it)
            SnapshotGeometry.target(
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH),
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT),
                density,
            )
        }.maxBy { it.width * it.height }
    }

    private fun views(app: Context, settings: PreviewSettings, snapshot: Bitmap?) =
        RemoteViews(app.packageName, R.layout.scene_widget).apply {
            val sceneName = settings.scene.assetId.replaceFirstChar { it.uppercase() }
            if (snapshot != null) {
                setImageViewBitmap(R.id.widget_snapshot, snapshot)
                setViewVisibility(R.id.widget_snapshot, View.VISIBLE)
                setViewVisibility(R.id.widget_fallback, View.GONE)
            } else {
                setTextViewText(R.id.widget_scene_name, app.getString(R.string.widget_fallback_scene, sceneName))
                setViewVisibility(R.id.widget_snapshot, View.GONE)
                setViewVisibility(R.id.widget_fallback, View.VISIBLE)
            }
            setContentDescription(R.id.widget_root, app.getString(R.string.widget_content_description, sceneName))
            val open = Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getActivity(app, 0, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
            )
        }

    companion object {
        private const val SETTINGS_TIMEOUT_MS = 1_000L
        private const val TOTAL_TIMEOUT_MS = 8_000L
        private const val RENDER_TIMEOUT_MS = 6_000L
        /** One snapshot WebView at a time, process-wide; each render still owns its own WebView. */
        private val renderLock = Mutex()

        /** Asks every placed widget to refresh; a no-op when none is placed. */
        fun requestUpdate(context: Context) {
            val app = context.applicationContext
            val component = ComponentName(app, SceneWidgetProvider::class.java)
            val ids = runCatching { AppWidgetManager.getInstance(app)?.getAppWidgetIds(component) }
                .onFailure { Log.w(TAG, "widget ids unavailable", it) }.getOrNull()
            if (ids == null || ids.isEmpty()) return
            app.sendBroadcast(
                Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).setComponent(component)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids),
            )
        }
    }
}

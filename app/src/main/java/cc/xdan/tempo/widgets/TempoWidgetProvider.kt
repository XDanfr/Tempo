package cc.xdan.tempo.widgets

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.*
import android.util.SizeF
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.work.*
import cc.xdan.tempo.data.TimetableRepository
import cc.xdan.tempo.design.presetColorScheme
import cc.xdan.tempo.model.*
import kotlinx.coroutines.flow.first
import java.time.*
import java.util.concurrent.TimeUnit

open class TempoWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) { WidgetRefresh.enqueue(context) }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) { WidgetRefresh.enqueue(context) }
    override fun onEnabled(context: Context) { WidgetRefresh.start(context) }
    override fun onDeleted(context: Context, ids: IntArray) { ids.forEach { WidgetSettings.delete(context, it) }; WidgetRefresh.enqueue(context) }
    override fun onDisabled(context: Context) { WidgetRefresh.enqueue(context) }
}
class AdaptiveTempoWidget : TempoWidgetProvider()
class PillWidget : TempoWidgetProvider()
class SquareWidget : TempoWidgetProvider()
class WideWidget : TempoWidgetProvider()
class WidgetClockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { WidgetRefresh.start(context) }
}

object WidgetRefresh {
    fun ids(context: Context): IntArray {
        val manager = AppWidgetManager.getInstance(context)
        return listOf(AdaptiveTempoWidget::class.java, PillWidget::class.java, SquareWidget::class.java, WideWidget::class.java)
            .flatMap { manager.getAppWidgetIds(ComponentName(context, it)).toList() }.toIntArray()
    }
    fun start(context: Context) {
        if (ids(context).isEmpty()) return
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("tempo-widgets-periodic", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<WidgetRefreshWorker>(15, TimeUnit.MINUTES).build())
        enqueue(context)
    }
    fun enqueue(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork("tempo-widgets-refresh", ExistingWorkPolicy.APPEND_OR_REPLACE,
            OneTimeWorkRequestBuilder<WidgetRefreshWorker>().build())
    }
}

class WidgetRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        val manager = AppWidgetManager.getInstance(context)
        val ids = WidgetRefresh.ids(context)
        val alarm = context.getSystemService(AlarmManager::class.java)
        val refresh = PendingIntent.getBroadcast(context, 0, Intent(context, WidgetClockReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (ids.isEmpty()) {
            WorkManager.getInstance(context).cancelUniqueWork("tempo-widgets-periodic")
            alarm.cancel(refresh)
            return Result.success()
        }
        return try {
            val collection = TimetableRepository(context).collection.first()
            val now = ZonedDateTime.now()
            var nextRefresh = now.plusDays(1).toInstant().toEpochMilli()
            ids.forEach { id ->
                val settings = WidgetSettings.read(context, id)
                val timetable = if (settings.timetableId == null) collection.active else collection.timetables.firstOrNull { it.id == settings.timetableId }?.timetable
                val provider = manager.getAppWidgetInfo(id)?.provider?.className
                val pill = provider == PillWidget::class.java.name

                val appearance = timetable?.appearance ?: Appearance()
                val selected = settings.preset ?: appearance.selectedPreset
                val mode = if (settings.preset == null) appearance.mode else settings.mode
                val dark = mode == ThemeMode.DARK || (mode == ThemeMode.SYSTEM && (context.resources.configuration.uiMode and 0x30) == 0x20)
                val scheme = if (selected == ThemePreset.MATERIAL_YOU && android.os.Build.VERSION.SDK_INT >= 31) {
                    if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                } else presetColorScheme(selected, dark)
                val options = manager.getAppWidgetOptions(id)
                val presentation = widgetPresentation(timetable, settings, now)
                presentation.boundaryEpochMillis?.let { nextRefresh = minOf(nextRefresh, it) }
                suspend fun render(size: SizeF): RemoteViews = adaptiveWidgetViews(
                    context, presentation, WidgetSizing(size.width, size.height, context.resources.configuration.fontScale),
                    settings, scheme, dark, settings.showIcons ?: appearance.showIcons, now.toInstant().toEpochMilli())
                val sizes = widgetSizes(options, pill)
                val views = if (Build.VERSION.SDK_INT >= 31) {
                    RemoteViews(sizes.associateWith { render(it) })
                } else {
                    RemoteViews(render(sizes.last()), render(sizes.first()))
                }
                manager.updateAppWidget(id, views)
            }
            alarm.setAndAllowWhileIdle(AlarmManager.RTC, nextRefresh.coerceAtLeast(System.currentTimeMillis() + 1000), refresh)
            Result.success()
        } catch (_: Exception) { Result.retry() }
    }
}

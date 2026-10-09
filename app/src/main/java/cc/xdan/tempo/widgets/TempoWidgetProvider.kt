package cc.xdan.tempo.widgets

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.*
import android.graphics.*
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.toArgb
import androidx.work.*
import cc.xdan.tempo.MainActivity
import cc.xdan.tempo.R
import cc.xdan.tempo.data.TimetableRepository
import cc.xdan.tempo.design.presetColorScheme
import cc.xdan.tempo.model.*
import cc.xdan.tempo.schedule.*
import kotlinx.coroutines.flow.first
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

open class TempoWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) { WidgetRefresh.enqueue(context) }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) { WidgetRefresh.enqueue(context) }
    override fun onEnabled(context: Context) { WidgetRefresh.start(context) }
    override fun onDeleted(context: Context, ids: IntArray) { ids.forEach { WidgetSettings.delete(context, it) }; WidgetRefresh.enqueue(context) }
    override fun onDisabled(context: Context) { WidgetRefresh.enqueue(context) }
}
class PillWidget : TempoWidgetProvider()
class SquareWidget : TempoWidgetProvider()
class WideWidget : TempoWidgetProvider()
class WidgetClockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { WidgetRefresh.start(context) }
}

object WidgetRefresh {
    fun ids(context: Context): IntArray {
        val manager = AppWidgetManager.getInstance(context)
        return listOf(PillWidget::class.java, SquareWidget::class.java, WideWidget::class.java)
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
                val pill = manager.getAppWidgetInfo(id)?.provider?.className == PillWidget::class.java.name
                val views = RemoteViews(context.packageName, if (pill) R.layout.tempo_widget_pill else R.layout.tempo_widget)
                val appearance = timetable?.appearance ?: Appearance()
                val selected = settings.preset ?: appearance.selectedPreset
                val mode = if (settings.preset == null) appearance.mode else settings.mode
                val dark = mode == ThemeMode.DARK || (mode == ThemeMode.SYSTEM && (context.resources.configuration.uiMode and 0x30) == 0x20)
                val scheme = if (selected == ThemePreset.MATERIAL_YOU && android.os.Build.VERSION.SDK_INT >= 31) {
                    if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                } else presetColorScheme(selected, dark)
                val options = manager.getAppWidgetOptions(id)
                val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 150).coerceIn(110, 600)
                val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 90).coerceIn(56, 400)
                val bitmap = Bitmap.createBitmap(width * 2, height * 2, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                val radius = if (pill) bitmap.height / 2f else 56f
                canvas.drawRoundRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = scheme.surfaceContainer.toArgb() })
                views.setImageViewBitmap(R.id.widget_background, bitmap)
                listOf(R.id.widget_title, R.id.widget_detail, R.id.widget_settings).forEach { views.setTextColor(it, scheme.onSurface.toArgb()) }
                views.setTextColor(R.id.widget_label, scheme.onSurfaceVariant.toArgb())
                views.setTextColor(R.id.widget_timer, scheme.primary.toArgb())
                views.setViewVisibility(R.id.widget_detail, if (height < 100) View.GONE else View.VISIBLE)
                views.setTextViewText(R.id.widget_label, timetable?.name ?: "Tempo")
                var title = if (timetable == null) "Choose a timetable" else if (!timetable.onboarded) "Finish setting up Tempo" else settings.emptyText
                var detail = if (timetable == null) "This widget’s timetable was removed. Open widget settings." else ""
                var target: ZonedDateTime? = null
                var format = "%s until"
                if (timetable?.onboarded == true) {
                    val summary = overview(timetable, now.toLocalDateTime())
                    summary.boundary?.atZone(now.zone)?.let { nextRefresh = minOf(nextRefresh, it.toInstant().toEpochMilli()) }
                    val current = summary.current
                    when (current) {
                        is ScheduleBlock.Lesson -> {
                            title = current.subject.name
                            detail = if (settings.showLocation) current.location else ""
                            target = now.toLocalDate().atStartOfDay().plusMinutes(current.time.end.toLong()).atZone(now.zone)
                            format = "%s left"
                        }
                        is ScheduleBlock.Break -> {
                            title = current.scheduled.name
                            target = now.toLocalDate().atStartOfDay().plusMinutes(current.time.end.toLong()).atZone(now.zone)
                            format = "%s left"
                            detail = summary.next?.let { "Up next: ${it.lesson.subject.name} · ${it.starts.format(DateTimeFormatter.ofPattern("EEE HH:mm"))}" } ?: "No upcoming sessions"
                        }
                        else -> summary.next?.let {
                            title = it.lesson.subject.name
                            target = it.starts.atZone(now.zone)
                            detail = "${it.starts.format(DateTimeFormatter.ofPattern("EEE HH:mm"))}" + if (settings.showLocation && it.lesson.location.isNotBlank()) " · ${it.lesson.location}" else ""
                        }
                    }
                }
                views.setTextViewText(R.id.widget_title, title)
                views.setTextViewText(R.id.widget_detail, detail)
                val until = target?.toInstant()?.toEpochMilli()?.minus(now.toInstant().toEpochMilli())
                if (pill && (until == null || until > 86_400_000) && detail.isNotBlank()) views.setViewVisibility(R.id.widget_detail, View.VISIBLE)
                views.setViewVisibility(R.id.widget_timer, if (until != null && until in 1..86_400_000) View.VISIBLE else View.GONE)
                views.setChronometerCountDown(R.id.widget_timer, true)
                views.setChronometer(R.id.widget_timer, SystemClock.elapsedRealtime() + (until ?: 0), format, until != null && until in 1..86_400_000)
                val open = Intent(context, MainActivity::class.java).putExtra("widgetTimetableId", settings.timetableId)
                views.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(context, id, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                val configure = Intent(context, WidgetConfigureActivity::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                views.setOnClickPendingIntent(R.id.widget_settings, PendingIntent.getActivity(context, id + 100000, configure, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                manager.updateAppWidget(id, views)
            }
            alarm.setAndAllowWhileIdle(AlarmManager.RTC, nextRefresh.coerceAtLeast(System.currentTimeMillis() + 1000), refresh)
            Result.success()
        } catch (_: Exception) { Result.retry() }
    }
}

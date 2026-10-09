package cc.xdan.tempo.widgets

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import cc.xdan.tempo.ui.accent
import cc.xdan.tempo.ui.symbol
import cc.xdan.tempo.ui.title
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
                val summary = timetable?.takeIf { it.onboarded }?.let { weekOverview(it, now.toLocalDateTime()) }
                summary?.boundary?.atZone(now.zone)?.let { nextRefresh = minOf(nextRefresh, it.toInstant().toEpochMilli()) }
                val block = summary?.block
                val accent = block?.accent(scheme.primary) ?: scheme.primary
                val outlined = block != null && (if (block is ScheduleBlock.Break) appearance.effectiveBreakStyle else appearance.blockStyle) == BlockStyle.OUTLINED
                val blockColour = if (outlined) scheme.surfaceContainerHigh else lerp(scheme.surfaceContainerHigh, accent, if (dark) .38f else .65f)
                val foreground = if (dark) Color(0xFFF2F6EE) else Color(0xFF152015)
                val title = when {
                    timetable == null -> "Choose a timetable"
                    !timetable.onboarded -> "Finish setup"
                    summary?.status == WeekStatus.DONE -> "All done"
                    summary?.status == WeekStatus.EMPTY -> settings.emptyText
                    else -> block?.title() ?: settings.emptyText
                }
                val current = summary?.status == WeekStatus.CURRENT
                val lesson = block as? ScheduleBlock.Lesson
                val target = if (current && block != null) now.toLocalDate().atStartOfDay().plusMinutes(block.time.end.toLong()).atZone(now.zone)
                    else summary?.next?.starts?.atZone(now.zone)
                val until = target?.toInstant()?.toEpochMilli()?.minus(now.toInstant().toEpochMilli())
                val ticking = until != null && until in 1..86_400_000
                val whenText = if (current && block != null) "${minuteLabel(block.time.start)}–${minuteLabel(block.time.end)}"
                    else summary?.next?.starts?.format(DateTimeFormatter.ofPattern("EEE HH:mm")) ?: ""
                val location = if (settings.showLocation) lesson?.location?.takeIf { it.isNotBlank() } else null
                var detail = when (summary?.status) {
                    WeekStatus.DONE -> "Nothing else scheduled this week"
                    WeekStatus.EMPTY -> "Your week is clear"
                    else -> listOfNotNull(whenText.takeIf { it.isNotBlank() }, location).joinToString(" · ")
                }
                if (timetable == null) detail = "Timetable removed · choose another"
                if (current && block is ScheduleBlock.Break && summary?.next != null) {
                    detail += "\nNext: ${summary.next.lesson.subject.name} · ${summary.next.starts.format(DateTimeFormatter.ofPattern("EEE HH:mm"))}"
                }
                val showIcon = appearance.showIcons
                val icon = when (summary?.status) {
                    WeekStatus.DONE -> Icons.Outlined.CheckCircle
                    WeekStatus.EMPTY -> Icons.Outlined.EventAvailable
                    else -> block?.symbol() ?: Icons.Outlined.CalendarMonth
                }
                val bg = if (pill) blockColour else scheme.surfaceContainerHigh
                views.setImageViewBitmap(R.id.widget_background, widgetShape(width * 2, height * 2, bg.toArgb(), if (pill) height.toFloat() else 56f, if (pill && outlined) accent.toArgb() else null))
                if (!pill) views.setImageViewBitmap(R.id.widget_block_background, widgetShape(width * 2, height * 2, blockColour.toArgb(), 40f, accent.copy(alpha = .65f).toArgb()))
                views.setViewVisibility(R.id.widget_icon, if (showIcon) View.VISIBLE else View.GONE)
                views.setImageViewBitmap(R.id.widget_icon, widgetIcon(icon, accent.toArgb()))
                views.setTextViewText(R.id.widget_label, when (summary?.status) {
                    WeekStatus.DONE -> "THIS WEEK · ${timetable?.name}"
                    WeekStatus.CURRENT -> "NOW · ${timetable?.name}"
                    WeekStatus.UPCOMING -> "UP NEXT · ${timetable?.name}"
                    else -> timetable?.name ?: "TEMPO"
                })
                views.setTextColor(R.id.widget_label, scheme.primary.toArgb())
                views.setTextColor(R.id.widget_settings, scheme.onSurface.toArgb())
                listOf(R.id.widget_title, R.id.widget_detail, R.id.widget_timer, R.id.widget_time).forEach { views.setTextColor(it, foreground.toArgb()) }
                views.setTextViewText(R.id.widget_title, title)
                views.setTextViewText(R.id.widget_detail, detail)
                views.setInt(R.id.widget_title, "setMaxLines", if (pill || height < 145) 1 else 2)
                views.setTextViewTextSize(R.id.widget_title, android.util.TypedValue.COMPLEX_UNIT_SP, if (pill) 14f else if (height < 145) 16f else 20f)
                views.setTextViewTextSize(R.id.widget_timer, android.util.TypedValue.COMPLEX_UNIT_SP, if (pill) 12f else if (height < 145) 16f else 24f)
                views.setViewVisibility(R.id.widget_timer, if (ticking) View.VISIBLE else View.GONE)
                views.setChronometerCountDown(R.id.widget_timer, true)
                views.setChronometer(R.id.widget_timer, SystemClock.elapsedRealtime() + (until ?: 0), if (current) "%s left" else "In %s", ticking)
                views.setViewVisibility(R.id.widget_detail, if (pill && ticking || !pill && height < 130) View.GONE else View.VISIBLE)
                views.setViewVisibility(R.id.widget_time, if (!pill && !ticking && target != null && height >= 145) View.VISIBLE else View.GONE)
                views.setTextViewText(R.id.widget_time, whenText)
                if (!pill) {
                    val endOfWeek = now.toLocalDate().plusDays((7 - now.dayOfWeek.value).toLong())
                    val after = summary?.next?.starts ?: now.toLocalDateTime()
                    val upcoming = timetable?.let { t -> (0..(7 - now.dayOfWeek.value)).firstNotNullOfOrNull { offset ->
                        val date = now.toLocalDate().plusDays(offset.toLong())
                        ScheduleResolver.resolve(t, date.dayOfWeek.value).filterIsInstance<ScheduleBlock.Lesson>()
                            .firstOrNull { date.atStartOfDay().plusMinutes(it.time.start.toLong()) > after }
                            ?.let { UpcomingLesson(it, date.atStartOfDay().plusMinutes(it.time.start.toLong())) }
                    } }?.takeIf { !it.starts.toLocalDate().isAfter(endOfWeek) }
                    val secondary = if (current) summary?.next else upcoming
                    val showNext = width >= 280 && height >= 145 && secondary != null && summary?.status != WeekStatus.DONE
                    views.setViewVisibility(R.id.widget_next, if (showNext) View.VISIBLE else View.GONE)
                    if (showNext && secondary != null) {
                        val nextAccent = secondary.lesson.accent(scheme.primary)
                        views.setImageViewBitmap(R.id.widget_next_background, widgetShape(264, height * 2, lerp(scheme.surfaceContainerHigh, nextAccent, if (dark) .24f else .45f).toArgb(), 40f, nextAccent.copy(alpha = .45f).toArgb()))
                        views.setImageViewBitmap(R.id.widget_next_icon, widgetIcon(secondary.lesson.symbol() ?: Icons.Outlined.CalendarMonth, nextAccent.toArgb()))
                        views.setViewVisibility(R.id.widget_next_icon, if (showIcon) View.VISIBLE else View.GONE)
                        views.setTextViewText(R.id.widget_next_label, if (current) "UP NEXT" else "AFTER THAT")
                        views.setTextViewText(R.id.widget_next_title, secondary.lesson.subject.name)
                        views.setTextViewText(R.id.widget_next_detail, secondary.starts.format(DateTimeFormatter.ofPattern("EEE HH:mm")) + if (settings.showLocation && secondary.lesson.location.isNotBlank()) " · ${secondary.lesson.location}" else "")
                        listOf(R.id.widget_next_label, R.id.widget_next_title, R.id.widget_next_detail).forEach { views.setTextColor(it, foreground.toArgb()) }
                    }
                }
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

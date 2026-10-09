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
                val provider = manager.getAppWidgetInfo(id)?.provider?.className
                val pill = provider == PillWidget::class.java.name
                val wide = provider == WideWidget::class.java.name
                val views = RemoteViews(context.packageName, when { pill -> R.layout.tempo_widget_pill; wide -> R.layout.tempo_widget_wide; else -> R.layout.tempo_widget })
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
                val outlined = settings.outlined ?: (block != null && (if (block is ScheduleBlock.Break) appearance.effectiveBreakStyle else appearance.blockStyle) == BlockStyle.OUTLINED)
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
                val primaryDate = if (current) now.toLocalDate() else summary?.next?.starts?.toLocalDate()
                val whenText = block?.let {
                    val day = if (primaryDate != now.toLocalDate()) primaryDate?.format(DateTimeFormatter.ofPattern("EEE"))?.plus(" · ") ?: "" else ""
                    "$day${minuteLabel(it.time.start)}–${minuteLabel(it.time.end)}"
                } ?: ""
                val location = if (settings.showLocation) lesson?.location?.takeIf { it.isNotBlank() } else null
                val detail = when {
                    timetable == null -> "Choose another timetable in widget settings"
                    !timetable.onboarded -> "Open Tempo to finish setup"
                    summary?.status == WeekStatus.DONE -> "Nothing else scheduled this week"
                    summary?.status == WeekStatus.EMPTY -> "Your week is clear"
                    else -> listOfNotNull(whenText.takeIf { it.isNotBlank() }, location).joinToString(" · ")
                }
                val showIcon = settings.showIcons ?: appearance.showIcons
                val iconColour = if (dark) accent else lerp(Color.Black, accent, .45f)
                val icon = when (summary?.status) {
                    WeekStatus.DONE -> Icons.Outlined.CheckCircle
                    WeekStatus.EMPTY -> Icons.Outlined.EventAvailable
                    else -> block?.symbol() ?: Icons.Outlined.CalendarMonth
                }
                // The pill follows the selected theme by default; lesson colour is an explicit choice.
                val bg = when { pill && !settings.lessonBackground -> scheme.background; wide -> scheme.surfaceContainerHigh; else -> blockColour }
                val border = if (outlined && (!pill || settings.lessonBackground) && !wide) accent.copy(alpha = .65f).toArgb() else null
                views.setImageViewBitmap(R.id.widget_background, widgetShape(width * 2, height * 2, bg.toArgb(), if (pill) height.toFloat() else 48f, border))
                if (wide) views.setImageViewBitmap(R.id.widget_block_background, widgetShape(width, height * 2, blockColour.toArgb(), 36f, if (outlined) accent.copy(alpha = .65f).toArgb() else null))
                views.setViewVisibility(R.id.widget_icon, if (showIcon) View.VISIBLE else View.GONE)
                views.setImageViewBitmap(R.id.widget_icon, widgetIcon(icon, iconColour.toArgb()))
                listOf(R.id.widget_title, R.id.widget_detail, R.id.widget_timer, R.id.widget_time).forEach { views.setTextColor(it, foreground.toArgb()) }
                views.setTextViewText(R.id.widget_title, title)
                views.setTextViewText(R.id.widget_detail, detail)
                views.setInt(R.id.widget_detail, "setMaxLines", if (pill || height < 150) 1 else 2)
                views.setInt(R.id.widget_title, "setMaxLines", if (pill || height < 150) 1 else 2)
                views.setTextViewTextSize(R.id.widget_title, android.util.TypedValue.COMPLEX_UNIT_SP, if (pill) 14f else if (height < 150) 16f else 20f)
                views.setTextViewTextSize(R.id.widget_timer, android.util.TypedValue.COMPLEX_UNIT_SP, if (pill) 12f else if (height < 150) 18f else 22f)
                views.setViewVisibility(R.id.widget_timer, if (ticking) View.VISIBLE else View.GONE)
                views.setChronometerCountDown(R.id.widget_timer, true)
                views.setChronometer(R.id.widget_timer, SystemClock.elapsedRealtime() + (until ?: 0), if (current) "%s left" else "In %s", ticking)
                views.setViewVisibility(R.id.widget_detail, if (pill && ticking) View.GONE else View.VISIBLE)
                // Distant activities already show their day/time in the detail; no duplicate large clock.
                views.setViewVisibility(R.id.widget_time, View.GONE)
                if (wide) {
                    val plan = if (timetable != null && summary != null) widgetDayPlan(timetable, now.toLocalDateTime(), summary) else null
                    views.removeAllViews(R.id.widget_agenda)
                    views.setViewVisibility(R.id.widget_agenda_panel, if (plan != null) View.VISIBLE else View.GONE)
                    if (plan != null) {
                        val rowCount = if (height >= 180) 3 else if (height >= 100) 2 else 1
                        val later = plan.later.take(rowCount)
                        val hidden = plan.later.size - later.size
                        views.setViewVisibility(R.id.widget_agenda_label, if (height >= 150) View.VISIBLE else View.GONE)
                        views.setTextViewText(R.id.widget_agenda_label, if (plan.date == now.toLocalDate()) "LATER TODAY" else plan.date.format(DateTimeFormatter.ofPattern("EEEE")).uppercase())
                        views.setTextColor(R.id.widget_agenda_label, scheme.onSurfaceVariant.toArgb())
                        val finish = "Finish ${minuteLabel(plan.finish)}"
                        val count = "${plan.lessonsLeft} left"
                        views.setTextViewText(R.id.widget_day_summary, "$finish · $count")
                        views.setTextColor(R.id.widget_day_summary, scheme.onSurfaceVariant.toArgb())
                        fun addRow(activity: ScheduleBlock?, overflow: Int = 0) {
                            val row = RemoteViews(context.packageName, R.layout.tempo_widget_agenda_row)
                            val colour = activity?.accent(scheme.primary) ?: scheme.primary
                            val rowOutlined = settings.outlined ?: (activity != null && (if (activity is ScheduleBlock.Break) appearance.effectiveBreakStyle else appearance.blockStyle) == BlockStyle.OUTLINED)
                            val fill = if (rowOutlined) scheme.surfaceContainerHigh else lerp(scheme.surfaceContainerHigh, colour, if (dark) .30f else .55f)
                            row.setImageViewBitmap(R.id.widget_row_background, widgetShape(320, 96, fill.toArgb(), 24f, if (rowOutlined) colour.copy(alpha = .65f).toArgb() else null))
                            row.setViewVisibility(R.id.widget_row_icon, if (showIcon) View.VISIBLE else View.GONE)
                            row.setImageViewBitmap(R.id.widget_row_icon, widgetIcon(activity?.symbol() ?: Icons.Outlined.CheckCircle, (if (dark) colour else lerp(Color.Black, colour, .45f)).toArgb()))
                            row.setTextViewText(R.id.widget_row_title, activity?.title() ?: "Last period")
                            val room = if (settings.showLocation) (activity as? ScheduleBlock.Lesson)?.location?.takeIf { it.isNotBlank() } else null
                            val time = activity?.let { "${minuteLabel(it.time.start)}–${minuteLabel(it.time.end)}" }
                            val extra = if (overflow > 0) "+$overflow more" else room
                            row.setTextViewText(R.id.widget_row_detail, if (activity == null) "Nothing after this today" else listOfNotNull(time, extra).joinToString(" · "))
                            listOf(R.id.widget_row_title, R.id.widget_row_detail).forEach { row.setTextColor(it, foreground.toArgb()) }
                            views.addView(R.id.widget_agenda, row)
                        }
                        if (later.isEmpty()) addRow(null) else later.forEachIndexed { index, activity -> addRow(activity, if (index == later.lastIndex) hidden else 0) }
                    }
                }
                val open = Intent(context, MainActivity::class.java).putExtra("widgetTimetableId", settings.timetableId)
                views.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(context, id, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                manager.updateAppWidget(id, views)
            }
            alarm.setAndAllowWhileIdle(AlarmManager.RTC, nextRefresh.coerceAtLeast(System.currentTimeMillis() + 1000), refresh)
            Result.success()
        } catch (_: Exception) { Result.retry() }
    }
}

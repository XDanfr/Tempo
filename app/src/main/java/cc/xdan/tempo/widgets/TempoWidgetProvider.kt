package cc.xdan.tempo.widgets

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.*
import androidx.compose.ui.graphics.Color
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
                val appearance = timetable?.appearance ?: Appearance()
                val selected = settings.preset ?: appearance.selectedPreset
                val mode = if (settings.preset == null) appearance.mode else settings.mode
                val dark = mode == ThemeMode.DARK || (mode == ThemeMode.SYSTEM && (context.resources.configuration.uiMode and 0x30) == 0x20)
                val scheme = if (selected == ThemePreset.MATERIAL_YOU && android.os.Build.VERSION.SDK_INT >= 31) {
                    if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                } else presetColorScheme(selected, dark)
                val options = manager.getAppWidgetOptions(id)
                val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 90).coerceIn(56, 400)
                val summary = timetable?.takeIf { it.onboarded }?.let { weekOverview(it, now.toLocalDateTime()) }
                summary?.boundary?.atZone(now.zone)?.let { nextRefresh = minOf(nextRefresh, it.toInstant().toEpochMilli()) }
                val dayComplete = timetable != null && summary != null && widgetDayComplete(timetable, now.toLocalDateTime(), summary)
                val messageOnly = !pill && !wide && dayComplete && settings.doneForDayMessage
                val plan = if (wide && timetable != null && summary != null) widgetDayPlan(timetable, now.toLocalDateTime(), summary) else null
                val agendaLayout = plan?.later?.isNotEmpty() == true
                val views = RemoteViews(context.packageName, when { pill -> R.layout.tempo_widget_pill; agendaLayout -> R.layout.tempo_widget_wide; else -> R.layout.tempo_widget })
                val block = summary?.block.takeUnless { messageOnly }
                val accent = block?.accent(scheme.primary) ?: scheme.primary
                val outlined = settings.outlined ?: (block != null && (if (block is ScheduleBlock.Break) appearance.effectiveBreakStyle else appearance.blockStyle) == BlockStyle.OUTLINED)
                val themeBackground = widgetThemeBackground(scheme, dark)
                val blockColour = if (outlined || block == null) themeBackground else accent
                val background = if ((!wide && !settings.lessonBackground) || block == null) themeBackground else blockColour
                val foreground = widgetForeground(if (agendaLayout) blockColour else background)
                val title = when {
                    timetable == null -> "Choose a timetable"
                    !timetable.onboarded -> "Finish setup"
                    messageOnly -> "Done for today!"
                    summary?.status == WeekStatus.DONE -> "All done"
                    summary?.status == WeekStatus.EMPTY -> settings.emptyText
                    else -> block?.title() ?: settings.emptyText
                }
                val current = summary?.status == WeekStatus.CURRENT
                val lesson = block as? ScheduleBlock.Lesson
                val target = if (current && block != null) now.toLocalDate().atStartOfDay().plusMinutes(block.time.end.toLong()).atZone(now.zone)
                    else summary?.next?.starts?.atZone(now.zone)
                val until = target?.toInstant()?.toEpochMilli()?.minus(now.toInstant().toEpochMilli())
                val ticking = !messageOnly && until != null && until in 1..604_800_000
                val primaryDate = if (current) now.toLocalDate() else summary?.next?.starts?.toLocalDate()
                val whenText = block?.let {
                    val day = if (primaryDate != now.toLocalDate()) primaryDate?.format(DateTimeFormatter.ofPattern("EEE"))?.plus(" · ") ?: "" else ""
                    "$day${minuteLabel(it.time.start)}–${minuteLabel(it.time.end)}"
                } ?: ""
                val location = if (settings.showLocation) lesson?.location?.takeIf { it.isNotBlank() } else null
                val detail = when {
                    timetable == null -> "Choose another timetable in widget settings"
                    !timetable.onboarded -> "Open Tempo to finish setup"
                    messageOnly -> "Nothing else scheduled today"
                    summary?.status == WeekStatus.DONE -> "Nothing else scheduled this week"
                    summary?.status == WeekStatus.EMPTY -> "Your week is clear"
                    else -> listOfNotNull(whenText.takeIf { it.isNotBlank() }, location).joinToString(" · ")
                }
                val showIcon = settings.showIcons ?: appearance.showIcons
                val iconColour = foreground
                val icon = if (messageOnly) Icons.Outlined.CheckCircle else when (summary?.status) {
                    WeekStatus.DONE -> Icons.Outlined.CheckCircle
                    WeekStatus.EMPTY -> Icons.Outlined.EventAvailable
                    else -> block?.symbol() ?: Icons.Outlined.CalendarMonth
                }
                // Native drawables retain their corner radius/stroke at the actual launcher bounds.
                fun surface(target: RemoteViews, fillId: Int, outlineId: Int, fill: Color, colour: Color, border: Boolean) {
                    target.setInt(fillId, "setColorFilter", fill.toArgb())
                    target.setInt(outlineId, "setColorFilter", widgetOutline(colour, fill).toArgb())
                    target.setViewVisibility(outlineId, if (border) View.VISIBLE else View.GONE)
                }
                surface(views, R.id.widget_background, R.id.widget_outline, if (agendaLayout) themeBackground else background, scheme.primaryFixed, settings.showOutline)
                if (agendaLayout) surface(views, R.id.widget_block_background, R.id.widget_block_outline, blockColour, accent, settings.showOutline)
                views.setViewVisibility(R.id.widget_icon, if (showIcon) View.VISIBLE else View.GONE)
                views.setImageViewBitmap(R.id.widget_icon, widgetIcon(icon, iconColour.toArgb()))
                listOf(R.id.widget_title, R.id.widget_detail, R.id.widget_timer).forEach { views.setTextColor(it, foreground.toArgb()) }
                views.setTextViewText(R.id.widget_title, title)
                views.setTextViewText(R.id.widget_detail, detail)
                val compact = height < 150 || dayComplete && height < 180
                views.setInt(R.id.widget_detail, "setMaxLines", if (pill || compact) 1 else 2)
                views.setInt(R.id.widget_title, "setMaxLines", if (pill || compact) 1 else 2)
                views.setTextViewTextSize(R.id.widget_title, android.util.TypedValue.COMPLEX_UNIT_SP, if (pill) 14f else if (compact) 16f else 20f)
                views.setTextViewTextSize(R.id.widget_timer, android.util.TypedValue.COMPLEX_UNIT_SP, if (pill) 12f else if (compact) 18f else 22f)
                views.setViewVisibility(R.id.widget_timer, if (ticking) View.VISIBLE else View.GONE)
                views.setChronometerCountDown(R.id.widget_timer, true)
                views.setChronometer(R.id.widget_timer, SystemClock.elapsedRealtime() + (until ?: 0), if (current) "%s left" else "In %s", ticking)
                views.setViewVisibility(R.id.widget_detail, if (pill && ticking || !pill && dayComplete && !messageOnly && compact && ticking) View.GONE else View.VISIBLE)
                if (!pill) {
                    views.setTextColor(R.id.widget_day_done, foreground.toArgb())
                    views.setViewVisibility(R.id.widget_day_done, if (dayComplete && !messageOnly) View.VISIBLE else View.GONE)
                }
                if (agendaLayout && plan != null) {
                    views.removeAllViews(R.id.widget_agenda)
                    val rowCount = if (height >= 180) 3 else if (height >= 100) 2 else 1
                    val later = plan.later.take(rowCount)
                    val hidden = plan.later.size - later.size
                    val finish = "Finish ${minuteLabel(plan.finish)}"
                    val count = "${plan.lessonsLeft} left"
                    views.setTextViewText(R.id.widget_day_summary, "$finish · $count")
                    views.setTextColor(R.id.widget_day_summary, widgetForeground(themeBackground).toArgb())
                    fun addRow(activity: ScheduleBlock, overflow: Int = 0) {
                        val row = RemoteViews(context.packageName, R.layout.tempo_widget_agenda_row)
                        val colour = activity.accent(scheme.primary)
                        val rowOutlined = settings.outlined ?: ((if (activity is ScheduleBlock.Break) appearance.effectiveBreakStyle else appearance.blockStyle) == BlockStyle.OUTLINED)
                        val fill = if (rowOutlined) themeBackground else colour
                        val rowForeground = widgetForeground(fill)
                        surface(row, R.id.widget_row_background, R.id.widget_row_outline, fill, colour, settings.showOutline)
                        row.setViewVisibility(R.id.widget_row_icon, if (showIcon) View.VISIBLE else View.GONE)
                        row.setImageViewBitmap(R.id.widget_row_icon, widgetIcon(activity.symbol() ?: Icons.Outlined.CalendarMonth, rowForeground.toArgb()))
                        row.setTextViewText(R.id.widget_row_title, activity.title())
                        val room = if (settings.showLocation) (activity as? ScheduleBlock.Lesson)?.location?.takeIf { it.isNotBlank() } else null
                        val time = "${minuteLabel(activity.time.start)}–${minuteLabel(activity.time.end)}"
                        val extra = if (overflow > 0) "+$overflow more" else room
                        row.setTextViewText(R.id.widget_row_detail, listOfNotNull(time, extra).joinToString(" · "))
                        listOf(R.id.widget_row_title, R.id.widget_row_detail).forEach { row.setTextColor(it, rowForeground.toArgb()) }
                        views.addView(R.id.widget_agenda, row)
                    }
                    later.forEachIndexed { index, activity -> addRow(activity, if (index == later.lastIndex) hidden else 0) }
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

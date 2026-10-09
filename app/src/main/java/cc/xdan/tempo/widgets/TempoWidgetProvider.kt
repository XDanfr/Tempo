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
                val appearance = timetable?.appearance ?: Appearance()
                val selected = settings.preset ?: appearance.selectedPreset
                val mode = if (settings.preset == null) appearance.mode else settings.mode
                val dark = mode == ThemeMode.DARK || (mode == ThemeMode.SYSTEM && (context.resources.configuration.uiMode and 0x30) == 0x20)
                val scheme = if (selected == ThemePreset.MATERIAL_YOU && android.os.Build.VERSION.SDK_INT >= 31) {
                    if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                } else presetColorScheme(selected, dark)
                val options = manager.getAppWidgetOptions(id)
                val height = (options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 90) / context.resources.configuration.fontScale).toInt().coerceIn(56, 400)
                val summary = timetable?.takeIf { it.onboarded }?.let { weekOverview(it, now.toLocalDateTime()) }
                summary?.boundary?.atZone(now.zone)?.let { nextRefresh = minOf(nextRefresh, it.toInstant().toEpochMilli()) }
                val dayComplete = timetable != null && summary != null && widgetDayComplete(timetable, now.toLocalDateTime(), summary)
                val messageOnly = !pill && !wide && dayComplete && settings.doneForDayMessage
                val completion = !pill && dayComplete
                val plan = if (wide && !completion && timetable != null && summary != null) widgetDayPlan(timetable, now.toLocalDateTime(), summary) else null
                val agendaLayout = plan?.later?.isNotEmpty() == true
                val views = RemoteViews(context.packageName, when {
                    pill -> R.layout.tempo_widget_pill
                    completion -> R.layout.tempo_widget_done_today
                    agendaLayout -> R.layout.tempo_widget_wide
                    else -> R.layout.tempo_widget
                })
                val block = summary?.block
                val accent = block?.accent(scheme.primaryFixed) ?: scheme.primaryFixed
                val themeBackground = widgetThemeBackground(scheme, dark, pill)
                val blockColour = if (pill) accent else widgetPeriodBackground(scheme, accent, dark, prominent = true)
                val background = when {
                    completion || block == null -> themeBackground
                    wide || settings.lessonBackground -> blockColour
                    else -> themeBackground
                }
                val foreground = widgetForeground(if (agendaLayout) blockColour else background)
                val current = summary?.status == WeekStatus.CURRENT
                val lesson = block as? ScheduleBlock.Lesson
                val title = when {
                    timetable == null -> "Choose a timetable"
                    !timetable.onboarded -> "Finish setup"
                    completion -> "Done for today!"
                    summary?.status == WeekStatus.DONE -> "All done"
                    summary?.status == WeekStatus.EMPTY -> settings.emptyText
                    else -> block?.title() ?: settings.emptyText
                }
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
                    completion -> "Next: ${summary?.next?.lesson?.subject?.name.orEmpty()}"
                    summary?.status == WeekStatus.DONE -> "Nothing else scheduled this week"
                    summary?.status == WeekStatus.EMPTY -> "Your week is clear"
                    else -> whenText
                }
                val showIcon = settings.showIcons ?: appearance.showIcons
                val icon = when {
                    completion || summary?.status == WeekStatus.DONE -> Icons.Outlined.CheckCircle
                    summary?.status == WeekStatus.EMPTY -> Icons.Outlined.EventAvailable
                    else -> block?.symbol() ?: Icons.Outlined.CalendarMonth
                }
                fun surface(target: RemoteViews, fillId: Int, outlineId: Int, fill: Color, colour: Color, border: Boolean) {
                    val shape = when (fillId) {
                        R.id.widget_background -> if (pill) R.drawable.widget_shape_pill else R.drawable.widget_shape_surface
                        R.id.widget_block_background -> R.drawable.widget_shape_block
                        else -> R.drawable.widget_shape_block
                    }
                    val edge = when (outlineId) {
                        R.id.widget_outline -> if (pill) R.drawable.widget_shape_pill_outline else R.drawable.widget_shape_surface_outline
                        R.id.widget_block_outline -> R.drawable.widget_shape_block_outline
                        else -> R.drawable.widget_shape_block_outline
                    }
                    target.setImageViewResource(fillId, shape)
                    target.setImageViewResource(outlineId, edge)
                    target.setInt(fillId, "setColorFilter", fill.toArgb())
                    target.setInt(outlineId, "setColorFilter", widgetOutline(colour, fill).toArgb())
                    target.setViewVisibility(outlineId, if (border) View.VISIBLE else View.GONE)
                }
                surface(views, R.id.widget_background, R.id.widget_outline, if (agendaLayout) themeBackground else background, scheme.primaryFixed, !wide && settings.showOutline)
                if (agendaLayout) surface(views, R.id.widget_block_background, R.id.widget_block_outline, blockColour, accent, false)
                val showBadge = showIcon && (height >= (if (completion) 200 else 170) || pill)
                if (!pill) {
                    val badgeColour = if (dark) lerp(scheme.surfaceContainerHigh, accent, .60f) else lerp(accent, Color.White, .32f)
                    views.setInt(R.id.widget_badge_background, "setColorFilter", badgeColour.toArgb())
                    views.setViewVisibility(R.id.widget_badge, if (showBadge) View.VISIBLE else View.GONE)
                    views.setImageViewBitmap(R.id.widget_icon, widgetIcon(icon, widgetForeground(badgeColour).toArgb()))
                } else {
                    views.setViewVisibility(R.id.widget_icon, if (showIcon) View.VISIBLE else View.GONE)
                    views.setImageViewBitmap(R.id.widget_icon, widgetIcon(icon, foreground.toArgb()))
                }
                views.setTextColor(R.id.widget_title, foreground.toArgb())
                views.setTextViewText(R.id.widget_title, title)
                views.setInt(R.id.widget_title, "setMaxLines", if (pill || !completion && height < 150) 1 else 2)
                views.setTextViewTextSize(R.id.widget_title, android.util.TypedValue.COMPLEX_UNIT_SP, when { pill -> 14f; completion -> if (height < 140) 18f else 24f; height < 150 -> 18f; height < 200 -> 20f; else -> 22f })
                val chip = widgetPeriodBackground(scheme, accent, dark, prominent = false)
                val infoForeground = if (completion) widgetForeground(chip) else foreground
                val roomPill = !completion && !pill && location != null && (height >= 200 || !wide && !showBadge && height >= 150)
                views.setTextViewText(R.id.widget_detail, if (pill || !completion && !roomPill && location != null) listOfNotNull(whenText.takeIf { it.isNotBlank() }, location).joinToString(" · ") else detail)
                views.setInt(R.id.widget_detail, "setMaxLines", if (height < 200 || completion) 1 else 2)
                views.setTextColor(R.id.widget_detail, infoForeground.toArgb())
                views.setTextColor(R.id.widget_timer, infoForeground.toArgb())
                views.setTextViewTextSize(R.id.widget_detail, android.util.TypedValue.COMPLEX_UNIT_SP, if (pill) 11f else if (height < 140) 12f else 14f)
                views.setTextViewTextSize(R.id.widget_timer, android.util.TypedValue.COMPLEX_UNIT_SP, when { pill -> 12f; completion -> if (height < 140) 14f else 16f; height < 150 -> 20f; else -> 24f })
                views.setViewVisibility(R.id.widget_timer, if (ticking) View.VISIBLE else View.GONE)
                views.setChronometerCountDown(R.id.widget_timer, true)
                views.setChronometer(R.id.widget_timer, SystemClock.elapsedRealtime() + (until ?: 0), if (current) "%s left" else "In %s", ticking)
                views.setViewVisibility(R.id.widget_detail, if (pill && ticking) View.GONE else View.VISIBLE)
                if (completion) {
                    views.setInt(R.id.widget_next_pill_background, "setColorFilter", chip.toArgb())
                    views.setViewVisibility(R.id.widget_next_pill, if (messageOnly) View.GONE else View.VISIBLE)
                    views.setInt(R.id.widget_detail, "setMaxLines", 1)
                } else if (!pill) {
                    val roomColour = widgetPeriodBackground(scheme, accent, dark, prominent = false)
                    views.setInt(R.id.widget_location_background, "setColorFilter", roomColour.toArgb())
                    views.setTextColor(R.id.widget_location_text, widgetForeground(roomColour).toArgb())
                    views.setTextViewText(R.id.widget_location_text, location ?: "")
                    views.setViewVisibility(R.id.widget_location, if (roomPill) View.VISIBLE else View.GONE)
                }
                if (agendaLayout && plan != null) {
                    views.removeAllViews(R.id.widget_agenda)
                    val later = plan.later.take(if (height >= 190) 2 else 1)
                    val hidden = plan.later.size - later.size
                    views.setTextViewText(R.id.widget_day_summary, "Finish ${minuteLabel(plan.finish)} · ${plan.lessonsLeft} left")
                    views.setTextColor(R.id.widget_day_summary, widgetForeground(themeBackground).toArgb())
                    later.forEachIndexed { index, activity ->
                        val row = RemoteViews(context.packageName, R.layout.tempo_widget_agenda_row)
                        val colour = activity.accent(scheme.primaryFixed)
                        val fill = widgetPeriodBackground(scheme, colour, dark, prominent = false)
                        val rowForeground = widgetForeground(fill)
                        surface(row, R.id.widget_row_background, R.id.widget_row_outline, fill, colour, false)
                        row.setTextViewText(R.id.widget_row_title, activity.title())
                        val room = if (settings.showLocation) (activity as? ScheduleBlock.Lesson)?.location?.takeIf { it.isNotBlank() } else null
                        val overflow = if (index == later.lastIndex && hidden > 0) "+$hidden more" else room
                        row.setTextViewText(R.id.widget_row_detail, listOfNotNull("${minuteLabel(activity.time.start)}–${minuteLabel(activity.time.end)}", overflow).joinToString(" · "))
                        listOf(R.id.widget_row_title, R.id.widget_row_detail).forEach { row.setTextColor(it, rowForeground.toArgb()) }
                        views.addView(R.id.widget_agenda, row)
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

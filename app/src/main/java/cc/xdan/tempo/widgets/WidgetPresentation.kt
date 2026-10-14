package cc.xdan.tempo.widgets

import cc.xdan.tempo.model.*
import cc.xdan.tempo.schedule.*
import java.time.ZonedDateTime

internal enum class WidgetState { MISSING, SETUP, CURRENT, BREAK, UPCOMING, DONE_TODAY, WEEK_DONE, EMPTY }

/** Immutable schedule snapshot. Contains no Android views, clocks, colours or callbacks. */
internal data class WidgetPresentation(
    val state: WidgetState,
    val summary: WeekOverview?,
    val dayComplete: Boolean,
    val title: String,
    val countdownLabel: String,
    val targetEpochMillis: Long?,
    val boundaryEpochMillis: Long?,
    val progress: Float?,
    val location: String?,
    val plan: WidgetDayPlan?,
) {
    val block: ScheduleBlock? get() = summary?.block
    val current: Boolean get() = summary?.status == WeekStatus.CURRENT
}

/** Resolve once per widget update, before rendering each host-reported size. */
internal fun widgetPresentation(timetable: Timetable?, settings: WidgetSettings, now: ZonedDateTime): WidgetPresentation {
    val local = now.toLocalDateTime()
    val summary = timetable?.takeIf { it.onboarded }?.let { weekOverview(it, local) }
    val complete = timetable != null && summary != null && widgetDayComplete(timetable, local, summary)
    val current = summary?.status == WeekStatus.CURRENT
    val block = summary?.block
    val state = when {
        timetable == null -> WidgetState.MISSING
        !timetable.onboarded -> WidgetState.SETUP
        summary?.status == WeekStatus.DONE -> WidgetState.WEEK_DONE
        summary?.status == WeekStatus.EMPTY -> WidgetState.EMPTY
        complete && settings.doneForDayMessage -> WidgetState.DONE_TODAY
        current && block is ScheduleBlock.Break -> WidgetState.BREAK
        current -> WidgetState.CURRENT
        else -> WidgetState.UPCOMING
    }
    val target = if (current && block != null) local.toLocalDate().atStartOfDay().plusMinutes(block.time.end.toLong()) else summary?.next?.starts
    val terminal = state in listOf(WidgetState.MISSING, WidgetState.SETUP, WidgetState.WEEK_DONE, WidgetState.EMPTY, WidgetState.DONE_TODAY)
    val title = when (state) {
        WidgetState.MISSING -> "Choose a timetable"
        WidgetState.SETUP -> "Finish setup"
        WidgetState.WEEK_DONE -> "All done"
        WidgetState.DONE_TODAY -> "Done for today"
        WidgetState.EMPTY -> settings.emptyText
        else -> when (block) {
            is ScheduleBlock.Lesson -> block.subject.name
            is ScheduleBlock.Break -> block.scheduled.name
            else -> settings.emptyText
        }
    }
    val elapsed = block?.let { local.hour * 60 + local.minute + local.second / 60f - it.time.start }
    return WidgetPresentation(state, summary, complete, title, if (current) "Time left" else "Starts in",
        target?.takeUnless { terminal }?.atZone(now.zone)?.toInstant()?.toEpochMilli(),
        summary?.boundary?.atZone(now.zone)?.toInstant()?.toEpochMilli(),
        if (current && block != null && elapsed != null) (elapsed / block.time.minutes).coerceIn(0f, 1f) else null,
        (block as? ScheduleBlock.Lesson)?.location?.takeIf { timetable != null && settings.showsLocations(timetable.appearance) && it.isNotBlank() },
        if (!terminal && !complete && timetable != null && summary != null) widgetDayPlan(timetable, local, summary) else null)
}

package cc.xdan.tempo.schedule

import cc.xdan.tempo.model.*

/** Lengths in minutes for the gaps between periods; null means that kind of gap isn't used. */
data class BreakLengths(val break_: Int? = 20, val lunch: Int? = 50, val changeover: Int? = 10)

/**
 * Suggests evenly sized periods with break, lunch and changeover gaps across a day.
 *
 * The first gap is the break, the middle gap is lunch and any others are changeovers, so four
 * periods from 09:00 to 16:20 with 20/50/10-minute gaps give 90-minute periods:
 * P1 09:00–10:30, Break, P2 10:50–12:20, Lunch, P3 13:10–14:40, Changeover, P4 14:50–16:20.
 * Period lengths round down to five minutes and the last period absorbs any remainder.
 * Returns null when the gaps leave no room for the periods.
 */
object PeriodPlanner {
    data class Plan(val periods: List<PeriodTemplate>, val breaks: List<ScheduledBreak>)

    fun gapKinds(count: Int, lengths: BreakLengths): List<Pair<BreakKind, Int>?> {
        val gaps = (count - 1).coerceAtLeast(0)
        val lunchAt = if (gaps >= 2) gaps / 2 else 0
        return List(gaps) { index ->
            when {
                gaps == 1 -> (lengths.lunch?.let { BreakKind.LUNCH to it }) ?: lengths.break_?.let { BreakKind.BREAK to it }
                index == lunchAt -> lengths.lunch?.let { BreakKind.LUNCH to it }
                index == 0 -> lengths.break_?.let { BreakKind.BREAK to it }
                else -> lengths.changeover?.let { BreakKind.CHANGEOVER to it }
            }
        }
    }

    fun plan(day: TimeSpan, count: Int, lengths: BreakLengths, days: List<Int>): Plan? {
        if (count < 1) return null
        val gaps = gapKinds(count, lengths)
        val available = day.minutes - gaps.sumOf { it?.second ?: 0 }
        val length = (available / count) / 5 * 5
        if (length < 10) return null
        val periods = mutableListOf<PeriodTemplate>()
        val breaks = mutableListOf<ScheduledBreak>()
        var cursor = day.start
        for (index in 0 until count) {
            val end = if (index == count - 1) day.end else cursor + length
            periods += PeriodTemplate("p${index + 1}", "Period ${index + 1}", TimeSpan(cursor, end))
            cursor = end
            gaps.getOrNull(index)?.let { (kind, minutes) ->
                breaks += ScheduledBreak("${kind.name.lowercase()}-${index + 1}", kind.label(), days, TimeSpan(cursor, cursor + minutes), kind)
                cursor += minutes
            }
        }
        return Plan(periods, breaks)
    }

    private fun BreakKind.label() = when (this) { BreakKind.BREAK -> "Break"; BreakKind.LUNCH -> "Lunch"; BreakKind.CHANGEOVER -> "Changeover"; BreakKind.CUSTOM -> "Break" }
}

package cc.xdan.tempo.widgets

/** Round up so an event that has not started never reads “0m”. No per-second app work. */
internal fun widgetDuration(target: Long, now: Long): String {
    val minutes = ((target - now).coerceAtLeast(0) + 59_999) / 60_000
    val days = minutes / 1440
    val hours = minutes / 60 % 24
    return when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h ${minutes % 60}m"
        else -> "${minutes}m"
    }
}

/** Use the existing single inexact alarm; Android may defer it in idle/battery saving. */
internal fun WidgetPresentation.nextRefreshMillis(now: Long): Long? {
    val target = targetEpochMillis
    val labelChange = if (target != null && target > now) {
        val minutes = (target - now + 59_999) / 60_000
        val nextMinutes = if (minutes >= 1440) (minutes / 60) * 60 - 1 else minutes - 1
        target - nextMinutes * 60_000
    } else null
    return listOfNotNull(boundaryEpochMillis, labelChange).minOrNull()
}

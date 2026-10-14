package cc.xdan.tempo.widgets

/** A content budget, not a launcher grid. All values are in font-adjusted dp. */
internal data class WidgetLayoutPlan(val sizing: WidgetSizing) {
    val composition = sizing.composition
    val compactSplit = sizing.width >= 232 && sizing.height >= 56
    val inset = if (composition == WidgetComposition.COMPACT) 12 else 16
    val heroHeight = if (composition == WidgetComposition.EXPANDED) {
        if (sizing.stacked) 150 else 112
    } else sizing.height
    val badgeSize = minOf(sizing.width - 32, sizing.height - 112, 136).coerceAtLeast(44)
    val squareContext = sizing.height >= 238
    val wideContext = sizing.height >= 174
    val agendaRows = if (composition != WidgetComposition.EXPANDED) 0 else
        ((sizing.height - heroHeight - 48) / 56).coerceIn(1, 5)
}

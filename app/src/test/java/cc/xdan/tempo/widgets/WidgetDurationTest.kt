package cc.xdan.tempo.widgets

import org.junit.Assert.*
import org.junit.Test

class WidgetDurationTest {
    @Test fun durationsAreReadableAndRoundUpWithoutZeroBeforeStart() {
        assertEquals("1h 29m", widgetDuration(5_340_000, 0))
        assertEquals("1h 30m", widgetDuration(5_340_001, 0))
        assertEquals("1m", widgetDuration(1, 0))
        assertEquals("0m", widgetDuration(1, 2))
        assertEquals("1d 1h", widgetDuration(90_000_000, 0))
    }

    private fun presentation(state: WidgetState, target: Long?) = WidgetPresentation(
        state, null, false, "Tutor", "Starts in", target, target, null, null, null)

    @Test fun onlyUpcomingLabelsRequestMinuteRefreshesAndBoundariesWin() {
        assertEquals(40_000L, presentation(WidgetState.UPCOMING, 100_000).nextRefreshMillis(0))
        assertEquals(100_000L, presentation(WidgetState.CURRENT, 100_000).nextRefreshMillis(0))
        assertEquals(40_000L, presentation(WidgetState.UPCOMING, 100_000).nextRefreshMillis(40_000 - 1))
        assertEquals(100_000L, presentation(WidgetState.UPCOMING, 100_000).nextRefreshMillis(40_000))
        assertEquals(20_000L, presentation(WidgetState.UPCOMING, 100_000).copy(boundaryEpochMillis = 20_000).nextRefreshMillis(0))
        assertNull(presentation(WidgetState.EMPTY, null).nextRefreshMillis(0))
    }
}

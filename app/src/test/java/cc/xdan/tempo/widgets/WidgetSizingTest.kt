package cc.xdan.tempo.widgets

import org.junit.Assert.*
import org.junit.Test

class WidgetSizingTest {
    @Test fun narrowTallUsesStackedAgenda() {
        val size = WidgetSizing(160f, 360f, 1f)
        assertTrue(size.stacked)
        assertTrue(size.showAgenda)
        assertEquals(1, size.agendaRows)
        assertEquals(20f, size.textSize(32f, false, false))
    }

    @Test fun shortWideKeepsSingleReadablePanel() {
        val size = WidgetSizing(400f, 110f, 1f)
        assertFalse(size.showAgenda)
        assertEquals(16f, size.textSize(28f, false, false))
    }

    @Test fun largeTextReducesDensityBeforeAddingAgenda() {
        assertTrue(WidgetSizing(300f, 180f, 1f).showAgenda)
        assertFalse(WidgetSizing(300f, 180f, 1.5f).showAgenda)
        assertEquals(14f, WidgetSizing(140f, 56f, 1f).textSize(22f, true, false))
    }

    @Test fun wideAgendaAddsSecondRowOnlyWithRoom() {
        assertEquals(1, WidgetSizing(360f, 180f, 1f).agendaRows)
        assertEquals(2, WidgetSizing(360f, 240f, 1f).agendaRows)
        assertEquals(16f, WidgetSizing(280f, 180f, 1f).textSize(28f, false, true))
    }
}

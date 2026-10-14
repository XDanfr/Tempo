package cc.xdan.tempo.widgets

import org.junit.Assert.*
import org.junit.Test

class WidgetSizingTest {
    @Test fun canonicalCompositionUsesBothDimensionsAndFontScale() {
        assertEquals(WidgetComposition.COMPACT, WidgetSizing(400f, 110f, 1f).composition)
        assertEquals(WidgetComposition.SQUARE, WidgetSizing(180f, 180f, 1f).composition)
        assertEquals(WidgetComposition.WIDE, WidgetSizing(320f, 180f, 1f).composition)
        assertEquals(WidgetComposition.EXPANDED, WidgetSizing(180f, 320f, 1f).composition)
        assertEquals(WidgetComposition.EXPANDED, WidgetSizing(360f, 240f, 1f).composition)
        assertEquals(WidgetComposition.COMPACT, WidgetSizing(320f, 180f, 1.5f).composition)
    }
    @Test fun compactDisclosureShrinksWithLargeText() {
        assertTrue(WidgetSizing(280f, 90f, 1f).compactSplit)
        assertFalse(WidgetSizing(280f, 90f, 1.5f).compactSplit)
        assertFalse(WidgetSizing(140f, 56f, 1f).compactIcon)
        assertEquals(18f, WidgetSizing(140f, 56f, 1f).compactTimerSp)
    }

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

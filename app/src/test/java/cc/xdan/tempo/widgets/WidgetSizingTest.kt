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

    @Test fun narrowTallUsesStackedAgendaAndWideShortRemainsCompact() {
        val tall = WidgetLayoutPlan(WidgetSizing(160f, 360f, 1f))
        assertTrue(tall.sizing.stacked)
        assertEquals(2, tall.agendaRows)
        assertEquals(0, WidgetLayoutPlan(WidgetSizing(400f, 110f, 1f)).agendaRows)
    }

    @Test fun largeTextRemovesSecondaryContentBeforeItClips() {
        val normal = WidgetLayoutPlan(WidgetSizing(360f, 240f, 1f))
        val large = WidgetLayoutPlan(WidgetSizing(360f, 240f, 1.5f))
        assertEquals(WidgetComposition.EXPANDED, normal.composition)
        assertEquals(WidgetComposition.SQUARE, large.composition)
        assertEquals(0, large.agendaRows)
        assertTrue(WidgetLayoutPlan(WidgetSizing(280f, 88f, 1f)).compactSplit)
        assertFalse(WidgetLayoutPlan(WidgetSizing(280f, 88f, 1.5f)).compactSplit)
    }

    @Test fun expandedRowBudgetsFitAndGrowWithHeightWithoutScrolling() {
        for (width in listOf(180f, 280f, 360f, 600f)) {
            var previous = 0
            for (height in 136..700) {
                val plan = WidgetLayoutPlan(WidgetSizing(width, height.toFloat(), 1f))
                if (plan.composition == WidgetComposition.EXPANDED) {
                    assertTrue(plan.agendaRows in previous.coerceAtLeast(1)..5)
                    assertTrue("$width × $height", plan.heroHeight + 48 + plan.agendaRows * 56 <= height)
                    previous = plan.agendaRows
                }
            }
        }
    }
}

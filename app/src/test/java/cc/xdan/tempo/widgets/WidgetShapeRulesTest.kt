package cc.xdan.tempo.widgets

import org.junit.Assert.*
import org.junit.Test

class WidgetShapeRulesTest {
    @Test fun standaloneAndSingleMemberHaveEqualCorners() {
        val c = WidgetShapeRules.standalone()
        assertEquals(c.topLeft, c.topRight)
        assertEquals(c.topLeft, c.bottomLeft)
        assertEquals(c.topLeft, c.bottomRight)
        assertEquals(c, WidgetShapeRules.member(0, 1))
    }
    @Test fun horizontalPairMirrorsSharedAndOutsideEdges() {
        val a = WidgetShapeRules.member(0, 2)
        val b = WidgetShapeRules.member(1, 2)
        assertEquals(a.topRight, b.topLeft)
        assertEquals(a.bottomRight, b.bottomLeft)
        assertEquals(a.topLeft, b.topRight)
        assertEquals(a.bottomLeft, b.bottomRight)
        assertTrue(a.topLeft > a.topRight)
    }
    @Test fun middleMembersUseOnlyInnerCornersInEitherOrientation() {
        for (vertical in listOf(false, true)) for (count in 3..6) {
            for (index in 1 until count - 1) assertEquals(WidgetShapeRules.standalone(WidgetShapeRules.INNER), WidgetShapeRules.member(index, count, vertical))
        }
    }
    @Test fun verticalPairRotatesMirroredEdges() {
        val a = WidgetShapeRules.member(0, 2, true)
        val b = WidgetShapeRules.member(1, 2, true)
        assertEquals(a.bottomLeft, b.topLeft)
        assertEquals(a.bottomRight, b.topRight)
        assertEquals(a.topLeft, b.bottomLeft)
        assertEquals(a.topRight, b.bottomRight)
    }
}

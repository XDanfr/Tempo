package cc.xdan.tempo.widgets

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test

/** Protect the XML fallback from drifting away from the document/preview geometry contract. */
class WidgetShapeAssetsTest {
    private fun root(path: String) = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File("src/main/res/$path")).documentElement
    @Test fun drawableCornersAndDimensionsMatchSharedTokens() {
        val dims = root("values/widget_shapes.xml").getElementsByTagName("dimen")
        val values = (0 until dims.length).associate { index ->
            val node = dims.item(index)
            node.attributes.getNamedItem("name").nodeValue to node.textContent.removeSuffix("dp").toFloat()
        }
        assertEquals(WidgetShapeRules.OUTER, values["widget_corner_outer"])
        assertEquals(WidgetShapeRules.INNER, values["widget_corner_inner"])
        assertEquals(WidgetShapeRules.GAP, values["widget_segment_gap"])
        for (outline in listOf("", "_outline")) {
            val single = root("drawable/widget_compact_single$outline.xml").getElementsByTagName("corners").item(0)
            assertEquals("@dimen/widget_corner_outer", single.attributes.getNamedItem("android:radius").nodeValue)
            for ((index, side) in listOf("top", "middle", "bottom").withIndex()) {
                val corners = root("drawable/widget_group_$side$outline.xml").getElementsByTagName("corners").item(0)
                val rule = WidgetShapeRules.member(index, 3, vertical = true)
                for ((name, radius) in listOf("topLeft" to rule.topLeft, "topRight" to rule.topRight, "bottomRight" to rule.bottomRight, "bottomLeft" to rule.bottomLeft)) {
                    val token = corners.attributes.getNamedItem("android:${name}Radius").nodeValue.substringAfter("@dimen/")
                    assertEquals(radius, values[token])
                }
            }
            for ((index, side) in listOf("start", "end").withIndex()) {
                val corners = root("drawable/widget_compact_$side$outline.xml").getElementsByTagName("corners").item(0)
                val rule = WidgetShapeRules.member(index, 2)
                val expected = listOf("topLeft" to rule.topLeft, "topRight" to rule.topRight, "bottomRight" to rule.bottomRight, "bottomLeft" to rule.bottomLeft)
                for ((name, radius) in expected) {
                    val token = corners.attributes.getNamedItem("android:${name}Radius").nodeValue.substringAfter("@dimen/")
                    assertEquals(radius, values[token])
                }
            }
        }
    }
}

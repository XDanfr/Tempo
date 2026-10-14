package cc.xdan.tempo.widgets

/** Material corner tokens, in dp. Shared by widget drawables, previews and the isolated spike. */
internal object WidgetShapeRules {
    const val OUTER = 28f // extra large
    const val INNER = 4f // extra small
    const val GAP = 4f

    data class Corners(val topLeft: Float, val topRight: Float, val bottomRight: Float, val bottomLeft: Float) {
        fun radii(scale: Float = 1f): FloatArray = floatArrayOf(
            topLeft, topLeft, topRight, topRight, bottomRight, bottomRight, bottomLeft, bottomLeft
        ).map { it * scale }.toFloatArray()
    }

    fun standalone(radius: Float = OUTER) = Corners(radius, radius, radius, radius)

    /** Physical order: callers reverse member order for an RTL horizontal group. */
    fun member(index: Int, count: Int, vertical: Boolean = false): Corners {
        require(count > 0 && index in 0 until count)
        if (count == 1) return standalone()
        val start = if (index == 0) OUTER else INNER
        val end = if (index == count - 1) OUTER else INNER
        return if (vertical) Corners(start, start, end, end) else Corners(start, end, end, start)
    }
}

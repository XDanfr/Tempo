package cc.xdan.tempo.widgets

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.*

internal fun widgetIcon(vector: ImageVector, colour: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.scale(96 / vector.viewportWidth, 96 / vector.viewportHeight)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colour }
    fun draw(group: VectorGroup) {
        canvas.save()
        canvas.translate(group.translationX + group.pivotX, group.translationY + group.pivotY)
        canvas.rotate(group.rotation)
        canvas.scale(group.scaleX, group.scaleY)
        canvas.translate(-group.pivotX, -group.pivotY)
        if (group.clipPathData.isNotEmpty()) canvas.clipPath(PathParser().addPathNodes(group.clipPathData).toPath().asAndroidPath())
        for (index in 0 until group.size) {
            when (val node = group[index]) {
                is VectorGroup -> draw(node)
                is VectorPath -> {
                    val path = PathParser().addPathNodes(node.pathData).toPath().apply { fillType = node.pathFillType }
                    if (node.fill != null) {
                        paint.style = Paint.Style.FILL; paint.alpha = (node.fillAlpha * 255).toInt()
                        canvas.drawPath(path.asAndroidPath(), paint)
                    }
                    if (node.stroke != null) {
                        paint.style = Paint.Style.STROKE; paint.strokeWidth = node.strokeLineWidth; paint.alpha = (node.strokeAlpha * 255).toInt()
                        canvas.drawPath(path.asAndroidPath(), paint)
                    }
                }
            }
        }
        canvas.restore()
    }
    draw(vector.root)
    return bitmap
}

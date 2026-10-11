package cc.xdan.tempo.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import cc.xdan.tempo.core.designsystem.R
import cc.xdan.tempo.model.minuteLabel
import cc.xdan.tempo.schedule.ScheduleImagePlan

/** Measure every wrapped line before allocating the same bitmap used for preview and saving. */
fun renderSchedulePng(context: Context, plan: ScheduleImagePlan): Bitmap {
    val scale = plan.width / 1080f
    val margin = (40 * scale).toInt()
    val contentWidth = plan.width - margin * 2
    val font = ResourcesCompat.getFont(context, R.font.outfit)
    val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = font; color = Color.rgb(23, 33, 22) }
    fun layout(text: String, size: Float, width: Int = contentWidth): StaticLayout {
        textPaint.textSize = size * scale
        return StaticLayout.Builder.obtain(text, 0, text.length, TextPaint(textPaint), width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(false).build()
    }
    data class Element(val text: StaticLayout, val x: Int, val y: Int)
    data class Panel(val rect: RectF, val colour: Int?, val outlined: Boolean)
    val elements = mutableListOf<Element>()
    val panels = mutableListOf<Panel>()
    var y = margin
    fun add(text: String, size: Float, inset: Int = 0) {
        val textLayout = layout(text, size, contentWidth - inset * 2)
        elements += Element(textLayout, margin + inset, y)
        y += textLayout.height + (10 * scale).toInt()
        require(y <= 16000) { "This image is too tall. Export individual days or fewer details." }
    }
    add(plan.title, 52f)
    add(if (plan.days.size == 1) "Day timetable" else "Week timetable", 28f)
    for (day in plan.days) {
        y += (24 * scale).toInt()
        add("${weekdayNames[day.date.dayOfWeek.value - 1]} · ${day.date}", 36f)
        if (day.rows.isEmpty()) add("No scheduled activities", 26f)
        for (row in day.rows) {
            val top = y
            val inset = (24 * scale).toInt()
            y += inset
            add("${minuteLabel(row.time.start)}–${minuteLabel(row.time.end)} · ${row.title}", 32f, inset)
            if (row.location.isNotBlank()) add(row.location, 26f, inset)
            if (row.notes.isNotBlank()) add(row.notes, 24f, inset)
            y += (14 * scale).toInt()
            panels += Panel(RectF(margin.toFloat(), top.toFloat(), (plan.width - margin).toFloat(), y.toFloat()), row.colour?.toInt(), row.outlined)
            y += (12 * scale).toInt()
        }
    }
    y += margin
    require(y <= 16384 && plan.width.toLong() * y <= 16_000_000) { "This image is too large. Export individual days or fewer details." }
    val bitmap = Bitmap.createBitmap(plan.width, y, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.WHITE)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    for (panel in panels) {
        paint.style = if (panel.outlined) Paint.Style.STROKE else Paint.Style.FILL
        paint.strokeWidth = 2 * scale
        paint.color = if (panel.outlined) Color.rgb(100, 110, 100)
            else ColorUtils.blendARGB(Color.WHITE, (panel.colour ?: Color.LTGRAY) or (0xFF shl 24), .3f)
        canvas.drawRoundRect(panel.rect, 24 * scale, 24 * scale, paint)
    }
    for (element in elements) {
        canvas.save(); canvas.translate(element.x.toFloat(), element.y.toFloat())
        element.text.draw(canvas); canvas.restore()
    }
    return bitmap
}

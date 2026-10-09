package cc.xdan.tempo.widgets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

internal enum class WidgetPreview { THEME, LESSON, NO_OUTLINE, OUTLINE, NEXT, DONE }

@Composable
internal fun WidgetOptionCards(title: String, selected: Boolean, first: String, second: String,
    firstPreview: WidgetPreview, secondPreview: WidgetPreview, pill: Boolean, select: (Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(false to first, true to second).forEach { (value, label) ->
                Surface(Modifier.weight(1f).selectable(selected == value, role = Role.RadioButton, onClick = { select(value) }),
                    shape = MaterialTheme.shapes.large,
                    color = if (selected == value) scheme.secondaryContainer else scheme.surfaceContainer,
                    border = BorderStroke(if (selected == value) 2.dp else 1.dp, if (selected == value) scheme.primary else scheme.outlineVariant)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val preview = if (value) secondPreview else firstPreview
                        Canvas(Modifier.fillMaxWidth().height(if (pill) 48.dp else 72.dp)) {
                            inset(horizontal = if (pill) 0f else (size.width - minOf(size.width, size.height)) / 2) {
                                val radius = CornerRadius(if (pill) size.height / 2 else 14.dp.toPx())
                                val lesson = scheme.tertiaryContainer
                                val background = if (preview == WidgetPreview.LESSON) lesson else scheme.primaryContainer
                                val edge = preview == WidgetPreview.OUTLINE
                                drawRoundRect(background, cornerRadius = radius)
                                if (edge) drawRoundRect(scheme.primary, cornerRadius = radius, style = Stroke(2.dp.toPx()))
                                val ink = scheme.onPrimaryContainer
                                val x = size.width * .16f
                                if (preview == WidgetPreview.DONE || preview == WidgetPreview.NEXT) {
                                    drawCircle(ink, 5.dp.toPx(), Offset(size.width * .5f, size.height * .24f))
                                    drawRoundRect(ink, Offset(x, size.height * .42f), Size(size.width * .68f, 4.dp.toPx()), CornerRadius(4f))
                                    if (preview == WidgetPreview.NEXT) {
                                        drawRoundRect(scheme.secondaryContainer, Offset(x, size.height * .65f), Size(size.width * .68f, size.height * .23f), CornerRadius(size.height * .12f))
                                        drawRoundRect(scheme.onSecondaryContainer, Offset(size.width * .28f, size.height * .75f), Size(size.width * .44f, 3.dp.toPx()), CornerRadius(3f))
                                    }
                                } else if (pill) {
                                    drawCircle(lerp(background, ink, .20f), size.height * .16f, Offset(size.width * .23f, size.height * .5f))
                                    drawRoundRect(ink, Offset(size.width * .42f, size.height * .35f), Size(size.width * .40f, 4.dp.toPx()), CornerRadius(4f))
                                    drawRoundRect(ink.copy(alpha = .60f), Offset(size.width * .42f, size.height * .58f), Size(size.width * .27f, 3.dp.toPx()), CornerRadius(3f))
                                } else {
                                    drawCircle(lerp(background, ink, .20f), size.height * .14f, Offset(size.width * .5f, size.height * .25f))
                                    drawRoundRect(ink, Offset(size.width * .18f, size.height * .48f), Size(size.width * .64f, 4.dp.toPx()), CornerRadius(4f))
                                    drawRoundRect(ink.copy(alpha = .55f), Offset(size.width * .30f, size.height * .65f), Size(size.width * .40f, 3.dp.toPx()), CornerRadius(3f))
                                    drawRoundRect(ink, Offset(size.width * .25f, size.height * .80f), Size(size.width * .50f, 4.dp.toPx()), CornerRadius(4f))
                                }
                            }
                        }
                        Text(label, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

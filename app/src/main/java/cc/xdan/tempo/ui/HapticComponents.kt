package cc.xdan.tempo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ChipElevation
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cc.xdan.tempo.design.rememberTempoHaptics
import androidx.compose.material3.Button as M3Button
import androidx.compose.material3.Card as M3Card
import androidx.compose.material3.Checkbox as M3Checkbox
import androidx.compose.material3.DropdownMenuItem as M3DropdownMenuItem
import androidx.compose.material3.ElevatedCard as M3ElevatedCard
import androidx.compose.material3.FilledTonalButton as M3FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton as M3FilledTonalIconButton
import androidx.compose.material3.FilterChip as M3FilterChip
import androidx.compose.material3.IconButton as M3IconButton
import androidx.compose.material3.OutlinedButton as M3OutlinedButton
import androidx.compose.material3.Surface as M3Surface
import androidx.compose.material3.Switch as M3Switch
import androidx.compose.material3.TextButton as M3TextButton

/*
 * Haptic layer for Tempo's interactive Material components.
 *
 * Material 3 components draw their own ripple and offer no press hook, so these wrappers keep
 * the exact Material signatures and add one haptic per interaction. Kotlin resolves names
 * declared in this package before star imports, so every screen in `cc.xdan.tempo.ui` that
 * imports `androidx.compose.material3.*` gets haptics automatically. A file that imports one of
 * these components explicitly from Material opts out.
 *
 * Kinds: buttons, cards and menu items give a light press; switches and checkboxes give a
 * toggle; chips give a selection tick. Committed edits add a confirmation via FeedbackHost.
 */

@Composable
fun Button(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(), elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null, contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null, content: @Composable RowScope.() -> Unit) {
    val haptics = rememberTempoHaptics()
    M3Button({ haptics.press(); onClick() }, modifier, enabled, shape, colors, elevation, border, contentPadding, interactionSource, content)
}

@Composable
fun FilledTonalButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, shape: Shape = ButtonDefaults.filledTonalShape,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(), elevation: ButtonElevation? = ButtonDefaults.filledTonalButtonElevation(),
    border: BorderStroke? = null, contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null, content: @Composable RowScope.() -> Unit) {
    val haptics = rememberTempoHaptics()
    M3FilledTonalButton({ haptics.press(); onClick() }, modifier, enabled, shape, colors, elevation, border, contentPadding, interactionSource, content)
}

@Composable
fun OutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, shape: Shape = ButtonDefaults.outlinedShape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(), elevation: ButtonElevation? = null,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled), contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null, content: @Composable RowScope.() -> Unit) {
    val haptics = rememberTempoHaptics()
    M3OutlinedButton({ haptics.press(); onClick() }, modifier, enabled, shape, colors, elevation, border, contentPadding, interactionSource, content)
}

@Composable
fun TextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, shape: Shape = ButtonDefaults.textShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(), elevation: ButtonElevation? = null, border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding, interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit) {
    val haptics = rememberTempoHaptics()
    M3TextButton({ haptics.press(); onClick() }, modifier, enabled, shape, colors, elevation, border, contentPadding, interactionSource, content)
}

@Composable
fun IconButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(), interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit) {
    val haptics = rememberTempoHaptics()
    M3IconButton({ haptics.press(); onClick() }, modifier, enabled, colors, interactionSource, content = content)
}

@Composable
fun FilledTonalIconButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: Shape = IconButtonDefaults.filledShape, colors: IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors(),
    interactionSource: MutableInteractionSource? = null, content: @Composable () -> Unit) {
    val haptics = rememberTempoHaptics()
    M3FilledTonalIconButton({ haptics.press(); onClick() }, modifier, enabled, shape, colors, interactionSource, content)
}

@Composable
fun Switch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null, enabled: Boolean = true, colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource? = null) {
    val haptics = rememberTempoHaptics()
    M3Switch(checked, onCheckedChange?.let { change -> { value: Boolean -> haptics.toggle(value); change(value) } },
        modifier, thumbContent, enabled, colors, interactionSource)
}

@Composable
fun Checkbox(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true,
    colors: CheckboxColors = CheckboxDefaults.colors(), interactionSource: MutableInteractionSource? = null) {
    val haptics = rememberTempoHaptics()
    M3Checkbox(checked, onCheckedChange?.let { change -> { value: Boolean -> haptics.toggle(value); change(value) } },
        modifier, enabled, colors, interactionSource)
}

@Composable
fun FilterChip(selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, leadingIcon: (@Composable () -> Unit)? = null, trailingIcon: (@Composable () -> Unit)? = null,
    shape: Shape = FilterChipDefaults.shape, colors: SelectableChipColors = FilterChipDefaults.filterChipColors(),
    elevation: SelectableChipElevationOrNull = FilterChipDefaults.filterChipElevation(),
    border: BorderStroke? = FilterChipDefaults.filterChipBorder(enabled, selected), interactionSource: MutableInteractionSource? = null) {
    val haptics = rememberTempoHaptics()
    M3FilterChip(selected, { haptics.tick(); onClick() }, label, modifier, enabled, leadingIcon, trailingIcon, shape, colors, elevation, border, interactionSource = interactionSource)
}

private typealias SelectableChipElevationOrNull = androidx.compose.material3.SelectableChipElevation?

@Composable
fun DropdownMenuItem(text: @Composable () -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null, trailingIcon: (@Composable () -> Unit)? = null, enabled: Boolean = true,
    colors: MenuItemColors = MenuDefaults.itemColors(), contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding,
    interactionSource: MutableInteractionSource? = null) {
    val haptics = rememberTempoHaptics()
    M3DropdownMenuItem(text, { haptics.press(); onClick() }, modifier, leadingIcon, trailingIcon, enabled, colors, contentPadding, interactionSource)
}

@Composable
fun Card(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(), elevation: CardElevation = CardDefaults.cardElevation(), border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null, content: @Composable ColumnScope.() -> Unit) {
    val haptics = rememberTempoHaptics()
    M3Card({ haptics.press(); onClick() }, modifier, enabled, shape, colors, elevation, border, interactionSource, content)
}

@Composable
fun ElevatedCard(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, shape: Shape = CardDefaults.elevatedShape,
    colors: CardColors = CardDefaults.elevatedCardColors(), elevation: CardElevation = CardDefaults.elevatedCardElevation(),
    interactionSource: MutableInteractionSource? = null, content: @Composable ColumnScope.() -> Unit) {
    val haptics = rememberTempoHaptics()
    M3ElevatedCard({ haptics.press(); onClick() }, modifier, enabled, shape, colors, elevation, interactionSource, content)
}

/** Clickable Surface. Non-clickable Surfaces still resolve to Material's own overload. */
@Composable
fun Surface(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, shape: Shape = RectangleShape,
    color: Color = MaterialTheme.colorScheme.surface, contentColor: Color = contentColorFor(color), tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp, border: BorderStroke? = null, interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit) {
    val haptics = rememberTempoHaptics()
    M3Surface({ haptics.press(); onClick() }, modifier, enabled, shape, color, contentColor, tonalElevation, shadowElevation, border, interactionSource, content)
}

/** A whole row that toggles a switch or checkbox, with the matching toggle haptic. */
@Composable
fun Modifier.hapticToggleable(value: Boolean, enabled: Boolean = true, role: Role? = null, onValueChange: (Boolean) -> Unit): Modifier {
    val haptics = rememberTempoHaptics()
    return toggleable(value, enabled = enabled, role = role) { haptics.toggle(it); onValueChange(it) }
}

/**
 * Bouncy squish for buttons placed side by side: the pressed one grows and its neighbours
 * give way, then everything springs back. Pass the same [interactionSource] to the button.
 */
@Composable
fun androidx.compose.foundation.layout.RowScope.squishWeight(interactionSource: MutableInteractionSource, base: Float = 1f): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val weight by androidx.compose.animation.core.animateFloatAsState(if (pressed) base * 1.22f else base,
        androidx.compose.animation.core.spring(dampingRatio = .45f, stiffness = 700f), label = "squish")
    return Modifier.weight(weight)
}

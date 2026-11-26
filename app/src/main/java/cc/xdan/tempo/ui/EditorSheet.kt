package cc.xdan.tempo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Tempo's editor drawer: the title, and Save on the top right. Back, dragging down or tapping outside closes it. The drawer
 * can be dragged down to close. When [dirty] is true, closing by back, dragging or tapping outside asks
 * whether to save or discard first, so no edit is lost by accident.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorSheet(title: String, dismiss: () -> Unit, confirm: (() -> Unit)? = null, enabled: Boolean = true,
    confirmLabel: String = "Save", scrollContent: Boolean = true, compact: Boolean = false, dirty: Boolean = false,
    content: @Composable ColumnScope.() -> Unit) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var askToSave by remember { mutableStateOf(false) }
    val latestDirty by rememberUpdatedState(dirty && confirm != null)
    if (compact) {
        val requestClose = { if (latestDirty) askToSave = true else dismiss() }
        Dialog(onDismissRequest = requestClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.padding(24.dp).widthIn(max = 480.dp).fillMaxWidth().heightIn(max = (LocalConfiguration.current.screenHeightDp * .85f).dp).imePadding(), shape = MaterialTheme.shapes.extraLarge) {
                Column(Modifier.testTag("editor-panel").padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EditorHeader(title, requestClose, confirm?.let { { focus.clearFocus(); keyboard?.hide(); it() } }, enabled, confirmLabel)
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState(), overscrollEffect = null).testTag("editor-content").padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
                }
            }
        }
        if (askToSave) SaveChangesDialog({ askToSave = false; confirm?.invoke() }, { askToSave = false; dismiss() }, { askToSave = false }, enabled)
        return
    }
    // Dragging down past the threshold asks first when there are unsaved changes.
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { value ->
        if (value == SheetValue.Hidden && latestDirty) { askToSave = true; false } else true
    })
    val scope = rememberCoroutineScope()
    val close = { focus.clearFocus(); keyboard?.hide(); scope.launch { state.hide() }.invokeOnCompletion { dismiss() }; Unit }
    val requestClose = { if (latestDirty) askToSave = true else close() }
    ModalBottomSheet(onDismissRequest = { if (latestDirty) askToSave = true else dismiss() }, sheetState = state,
        dragHandle = { BottomSheetDefaults.DragHandle() }) {
        val maxHeight = (LocalConfiguration.current.screenHeightDp * .88f).dp
        Column(Modifier.fillMaxWidth().heightIn(max = maxHeight).imePadding().testTag("editor-panel")) {
            EditorHeader(title, requestClose, confirm?.let { save -> {
                focus.clearFocus(); keyboard?.hide(); scope.launch { state.hide() }.invokeOnCompletion { save() }
            } }, enabled, confirmLabel)
            Column(Modifier.weight(1f, fill = false).nestedScroll(KeepFlingsInContent).then(if (scrollContent) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .testTag("editor-content").padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
            Spacer(Modifier.navigationBarsPadding())
        }
    }
    if (askToSave) SaveChangesDialog({ askToSave = false; focus.clearFocus(); keyboard?.hide(); scope.launch { state.hide() }.invokeOnCompletion { confirm?.invoke() } },
        { askToSave = false; close() }, { askToSave = false }, enabled)
}

/** Title on the left, primary action on the right. Back, swiping down or tapping outside closes. */
@Composable
private fun EditorHeader(title: String, close: () -> Unit, confirm: (() -> Unit)?, enabled: Boolean, confirmLabel: String) {
    Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (confirm != null) Button(onClick = confirm, enabled = enabled) { Text(confirmLabel) }
    }
}

@Composable
private fun SaveChangesDialog(save: () -> Unit, discard: () -> Unit, keepEditing: () -> Unit, canSave: Boolean) {
    AlertDialog(onDismissRequest = keepEditing, title = { Text("Save your changes?") },
        text = { Text(if (canSave) "You've changed this. Save it, or discard the changes." else "Some details still need fixing before this can be saved.") },
        confirmButton = { if (canSave) Button(onClick = save) { Text("Save") } else TextButton(onClick = keepEditing) { Text("Keep editing") } },
        dismissButton = { TextButton(onClick = discard) { Text("Discard", color = MaterialTheme.colorScheme.error) } })
}

/**
 * A clearly destructive action: error colours and a trash icon. Deleting is immediate because
 * the snackbar offers Undo, so there is no extra confirmation step.
 */
@Composable
fun DeleteButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalButton(onClick = onClick, modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer)) {
        Icon(Icons.Outlined.Delete, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(label)
    }
}

/**
 * Stops content flings from moving the drawer. When a fling inside the drawer hits the top or
 * bottom of its content, Material hands the leftover momentum to the sheet, which then slides
 * or dismisses on its own. Leftover fling scroll and velocity are absorbed here; a finger drag
 * (UserInput) still reaches the sheet, so dragging down from the top closes it as expected.
 */
internal val KeepFlingsInContent = object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
    override fun onPostScroll(consumed: androidx.compose.ui.geometry.Offset, available: androidx.compose.ui.geometry.Offset,
        source: androidx.compose.ui.input.nestedscroll.NestedScrollSource) =
        if (source == androidx.compose.ui.input.nestedscroll.NestedScrollSource.UserInput) androidx.compose.ui.geometry.Offset.Zero else available
    override suspend fun onPostFling(consumed: androidx.compose.ui.unit.Velocity, available: androidx.compose.ui.unit.Velocity) = available
}

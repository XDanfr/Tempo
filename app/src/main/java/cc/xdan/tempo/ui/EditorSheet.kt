package cc.xdan.tempo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Material's modal sheet owns its predictive-back progress and gesture cancellation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorSheet(title: String, dismiss: () -> Unit, confirm: (() -> Unit)? = null, enabled: Boolean = true,
    confirmLabel: String = "Save", scrollContent: Boolean = true, compact: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    if (compact) {
        Dialog(onDismissRequest = dismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            val focus = LocalFocusManager.current
            val keyboard = LocalSoftwareKeyboardController.current
            Surface(Modifier.padding(24.dp).widthIn(max = 480.dp).fillMaxWidth().heightIn(max = (LocalConfiguration.current.screenHeightDp * .85f).dp).imePadding(), shape = MaterialTheme.shapes.extraLarge) {
                Column(Modifier.testTag("editor-panel").padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                        IconButton(onClick = dismiss) { Icon(Icons.Outlined.Close, "Close editor") }
                    }
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState(), overscrollEffect = null).testTag("editor-content"), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
                    if (confirm != null) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = dismiss, Modifier.weight(1f)) { Text("Cancel") }
                        Button(enabled = enabled, onClick = { focus.clearFocus(); keyboard?.hide(); confirm() }, modifier = Modifier.weight(1f)) { Text(confirmLabel) }
                    }
                }
            }
        }
        return
    }
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // Keep content flings out of the sheet's draggable anchors, including at scroll boundaries.
    ModalBottomSheet(onDismissRequest = dismiss, sheetState = state, sheetGesturesEnabled = false,
        dragHandle = null) {
        val focus = LocalFocusManager.current
        val keyboard = LocalSoftwareKeyboardController.current
        val close = { focus.clearFocus(); keyboard?.hide(); scope.launch { state.hide(); dismiss() }; Unit }
        val maxHeight = (LocalConfiguration.current.screenHeightDp * .88f).dp
        Column(Modifier.fillMaxWidth().height(maxHeight).imePadding().testTag("editor-panel")) {
            Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, Modifier.weight(1f).padding(top = 8.dp), style = MaterialTheme.typography.headlineSmall)
                IconButton(onClick = close) { Icon(Icons.Outlined.Close, "Close editor") }
            }
            Column(Modifier.weight(1f).then(if (scrollContent) Modifier.verticalScroll(rememberScrollState(), overscrollEffect = null) else Modifier).testTag("editor-content").padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
            if (confirm != null) Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = close, Modifier.weight(1f)) { Text("Cancel") }
                Button(enabled = enabled, onClick = { focus.clearFocus(); keyboard?.hide(); scope.launch { state.hide(); confirm() } }, modifier = Modifier.weight(1f)) { Text(confirmLabel) }
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

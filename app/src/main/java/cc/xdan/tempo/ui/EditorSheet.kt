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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Material's modal sheet owns its predictive-back progress and gesture cancellation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorSheet(title: String, dismiss: () -> Unit, confirm: (() -> Unit)? = null, enabled: Boolean = true,
    confirmLabel: String = "Save", scrollContent: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close = { scope.launch { state.hide(); dismiss() }; Unit }
    ModalBottomSheet(onDismissRequest = dismiss, sheetState = state, sheetGesturesEnabled = scrollContent,
        dragHandle = if (scrollContent) ({ BottomSheetDefaults.DragHandle() }) else null) {
        val maxHeight = (LocalConfiguration.current.screenHeightDp * .88f).dp
        Column(Modifier.fillMaxWidth().then(if (scrollContent) Modifier.heightIn(max = maxHeight) else Modifier.height(maxHeight)).imePadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, Modifier.weight(1f).padding(top = 8.dp), style = MaterialTheme.typography.headlineSmall)
                IconButton(onClick = close) { Icon(Icons.Outlined.Close, "Close editor") }
            }
            Column(Modifier.weight(1f, fill = !scrollContent).then(if (scrollContent) Modifier.verticalScroll(rememberScrollState()) else Modifier).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
            if (confirm != null) Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = close, Modifier.weight(1f)) { Text("Cancel") }
                Button(enabled = enabled, onClick = { scope.launch { state.hide(); confirm() } }, modifier = Modifier.weight(1f)) { Text(confirmLabel) }
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

package cc.xdan.tempo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.xdan.tempo.account.*
import kotlinx.coroutines.launch

/** Sign-up and sign-in with a passkey, including the recovery-code steps and the first cloud sync. */
class AccountFlow internal constructor() {
    var busy by mutableStateOf(false); internal set
    var error by mutableStateOf<String?>(null); internal set
    var account by mutableStateOf<SignedIn?>(null); internal set
    internal var showCode by mutableStateOf<String?>(null)
    internal var askCode by mutableStateOf<Pair<SignedIn, String>?>(null)
    internal var run: (suspend () -> Unit) -> Unit = {}
    internal var create: () -> Unit = {}
    internal var signIn: () -> Unit = {}
    fun createAccount() = create()
    fun signInAccount() = signIn()
}

@Composable
fun rememberAccountFlow(name: String, signedIn: (SignedIn) -> Unit = {}): AccountFlow {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val scope = rememberCoroutineScope()
    val flow = remember { AccountFlow().apply { account = AccountStore.current(context) } }
    val latestName by rememberUpdatedState(name)
    val latestSignedIn by rememberUpdatedState(signedIn)
    suspend fun finish(result: AccountResult) {
        if (result.needsRecoveryCode != null) { flow.askCode = result.account to result.needsRecoveryCode; return }
        AccountStore.save(context, result.account); flow.account = result.account
        result.recoveryCode?.let { flow.showCode = it }
        runCatching { CloudSync.sync(context) }
        latestSignedIn(result.account)
    }
    flow.run = { action ->
        flow.busy = true; flow.error = null
        scope.launch {
            try { action() } catch (e: AccountException) { if (!e.cancelled) flow.error = e.message }
            catch (e: Exception) { flow.error = e.message ?: "Something went wrong" }
            finally { flow.busy = false }
        }
    }
    flow.create = { activity?.let { a -> flow.run { finish(Accounts.create(a, latestName)) } } }
    flow.signIn = { activity?.let { a -> flow.run { finish(Accounts.signIn(a)) } } }

    flow.showCode?.let { code ->
        val clipboard = LocalClipboardManager.current
        EditorSheet("Save your recovery code", { flow.showCode = null }) {
            Text("Your passkey provider can't unlock your saved timetables on a new phone by itself. You'll need this code once on each new phone.")
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Text(code, Modifier.fillMaxWidth().padding(18.dp), style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = { clipboard.setText(AnnotatedString(code)) }, modifier = Modifier.fillMaxWidth()) { Text("Copy") }
            Button(onClick = { flow.showCode = null }, modifier = Modifier.fillMaxWidth()) { Text("I've saved it") }
        }
    }
    flow.askCode?.let { (account, wrapped) ->
        var code by remember { mutableStateOf("") }
        var wrong by remember { mutableStateOf<String?>(null) }
        EditorSheet("Enter your recovery code", { flow.askCode = null }) {
            Text("This unlocks your saved timetables on this phone. You saved it when you made your account.")
            OutlinedTextField(code, { code = it.take(30); wrong = null }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                isError = wrong != null, supportingText = wrong?.let { { Text(it) } }, placeholder = { Text("XXXXX-XXXXX-XXXXX-XXXXX") })
            Button(onClick = {
                try { val unlocked = Accounts.unlockWithCode(account, wrapped, code); flow.askCode = null; flow.run { finish(AccountResult(unlocked)) } }
                catch (e: AccountException) { wrong = e.message }
            }, enabled = code.count { it.isLetterOrDigit() } >= 20, modifier = Modifier.fillMaxWidth()) { Text("Unlock") }
        }
    }
    return flow
}

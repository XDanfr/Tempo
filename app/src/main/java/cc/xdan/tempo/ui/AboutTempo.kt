package cc.xdan.tempo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import cc.xdan.tempo.BuildConfig
import cc.xdan.tempo.R
import androidx.compose.ui.res.painterResource

@Composable
fun AboutTempo() {
    val uri = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Made by XDan", style = MaterialTheme.typography.titleLarge)
        Text("Tempo ${BuildConfig.VERSION_NAME} · Part of Axis", style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = { uri.openUri("https://github.com/sponsors/XDanfr") }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.FavoriteBorder, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp)); Text("Sponsor XDan")
        }
        TextButton(onClick = { uri.openUri("https://github.com/XDanfr/Tempo") }) {
            Icon(painterResource(R.drawable.ic_github), null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp)); Text("Tempo on GitHub")
        }
    }
}

package cc.xdan.tempo.updates

import org.json.JSONObject
import java.net.URI

data class ReleaseManifest(val versionCode: Long, val versionName: String, val apk: String, val sha256: String) {
    companion object {
        fun parse(text: String): ReleaseManifest {
            val json = JSONObject(text)
            require(json.getInt("formatVersion") == 1) { "Unsupported update metadata" }
            val result = ReleaseManifest(json.getLong("versionCode"), json.getString("versionName"), json.getString("apk"), json.getString("sha256"))
            require(result.versionCode > 0 && result.versionName.matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+")))
            require(result.apk.matches(Regex("Tempo-[0-9]+\\.[0-9]+\\.[0-9]+\\.apk")))
            require(result.sha256.matches(Regex("[a-f0-9]{64}")))
            return result
        }
        fun validAsset(url: String): Boolean = runCatching {
            val uri = URI(url)
            uri.scheme == "https" && uri.host == "github.com" && uri.port == -1 && uri.userInfo == null && uri.query == null && uri.fragment == null &&
                uri.rawPath.matches(Regex("/XDanfr/Tempo/releases/download/v[0-9]+\\.[0-9]+\\.[0-9]+/[A-Za-z0-9._-]+"))
        }.getOrDefault(false)
    }
}

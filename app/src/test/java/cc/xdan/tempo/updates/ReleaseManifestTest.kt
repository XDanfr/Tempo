package cc.xdan.tempo.updates

import org.junit.Assert.*
import org.junit.Test

class ReleaseManifestTest {
    private fun metadata(version: String = "0.4.0", sha: String = "a".repeat(64)) = """{"formatVersion":1,"versionCode":4,"versionName":"$version","apk":"Tempo-$version.apk","sha256":"$sha"}"""
    @Test fun acceptsStableManifest() { assertEquals(4L, ReleaseManifest.parse(metadata()).versionCode) }
    @Test(expected = IllegalArgumentException::class) fun rejectsMalformedChecksum() { ReleaseManifest.parse(metadata(sha = "invalid")) }
    @Test(expected = IllegalArgumentException::class) fun rejectsPrereleaseName() { ReleaseManifest.parse(metadata(version = "0.4.0-beta")) }
    @Test fun confinesDownloadsToOfficialTaggedAssets() {
        assertTrue(ReleaseManifest.validAsset("https://github.com/XDanfr/Tempo/releases/download/v0.4.0/Tempo-0.4.0.apk"))
        listOf("https://github.com.evil.test/XDanfr/Tempo/releases/download/v0.4.0/a.apk", "https://github.com/other/Tempo/releases/download/v0.4.0/a.apk", "http://github.com/XDanfr/Tempo/releases/download/v0.4.0/a.apk", "https://github.com/XDanfr/Tempo/releases/download/v0.4.0/a.apk?url=bad", "https://github.com/XDanfr/Tempo/releases/download/v0.4.0/../a.apk").forEach { assertFalse(ReleaseManifest.validAsset(it)) }
    }
}

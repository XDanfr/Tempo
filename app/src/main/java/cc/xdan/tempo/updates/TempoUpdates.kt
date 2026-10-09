package cc.xdan.tempo.updates

import android.app.DownloadManager
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object TempoUpdates {
    private const val API = "https://api.github.com/repos/XDanfr/Tempo/releases/latest"
    fun prefs(context: Context) = context.getSharedPreferences("release-updates", Context.MODE_PRIVATE)
    fun installedCode(context: Context): Long {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        @Suppress("DEPRECATION") return if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
    }
    fun schedule(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("automatic", enabled).apply()
        val work = WorkManager.getInstance(context)
        if (enabled) {
            work.enqueueUniquePeriodicWork("tempo-release-updates", ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<ReleaseUpdateWorker>(1, TimeUnit.DAYS)
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).build()).build())
            check(context)
        } else work.cancelUniqueWork("tempo-release-updates")
    }
    fun check(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork("tempo-check-release", ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<ReleaseUpdateWorker>().setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build())
    }
    private fun getText(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000; connection.readTimeout = 15_000
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("User-Agent", "Tempo-Android")
        return try {
            require(connection.responseCode == 200) { if (connection.responseCode == 404) "No public stable release is available yet" else "GitHub returned HTTP ${connection.responseCode}" }
            val bytes = connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= 262144) { "Update metadata is too large" }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            require(bytes.size <= 262144) { "Update metadata is too large" }
            bytes.toString(Charsets.UTF_8)
        } finally { connection.disconnect() }
    }
    suspend fun findRelease(context: Context) = withContext(Dispatchers.IO) {
        val p = prefs(context)
        val release = JSONObject(getText(API))
        require(!release.optBoolean("draft") && !release.optBoolean("prerelease"))
        val assets = release.getJSONArray("assets")
        val manifestAsset = (0 until assets.length()).map { assets.getJSONObject(it) }.firstOrNull { it.optString("name") == "tempo-update.json" }
            ?: error("This release has no Tempo update metadata")
        val manifestUrl = manifestAsset.getString("browser_download_url")
        require(ReleaseManifest.validAsset(manifestUrl)) { "Unrecognised release source" }
        val manifest = ReleaseManifest.parse(getText(manifestUrl))
        require(release.getString("tag_name") == "v${manifest.versionName}") { "Release version does not match its metadata" }
        val apk = (0 until assets.length()).map { assets.getJSONObject(it) }.firstOrNull { it.optString("name") == manifest.apk }
            ?: error("Release APK is missing")
        require(apk.getLong("size") in 1..150_000_000) { "Release APK is too large" }
        val url = apk.getString("browser_download_url")
        require(ReleaseManifest.validAsset(url)) { "Unrecognised APK source" }
        val previous = p.getLong("versionCode", 0)
        if (previous != manifest.versionCode || p.getString("sha256", "") != manifest.sha256) {
            val download = p.getLong("downloadId", -1)
            if (download != -1L) context.getSystemService(DownloadManager::class.java).remove(download)
            p.edit().remove("downloadId").putBoolean("ready", false).apply()
        }
        p.edit().putLong("versionCode", manifest.versionCode).putString("versionName", manifest.versionName)
            .putString("url", url).putString("sha256", manifest.sha256).putString("status", if (manifest.versionCode > installedCode(context)) "Tempo ${manifest.versionName} is available" else "You’re up to date")
            .putLong("checked", System.currentTimeMillis()).apply()
    }
    fun download(context: Context) {
        val p = prefs(context)
        val url = p.getString("url", null) ?: return
        require(ReleaseManifest.validAsset(url))
        require(p.getLong("versionCode", 0) > installedCode(context))
        if (p.getLong("downloadId", -1) != -1L) return
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "tempo-update.apk")
        require(!file.exists() || file.delete()) { "Could not replace the old update" }
        val request = DownloadManager.Request(Uri.parse(url)).setTitle("Tempo update").setDescription("Downloading the next Tempo release")
            .setDestinationUri(Uri.fromFile(file)).setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverRoaming(false)
        if (p.getBoolean("automatic", false)) request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
        val id = context.getSystemService(DownloadManager::class.java).enqueue(request)
        p.edit().putLong("downloadId", id).putBoolean("ready", false).putString("status", "Downloading update…").apply()
    }
    suspend fun verify(context: Context) = withContext(Dispatchers.IO) {
        val p = prefs(context)
        val id = p.getLong("downloadId", -1)
        require(id != -1L) { "No update download found" }
        context.getSystemService(DownloadManager::class.java).query(DownloadManager.Query().setFilterById(id)).use { cursor ->
            require(cursor.moveToFirst()) { "Update download was removed" }
            require(cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) { "Update download is not complete" }
        }
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "tempo-update.apk")
        require(file.length() in 1..150_000_000) { "Invalid APK size" }
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input -> val buffer = ByteArray(65536); while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) } }
        require(digest.digest().joinToString("") { "%02x".format(it) } == p.getString("sha256", null)) { "Update checksum does not match" }
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES
        val installed = context.packageManager.getPackageInfo(context.packageName, flags)
        val archive = context.packageManager.getPackageArchiveInfo(file.path, flags) ?: error("Invalid APK")
        require(archive.packageName == context.packageName) { "APK is for another app" }
        @Suppress("DEPRECATION") val code = if (Build.VERSION.SDK_INT >= 28) archive.longVersionCode else archive.versionCode.toLong()
        require(code == p.getLong("versionCode", 0) && code > installedCode(context)) { "Unexpected APK version" }
        @Suppress("DEPRECATION") val installedSignatures = if (Build.VERSION.SDK_INT >= 28) installed.signingInfo?.apkContentsSigners else installed.signatures
        @Suppress("DEPRECATION") val archiveSignatures = if (Build.VERSION.SDK_INT >= 28) archive.signingInfo?.apkContentsSigners else archive.signatures
        require(!installedSignatures.isNullOrEmpty() && !archiveSignatures.isNullOrEmpty() && installedSignatures.toSet() == archiveSignatures.toSet()) {
            "This release uses a different signing key. Install an official signed release before using updates. Export your timetables before uninstalling."
        }
        p.edit().putBoolean("ready", true).putString("status", "Update ready to install").apply()
    }
    fun install(context: Context) {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "tempo-update.apk")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
        context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

class ReleaseUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        TempoUpdates.findRelease(applicationContext)
        val p = TempoUpdates.prefs(applicationContext)
        if (p.getBoolean("automatic", false) && p.getLong("versionCode", 0) > TempoUpdates.installedCode(applicationContext)) {
            TempoUpdates.download(applicationContext)
        }
        Result.success()
    } catch (e: kotlinx.coroutines.CancellationException) { throw e }
    catch (e: Exception) {
        TempoUpdates.prefs(applicationContext).edit().putString("status", e.message ?: "Could not check for updates").apply()
        Result.failure()
    }
}
class VerifyUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try { TempoUpdates.verify(applicationContext); Result.success() }
    catch (e: kotlinx.coroutines.CancellationException) { throw e }
    catch (e: Exception) {
        val p = TempoUpdates.prefs(applicationContext)
        val id = p.getLong("downloadId", -1)
        if (id != -1L) applicationContext.getSystemService(DownloadManager::class.java).remove(id)
        p.edit().remove("downloadId").putBoolean("ready", false).putString("status", e.message ?: "Could not verify this update").apply()
        Result.failure()
    }
}
class UpdateDownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        if (id == -1L || id != TempoUpdates.prefs(context).getLong("downloadId", -1)) return
        WorkManager.getInstance(context).enqueueUniqueWork("tempo-verify-update", ExistingWorkPolicy.KEEP, OneTimeWorkRequestBuilder<VerifyUpdateWorker>().build())
    }
}

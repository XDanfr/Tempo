@file:Suppress("RestrictedApi")

package cc.xdan.tempo.remote

import android.annotation.SuppressLint
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.clickable
import androidx.glance.adaptive.appwidget.ui.AppWidgetTemplateRegistry
import androidx.glance.adaptive.appwidget.ui.selection.AppWidgetGlanceSurface
import androidx.glance.adaptive.appwidget.ui.selection.AppWidgetSizeTiers
import androidx.glance.adaptive.appwidget.ui.selection.LocalContainerDimensions
import androidx.glance.adaptive.core.ui.selection.Dimensions
import androidx.glance.adaptive.core.ui.selection.WidthTier
import androidx.glance.adaptive.core.ui.templates.AdaptiveGlanceTemplate
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlinx.coroutines.*

/** Restricted template/override APIs are used ONLY in this opt-in, separate experiment APK. */
@SuppressLint("RestrictedApi")
internal data class ProbeData(val current: Boolean) : AdaptiveGlanceTemplate

@SuppressLint("RestrictedApi")
internal class AdaptiveProbe(private val width: Int, private val height: Int, private val current: Boolean) : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        AppWidgetTemplateRegistry.register(ProbeData::class.java,
            selectArchetype = { _, surface, dimensions -> AppWidgetSizeTiers.from(dimensions, surface as AppWidgetGlanceSurface).width },
            renderArchetype = { data, tier -> ProbeContent(context, data, tier) })
        provideContent {
            CompositionLocalProvider(LocalContainerDimensions provides Dimensions(width, height)) {
                AppWidgetTemplateRegistry.render(ProbeData(current), AppWidgetGlanceSurface.HOME_SCREEN)
            }
        }
    }
}

@Composable
@SuppressLint("RestrictedApi")
private fun ProbeContent(context: Context, data: ProbeData, tier: WidthTier) {
    // The native countdown is deliberately absent: Glance's RC translator doesn't support it.
    Column(GlanceModifier.fillMaxSize().background(Color(0xFFDAECCF)).padding(16.dp)
        .clickable(actionStartActivity(Intent(context, AdaptiveProbeActivity::class.java)))) {
        Text("${if (data.current) "Now" else "Up next"} · Adaptive $tier", style = TextStyle(fontSize = 12.sp))
        Text("Physics", style = TextStyle(fontSize = 26.sp))
        Text("24:00 · sample time", style = TextStyle(fontSize = 20.sp))
        if (tier == WidthTier.W4) Text("Next · Art & Design", style = TextStyle(fontSize = 16.sp))
    }
}

@SuppressLint("RestrictedApi")
internal suspend fun adaptiveProbeViews(context: Context, width: Int, height: Int, current: Boolean, remote: Boolean): RemoteViews {
    val options = Bundle()
    if (remote) options.putInt(GLANCE_OPTION_APPWIDGET_FORCE_BACKEND, BACKEND_REMOTE_COMPOSE)
    return AdaptiveProbe(width, height, current).compose(context, options = options, size = DpSize(width.dp, height.dp))
}

class AdaptiveProbeActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var renderJob: Job? = null
    private var expanded = false
    private var current = true
    private var remote = false
    private lateinit var content: LinearLayout
    private lateinit var report: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16, 32, 16, 16) }
        setContentView(root)
        report = TextView(this)
        root.addView(report)
        for ((label, action) in listOf<Pair<String, () -> Unit>>(
            "Resize" to { expanded = !expanded }, "Change event state" to { current = !current },
            "Switch RV / Remote Compose (Android 16+)" to { remote = !remote })) {
            root.addView(Button(this).apply { text = label; setOnClickListener { action(); render() } })
        }
        content = LinearLayout(this)
        root.addView(content)
        render()
    }
    private fun render() {
        renderJob?.cancel()
        renderJob = scope.launch {
            val width = if (expanded) 360 else 200
            val height = if (expanded) 180 else 220
            if (remote && Build.VERSION.SDK_INT < 36) {
                report.text = "Remote Compose override needs Android 16+. RV template test is available."
                return@launch
            }
            try {
                val views = adaptiveProbeViews(this@AdaptiveProbeActivity, width, height, current, remote)
                content.removeAllViews()
                val scale = resources.displayMetrics.density
                content.addView(views.apply(this@AdaptiveProbeActivity, content), LinearLayout.LayoutParams((width * scale).toInt(), (height * scale).toInt()))
                report.text = "IN-APP TEST ONLY · ${if (remote) "Remote Compose" else "RemoteViews"}\nGlance alpha02 / Adaptive alpha03 · static sample clock\nUse the experimental launcher widget for host testing."
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { report.text = "Experiment failed: ${error.javaClass.simpleName}: ${error.message}" }
        }
    }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}

/** An explicitly experimental placement for collecting actual launcher evidence. */
class RemoteComposeProbeWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = refresh(context, manager, ids)
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) = refresh(context, manager, intArrayOf(id))
    private fun refresh(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                for (id in ids) {
                    val options = manager.getAppWidgetOptions(id)
                    val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 200).coerceAtLeast(100)
                    val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 220).coerceAtLeast(56)
                    val views = if (Build.VERSION.SDK_INT >= 36) adaptiveProbeViews(context, width, height, true, true)
                        else RemoteViews(context.packageName, R.layout.probe_unavailable)
                    manager.updateAppWidget(id, views)
                }
            } catch (error: Exception) {
                Log.e("TempoRemoteProbe", "Remote Compose generation failed", error)
                for (id in ids) manager.updateAppWidget(id, RemoteViews(context.packageName, R.layout.probe_unavailable))
            } finally { pending.finish() }
        }
    }
}

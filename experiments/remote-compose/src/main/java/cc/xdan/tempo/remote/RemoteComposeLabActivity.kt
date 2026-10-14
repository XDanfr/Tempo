package cc.xdan.tempo.remote

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.view.RemoteComposePlayer
import cc.xdan.tempo.widgets.WidgetShapeRules

/** A local document/player experiment. It neither registers nor claims to be an AppWidget. */
class RemoteComposeLabActivity : Activity() {
    private var expanded = false
    private var current = true
    private lateinit var container: LinearLayout
    private lateinit var report: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16, 32, 16, 16); setBackgroundColor(Color.WHITE) }
        setContentView(container)
        report = TextView(this).apply { text = "In-app Remote Compose player • NOT launcher verification" }
        container.addView(report)
        container.addView(Button(this).apply {
            text = "Change document size"
            setOnClickListener { expanded = !expanded; render() }
        })
        container.addView(Button(this).apply {
            text = "Change event state"
            setOnClickListener { current = !current; render() }
        })
        render()
    }

    // alpha21's View player and listener remain library-group restricted. This suppression
    // is deliberately confined to this opt-in APK and is itself a production adoption gap.
    @SuppressLint("RestrictedApi")
    private fun render() {
        if (container.childCount > 3) container.removeViewAt(3)
        val width = if (expanded) 360 else 240
        val height = if (expanded) 160 else 96
        val writer = RemoteComposeWriter(CreationDisplayInfo(width, height, 160), "Physics, ${if (current) "time left" else "starts in"}", RcPlatformProfiles.ANDROIDX)
        val primary = if (Build.VERSION.SDK_INT >= 31) getColor(android.R.color.system_accent1_200) else Color.rgb(183, 243, 151)
        val neutral = if (Build.VERSION.SDK_INT >= 31) getColor(android.R.color.system_neutral1_900) else Color.rgb(32, 43, 31)
        val mid = (width - WidgetShapeRules.GAP) / 2f
        repeat(2) { index ->
            val left = if (index == 0) 0f else mid + WidgetShapeRules.GAP
            val path = Path().apply {
                addRoundRect(left, 0f, left + mid, height.toFloat(), WidgetShapeRules.member(index, 2).radii(), Path.Direction.CW)
            }
            writer.rcPaint.setColor(if (index == 0) neutral else primary).commit()
            writer.drawPath(path)
        }
        fun text(value: String, x: Float, y: Float, size: Float, colour: Int) {
            writer.rcPaint.setColor(colour).setTextSize(size).commit()
            writer.drawTextRun(value, 0, value.length, 0, value.length, x, y, false)
        }
        text("Physics", 12f, 40f, 22f, Color.WHITE)
        text(if (current) "Time left" else "Starts in", mid + 16, 30f, 12f, neutral)
        text("24:00", mid + 16, 64f, 28f, neutral)
        if (expanded) text("Room 12 • next: Maths", 12f, 128f, 13f, Color.WHITE)
        // Public document click area; listener below is currently restricted in alpha21.
        writer.addClickArea(1, "Change sample event state", 0f, 0f, width.toFloat(), height.toFloat(), "local-toggle")
        val bytes = writer.encodeToByteArray()
        val player = RemoteComposePlayer(this)
        player.setDocument(bytes)
        player.addIdActionListener { _, _ -> current = !current; render() }
        val scale = resources.displayMetrics.density
        container.addView(player, LinearLayout.LayoutParams((width * scale).toInt(), (height * scale).toInt()))
        report.text = "IN-APP ONLY • ${width}×${height} • ${bytes.size} bytes\nStatic sample time; no host countdown or particles verified."
    }
}

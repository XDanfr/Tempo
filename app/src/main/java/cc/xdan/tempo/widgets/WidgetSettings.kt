package cc.xdan.tempo.widgets

import android.content.Context
import cc.xdan.tempo.model.*

data class WidgetSettings(val timetableId: String? = null, val preset: ThemePreset? = null, val mode: ThemeMode = ThemeMode.DARK,
    val showLocation: Boolean = true, val emptyText: String = "Nothing scheduled this week") {
    companion object {
        fun read(context: Context, id: Int): WidgetSettings {
            val p = context.getSharedPreferences("widgets", Context.MODE_PRIVATE)
            return WidgetSettings(p.getString("$id.table", null), p.getString("$id.theme", null)?.let { name -> ThemePreset.entries.firstOrNull { it.name == name } },
                ThemeMode.entries.firstOrNull { it.name == p.getString("$id.mode", null) } ?: ThemeMode.DARK,
                p.getBoolean("$id.location", true), p.getString("$id.empty", null) ?: "Nothing scheduled this week")
        }
        fun delete(context: Context, id: Int) {
            context.getSharedPreferences("widgets", Context.MODE_PRIVATE).edit().apply {
                listOf("table", "theme", "mode", "location", "empty").forEach { remove("$id.$it") }
            }.apply()
        }
    }
    fun save(context: Context, id: Int) {
        context.getSharedPreferences("widgets", Context.MODE_PRIVATE).edit()
            .putString("$id.table", timetableId).putString("$id.theme", preset?.name).putString("$id.mode", mode.name)
            .putBoolean("$id.location", showLocation).putString("$id.empty", emptyText.trim().take(120)).apply()
    }
}

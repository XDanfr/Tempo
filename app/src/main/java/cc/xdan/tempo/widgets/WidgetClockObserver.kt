package cc.xdan.tempo.widgets

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat

/** Opportunistic freshness while the process survives; never a keep-alive service. */
internal object WidgetClockObserver {
    private var receiver: WidgetClockReceiver? = null
    @Volatile private var nextRefresh = Long.MAX_VALUE

    fun isDue(now: Long): Boolean = now >= nextRefresh
    fun refreshedUntil(deadline: Long) { nextRefresh = deadline }

    @Synchronized
    fun update(context: Context, hasWidgets: Boolean) {
        val app = context.applicationContext
        if (!hasWidgets) {
            receiver?.let { app.unregisterReceiver(it) }
            receiver = null
            nextRefresh = Long.MAX_VALUE
        } else if (receiver == null) {
            val clock = WidgetClockReceiver()
            ContextCompat.registerReceiver(app, clock, IntentFilter().apply {
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }, ContextCompat.RECEIVER_NOT_EXPORTED)
            receiver = clock
        }
    }
}

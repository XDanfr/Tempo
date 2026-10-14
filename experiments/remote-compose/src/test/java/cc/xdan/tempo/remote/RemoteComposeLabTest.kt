package cc.xdan.tempo.remote

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RemoteComposeLabTest {
    @Test fun serializesLoadsAndRendersTwoDocumentSizesAndEventStatesLocally() {
        val controller = Robolectric.buildActivity(RemoteComposeLabActivity::class.java).setup()
        try {
            val activity = controller.get()
            val container = activity.findViewById<android.widget.FrameLayout>(android.R.id.content).getChildAt(0) as LinearLayout
            for (expanded in listOf(false, true)) {
                if (expanded) (container.getChildAt(1) as Button).performClick()
                for (current in listOf(true, false)) {
                    if (!current) (container.getChildAt(2) as Button).performClick()
                    container.measure(View.MeasureSpec.makeMeasureSpec(420, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY))
                    container.layout(0, 0, 420, 500)
                    val report = (container.getChildAt(0) as TextView).text.toString()
                    assertTrue(report.contains(if (expanded) "360×160" else "240×96"))
                    assertTrue(report.contains("IN-APP ONLY"))
                    val bitmap = Bitmap.createBitmap(420, 500, Bitmap.Config.ARGB_8888)
                    container.draw(Canvas(bitmap))
                    val output = File("build/reports/remote-previews/local-$expanded-$current.png")
                    output.parentFile!!.mkdirs()
                    output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    assertTrue(output.length() > 100)
                }
                (container.getChildAt(2) as Button).performClick()
            }
        } finally { controller.pause().stop().destroy() }
    }
}

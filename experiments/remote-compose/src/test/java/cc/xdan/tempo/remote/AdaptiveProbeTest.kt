package cc.xdan.tempo.remote

import android.os.Parcel
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.Before
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/** Tests generated documents / local inflation. This does not verify a real launcher. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35, 37])
@LooperMode(LooperMode.Mode.PAUSED)
@OptIn(ExperimentalCoroutinesApi::class)
class AdaptiveProbeTest {
    @Before fun mainDispatcher() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun resetMainDispatcher() { Dispatchers.resetMain() }
    private fun texts(view: View): List<String> = (if (view is TextView) listOf(view.text.toString()) else emptyList()) +
        if (view is ViewGroup) (0 until view.childCount).flatMap { texts(view.getChildAt(it)) } else emptyList()

    @Test fun restrictedAdaptiveRegistrySelectsTemplateAndReflectsEventState() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        for (width in listOf(200, 360)) for (current in listOf(false, true)) {
            val views = adaptiveProbeViews(context, width, 220, current, false)
            val root = views.apply(context, FrameLayout(context))
            val text = texts(root)
            assertTrue(text.contains("Physics"))
            assertTrue(text.any { it.startsWith(if (current) "Now" else "Up next") })
            assertEquals(width == 360, text.contains("Next · Art & Design"))
        }
    }

    @Test @Config(sdk = [37]) fun restrictedGlanceOverrideGeneratesParcelableDrawInstructions() = runBlocking {
        val views = adaptiveProbeViews(RuntimeEnvironment.getApplication(), 360, 220, true, true)
        // Remote Compose has draw instructions instead of a native layout resource.
        assertEquals(0, views.layoutId)
        val parcel = Parcel.obtain()
        try {
            views.writeToParcel(parcel, 0)
            assertTrue(parcel.dataSize() > 100)
        } finally { parcel.recycle() }
    }
}

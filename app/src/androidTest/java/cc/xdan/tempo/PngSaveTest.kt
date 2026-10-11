package cc.xdan.tempo

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.app.ActivityOptionsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cc.xdan.tempo.design.TempoTheme
import cc.xdan.tempo.design.presetColorScheme
import cc.xdan.tempo.model.*
import cc.xdan.tempo.schedule.*
import cc.xdan.tempo.ui.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PngSaveTest {
    @get:Rule val compose = createComposeRule()
    @Test fun savePickerResultWritesExactlyThePrivacyFilteredPreview() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val date = LocalDate.parse("2026-10-12")
        val table = Timetable(name = "Private name", subjects = listOf(Subject("s", "Private subject", location = "Private room")),
            sessions = listOf(Session("a", "s", 1, TimeSpan(540, 600), notes = "Private notes")))
        val file = java.io.File(context.filesDir, "qa-saved.png").also { it.delete() }
        val registry = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) {
                dispatchResult(requestCode, Activity.RESULT_OK, Intent().setData(Uri.fromFile(file)))
            }
        }
        val owner = object : ActivityResultRegistryOwner { override val activityResultRegistry = registry }
        var message: String? = null
        compose.setContent { TempoTheme(table.appearance) {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) { PngExportSheet(table, {}, { message = it }) }
        } }
        compose.chooseDate("Date", LocalDate.now(), date)
        compose.onNodeWithText("Image theme").performScrollTo()
        compose.onNodeWithText("Ocean").performClick()
        compose.onNodeWithText("Dark").performScrollTo().performClick()
        compose.onNodeWithText("Timetable name").performScrollTo().performClick()
        compose.onNodeWithText("Subject and break names").performScrollTo().performClick()
        compose.onNodeWithText("Locations").performScrollTo().assertIsOff()
        compose.onNodeWithText("Notes").performScrollTo().assertIsOff()
        compose.waitUntil(20_000) { compose.onAllNodesWithContentDescription("Exact PNG export preview").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Save PNG").performScrollTo().performClick()
        compose.waitUntil(10_000) { message?.startsWith("Saved PNG") == true }
        val actual = BitmapFactory.decodeFile(file.path)
        val expected = renderSchedulePng(context, scheduleImagePlan(table, date, false, 1080, ImagePrivacy(false, false)), imagePalette(presetColorScheme(ThemePreset.OCEAN, true)))
        assertTrue("Saved pixels must equal the privacy-filtered preview", expected.sameAs(actual))
    }
}

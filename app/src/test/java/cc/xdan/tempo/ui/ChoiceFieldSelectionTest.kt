package cc.xdan.tempo.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ChoiceFieldSelectionTest {
    @get:Rule val rule = createComposeRule()

    private fun check(compact: Boolean) {
        rule.setContent {
            var v by remember { mutableStateOf("Apple") }
            EditorSheet("T", {}, compact = compact) {
                ChoiceField("Fruit", v, listOf("Apple", "Pear"), { it }, { v = it })
                Text("chosen=$v")
            }
        }
        rule.onNodeWithText("Apple").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Pear").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("chosen=Pear").assertExists()
    }
    @Test fun sheet() = check(false)
    @Test fun dialog() = check(true)
}

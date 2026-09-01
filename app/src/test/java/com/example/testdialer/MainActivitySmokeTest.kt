package com.example.testdialer

import android.content.res.Configuration
import android.widget.Button
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MainActivitySmokeTest {
    @Test
    fun `legacy voice UI remains reachable after ComponentActivity migration`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()

        findButton(activity, activity.getString(R.string.nav_test)).performClick()

        assertNotNull(findButton(activity, activity.getString(R.string.dial_test)))
        assertTrue(activity.findViewById<android.view.View>(android.R.id.content).isShown)
        controller.configurationChange(Configuration())
    }

    @Test
    fun `manual session and history entry points are visible`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        findButton(activity, activity.getString(R.string.nav_test)).performClick()
        assertNotNull(findButton(activity, activity.getString(R.string.manual_session_start)))

        findButton(activity, activity.getString(R.string.nav_register)).performClick()
        val allText = collectText(activity.findViewById(android.R.id.content))
        assertTrue(allText.contains(activity.getString(R.string.manual_history_title)))
        assertTrue(allText.contains(activity.getString(R.string.legacy_voice_history_title)))
    }

    private fun findButton(activity: MainActivity, text: String): Button {
        val root = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
        return descendants(root).filterIsInstance<Button>().first { it.text.toString() == text }
    }

    private fun collectText(root: android.view.View): String =
        descendants(root).filterIsInstance<android.widget.TextView>().joinToString("\n") { it.text }

    private fun descendants(root: android.view.View): Sequence<android.view.View> = sequence {
        yield(root)
        if (root is android.view.ViewGroup) {
            repeat(root.childCount) { yieldAll(descendants(root.getChildAt(it))) }
        }
    }
}

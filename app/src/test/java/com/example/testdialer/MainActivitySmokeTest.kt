package com.example.testdialer

import android.content.res.Configuration
import android.widget.Button
import com.example.testdialer.ui.RunHomeView
import com.example.testdialer.ui.SystemStatusStripView
import com.example.testdialer.persistence.TestRunRepository
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.Executors

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MainActivitySmokeTest {
    @Test
    fun `run centered shell is neutral and keeps all task entry points`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()

        findButton(activity, activity.getString(R.string.nav_test)).performClick()
        val allText = collectText(activity.findViewById(android.R.id.content))

        assertTrue(allText.contains(activity.getString(R.string.run_empty_title)))
        assertTrue(allText.contains(activity.getString(R.string.run_tasks_title)))
        val addTest = findButton(activity, activity.getString(R.string.run_add_test))
        assertNotNull(addTest)
        assertNotNull(findButton(activity, activity.getString(R.string.voice_type)))
        assertNotNull(findButton(activity, activity.getString(R.string.sms_type)))
        assertNotNull(findButton(activity, activity.getString(R.string.data_type)))
        assertNotNull(findButton(activity, activity.getString(R.string.manual_session_start)))

        addTest.performClick()
        val afterCta = collectText(activity.findViewById(android.R.id.content))
        assertTrue(afterCta.contains(activity.getString(R.string.run_empty_title)))
        assertFalse(afterCta.contains(activity.getString(R.string.manual_session_active, "")))
    }

    @Test
    fun `add test focuses Tasks without creating a persisted run`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        findButton(activity, activity.getString(R.string.nav_test)).performClick()
        val repository = (activity.application as TestDialerApplication).testRunRepository
        val before = persistedRunCount(repository)

        findButton(activity, activity.getString(R.string.run_add_test)).performClick()

        val runHome = descendants(activity.findViewById(android.R.id.content))
            .filterIsInstance<RunHomeView>()
            .single()
        assertTrue(runHome.tasksHeading.hasFocus())
        assertEquals(activity.getString(R.string.run_tasks_title), runHome.tasksHeading.text.toString())
        val after = persistedRunCount(repository)
        assertEquals(before, after)
    }

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

    @Test
    fun `guided SMS and cellular Data remain reachable without hiding Voice`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        findButton(activity, activity.getString(R.string.nav_test)).performClick()

        findButton(activity, activity.getString(R.string.sms_type)).performClick()
        var allText = collectText(activity.findViewById(android.R.id.content))
        assertTrue(allText.contains(activity.getString(R.string.sms_card_title)))
        assertNotNull(findButton(activity, activity.getString(R.string.sms_open_composer)))

        findButton(activity, activity.getString(R.string.data_type)).performClick()
        allText = collectText(activity.findViewById(android.R.id.content))
        assertTrue(allText.contains(activity.getString(R.string.data_card_title)))
        assertNotNull(findButton(activity, activity.getString(R.string.data_start)))

        findButton(activity, activity.getString(R.string.voice_type)).performClick()
        assertNotNull(findButton(activity, activity.getString(R.string.dial_test)))
    }

    @Test
    fun `selected task survives rotation in run centered shell`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        findButton(activity, activity.getString(R.string.nav_test)).performClick()
        findButton(activity, activity.getString(R.string.data_type)).performClick()

        controller.configurationChange(Configuration())
        val rotated = controller.get()

        assertTrue(collectText(rotated.findViewById(android.R.id.content)).contains(
            rotated.getString(R.string.data_card_title),
        ))
        val dataButton = findButton(rotated, rotated.getString(R.string.data_type))
        assertEquals(1f, dataButton.alpha)
        assertTrue(dataButton.isSelected)
    }

    @Test
    fun `test section and status strip survive lifecycle restart and rotation`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        findButton(activity, activity.getString(R.string.nav_test)).performClick()

        controller.pause().stop().start().resume()
        controller.configurationChange(Configuration())
        val restored = controller.get()

        assertTrue(collectText(restored.findViewById(android.R.id.content)).contains(
            restored.getString(R.string.run_home_title),
        ))
        val strip = descendants(restored.findViewById(android.R.id.content))
            .filterIsInstance<SystemStatusStripView>()
            .single()
        assertTrue(strip.contentDescription.contains(restored.getString(R.string.status_wifi_label)))
        assertNotNull(findButton(restored, restored.getString(R.string.run_add_test)))
    }

    private fun findButton(activity: MainActivity, text: String): Button {
        val root = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
        return descendants(root).filterIsInstance<Button>().first { it.text.toString() == text }
    }

    private fun collectText(root: android.view.View): String =
        descendants(root).filterIsInstance<android.widget.TextView>().joinToString("\n") { it.text }

    private fun persistedRunCount(repository: TestRunRepository): Int {
        val executor = Executors.newSingleThreadExecutor()
        return try {
            executor.submit<Int> { repository.listSummaries().size }.get()
        } finally {
            executor.shutdownNow()
        }
    }

    private fun descendants(root: android.view.View): Sequence<android.view.View> = sequence {
        yield(root)
        if (root is android.view.ViewGroup) {
            repeat(root.childCount) { yieldAll(descendants(root.getChildAt(it))) }
        }
    }
}

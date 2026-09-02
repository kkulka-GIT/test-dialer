package com.example.testdialer

import android.content.res.Configuration
import android.graphics.Rect
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ScrollView
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
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.util.concurrent.Executors

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MainActivitySmokeTest {
    @Test
    fun `operations home replaces separate Status and Test navigation`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        val root = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
        val buttons = descendants(root).filterIsInstance<Button>().toList()
        val allText = collectText(root)

        assertTrue(buttons.any { it.text.toString() == activity.getString(R.string.nav_operations) })
        assertTrue(buttons.any { it.text.toString() == activity.getString(R.string.nav_register) })
        assertFalse(buttons.any { it.text.toString() == activity.getString(R.string.nav_status) })
        assertFalse(buttons.any { it.text.toString() == activity.getString(R.string.nav_test) })
        assertFalse(allText.contains(activity.getString(R.string.status_dashboard_title)))
        assertFalse(allText.contains("Ręczna sesja billingowa"))
        assertTrue(allText.contains(activity.getString(R.string.run_home_title)))
    }

    @Test
    fun `run centered shell is neutral and keeps all task entry points`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()

        findButton(activity, activity.getString(R.string.nav_operations)).performClick()
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
        findButton(activity, activity.getString(R.string.nav_operations)).performClick()
        val repository = (activity.application as TestDialerApplication).testRunRepository
        val before = persistedRunCount(repository)
        val runHome = descendants(activity.findViewById(android.R.id.content))
            .filterIsInstance<RunHomeView>()
            .single()
        val scroll = generateSequence(runHome.parent) { it.parent }
            .filterIsInstance<ScrollView>()
            .first()
        val viewportWidth = (360 * activity.resources.displayMetrics.density).toInt()
        val viewportHeight = (240 * activity.resources.displayMetrics.density).toInt()
        scroll.measure(
            View.MeasureSpec.makeMeasureSpec(viewportWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(viewportHeight, View.MeasureSpec.EXACTLY),
        )
        scroll.layout(0, 0, viewportWidth, viewportHeight)
        val tasksBounds = Rect().also { bounds ->
            runHome.tasksHeading.getDrawingRect(bounds)
            scroll.offsetDescendantRectToMyCoords(runHome.tasksHeading, bounds)
        }

        assertEquals(0, scroll.scrollY)
        assertTrue(tasksBounds.top >= scroll.height)

        findButton(activity, activity.getString(R.string.run_add_test)).performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertTrue(runHome.tasksHeading.hasFocus())
        assertEquals(activity.getString(R.string.run_tasks_title), runHome.tasksHeading.text.toString())
        assertTrue(scroll.scrollY > 0)
        assertTrue(tasksBounds.bottom > scroll.scrollY)
        assertTrue(tasksBounds.top < scroll.scrollY + scroll.height)
        val after = persistedRunCount(repository)
        assertEquals(before, after)
    }

    @Test
    fun `legacy voice UI remains reachable after ComponentActivity migration`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()

        findButton(activity, activity.getString(R.string.nav_operations)).performClick()

        assertNotNull(findButton(activity, activity.getString(R.string.dial_test)))
        assertTrue(activity.findViewById<android.view.View>(android.R.id.content).isShown)
        controller.configurationChange(Configuration())
    }

    @Test
    fun `manual session and history entry points are visible`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        findButton(activity, activity.getString(R.string.nav_operations)).performClick()
        assertNotNull(findButton(activity, activity.getString(R.string.manual_session_start)))

        findButton(activity, activity.getString(R.string.nav_register)).performClick()
        val allText = collectText(activity.findViewById(android.R.id.content))
        assertTrue(allText.contains(activity.getString(R.string.manual_history_title)))
        assertTrue(allText.contains(activity.getString(R.string.legacy_voice_history_title)))
    }

    @Test
    fun `guided SMS and cellular Data remain reachable without hiding Voice`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        findButton(activity, activity.getString(R.string.nav_operations)).performClick()

        findButton(activity, activity.getString(R.string.sms_type)).performClick()
        var allText = collectText(activity.findViewById(android.R.id.content))
        assertTrue(allText.contains(activity.getString(R.string.sms_card_title)))
        assertNotNull(findButton(activity, activity.getString(R.string.sms_open_composer)))
        assertSingleStatusStrip(activity)

        findButton(activity, activity.getString(R.string.data_type)).performClick()
        allText = collectText(activity.findViewById(android.R.id.content))
        assertTrue(allText.contains(activity.getString(R.string.data_card_title)))
        assertNotNull(findButton(activity, activity.getString(R.string.data_start)))
        assertSingleStatusStrip(activity)

        findButton(activity, activity.getString(R.string.voice_type)).performClick()
        assertNotNull(findButton(activity, activity.getString(R.string.dial_test)))
        assertSingleStatusStrip(activity)
    }

    @Test
    fun `selected task survives rotation in run centered shell`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        findButton(activity, activity.getString(R.string.nav_operations)).performClick()
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
        findButton(activity, activity.getString(R.string.nav_operations)).performClick()

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

    private fun assertSingleStatusStrip(activity: MainActivity) {
        val strips = descendants(activity.findViewById(android.R.id.content))
            .filterIsInstance<SystemStatusStripView>()
            .toList()
        assertEquals(1, strips.size)
        assertTrue(strips.single().contentDescription.contains(activity.getString(R.string.status_sim_label)))
        assertTrue(strips.single().contentDescription.contains(activity.getString(R.string.status_network_label)))
        assertTrue(strips.single().contentDescription.contains(activity.getString(R.string.status_cellular_label)))
        assertTrue(strips.single().contentDescription.contains(activity.getString(R.string.status_wifi_label)))
    }

    private fun descendants(root: android.view.View): Sequence<android.view.View> = sequence {
        yield(root)
        if (root is android.view.ViewGroup) {
            repeat(root.childCount) { yieldAll(descendants(root.getChildAt(it))) }
        }
    }
}

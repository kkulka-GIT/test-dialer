package com.example.testdialer

import android.content.res.Configuration
import android.graphics.Rect
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
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
import com.example.testdialer.sms.GuidedSmsUiState

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MainActivitySmokeTest {
    @Test
    fun `operations home replaces separate Status and Test navigation`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        val root = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
        val buttons = descendants(root).filterIsInstance<Button>().toList()
        val allText = collectText(root)

        assertEquals("Operacje", activity.getString(R.string.nav_operations))
        assertTrue(buttons.any { it.text.toString() == "Operacje" })
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
    fun `planned Task actions name the Task for accessibility and reopen a fresh prefilled form`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        findButton(activity, activity.getString(R.string.run_start_scenario)).performClick()
        val smsTitle = "SMS standard"
        val openDescription = activity.getString(R.string.task_open_accessibility, smsTitle)
        val skipDescription = activity.getString(R.string.task_skip_accessibility, smsTitle)
        val openSms = awaitButtonWithDescription(activity, openDescription)

        assertNotNull(awaitButtonWithDescription(activity, skipDescription))
        MainActivity::class.java.getDeclaredField("guidedSmsState").apply {
            isAccessible = true
            set(activity, GuidedSmsUiState(saved = true))
        }

        openSms.performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val inputs = descendants(activity.findViewById(android.R.id.content)).filterIsInstance<EditText>().toList()
        assertTrue(inputs.any { it.text.toString() == "+48123456789" })
        assertTrue(inputs.any { it.text.toString() == "Test Dialer" })
        assertNotNull(findButton(activity, activity.getString(R.string.sms_open_composer)))
    }

    @Test
    fun `planned execution keeps Run Task and stage visible with compact optional fields`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        findButton(activity, activity.getString(R.string.run_start_scenario)).performClick()
        val taskTitle = "SMS standard"
        awaitButtonWithDescription(
            activity,
            activity.getString(R.string.task_open_accessibility, taskTitle),
        ).performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val root = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
        val context = descendants(root).first {
            it.contentDescription?.toString()?.contains("Aktywny Run") == true &&
                it.contentDescription?.toString()?.contains(taskTitle) == true
        }
        assertTrue(context.contentDescription.contains(activity.getString(R.string.execution_stage_prepare)))
        val optionalButton = findButton(activity, activity.getString(R.string.execution_optional_details))
        val labelInput = descendants(root).filterIsInstance<EditText>()
            .first { it.hint == activity.getString(R.string.sms_label_hint) }
        assertEquals(View.GONE, labelInput.parent.let { it as View }.visibility)
        assertEquals(
            activity.getString(R.string.execution_optional_collapsed),
            optionalButton.stateDescription,
        )

        optionalButton.performClick()
        assertTrue(labelInput.isShown)
        assertEquals(activity.getString(R.string.execution_optional_hide), optionalButton.text.toString())
        assertEquals(activity.getString(R.string.execution_optional_hide), optionalButton.contentDescription)
        assertEquals(
            activity.getString(R.string.execution_optional_expanded),
            optionalButton.stateDescription,
        )
        assertSingleStatusStrip(activity)
    }

    @Test
    fun `unfinished asynchronous SMS keeps its execution screen selected`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        findButton(activity, activity.getString(R.string.sms_type)).performClick()
        MainActivity::class.java.getDeclaredField("guidedSmsState").apply {
            isAccessible = true
            set(activity, GuidedSmsUiState(awaitingObservation = true))
        }
        renderScenario(activity, "SMS")

        val dataButton = findButton(activity, activity.getString(R.string.data_type))

        assertFalse(dataButton.isEnabled)
        dataButton.performClick()
        assertTrue(findButton(activity, activity.getString(R.string.sms_type)).isSelected)
        assertTrue(collectText(activity.findViewById(android.R.id.content)).contains(
            activity.getString(R.string.sms_observation_title),
        ))
    }

    @Test
    fun `unfinished Voice observation keeps its execution screen selected`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        MainActivity::class.java.getDeclaredField("awaitingVoiceOutcome").apply {
            isAccessible = true
            setBoolean(activity, true)
        }
        renderScenario(activity, "VOICE")

        val smsButton = findButton(activity, activity.getString(R.string.sms_type))

        assertFalse(smsButton.isEnabled)
        smsButton.performClick()
        assertTrue(findButton(activity, activity.getString(R.string.voice_type)).isSelected)
        assertTrue(collectText(activity.findViewById(android.R.id.content)).contains(
            activity.getString(R.string.voice_outcome_title),
        ))
    }

    @Test
    fun `running Data keeps its execution screen selected`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        findButton(activity, activity.getString(R.string.data_type)).performClick()
        MainActivity::class.java.getDeclaredField("cellularDataState").apply {
            isAccessible = true
            set(activity, com.example.testdialer.data.CellularDataUiState(busy = true))
        }
        renderScenario(activity, "DATA")

        val smsButton = findButton(activity, activity.getString(R.string.sms_type))

        assertFalse(smsButton.isEnabled)
        smsButton.performClick()
        assertTrue(findButton(activity, activity.getString(R.string.data_type)).isSelected)
        assertTrue(collectText(activity.findViewById(android.R.id.content)).contains(
            activity.getString(R.string.data_running),
        ))
    }

    @Test
    fun `edited Scenario parameters survive rotation and remain executable`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        findButton(activity, activity.getString(R.string.run_start_scenario)).performClick()
        awaitButtonWithDescription(
            activity,
            activity.getString(R.string.task_open_accessibility, "SMS standard"),
        ).performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val inputs = descendants(activity.findViewById(android.R.id.content)).filterIsInstance<EditText>().toList()
        inputs.first { it.hint == activity.getString(R.string.sms_destination_hint) }.setText("+48999888777")
        inputs.first { it.hint == activity.getString(R.string.sms_message_hint) }.setText("Edytowana treść")

        controller.configurationChange(Configuration())
        val rotated = controller.get()
        val rotatedInputs = descendants(rotated.findViewById(android.R.id.content)).filterIsInstance<EditText>().toList()
        assertEquals("+48999888777", rotatedInputs.first { it.hint == rotated.getString(R.string.sms_destination_hint) }.text.toString())
        assertEquals("Edytowana treść", rotatedInputs.first { it.hint == rotated.getString(R.string.sms_message_hint) }.text.toString())
        assertNotNull(findButton(rotated, rotated.getString(R.string.sms_open_composer)))
        assertSingleStatusStrip(rotated)
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

    private fun awaitButtonWithDescription(activity: MainActivity, description: String): Button {
        repeat(100) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            descendants(activity.findViewById(android.R.id.content)).filterIsInstance<Button>()
                .firstOrNull { it.contentDescription?.toString() == description }
                ?.let { return it }
            Thread.sleep(10)
        }
        error("Button not found: $description")
    }

    private fun collectText(root: android.view.View): String =
        descendants(root).filterIsInstance<android.widget.TextView>().joinToString("\n") { it.text }

    private fun renderScenario(activity: MainActivity, typeName: String) {
        val typeClass = Class.forName("com.example.testdialer.ui.TestType")
        val type = typeClass.enumConstants.first { (it as Enum<*>).name == typeName }
        MainActivity::class.java.getDeclaredMethod("renderScenario", typeClass).apply {
            isAccessible = true
            invoke(activity, type)
        }
    }

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

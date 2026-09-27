package com.example.testdialer

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import com.example.testdialer.active.*
import com.example.testdialer.domain.*
import com.example.testdialer.persistence.StoredTestRun
import com.example.testdialer.register.RegisterUiState
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReportWorkflowUiTest {
    @Test fun `run name draft survives recreation`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        views(activity.findViewById(android.R.id.content)).filterIsInstance<EditText>()
            .first { it.hint == activity.getString(R.string.run_name_hint) }.setText("Test taryfy weekendowej")
        controller.configurationChange(Configuration())
        val restored = controller.get()
        assertEquals("Test taryfy weekendowej", views(restored.findViewById(android.R.id.content)).filterIsInstance<EditText>()
            .first { it.hint == restored.getString(R.string.run_name_hint) }.text.toString())
        controller.pause().stop().destroy()
    }

    @Test fun `replay prefills actual SMS without sending or changing history`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val stored = sample()
        setField(activity, "activeRunState", ActiveRunUiState(active = ActiveRun(stored, emptyList())))
        invoke(activity, "renderActiveRun")
        setField(activity, "registerState", RegisterUiState(selectedRun = stored, selectedEventId = EventId("event-1")))
        invoke(activity, "renderRegister")
        button(activity, R.string.nav_register).performClick()
        button(activity, R.string.repeat_event).performClick()
        val edits = views(activity.findViewById(android.R.id.content)).filterIsInstance<EditText>().map { it.text.toString() }.toList()
        assertTrue(edits.contains("+48123456789"))
        assertTrue(edits.contains("Test wiadomości"))
        assertNull(Shadows.shadowOf(activity).nextStartedActivity)
        assertEquals(1, stored.run.events.size)
        controller.pause().stop().destroy()
    }

    @Test fun `render actual home run and report screens for visual review`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        capture(activity, "01-home")
        val stored = sample()
        setField(activity, "activeRunState", ActiveRunUiState(active = ActiveRun(stored, listOf(
            ActiveTask(stored.scenario.steps[0], ActiveTaskStatus.DONE),
            ActiveTask(stored.scenario.steps[1], ActiveTaskStatus.PENDING),
        ))))
        invoke(activity, "renderActiveRun")
        capture(activity, "02-active-run")
        setField(activity, "registerState", RegisterUiState(selectedRun = stored))
        invoke(activity, "renderRegister")
        button(activity, R.string.nav_register).performClick()
        capture(activity, "03-report")
        assertNotNull(button(activity, R.string.report_export))
        controller.pause().stop().destroy()
    }

    @Test fun `large font keeps action labels within button bounds`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val config = Configuration(controller.get().resources.configuration).apply { fontScale = 1.6f }
        controller.configurationChange(config)
        val activity = controller.get()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        capture(activity, "04-home-large-font")
        views(activity.findViewById(android.R.id.content)).filterIsInstance<Button>()
            .filter { it.isShown && it.width > 0 }.forEach { button ->
                val layout = button.layout
                if (layout != null && layout.lineCount > 0) {
                    assertTrue("Clipped action: ${button.text}", layout.getLineBottom(layout.lineCount - 1) <= button.height - button.compoundPaddingTop - button.compoundPaddingBottom)
                }
            }
        controller.pause().stop().destroy()
    }

    @Test fun `volume form validates input and preserves amount after rotation`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        button(activity, R.string.data_type).performClick()
        val input = views(activity.findViewById(android.R.id.content)).filterIsInstance<EditText>()
            .first { it.hint == activity.getString(R.string.data_amount_hint) }
        input.setText("0")
        button(activity, R.string.data_start).performClick()
        assertNotNull(input.error)
        input.setText("200")
        controller.configurationChange(Configuration())
        val rotated = controller.get()
        assertEquals("200", views(rotated.findViewById(android.R.id.content)).filterIsInstance<EditText>()
            .first { it.hint == rotated.getString(R.string.data_amount_hint) }.text.toString())
        captureDataCard(rotated, "05-data-volume")
        controller.pause().stop().destroy()
    }

    @Test fun `progress shows partial transfer and offers cancellation`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        setField(activity, "cellularDataState", com.example.testdialer.data.CellularDataUiState(
            busy = true, bytes = 125_000_000, targetBytes = 500_000_000, elapsedMillis = 10_000,
        ))
        val card = captureDataCard(activity, "06-data-progress")
        assertEquals(250, views(card).filterIsInstance<android.widget.ProgressBar>().single().progress)
        assertTrue(views(card).filterIsInstance<Button>().any { it.text == activity.getString(R.string.data_cancel) })
        controller.pause().stop().destroy()
    }

    @Test fun `quota dialog requires a number and does not send SMS`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        button(activity, R.string.quota_scenario).performClick()
        val dialog = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick()
        assertTrue(dialog.isShowing)
        assertTrue(views(dialog.window!!.decorView).filterIsInstance<EditText>().any { it.error != null })
        assertNull(Shadows.shadowOf(activity).nextStartedActivity)
        dialog.dismiss()
        controller.pause().stop().destroy()
    }

    private fun captureDataCard(activity: MainActivity, name: String): View {
        val card = MainActivity::class.java.getDeclaredMethod("createCellularDataScenario").apply { isAccessible = true }.invoke(activity) as View
        card.measure(View.MeasureSpec.makeMeasureSpec(336, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(1200, View.MeasureSpec.AT_MOST))
        card.layout(0, 0, 336, card.measuredHeight)
        val bitmap = Bitmap.createBitmap(336, card.height, Bitmap.Config.ARGB_8888)
        card.draw(Canvas(bitmap))
        val target = File("build/reports/screenshots/$name.png")
        target.parentFile!!.mkdirs()
        target.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
        return card
    }

    private fun capture(activity: MainActivity, name: String) {
        val root = activity.findViewById<ViewGroup>(android.R.id.content)
        root.measure(View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY))
        root.layout(0, 0, 360, 800)
        val bitmap = Bitmap.createBitmap(360, 800, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        val target = File("build/reports/screenshots/$name.png")
        target.parentFile!!.mkdirs()
        target.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
    }

    private fun sample(): StoredTestRun {
        val steps = listOf(
            ScenarioStepDefinition(StepId("sms"), 0, "Wiadomość SMS", "Wyślij", TestAction.Sms("+48123456789", "Test wiadomości")),
            ScenarioStepDefinition(StepId("data"), 1, "Transmisja danych", "Pobierz", TestAction.Data("https://example.com/test")),
        )
        val scenario = ScenarioDefinition(ScenarioId("demo"), 1, "Test taryfy weekendowej", steps = steps)
        val run = TestRun(RunId("run-demo-20260926"), scenario.id, 1, TestRunStatus.RUNNING, 1790424000000,
            events = listOf(TestEvent(EventId("event-1"), RunId("run-demo-20260926"), StepId("sms"), steps[0].action, 1790424060000,
                Observation(ObservationStatus.CONFIRMED, ObservationSource.TESTER, "USER_REPORTED_SENT"))))
        return StoredTestRun(scenario, run, 1)
    }
    private fun button(activity: MainActivity, id: Int) = views(activity.findViewById(android.R.id.content)).filterIsInstance<Button>().first { it.text == activity.getString(id) }
    private fun setField(activity: MainActivity, name: String, value: Any) = MainActivity::class.java.getDeclaredField(name).apply { isAccessible = true }.set(activity, value)
    private fun invoke(activity: MainActivity, name: String) = MainActivity::class.java.getDeclaredMethod(name).apply { isAccessible = true }.invoke(activity)
    private fun views(view: View): Sequence<View> = sequence {
        yield(view)
        if (view is ViewGroup) repeat(view.childCount) { yieldAll(views(view.getChildAt(it))) }
    }
}

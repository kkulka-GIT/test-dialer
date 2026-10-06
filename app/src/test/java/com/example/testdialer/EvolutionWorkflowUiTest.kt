package com.example.testdialer

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import com.example.testdialer.templates.TestTemplateStore
import com.example.testdialer.domain.TestAction
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowAlertDialog
import androidx.test.core.app.ApplicationProvider
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EvolutionWorkflowUiTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    @Before fun clear() {
        context.getSharedPreferences("ui-settings", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("test-templates-v1", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences(com.example.testdialer.active.ScenarioPlanStore.PREFERENCES, Context.MODE_PRIVATE).edit().clear().commit()
    }
    @After fun cleanup() { clear() }

    @Test fun `saved template opens a new editable SMS test without sending`() {
        TestTemplateStore(context).save("SMS kontrolny", TestAction.Sms("+48123456789", "Treść kontrolna"))
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        button(activity, activity.getString(R.string.run_add_test)).performClick()
        val add = ShadowAlertDialog.getLatestAlertDialog()
        add.listView.performItemClick(null, 3, 3)
        val picker = ShadowAlertDialog.getLatestAlertDialog()
        picker.listView.performItemClick(null, 0, 0)
        val use = ShadowAlertDialog.getLatestAlertDialog()
        use.listView.performItemClick(null, 0, 0)
        await { views(activity.findViewById(android.R.id.content)).filterIsInstance<EditText>().any { it.text.toString() == "Treść kontrolna" && it.isShown } }
        val edits = views(activity.findViewById(android.R.id.content)).filterIsInstance<EditText>().map { it.text.toString() }.toList()
        assertTrue(edits.contains("+48123456789"))
        assertNull(Shadows.shadowOf(activity).nextStartedActivity)
        capture(activity, "evolution-template-sms")
        button(activity, "Wróć do sesji i listy testów").performClick()
        assertTrue(button(activity, activity.getString(R.string.run_complete)).isShown)
        val preserved = views(activity.findViewById(android.R.id.content)).filterIsInstance<EditText>()
            .first { it.hint == activity.getString(R.string.sms_message_hint) }
        assertEquals("Treść kontrolna", preserved.text.toString())

        controller.pause().stop().destroy()
    }

    @Test fun `search preserves typing focus and query across recreation`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        button(activity, activity.getString(R.string.nav_register)).performClick()
        val search = views(activity.findViewById(android.R.id.content)).filterIsInstance<EditText>().first { it.hint == "Szukaj nazwy sesji lub ID" }
        search.requestFocus()
        search.setText("moja sesja")
        assertTrue(search.hasFocus())
        assertTrue(search.isShown)
        controller.configurationChange(Configuration())
        val restored = controller.get()
        val restoredSearch = views(restored.findViewById(android.R.id.content)).filterIsInstance<EditText>().first { it.hint == "Szukaj nazwy sesji lub ID" }
        assertEquals("moja sesja", restoredSearch.text.toString())
        capture(restored, "evolution-register-search")
        controller.pause().stop().destroy()
    }

    @Test fun `night theme and large fonts retain readable home actions`() {
        context.getSharedPreferences("ui-settings", Context.MODE_PRIVATE).edit().putInt("theme", 2).commit()
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        var activity = controller.get()
        assertEquals(Configuration.UI_MODE_NIGHT_YES, activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
        assertEquals(0xFF10191F.toInt(), activity.getColor(R.color.ui_background))
        capture(activity, "evolution-home-dark")
        controller.configurationChange(Configuration(activity.resources.configuration).apply { fontScale = 1.6f })
        activity = controller.get()
        capture(activity, "evolution-home-dark-large")
        views(activity.findViewById(android.R.id.content)).filterIsInstance<Button>().filter { it.isShown && it.width > 0 }.forEach { control ->
            control.layout?.let { layout -> if (layout.lineCount > 0) assertTrue("Clipped: ${control.text}", layout.getLineBottom(layout.lineCount - 1) <= control.height - control.compoundPaddingTop - control.compoundPaddingBottom) }
        }
        controller.pause().stop().destroy()
    }

    @Test fun `register pages bound rendering while search finds sessions outside current page`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        button(activity, activity.getString(R.string.nav_register)).performClick()
        await { !getRegisterState(activity).busy }
        val summaries = (0 until 53).map { index -> com.example.testdialer.persistence.TestRunSummary(
            com.example.testdialer.domain.RunId("page-$index"), "Sesja numer $index", 1,
            com.example.testdialer.domain.TestRunStatus.COMPLETED, 1000L + index, 2000L + index, 1L) }
        setRegisterState(activity, com.example.testdialer.register.RegisterUiState(runs = summaries))
        fun openCount() = views(activity.findViewById(android.R.id.content)).filterIsInstance<Button>()
            .count { it.text.toString() == activity.getString(R.string.register_open_run) }
        assertEquals(20, openCount())
        assertFalse(button(activity, "Poprzednia strona").isEnabled)
        button(activity, "Następna strona").performClick()
        assertEquals(20, openCount())
        button(activity, "Następna strona").performClick()
        assertEquals(13, openCount())
        assertFalse(button(activity, "Następna strona").isEnabled)
        val search = views(activity.findViewById(android.R.id.content)).filterIsInstance<EditText>().first { it.hint == "Szukaj nazwy sesji lub ID" }
        search.setText("page-0")
        assertEquals(1, openCount())
        assertTrue(views(activity.findViewById(android.R.id.content)).filterIsInstance<android.widget.TextView>().any { it.text.toString().contains("Sesja numer 0") })
        search.setText("")
        assertEquals(20, openCount())
        capture(activity, "phase2-register-pages")
        controller.pause().stop().destroy()
    }

    @Test fun `backup and import use system document picker without telecom action`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        button(activity, "Ustawienia").performClick()
        ShadowAlertDialog.getLatestAlertDialog().listView.performItemClick(null, 3, 3)
        val backup = ShadowAlertDialog.getLatestAlertDialog()
        assertTrue(backup.isShowing)
        backup.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val intent = Shadows.shadowOf(activity).nextStartedActivity
        assertEquals(android.content.Intent.ACTION_CREATE_DOCUMENT, intent.action)
        assertEquals("application/json", intent.type)
        button(activity, "Ustawienia").performClick()
        ShadowAlertDialog.getLatestAlertDialog().listView.performItemClick(null, 3, 3)
        val importDialog = ShadowAlertDialog.getLatestAlertDialog()
        capture(activity, "phase2-backup-settings")
        importDialog.getButton(android.app.AlertDialog.BUTTON_NEUTRAL).performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val importIntent = Shadows.shadowOf(activity).nextStartedActivity
        assertEquals(android.content.Intent.ACTION_OPEN_DOCUMENT, importIntent.action)
        assertNull(Shadows.shadowOf(activity).nextStartedActivity)
        controller.pause().stop().destroy()
    }

    @Test fun `import preview needs confirmation and cancellation never changes stored templates`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        val incoming = listOf(com.example.testdialer.templates.TestTemplate("from-file", "SMS z kopii", TestAction.Sms("123", "Test")))
        val preview = MainActivity::class.java.getDeclaredMethod("confirmTemplateImport", List::class.java).apply { isAccessible = true }
        preview.invoke(activity, incoming)
        assertTrue(TestTemplateStore(context).list().isEmpty())
        val first = ShadowAlertDialog.getLatestAlertDialog()
        first.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).performClick()
        assertTrue(TestTemplateStore(context).list().isEmpty())
        preview.invoke(activity, incoming)
        val confirm = ShadowAlertDialog.getLatestAlertDialog()
        confirm.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick()
        await { TestTemplateStore(context).list().size == 1 }
        assertEquals("SMS z kopii", TestTemplateStore(context).list().single().name)
        assertNull(Shadows.shadowOf(activity).nextStartedActivity)
        controller.pause().stop().destroy()
    }

    @Test fun `plan backup uses system document pickers without telecom action`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        button(activity, "Ustawienia").performClick()
        ShadowAlertDialog.getLatestAlertDialog().listView.performItemClick(null, 4, 4)
        ShadowAlertDialog.getLatestAlertDialog().getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertEquals(android.content.Intent.ACTION_OPEN_DOCUMENT, Shadows.shadowOf(activity).nextStartedActivity.action)
        assertNull(Shadows.shadowOf(activity).nextStartedActivity)

        val step = com.example.testdialer.domain.StepId("backup")
        com.example.testdialer.active.ScenarioPlanStore(context).save("Kopia", com.example.testdialer.active.LocalScenario("backup", "Kopia", listOf(
            com.example.testdialer.domain.ScenarioStepDefinition(step, 0, "SMS", "Wyślij", TestAction.Sms("123", "Test")))))
        button(activity, "Ustawienia").performClick()
        ShadowAlertDialog.getLatestAlertDialog().listView.performItemClick(null, 4, 4)
        ShadowAlertDialog.getLatestAlertDialog().getButton(android.app.AlertDialog.BUTTON_NEUTRAL).performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val backup = ShadowAlertDialog.getLatestAlertDialog()
        captureRoot(backup.window!!.decorView, "phase6-plan-backup")
        backup.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val create = Shadows.shadowOf(activity).nextStartedActivity
        assertEquals(android.content.Intent.ACTION_CREATE_DOCUMENT, create.action)
        assertEquals("application/json", create.type)
        assertNull(Shadows.shadowOf(activity).nextStartedActivity)
        controller.pause().stop().destroy()
    }

    @Test fun `plan import preview is cancellable and confirmation only adds parameters`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        val step = com.example.testdialer.domain.StepId("incoming")
        val incoming = listOf(com.example.testdialer.active.SavedScenarioPlan("file", "Plan z kopii", com.example.testdialer.active.LocalScenario("file", "Plan z kopii", listOf(
            com.example.testdialer.domain.ScenarioStepDefinition(step, 0, "SMS", "Wyślij osobno", TestAction.Sms("123", "Test"))))))
        val confirm = MainActivity::class.java.getDeclaredMethod("confirmPlanImport", List::class.java).apply { isAccessible = true }
        confirm.invoke(activity, incoming)
        ShadowAlertDialog.getLatestAlertDialog().getButton(android.app.AlertDialog.BUTTON_NEGATIVE).performClick()
        assertTrue(com.example.testdialer.active.ScenarioPlanStore(context).list().isEmpty())
        confirm.invoke(activity, incoming)
        ShadowAlertDialog.getLatestAlertDialog().getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick()
        await { com.example.testdialer.active.ScenarioPlanStore(context).list().size == 1 }
        assertEquals("Plan z kopii", com.example.testdialer.active.ScenarioPlanStore(context).list().single().name)
        assertNull(Shadows.shadowOf(activity).nextStartedActivity)
        controller.pause().stop().destroy()
    }

    @Test fun `full history backup uses document picker and empty import stays read only`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        button(activity, "Ustawienia").performClick()
        ShadowAlertDialog.getLatestAlertDialog().listView.performItemClick(null, 5, 5)
        val backup = ShadowAlertDialog.getLatestAlertDialog()
        captureRoot(backup.window!!.decorView, "phase7-history-backup")
        backup.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val create = Shadows.shadowOf(activity).nextStartedActivity
        assertEquals(android.content.Intent.ACTION_CREATE_DOCUMENT, create.action)
        assertEquals("application/json", create.type)
        assertNull(Shadows.shadowOf(activity).nextStartedActivity)

        button(activity, "Ustawienia").performClick()
        ShadowAlertDialog.getLatestAlertDialog().listView.performItemClick(null, 5, 5)
        ShadowAlertDialog.getLatestAlertDialog().getButton(android.app.AlertDialog.BUTTON_NEUTRAL).performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertEquals(android.content.Intent.ACTION_OPEN_DOCUMENT, Shadows.shadowOf(activity).nextStartedActivity.action)
        assertNull(Shadows.shadowOf(activity).nextStartedActivity)

        MainActivity::class.java.getDeclaredMethod("showHistoryImportPreview", List::class.java).apply {
            isAccessible = true
            invoke(activity, emptyList<com.example.testdialer.report.HistoryArchiveEntry>())
        }
        val preview = ShadowAlertDialog.getLatestAlertDialog()
        assertTrue(preview.isShowing)
        val text = views(preview.window!!.decorView).filterIsInstance<android.widget.TextView>().joinToString("\n") { it.text.toString() }
        assertTrue(text.contains("Plik nie zawiera sesji", ignoreCase = true))
        assertTrue(text.contains("Import nie uruchamia połączeń, SMS ani transferu danych"))
        assertTrue(text.contains("Sesje: 0"))
        assertEquals("Zamknij", preview.getButton(android.app.AlertDialog.BUTTON_POSITIVE).text.toString())
        assertEquals(View.GONE, preview.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).visibility)
        assertNull(Shadows.shadowOf(activity).nextStartedActivity)
        controller.pause().stop().destroy()
    }

    @Test fun `history restore requires preview confirmation and never starts telecom`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        val suffix = java.util.UUID.randomUUID().toString()
        val step = com.example.testdialer.domain.ScenarioStepDefinition(
            com.example.testdialer.domain.StepId("step-$suffix"), 0, "SMS kontrolny", "Wyślij osobno",
            TestAction.Sms("123", "Treść"),
        )
        val scenario = com.example.testdialer.domain.ScenarioDefinition(
            com.example.testdialer.domain.ScenarioId("scenario-$suffix"), 1, "Kontrola taryfy", null, listOf(step),
        )
        val runId = com.example.testdialer.domain.RunId("run-$suffix")
        val event = com.example.testdialer.domain.TestEvent(
            com.example.testdialer.domain.EventId("event-$suffix"), runId, step.id, step.action, 1_100,
            com.example.testdialer.domain.Observation(
                com.example.testdialer.domain.ObservationStatus.CONFIRMED,
                com.example.testdialer.domain.ObservationSource.TESTER, "MANUAL_OK", null,
            ),
        )
        val entry = com.example.testdialer.report.HistoryArchiveEntry(
            com.example.testdialer.persistence.StoredTestRun(
                scenario,
                com.example.testdialer.domain.TestRun(
                    runId, scenario.id, 1, com.example.testdialer.domain.TestRunStatus.COMPLETED,
                    1_000, 1_200, listOf(event),
                ),
                4,
            ),
            "Notatka", emptyMap(), 0,
        )
        val preview = MainActivity::class.java.getDeclaredMethod("showHistoryImportPreview", List::class.java)
            .apply { isAccessible = true }
        preview.invoke(activity, listOf(entry))
        var dialog = ShadowAlertDialog.getLatestAlertDialog()
        assertEquals("Przywróć historię", dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).text.toString())
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).performClick()
        val reader = java.util.concurrent.Executors.newSingleThreadExecutor()
        fun storedRun() = reader.submit<com.example.testdialer.persistence.StoredTestRun?> {
            (activity.application as TestDialerApplication).testRunRepository.get(runId)
        }.get(5, java.util.concurrent.TimeUnit.SECONDS)
        assertNull(storedRun())

        preview.invoke(activity, listOf(entry))
        dialog = ShadowAlertDialog.getLatestAlertDialog()
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick()
        await { storedRun() != null }
        assertEquals(4, storedRun()?.revision)
        reader.shutdownNow()
        assertNull(Shadows.shadowOf(activity).nextStartedActivity)
        controller.pause().stop().destroy()
    }

    @Test fun `comparison filters differences and shares a read-only text snapshot`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        val comparison = com.example.testdialer.report.RunComparison("a", "b", "Baza", "Powtórka", true, listOf(
            com.example.testdialer.report.ComparisonRow("Pole identyczne", "1", "1"),
            com.example.testdialer.report.ComparisonRow("Pole zmienione", "0 B", "100 B")))
        MainActivity::class.java.getDeclaredMethod("showComparison", com.example.testdialer.report.RunComparison::class.java).apply { isAccessible = true; invoke(activity, comparison) }
        val dialog = ShadowAlertDialog.getLatestAlertDialog()
        val root = dialog.window!!.decorView
        fun texts() = views(root).filterIsInstance<android.widget.TextView>().map { it.text.toString() }.joinToString("\n")
        assertTrue(texts().contains("Pole identyczne"))
        val toggle = views(root).filterIsInstance<Button>().first { it.text.toString() == "Pokaż tylko różnice" }
        toggle.performClick()
        assertFalse(texts().contains("Pole identyczne"))
        assertTrue(texts().contains("Pole zmienione"))
        assertTrue(texts().contains("nie potwierdza poprawności naliczenia", ignoreCase = true))
        captureRoot(root, "phase3-comparison")
        assertNull(Shadows.shadowOf(activity).nextStartedActivity)
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick()
        var started: android.content.Intent? = null
        await { started = Shadows.shadowOf(activity).nextStartedActivity; started != null }
        val send = started!!.getParcelableExtra(android.content.Intent.EXTRA_INTENT, android.content.Intent::class.java)!!
        assertEquals(android.content.Intent.ACTION_SEND, send.action)
        assertEquals("text/plain", send.type)
        assertTrue(send.flags and android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        val uri = send.getParcelableExtra(android.content.Intent.EXTRA_STREAM, android.net.Uri::class.java)!!
        val exported = activity.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
        assertTrue(exported.contains("Run A: a"))
        assertTrue(exported.contains("Pole zmienione"))
        assertFalse(exported.contains("Pole identyczne"))
        assertTrue(TestTemplateStore(context).list().isEmpty())
        controller.pause().stop().destroy()
    }

    @Test fun `comparison without another session explains what is missing without executing a service`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        await { !getRegisterState(activity).busy }
        setRegisterState(activity, com.example.testdialer.register.RegisterUiState())
        MainActivity::class.java.declaredMethods.first { it.name.startsWith("chooseComparison-") && it.parameterTypes.contentEquals(arrayOf(String::class.java)) }.apply {
            isAccessible = true
            invoke(activity, "only")
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertEquals("Zapisz co najmniej dwie sesje, aby porównać wyniki.", org.robolectric.shadows.ShadowToast.getTextOfLatestToast())
        assertNull(Shadows.shadowOf(activity).nextStartedActivity)
        controller.pause().stop().destroy()
    }

    private fun captureRoot(root: View, name: String) {
        root.measure(View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY))
        root.layout(0, 0, 360, 800)
        val bitmap = Bitmap.createBitmap(360, 800, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        val file = File("build/reports/screenshots/$name.png").apply { parentFile.mkdirs() }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun `repeat preview cancellation is harmless and known data amount opens unchanged without transfer`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        val stepId = com.example.testdialer.domain.StepId("repeat-known")
        val plan = com.example.testdialer.active.LocalScenario("test-repeat", "Powtórka kontrolna", listOf(
            com.example.testdialer.domain.ScenarioStepDefinition(stepId, 0, "1. Dane", "Sprawdź parametry", TestAction.Data("https://example.com/file"))), mapOf(stepId to 500000000L))
        val preview = MainActivity::class.java.getDeclaredMethod("showRepeatPlanPreview", com.example.testdialer.active.LocalScenario::class.java).apply { isAccessible = true }
        preview.invoke(activity, plan)
        ShadowAlertDialog.getLatestAlertDialog().getButton(android.app.AlertDialog.BUTTON_NEGATIVE).performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val stateField = MainActivity::class.java.getDeclaredField("activeRunState").apply { isAccessible = true }
        assertNull((stateField.get(activity) as com.example.testdialer.active.ActiveRunUiState).active)
        preview.invoke(activity, plan)
        ShadowAlertDialog.getLatestAlertDialog().getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick()
        await { (stateField.get(activity) as com.example.testdialer.active.ActiveRunUiState).active != null }
        val state = stateField.get(activity) as com.example.testdialer.active.ActiveRunUiState
        assertTrue(state.active!!.stored.run.events.isEmpty())
        assertEquals(com.example.testdialer.active.ActiveTaskStatus.PENDING, state.active!!.tasks.single().status)
        button(activity, activity.getString(R.string.task_open)).performClick()
        assertEquals("500000000", MainActivity::class.java.getDeclaredField("dataAmountDraft").apply { isAccessible = true }.get(activity))
        assertEquals("B", MainActivity::class.java.getDeclaredField("dataUnitDraft").apply { isAccessible = true }.get(activity))
        assertNull(Shadows.shadowOf(activity).nextStartedActivity)
        capture(activity, "phase4-repeat-data")
        controller.pause().stop().destroy()
    }

    @Test fun `repeat with unknown quantity clears old draft and blocks transfer until explicit input`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        MainActivity::class.java.getDeclaredField("dataAmountDraft").apply { isAccessible = true; set(activity, "500") }
        val stepId = com.example.testdialer.domain.StepId("repeat-unknown")
        val plan = com.example.testdialer.active.LocalScenario("test-unknown", "Powtórka bez ilości", listOf(
            com.example.testdialer.domain.ScenarioStepDefinition(stepId, 0, "1. Dane", "Podaj ilość", TestAction.Data("https://example.com/file"))), mapOf(stepId to null))
        val preview = MainActivity::class.java.getDeclaredMethod("showRepeatPlanPreview", com.example.testdialer.active.LocalScenario::class.java).apply { isAccessible = true }
        preview.invoke(activity, plan)
        ShadowAlertDialog.getLatestAlertDialog().getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick()
        val stateField = MainActivity::class.java.getDeclaredField("activeRunState").apply { isAccessible = true }
        await { (stateField.get(activity) as com.example.testdialer.active.ActiveRunUiState).active != null }
        val originalId = (stateField.get(activity) as com.example.testdialer.active.ActiveRunUiState).active!!.stored.run.id
        preview.invoke(activity, plan)
        ShadowAlertDialog.getLatestAlertDialog().getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertEquals(originalId, (stateField.get(activity) as com.example.testdialer.active.ActiveRunUiState).active!!.stored.run.id)
        button(activity, activity.getString(R.string.task_open)).performClick()
        val amount = views(activity.findViewById(android.R.id.content)).filterIsInstance<EditText>().first { it.hint == activity.getString(R.string.data_amount_hint) }
        assertEquals("", amount.text.toString())
        button(activity, activity.getString(R.string.data_start)).performClick()
        assertNotNull(amount.error)
        assertFalse((stateField.get(activity) as com.example.testdialer.active.ActiveRunUiState).executionInProgress)
        assertTrue((stateField.get(activity) as com.example.testdialer.active.ActiveRunUiState).active!!.stored.run.events.isEmpty())
        assertNull(Shadows.shadowOf(activity).nextStartedActivity)
        controller.pause().stop().destroy()
    }

    @Test fun `saved multi-step plan reopens as a fresh pending session without execution`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        val data = com.example.testdialer.domain.StepId("saved-data")
        val plan = com.example.testdialer.active.LocalScenario("save-source", "Plan źródłowy", listOf(
            com.example.testdialer.domain.ScenarioStepDefinition(com.example.testdialer.domain.StepId("saved-sms"), 0, "1. SMS", "Wyślij osobno", TestAction.Sms("123", "Kontrola")),
            com.example.testdialer.domain.ScenarioStepDefinition(data, 1, "2. Dane", "Pobierz osobno", TestAction.Data("https://example.com/file")),
        ), mapOf(data to 100_000_000L))
        val preview = MainActivity::class.java.getDeclaredMethod("showRepeatPlanPreview", com.example.testdialer.active.LocalScenario::class.java).apply { isAccessible = true }
        preview.invoke(activity, plan)
        ShadowAlertDialog.getLatestAlertDialog().getButton(android.app.AlertDialog.BUTTON_NEUTRAL).performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val save = ShadowAlertDialog.getLatestAlertDialog()
        val name = views(save.window!!.decorView).filterIsInstance<EditText>().single()
        name.setText("Plan regresyjny")
        save.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick()
        val store = com.example.testdialer.active.ScenarioPlanStore(context)
        assertEquals(listOf("Plan regresyjny"), store.list().map { it.name })
        val show = MainActivity::class.java.getDeclaredMethod("showScenarioPlans").apply { isAccessible = true }
        show.invoke(activity)
        ShadowAlertDialog.getLatestAlertDialog().listView.performItemClick(null, 0, 0)
        ShadowAlertDialog.getLatestAlertDialog().listView.performItemClick(null, 0, 0)
        val reopened = ShadowAlertDialog.getLatestAlertDialog()
        assertTrue(views(reopened.window!!.decorView).filterIsInstance<android.widget.TextView>()
            .any { it.text.toString().contains("100000000 B") })
        reopened.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick()
        val stateField = MainActivity::class.java.getDeclaredField("activeRunState").apply { isAccessible = true }
        await { (stateField.get(activity) as com.example.testdialer.active.ActiveRunUiState).active != null }
        val active = (stateField.get(activity) as com.example.testdialer.active.ActiveRunUiState).active!!
        assertEquals(2, active.tasks.size)
        assertTrue(active.tasks.all { it.status == com.example.testdialer.active.ActiveTaskStatus.PENDING })
        assertTrue(active.stored.run.events.isEmpty())
        assertNull(Shadows.shadowOf(activity).nextStartedActivity)
        capture(activity, "phase5-saved-plan")
        controller.pause().stop().destroy()
    }

    private fun getRegisterState(activity: MainActivity) = MainActivity::class.java.getDeclaredField("registerState").apply { isAccessible = true }.get(activity) as com.example.testdialer.register.RegisterUiState
    private fun setRegisterState(activity: MainActivity, state: com.example.testdialer.register.RegisterUiState) {
        MainActivity::class.java.getDeclaredField("registerState").apply { isAccessible = true; set(activity, state) }
        MainActivity::class.java.getDeclaredMethod("renderRegister").apply { isAccessible = true; invoke(activity) }
    }

    private fun button(activity: MainActivity, label: String) = views(activity.findViewById(android.R.id.content)).filterIsInstance<Button>().first { it.text.toString() == label }
    private fun views(view: View): Sequence<View> = sequence {
        yield(view)
        if (view is ViewGroup) repeat(view.childCount) { yieldAll(views(view.getChildAt(it))) }
    }
    private fun await(condition: () -> Boolean) {
        repeat(200) { Shadows.shadowOf(Looper.getMainLooper()).idle(); if (condition()) return; Thread.sleep(5) }
        assertTrue("UI did not reach expected state", condition())
    }
    private fun capture(activity: MainActivity, name: String) {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val root = activity.findViewById<View>(android.R.id.content)
        root.measure(View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY))
        root.layout(0, 0, 360, 800)
        val bitmap = Bitmap.createBitmap(360, 800, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        val file = File("build/reports/screenshots/$name.png").apply { parentFile.mkdirs() }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}

package com.example.testdialer.report

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.example.testdialer.domain.*
import com.example.testdialer.persistence.StoredTestRun
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RunReportTest {
    @Test fun `CSV quotes unicode commas newlines and neutralizes spreadsheet formulas`() {
        val csv = RunReportFormatter.csv(snapshot(), "  =HYPERLINK(\"bad\")")
        assertTrue(csv.startsWith("\"run_id\",\"scenario\""))
        assertTrue(csv.contains("Zażółć \"\"test\"\"\nDruga linia"))
        assertTrue(csv.contains("'  =HYPERLINK"))
        assertTrue(csv.contains("'+48987654321"))
        assertEquals("\"'@SUM(A1)\"", RunReportFormatter.csvCell("@SUM(A1)"))
        assertEquals("\"a,b\"", RunReportFormatter.csvCell("a,b"))
        assertTrue(csv.endsWith("\r\n"))
    }

    @Test fun `billing annotations export separately without altering service observation`() {
        val stored = snapshot()
        val reviews = mapOf("event" to com.example.testdialer.review.BillingReview("0,79 PLN", "1,58 PLN", com.example.testdialer.review.BillingVerdict.FAIL, 5000))
        val json = JSONObject(RunReportFormatter.json(stored, "", reviews, 6000))
        val annotation = json.getJSONObject("testerAnnotations")
        assertEquals("TESTER", annotation.getString("source"))
        assertEquals("FAIL", annotation.getJSONArray("billingReviews").getJSONObject(0).getString("verdict"))
        assertEquals(6000L, annotation.getLong("markedInterruptedAtMillis"))
        assertEquals("NOT_VERIFIED", json.getJSONObject("run").getJSONArray("events").getJSONObject(0).getJSONObject("observation").getString("status"))
        assertTrue(RunReportFormatter.text(stored, "", reviews).contains("OCENA ROZLICZENIA"))
        assertTrue(RunReportFormatter.csv(stored, "", reviews).contains("\"FAIL\""))
        assertEquals(snapshot(), stored)
    }

    @Test fun `CSV shares a read-only file using CSV MIME type`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = RunReportFiles.shareIntent(context, RunReportFormatter.csv(snapshot()), "csv")
        assertEquals("text/csv", intent.type)
        assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION, intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
        assertEquals(0, intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }

    private fun snapshot(): StoredTestRun {
        val scenario = ScenarioDefinition(ScenarioId("scenario"), 1, "Próba \"SIM\"", steps = listOf(
            ScenarioStepDefinition(StepId("sms"), 0, "SMS", "Wyślij", TestAction.Sms("+48123456789", "szablon")),
        ))
        val run = TestRun(RunId("run"), scenario.id, 1, TestRunStatus.COMPLETED, 1000, 2000,
            events = listOf(TestEvent(EventId("event"), RunId("run"), StepId("sms"),
                TestAction.Sms("+48987654321", "Zażółć \"test\"\nDruga linia"), 1501,
                Observation(ObservationStatus.NOT_VERIFIED, ObservationSource.TESTER, "NOT_VERIFIED"),
                CorrelationMetadata(references = listOf(CorrelationReference("sourceEventId", "source-event"))),
            )),
        )
        return StoredTestRun(scenario, run, 3)
    }

    @Test fun `editable notes survive reopening and export without altering event history`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val original = snapshot()
        val notes = com.example.testdialer.notes.RunNotesStore(context)
        val text = "Przed: 501 MB\nPo: 1 MB. Odpowiedź operatora."
        assertTrue(notes.save(original.run.id, text))
        val reopened = com.example.testdialer.notes.RunNotesStore(context)
        assertEquals(text, reopened.get(original.run.id))
        assertEquals("", reopened.get(RunId("another-run")))
        assertEquals(text, JSONObject(RunReportFormatter.json(original, text)).getString("testerNote"))
        assertTrue(RunReportFormatter.text(original, text).contains(text))
        assertTrue(notes.save(original.run.id, ""))
        assertEquals("", reopened.get(original.run.id))
        assertThrows(IllegalArgumentException::class.java) { notes.save(original.run.id, "x".repeat(4001)) }
        assertEquals(snapshot(), original)
    }

    @Test fun `JSON preserves actual values unicode nulls and exact times without changing snapshot`() {
        val original = snapshot()
        val result = JSONObject(RunReportFormatter.json(original))
        assertEquals(1, result.getInt("schemaVersion"))
        assertEquals(3L, result.getLong("revision"))
        val event = result.getJSONObject("run").getJSONArray("events").getJSONObject(0)
        assertEquals(1501L, event.getLong("occurredAtMillis"))
        assertEquals("1970-01-01T00:00:01.501Z", event.getString("occurredAtUtc"))
        assertEquals("+48987654321", event.getJSONObject("action").getString("destination"))
        assertEquals("Zażółć \"test\"\nDruga linia", event.getJSONObject("action").getString("message"))
        assertEquals("NOT_VERIFIED", event.getJSONObject("observation").getString("status"))
        assertTrue(event.getJSONObject("correlation").isNull("sourceAddress"))
        assertEquals("source-event", event.getJSONObject("correlation").getJSONArray("references").getJSONObject(0).getString("value"))
        assertEquals(snapshot(), original)
    }

    @Test fun `running empty run exports as incomplete without invented outcome`() {
        val stored = snapshot().let { it.copy(run = it.run.copy(status = TestRunStatus.RUNNING, completedAtMillis = null, events = emptyList())) }
        val run = JSONObject(RunReportFormatter.json(stored)).getJSONObject("run")
        assertTrue(run.isNull("completedAtMillis"))
        assertEquals(0, run.getJSONArray("events").length())
        val text = RunReportFormatter.text(stored)
        assertTrue(text.contains("RUNNING"))
        assertTrue(text.contains("Zdarzenia: 0"))
        assertFalse(text.contains("PASS"))
    }

    @Test fun `JSON retains timeline order attempt links and all service variants`() {
        val scenario = ScenarioDefinition(ScenarioId("timeline"), 2, "All services", steps = listOf(
            ScenarioStepDefinition(StepId("v"), 0, "Voice", "Dial", TestAction.Voice("123")),
            ScenarioStepDefinition(StepId("d"), 1, "Data", "Download", TestAction.Data("https://example.com/test")),
        ))
        val recorder = com.example.testdialer.domain.execution.TestRunRecorder.start(scenario)
        scenario.steps.forEach { step ->
            recorder.startStep(step.id)
            recorder.startAttempt()
            recorder.recordEvent()
            recorder.finishAttempt()
            recorder.finishStep()
        }
        val run = recorder.complete()
        val exported = JSONObject(RunReportFormatter.json(StoredTestRun(scenario, run, 8))).getJSONObject("run")
        val timeline = exported.getJSONArray("timeline")
        assertEquals(run.timeline.size, timeline.length())
        run.timeline.forEachIndexed { index, entry ->
            val item = timeline.getJSONObject(index)
            assertEquals(entry.sequenceNumber, item.getLong("sequenceNumber"))
            assertEquals(entry.capturedAt.epochMillis, item.getLong("epochMillis"))
            assertEquals(entry.capturedAt.monotonicNanos, item.getLong("monotonicNanos"))
            entry.attemptId?.let { assertEquals(it.value, item.getString("attemptId")) }
            entry.relatedEventId?.let { assertEquals(it.value, item.getString("relatedEventId")) }
        }
        assertEquals("VOICE", exported.getJSONArray("events").getJSONObject(0).getJSONObject("action").getString("serviceType"))
        assertEquals("https://example.com/test", exported.getJSONArray("events").getJSONObject(1).getJSONObject("action").getString("target"))
    }

    @Test fun `shared files use read only content URI and independent utf8 snapshots`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val first = RunReportFiles.shareIntent(context, "Zażółć", false)
        val second = RunReportFiles.shareIntent(context, "{}", true)
        val uri = first.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)!!
        val secondUri = second.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)!!
        assertEquals("content", uri.scheme)
        assertNotEquals(uri, secondUri)
        assertEquals("text/plain", first.type)
        assertEquals("application/json", second.type)
        assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION, first.flags)
        assertEquals(uri, first.clipData!!.getItemAt(0).uri)
        assertEquals("Zażółć", context.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() })
    }
}

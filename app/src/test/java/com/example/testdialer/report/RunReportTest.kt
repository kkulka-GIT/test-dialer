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

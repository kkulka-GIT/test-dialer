package com.example.testdialer.report

import com.example.testdialer.domain.*
import com.example.testdialer.persistence.StoredTestRun
import com.example.testdialer.review.BillingReview
import com.example.testdialer.review.BillingVerdict
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HistoryArchiveTest {
    private fun entry(runId: String = "run-1"): HistoryArchiveEntry {
        val step = ScenarioStepDefinition(StepId("step-1"), 0, "SMS kontrolny", "Wyślij i sprawdź", TestAction.Sms("123", "Treść"))
        val scenario = ScenarioDefinition(ScenarioId("scenario-1"), 1, "Kontrola taryfy", "Opis", listOf(step))
        val event = TestEvent(
            EventId("event-$runId"), RunId(runId), step.id, step.action, 1_100,
            Observation(ObservationStatus.CONFIRMED, ObservationSource.ANDROID, "SMS_COMPOSER_RETURNED", "Powrót do aplikacji"),
            CorrelationMetadata(destinationAddress = "123", references = listOf(CorrelationReference("messageId", "abc"))),
        )
        val run = TestRun(RunId(runId), scenario.id, scenario.version, TestRunStatus.COMPLETED, 1_000, 1_200, listOf(event))
        return HistoryArchiveEntry(
            StoredTestRun(scenario, run, 4), "Notatka testera",
            mapOf(event.id to BillingReview("0,79 PLN", "1,58 PLN", BillingVerdict.FAIL, 1_300)),
            0,
        )
    }

    @Test fun `round trip preserves technical observations and separate billing reviews`() {
        val source = entry()
        val text = HistoryArchive.encode(listOf(source), 2_000)
        val decoded = HistoryArchive.read(ByteArrayInputStream(text.toByteArray())).single()
        assertEquals(source, decoded)
        val event = decoded.stored.run.events.single()
        assertEquals(ObservationSource.ANDROID, event.observation?.source)
        assertEquals(BillingVerdict.FAIL, decoded.reviews.getValue(event.id).verdict)
        assertFalse(text.contains("observationVerdict"))
    }

    @Test fun `empty history is a valid versioned backup`() {
        assertTrue(HistoryArchive.decode(HistoryArchive.encode(emptyList(), 2_000)).isEmpty())
    }

    @Test fun `wrong versions malformed annotations and unknown review targets are rejected`() {
        val valid = JSONObject(HistoryArchive.encode(listOf(entry()), 2_000))
        val wrongVersion = JSONObject(valid.toString()).put("schemaVersion", 2)
        val blankNote = JSONObject(valid.toString()).apply {
            getJSONArray("runs").getJSONObject(0).put("testerNote", "x".repeat(4_001))
        }
        val missingEvent = JSONObject(valid.toString()).apply {
            getJSONArray("runs").getJSONObject(0).getJSONObject("testerAnnotations")
                .getJSONArray("billingReviews").getJSONObject(0).put("eventId", "missing")
        }
        listOf(wrongVersion, blankNote, missingEvent).forEach {
            assertThrows(Exception::class.java) { HistoryArchive.decode(it.toString()) }
        }
    }

    @Test fun `duplicate run and event identities are rejected for future additive restore`() {
        val root = JSONObject(HistoryArchive.encode(listOf(entry()), 2_000))
        root.getJSONArray("runs").put(JSONObject(root.getJSONArray("runs").getJSONObject(0).toString()))
        assertThrows(IllegalArgumentException::class.java) { HistoryArchive.decode(root.toString()) }
    }

    @Test fun `oversized stream is rejected before JSON decoding`() {
        val bytes = ByteArray(HistoryArchive.MAX_FILE_BYTES + 1) { 'x'.code.toByte() }
        assertThrows(IllegalArgumentException::class.java) { HistoryArchive.read(ByteArrayInputStream(bytes)) }
    }
}

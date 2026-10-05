package com.example.testdialer.report

import com.example.testdialer.domain.*
import com.example.testdialer.persistence.StoredTestRun
import com.example.testdialer.review.*
import org.junit.Assert.*
import org.junit.Test

class RunComparisonTest {
    private fun stored(id: String, actions: List<TestAction> = listOf(TestAction.Sms("123", "Test")),
        status: TestRunStatus = TestRunStatus.COMPLETED, end: Long? = 2000L,
        observations: List<Observation?> = List(actions.size) { null },
        references: List<List<CorrelationReference>> = List(actions.size) { emptyList() }): StoredTestRun {
        val step = ScenarioStepDefinition(StepId("step"), 0, "Test", "Wykonaj", TestAction.Voice("123"))
        val scenario = ScenarioDefinition(ScenarioId("scenario"), 1, "Sesja $id", steps = listOf(step))
        val runId = RunId(id)
        val events = actions.mapIndexed { index, action -> TestEvent(EventId("$id-$index"), runId, step.id, action, 1500, observations[index], CorrelationMetadata(references = references[index])) }
        return StoredTestRun(scenario, TestRun(runId, scenario.id, 1, status, 1000, end, events), 1)
    }
    private fun row(result: RunComparison, label: String) = result.rows.single { it.label == label }

    @Test fun `same parameters ignore ids and order but preserve repeated action multiplicity`() {
        val sms = TestAction.Sms("123", "Test")
        val voice = TestAction.Voice("456")
        assertEquals(true, RunComparisonFormatter.compare(stored("a", listOf(sms, voice)), stored("b", listOf(voice, sms))).parametersMatch)
        assertEquals(false, RunComparisonFormatter.compare(stored("a", listOf(sms, sms)), stored("b", listOf(sms))).parametersMatch)
        assertEquals(false, RunComparisonFormatter.compare(stored("a"), stored("b", listOf(sms.copy(message = "Changed")))).parametersMatch)
        assertThrows(IllegalArgumentException::class.java) { RunComparisonFormatter.compare(stored("same"), stored("same")) }
    }

    @Test fun `unknown measurement is distinct from zero and unknown inputs do not imply matched parameters`() {
        val data = TestAction.Data("https://example.com/file")
        val a = stored("a", listOf(data), references = listOf(listOf(CorrelationReference("requestedBytes", "100"))))
        val b = stored("b", listOf(data), references = listOf(listOf(CorrelationReference("requestedBytes", "100"), CorrelationReference("bytes", "0"))))
        val result = RunComparisonFormatter.compare(a, b)
        assertEquals(true, result.parametersMatch)
        assertTrue(row(result, "Dane · zapisane bajty").baseline.contains("0/1 · niepełne dane"))
        assertTrue(row(result, "Dane · zapisane bajty").compared.contains("1/1"))
        assertTrue(row(result, "Dane · zapisane bajty").different)
        assertNull(RunComparisonFormatter.compare(a, stored("c", listOf(data))).parametersMatch)
        assertNull(RunComparisonFormatter.compare(stored("e", emptyList()), stored("f", emptyList())).parametersMatch)
    }

    @Test fun `bytes sum does not overflow and ambiguous references are not counted as valid measurements`() {
        val data = TestAction.Data("https://example.com")
        val refs = listOf(CorrelationReference("bytes", Long.MAX_VALUE.toString()), CorrelationReference("requestedBytes", "1"))
        val a = stored("a", listOf(data, data), references = listOf(refs, refs))
        val b = stored("b", listOf(data), references = listOf(listOf(CorrelationReference("bytes", "1"), CorrelationReference("bytes", "2"))))
        val result = RunComparisonFormatter.compare(a, b)
        assertTrue(row(result, "Dane · zapisane bajty").baseline.contains("18446744073709551614 B"))
        assertTrue(row(result, "Dane · zapisane bajty").compared.contains("0/1 · niepełne dane"))
    }

    @Test fun `billing fail does not alter confirmed observation and descriptions compare independently`() {
        val observation = Observation(ObservationStatus.CONFIRMED, ObservationSource.ANDROID, "RECORDED")
        val a = stored("a", observations = listOf(observation))
        val b = stored("b", observations = listOf(observation))
        val reviews = mapOf("a-0" to BillingReview("0,79 PLN", "1,58 PLN", BillingVerdict.FAIL, 1), "b-0" to BillingReview("0,79 PLN", "2 PLN", BillingVerdict.FAIL, 2))
        val result = RunComparisonFormatter.compare(a, b, reviews)
        assertFalse(row(result, "Obserwacje · potwierdzone").different)
        assertEquals("1", row(result, "Ocena rozliczenia TESTER · niezgodne (FAIL)").baseline)
        assertTrue(row(result, "Opisy ocen rozliczenia TESTER").different)
        assertEquals(observation, a.run.events.single().observation)
        assertTrue(result.text().contains("Nie potwierdza", ignoreCase = true))
        assertFalse(result.text(true).contains("Obserwacje · potwierdzone"))
    }

    @Test fun `open session and inconsistent wall clock are never converted to synthetic duration`() {
        val a = stored("a", status = TestRunStatus.RUNNING, end = null)
        val b = stored("b", end = 500)
        val result = RunComparisonFormatter.compare(a, b)
        assertEquals("Brak końcowego czasu", row(result, "Czas całej sesji (ms)").baseline)
        assertEquals("Niespójny zegar", row(result, "Czas całej sesji (ms)").compared)
    }
}

package com.example.testdialer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TestRunTest {
    @Test
    fun supportsMultipleEventsForOneStep() {
        val runId = RunId("run-1")
        val stepId = StepId("voice-step")
        val events = listOf(
            event(EventId("event-1"), runId, stepId, 1_000L),
            event(EventId("event-2"), runId, stepId, 2_000L),
        )

        val run = runningRun(runId = runId, events = events)

        assertEquals(2, run.events.count { it.stepId == stepId })
    }

    @Test
    fun rejectsEventFromAnotherRun() {
        assertThrows(IllegalArgumentException::class.java) {
            runningRun(
                runId = RunId("run-1"),
                events = listOf(
                    event(EventId("event-1"), RunId("run-2"), StepId("voice-step"), 1_000L),
                ),
            )
        }
    }

    @Test
    fun expectedResultAndObservationRemainIndependent() {
        val expected = ExpectedResult("RATED", "A billing record should be rated")
        val observation = Observation(
            status = ObservationStatus.NOT_VERIFIED,
            source = ObservationSource.TESTER,
            code = "NOT_VERIFIED",
        )

        assertEquals("RATED", expected.code)
        assertEquals(ObservationStatus.NOT_VERIFIED, observation.status)
    }

    @Test
    fun rejectsCompletedRunWithoutCompletionTime() {
        assertThrows(IllegalArgumentException::class.java) {
            TestRun(
                id = RunId("run-1"),
                scenarioId = ScenarioId("scenario-1"),
                scenarioVersion = 1,
                status = TestRunStatus.COMPLETED,
                startedAtMillis = 500L,
            )
        }
    }

    @Test
    fun rejectsRunningRunWithCompletionTime() {
        assertThrows(IllegalArgumentException::class.java) {
            TestRun(
                id = RunId("run-1"),
                scenarioId = ScenarioId("scenario-1"),
                scenarioVersion = 1,
                status = TestRunStatus.RUNNING,
                startedAtMillis = 500L,
                completedAtMillis = 1_000L,
            )
        }
    }

    private fun runningRun(
        runId: RunId,
        events: List<TestEvent>,
    ) = TestRun(
        id = runId,
        scenarioId = ScenarioId("scenario-1"),
        scenarioVersion = 1,
        status = TestRunStatus.RUNNING,
        startedAtMillis = 500L,
        events = events,
    )

    private fun event(id: EventId, runId: RunId, stepId: StepId, time: Long) = TestEvent(
        id = id,
        runId = runId,
        stepId = stepId,
        action = TestAction.Voice("+48123123123"),
        occurredAtMillis = time,
    )
}

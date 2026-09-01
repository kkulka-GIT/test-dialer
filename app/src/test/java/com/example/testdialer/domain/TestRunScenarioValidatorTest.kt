package com.example.testdialer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TestRunScenarioValidatorTest {
    @Test
    fun acceptsEventsWhoseStepsAndServicesMatchScenario() {
        val scenario = scenario()
        val run = runWith(
            TestEvent(
                id = EventId("event-1"),
                runId = RUN_ID,
                stepId = StepId("voice-step"),
                action = TestAction.Voice("+48222222222"),
                occurredAtMillis = 1_000L,
            ),
        )

        assertEquals(emptyList<TestRunValidationIssue>(), TestRunScenarioValidator.validate(run, scenario))
        TestRunScenarioValidator.requireValid(run, scenario)
    }

    @Test
    fun reportsEventReferencingUnknownStep() {
        val issues = TestRunScenarioValidator.validate(
            runWith(event(stepId = StepId("unknown-step"), action = TestAction.Voice("+48111"))),
            scenario(),
        )

        assertEquals(EventId("event-1"), issues.single().eventId)
    }

    @Test
    fun reportsEventWhoseServiceDoesNotMatchStep() {
        val issues = TestRunScenarioValidator.validate(
            runWith(event(stepId = StepId("voice-step"), action = TestAction.Sms("+48111"))),
            scenario(),
        )

        assertEquals(EventId("event-1"), issues.single().eventId)
    }

    @Test
    fun requireValidRejectsScenarioIdentityMismatch() {
        val run = TestRun(
            id = RUN_ID,
            scenarioId = ScenarioId("another-scenario"),
            scenarioVersion = 1,
            status = TestRunStatus.RUNNING,
            startedAtMillis = 500L,
        )

        assertThrows(IllegalArgumentException::class.java) {
            TestRunScenarioValidator.requireValid(run, scenario())
        }
    }

    private fun scenario() = ScenarioDefinition(
        id = ScenarioId("scenario-1"),
        version = 1,
        name = "Voice scenario",
        steps = listOf(
            ScenarioStepDefinition(
                id = StepId("voice-step"),
                order = 0,
                title = "Voice",
                instruction = "Place a call",
                action = TestAction.Voice("+48123123123"),
            ),
        ),
    )

    private fun runWith(event: TestEvent) = TestRun(
        id = RUN_ID,
        scenarioId = ScenarioId("scenario-1"),
        scenarioVersion = 1,
        status = TestRunStatus.RUNNING,
        startedAtMillis = 500L,
        events = listOf(event),
    )

    private fun event(stepId: StepId, action: TestAction) = TestEvent(
        id = EventId("event-1"),
        runId = RUN_ID,
        stepId = stepId,
        action = action,
        occurredAtMillis = 1_000L,
    )

    private companion object {
        val RUN_ID = RunId("run-1")
    }
}

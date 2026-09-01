package com.example.testdialer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TestRunScenarioValidatorTest {
    @Test
    fun acceptsEventsWhoseActionsExactlyMatchScenario() {
        val scenario = scenario(
            stepId = StepId("voice-step"),
            action = TestAction.Voice("+48123123123"),
        )
        val run = runWith(
            event(
                stepId = StepId("voice-step"),
                action = TestAction.Voice("+48123123123"),
            ),
        )

        assertEquals(emptyList<TestRunValidationIssue>(), TestRunScenarioValidator.validate(run, scenario))
        TestRunScenarioValidator.requireValid(run, scenario)
    }

    @Test
    fun reportsEventReferencingUnknownStep() {
        val issues = TestRunScenarioValidator.validate(
            runWith(event(stepId = StepId("unknown-step"), action = TestAction.Voice("+48111"))),
            scenario(stepId = StepId("voice-step"), action = TestAction.Voice("+48111")),
        )

        assertEquals(EventId("event-1"), issues.single().eventId)
    }

    @Test
    fun reportsVoiceDestinationMismatch() {
        assertActionMismatch(
            scenarioAction = TestAction.Voice("+48111"),
            eventAction = TestAction.Voice("+48222"),
        )
    }

    @Test
    fun reportsSmsDestinationMismatch() {
        assertActionMismatch(
            scenarioAction = TestAction.Sms(destination = "+48111", message = "TEST"),
            eventAction = TestAction.Sms(destination = "+48222", message = "TEST"),
        )
    }

    @Test
    fun reportsSmsMessageMismatch() {
        assertActionMismatch(
            scenarioAction = TestAction.Sms(destination = "+48111", message = "TEST"),
            eventAction = TestAction.Sms(destination = "+48111", message = "OTHER"),
        )
    }

    @Test
    fun reportsDataTargetMismatch() {
        assertActionMismatch(
            scenarioAction = TestAction.Data("https://example.test/rating"),
            eventAction = TestAction.Data("https://example.test/other"),
        )
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
            TestRunScenarioValidator.requireValid(
                run,
                scenario(stepId = STEP_ID, action = TestAction.Voice("+48111")),
            )
        }
    }

    private fun assertActionMismatch(
        scenarioAction: TestAction,
        eventAction: TestAction,
    ) {
        val issues = TestRunScenarioValidator.validate(
            runWith(event(stepId = STEP_ID, action = eventAction)),
            scenario(stepId = STEP_ID, action = scenarioAction),
        )

        assertEquals(EventId("event-1"), issues.single().eventId)
        assertEquals("Event action does not match its scenario step", issues.single().message)
    }

    private fun scenario(
        stepId: StepId,
        action: TestAction,
    ) = ScenarioDefinition(
        id = ScenarioId("scenario-1"),
        version = 1,
        name = "Billing scenario",
        steps = listOf(
            ScenarioStepDefinition(
                id = stepId,
                order = 0,
                title = "Generate event",
                instruction = "Perform the configured action",
                action = action,
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
        val STEP_ID = StepId("step-1")
    }
}

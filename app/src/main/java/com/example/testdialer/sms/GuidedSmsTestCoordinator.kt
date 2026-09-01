package com.example.testdialer.sms

import com.example.testdialer.domain.Observation
import com.example.testdialer.domain.ObservationSource
import com.example.testdialer.domain.ObservationStatus
import com.example.testdialer.domain.ScenarioDefinition
import com.example.testdialer.domain.ScenarioId
import com.example.testdialer.domain.ScenarioStepDefinition
import com.example.testdialer.domain.StepId
import com.example.testdialer.domain.TestAction
import com.example.testdialer.domain.execution.TestRunRecorder
import com.example.testdialer.domain.execution.CapturedTime
import com.example.testdialer.domain.execution.SystemTimeProvider
import com.example.testdialer.domain.execution.TimeProvider
import com.example.testdialer.persistence.StoredTestRun
import com.example.testdialer.persistence.TestRunRepository
import java.util.UUID

data class GuidedSmsInput(
    val destination: String,
    val message: String,
    val label: String?,
)

enum class GuidedSmsOutcome(
    val status: ObservationStatus,
    val code: String,
) {
    USER_REPORTED_SENT(ObservationStatus.CONFIRMED, "USER_REPORTED_SMS_SENT"),
    USER_REPORTED_NOT_SENT(ObservationStatus.NOT_CONFIRMED, "USER_REPORTED_SMS_NOT_SENT"),
    NOT_VERIFIED(ObservationStatus.NOT_VERIFIED, "SMS_NOT_VERIFIED"),
}

class GuidedSmsTestCoordinator(
    private val repository: TestRunRepository,
    private val timeProvider: TimeProvider = SystemTimeProvider,
) {
    private data class Session(
        val scenario: ScenarioDefinition,
        val recorder: TestRunRecorder,
        val revision: Long,
    )

    private var session: Session? = null

    fun start(input: GuidedSmsInput): StoredTestRun {
        require(session == null) { "A guided SMS test is already active" }
        val destination = input.destination.trim()
        require(destination.isNotBlank()) { "SMS destination must not be blank" }
        require(input.message.isNotBlank()) { "SMS message must not be blank" }

        val stepId = StepId("guided-sms")
        val scenario = ScenarioDefinition(
            id = ScenarioId("guided-sms-${UUID.randomUUID()}"),
            version = 1,
            name = input.label?.trim()?.takeIf { it.isNotBlank() } ?: "Guided SMS test",
            description = "User-guided SMS test for billing/rating correlation",
            steps = listOf(
                ScenarioStepDefinition(
                    id = stepId,
                    order = 0,
                    title = "Send SMS in the system composer",
                    instruction = "Return to Test Dialer and record the tester observation.",
                    action = TestAction.Sms(destination, input.message),
                ),
            ),
        )
        val recorder = TestRunRecorder.start(scenario, timeProvider = timeProvider)
        return try {
            recorder.startStep(stepId)
            recorder.startAttempt()
            val stored = repository.saveSnapshot(scenario, recorder.snapshot())
            session = Session(scenario, recorder, stored.revision)
            stored
        } catch (error: Throwable) {
            session = null
            throw error
        }
    }

    fun recordAndComplete(outcome: GuidedSmsOutcome, observedAt: CapturedTime): StoredTestRun {
        val current = requireNotNull(session) { "No guided SMS test is active" }
        return try {
            current.recorder.recordEventAt(
                capturedAt = observedAt,
                observation = Observation(
                    status = outcome.status,
                    source = ObservationSource.TESTER,
                    code = outcome.code,
                ),
            )
            current.recorder.finishAttempt()
            current.recorder.finishStep()
            current.recorder.complete()
            repository.saveSnapshot(
                scenario = current.scenario,
                run = current.recorder.snapshot(),
                expectedRevision = current.revision,
            ).also { session = null }
        } catch (error: Throwable) {
            session = null
            throw error
        }
    }

    fun hasActiveSession(): Boolean = session != null
}

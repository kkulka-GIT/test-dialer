package com.example.testdialer.session

import com.example.testdialer.domain.Observation
import com.example.testdialer.domain.ObservationSource
import com.example.testdialer.domain.ObservationStatus
import com.example.testdialer.domain.ScenarioDefinition
import com.example.testdialer.domain.ScenarioId
import com.example.testdialer.domain.ScenarioStepDefinition
import com.example.testdialer.domain.ServiceType
import com.example.testdialer.domain.StepId
import com.example.testdialer.domain.TestAction
import com.example.testdialer.domain.execution.TestRunRecorder
import com.example.testdialer.persistence.StoredTestRun
import com.example.testdialer.persistence.TestRunRepository
import java.util.UUID

data class ManualSessionInput(
    val name: String,
    val serviceType: ServiceType,
    val target: String,
)

data class ActiveManualSession(
    val stored: StoredTestRun,
    val eventRecorded: Boolean,
)

/** Owns one in-process recorder. Persisted RUNNING sessions are intentionally not resumed. */
class ManualBillingSessionCoordinator(
    private val repository: TestRunRepository,
) {
    private data class Session(
        val scenario: ScenarioDefinition,
        val recorder: TestRunRecorder,
        var revision: Long,
        var eventRecorded: Boolean,
    )

    private var session: Session? = null

    fun start(input: ManualSessionInput): ActiveManualSession {
        require(session == null) { "A manual session is already active" }
        require(input.name.isNotBlank()) { "Session name must not be blank" }
        require(input.target.isNotBlank()) { "Target must not be blank" }

        val scenarioId = ScenarioId("manual-${UUID.randomUUID()}")
        val stepId = StepId("manual-event")
        val scenario = ScenarioDefinition(
            id = scenarioId,
            version = 1,
            name = input.name.trim(),
            description = "Manual billing/rating test session",
            steps = listOf(
                ScenarioStepDefinition(
                    id = stepId,
                    order = 0,
                    title = "Record ${input.serviceType.name} event",
                    instruction = "Perform the event manually, then record its timestamp.",
                    action = actionFor(input.serviceType, input.target.trim()),
                ),
            ),
        )
        val recorder = TestRunRecorder.start(scenario)
        return try {
            val stored = repository.saveSnapshot(scenario, recorder.snapshot())
            session = Session(scenario, recorder, stored.revision, eventRecorded = false)
            ActiveManualSession(stored, eventRecorded = false)
        } catch (error: Throwable) {
            session = null
            throw error
        }
    }

    fun recordEvent(): ActiveManualSession {
        val current = requireNotNull(session) { "No manual session is active" }
        require(!current.eventRecorded) { "The manual event is already recorded" }
        return persistOrInvalidate(current) {
            val stepId = current.scenario.steps.single().id
            current.recorder.startStep(stepId)
            current.recorder.startAttempt()
            current.recorder.recordEvent(
                observation = Observation(
                    status = ObservationStatus.NOT_VERIFIED,
                    source = ObservationSource.TESTER,
                    code = "MANUAL_EVENT_RECORDED",
                ),
            )
            current.recorder.finishAttempt()
            current.recorder.finishStep()
            current.eventRecorded = true
        }
    }

    fun complete(): ActiveManualSession {
        val current = requireNotNull(session) { "No manual session is active" }
        require(current.eventRecorded) { "Record the event before completing the session" }
        val result = persistOrInvalidate(current) { current.recorder.complete() }
        session = null
        return result
    }

    fun active(): ActiveManualSession? = session?.let {
        ActiveManualSession(
            stored = StoredTestRun(it.scenario, it.recorder.snapshot(), it.revision),
            eventRecorded = it.eventRecorded,
        )
    }

    private inline fun persistOrInvalidate(current: Session, change: () -> Unit): ActiveManualSession {
        return try {
            change()
            val stored = repository.saveSnapshot(
                scenario = current.scenario,
                run = current.recorder.snapshot(),
                expectedRevision = current.revision,
            )
            current.revision = stored.revision
            ActiveManualSession(stored, current.eventRecorded)
        } catch (error: Throwable) {
            session = null
            throw error
        }
    }

    private fun actionFor(type: ServiceType, target: String): TestAction = when (type) {
        ServiceType.VOICE -> TestAction.Voice(target)
        ServiceType.SMS -> TestAction.Sms(target)
        ServiceType.DATA -> TestAction.Data(target)
    }
}

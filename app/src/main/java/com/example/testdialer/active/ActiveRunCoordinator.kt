package com.example.testdialer.active

import com.example.testdialer.domain.CorrelationMetadata
import com.example.testdialer.domain.CorrelationReference
import com.example.testdialer.domain.Observation
import com.example.testdialer.domain.ObservationSource
import com.example.testdialer.domain.ObservationStatus
import com.example.testdialer.domain.RunId
import com.example.testdialer.domain.ScenarioDefinition
import com.example.testdialer.domain.ScenarioId
import com.example.testdialer.domain.ScenarioStepDefinition
import com.example.testdialer.domain.ServiceType
import com.example.testdialer.domain.StepId
import com.example.testdialer.domain.TestAction
import com.example.testdialer.domain.TestRunStatus
import com.example.testdialer.domain.execution.TestRunRecorder
import com.example.testdialer.domain.execution.CapturedTime
import com.example.testdialer.domain.execution.TimelineEntryKind
import com.example.testdialer.persistence.StoredTestRun
import com.example.testdialer.persistence.TestRunRepository
import java.util.UUID

enum class ActiveTaskStatus { PENDING, DONE, SKIPPED }

data class ActiveTask(
    val step: ScenarioStepDefinition,
    val status: ActiveTaskStatus,
)

data class ActiveRun(
    val stored: StoredTestRun,
    val tasks: List<ActiveTask>,
)

data class ActiveExecutionContext(
    val runId: RunId,
    val stepId: StepId?,
    val serviceType: ServiceType,
)

/** Owns exactly one in-process Run. Persisted RUNNING history is never resumed. */
class ActiveRunCoordinator(private val repository: TestRunRepository) {
    private data class Session(
        val scenario: ScenarioDefinition,
        val recorder: TestRunRecorder,
        var revision: Long,
    )

    private var session: Session? = null
    private var activeExecution: ActiveExecutionContext? = null

    @Synchronized
    fun beginExecution(stepId: StepId?, serviceType: ServiceType): ActiveExecutionContext {
        val current = requireNotNull(session) { "Brak aktywnego Run" }
        check(activeExecution == null) { "Inny test jest już wykonywany" }
        resolveTargetStep(current, stepId, serviceType)
        return ActiveExecutionContext(current.recorder.snapshot().id, stepId, serviceType)
            .also { activeExecution = it }
    }

    @Synchronized
    fun cancelExecution(context: ActiveExecutionContext) {
        if (activeExecution == context) activeExecution = null
    }

    @Synchronized
    fun executionInProgress(): Boolean = activeExecution != null

    fun startEmpty(name: String): ActiveRun = start(
        name = name.trim().ifBlank { "Pusty Run" },
        plannedSteps = emptyList(),
    )

    fun startScenario(scenario: LocalScenario): ActiveRun = start(scenario.name, scenario.steps)

    fun record(
        stepId: StepId?,
        action: TestAction,
        observation: Observation,
        correlation: CorrelationMetadata = CorrelationMetadata(),
    ): ActiveRun {
        val current = requireNotNull(session) { "Brak aktywnego Run" }
        val targetStep = resolveTargetStep(current, stepId, action.serviceType())
        return persistOrInvalidate(current) {
            current.recorder.startStep(targetStep.id)
            current.recorder.startAttempt()
            current.recorder.recordEvent(action = action, observation = observation, correlation = correlation)
            current.recorder.finishAttempt()
            current.recorder.finishStep()
        }
    }

    @Synchronized
    fun record(
        context: ActiveExecutionContext,
        action: TestAction,
        observation: Observation,
        correlation: CorrelationMetadata = CorrelationMetadata(),
        occurredAtMillis: Long? = null,
    ): ActiveRun = try {
        val current = requireNotNull(session) { "Run wykonania nie jest już aktywny" }
        check(activeExecution == context) { "Kontekst wykonania nie jest aktywny" }
        check(current.recorder.snapshot().id == context.runId) { "Wynik należy do innego Runu" }
        check(action.serviceType() == context.serviceType) { "Typ wyniku nie pasuje do rozpoczętego testu" }
        val targetStep = resolveTargetStep(current, context.stepId, context.serviceType)
        persistOrInvalidate(current) {
            current.recorder.startStep(targetStep.id)
            current.recorder.startAttempt()
            if (occurredAtMillis == null) {
                current.recorder.recordEvent(action, observation, correlation)
            } else {
                val monotonic = current.recorder.snapshot().timeline.last().capturedAt.monotonicNanos
                current.recorder.recordEventAt(CapturedTime(occurredAtMillis, monotonic), action, observation, correlation)
            }
            current.recorder.finishAttempt()
            current.recorder.finishStep()
        }
    } finally {
        activeExecution = null
    }

    @Synchronized
    fun recordExternal(context: ActiveExecutionContext, source: StoredTestRun): ActiveRun = try {
        val event = source.run.events.singleOrNull() ?: error("Test źródłowy nie zawiera jednego zdarzenia")
        val sourceReference = CorrelationReference("sourceEventId", event.id.value)
        val current = requireNotNull(session) { "Brak aktywnego Run" }
        if (current.recorder.snapshot().events.any { sourceReference in it.correlation.references }) {
            snapshot(current)
        } else {
            record(
                context = context,
                action = event.action,
                observation = event.observation ?: Observation(
                    ObservationStatus.NOT_VERIFIED,
                    ObservationSource.APPLICATION,
                    "SOURCE_EVENT_WITHOUT_OBSERVATION",
                ),
                correlation = event.correlation.copy(references = event.correlation.references + sourceReference),
                occurredAtMillis = event.occurredAtMillis,
            )
        }
    } finally {
        activeExecution = null
    }

    fun skip(stepId: StepId): ActiveRun {
        val current = requireNotNull(session) { "Brak aktywnego Run" }
        require(taskStatus(current, stepId) == ActiveTaskStatus.PENDING) { "Task nie jest oczekujący" }
        return persistOrInvalidate(current) {
            current.recorder.startStep(stepId)
            current.recorder.finishStep()
        }
    }

    @Synchronized
    fun complete(): StoredTestRun {
        val current = requireNotNull(session) { "Brak aktywnego Run" }
        check(activeExecution == null) { "Trwający test musi zostać zakończony przed zakończeniem Runu" }
        return try {
            current.recorder.complete()
            repository.saveSnapshot(current.scenario, current.recorder.snapshot(), current.revision)
                .also { session = null }
        } catch (error: Throwable) {
            session = null
            throw error
        }
    }

    @Synchronized
    fun active(): ActiveRun? = session?.let(::snapshot)

    private fun start(name: String, plannedSteps: List<ScenarioStepDefinition>): ActiveRun {
        require(session == null) { "Run jest już aktywny" }
        val scenario = ScenarioDefinition(
            id = ScenarioId("active-${UUID.randomUUID()}"),
            version = 1,
            name = name,
            description = "Active Run with optional planned Tasks",
            steps = plannedSteps + manualSlots(plannedSteps.size),
        )
        val recorder = TestRunRecorder.start(scenario)
        return try {
            val stored = repository.saveSnapshot(scenario, recorder.snapshot())
            val current = Session(scenario, recorder, stored.revision)
            session = current
            snapshot(current)
        } catch (error: Throwable) {
            session = null
            throw error
        }
    }

    private inline fun persistOrInvalidate(current: Session, change: () -> Unit): ActiveRun = try {
        change()
        val stored = repository.saveSnapshot(current.scenario, current.recorder.snapshot(), current.revision)
        current.revision = stored.revision
        snapshot(current)
    } catch (error: Throwable) {
        session = null
        throw error
    }

    private fun snapshot(current: Session): ActiveRun = ActiveRun(
        stored = StoredTestRun(current.scenario, current.recorder.snapshot(), current.revision),
        tasks = current.scenario.steps.filterNot { it.isManualSlot() }.map { ActiveTask(it, taskStatus(current, it.id)) },
    )

    private fun taskStatus(current: Session, stepId: StepId): ActiveTaskStatus {
        if (current.recorder.snapshot().events.any { it.stepId == stepId }) return ActiveTaskStatus.DONE
        return if (current.recorder.snapshot().timeline.any {
                it.kind == TimelineEntryKind.STEP_FINISHED && it.stepId == stepId
            }) ActiveTaskStatus.SKIPPED else ActiveTaskStatus.PENDING
    }

    private fun resolveTargetStep(current: Session, stepId: StepId?, serviceType: ServiceType) =
        stepId?.let { requested ->
            current.scenario.steps.singleOrNull { it.id == requested && !it.isManualSlot() }
                ?: error("Task nie należy do aktywnego Run")
        }?.also {
            require(it.action.serviceType() == serviceType) { "Typ testu nie pasuje do Tasku" }
        } ?: current.scenario.steps.single { it.id == manualStepId(serviceType) }

    private fun ScenarioStepDefinition.isManualSlot() = id.value.startsWith(MANUAL_PREFIX)

    companion object {
        private const val MANUAL_PREFIX = "manual-slot-"
        fun manualStepId(type: ServiceType) = StepId("$MANUAL_PREFIX${type.name.lowercase()}")
        private fun manualSlots(startOrder: Int) = ServiceType.entries.mapIndexed { index, type ->
            ScenarioStepDefinition(
                id = manualStepId(type),
                order = startOrder + index,
                title = "Dodatkowy ${type.name}",
                instruction = "Ręczny test dodany podczas aktywnego Runu.",
                action = when (type) {
                    ServiceType.VOICE -> TestAction.Voice("manual")
                    ServiceType.SMS -> TestAction.Sms("manual")
                    ServiceType.DATA -> TestAction.Data("manual")
                },
            )
        }
    }
}

data class LocalScenario(val id: String, val name: String, val steps: List<ScenarioStepDefinition>)

object LocalScenarioCatalog {
    val smoke = LocalScenario(
        id = "basic-smoke-v1",
        name = "Podstawowy test Voice / SMS / Data",
        steps = listOf(
            ScenarioStepDefinition(StepId("voice-domestic"), 0, "Voice krajowy", "Wykonaj połączenie testowe.", TestAction.Voice("+48123456789")),
            ScenarioStepDefinition(StepId("sms-standard"), 1, "SMS standard", "Wyślij przygotowaną wiadomość.", TestAction.Sms("+48123456789", "Test Dialer")),
            ScenarioStepDefinition(StepId("data-small"), 2, "Data HTTPS", "Wykonaj ograniczone pobranie.", TestAction.Data("https://example.com/")),
        ),
    )
    val all = listOf(smoke)
}

private fun TestAction.serviceType(): ServiceType = when (this) {
    is TestAction.Voice -> ServiceType.VOICE
    is TestAction.Sms -> ServiceType.SMS
    is TestAction.Data -> ServiceType.DATA
}

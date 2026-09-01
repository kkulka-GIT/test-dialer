package com.example.testdialer.domain.execution

import com.example.testdialer.domain.AttemptId
import com.example.testdialer.domain.CorrelationMetadata
import com.example.testdialer.domain.EventId
import com.example.testdialer.domain.Observation
import com.example.testdialer.domain.RunId
import com.example.testdialer.domain.ScenarioDefinition
import com.example.testdialer.domain.StepId
import com.example.testdialer.domain.TestEvent
import com.example.testdialer.domain.TestRun
import com.example.testdialer.domain.TestRunScenarioValidator
import com.example.testdialer.domain.TestRunStatus

class TestRunRecorder private constructor(
    private val scenario: ScenarioDefinition,
    private val timeProvider: TimeProvider,
    private val eventIdProvider: EventIdProvider,
    private val attemptIdProvider: AttemptIdProvider,
    private val timelineEntryIdProvider: TimelineEntryIdProvider,
    initialRun: TestRun,
) {
    private var currentRun: TestRun = initialRun
    private var activeStepId: StepId? = null
    private var activeAttemptId: AttemptId? = null

    fun snapshot(): TestRun = currentRun

    fun startStep(stepId: StepId): TestRun {
        requireRunning()
        require(activeStepId == null) { "Another step is already active" }
        require(scenario.steps.any { it.id == stepId }) { "Step is outside the scenario definition" }

        currentRun = appendEntry(
            kind = TimelineEntryKind.STEP_STARTED,
            capturedAt = captureNext(),
            stepId = stepId,
        )
        activeStepId = stepId
        return currentRun
    }

    fun startAttempt(): TestRun {
        requireRunning()
        val stepId = requireNotNull(activeStepId) { "A step must be active before starting an attempt" }
        require(activeAttemptId == null) { "Another attempt is already active" }
        val attemptId = attemptIdProvider.next()

        currentRun = appendEntry(
            kind = TimelineEntryKind.ATTEMPT_STARTED,
            capturedAt = captureNext(),
            stepId = stepId,
            attemptId = attemptId,
        )
        activeAttemptId = attemptId
        return currentRun
    }

    fun recordEvent(
        observation: Observation? = null,
        correlation: CorrelationMetadata = CorrelationMetadata(),
    ): TestRun {
        requireRecordingState()
        return recordEventAt(captureNext(), observation, correlation)
    }

    fun recordEventAt(
        capturedAt: CapturedTime,
        observation: Observation? = null,
        correlation: CorrelationMetadata = CorrelationMetadata(),
    ): TestRun {
        val (stepId, attemptId) = requireRecordingState()
        val step = scenario.steps.single { it.id == stepId }
        require(capturedAt.epochMillis > 0L) { "Captured epoch time must be positive" }
        val previous = currentRun.timeline.lastOrNull()?.capturedAt
        require(previous == null || capturedAt.monotonicNanos >= previous.monotonicNanos) {
            "Monotonic time must not move backwards"
        }
        val eventId = eventIdProvider.next()
        val event = TestEvent(
            id = eventId,
            runId = currentRun.id,
            stepId = stepId,
            action = step.action,
            occurredAtMillis = capturedAt.epochMillis,
            observation = observation,
            correlation = correlation,
        )
        val entry = newEntry(
            kind = TimelineEntryKind.ACTION_RECORDED,
            capturedAt = capturedAt,
            stepId = stepId,
            attemptId = attemptId,
            relatedEventId = eventId,
        )

        currentRun = currentRun.copy(
            events = currentRun.events + event,
            timeline = currentRun.timeline + entry,
        )
        TestRunScenarioValidator.requireValid(currentRun, scenario)
        return currentRun
    }

    private fun requireRecordingState(): Pair<StepId, AttemptId> {
        requireRunning()
        val stepId = requireNotNull(activeStepId) { "A step must be active before recording an event" }
        val attemptId = requireNotNull(activeAttemptId) {
            "An attempt must be active before recording an event"
        }
        return stepId to attemptId
    }

    fun finishAttempt(): TestRun {
        requireRunning()
        val stepId = requireNotNull(activeStepId) { "No step is active" }
        val attemptId = requireNotNull(activeAttemptId) { "No attempt is active" }

        currentRun = appendEntry(
            kind = TimelineEntryKind.ATTEMPT_FINISHED,
            capturedAt = captureNext(),
            stepId = stepId,
            attemptId = attemptId,
        )
        activeAttemptId = null
        return currentRun
    }

    fun finishStep(): TestRun {
        requireRunning()
        val stepId = requireNotNull(activeStepId) { "No step is active" }
        require(activeAttemptId == null) { "The active attempt must be finished first" }

        currentRun = appendEntry(
            kind = TimelineEntryKind.STEP_FINISHED,
            capturedAt = captureNext(),
            stepId = stepId,
        )
        activeStepId = null
        return currentRun
    }

    fun complete(): TestRun {
        requireRunning()
        require(activeStepId == null && activeAttemptId == null) {
            "Active step and attempt must be finished before completing the run"
        }
        val capturedAt = captureNext()
        val entry = newEntry(TimelineEntryKind.RUN_COMPLETED, capturedAt)
        currentRun = currentRun.copy(
            status = TestRunStatus.COMPLETED,
            completedAtMillis = capturedAt.epochMillis,
            timeline = currentRun.timeline + entry,
        )
        return currentRun
    }

    fun abort(): TestRun {
        requireRunning()
        val capturedAt = captureNext()
        val entry = newEntry(TimelineEntryKind.RUN_ABORTED, capturedAt)
        currentRun = currentRun.copy(
            status = TestRunStatus.ABORTED,
            completedAtMillis = capturedAt.epochMillis,
            timeline = currentRun.timeline + entry,
        )
        activeAttemptId = null
        activeStepId = null
        return currentRun
    }

    private fun appendEntry(
        kind: TimelineEntryKind,
        capturedAt: CapturedTime,
        stepId: StepId? = null,
        attemptId: AttemptId? = null,
    ): TestRun = currentRun.copy(
        timeline = currentRun.timeline + newEntry(
            kind = kind,
            capturedAt = capturedAt,
            stepId = stepId,
            attemptId = attemptId,
        ),
    )

    private fun newEntry(
        kind: TimelineEntryKind,
        capturedAt: CapturedTime,
        stepId: StepId? = null,
        attemptId: AttemptId? = null,
        relatedEventId: EventId? = null,
    ) = TimelineEntry(
        id = timelineEntryIdProvider.next(),
        runId = currentRun.id,
        sequenceNumber = currentRun.timeline.size.toLong(),
        kind = kind,
        capturedAt = capturedAt,
        stepId = stepId,
        attemptId = attemptId,
        relatedEventId = relatedEventId,
    )

    private fun captureNext(): CapturedTime {
        val captured = timeProvider.capture()
        val previous = currentRun.timeline.lastOrNull()?.capturedAt
        require(previous == null || captured.monotonicNanos >= previous.monotonicNanos) {
            "Monotonic time must not move backwards"
        }
        return captured
    }

    private fun requireRunning() {
        require(currentRun.status == TestRunStatus.RUNNING) { "Run is already terminal" }
    }

    companion object {
        fun start(
            scenario: ScenarioDefinition,
            timeProvider: TimeProvider = SystemTimeProvider,
            runIdProvider: RunIdProvider = UuidRunIdProvider,
            eventIdProvider: EventIdProvider = UuidEventIdProvider,
            attemptIdProvider: AttemptIdProvider = UuidAttemptIdProvider,
            timelineEntryIdProvider: TimelineEntryIdProvider = UuidTimelineEntryIdProvider,
        ): TestRunRecorder {
            val runId: RunId = runIdProvider.next()
            val capturedAt = timeProvider.capture()
            val startEntry = TimelineEntry(
                id = timelineEntryIdProvider.next(),
                runId = runId,
                sequenceNumber = 0L,
                kind = TimelineEntryKind.RUN_STARTED,
                capturedAt = capturedAt,
            )
            val run = TestRun(
                id = runId,
                scenarioId = scenario.id,
                scenarioVersion = scenario.version,
                status = TestRunStatus.RUNNING,
                startedAtMillis = capturedAt.epochMillis,
                timeline = listOf(startEntry),
            )
            return TestRunRecorder(
                scenario = scenario,
                timeProvider = timeProvider,
                eventIdProvider = eventIdProvider,
                attemptIdProvider = attemptIdProvider,
                timelineEntryIdProvider = timelineEntryIdProvider,
                initialRun = run,
            )
        }
    }
}

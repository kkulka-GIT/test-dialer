package com.example.testdialer.domain

import com.example.testdialer.domain.execution.TimelineEntry
import com.example.testdialer.domain.execution.TimelineEntryKind

enum class TestRunStatus {
    CREATED,
    RUNNING,
    COMPLETED,
    ABORTED,
}

data class TestEvent(
    val id: EventId,
    val runId: RunId,
    val stepId: StepId,
    val action: TestAction,
    val occurredAtMillis: Long,
    val observation: Observation? = null,
    val correlation: CorrelationMetadata = CorrelationMetadata(),
) {
    init { require(occurredAtMillis > 0L) { "Event time must be positive" } }
}

data class TestRun(
    val id: RunId,
    val scenarioId: ScenarioId,
    val scenarioVersion: Int,
    val status: TestRunStatus,
    val startedAtMillis: Long,
    val completedAtMillis: Long? = null,
    val events: List<TestEvent> = emptyList(),
    val timeline: List<TimelineEntry> = emptyList(),
) {
    init {
        require(scenarioVersion > 0) { "Scenario version must be positive" }
        require(startedAtMillis > 0L) { "Run start time must be positive" }
        require(completedAtMillis == null || completedAtMillis >= startedAtMillis) {
            "Run completion time must not precede its start"
        }
        when (status) {
            TestRunStatus.CREATED,
            TestRunStatus.RUNNING,
            -> require(completedAtMillis == null) {
                "$status run must not have a completion time"
            }
            TestRunStatus.COMPLETED,
            TestRunStatus.ABORTED,
            -> require(completedAtMillis != null) {
                "$status run must have a completion time"
            }
        }
        require(events.all { it.runId == id }) {
            "Every event must belong to this run"
        }
        require(events.map { it.id }.distinct().size == events.size) {
            "Event identifiers must be unique within a run"
        }
        validateTimeline()
    }

    private fun validateTimeline() {
        if (timeline.isEmpty()) return

        require(status != TestRunStatus.CREATED) { "A created run must not have timeline entries" }
        require(timeline.all { it.runId == id }) { "Every timeline entry must belong to this run" }
        require(timeline.map { it.id }.distinct().size == timeline.size) {
            "Timeline entry identifiers must be unique within a run"
        }
        require(timeline.map { it.sequenceNumber } == timeline.indices.map(Int::toLong)) {
            "Timeline sequence numbers must be contiguous and start at zero"
        }
        require(timeline.zipWithNext().all { (previous, next) ->
            previous.capturedAt.monotonicNanos <= next.capturedAt.monotonicNanos
        }) {
            "Timeline monotonic time must not move backwards"
        }
        require(timeline.first().kind == TimelineEntryKind.RUN_STARTED) {
            "The first timeline entry must start the run"
        }
        require(timeline.count { it.kind == TimelineEntryKind.RUN_STARTED } == 1) {
            "A timeline must contain exactly one run start"
        }

        val terminalKinds = setOf(TimelineEntryKind.RUN_COMPLETED, TimelineEntryKind.RUN_ABORTED)
        val terminalEntries = timeline.filter { it.kind in terminalKinds }
        when (status) {
            TestRunStatus.RUNNING -> require(terminalEntries.isEmpty()) {
                "A running run must not contain a terminal timeline entry"
            }
            TestRunStatus.COMPLETED -> require(
                terminalEntries.size == 1 && timeline.last().kind == TimelineEntryKind.RUN_COMPLETED,
            ) { "A completed run must end with one completion entry" }
            TestRunStatus.ABORTED -> require(
                terminalEntries.size == 1 && timeline.last().kind == TimelineEntryKind.RUN_ABORTED,
            ) { "An aborted run must end with one abort entry" }
            TestRunStatus.CREATED -> Unit
        }

        validateTimelineTransitions()

        val recordedEventIds = timeline
            .filter { it.kind == TimelineEntryKind.ACTION_RECORDED }
            .mapNotNull { it.relatedEventId }
        require(recordedEventIds.distinct().size == recordedEventIds.size) {
            "Each event may be recorded on the timeline only once"
        }
        require(recordedEventIds.toSet() == events.map { it.id }.toSet()) {
            "Timeline action entries must correspond exactly to run events"
        }
    }

    private fun validateTimelineTransitions() {
        var activeStepId: StepId? = null
        var activeAttemptId: AttemptId? = null

        timeline.drop(1).forEach { entry ->
            when (entry.kind) {
                TimelineEntryKind.RUN_STARTED -> error("Run start may only be the first entry")
                TimelineEntryKind.STEP_STARTED -> {
                    require(activeStepId == null) { "Another step is already active" }
                    activeStepId = entry.stepId
                }
                TimelineEntryKind.ATTEMPT_STARTED -> {
                    require(activeStepId == entry.stepId && activeAttemptId == null) {
                        "Attempt must start inside the active step"
                    }
                    activeAttemptId = entry.attemptId
                }
                TimelineEntryKind.ACTION_RECORDED -> require(
                    activeStepId == entry.stepId && activeAttemptId == entry.attemptId,
                ) { "Action must be recorded inside the active attempt" }
                TimelineEntryKind.ATTEMPT_FINISHED -> {
                    require(activeStepId == entry.stepId && activeAttemptId == entry.attemptId) {
                        "Only the active attempt may be finished"
                    }
                    activeAttemptId = null
                }
                TimelineEntryKind.STEP_FINISHED -> {
                    require(activeStepId == entry.stepId && activeAttemptId == null) {
                        "Only an active step without an open attempt may be finished"
                    }
                    activeStepId = null
                }
                TimelineEntryKind.RUN_COMPLETED -> require(
                    activeStepId == null && activeAttemptId == null,
                ) { "Run completion requires closed step and attempt" }
                TimelineEntryKind.RUN_ABORTED -> {
                    activeAttemptId = null
                    activeStepId = null
                }
            }
        }
    }
}

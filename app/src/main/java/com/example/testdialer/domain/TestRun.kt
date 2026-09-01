package com.example.testdialer.domain

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
    }
}

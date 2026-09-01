package com.example.testdialer.domain.execution

import com.example.testdialer.domain.AttemptId
import com.example.testdialer.domain.EventId
import com.example.testdialer.domain.RunId
import com.example.testdialer.domain.StepId
import com.example.testdialer.domain.TimelineEntryId

enum class TimelineEntryKind {
    RUN_STARTED,
    STEP_STARTED,
    ATTEMPT_STARTED,
    ACTION_RECORDED,
    ATTEMPT_FINISHED,
    STEP_FINISHED,
    RUN_COMPLETED,
    RUN_ABORTED,
}

data class TimelineEntry(
    val id: TimelineEntryId,
    val runId: RunId,
    val sequenceNumber: Long,
    val kind: TimelineEntryKind,
    val capturedAt: CapturedTime,
    val stepId: StepId? = null,
    val attemptId: AttemptId? = null,
    val relatedEventId: EventId? = null,
) {
    init {
        require(sequenceNumber >= 0L) { "Timeline sequence number must be non-negative" }
        when (kind) {
            TimelineEntryKind.RUN_STARTED,
            TimelineEntryKind.RUN_COMPLETED,
            TimelineEntryKind.RUN_ABORTED,
            -> require(stepId == null && attemptId == null && relatedEventId == null) {
                "$kind must not reference a step, attempt, or event"
            }

            TimelineEntryKind.STEP_STARTED,
            TimelineEntryKind.STEP_FINISHED,
            -> require(stepId != null && attemptId == null && relatedEventId == null) {
                "$kind must reference only a step"
            }

            TimelineEntryKind.ATTEMPT_STARTED,
            TimelineEntryKind.ATTEMPT_FINISHED,
            -> require(stepId != null && attemptId != null && relatedEventId == null) {
                "$kind must reference a step and attempt"
            }

            TimelineEntryKind.ACTION_RECORDED,
            -> require(stepId != null && attemptId != null && relatedEventId != null) {
                "$kind must reference a step, attempt, and event"
            }
        }
    }
}

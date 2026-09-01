package com.example.testdialer.domain.execution

import com.example.testdialer.domain.AttemptId
import com.example.testdialer.domain.CorrelationMetadata
import com.example.testdialer.domain.CorrelationReference
import com.example.testdialer.domain.EventId
import com.example.testdialer.domain.Observation
import com.example.testdialer.domain.ObservationSource
import com.example.testdialer.domain.ObservationStatus
import com.example.testdialer.domain.RunId
import com.example.testdialer.domain.ScenarioDefinition
import com.example.testdialer.domain.ScenarioId
import com.example.testdialer.domain.ScenarioStepDefinition
import com.example.testdialer.domain.StepId
import com.example.testdialer.domain.TestAction
import com.example.testdialer.domain.TestRunStatus
import com.example.testdialer.domain.TimelineEntryId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TestRunRecorderTest {
    @Test
    fun recordsCompleteRunWithStableSequenceAndEventLink() {
        val recorder = recorder(times(8))

        recorder.startStep(STEP_ID)
        recorder.startAttempt()
        recorder.recordEvent(
            observation = Observation(
                status = ObservationStatus.NOT_VERIFIED,
                source = ObservationSource.TESTER,
                code = "AWAITING_CDR",
            ),
            correlation = CorrelationMetadata(
                destinationAddress = "+48123123123",
                references = listOf(CorrelationReference("test.case", "VOICE-001")),
            ),
        )
        recorder.finishAttempt()
        recorder.finishStep()
        val run = recorder.complete()

        assertEquals(TestRunStatus.COMPLETED, run.status)
        assertEquals((0L..6L).toList(), run.timeline.map { it.sequenceNumber })
        assertEquals(
            listOf(
                TimelineEntryKind.RUN_STARTED,
                TimelineEntryKind.STEP_STARTED,
                TimelineEntryKind.ATTEMPT_STARTED,
                TimelineEntryKind.ACTION_RECORDED,
                TimelineEntryKind.ATTEMPT_FINISHED,
                TimelineEntryKind.STEP_FINISHED,
                TimelineEntryKind.RUN_COMPLETED,
            ),
            run.timeline.map { it.kind },
        )
        assertEquals(run.events.single().id, run.timeline[3].relatedEventId)
        assertEquals("+48123123123", run.events.single().correlation.destinationAddress)
    }

    @Test
    fun supportsTwoAttemptsForTheSameStep() {
        val recorder = recorder(times(11))
        recorder.startStep(STEP_ID)

        recorder.startAttempt()
        recorder.recordEvent()
        recorder.finishAttempt()

        recorder.startAttempt()
        recorder.recordEvent()
        recorder.finishAttempt()

        recorder.finishStep()
        val run = recorder.complete()

        val attemptStarts = run.timeline.filter { it.kind == TimelineEntryKind.ATTEMPT_STARTED }
        assertEquals(2, attemptStarts.size)
        assertEquals(2, attemptStarts.mapNotNull { it.attemptId }.distinct().size)
        assertEquals(2, run.events.size)
    }

    @Test
    fun acceptsWallClockCorrectionWhenMonotonicTimeMovesForward() {
        val suppliedTimes = listOf(
            CapturedTime(epochMillis = 2_000L, monotonicNanos = 10L),
            CapturedTime(epochMillis = 1_900L, monotonicNanos = 20L),
        )
        val recorder = recorder(suppliedTimes.iterator())

        val run = recorder.startStep(STEP_ID)

        assertEquals(1_900L, run.timeline.last().capturedAt.epochMillis)
        assertEquals(listOf(0L, 1L), run.timeline.map { it.sequenceNumber })
    }

    @Test
    fun rejectsMonotonicTimeMovingBackwards() {
        val suppliedTimes = listOf(
            CapturedTime(epochMillis = 1_000L, monotonicNanos = 20L),
            CapturedTime(epochMillis = 1_001L, monotonicNanos = 10L),
        )
        val recorder = recorder(suppliedTimes.iterator())

        assertThrows(IllegalArgumentException::class.java) {
            recorder.startStep(STEP_ID)
        }
    }

    @Test
    fun requiresAnActiveAttemptBeforeRecordingEvent() {
        val recorder = recorder(times(3))
        recorder.startStep(STEP_ID)

        assertThrows(IllegalArgumentException::class.java) {
            recorder.recordEvent()
        }
    }

    @Test
    fun recordsEventAtSuppliedUiBoundaryTime() {
        val recorder = recorder(times(7))
        recorder.startStep(STEP_ID)
        recorder.startAttempt()
        val clickTime = CapturedTime(epochMillis = 5_000L, monotonicNanos = 350L)

        val run = recorder.recordEventAt(clickTime)

        assertEquals(clickTime.epochMillis, run.events.single().occurredAtMillis)
        assertEquals(clickTime, run.timeline.last().capturedAt)
        assertEquals(TimelineEntryKind.ACTION_RECORDED, run.timeline.last().kind)
    }

    @Test
    fun rejectsSuppliedEventTimeBeforeActiveAttempt() {
        val recorder = recorder(times(4))
        recorder.startStep(STEP_ID)
        recorder.startAttempt()

        assertThrows(IllegalArgumentException::class.java) {
            recorder.recordEventAt(CapturedTime(epochMillis = 5_000L, monotonicNanos = 250L))
        }
    }

    @Test
    fun requiresAttemptToFinishBeforeStep() {
        val recorder = recorder(times(4))
        recorder.startStep(STEP_ID)
        recorder.startAttempt()

        assertThrows(IllegalArgumentException::class.java) {
            recorder.finishStep()
        }
    }

    @Test
    fun doesNotAllowChangesAfterCompletion() {
        val recorder = recorder(times(5))
        recorder.startStep(STEP_ID)
        recorder.finishStep()
        recorder.complete()

        assertThrows(IllegalArgumentException::class.java) {
            recorder.startStep(STEP_ID)
        }
    }

    @Test
    fun abortsEvenWhenAnAttemptIsOpenAndMakesRunTerminal() {
        val recorder = recorder(times(5))
        recorder.startStep(STEP_ID)
        recorder.startAttempt()

        val run = recorder.abort()

        assertEquals(TestRunStatus.ABORTED, run.status)
        assertEquals(TimelineEntryKind.RUN_ABORTED, run.timeline.last().kind)
        assertThrows(IllegalArgumentException::class.java) {
            recorder.recordEvent()
        }
    }

    @Test
    fun rejectsDuplicateTimelineEntryIdentifiers() {
        var supplied = 0
        val recorder = TestRunRecorder.start(
            scenario = scenario(),
            timeProvider = TimeProvider { times(3).next() },
            runIdProvider = RunIdProvider { RunId("run-1") },
            eventIdProvider = EventIdProvider { EventId("event-1") },
            attemptIdProvider = AttemptIdProvider { AttemptId("attempt-1") },
            timelineEntryIdProvider = TimelineEntryIdProvider {
                supplied += 1
                TimelineEntryId("same-id")
            },
        )

        assertEquals(1, supplied)
        assertThrows(IllegalArgumentException::class.java) {
            recorder.startStep(STEP_ID)
        }
    }

    @Test
    fun completesWhenWallClockMovesBackwardsButMonotonicTimeAdvances() {
        val recorder = recorder(
            listOf(
                CapturedTime(epochMillis = 2_000L, monotonicNanos = 10L),
                CapturedTime(epochMillis = 1_900L, monotonicNanos = 20L),
            ).iterator(),
        )

        val run = recorder.complete()

        assertEquals(TestRunStatus.COMPLETED, run.status)
        assertEquals(1_900L, run.completedAtMillis)
        assertEquals(listOf(0L, 1L), run.timeline.map { it.sequenceNumber })
    }

    @Test
    fun abortsWhenWallClockMovesBackwardsButMonotonicTimeAdvances() {
        val recorder = recorder(
            listOf(
                CapturedTime(epochMillis = 2_000L, monotonicNanos = 10L),
                CapturedTime(epochMillis = 1_800L, monotonicNanos = 30L),
            ).iterator(),
        )

        val run = recorder.abort()

        assertEquals(TestRunStatus.ABORTED, run.status)
        assertEquals(1_800L, run.completedAtMillis)
        assertEquals(listOf(0L, 1L), run.timeline.map { it.sequenceNumber })
    }

    @Test
    fun rejectsAttemptIdentifierReusedByLaterRetry() {
        val timeIterator = times(8)
        var timelineNumber = 0
        val recorder = TestRunRecorder.start(
            scenario = scenario(),
            timeProvider = TimeProvider { timeIterator.next() },
            runIdProvider = RunIdProvider { RunId("run-1") },
            eventIdProvider = EventIdProvider { EventId("event-1") },
            attemptIdProvider = AttemptIdProvider { AttemptId("reused-attempt") },
            timelineEntryIdProvider = TimelineEntryIdProvider {
                timelineNumber += 1
                TimelineEntryId("timeline-$timelineNumber")
            },
        )
        recorder.startStep(STEP_ID)
        recorder.startAttempt()
        recorder.finishAttempt()

        assertThrows(IllegalArgumentException::class.java) {
            recorder.startAttempt()
        }
    }

    @Test
    fun rejectsActionLinkedToEventFromAnotherStep() {
        val recorder = recorder(times(7))
        recorder.startStep(STEP_ID)
        recorder.startAttempt()
        recorder.recordEvent()
        val run = recorder.snapshot()
        val eventFromAnotherStep = run.events.single().copy(stepId = StepId("other-step"))

        assertThrows(IllegalArgumentException::class.java) {
            run.copy(events = listOf(eventFromAnotherStep))
        }
    }

    private fun recorder(timeIterator: Iterator<CapturedTime>): TestRunRecorder {
        var eventNumber = 0
        var attemptNumber = 0
        var timelineNumber = 0
        return TestRunRecorder.start(
            scenario = scenario(),
            timeProvider = TimeProvider { timeIterator.next() },
            runIdProvider = RunIdProvider { RunId("run-1") },
            eventIdProvider = EventIdProvider {
                eventNumber += 1
                EventId("event-$eventNumber")
            },
            attemptIdProvider = AttemptIdProvider {
                attemptNumber += 1
                AttemptId("attempt-$attemptNumber")
            },
            timelineEntryIdProvider = TimelineEntryIdProvider {
                timelineNumber += 1
                TimelineEntryId("timeline-$timelineNumber")
            },
        )
    }

    private fun scenario() = ScenarioDefinition(
        id = ScenarioId("voice-rating"),
        version = 1,
        name = "Voice rating",
        steps = listOf(
            ScenarioStepDefinition(
                id = STEP_ID,
                order = 0,
                title = "Voice event",
                instruction = "Place the controlled call",
                action = TestAction.Voice("+48123123123"),
            ),
        ),
    )

    private fun times(count: Int): Iterator<CapturedTime> =
        (1..count).map { index ->
            CapturedTime(
                epochMillis = 1_000L + index,
                monotonicNanos = index.toLong() * 100L,
            )
        }.iterator()

    private companion object {
        val STEP_ID = StepId("voice-step")
    }
}

package com.example.testdialer.persistence

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.testdialer.domain.AttemptId
import com.example.testdialer.domain.CorrelationMetadata
import com.example.testdialer.domain.CorrelationReference
import com.example.testdialer.domain.EventId
import com.example.testdialer.domain.ExpectedResult
import com.example.testdialer.domain.Observation
import com.example.testdialer.domain.ObservationSource
import com.example.testdialer.domain.ObservationStatus
import com.example.testdialer.domain.RunId
import com.example.testdialer.domain.ScenarioDefinition
import com.example.testdialer.domain.ScenarioId
import com.example.testdialer.domain.ScenarioStepDefinition
import com.example.testdialer.domain.StepId
import com.example.testdialer.domain.TestAction
import com.example.testdialer.domain.TestEvent
import com.example.testdialer.domain.TestRun
import com.example.testdialer.domain.TestRunStatus
import com.example.testdialer.domain.TimelineEntryId
import com.example.testdialer.domain.execution.CapturedTime
import com.example.testdialer.domain.execution.TimelineEntry
import com.example.testdialer.domain.execution.TimelineEntryKind
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TestRunPersistenceTest {
    private lateinit var database: TestDialerDatabase
    private lateinit var repository: RoomTestRunRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            TestDialerDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = RoomTestRunRepository(database.testRunDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun completeSnapshotRoundTripsEveryDomainField() {
        val scenario = scenario()
        val run = completedRun()

        val stored = repository.saveSnapshot(scenario, run)
        val loaded = repository.get(run.id)

        assertEquals(0L, stored.revision)
        assertEquals(StoredTestRun(scenario, run, 0L), loaded)
        assertEquals(listOf(run.id), repository.listSummaries().map { it.runId })
    }

    @Test
    fun revisionConflictDoesNotReplaceExistingSnapshot() {
        val scenario = scenario()
        val first = runningRun()
        val initial = repository.saveSnapshot(scenario, first)
        val extended = first.copy(
            timeline = first.timeline + timelineEntry(
                sequence = 4,
                kind = TimelineEntryKind.ATTEMPT_FINISHED,
                stepId = StepId("voice"),
                attemptId = AttemptId("attempt-1"),
            ),
        )

        val updated = repository.saveSnapshot(scenario, extended, initial.revision)
        assertEquals(1L, updated.revision)

        assertThrows(SnapshotConflictException::class.java) {
            repository.saveSnapshot(scenario, first, initial.revision)
        }
        assertEquals(extended, repository.get(first.id)?.run)
    }

    @Test
    fun scenarioConflictRollsBackWithoutChangingStoredHistory() {
        val scenario = scenario()
        val run = runningRun()
        repository.saveSnapshot(scenario, run)
        val conflicting = scenario.copy(name = "Different definition")

        assertThrows(SnapshotConflictException::class.java) {
            repository.saveSnapshot(conflicting, run, 0L)
        }
        assertEquals(scenario, repository.get(run.id)?.scenario)
        assertEquals(run, repository.get(run.id)?.run)
    }

    @Test
    fun mapperRejectsUnknownActionKind() {
        val snapshot = TestRunPersistenceMapper.toPersistence(scenario(), runningRun())
        val corrupted = snapshot.copy(
            events = snapshot.events.map { it.copy(actionKind = "UNKNOWN") },
        )

        assertThrows(IllegalArgumentException::class.java) {
            TestRunPersistenceMapper.fromPersistence(corrupted)
        }
    }

    @Test
    fun mapperRejectsUnknownEnumAndIncompleteNullableStructures() {
        val snapshot = TestRunPersistenceMapper.toPersistence(scenario(), runningRun())

        assertThrows(IllegalArgumentException::class.java) {
            TestRunPersistenceMapper.fromPersistence(
                snapshot.copy(run = snapshot.run.copy(status = "UNKNOWN")),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            TestRunPersistenceMapper.fromPersistence(
                snapshot.copy(events = snapshot.events.map {
                    it.copy(observationSource = null)
                }),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            TestRunPersistenceMapper.fromPersistence(
                snapshot.copy(scenarioSteps = snapshot.scenarioSteps.mapIndexed { index, step ->
                    if (index == 0) step.copy(expectedResultDescription = null) else step
                }),
            )
        }
    }

    @Test
    fun immutableIdentityAndIllegalStatusRollbackCompletely() {
        val original = TestRunPersistenceMapper.toPersistence(scenario(), runningRun())
        database.testRunDao().storeSnapshot(original, null)

        assertThrows(SnapshotConflictException::class.java) {
            database.testRunDao().storeSnapshot(
                original.copy(run = original.run.copy(startedAtMillis = original.run.startedAtMillis + 1)),
                0L,
            )
        }
        assertThrows(SnapshotConflictException::class.java) {
            database.testRunDao().storeSnapshot(
                original.copy(run = original.run.copy(status = "CREATED")),
                0L,
            )
        }

        assertEquals(0L, database.testRunDao().findRun(original.run.runId)?.revision)
        assertEquals(original, database.testRunDao().loadSnapshot(original.run.runId))
    }

    @Test
    fun legalExtensionReplacesChildrenWithoutDuplicates() {
        val initial = repository.saveSnapshot(scenario(), runningRun())
        val completed = completedRun()

        repository.saveSnapshot(scenario(), completed, initial.revision)

        assertEquals(completed.events.size, database.testRunDao().eventCount(completed.id.value))
        assertEquals(completed.timeline.size, database.testRunDao().timelineCount(completed.id.value))
        assertEquals(completed, repository.get(completed.id)?.run)
    }

    @Test
    fun failureAfterChildDeletionRollsBackCasAndChildren() {
        val original = TestRunPersistenceMapper.toPersistence(scenario(), runningRun())
        database.testRunDao().storeSnapshot(original, null)
        val invalidEntry = original.timeline.last().copy(
            timelineEntryId = "invalid-fk-entry",
            sequenceNumber = original.timeline.size.toLong(),
            relatedEventId = "missing-event",
        )
        val invalid = original.copy(timeline = original.timeline + invalidEntry)

        assertThrows(RuntimeException::class.java) {
            database.testRunDao().storeSnapshot(invalid, 0L)
        }

        assertEquals(original, database.testRunDao().loadSnapshot(original.run.runId))
        assertEquals(0L, database.testRunDao().findRun(original.run.runId)?.revision)
    }

    @Test
    fun terminalSnapshotIsImmutable() {
        val terminal = completedRun()
        repository.saveSnapshot(scenario(), terminal)

        assertThrows(SnapshotConflictException::class.java) {
            repository.saveSnapshot(scenario(), terminal, 0L)
        }
        assertEquals(StoredTestRun(scenario(), terminal, 0L), repository.get(terminal.id))
    }

    @Test
    fun compositeForeignKeyRejectsEventFromAnotherRun() {
        val first = TestRunPersistenceMapper.toPersistence(scenario(), runningRun())
        database.testRunDao().storeSnapshot(first, null)
        val secondRunId = "run-2"
        val second = first.copy(
            run = first.run.copy(runId = secondRunId, revision = 0L),
            events = emptyList(),
            references = emptyList(),
            timeline = listOf(
                first.timeline.first().copy(
                    timelineEntryId = "run-2-entry",
                    runId = secondRunId,
                    relatedEventId = first.events.single().eventId,
                ),
            ),
        )

        assertThrows(RuntimeException::class.java) {
            database.testRunDao().storeSnapshot(second, null)
        }
        assertNull(database.testRunDao().findRun(secondRunId))
    }

    @Test
    fun staleCasAcrossTwoDatabaseInstancesCannotOverwriteWinner() {
        database.close()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "cas-${UUID.randomUUID()}.db"
        val firstDatabase = fileDatabase(context, name)
        val secondDatabase = fileDatabase(context, name)
        try {
            val firstRepository = RoomTestRunRepository(firstDatabase.testRunDao())
            val secondRepository = RoomTestRunRepository(secondDatabase.testRunDao())
            firstRepository.saveSnapshot(scenario(), runningRun())
            val firstView = requireNotNull(firstRepository.get(RunId("run-1")))
            val secondView = requireNotNull(secondRepository.get(RunId("run-1")))
            assertEquals(0L, firstView.revision)
            assertEquals(0L, secondView.revision)

            val winner = firstRepository.saveSnapshot(
                scenario(),
                completedRun(),
                firstView.revision,
            )
            assertEquals(1L, winner.revision)
            assertThrows(SnapshotConflictException::class.java) {
                secondRepository.saveSnapshot(
                    scenario(),
                    abortedRun(),
                    secondView.revision,
                )
            }

            val finalState = secondRepository.get(RunId("run-1"))
            assertEquals(1L, finalState?.revision)
            assertEquals(completedRun(), finalState?.run)
        } finally {
            firstDatabase.close()
            secondDatabase.close()
            context.deleteDatabase(name)
            database = Room.inMemoryDatabaseBuilder(context, TestDialerDatabase::class.java)
                .allowMainThreadQueries()
                .build()
            repository = RoomTestRunRepository(database.testRunDao())
        }
    }

    private fun fileDatabase(context: Context, name: String): TestDialerDatabase =
        Room.databaseBuilder(context, TestDialerDatabase::class.java, name)
            .allowMainThreadQueries()
            .build()

    private fun scenario() = ScenarioDefinition(
        id = ScenarioId("rating-e2e"),
        version = 2,
        name = "Rating E2E",
        description = "Voice, SMS and data billing",
        steps = listOf(
            ScenarioStepDefinition(
                id = StepId("voice"),
                order = 0,
                title = "Voice",
                instruction = "Call the destination",
                action = TestAction.Voice("+48123123123"),
                expectedResult = ExpectedResult("VOICE_CDR", "One voice CDR is rated"),
            ),
            ScenarioStepDefinition(
                id = StepId("sms"),
                order = 1,
                title = "SMS",
                instruction = "Send the message",
                action = TestAction.Sms("+48999999999", "billing marker"),
                expectedResult = ExpectedResult("SMS_CDR", "One SMS CDR is rated"),
            ),
            ScenarioStepDefinition(
                id = StepId("data"),
                order = 2,
                title = "Data",
                instruction = "Fetch the target",
                action = TestAction.Data("https://example.invalid/marker"),
                expectedResult = null,
            ),
        ),
    )

    private fun runningRun(): TestRun {
        val runId = RunId("run-1")
        val eventId = EventId("event-1")
        val stepId = StepId("voice")
        val attemptId = AttemptId("attempt-1")
        val event = TestEvent(
            id = eventId,
            runId = runId,
            stepId = stepId,
            action = TestAction.Voice("+48123123123"),
            occurredAtMillis = 1_700_000_000_002L,
            observation = Observation(
                ObservationStatus.CONFIRMED,
                ObservationSource.TESTER,
                "MANUAL_OK",
                "Tester observed completion",
            ),
            correlation = CorrelationMetadata(
                sourceAddress = "subscriber-A",
                destinationAddress = "+48123123123",
                subscriberAlias = "rating-user",
                references = listOf(
                    CorrelationReference("test-marker", "abc"),
                    CorrelationReference("external-session", "xyz"),
                ),
            ),
        )
        return TestRun(
            id = runId,
            scenarioId = ScenarioId("rating-e2e"),
            scenarioVersion = 2,
            status = TestRunStatus.RUNNING,
            startedAtMillis = 1_700_000_000_000L,
            events = listOf(event),
            timeline = listOf(
                timelineEntry(0, TimelineEntryKind.RUN_STARTED),
                timelineEntry(1, TimelineEntryKind.STEP_STARTED, stepId),
                timelineEntry(2, TimelineEntryKind.ATTEMPT_STARTED, stepId, attemptId),
                timelineEntry(3, TimelineEntryKind.ACTION_RECORDED, stepId, attemptId, eventId),
            ),
        )
    }

    private fun completedRun(): TestRun {
        val running = runningRun()
        return running.copy(
            status = TestRunStatus.COMPLETED,
            completedAtMillis = 1_699_999_999_900L,
            timeline = running.timeline + listOf(
                timelineEntry(4, TimelineEntryKind.ATTEMPT_FINISHED, StepId("voice"), AttemptId("attempt-1")),
                timelineEntry(5, TimelineEntryKind.STEP_FINISHED, StepId("voice")),
                timelineEntry(6, TimelineEntryKind.RUN_COMPLETED),
            ),
        )
    }

    private fun abortedRun(): TestRun {
        val running = runningRun()
        return running.copy(
            status = TestRunStatus.ABORTED,
            completedAtMillis = 1_700_000_000_010L,
            timeline = running.timeline + timelineEntry(4, TimelineEntryKind.RUN_ABORTED),
        )
    }

    private fun timelineEntry(
        sequence: Long,
        kind: TimelineEntryKind,
        stepId: StepId? = null,
        attemptId: AttemptId? = null,
        eventId: EventId? = null,
    ) = TimelineEntry(
        id = TimelineEntryId("timeline-$sequence"),
        runId = RunId("run-1"),
        sequenceNumber = sequence,
        kind = kind,
        capturedAt = CapturedTime(
            epochMillis = 1_700_000_000_000L - sequence,
            monotonicNanos = 10_000L + sequence,
        ),
        stepId = stepId,
        attemptId = attemptId,
        relatedEventId = eventId,
    )
}

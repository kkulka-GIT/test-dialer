package com.example.testdialer.sms

import com.example.testdialer.domain.RunId
import com.example.testdialer.domain.ScenarioDefinition
import com.example.testdialer.domain.TestAction
import com.example.testdialer.domain.TestRun
import com.example.testdialer.domain.TestRunStatus
import com.example.testdialer.domain.execution.SystemTimeProvider
import com.example.testdialer.domain.execution.TimelineEntryKind
import com.example.testdialer.persistence.StoredTestRun
import com.example.testdialer.persistence.TestRunRepository
import com.example.testdialer.persistence.TestRunSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class GuidedSmsTestCoordinatorTest {
    @Test
    fun `persists full SMS action then user observation with CAS`() {
        GuidedSmsOutcome.entries.forEach { outcome ->
            val repository = FakeRepository()
            val coordinator = GuidedSmsTestCoordinator(repository)

            val started = coordinator.start(GuidedSmsInput(" +48123 ", "hello", "Roaming SMS"))
            val completed = coordinator.recordAndComplete(outcome, SystemTimeProvider.capture())

            assertEquals(TestRunStatus.RUNNING, started.run.status)
            assertEquals(
                listOf(
                    TimelineEntryKind.RUN_STARTED,
                    TimelineEntryKind.STEP_STARTED,
                    TimelineEntryKind.ATTEMPT_STARTED,
                ),
                started.run.timeline.map { it.kind },
            )
            assertEquals(TestAction.Sms("+48123", "hello"), started.scenario.steps.single().action)
            assertEquals(TestRunStatus.COMPLETED, completed.run.status)
            assertEquals(outcome.status, completed.run.events.single().observation?.status)
            assertEquals(outcome.code, completed.run.events.single().observation?.code)
            assertEquals(listOf<Long?>(null, 0L), repository.expectedRevisions)
            assertFalse(coordinator.hasActiveSession())
        }
    }

    @Test
    fun `persistence failure invalidates recorder`() {
        val repository = FakeRepository()
        val coordinator = GuidedSmsTestCoordinator(repository)
        coordinator.start(GuidedSmsInput("123", "hello", null))
        repository.failNext = true

        runCatching {
            coordinator.recordAndComplete(GuidedSmsOutcome.NOT_VERIFIED, SystemTimeProvider.capture())
        }

        assertFalse(coordinator.hasActiveSession())
    }

    @Test
    fun `persisted active attempt is read only after process recreation`() {
        val repository = FakeRepository()
        GuidedSmsTestCoordinator(repository).start(GuidedSmsInput("123", "hello", null))

        val recreated = GuidedSmsTestCoordinator(repository)

        assertFalse(recreated.hasActiveSession())
        assertEquals(3, repository.lastStored!!.run.timeline.size)
        runCatching {
            recreated.recordAndComplete(GuidedSmsOutcome.NOT_VERIFIED, SystemTimeProvider.capture())
        }.onSuccess { error("Recreated coordinator must not resume the active attempt") }
    }

    private class FakeRepository : TestRunRepository {
        val expectedRevisions = mutableListOf<Long?>()
        var failNext = false
        var lastStored: StoredTestRun? = null

        override fun saveSnapshot(
            scenario: ScenarioDefinition,
            run: TestRun,
            expectedRevision: Long?,
        ): StoredTestRun {
            if (failNext) error("disk unavailable")
            expectedRevisions += expectedRevision
            return StoredTestRun(scenario, run, (expectedRevision ?: -1L) + 1L)
                .also { lastStored = it }
        }

        override fun get(runId: RunId): StoredTestRun? = null
        override fun listSummaries(): List<TestRunSummary> = emptyList()
    }
}

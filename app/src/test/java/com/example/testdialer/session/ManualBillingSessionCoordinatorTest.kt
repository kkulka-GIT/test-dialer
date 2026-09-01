package com.example.testdialer.session

import com.example.testdialer.domain.RunId
import com.example.testdialer.domain.ServiceType
import com.example.testdialer.domain.TestRun
import com.example.testdialer.domain.TestRunStatus
import com.example.testdialer.domain.ScenarioDefinition
import com.example.testdialer.persistence.StoredTestRun
import com.example.testdialer.persistence.TestRunRepository
import com.example.testdialer.persistence.TestRunSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualBillingSessionCoordinatorTest {
    @Test
    fun `start record and complete persist monotonic snapshots`() {
        val repository = FakeRepository()
        val coordinator = ManualBillingSessionCoordinator(repository)

        val started = coordinator.start(ManualSessionInput("Roaming SMS", ServiceType.SMS, "+48123"))
        val recorded = coordinator.recordEvent()
        val completed = coordinator.complete()

        assertEquals(TestRunStatus.RUNNING, started.stored.run.status)
        assertFalse(started.eventRecorded)
        assertEquals(1, recorded.stored.run.events.size)
        assertTrue(recorded.eventRecorded)
        assertEquals(TestRunStatus.COMPLETED, completed.stored.run.status)
        assertEquals(listOf<Long?>(null, 0L, 1L), repository.expectedRevisions)
        assertNull(coordinator.active())
    }

    @Test
    fun `persistence failure invalidates mutable in-process session`() {
        val repository = FakeRepository()
        val coordinator = ManualBillingSessionCoordinator(repository)
        coordinator.start(ManualSessionInput("Data", ServiceType.DATA, "https://example.test"))
        repository.failNext = true

        runCatching { coordinator.recordEvent() }

        assertNull(coordinator.active())
    }

    private class FakeRepository : TestRunRepository {
        val expectedRevisions = mutableListOf<Long?>()
        var failNext = false
        private val stored = linkedMapOf<RunId, StoredTestRun>()

        override fun saveSnapshot(
            scenario: ScenarioDefinition,
            run: TestRun,
            expectedRevision: Long?,
        ): StoredTestRun {
            if (failNext) {
                failNext = false
                error("disk unavailable")
            }
            expectedRevisions += expectedRevision
            val next = StoredTestRun(scenario, run, (expectedRevision ?: -1L) + 1L)
            stored[run.id] = next
            return next
        }

        override fun get(runId: RunId): StoredTestRun? = stored[runId]

        override fun listSummaries(): List<TestRunSummary> = stored.values.map {
            TestRunSummary(
                it.run.id,
                it.scenario.name,
                it.scenario.version,
                it.run.status,
                it.run.startedAtMillis,
                it.run.completedAtMillis,
                it.revision,
            )
        }
    }
}

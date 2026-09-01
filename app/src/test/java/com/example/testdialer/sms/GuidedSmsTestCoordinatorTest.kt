package com.example.testdialer.sms

import com.example.testdialer.domain.RunId
import com.example.testdialer.domain.ScenarioDefinition
import com.example.testdialer.domain.TestAction
import com.example.testdialer.domain.TestRun
import com.example.testdialer.domain.TestRunStatus
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
            val completed = coordinator.recordAndComplete(outcome)

            assertEquals(TestRunStatus.RUNNING, started.run.status)
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

        runCatching { coordinator.recordAndComplete(GuidedSmsOutcome.NOT_VERIFIED) }

        assertFalse(coordinator.hasActiveSession())
    }

    private class FakeRepository : TestRunRepository {
        val expectedRevisions = mutableListOf<Long?>()
        var failNext = false

        override fun saveSnapshot(
            scenario: ScenarioDefinition,
            run: TestRun,
            expectedRevision: Long?,
        ): StoredTestRun {
            if (failNext) error("disk unavailable")
            expectedRevisions += expectedRevision
            return StoredTestRun(scenario, run, (expectedRevision ?: -1L) + 1L)
        }

        override fun get(runId: RunId): StoredTestRun? = null
        override fun listSummaries(): List<TestRunSummary> = emptyList()
    }
}

package com.example.testdialer.active

import com.example.testdialer.domain.Observation
import com.example.testdialer.domain.ObservationSource
import com.example.testdialer.domain.ObservationStatus
import com.example.testdialer.domain.RunId
import com.example.testdialer.domain.ScenarioDefinition
import com.example.testdialer.domain.ServiceType
import com.example.testdialer.domain.TestAction
import com.example.testdialer.domain.TestRun
import com.example.testdialer.domain.TestRunStatus
import com.example.testdialer.persistence.StoredTestRun
import com.example.testdialer.persistence.TestRunRepository
import com.example.testdialer.persistence.TestRunSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class ActiveRunCoordinatorTest {
    private val repository = MemoryRepository()

    @Test
    fun `empty Run accepts multiple manual service Events and completes without rewriting history`() {
        val coordinator = ActiveRunCoordinator(repository)
        val started = coordinator.startEmpty("Run ad hoc")

        assertTrue(started.tasks.isEmpty())
        val runId = started.stored.run.id
        coordinator.record(null, TestAction.Voice("+48111"), observation("VOICE"))
        coordinator.record(null, TestAction.Sms("+48222", "test"), observation("SMS"))
        val completed = coordinator.complete()

        assertEquals(runId, completed.run.id)
        assertEquals(TestRunStatus.COMPLETED, completed.run.status)
        assertEquals(listOf(TestAction.Voice("+48111"), TestAction.Sms("+48222", "test")), completed.run.events.map { it.action })
        assertEquals(2, repository.snapshots.values.single().run.events.size)
        assertNull(coordinator.active())
    }

    @Test
    fun `Scenario tasks are independent and persist done skipped and pending state`() {
        val coordinator = ActiveRunCoordinator(repository)
        val active = coordinator.startScenario(LocalScenarioCatalog.smoke)
        val voice = active.tasks[0]
        val sms = active.tasks[1]

        coordinator.skip(sms.step.id)
        val updated = coordinator.record(voice.step.id, TestAction.Voice("+48999"), observation("VOICE"))

        assertEquals(ActiveTaskStatus.DONE, updated.tasks[0].status)
        assertEquals(ActiveTaskStatus.SKIPPED, updated.tasks[1].status)
        assertEquals(ActiveTaskStatus.PENDING, updated.tasks[2].status)
        assertEquals("+48999", (updated.stored.run.events.single().action as TestAction.Voice).destination)
    }

    @Test
    fun `persisted running Run is not resumed by a new coordinator`() {
        val first = ActiveRunCoordinator(repository)
        val persisted = first.startScenario(LocalScenarioCatalog.smoke).stored

        val afterProcessDeath = ActiveRunCoordinator(repository)

        assertNull(afterProcessDeath.active())
        assertEquals(TestRunStatus.RUNNING, repository.get(persisted.run.id)?.run?.status)
    }

    @Test
    fun `async result stays bound to Run and Task selected when execution began`() {
        val coordinator = ActiveRunCoordinator(repository)
        val active = coordinator.startScenario(LocalScenarioCatalog.smoke)
        val sms = active.tasks[1]
        val context = coordinator.beginExecution(sms.step.id, ServiceType.SMS)

        assertThrows(IllegalStateException::class.java) { coordinator.complete() }
        assertThrows(IllegalStateException::class.java) {
            coordinator.beginExecution(active.tasks[2].step.id, ServiceType.DATA)
        }

        val updated = coordinator.record(
            context,
            TestAction.Sms("+48999", "actual"),
            observation("SMS"),
        )

        assertEquals(sms.step.id, updated.stored.run.events.single().stepId)
        assertEquals(ActiveTaskStatus.DONE, updated.tasks[1].status)
        assertEquals(ActiveTaskStatus.PENDING, updated.tasks[2].status)
        assertEquals(TestRunStatus.COMPLETED, coordinator.complete().run.status)
    }

    @Test
    fun `external Event preserves source correlation time`() {
        val coordinator = ActiveRunCoordinator(repository)
        val active = coordinator.startScenario(LocalScenarioCatalog.smoke)
        val data = active.tasks[2]
        val context = coordinator.beginExecution(data.step.id, ServiceType.DATA)
        val source = sourceRun(
            action = TestAction.Data("https://example.com/result"),
            occurredAtMillis = 1_234_567L,
        )

        val imported = coordinator.recordExternal(context, source)

        assertEquals(1_234_567L, imported.stored.run.events.single().occurredAtMillis)
        assertEquals(data.step.id, imported.stored.run.events.single().stepId)
    }

    private fun observation(code: String) = Observation(ObservationStatus.NOT_VERIFIED, ObservationSource.TESTER, code)

    private fun sourceRun(action: TestAction, occurredAtMillis: Long): StoredTestRun {
        val sourceRepository = MemoryRepository()
        val sourceCoordinator = ActiveRunCoordinator(sourceRepository)
        sourceCoordinator.startEmpty("source")
        val context = sourceCoordinator.beginExecution(null, action.serviceTypeForTest())
        sourceCoordinator.record(context, action, observation("SOURCE"), occurredAtMillis = occurredAtMillis)
        return sourceCoordinator.complete()
    }

    private fun TestAction.serviceTypeForTest() = when (this) {
        is TestAction.Voice -> ServiceType.VOICE
        is TestAction.Sms -> ServiceType.SMS
        is TestAction.Data -> ServiceType.DATA
    }

    private class MemoryRepository : TestRunRepository {
        val snapshots = linkedMapOf<RunId, StoredTestRun>()
        override fun saveSnapshot(scenario: ScenarioDefinition, run: TestRun, expectedRevision: Long?): StoredTestRun {
            val existing = snapshots[run.id]
            if (existing == null) require(expectedRevision == null) else require(existing.revision == expectedRevision)
            val stored = StoredTestRun(scenario, run, (existing?.revision ?: -1L) + 1L)
            snapshots[run.id] = stored
            return stored
        }
        override fun get(runId: RunId) = snapshots[runId]
        override fun listSummaries() = snapshots.values.map {
            TestRunSummary(it.run.id, it.scenario.name, it.scenario.version, it.run.status, it.run.startedAtMillis, it.run.completedAtMillis, it.revision)
        }
    }
}

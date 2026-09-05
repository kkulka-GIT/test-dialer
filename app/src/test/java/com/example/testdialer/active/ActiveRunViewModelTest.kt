package com.example.testdialer.active

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.testdialer.VoiceTestResult
import com.example.testdialer.domain.RunId
import com.example.testdialer.domain.ScenarioDefinition
import com.example.testdialer.domain.ServiceType
import com.example.testdialer.domain.TestAction
import com.example.testdialer.domain.TestRun
import com.example.testdialer.persistence.StoredTestRun
import com.example.testdialer.persistence.TestRunRepository
import com.example.testdialer.persistence.TestRunSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.RejectedExecutionException

class ActiveRunViewModelTest {
    @get:Rule val instant = InstantTaskExecutorRule()

    @Test
    fun `active execution blocks Run mutations and result remains attached to original Task`() {
        val repository = MemoryRepository()
        val coordinator = ActiveRunCoordinator(repository)
        val executor = QueuedExecutorService()
        val viewModel = ActiveRunViewModel(coordinator, executor)

        viewModel.startScenario(LocalScenarioCatalog.smoke)
        executor.runNext()
        val active = viewModel.state.value!!.active!!
        val voice = active.tasks[0]
        val data = active.tasks[2]
        viewModel.beginExecution(voice.step.id, ServiceType.VOICE)
        assertTrue(viewModel.state.value!!.executionInProgress)

        viewModel.skip(data.step.id)
        viewModel.complete()
        assertEquals(0, executor.queuedCount)
        assertEquals(ActiveTaskStatus.PENDING, viewModel.state.value!!.active!!.tasks[2].status)
        viewModel.recordVoice("+48999", VoiceTestResult.Outcome.SUCCESS)

        assertEquals(1, executor.queuedCount)
        executor.runNext()

        assertFalse(viewModel.executionInProgress())
        assertFalse(viewModel.state.value!!.executionInProgress)
        val stored = repository.snapshots.values.single()
        assertEquals(voice.step.id, stored.run.events.single().stepId)
        assertEquals(TestAction.Voice("+48999"), stored.run.events.single().action)
        assertEquals(ActiveTaskStatus.DONE, viewModel.state.value!!.active!!.tasks[0].status)
        assertEquals(ActiveTaskStatus.PENDING, viewModel.state.value!!.active!!.tasks[2].status)
        assertFalse(viewModel.state.value!!.busy)
    }

    @Test
    fun `failed result persistence releases context and does not prevent a new Run`() {
        val repository = MemoryRepository()
        val coordinator = ActiveRunCoordinator(repository)
        val executor = QueuedExecutorService()
        val viewModel = ActiveRunViewModel(coordinator, executor)
        viewModel.startScenario(LocalScenarioCatalog.smoke)
        executor.runNext()
        val active = viewModel.state.value!!.active!!
        viewModel.beginExecution(active.tasks[0].step.id, ServiceType.VOICE)

        repository.failNext = true
        viewModel.recordVoice("+48999", VoiceTestResult.Outcome.SUCCESS)
        executor.runNext()

        assertFalse(viewModel.executionInProgress())
        assertFalse(viewModel.state.value!!.executionInProgress)
        assertTrue(viewModel.state.value!!.error!!.contains("simulated save failure"))
        viewModel.startEmpty("Run after failure")
        executor.runNext()
        assertTrue(viewModel.state.value!!.active != null)
        viewModel.complete()
        executor.runNext()
        assertFalse(viewModel.executionInProgress())
        assertEquals(null, viewModel.state.value!!.active)
    }

    @Test
    fun `executor rejection releases execution context instead of locking Run`() {
        val repository = MemoryRepository()
        val coordinator = ActiveRunCoordinator(repository)
        val executor = QueuedExecutorService()
        val viewModel = ActiveRunViewModel(coordinator, executor)
        viewModel.startScenario(LocalScenarioCatalog.smoke)
        executor.runNext()
        viewModel.beginExecution(viewModel.state.value!!.active!!.tasks[0].step.id, ServiceType.VOICE)
        executor.reject = true

        viewModel.recordVoice("+48999", VoiceTestResult.Outcome.SUCCESS)

        assertFalse(viewModel.executionInProgress())
        assertTrue(viewModel.state.value!!.error!!.contains("executor"))
    }

    private class MemoryRepository : TestRunRepository {
        val snapshots = linkedMapOf<RunId, StoredTestRun>()
        var failNext = false
        override fun saveSnapshot(scenario: ScenarioDefinition, run: TestRun, expectedRevision: Long?): StoredTestRun {
            if (failNext) {
                failNext = false
                error("simulated save failure")
            }
            val existing = snapshots[run.id]
            if (existing == null) require(expectedRevision == null) else require(existing.revision == expectedRevision)
            return StoredTestRun(scenario, run, (existing?.revision ?: -1L) + 1L).also { snapshots[run.id] = it }
        }
        override fun get(runId: RunId) = snapshots[runId]
        override fun listSummaries(): List<TestRunSummary> = emptyList()
    }

    private class QueuedExecutorService : AbstractExecutorService() {
        private val tasks = ArrayDeque<Runnable>()
        private var stopped = false
        var reject = false
        val queuedCount get() = tasks.size
        override fun execute(command: Runnable) {
            if (reject) throw RejectedExecutionException("simulated rejection")
            tasks.addLast(command)
        }
        fun runNext() = tasks.removeFirst().run()
        override fun shutdown() { stopped = true }
        override fun shutdownNow(): MutableList<Runnable> = tasks.toMutableList().also { tasks.clear(); stopped = true }
        override fun isShutdown() = stopped
        override fun isTerminated() = stopped && tasks.isEmpty()
        override fun awaitTermination(timeout: Long, unit: TimeUnit) = isTerminated
    }
}

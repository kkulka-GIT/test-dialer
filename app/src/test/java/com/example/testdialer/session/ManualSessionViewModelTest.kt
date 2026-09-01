package com.example.testdialer.session

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.testdialer.domain.RunId
import com.example.testdialer.domain.ScenarioDefinition
import com.example.testdialer.domain.ServiceType
import com.example.testdialer.domain.TestRun
import com.example.testdialer.persistence.StoredTestRun
import com.example.testdialer.persistence.TestRunRepository
import com.example.testdialer.persistence.TestRunSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit

class ManualSessionViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun `busy state prevents duplicate start submission`() {
        val repository = FakeRepository()
        val executor = QueueExecutorService()
        val viewModel = ManualSessionViewModel(
            repository,
            ManualBillingSessionCoordinator(repository),
            executor,
        )

        viewModel.start("Voice rating", ServiceType.VOICE, "+48123")
        viewModel.start("Duplicate", ServiceType.VOICE, "+48456")

        assertTrue(viewModel.state.value!!.busy)
        assertEquals(1, executor.queuedCount)
        executor.runNext()
        assertFalse(viewModel.state.value!!.busy)
        assertNotNull(viewModel.state.value!!.active)
        assertEquals(1, repository.saveCount)
    }

    @Test
    fun `repository error is explicit and leaves no active recorder`() {
        val repository = FakeRepository().apply { fail = true }
        val executor = QueueExecutorService()
        val viewModel = ManualSessionViewModel(
            repository,
            ManualBillingSessionCoordinator(repository),
            executor,
        )

        viewModel.start("Data rating", ServiceType.DATA, "https://example.test")
        executor.runNext()

        assertTrue(viewModel.state.value!!.error!!.contains("Could not start"))
        assertEquals(null, viewModel.state.value!!.active)
    }

    private class FakeRepository : TestRunRepository {
        var fail = false
        var saveCount = 0
        private val snapshots = linkedMapOf<RunId, StoredTestRun>()

        override fun saveSnapshot(
            scenario: ScenarioDefinition,
            run: TestRun,
            expectedRevision: Long?,
        ): StoredTestRun {
            saveCount++
            if (fail) error("storage failure")
            val stored = StoredTestRun(scenario, run, (expectedRevision ?: -1L) + 1L)
            snapshots[run.id] = stored
            return stored
        }

        override fun get(runId: RunId): StoredTestRun? = snapshots[runId]
        override fun listSummaries(): List<TestRunSummary> = emptyList()
    }

    private class QueueExecutorService : AbstractExecutorService() {
        private val tasks = ArrayDeque<Runnable>()
        private var shutdown = false
        val queuedCount: Int get() = tasks.size

        override fun execute(command: Runnable) { tasks.addLast(command) }
        fun runNext() { tasks.removeFirst().run() }
        override fun shutdown() { shutdown = true }
        override fun shutdownNow(): MutableList<Runnable> = tasks.toMutableList().also { tasks.clear() }
        override fun isShutdown(): Boolean = shutdown
        override fun isTerminated(): Boolean = shutdown && tasks.isEmpty()
        override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean = isTerminated
    }
}

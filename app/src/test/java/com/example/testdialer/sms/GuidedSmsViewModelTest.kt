package com.example.testdialer.sms

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.testdialer.domain.RunId
import com.example.testdialer.domain.ScenarioDefinition
import com.example.testdialer.domain.TestRun
import com.example.testdialer.persistence.StoredTestRun
import com.example.testdialer.persistence.TestRunRepository
import com.example.testdialer.persistence.TestRunSummary
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit

class GuidedSmsViewModelTest {
    @get:Rule val instant = InstantTaskExecutorRule()
    private val executor = DirectExecutorService()

    @After fun tearDown() = executor.shutdown()

    @Test
    fun `start requests composer and return requests observation`() {
        val viewModel = GuidedSmsViewModel(GuidedSmsTestCoordinator(FakeRepository()), executor)

        viewModel.start(GuidedSmsInput("123", "hello", null))
        assertTrue(viewModel.state.value!!.composerRequested)

        viewModel.composerOpened()
        assertFalse(viewModel.state.value!!.composerRequested)
        assertTrue(viewModel.state.value!!.composerOpen)
        viewModel.returnedFromComposer()
        assertTrue(viewModel.state.value!!.awaitingObservation)
    }

    private class FakeRepository : TestRunRepository {
        override fun saveSnapshot(
            scenario: ScenarioDefinition,
            run: TestRun,
            expectedRevision: Long?,
        ) = StoredTestRun(scenario, run, (expectedRevision ?: -1L) + 1L)
        override fun get(runId: RunId): StoredTestRun? = null
        override fun listSummaries(): List<TestRunSummary> = emptyList()
    }

    private class DirectExecutorService : AbstractExecutorService() {
        private var stopped = false
        override fun shutdown() { stopped = true }
        override fun shutdownNow(): MutableList<Runnable> = mutableListOf<Runnable>().also { stopped = true }
        override fun isShutdown() = stopped
        override fun isTerminated() = stopped
        override fun awaitTermination(timeout: Long, unit: TimeUnit) = true
        override fun execute(command: Runnable) = command.run()
    }
}

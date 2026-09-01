package com.example.testdialer.sms

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.testdialer.domain.RunId
import com.example.testdialer.domain.ScenarioDefinition
import com.example.testdialer.domain.TestRun
import com.example.testdialer.persistence.StoredTestRun
import com.example.testdialer.persistence.TestRunRepository
import com.example.testdialer.persistence.TestRunSummary
import com.example.testdialer.domain.execution.CapturedTime
import com.example.testdialer.domain.execution.TimeProvider
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

    @Test
    fun `event timestamp is captured at click before queued persistence`() {
        val repository = FakeRepository()
        val time = IncrementingTimeProvider()
        val queued = QueuedExecutorService()
        val coordinator = GuidedSmsTestCoordinator(repository, time)
        val viewModel = GuidedSmsViewModel(coordinator, queued, time)

        viewModel.start(GuidedSmsInput("123", "hello", null))
        queued.runNext()
        viewModel.composerOpened()
        viewModel.returnedFromComposer()

        viewModel.record(GuidedSmsOutcome.USER_REPORTED_SENT)
        val clickTime = time.lastCaptured
        time.capture() // Simulate clock progress while the executor is delayed.
        queued.runNext()

        val event = repository.lastStored!!.run.events.single()
        val actionEntry = repository.lastStored!!.run.timeline.single {
            it.relatedEventId == event.id
        }
        assertTrue(event.occurredAtMillis == clickTime.epochMillis)
        assertTrue(actionEntry.capturedAt == clickTime)
        queued.shutdown()
    }

    @Test
    fun `busy state ignores duplicate start click`() {
        val repository = FakeRepository()
        val queued = QueuedExecutorService()
        val viewModel = GuidedSmsViewModel(GuidedSmsTestCoordinator(repository), queued)

        val input = GuidedSmsInput("123", "hello", null)
        viewModel.start(input)
        viewModel.start(input)
        queued.runNext()

        assertTrue(repository.saveCount == 1)
        queued.shutdown()
    }

    @Test
    fun `activity not found after preflight leaves a closable neutral observation`() {
        val viewModel = GuidedSmsViewModel(GuidedSmsTestCoordinator(FakeRepository()), executor)
        viewModel.start(GuidedSmsInput("123", "hello", null))
        viewModel.composerOpened()

        viewModel.composerLaunchFailed()

        assertFalse(viewModel.state.value!!.composerOpen)
        assertTrue(viewModel.state.value!!.awaitingObservation)
    }

    private class FakeRepository : TestRunRepository {
        var lastStored: StoredTestRun? = null
        var saveCount = 0
        override fun saveSnapshot(
            scenario: ScenarioDefinition,
            run: TestRun,
            expectedRevision: Long?,
        ) = StoredTestRun(scenario, run, (expectedRevision ?: -1L) + 1L).also {
            lastStored = it
            saveCount++
        }
        override fun get(runId: RunId): StoredTestRun? = null
        override fun listSummaries(): List<TestRunSummary> = emptyList()
    }

    private class IncrementingTimeProvider : TimeProvider {
        var lastCaptured = CapturedTime(1L, 0L)
        override fun capture(): CapturedTime {
            lastCaptured = CapturedTime(
                epochMillis = lastCaptured.epochMillis + 10L,
                monotonicNanos = lastCaptured.monotonicNanos + 10L,
            )
            return lastCaptured
        }
    }

    private class QueuedExecutorService : AbstractExecutorService() {
        private val tasks = ArrayDeque<Runnable>()
        private var stopped = false
        override fun shutdown() { stopped = true }
        override fun shutdownNow(): MutableList<Runnable> = tasks.toMutableList().also {
            tasks.clear()
            stopped = true
        }
        override fun isShutdown() = stopped
        override fun isTerminated() = stopped && tasks.isEmpty()
        override fun awaitTermination(timeout: Long, unit: TimeUnit) = isTerminated
        override fun execute(command: Runnable) { tasks.addLast(command) }
        fun runNext() = tasks.removeFirst().run()
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

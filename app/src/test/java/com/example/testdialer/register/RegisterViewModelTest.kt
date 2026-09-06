package com.example.testdialer.register

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.testdialer.domain.EventId
import com.example.testdialer.domain.RunId
import com.example.testdialer.domain.ScenarioDefinition
import com.example.testdialer.domain.ScenarioId
import com.example.testdialer.domain.ScenarioStepDefinition
import com.example.testdialer.domain.StepId
import com.example.testdialer.domain.TestAction
import com.example.testdialer.domain.TestEvent
import com.example.testdialer.domain.TestRun
import com.example.testdialer.domain.TestRunStatus
import com.example.testdialer.persistence.StoredTestRun
import com.example.testdialer.persistence.TestRunRepository
import com.example.testdialer.persistence.TestRunSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit

class RegisterViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun `loads run list and navigates from run to event detail`() {
        val repository = FakeRepository()
        val executor = QueueExecutorService()
        val viewModel = RegisterViewModel(repository, executor)

        viewModel.load()
        executor.runNext()
        assertEquals(listOf("run-1"), viewModel.state.value!!.runs.map { it.runId.value })
        assertEquals(1, viewModel.state.value!!.runs.single().eventCount)

        viewModel.selectRun(RunId("run-1"))
        executor.runNext()
        assertEquals(RunId("run-1"), viewModel.state.value!!.selectedRun?.run?.id)

        viewModel.selectEvent(EventId("event-1"))
        assertEquals(EventId("event-1"), viewModel.state.value!!.selectedEventId)
        viewModel.clearEvent()
        assertNull(viewModel.state.value!!.selectedEventId)
        viewModel.clearRun()
        assertNull(viewModel.state.value!!.selectedRun)
        executor.shutdown()
    }

    @Test
    fun `empty repository produces an explicit empty state`() {
        val executor = QueueExecutorService()
        val viewModel = RegisterViewModel(FakeRepository(empty = true), executor)

        viewModel.load()
        executor.runNext()

        assertTrue(viewModel.state.value!!.runs.isEmpty())
        assertNull(viewModel.state.value!!.error)
        executor.shutdown()
    }

    @Test
    fun `refresh requested while busy is coalesced and executed after current read`() {
        val repository = FakeRepository()
        val executor = QueueExecutorService()
        val viewModel = RegisterViewModel(repository, executor)

        viewModel.load()
        viewModel.load()
        viewModel.load()

        assertEquals(1, executor.size())
        executor.runNext()
        assertEquals(1, repository.listSummariesCalls)
        assertEquals(1, executor.size())

        executor.runNext()
        assertEquals(2, repository.listSummariesCalls)
        assertTrue(!viewModel.state.value!!.busy)
        executor.shutdown()
    }

    @Test
    fun `stale refresh cannot restore a run cleared during the read`() {
        val executor = QueueExecutorService()
        val viewModel = RegisterViewModel(FakeRepository(), executor)

        viewModel.selectRun(RunId("run-1"))
        executor.runNext()
        assertEquals(RunId("run-1"), viewModel.state.value!!.selectedRun?.run?.id)

        viewModel.load()
        viewModel.clearRun()
        executor.runNext()

        assertNull(viewModel.state.value!!.selectedRun)
        assertNull(viewModel.state.value!!.selectedEventId)
        assertTrue(!viewModel.state.value!!.busy)
        executor.shutdown()
    }

    @Test
    fun `stale refresh cannot restore an event cleared during the read`() {
        val executor = QueueExecutorService()
        val viewModel = RegisterViewModel(FakeRepository(), executor)

        viewModel.selectRun(RunId("run-1"))
        executor.runNext()
        viewModel.selectEvent(EventId("event-1"))
        assertEquals(EventId("event-1"), viewModel.state.value!!.selectedEventId)

        viewModel.load()
        viewModel.clearEvent()
        executor.runNext()

        assertEquals(RunId("run-1"), viewModel.state.value!!.selectedRun?.run?.id)
        assertNull(viewModel.state.value!!.selectedEventId)
        assertTrue(!viewModel.state.value!!.busy)
        executor.shutdown()
    }

    @Test
    fun `stale run selection cannot restore a run cleared during the read`() {
        val executor = QueueExecutorService()
        val viewModel = RegisterViewModel(FakeRepository(), executor)

        viewModel.selectRun(RunId("run-1"))
        viewModel.clearRun()
        executor.runNext()

        assertNull(viewModel.state.value!!.selectedRun)
        assertNull(viewModel.state.value!!.selectedEventId)
        assertTrue(!viewModel.state.value!!.busy)
        executor.shutdown()
    }

    @Test
    fun `navigation requested while busy is processed after the stale read`() {
        val executor = QueueExecutorService()
        val viewModel = RegisterViewModel(FakeRepository(), executor)

        viewModel.load()
        viewModel.selectRun(RunId("run-1"))
        assertEquals(1, executor.size())

        executor.runNext()
        assertEquals(1, executor.size())
        executor.runNext()

        assertEquals(RunId("run-1"), viewModel.state.value!!.selectedRun?.run?.id)
        assertNull(viewModel.state.value!!.selectedEventId)
        assertTrue(!viewModel.state.value!!.busy)
        executor.shutdown()
    }

    private class FakeRepository(private val empty: Boolean = false) : TestRunRepository {
        var listSummariesCalls = 0

        private val scenario = ScenarioDefinition(
            id = ScenarioId("register-test"),
            version = 1,
            name = "Register smoke",
            steps = listOf(
                ScenarioStepDefinition(
                    id = StepId("step-1"),
                    order = 0,
                    title = "Voice",
                    instruction = "Record",
                    action = TestAction.Voice("+48123"),
                ),
            ),
        )
        private val event = TestEvent(
            id = EventId("event-1"),
            runId = RunId("run-1"),
            stepId = StepId("step-1"),
            action = TestAction.Voice("+48123"),
            occurredAtMillis = 1L,
        )
        private val stored = StoredTestRun(
            scenario,
            TestRun(
                id = RunId("run-1"),
                scenarioId = scenario.id,
                scenarioVersion = 1,
                status = TestRunStatus.CREATED,
                startedAtMillis = 1L,
                events = listOf(event),
            ),
            revision = 0L,
        )

        override fun saveSnapshot(scenario: ScenarioDefinition, run: TestRun, expectedRevision: Long?) = stored
        override fun get(runId: RunId): StoredTestRun? = stored.takeUnless { empty || it.run.id != runId }
        override fun listSummaries(): List<TestRunSummary> {
            listSummariesCalls += 1
            return if (empty) emptyList() else listOf(
                TestRunSummary(
                    runId = stored.run.id,
                    scenarioName = scenario.name,
                    scenarioVersion = 1,
                    status = TestRunStatus.CREATED,
                    startedAtMillis = 1L,
                    completedAtMillis = null,
                    revision = 0L,
                    eventCount = 1,
                ),
            )
        }
    }

    private class QueueExecutorService : AbstractExecutorService() {
        private val tasks = ArrayDeque<Runnable>()
        private var stopped = false
        override fun execute(command: Runnable) { tasks.addLast(command) }
        fun runNext() = tasks.removeFirst().run()
        fun size() = tasks.size
        override fun shutdown() { stopped = true }
        override fun shutdownNow(): MutableList<Runnable> = tasks.toMutableList().also { tasks.clear(); stopped = true }
        override fun isShutdown() = stopped
        override fun isTerminated() = stopped && tasks.isEmpty()
        override fun awaitTermination(timeout: Long, unit: TimeUnit) = isTerminated
    }
}

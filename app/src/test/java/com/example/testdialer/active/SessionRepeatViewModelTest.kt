package com.example.testdialer.active

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.example.testdialer.TestDialerApplication
import com.example.testdialer.domain.RunId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SessionRepeatViewModelTest {
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun `repeat plan is prepared once and retained until explicit dismissal`() {
        val application = ApplicationProvider.getApplicationContext<TestDialerApplication>()
        val executor = QueuedExecutor()
        val plan = LocalScenario("repeat", "Powtórka", emptyList())
        var loads = 0
        val viewModel = SessionRepeatViewModel(application, executor) {
            loads += 1
            plan
        }

        viewModel.load(RunId("source"))
        viewModel.load(RunId("ignored"))

        assertTrue(viewModel.state.value is SessionRepeatState.Loading)
        assertEquals(1, executor.pending)
        executor.runNext()

        assertEquals(1, loads)
        assertEquals(plan, (viewModel.state.value as SessionRepeatState.Ready).plan)
        viewModel.dismiss()
        assertEquals(SessionRepeatState.Idle, viewModel.state.value)
    }

    @Test
    fun `failure can be acknowledged before retry`() {
        val application = ApplicationProvider.getApplicationContext<TestDialerApplication>()
        val executor = QueuedExecutor()
        val viewModel = SessionRepeatViewModel(application, executor) {
            error("Sesja jest niedostępna.")
        }

        viewModel.load(RunId("missing"))
        executor.runNext()

        assertEquals(
            "Sesja jest niedostępna.",
            (viewModel.state.value as SessionRepeatState.Failure).message,
        )
        viewModel.acknowledgeFailure()
        assertEquals(SessionRepeatState.Idle, viewModel.state.value)
    }

    private class QueuedExecutor : AbstractExecutorService() {
        private val tasks = ArrayDeque<Runnable>()
        private var shutdown = false
        val pending get() = tasks.size
        override fun execute(command: Runnable) { check(!shutdown); tasks.addLast(command) }
        fun runNext() = tasks.removeFirst().run()
        override fun shutdown() { shutdown = true }
        override fun shutdownNow(): MutableList<Runnable> {
            shutdown = true
            return tasks.toMutableList().also { tasks.clear() }
        }
        override fun isShutdown() = shutdown
        override fun isTerminated() = shutdown && tasks.isEmpty()
        override fun awaitTermination(timeout: Long, unit: TimeUnit) = isTerminated
    }
}

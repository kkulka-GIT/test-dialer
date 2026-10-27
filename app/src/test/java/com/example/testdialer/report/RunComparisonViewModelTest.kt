package com.example.testdialer.report

import android.content.Intent
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
class RunComparisonViewModelTest {
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun `comparison stays single and keeps display state until dismissed`() {
        val application = ApplicationProvider.getApplicationContext<TestDialerApplication>()
        val executor = QueuedExecutor()
        val comparison = comparison()
        val viewModel = RunComparisonViewModel(
            application,
            executor,
            { _, _ -> comparison },
        ) { _, _, _ ->
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        viewModel.load(RunId("baseline"), RunId("compared"))
        viewModel.load(RunId("other"), RunId("ignored"))

        assertTrue(viewModel.state.value is RunComparisonState.Loading)
        assertEquals(1, executor.pending)
        executor.runNext()

        assertEquals(comparison, (viewModel.state.value as RunComparisonState.Ready).comparison)
        viewModel.toggleDifferences()
        assertTrue((viewModel.state.value as RunComparisonState.Ready).onlyDifferences)

        viewModel.prepareShare()
        assertTrue(viewModel.state.value is RunComparisonState.Sharing)
        executor.runNext()

        val shared = viewModel.state.value as RunComparisonState.ShareReady
        assertTrue(shared.onlyDifferences)
        assertEquals("text/plain", shared.intent.type)
        assertTrue(shared.intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)

        viewModel.acknowledgeShare()
        assertEquals(RunComparisonState.Idle, viewModel.state.value)
    }

    @Test
    fun `share failure returns to retained comparison for retry`() {
        val application = ApplicationProvider.getApplicationContext<TestDialerApplication>()
        val executor = QueuedExecutor()
        val comparison = comparison()
        val viewModel = RunComparisonViewModel(
            application,
            executor,
            { _, _ -> comparison },
        ) { _, _, _ -> error("share failed") }

        viewModel.load(RunId("baseline"), RunId("compared"))
        executor.runNext()
        viewModel.prepareShare()
        executor.runNext()

        assertEquals(comparison, (viewModel.state.value as RunComparisonState.Failure).comparison)
        viewModel.acknowledgeFailure()
        assertEquals(comparison, (viewModel.state.value as RunComparisonState.Ready).comparison)
    }

    private fun comparison() = RunComparison(
        baselineId = "baseline",
        comparedId = "compared",
        baselineName = "Sesja A",
        comparedName = "Sesja B",
        parametersMatch = true,
        rows = listOf(ComparisonRow("Liczba zdarzeń", "1", "2")),
    )

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

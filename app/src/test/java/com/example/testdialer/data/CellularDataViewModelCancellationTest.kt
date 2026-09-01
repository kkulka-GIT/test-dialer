package com.example.testdialer.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.example.testdialer.domain.RunId
import com.example.testdialer.domain.ScenarioDefinition
import com.example.testdialer.domain.TestRun
import com.example.testdialer.domain.execution.CapturedTime
import com.example.testdialer.domain.execution.TimeProvider
import com.example.testdialer.persistence.StoredTestRun
import com.example.testdialer.persistence.TestRunRepository
import com.example.testdialer.persistence.TestRunSummary
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CellularDataViewModelCancellationTest {
    @Test fun `clear before queued execution prevents gateway prepare and execute`() {
        val gateway = CountingGateway()
        val executor = QueuedExecutor()
        val viewModel = CellularDataViewModel(
            CellularDataTestCoordinator(FakeRepository(), gateway, FixedTime()),
            executor,
            FixedTime(),
        )
        val store = ViewModelStore()
        val provider = ViewModelProvider(store, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>) = viewModel as T
        })
        provider[CellularDataViewModel::class.java]

        viewModel.start(CellularDataInput("https://example.com/file", null))
        store.clear()
        executor.runAll()

        assertEquals(0, gateway.prepareCount)
        assertEquals(0, gateway.executeCount)
        assertTrue(executor.isShutdown)
    }

    private class CountingGateway : CellularDownloadGateway {
        var prepareCount = 0
        var executeCount = 0
        override fun prepare(rawUrl: String): PreparedCellularDownload {
            prepareCount++
            return PreparedCellularDownload(SafeDownloadUrlValidator.requireValid(rawUrl))
        }
        override fun execute(prepared: PreparedCellularDownload, cancellation: DownloadCancellation): DownloadResult {
            executeCount++
            error("must not execute")
        }
    }

    private class FixedTime : TimeProvider {
        override fun capture() = CapturedTime(100, 100)
    }

    private class QueuedExecutor : AbstractExecutorService() {
        private val tasks = ArrayDeque<Runnable>()
        private var stopped = false
        override fun execute(command: Runnable) { if (!stopped) tasks.addLast(command) }
        override fun shutdown() { stopped = true }
        override fun shutdownNow(): MutableList<Runnable> = tasks.toMutableList().also {
            tasks.clear()
            stopped = true
        }
        override fun isShutdown() = stopped
        override fun isTerminated() = stopped && tasks.isEmpty()
        override fun awaitTermination(timeout: Long, unit: TimeUnit) = isTerminated
        fun runAll() { while (tasks.isNotEmpty()) tasks.removeFirst().run() }
    }

    private class FakeRepository : TestRunRepository {
        override fun saveSnapshot(scenario: ScenarioDefinition, run: TestRun, expectedRevision: Long?) =
            StoredTestRun(scenario, run, (expectedRevision ?: -1) + 1)
        override fun get(runId: RunId): StoredTestRun? = null
        override fun listSummaries(): List<TestRunSummary> = emptyList()
    }
}

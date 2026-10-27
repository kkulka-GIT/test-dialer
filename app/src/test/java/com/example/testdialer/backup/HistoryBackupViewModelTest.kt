package com.example.testdialer.backup

import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.example.testdialer.TestDialerApplication
import com.example.testdialer.report.HistoryArchive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HistoryBackupViewModelTest {
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun `history read stays single and exposes preview after queued work`() {
        val application = ApplicationProvider.getApplicationContext<TestDialerApplication>()
        val file = File(application.cacheDir, "history-view-model.json").apply {
            writeText(HistoryArchive.encode(emptyList(), 2_000))
        }
        val executor = QueuedExecutor()
        val viewModel = HistoryBackupViewModel(application, executor)

        viewModel.readImport(Uri.fromFile(file))
        viewModel.readImport(Uri.fromFile(file))

        assertEquals(HistoryBackupOperation.READ_IMPORT, (viewModel.state.value as HistoryBackupState.Working).operation)
        assertEquals(1, executor.pending)
        executor.runNext()

        assertTrue(viewModel.state.value is HistoryBackupState.Preview)
        assertTrue((viewModel.state.value as HistoryBackupState.Preview).entries.isEmpty())
        viewModel.discardPreview()
        assertEquals(HistoryBackupState.Idle, viewModel.state.value)
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

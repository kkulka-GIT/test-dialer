package com.example.testdialer.backup

import android.content.Context
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.example.testdialer.TestDialerApplication
import com.example.testdialer.domain.TestAction
import com.example.testdialer.templates.TestTemplate
import com.example.testdialer.templates.TestTemplateArchive
import com.example.testdialer.templates.TestTemplateStore
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
class TemplateBackupViewModelTest {
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun `template read stays single and exposes preview after queued work`() {
        val application = ApplicationProvider.getApplicationContext<TestDialerApplication>()
        clear(application)
        val file = File(application.cacheDir, "template-view-model.json").apply {
            writeText(TestTemplateArchive.encode(emptyList()))
        }
        val executor = QueuedExecutor()
        val viewModel = TemplateBackupViewModel(application, executor)

        viewModel.readImport(Uri.fromFile(file))
        viewModel.readImport(Uri.fromFile(file))

        assertEquals(
            TemplateBackupOperation.READ_IMPORT,
            (viewModel.state.value as TemplateBackupState.Working).operation,
        )
        assertEquals(1, executor.pending)
        executor.runNext()

        assertTrue(viewModel.state.value is TemplateBackupState.Preview)
        assertTrue((viewModel.state.value as TemplateBackupState.Preview).items.isEmpty())
        viewModel.discardPreview()
        assertEquals(TemplateBackupState.Idle, viewModel.state.value)
    }

    @Test
    fun `confirmed preview imports parameters without executing a service`() {
        val application = ApplicationProvider.getApplicationContext<TestDialerApplication>()
        clear(application)
        val incoming = listOf(
            TestTemplate("archive-id", "SMS z kopii", TestAction.Sms("123", "Test")),
        )
        val file = File(application.cacheDir, "template-view-model-import.json").apply {
            writeText(TestTemplateArchive.encode(incoming))
        }
        val executor = QueuedExecutor()
        val viewModel = TemplateBackupViewModel(application, executor)

        viewModel.readImport(Uri.fromFile(file))
        executor.runNext()
        assertEquals(1, (viewModel.state.value as TemplateBackupState.Preview).added)

        viewModel.importPreview()
        assertEquals(
            TemplateBackupOperation.IMPORT,
            (viewModel.state.value as TemplateBackupState.Working).operation,
        )
        executor.runNext()

        assertTrue(viewModel.state.value is TemplateBackupState.Success)
        assertEquals(listOf("SMS z kopii"), TestTemplateStore(application).list().map { it.name })
        clear(application)
    }

    private fun clear(context: Context) {
        context.getSharedPreferences("test-templates-v1", Context.MODE_PRIVATE).edit().clear().commit()
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

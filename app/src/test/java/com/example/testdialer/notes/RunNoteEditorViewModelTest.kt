package com.example.testdialer.notes

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.testdialer.domain.RunId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit

class RunNoteEditorViewModelTest {
    @get:Rule val instant = InstantTaskExecutorRule()

    @Test fun `draft survives observer replacement without being saved`() {
        var saves = 0
        val executor = QueuedExecutorService()
        val viewModel = RunNoteEditorViewModel({ "zapisana" }, { _, _ -> saves++; true }, executor)

        viewModel.open(RunId("run-1"))
        viewModel.updateDraft("szkic do korelacji", 5)

        assertTrue(viewModel.state.value!!.open)
        assertEquals("szkic do korelacji", viewModel.state.value!!.text)
        assertEquals(5, viewModel.state.value!!.selection)
        assertEquals(0, saves)
        executor.shutdown()
    }

    @Test fun `save in progress ignores duplicate save and survives UI recreation`() {
        var saves = 0
        val executor = QueuedExecutorService()
        val viewModel = RunNoteEditorViewModel({ "" }, { _, _ -> saves++; true }, executor)
        viewModel.open(RunId("run-2"))
        viewModel.updateDraft("dowód", 5)

        viewModel.save()
        viewModel.save()
        assertTrue(viewModel.state.value!!.busy)
        assertEquals("dowód", viewModel.state.value!!.text)
        executor.runNext()

        assertEquals(1, saves)
        assertFalse(viewModel.state.value!!.open)
        assertTrue(viewModel.state.value!!.saved)
        executor.shutdown()
    }

    @Test fun `failed save keeps draft and retry writes once more`() {
        var attempts = 0
        val executor = QueuedExecutorService()
        val viewModel = RunNoteEditorViewModel({ "" }, { _, _ -> ++attempts > 1 }, executor)
        viewModel.open(RunId("run-3"))
        viewModel.updateDraft("ważny szkic", 11)

        viewModel.save()
        executor.runNext()
        assertTrue(viewModel.state.value!!.open)
        assertTrue(viewModel.state.value!!.error)
        assertEquals("ważny szkic", viewModel.state.value!!.text)

        viewModel.save()
        executor.runNext()
        assertEquals(2, attempts)
        assertTrue(viewModel.state.value!!.saved)
        executor.shutdown()
    }

    private class QueuedExecutorService : AbstractExecutorService() {
        private val tasks = ArrayDeque<Runnable>()
        private var stopped = false
        override fun execute(command: Runnable) { check(!stopped); tasks.addLast(command) }
        fun runNext() = tasks.removeFirst().run()
        override fun shutdown() { stopped = true }
        override fun shutdownNow(): MutableList<Runnable> { stopped = true; return tasks.toMutableList() }
        override fun isShutdown() = stopped
        override fun isTerminated() = stopped && tasks.isEmpty()
        override fun awaitTermination(timeout: Long, unit: TimeUnit) = isTerminated
    }
}

package com.example.testdialer.review

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.testdialer.domain.EventId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit

class BillingReviewEditorViewModelTest {
    @get:Rule val instant = InstantTaskExecutorRule()

    @Test fun `draft survives observer replacement without being saved`() {
        var saves = 0
        val executor = QueuedExecutorService()
        val viewModel = BillingReviewEditorViewModel({ BillingReview() }, { _, _ -> saves++; true }, { 1000 }, executor)
        viewModel.open(EventId("event-1"))
        viewModel.updateDraft("0,79 PLN", "CDR 0,79 PLN", 4, 3, BillingVerdict.PASS)

        val state = viewModel.state.value!!
        assertTrue(state.open)
        assertEquals("0,79 PLN", state.expected)
        assertEquals("CDR 0,79 PLN", state.actual)
        assertEquals(BillingVerdict.PASS, state.verdict)
        assertEquals(0, saves)
        executor.shutdown()
    }

    @Test fun `save in progress ignores duplicate save`() {
        var saves = 0
        val executor = QueuedExecutorService()
        val viewModel = BillingReviewEditorViewModel({ BillingReview() }, { _, review ->
            saves++
            assertEquals(2000, review.reviewedAtMillis)
            true
        }, { 2000 }, executor)
        viewModel.open(EventId("event-2"))
        viewModel.updateDraft("1 PLN", "1 PLN", 5, 5, BillingVerdict.PASS)

        viewModel.save()
        viewModel.save()
        assertTrue(viewModel.state.value!!.busy)
        executor.runNext()

        assertEquals(1, saves)
        assertFalse(viewModel.state.value!!.open)
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

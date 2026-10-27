package com.example.testdialer.templates

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.testdialer.domain.CorrelationMetadata
import com.example.testdialer.domain.CorrelationReference
import com.example.testdialer.domain.EventId
import com.example.testdialer.domain.RunId
import com.example.testdialer.domain.StepId
import com.example.testdialer.domain.TestAction
import com.example.testdialer.domain.TestEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit

class TemplateSaveViewModelTest {
    @get:Rule val instant = InstantTaskExecutorRule()

    @Test fun `save in progress keeps draft and prevents duplicate template`() {
        var saves = 0
        val executor = QueuedExecutorService()
        val viewModel = TemplateSaveViewModel({ _, _, _ -> saves++; true }, executor)
        viewModel.open(event(), "SMS standard")
        viewModel.updateDraft("Powtórka klienta", 8)

        viewModel.save()
        viewModel.save()

        assertTrue(viewModel.state.value!!.busy)
        assertEquals("Powtórka klienta", viewModel.state.value!!.name)
        assertEquals(1, executor.pending)
        executor.runNext()

        assertEquals(1, saves)
        assertFalse(viewModel.state.value!!.open)
        assertTrue(viewModel.state.value!!.saved)
        executor.shutdown()
    }

    @Test fun `failed save keeps name and parameters for retry`() {
        var attempts = 0
        val executor = QueuedExecutorService()
        val viewModel = TemplateSaveViewModel({ _, _, _ -> ++attempts > 1 }, executor)
        viewModel.open(event(), "SMS standard")
        viewModel.updateDraft("Ważny szablon", 5)

        viewModel.save()
        executor.runNext()

        val failed = viewModel.state.value!!
        assertTrue(failed.open)
        assertEquals("Ważny szablon", failed.name)
        assertEquals(1_000_000L, failed.requestedBytes)
        assertTrue(failed.error != null)

        viewModel.save()
        executor.runNext()
        assertEquals(2, attempts)
        assertTrue(viewModel.state.value!!.saved)
        executor.shutdown()
    }

    private fun event() = TestEvent(
        id = EventId("event"),
        runId = RunId("run"),
        stepId = StepId("step"),
        action = TestAction.Sms("+48123456789", "Test Dialer"),
        occurredAtMillis = 1L,
        correlation = CorrelationMetadata(
            references = listOf(CorrelationReference("requestedBytes", "1000000")),
        ),
    )

    private class QueuedExecutorService : AbstractExecutorService() {
        private val tasks = ArrayDeque<Runnable>()
        private var stopped = false
        val pending get() = tasks.size
        override fun execute(command: Runnable) { check(!stopped); tasks.addLast(command) }
        fun runNext() = tasks.removeFirst().run()
        override fun shutdown() { stopped = true }
        override fun shutdownNow(): MutableList<Runnable> {
            stopped = true
            return tasks.toMutableList().also { tasks.clear() }
        }
        override fun isShutdown() = stopped
        override fun isTerminated() = stopped && tasks.isEmpty()
        override fun awaitTermination(timeout: Long, unit: TimeUnit) = isTerminated
    }
}

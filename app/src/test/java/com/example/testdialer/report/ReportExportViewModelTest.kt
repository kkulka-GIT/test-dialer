package com.example.testdialer.report

import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.example.testdialer.TestDialerApplication
import com.example.testdialer.domain.EventId
import com.example.testdialer.domain.Observation
import com.example.testdialer.domain.ObservationSource
import com.example.testdialer.domain.ObservationStatus
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
class ReportExportViewModelTest {
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun `report preparation stays single and keeps result until acknowledged`() {
        val application = ApplicationProvider.getApplicationContext<TestDialerApplication>()
        val executor = QueuedExecutor()
        val viewModel = ReportExportViewModel(application, executor)

        viewModel.prepare(snapshot(), ReportExportOption.COPY)
        viewModel.prepare(snapshot(), ReportExportOption.SHARE_JSON)

        assertEquals(
            ReportExportOption.COPY,
            (viewModel.state.value as ReportExportState.Working).option,
        )
        assertEquals(1, executor.pending)
        executor.runNext()

        val ready = viewModel.state.value as ReportExportState.Ready
        assertEquals(ReportExportOption.COPY, ready.option)
        assertTrue(ready.content.contains("run-report"))
        assertEquals(null, ready.shareIntent)

        viewModel.acknowledge()
        assertEquals(ReportExportState.Idle, viewModel.state.value)
    }

    @Test
    fun `json share result contains a read only content intent`() {
        val application = ApplicationProvider.getApplicationContext<TestDialerApplication>()
        val executor = QueuedExecutor()
        val viewModel = ReportExportViewModel(application, executor)

        viewModel.prepare(snapshot(), ReportExportOption.SHARE_JSON)
        executor.runNext()

        val ready = viewModel.state.value as ReportExportState.Ready
        assertTrue(ready.content.contains("\"schemaVersion\""))
        assertNotNull(ready.shareIntent)
        assertEquals("application/json", ready.shareIntent!!.type)
        assertTrue(ready.shareIntent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }

    private fun snapshot(): StoredTestRun {
        val step = ScenarioStepDefinition(
            StepId("sms"),
            0,
            "SMS",
            "Wyślij",
            TestAction.Sms("+48123", "Test"),
        )
        val scenario = ScenarioDefinition(ScenarioId("report"), 1, "Raport", steps = listOf(step))
        val run = TestRun(
            RunId("run-report"),
            scenario.id,
            1,
            TestRunStatus.COMPLETED,
            1_000,
            2_000,
            events = listOf(
                TestEvent(
                    EventId("event-report"),
                    RunId("run-report"),
                    step.id,
                    step.action,
                    1_500,
                    Observation(
                        ObservationStatus.NOT_VERIFIED,
                        ObservationSource.TESTER,
                        "NOT_VERIFIED",
                    ),
                ),
            ),
        )
        return StoredTestRun(scenario, run, 1)
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

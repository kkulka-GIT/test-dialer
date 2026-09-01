package com.example.testdialer.data

import com.example.testdialer.domain.RunId
import com.example.testdialer.domain.ScenarioDefinition
import com.example.testdialer.domain.TestRun
import com.example.testdialer.domain.TestRunStatus
import com.example.testdialer.domain.execution.CapturedTime
import com.example.testdialer.domain.execution.TimeProvider
import com.example.testdialer.persistence.StoredTestRun
import com.example.testdialer.persistence.TestRunRepository
import com.example.testdialer.persistence.TestRunSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CellularDataTestCoordinatorTest {
    @Test fun `persists one completed terminal event with correlation`() {
        val repository = FakeRepository()
        val gateway = FakeGateway(DownloadStatus.COMPLETED)
        val coordinator = CellularDataTestCoordinator(repository, gateway, IncrementingTime())

        val stored = coordinator.run(CellularDataInput("https://example.com/file", "Data test"), CapturedTime(10, 1))

        assertEquals(TestRunStatus.COMPLETED, stored.run.status)
        assertEquals(1, stored.run.events.size)
        assertEquals("COMPLETED", stored.run.events.single().observation?.code)
        val refs = stored.run.events.single().correlation.references.associate { it.namespace to it.value }
        assertEquals("CELLULAR", refs["transport"])
        assertEquals("512", refs["bytes"])
        assertEquals("example.com", refs["host"])
        assertEquals(2, repository.saveCount)
    }

    @Test fun `cancelled result persists one event and aborts run`() {
        val repository = FakeRepository()
        val stored = CellularDataTestCoordinator(
            repository,
            FakeGateway(DownloadStatus.CANCELLED),
            IncrementingTime(),
        ).run(CellularDataInput("https://example.com/file", null), CapturedTime(10, 1))

        assertEquals(TestRunStatus.ABORTED, stored.run.status)
        assertEquals(1, stored.run.events.size)
        assertEquals("CANCELLED", stored.run.events.single().observation?.code)
    }

    @Test fun `preflight failure creates no running snapshot`() {
        val repository = FakeRepository()
        val gateway = object : CellularDownloadGateway {
            override fun prepare(rawUrl: String): PreparedCellularDownload = error("Wi-Fi active")
            override fun execute(prepared: PreparedCellularDownload) = error("must not execute")
            override fun cancel() = Unit
        }

        runCatching {
            CellularDataTestCoordinator(repository, gateway, IncrementingTime())
                .run(CellularDataInput("https://example.com/file", null), CapturedTime(10, 1))
        }

        assertEquals(0, repository.saveCount)
    }

    private class FakeGateway(private val status: DownloadStatus) : CellularDownloadGateway {
        override fun prepare(rawUrl: String) = PreparedCellularDownload(SafeDownloadUrlValidator.requireValid(rawUrl))
        override fun execute(prepared: PreparedCellularDownload) = DownloadResult(
            status, CapturedTime(400, 400), CapturedTime(500, 500), 512, status.name,
        )
        override fun cancel() = Unit
    }

    private class IncrementingTime : TimeProvider {
        private var value = 100L
        override fun capture() = CapturedTime(value, value).also { value += 200 }
    }

    private class FakeRepository : TestRunRepository {
        var saveCount = 0
        override fun saveSnapshot(scenario: ScenarioDefinition, run: TestRun, expectedRevision: Long?): StoredTestRun {
            saveCount++
            return StoredTestRun(scenario, run, (expectedRevision ?: -1) + 1)
        }
        override fun get(runId: RunId): StoredTestRun? = null
        override fun listSummaries(): List<TestRunSummary> = emptyList()
    }
}

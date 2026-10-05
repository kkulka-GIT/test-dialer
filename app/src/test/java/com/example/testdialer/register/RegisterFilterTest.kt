package com.example.testdialer.register

import com.example.testdialer.domain.*
import com.example.testdialer.persistence.TestRunSummary
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class RegisterFilterTest {
    private val zone = ZoneId.of("Europe/Warsaw")
    private val now = Instant.parse("2026-10-25T12:00:00Z").toEpochMilli()
    private fun row(name: String, time: String, status: TestRunStatus = TestRunStatus.COMPLETED, service: ServiceType = ServiceType.DATA) =
        TestRunSummary(RunId(name), name, 1, status, Instant.parse(time).toEpochMilli(), null, 0, 1, setOf(service))

    @Test fun `today follows local midnight across daylight saving transition`() {
        val before = row("before", "2026-10-24T21:59:59Z")
        val start = row("start", "2026-10-24T22:00:00Z")
        val today = row("today", "2026-10-25T11:00:00Z")
        assertEquals(listOf(start, today), RegisterFilter(days = 1).apply(listOf(before, start, today), now, zone))
    }
    @Test fun `name id service and status filters compose without modifying history`() {
        val rows = listOf(row("SMS taryfa", "2026-10-25T11:00:00Z", TestRunStatus.RUNNING, ServiceType.SMS), row("Data", "2026-10-25T10:00:00Z"))
        assertEquals(listOf(rows[0]), RegisterFilter("  TARYFA ", TestRunStatus.RUNNING, ServiceType.SMS, 7).apply(rows, now, zone))
        assertEquals(2, rows.size)
        assertTrue(RegisterFilter(service = ServiceType.VOICE).apply(rows, now, zone).isEmpty())
    }
    @Test fun `seven days include today and preceding six calendar days`() {
        val rows = listOf(row("too old", "2026-10-18T21:59:59Z"), row("boundary", "2026-10-18T22:00:00Z"))
        assertEquals(listOf(rows[1]), RegisterFilter(days = 7).apply(rows, now, zone))
    }
    @Test fun `empty filter preserves ordering and future records are not in today`() {
        val rows = listOf(row("future", "2026-10-26T00:00:00Z"), row("now", "2026-10-25T12:00:00Z"))
        assertEquals(rows, RegisterFilter().apply(rows, now, zone))
        assertEquals(listOf(rows[1]), RegisterFilter(days = 1).apply(rows, now, zone))
    }
}

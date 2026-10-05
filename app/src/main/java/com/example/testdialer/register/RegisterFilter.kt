package com.example.testdialer.register

import com.example.testdialer.domain.ServiceType
import com.example.testdialer.domain.TestRunStatus
import com.example.testdialer.persistence.TestRunSummary
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/** Filter summaries only; never changes stored test results. Calendar days follow the device timezone. */
data class RegisterFilter(
    val query: String = "",
    val status: TestRunStatus? = null,
    val service: ServiceType? = null,
    val days: Int? = null,
) {
    fun apply(runs: List<TestRunSummary>, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): List<TestRunSummary> {
        require(days == null || days > 0)
        val needle = query.trim().lowercase(Locale.ROOT)
        val start = days?.let { Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
            .minusDays(it.toLong() - 1).atStartOfDay(zone).toInstant().toEpochMilli() }
        return runs.filter { run ->
            (needle.isEmpty() || run.scenarioName.lowercase(Locale.ROOT).contains(needle) || run.runId.value.lowercase(Locale.ROOT).contains(needle)) &&
                (status == null || run.status == status) &&
                (service == null || service in run.serviceTypes) &&
                (start == null || run.startedAtMillis in start..nowMillis)
        }
    }
}

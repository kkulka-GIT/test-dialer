package com.example.testdialer.data

import com.example.testdialer.domain.CorrelationMetadata
import com.example.testdialer.domain.CorrelationReference
import com.example.testdialer.domain.Observation
import com.example.testdialer.domain.ObservationSource
import com.example.testdialer.domain.ObservationStatus
import com.example.testdialer.domain.ScenarioDefinition
import com.example.testdialer.domain.ScenarioId
import com.example.testdialer.domain.ScenarioStepDefinition
import com.example.testdialer.domain.StepId
import com.example.testdialer.domain.TestAction
import com.example.testdialer.domain.execution.CapturedTime
import com.example.testdialer.domain.execution.SystemTimeProvider
import com.example.testdialer.domain.execution.TestRunRecorder
import com.example.testdialer.domain.execution.TimeProvider
import com.example.testdialer.persistence.StoredTestRun
import com.example.testdialer.persistence.TestRunRepository
import java.util.UUID

data class CellularDataInput(val url: String, val label: String?)

class CellularDataTestCoordinator(
    private val repository: TestRunRepository,
    private val gateway: CellularDownloadGateway,
    private val timeProvider: TimeProvider = SystemTimeProvider,
) {
    fun run(input: CellularDataInput, requestedAt: CapturedTime): StoredTestRun {
        val prepared = gateway.prepare(input.url) // preflight before RUNNING
        val stepId = StepId("cellular-data-download")
        val scenario = ScenarioDefinition(
            id = ScenarioId("cellular-data-${UUID.randomUUID()}"),
            version = 1,
            name = input.label?.trim()?.takeIf(String::isNotBlank) ?: "Cellular data download",
            description = "Foreground cellular HTTPS GET for rating/billing correlation",
            steps = listOf(
                ScenarioStepDefinition(
                    id = stepId,
                    order = 0,
                    title = "Download over active cellular network",
                    instruction = "Perform a bounded HTTPS GET over the active cellular transport.",
                    action = TestAction.Data(prepared.url.uri.toString()),
                ),
            ),
        )
        val recorder = TestRunRecorder.start(scenario, timeProvider = timeProvider)
        recorder.startStep(stepId)
        recorder.startAttempt()
        var revision = repository.saveSnapshot(scenario, recorder.snapshot()).revision
        return try {
            val result = gateway.execute(prepared)
            val observation = Observation(
                status = if (result.status == DownloadStatus.COMPLETED) {
                    ObservationStatus.CONFIRMED
                } else {
                    ObservationStatus.NOT_CONFIRMED
                },
                source = ObservationSource.APPLICATION,
                code = result.status.name,
            )
            recorder.recordEventAt(
                capturedAt = result.endedAt,
                observation = observation,
                correlation = CorrelationMetadata(
                    destinationAddress = prepared.url.host,
                    references = listOf(
                        CorrelationReference("requestedAtEpochMillis", requestedAt.epochMillis.toString()),
                        CorrelationReference("networkStartedAtEpochMillis", result.networkStartedAt.epochMillis.toString()),
                        CorrelationReference("endedAtEpochMillis", result.endedAt.epochMillis.toString()),
                        CorrelationReference("bytes", result.bytes.toString()),
                        CorrelationReference("durationMillis", durationMillis(result)),
                        CorrelationReference("status", result.status.name),
                        CorrelationReference("host", prepared.url.host),
                        CorrelationReference("transport", "CELLULAR"),
                    ),
                ),
            )
            recorder.finishAttempt()
            recorder.finishStep()
            if (result.status == DownloadStatus.CANCELLED) recorder.abort() else recorder.complete()
            repository.saveSnapshot(scenario, recorder.snapshot(), revision)
        } catch (error: Throwable) {
            // The mutable recorder is deliberately discarded after any execute/CAS failure.
            throw error
        }
    }

    fun cancel() = gateway.cancel()

    private fun durationMillis(result: DownloadResult): String =
        ((result.endedAt.monotonicNanos - result.networkStartedAt.monotonicNanos).coerceAtLeast(0L) / 1_000_000L).toString()
}

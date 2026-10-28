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

data class CellularDataInput(val url: String, val label: String?, val targetBytes: Long = 1_000_000L)

class CellularDataTestCoordinator(
    private val repository: TestRunRepository,
    private val gateway: CellularDownloadGateway,
    private val timeProvider: TimeProvider = SystemTimeProvider,
    private val networkContext: () -> Map<String, String> = { emptyMap() },
) {
    private fun captureNetworkContext(prefix: String): List<CorrelationReference> = runCatching {
        val values = networkContext()
        if (values.isEmpty()) emptyList() else listOf(
            CorrelationReference("${prefix}AtEpochMillis", timeProvider.capture().epochMillis.toString()),
        ) + values.map { (key, value) -> CorrelationReference("$prefix.$key", value) }
    }.getOrDefault(emptyList())

    fun run(
        input: CellularDataInput,
        requestedAt: CapturedTime,
        cancellation: DownloadCancellation,
        onProgress: (Long, Long) -> Unit = { _, _ -> },
    ): StoredTestRun {
        require(input.targetBytes in 1..DataVolume.MAX_BYTES) { "Nieprawidłowa ilość danych" }
        val contextBefore = captureNetworkContext("networkBefore")
        val prepared = gateway.prepare(input.url) // URL and context only; cellular acquisition belongs to the recorded attempt
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
                    instruction = "Pobierz ${input.targetBytes} bajtów treści przez aktywną sieć komórkową.",
                    action = TestAction.Data(prepared.url.uri.toString()),
                ),
            ),
        )
        val recorder = TestRunRecorder.start(scenario, timeProvider = timeProvider)
        recorder.startStep(stepId)
        recorder.startAttempt()
        var revision = repository.saveSnapshot(scenario, recorder.snapshot()).revision
        return try {
            val result = gateway.executeVolume(prepared, cancellation, input.targetBytes, onProgress)
            val contextAfter = captureNetworkContext("networkAfter")
            val observation = Observation(
                status = if (result.status == DownloadStatus.COMPLETED) {
                    ObservationStatus.CONFIRMED
                } else {
                    ObservationStatus.NOT_CONFIRMED
                },
                source = ObservationSource.APPLICATION,
                code = result.resultCode.name,
                description = result.failureStage?.let { stage ->
                    val reason = when (result.resultCode) {
                        DownloadResultCode.NETWORK_UNAVAILABLE -> "Android nie udostępnił bezpośredniej sieci komórkowej w wyznaczonym czasie."
                        DownloadResultCode.DNS_FAILURE -> "Nie udało się rozwiązać nazwy hosta."
                        DownloadResultCode.TLS_FAILURE -> "Nie udało się zestawić bezpiecznego połączenia TLS."
                        DownloadResultCode.CONNECTION_FAILURE -> "Nie udało się połączyć z serwerem."
                        DownloadResultCode.TIMEOUT -> "Upłynął limit czasu sieci."
                        DownloadResultCode.SECURITY_REJECTED -> "Odrzucono operację ze względów bezpieczeństwa."
                        else -> "Wystąpił błąd operacji sieciowej."
                    }
                    val vpn = when (prepared.vpnActiveAtPreparation) {
                        true -> "Android wskazywał aktywny VPN. Nie potwierdza to przyczyny błędu; sprawdź możliwość połączeń poza VPN w ustawieniach telefonu/VPN."
                        false -> "Android nie wskazywał aktywnego VPN."
                        null -> "Stan VPN nie został ustalony."
                    }
                    val diagnostic = listOfNotNull(result.failureCause?.name, result.failureErrno).joinToString("/")
                    val phaseNote = if (stage == DownloadFailureStage.RESPONSE) " Pobieranie odpowiedzi może obejmować DNS, połączenie i TLS." else ""
                    val socketNote = if (result.failureErrno == "EPERM") " Odmowa operacji gniazda; przyczyna odmowy nie została potwierdzona." else ""
                    "$reason$socketNote Etap: ${stage.name}.$phaseNote Diagnostyka: ${diagnostic.ifEmpty { "brak" }}. $vpn Test używa bezpośredniej sieci komórkowej."
                },
            )
            recorder.recordEventAt(
                capturedAt = result.endedAt,
                observation = observation,
                correlation = CorrelationMetadata(
                    destinationAddress = prepared.url.host,
                    references = listOf(
                        CorrelationReference("requestedAtEpochMillis", requestedAt.epochMillis.toString()),
                        CorrelationReference("endedAtEpochMillis", result.endedAt.epochMillis.toString()),
                        CorrelationReference("bytes", result.bytes.toString()),
                        CorrelationReference("requestedBytes", input.targetBytes.toString()),
                        CorrelationReference("byteSemantics", "HTTP_BODY_NOT_BILLING"),
                        CorrelationReference("durationMillis", durationMillis(result)),
                        CorrelationReference("status", result.status.name),
                        CorrelationReference("resultCode", result.resultCode.name),
                        CorrelationReference("host", prepared.url.host),
                        CorrelationReference("transport", "CELLULAR"),
                    ) + contextBefore + contextAfter + listOfNotNull(
                        result.networkStartedAt?.let {
                            CorrelationReference("networkStartedAtEpochMillis", it.epochMillis.toString())
                        },
                        result.httpStatus?.let { CorrelationReference("httpStatus", it.toString()) },
                        result.failureStage?.let { CorrelationReference("failureStage", it.name) },
                        result.failureCause?.let { CorrelationReference("failureCause", it.name) },
                        result.failureErrno?.let { CorrelationReference("failureErrno", it) },
                        prepared.vpnActiveAtPreparation?.let { CorrelationReference("vpnActiveAtPreparation", it.toString()) },
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

    private fun durationMillis(result: DownloadResult): String =
        result.networkStartedAt?.let {
            ((result.endedAt.monotonicNanos - it.monotonicNanos).coerceAtLeast(0L) / 1_000_000L).toString()
        } ?: "0"
}

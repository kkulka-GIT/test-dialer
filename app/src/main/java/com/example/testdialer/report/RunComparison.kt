package com.example.testdialer.report

import com.example.testdialer.domain.*
import com.example.testdialer.persistence.StoredTestRun
import com.example.testdialer.review.BillingReview
import com.example.testdialer.review.BillingVerdict
import java.math.BigInteger
import java.time.Instant

data class ComparisonRow(val label: String, val baseline: String, val compared: String) {
    val different: Boolean get() = baseline != compared
}

data class RunComparison(
    val baselineId: String,
    val comparedId: String,
    val baselineName: String,
    val comparedName: String,
    val parametersMatch: Boolean?,
    val rows: List<ComparisonRow>,
) {
    val differences: List<ComparisonRow> get() = rows.filter { it.different }
    fun text(onlyDifferences: Boolean = false): String = buildString {
        appendLine("TEST DIALER — PORÓWNANIE SESJI")
        appendLine("A · baza: $baselineName")
        appendLine("B · porównywana: $comparedName")
        appendLine("Run A: $baselineId")
        appendLine("Run B: $comparedId")
        appendLine(when (parametersMatch) { true -> "Zapisane parametry i liczba powtórzeń usług są zgodne."; false -> "Parametry lub liczba powtórzeń są różne. Nie traktuj różnic wyników jako automatycznej regresji."; null -> "Brak pełnych parametrów do sprawdzenia zgodności. Różnice wyników wymagają analizy testera." })
        appendLine("To zestawienie obserwacji i ręcznych ocen. Nie potwierdza poprawności naliczenia ani szybkości sieci.")
        appendLine("Czas sesji obejmuje także czynności testera. Brak pomiaru nie oznacza zera.")
        val visible = if (onlyDifferences) differences else rows
        if (visible.isEmpty()) appendLine("Brak różnic w porównywanych polach.")
        visible.forEach { row ->
            appendLine()
            appendLine(row.label + if (row.different) " · RÓŻNICA" else "")
            appendLine("A: ${row.baseline}")
            appendLine("B: ${row.compared}")
        }
    }
}

/** Aggregate comparison of recorded facts. No event pairing or verdict inference. */
object RunComparisonFormatter {
    fun compare(a: StoredTestRun, b: StoredTestRun, reviews: Map<String, BillingReview> = emptyMap()): RunComparison {
        require(a.run.id != b.run.id) { "Wybierz inną sesję do porównania." }
        val rows = mutableListOf<ComparisonRow>()
        fun row(label: String, metric: (StoredTestRun) -> String) { rows += ComparisonRow(label, metric(a), metric(b)) }
        row("Stan sesji") { when(it.run.status) { TestRunStatus.CREATED -> "Utworzona"; TestRunStatus.RUNNING -> "W toku"; TestRunStatus.COMPLETED -> "Zakończona"; TestRunStatus.ABORTED -> "Przerwana" } }
        row("Start UTC") { Instant.ofEpochMilli(it.run.startedAtMillis).toString() }
        row("Zakończenie UTC") { it.run.completedAtMillis?.let { time -> Instant.ofEpochMilli(time).toString() } ?: "Brak końcowego czasu" }
        row("Czas całej sesji (ms)") { stored ->
            stored.run.completedAtMillis?.let { end -> if (end >= stored.run.startedAtMillis) (end - stored.run.startedAtMillis).toString() else "Niespójny zegar" } ?: "Brak końcowego czasu"
        }
        row("Liczba zapisanych zdarzeń") { it.run.events.size.toString() }
        ServiceType.entries.forEach { type -> row("Zdarzenia · " + when(type) { ServiceType.VOICE -> "Połączenie"; ServiceType.SMS -> "SMS"; ServiceType.DATA -> "Dane" }) { it.run.events.count { event -> event.action.serviceType == type }.toString() } }
        ObservationStatus.entries.forEach { status -> row("Obserwacje · " + when(status) { ObservationStatus.CONFIRMED -> "potwierdzone"; ObservationStatus.NOT_CONFIRMED -> "niepotwierdzone"; ObservationStatus.NOT_VERIFIED -> "niezweryfikowane" }) { it.run.events.count { event -> event.observation?.status == status }.toString() } }
        row("Brak obserwacji") { it.run.events.count { event -> event.observation == null }.toString() }
        row("Kody obserwacji i źródła") { stored ->
            stored.run.events.mapNotNull { event -> event.observation?.let { "${it.source.name} / ${it.code}" } }
                .groupingBy { it }.eachCount().toSortedMap().entries.joinToString("\n") { "${it.key}: ${it.value}" }.ifEmpty { "Brak" }
        }
        row("Dane · zapisane bajty") { byteSummary(it, "bytes") }
        row("Dane · żądane bajty") { byteSummary(it, "requestedBytes") }
        BillingVerdict.entries.forEach { verdict -> row("Ocena rozliczenia TESTER · " + when(verdict) { BillingVerdict.NOT_CHECKED -> "nie sprawdzono"; BillingVerdict.PASS -> "zgodne (PASS)"; BillingVerdict.FAIL -> "niezgodne (FAIL)" }) {
            it.run.events.count { event -> (reviews[event.id.value]?.verdict ?: BillingVerdict.NOT_CHECKED) == verdict }.toString()
        } }
        row("Opisy ocen rozliczenia TESTER") { stored ->
            stored.run.events.mapNotNull { event -> reviews[event.id.value]?.takeIf { it.expected.isNotBlank() || it.actual.isNotBlank() } }
                .map { "${it.verdict.name} · oczekiwano: ${it.expected} · otrzymano: ${it.actual}" }
                .groupingBy { it }.eachCount().toSortedMap().entries.joinToString("\n") { "${it.key} (zdarzeń: ${it.value})" }.ifEmpty { "Brak opisów" }
        }
        return RunComparison(a.run.id.value, b.run.id.value, a.scenario.name, b.scenario.name, parameterMatch(a, b), rows)
    }

    private fun parameterMatch(a: StoredTestRun, b: StoredTestRun): Boolean? {
        if (a.run.events.isEmpty() || b.run.events.isEmpty()) return null
        if ((a.run.events + b.run.events).any { it.action is TestAction.Data && requested(it) == null }) return null
        return profile(a) == profile(b)
    }

    private fun requested(event: TestEvent): Long? = event.correlation.references.filter { it.namespace == "requestedBytes" }
        .singleOrNull()?.value?.toLongOrNull()?.takeIf { it in 1..com.example.testdialer.data.DataVolume.MAX_BYTES }

    private fun profile(stored: StoredTestRun): Map<Pair<TestAction, Long?>, Int> = stored.run.events
        .map { event -> event.action to if (event.action is TestAction.Data) requested(event) else null }
        .groupingBy { it }.eachCount()

    private fun byteSummary(stored: StoredTestRun, namespace: String): String {
        val events = stored.run.events.filter { it.action is TestAction.Data }
        if (events.isEmpty()) return "Brak zdarzeń danych"
        val valid = events.mapNotNull { event -> event.correlation.references.filter { it.namespace == namespace }.singleOrNull()?.value?.toLongOrNull()?.takeIf { it >= 0 } }
        val sum = valid.fold(BigInteger.ZERO) { total, value -> total + BigInteger.valueOf(value) }
        return "Zapisana suma: $sum B; pomiarów: ${valid.size}/${events.size}" + if (valid.size < events.size) " · niepełne dane" else ""
    }
}

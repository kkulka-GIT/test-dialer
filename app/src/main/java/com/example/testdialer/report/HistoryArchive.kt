package com.example.testdialer.report

import com.example.testdialer.domain.*
import com.example.testdialer.domain.execution.CapturedTime
import com.example.testdialer.domain.execution.TimelineEntry
import com.example.testdialer.domain.execution.TimelineEntryKind
import com.example.testdialer.notes.RunNotesStore
import com.example.testdialer.persistence.StoredTestRun
import com.example.testdialer.review.BillingReview
import com.example.testdialer.review.BillingVerdict
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

data class HistoryArchiveEntry(
    val stored: StoredTestRun,
    val testerNote: String,
    val reviews: Map<EventId, BillingReview>,
    val interruptedAtMillis: Long,
)

/** Versioned full-history snapshot. Decoding is deliberately read-only. */
object HistoryArchive {
    const val MAX_FILE_BYTES = 16_777_216
    private const val FORMAT = "test-dialer-history"
    private const val MAX_RUNS = 2_000
    private const val MAX_STEPS = 100
    private const val MAX_EVENTS = 2_000
    private const val MAX_TIMELINE = 20_000

    fun read(input: InputStream): List<HistoryArchiveEntry> {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            require(output.size() + count <= MAX_FILE_BYTES) { "Plik przekracza 16 MB." }
            output.write(buffer, 0, count)
        }
        val text = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(output.toByteArray())).toString()
        return decode(text)
    }

    fun encode(entries: List<HistoryArchiveEntry>, exportedAtMillis: Long = System.currentTimeMillis()): String {
        require(entries.size <= MAX_RUNS) { "Historia zawiera za dużo sesji." }
        require(exportedAtMillis > 0)
        val runs = JSONArray()
        entries.forEach { entry ->
            validateAnnotations(entry)
            runs.put(JSONObject(RunReportFormatter.json(
                entry.stored,
                entry.testerNote,
                entry.reviews.mapKeys { it.key.value },
                entry.interruptedAtMillis,
            )))
        }
        return JSONObject().put("format", FORMAT).put("schemaVersion", 1)
            .put("exportedAtMillis", exportedAtMillis).put("runs", runs).toString(2)
            .also {
                require(it.toByteArray(Charsets.UTF_8).size <= MAX_FILE_BYTES) { "Kopia historii przekracza 16 MB." }
                decode(it)
            }
    }

    fun decode(text: String): List<HistoryArchiveEntry> {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_FILE_BYTES) { "Plik przekracza 16 MB." }
        val root = JSONObject(text)
        require(root.strictString("format") == FORMAT) { "To nie jest kopia historii Test Dialer." }
        require(root.strictInt("schemaVersion") == 1) { "Nieobsługiwana wersja kopii historii." }
        require(root.strictLong("exportedAtMillis") > 0) { "Nieprawidłowy czas utworzenia kopii." }
        val runs = root.getJSONArray("runs")
        require(runs.length() <= MAX_RUNS) { "Plik zawiera za dużo sesji." }
        val entries = (0 until runs.length()).map { decodeRun(runs.getJSONObject(it)) }
        require(entries.map { it.stored.run.id }.distinct().size == entries.size) { "Powtórzony identyfikator sesji." }
        val eventIds = entries.flatMap { it.stored.run.events }.map { it.id }
        require(eventIds.distinct().size == eventIds.size) { "Powtórzony identyfikator zdarzenia." }
        val timelineIds = entries.flatMap { it.stored.run.timeline }.map { it.id }
        require(timelineIds.distinct().size == timelineIds.size) { "Powtórzony identyfikator osi czasu." }
        entries.groupBy { it.stored.scenario.id to it.stored.scenario.version }.values.forEach { sameVersion ->
            require(sameVersion.map { it.stored.scenario }.distinct().size == 1) {
                "Ta sama wersja scenariusza ma różne definicje."
            }
        }
        return entries
    }

    private fun decodeRun(raw: JSONObject): HistoryArchiveEntry {
        require(raw.strictString("format") == "test-dialer-run") { "Nieprawidłowy wpis sesji." }
        require(raw.strictInt("schemaVersion") == 1) { "Nieobsługiwana wersja wpisu sesji." }
        val revision = raw.strictLong("revision").also { require(it >= 0) }
        val note = raw.strictString("testerNote").also { require(it.length <= RunNotesStore.MAX_LENGTH) }
        val scenarioRaw = raw.getJSONObject("scenario")
        val scenarioId = ScenarioId(scenarioRaw.limitedString("id", 200, false))
        val scenarioVersion = scenarioRaw.strictInt("version").also { require(it > 0) }
        val stepsRaw = scenarioRaw.getJSONArray("steps")
        require(stepsRaw.length() in 1..MAX_STEPS) { "Nieprawidłowa liczba kroków scenariusza." }
        val steps = (0 until stepsRaw.length()).map { index ->
            val step = stepsRaw.getJSONObject(index)
            ScenarioStepDefinition(
                StepId(step.limitedString("id", 200, false)),
                step.strictInt("order"),
                step.limitedString("title", 200, false),
                step.limitedString("instruction", 4_000, false),
                decodeAction(step.getJSONObject("action")),
                step.optionalObject("expectedResult")?.let {
                    ExpectedResult(it.limitedString("code", 500, false), it.limitedString("description", 4_000, false))
                },
            )
        }
        val scenario = ScenarioDefinition(
            scenarioId,
            scenarioVersion,
            scenarioRaw.limitedString("name", 200, false),
            scenarioRaw.optionalString("description", 4_000),
            steps,
        )
        val runRaw = raw.getJSONObject("run")
        val runId = RunId(runRaw.limitedString("id", 200, false))
        require(runRaw.strictString("scenarioId") == scenarioId.value && runRaw.strictInt("scenarioVersion") == scenarioVersion) {
            "Sesja wskazuje inny scenariusz."
        }
        val eventsRaw = runRaw.getJSONArray("events")
        require(eventsRaw.length() <= MAX_EVENTS) { "Sesja zawiera za dużo zdarzeń." }
        val events = (0 until eventsRaw.length()).map { index ->
            val event = eventsRaw.getJSONObject(index)
            val eventRunId = RunId(event.limitedString("runId", 200, false))
            require(eventRunId == runId) { "Zdarzenie należy do innej sesji." }
            val observation = event.optionalObject("observation")?.let { item ->
                Observation(
                    item.strictEnum<ObservationStatus>("status"),
                    item.strictEnum<ObservationSource>("source"),
                    item.limitedString("code", 500, false),
                    item.optionalString("description", 4_000),
                )
            }
            val correlation = event.getJSONObject("correlation")
            val referencesRaw = correlation.getJSONArray("references")
            require(referencesRaw.length() <= 500) { "Zdarzenie zawiera za dużo referencji." }
            TestEvent(
                EventId(event.limitedString("id", 200, false)),
                eventRunId,
                StepId(event.limitedString("stepId", 200, false)),
                decodeAction(event.getJSONObject("action")),
                event.strictLong("occurredAtMillis"),
                observation,
                CorrelationMetadata(
                    correlation.optionalString("sourceAddress", 2_000),
                    correlation.optionalString("destinationAddress", 2_000),
                    correlation.optionalString("subscriberAlias", 2_000),
                    (0 until referencesRaw.length()).map { refIndex ->
                        referencesRaw.getJSONObject(refIndex).let {
                            CorrelationReference(it.limitedString("namespace", 500, false), it.limitedString("value", 2_000, false))
                        }
                    },
                ),
            )
        }
        val timelineRaw = runRaw.getJSONArray("timeline")
        require(timelineRaw.length() <= MAX_TIMELINE) { "Sesja zawiera za długą oś czasu." }
        val timeline = (0 until timelineRaw.length()).map { index ->
            val item = timelineRaw.getJSONObject(index)
            val itemRunId = RunId(item.limitedString("runId", 200, false))
            require(itemRunId == runId) { "Wpis osi czasu należy do innej sesji." }
            TimelineEntry(
                TimelineEntryId(item.limitedString("id", 200, false)), itemRunId,
                item.strictLong("sequenceNumber"), item.strictEnum<TimelineEntryKind>("kind"),
                CapturedTime(item.strictLong("epochMillis"), item.strictLong("monotonicNanos")),
                item.optionalString("stepId", 200)?.let(::StepId),
                item.optionalString("attemptId", 200)?.let(::AttemptId),
                item.optionalString("relatedEventId", 200)?.let(::EventId),
            )
        }
        val completedAt = runRaw.optionalLong("completedAtMillis")
        val run = TestRun(
            runId, scenarioId, scenarioVersion, runRaw.strictEnum("status"),
            runRaw.strictLong("startedAtMillis"), completedAt, events, timeline,
        )
        TestRunScenarioValidator.requireValid(run, scenario)
        val annotations = raw.getJSONObject("testerAnnotations")
        require(annotations.strictInt("schemaVersion") == 1 && annotations.strictString("source") == "TESTER") {
            "Nieprawidłowe adnotacje testera."
        }
        val reviewsRaw = annotations.getJSONArray("billingReviews")
        require(reviewsRaw.length() <= events.size) { "Za dużo ocen billingu." }
        val eventIds = events.map { it.id }.toSet()
        val reviews = (0 until reviewsRaw.length()).associate { index ->
            val review = reviewsRaw.getJSONObject(index)
            val eventId = EventId(review.limitedString("eventId", 200, false))
            require(eventId in eventIds) { "Ocena billingu wskazuje brakujące zdarzenie." }
            eventId to BillingReview(
                review.limitedString("expected", 2_000, true),
                review.limitedString("actual", 2_000, true),
                review.strictEnum("verdict"),
                review.strictLong("reviewedAtMillis"),
            ).also { validateReview(it) }
        }
        require(reviews.size == reviewsRaw.length()) { "Powtórzona ocena billingu." }
        val entry = HistoryArchiveEntry(
            StoredTestRun(scenario, run, revision), note, reviews,
            annotations.optionalLong("markedInterruptedAtMillis") ?: 0L,
        )
        validateAnnotations(entry)
        return entry
    }

    private fun decodeAction(raw: JSONObject): TestAction = when (raw.strictString("serviceType")) {
        "VOICE" -> TestAction.Voice(raw.limitedString("destination", 500, true))
        "SMS" -> TestAction.Sms(raw.limitedString("destination", 500, true), raw.optionalText("message", 10_000))
        "DATA" -> TestAction.Data(raw.limitedString("target", 2_000, true))
        else -> throw IllegalArgumentException("Nieznany rodzaj działania.")
    }

    private fun validateAnnotations(entry: HistoryArchiveEntry) {
        require(entry.testerNote.length <= RunNotesStore.MAX_LENGTH)
        require(entry.interruptedAtMillis >= 0)
        val eventIds = entry.stored.run.events.map { it.id }.toSet()
        require(entry.reviews.keys.all { it in eventIds })
        entry.reviews.values.forEach(::validateReview)
    }

    private fun validateReview(review: BillingReview) {
        require(review.expected.length <= 2_000 && review.actual.length <= 2_000)
        require(review.reviewedAtMillis > 0)
        require(review.verdict == BillingVerdict.NOT_CHECKED || (review.expected.isNotBlank() && review.actual.isNotBlank()))
    }

    private fun JSONObject.strictString(key: String): String = get(key) as? String
        ?: throw IllegalArgumentException("Pole $key musi być tekstem.")
    private fun JSONObject.limitedString(key: String, max: Int, allowBlank: Boolean): String = strictString(key).also {
        require(it.length <= max && (allowBlank || it.isNotBlank())) { "Nieprawidłowe pole $key." }
    }
    private fun JSONObject.strictInt(key: String): Int = (get(key) as? Int)
        ?: throw IllegalArgumentException("Pole $key musi być liczbą całkowitą.")
    private fun JSONObject.strictLong(key: String): Long = when (val value = get(key)) {
        is Int -> value.toLong()
        is Long -> value
        else -> throw IllegalArgumentException("Pole $key musi być liczbą całkowitą.")
    }
    private fun JSONObject.optionalLong(key: String): Long? = if (!has(key) || isNull(key)) null else strictLong(key)
    private fun JSONObject.optionalString(key: String, max: Int): String? = if (!has(key) || isNull(key)) null else
        strictString(key).also { require(it.isNotBlank() && it.length <= max) { "Nieprawidłowe pole $key." } }
    private fun JSONObject.optionalText(key: String, max: Int): String? = if (!has(key) || isNull(key)) null else
        strictString(key).also { require(it.length <= max) { "Nieprawidłowe pole $key." } }
    private fun JSONObject.optionalObject(key: String): JSONObject? = if (!has(key) || isNull(key)) null else getJSONObject(key)
    private inline fun <reified T : Enum<T>> JSONObject.strictEnum(key: String): T =
        enumValues<T>().singleOrNull { it.name == strictString(key) }
            ?: throw IllegalArgumentException("Nieprawidłowa wartość pola $key.")
}

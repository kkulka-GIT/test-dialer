package com.example.testdialer.report

import com.example.testdialer.domain.TestAction
import com.example.testdialer.persistence.StoredTestRun
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/** Versioned, read-only snapshot. Epoch milliseconds retain exact correlation timestamps. */
object RunReportFormatter {
    fun json(stored: StoredTestRun, testerNote: String = ""): String {
        val run = stored.run
        return obj(
            "format" to "test-dialer-run", "schemaVersion" to 1,
            "revision" to stored.revision,
            "testerNote" to testerNote,
            "timeSemantics" to "epochMillis=UTC; sequenceNumber=run order; monotonicNanos=source process only",
            "scenario" to obj(
                "id" to stored.scenario.id.value, "version" to stored.scenario.version,
                "name" to stored.scenario.name, "description" to stored.scenario.description,
                "steps" to array(stored.scenario.steps.map { step -> obj(
                    "id" to step.id.value, "order" to step.order, "title" to step.title,
                    "instruction" to step.instruction, "action" to action(step.action),
                    "expectedResult" to step.expectedResult?.let { obj("code" to it.code, "description" to it.description) },
                ) }),
            ),
            "run" to obj(
                "id" to run.id.value, "scenarioId" to run.scenarioId.value,
                "scenarioVersion" to run.scenarioVersion, "status" to run.status.name,
                "startedAtMillis" to run.startedAtMillis, "startedAtUtc" to utc(run.startedAtMillis),
                "completedAtMillis" to run.completedAtMillis,
                "completedAtUtc" to run.completedAtMillis?.let(::utc),
                "events" to array(run.events.map { event -> obj(
                    "id" to event.id.value, "runId" to event.runId.value, "stepId" to event.stepId.value,
                    "occurredAtMillis" to event.occurredAtMillis, "occurredAtUtc" to utc(event.occurredAtMillis),
                    "action" to action(event.action),
                    "observation" to event.observation?.let { obj(
                        "status" to it.status.name, "source" to it.source.name,
                        "code" to it.code, "description" to it.description,
                    ) },
                    "correlation" to obj(
                        "sourceAddress" to event.correlation.sourceAddress,
                        "destinationAddress" to event.correlation.destinationAddress,
                        "subscriberAlias" to event.correlation.subscriberAlias,
                        "references" to array(event.correlation.references.map { obj("namespace" to it.namespace, "value" to it.value) }),
                    ),
                ) }),
                "timeline" to array(run.timeline.map { entry -> obj(
                    "id" to entry.id.value, "runId" to entry.runId.value,
                    "sequenceNumber" to entry.sequenceNumber, "kind" to entry.kind.name,
                    "epochMillis" to entry.capturedAt.epochMillis, "utc" to utc(entry.capturedAt.epochMillis),
                    "monotonicNanos" to entry.capturedAt.monotonicNanos,
                    "stepId" to entry.stepId?.value, "attemptId" to entry.attemptId?.value,
                    "relatedEventId" to entry.relatedEventId?.value,
                ) }),
            ),
        ).toString(2)
    }

    fun text(stored: StoredTestRun, testerNote: String = ""): String = buildString {
        val run = stored.run
        appendLine("TEST DIALER — RAPORT RUNU")
        appendLine(stored.scenario.name)
        appendLine("Run ID: ${run.id.value}")
        appendLine("Status: ${run.status.name} | Rewizja: ${stored.revision}")
        appendLine("Start UTC: ${utc(run.startedAtMillis)}")
        appendLine("Koniec UTC: ${run.completedAtMillis?.let(::utc) ?: "—"}")
        appendLine("Zdarzenia: ${run.events.size}")
        appendLine("Obserwacje nie są oceną poprawności naliczenia. Pełna oś czasu jest w JSON.")
        if (testerNote.isNotBlank()) {
            appendLine()
            appendLine("NOTATKA TESTERA")
            appendLine(testerNote)
        }
        run.events.forEachIndexed { index, event ->
            appendLine()
            appendLine("${index + 1}. ${event.action.serviceType.name} — ${utc(event.occurredAtMillis)}")
            appendLine("Event ID: ${event.id.value}")
            appendLine("Step ID: ${event.stepId.value}")
            when (val action = event.action) {
                is TestAction.Voice -> appendLine("Numer: ${action.destination}")
                is TestAction.Sms -> { appendLine("Numer: ${action.destination}"); appendLine("SMS: ${action.message ?: "—"}") }
                is TestAction.Data -> appendLine("Cel: ${action.target}")
            }
            event.observation?.let {
                appendLine("Obserwacja: ${it.status.name} | ${it.source.name} | ${it.code}")
                it.description?.let(::appendLine)
            } ?: appendLine("Obserwacja: brak")
            event.correlation.sourceAddress?.let { appendLine("Źródło: $it") }
            event.correlation.destinationAddress?.let { appendLine("Cel korelacji: $it") }
            event.correlation.subscriberAlias?.let { appendLine("Abonent: $it") }
            event.correlation.references.forEach { appendLine("${it.namespace}: ${it.value}") }
        }
    }

    /** RFC-style quoting plus spreadsheet formula neutralization for every user-controlled cell. */
    fun csv(stored: StoredTestRun, testerNote: String = ""): String = buildString {
        val headers = listOf("run_id", "scenario", "run_status", "event_id", "step_id", "service", "occurred_at_utc",
            "epoch_millis", "destination_or_target", "message", "observation_status", "observation_source", "observation_code", "references_json", "tester_note")
        appendLine(headers.joinToString(",") { csvCell(it) })
        val events = stored.run.events.map { it as com.example.testdialer.domain.TestEvent? }.ifEmpty { listOf(null) }
        events.forEach { event ->
            val target = when (val action = event?.action) {
                is TestAction.Voice -> action.destination
                is TestAction.Sms -> action.destination
                is TestAction.Data -> action.target
                null -> ""
            }
            val values = listOf(stored.run.id.value, stored.scenario.name, stored.run.status.name,
                event?.id?.value.orEmpty(), event?.stepId?.value.orEmpty(), event?.action?.serviceType?.name.orEmpty(),
                event?.occurredAtMillis?.let(::utc).orEmpty(), event?.occurredAtMillis?.toString().orEmpty(), target,
                (event?.action as? TestAction.Sms)?.message.orEmpty(), event?.observation?.status?.name.orEmpty(),
                event?.observation?.source?.name.orEmpty(), event?.observation?.code.orEmpty(),
                event?.correlation?.references?.let { refs -> array(refs.map { obj("namespace" to it.namespace, "value" to it.value) }).toString() }.orEmpty(), testerNote)
            append(values.joinToString(",") { csvCell(it) }); append("\r\n")
        }
    }

    internal fun csvCell(value: String): String {
        val safe = if (value.trimStart().firstOrNull() in listOf('=', '+', '-', '@')) "'" + value else value
        return "\"" + safe.replace("\"", "\"\"") + "\""
    }

    private fun action(action: TestAction): JSONObject = when (action) {
        is TestAction.Voice -> obj("serviceType" to "VOICE", "destination" to action.destination)
        is TestAction.Sms -> obj("serviceType" to "SMS", "destination" to action.destination, "message" to action.message)
        is TestAction.Data -> obj("serviceType" to "DATA", "target" to action.target)
    }
    private fun utc(millis: Long) = Instant.ofEpochMilli(millis).toString()
    private fun obj(vararg entries: Pair<String, Any?>) = JSONObject().apply {
        entries.forEach { (key, value) -> put(key, value ?: JSONObject.NULL) }
    }
    private fun array(values: List<Any>) = JSONArray().apply { values.forEach { put(it) } }
}

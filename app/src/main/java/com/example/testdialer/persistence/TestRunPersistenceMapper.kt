package com.example.testdialer.persistence

import com.example.testdialer.domain.AttemptId
import com.example.testdialer.domain.CorrelationMetadata
import com.example.testdialer.domain.CorrelationReference
import com.example.testdialer.domain.EventId
import com.example.testdialer.domain.ExpectedResult
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
import com.example.testdialer.domain.TestRunScenarioValidator
import com.example.testdialer.domain.TestRunStatus
import com.example.testdialer.domain.TimelineEntryId
import com.example.testdialer.domain.execution.CapturedTime
import com.example.testdialer.domain.execution.TimelineEntry
import com.example.testdialer.domain.execution.TimelineEntryKind

data class StoredTestRun(
    val scenario: ScenarioDefinition,
    val run: TestRun,
    val revision: Long,
)

object TestRunPersistenceMapper {
    fun toPersistence(scenario: ScenarioDefinition, run: TestRun, revision: Long = 0L): PersistenceSnapshot {
        TestRunScenarioValidator.requireValid(run, scenario)
        return PersistenceSnapshot(
            scenario = ScenarioEntity(
                scenarioId = scenario.id.value,
                version = scenario.version,
                name = scenario.name,
                description = scenario.description,
            ),
            scenarioSteps = scenario.steps.map { step ->
                val action = encodeAction(step.action)
                ScenarioStepEntity(
                    scenarioId = scenario.id.value,
                    scenarioVersion = scenario.version,
                    stepId = step.id.value,
                    stepOrder = step.order,
                    title = step.title,
                    instruction = step.instruction,
                    actionKind = action.kind,
                    actionDestinationOrTarget = action.destinationOrTarget,
                    actionMessage = action.message,
                    expectedResultCode = step.expectedResult?.code,
                    expectedResultDescription = step.expectedResult?.description,
                )
            },
            run = TestRunEntity(
                runId = run.id.value,
                scenarioId = run.scenarioId.value,
                scenarioVersion = run.scenarioVersion,
                status = run.status.name,
                startedAtMillis = run.startedAtMillis,
                completedAtMillis = run.completedAtMillis,
                revision = revision,
            ),
            events = run.events.mapIndexed { index, event ->
                val action = encodeAction(event.action)
                TestEventEntity(
                    eventId = event.id.value,
                    runId = event.runId.value,
                    stepId = event.stepId.value,
                    eventOrder = index,
                    actionKind = action.kind,
                    actionDestinationOrTarget = action.destinationOrTarget,
                    actionMessage = action.message,
                    occurredAtMillis = event.occurredAtMillis,
                    observationStatus = event.observation?.status?.name,
                    observationSource = event.observation?.source?.name,
                    observationCode = event.observation?.code,
                    observationDescription = event.observation?.description,
                    correlationSourceAddress = event.correlation.sourceAddress,
                    correlationDestinationAddress = event.correlation.destinationAddress,
                    correlationSubscriberAlias = event.correlation.subscriberAlias,
                )
            },
            references = run.events.flatMap { event ->
                event.correlation.references.mapIndexed { ordinal, reference ->
                    CorrelationReferenceEntity(
                        eventId = event.id.value,
                        ordinal = ordinal,
                        namespace = reference.namespace,
                        value = reference.value,
                    )
                }
            },
            timeline = run.timeline.map { entry ->
                TimelineEntryEntity(
                    timelineEntryId = entry.id.value,
                    runId = entry.runId.value,
                    sequenceNumber = entry.sequenceNumber,
                    kind = entry.kind.name,
                    epochMillis = entry.capturedAt.epochMillis,
                    monotonicNanos = entry.capturedAt.monotonicNanos,
                    stepId = entry.stepId?.value,
                    attemptId = entry.attemptId?.value,
                    relatedEventId = entry.relatedEventId?.value,
                )
            },
        )
    }

    fun fromPersistence(snapshot: PersistenceSnapshot): StoredTestRun {
        val scenario = ScenarioDefinition(
            id = ScenarioId(snapshot.scenario.scenarioId),
            version = snapshot.scenario.version,
            name = snapshot.scenario.name,
            description = snapshot.scenario.description,
            steps = snapshot.scenarioSteps.map { step ->
                require(step.scenarioId == snapshot.scenario.scenarioId &&
                    step.scenarioVersion == snapshot.scenario.version) {
                    "Scenario step belongs to another scenario version"
                }
                val expected = decodeExpectedResult(step)
                ScenarioStepDefinition(
                    id = StepId(step.stepId),
                    order = step.stepOrder,
                    title = step.title,
                    instruction = step.instruction,
                    action = decodeAction(step.actionKind, step.actionDestinationOrTarget, step.actionMessage),
                    expectedResult = expected,
                )
            },
        )
        val references = snapshot.references.groupBy { it.eventId }
        val events = snapshot.events.map { event ->
            TestEvent(
                id = EventId(event.eventId),
                runId = RunId(event.runId),
                stepId = StepId(event.stepId),
                action = decodeAction(event.actionKind, event.actionDestinationOrTarget, event.actionMessage),
                occurredAtMillis = event.occurredAtMillis,
                observation = decodeObservation(event),
                correlation = CorrelationMetadata(
                    sourceAddress = event.correlationSourceAddress,
                    destinationAddress = event.correlationDestinationAddress,
                    subscriberAlias = event.correlationSubscriberAlias,
                    references = references[event.eventId].orEmpty()
                        .sortedBy { it.ordinal }
                        .map { CorrelationReference(it.namespace, it.value) },
                ),
            )
        }
        val run = TestRun(
            id = RunId(snapshot.run.runId),
            scenarioId = ScenarioId(snapshot.run.scenarioId),
            scenarioVersion = snapshot.run.scenarioVersion,
            status = strictEnum<TestRunStatus>(snapshot.run.status, "run status"),
            startedAtMillis = snapshot.run.startedAtMillis,
            completedAtMillis = snapshot.run.completedAtMillis,
            events = events,
            timeline = snapshot.timeline.map { entry ->
                TimelineEntry(
                    id = TimelineEntryId(entry.timelineEntryId),
                    runId = RunId(entry.runId),
                    sequenceNumber = entry.sequenceNumber,
                    kind = strictEnum<TimelineEntryKind>(entry.kind, "timeline kind"),
                    capturedAt = CapturedTime(entry.epochMillis, entry.monotonicNanos),
                    stepId = entry.stepId?.let(::StepId),
                    attemptId = entry.attemptId?.let(::AttemptId),
                    relatedEventId = entry.relatedEventId?.let(::EventId),
                )
            },
        )
        TestRunScenarioValidator.requireValid(run, scenario)
        return StoredTestRun(scenario, run, snapshot.run.revision)
    }

    private data class EncodedAction(
        val kind: String,
        val destinationOrTarget: String,
        val message: String?,
    )

    private fun encodeAction(action: TestAction): EncodedAction = when (action) {
        is TestAction.Voice -> EncodedAction("VOICE", action.destination, null)
        is TestAction.Sms -> EncodedAction("SMS", action.destination, action.message)
        is TestAction.Data -> EncodedAction("DATA", action.target, null)
    }

    private fun decodeAction(kind: String, destinationOrTarget: String, message: String?): TestAction =
        when (kind) {
            "VOICE" -> {
                require(message == null) { "VOICE action must not contain a message" }
                TestAction.Voice(destinationOrTarget)
            }
            "SMS" -> TestAction.Sms(destinationOrTarget, message)
            "DATA" -> {
                require(message == null) { "DATA action must not contain a message" }
                TestAction.Data(destinationOrTarget)
            }
            else -> throw IllegalArgumentException("Unknown action kind: $kind")
        }

    private fun decodeExpectedResult(step: ScenarioStepEntity): ExpectedResult? {
        val code = step.expectedResultCode
        val description = step.expectedResultDescription
        require((code == null) == (description == null)) {
            "Expected result code and description must both be present or absent"
        }
        return if (code == null) null else ExpectedResult(code, requireNotNull(description))
    }

    private fun decodeObservation(event: TestEventEntity): Observation? {
        val required = listOf(event.observationStatus, event.observationSource, event.observationCode)
        if (required.all { it == null }) {
            require(event.observationDescription == null) {
                "Observation description cannot exist without an observation"
            }
            return null
        }
        require(required.all { it != null }) { "Stored observation is incomplete" }
        return Observation(
            status = strictEnum(requireNotNull(event.observationStatus), "observation status"),
            source = strictEnum(requireNotNull(event.observationSource), "observation source"),
            code = requireNotNull(event.observationCode),
            description = event.observationDescription,
        )
    }

    private inline fun <reified T : Enum<T>> strictEnum(value: String, label: String): T =
        enumValues<T>().singleOrNull { it.name == value }
            ?: throw IllegalArgumentException("Unknown $label: $value")
}

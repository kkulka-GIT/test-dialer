package com.example.testdialer.persistence

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "scenarios",
    primaryKeys = ["scenarioId", "version"],
)
data class ScenarioEntity(
    val scenarioId: String,
    val version: Int,
    val name: String,
    val description: String?,
)

@Entity(
    tableName = "scenario_steps",
    primaryKeys = ["scenarioId", "scenarioVersion", "stepId"],
    foreignKeys = [
        ForeignKey(
            entity = ScenarioEntity::class,
            parentColumns = ["scenarioId", "version"],
            childColumns = ["scenarioId", "scenarioVersion"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["scenarioId", "scenarioVersion"]),
        Index(value = ["scenarioId", "scenarioVersion", "stepOrder"], unique = true),
    ],
)
data class ScenarioStepEntity(
    val scenarioId: String,
    val scenarioVersion: Int,
    val stepId: String,
    val stepOrder: Int,
    val title: String,
    val instruction: String,
    val actionKind: String,
    val actionDestinationOrTarget: String,
    val actionMessage: String?,
    val expectedResultCode: String?,
    val expectedResultDescription: String?,
)

@Entity(
    tableName = "test_runs",
    foreignKeys = [
        ForeignKey(
            entity = ScenarioEntity::class,
            parentColumns = ["scenarioId", "version"],
            childColumns = ["scenarioId", "scenarioVersion"],
        ),
    ],
    indices = [Index(value = ["scenarioId", "scenarioVersion"])],
)
data class TestRunEntity(
    @androidx.room.PrimaryKey val runId: String,
    val scenarioId: String,
    val scenarioVersion: Int,
    val status: String,
    val startedAtMillis: Long,
    val completedAtMillis: Long?,
    val revision: Long,
)

@Entity(
    tableName = "test_events",
    foreignKeys = [
        ForeignKey(
            entity = TestRunEntity::class,
            parentColumns = ["runId"],
            childColumns = ["runId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["runId"]),
        Index(value = ["eventId", "runId"], unique = true),
    ],
)
data class TestEventEntity(
    @androidx.room.PrimaryKey val eventId: String,
    val runId: String,
    val stepId: String,
    val actionKind: String,
    val actionDestinationOrTarget: String,
    val actionMessage: String?,
    val occurredAtMillis: Long,
    val observationStatus: String?,
    val observationSource: String?,
    val observationCode: String?,
    val observationDescription: String?,
    val correlationSourceAddress: String?,
    val correlationDestinationAddress: String?,
    val correlationSubscriberAlias: String?,
)

@Entity(
    tableName = "correlation_references",
    primaryKeys = ["eventId", "ordinal"],
    foreignKeys = [
        ForeignKey(
            entity = TestEventEntity::class,
            parentColumns = ["eventId"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["eventId"]),
        Index(value = ["eventId", "namespace", "value"], unique = true),
    ],
)
data class CorrelationReferenceEntity(
    val eventId: String,
    val ordinal: Int,
    val namespace: String,
    val value: String,
)

@Entity(
    tableName = "timeline_entries",
    foreignKeys = [
        ForeignKey(
            entity = TestRunEntity::class,
            parentColumns = ["runId"],
            childColumns = ["runId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TestEventEntity::class,
            parentColumns = ["eventId", "runId"],
            childColumns = ["relatedEventId", "runId"],
        ),
    ],
    indices = [
        Index(value = ["runId"]),
        Index(value = ["runId", "sequenceNumber"], unique = true),
        Index(value = ["relatedEventId", "runId"]),
    ],
)
data class TimelineEntryEntity(
    @androidx.room.PrimaryKey val timelineEntryId: String,
    val runId: String,
    val sequenceNumber: Long,
    val kind: String,
    val epochMillis: Long,
    val monotonicNanos: Long,
    val stepId: String?,
    val attemptId: String?,
    val relatedEventId: String?,
)

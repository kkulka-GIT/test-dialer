package com.example.testdialer.persistence

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update

data class PersistenceSnapshot(
    val scenario: ScenarioEntity,
    val scenarioSteps: List<ScenarioStepEntity>,
    val run: TestRunEntity,
    val events: List<TestEventEntity>,
    val references: List<CorrelationReferenceEntity>,
    val timeline: List<TimelineEntryEntity>,
)

class SnapshotConflictException(message: String) : IllegalStateException(message)

@Dao
abstract class TestRunDao {
    @Query("SELECT * FROM scenarios WHERE scenarioId = :id AND version = :version")
    abstract fun findScenario(id: String, version: Int): ScenarioEntity?

    @Query("SELECT * FROM scenario_steps WHERE scenarioId = :id AND scenarioVersion = :version ORDER BY stepOrder")
    abstract fun findScenarioSteps(id: String, version: Int): List<ScenarioStepEntity>

    @Query("SELECT * FROM test_runs WHERE runId = :runId")
    abstract fun findRun(runId: String): TestRunEntity?

    @Query("SELECT * FROM test_events WHERE runId = :runId ORDER BY eventOrder")
    abstract fun findEvents(runId: String): List<TestEventEntity>

    @Query("SELECT r.* FROM correlation_references r INNER JOIN test_events e ON e.eventId = r.eventId WHERE e.runId = :runId ORDER BY r.eventId, r.ordinal")
    abstract fun findReferences(runId: String): List<CorrelationReferenceEntity>

    @Query("SELECT * FROM timeline_entries WHERE runId = :runId ORDER BY sequenceNumber")
    abstract fun findTimeline(runId: String): List<TimelineEntryEntity>

    @Query("SELECT * FROM test_runs ORDER BY startedAtMillis DESC, runId")
    abstract fun listRuns(): List<TestRunEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract fun insertScenario(entity: ScenarioEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract fun insertScenarioSteps(entities: List<ScenarioStepEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract fun insertRun(entity: TestRunEntity)

    @Update
    protected abstract fun updateRun(entity: TestRunEntity): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract fun insertEvents(entities: List<TestEventEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract fun insertReferences(entities: List<CorrelationReferenceEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract fun insertTimeline(entities: List<TimelineEntryEntity>)

    @Query("DELETE FROM timeline_entries WHERE runId = :runId")
    protected abstract fun deleteTimeline(runId: String)

    @Query("DELETE FROM correlation_references WHERE eventId IN (SELECT eventId FROM test_events WHERE runId = :runId)")
    protected abstract fun deleteReferences(runId: String)

    @Query("DELETE FROM test_events WHERE runId = :runId")
    protected abstract fun deleteEvents(runId: String)

    @Query("DELETE FROM test_runs WHERE runId = :runId")
    abstract fun deleteRun(runId: String): Int

    @androidx.room.Transaction
    open fun loadSnapshot(runId: String): PersistenceSnapshot? {
        val run = findRun(runId) ?: return null
        val scenario = requireNotNull(findScenario(run.scenarioId, run.scenarioVersion)) {
            "Stored run references a missing scenario"
        }
        return PersistenceSnapshot(
            scenario = scenario,
            scenarioSteps = findScenarioSteps(run.scenarioId, run.scenarioVersion),
            run = run,
            events = findEvents(runId),
            references = findReferences(runId),
            timeline = findTimeline(runId),
        )
    }

    @androidx.room.Transaction
    open fun storeSnapshot(snapshot: PersistenceSnapshot, expectedRevision: Long?): Long {
        val storedScenario = findScenario(snapshot.scenario.scenarioId, snapshot.scenario.version)
        if (storedScenario == null) {
            insertScenario(snapshot.scenario)
            insertScenarioSteps(snapshot.scenarioSteps)
        } else {
            if (storedScenario != snapshot.scenario ||
                findScenarioSteps(storedScenario.scenarioId, storedScenario.version) != snapshot.scenarioSteps
            ) {
                throw SnapshotConflictException("Scenario identity and version already have a different definition")
            }
        }

        val existing = loadSnapshot(snapshot.run.runId)
        val nextRevision = if (existing == null) {
            if (expectedRevision != null) {
                throw SnapshotConflictException("Expected an existing snapshot")
            }
            0L
        } else {
            if (expectedRevision != existing.run.revision) {
                throw SnapshotConflictException("Snapshot revision is stale")
            }
            requireExtension(existing, snapshot)
            existing.run.revision + 1L
        }

        val nextRun = snapshot.run.copy(revision = nextRevision)
        if (existing == null) {
            insertRun(nextRun)
        } else if (updateRun(nextRun) != 1) {
            throw SnapshotConflictException("Snapshot update did not affect exactly one run")
        }

        deleteTimeline(nextRun.runId)
        deleteReferences(nextRun.runId)
        deleteEvents(nextRun.runId)
        insertEvents(snapshot.events)
        insertReferences(snapshot.references)
        insertTimeline(snapshot.timeline)
        return nextRevision
    }

    private fun requireExtension(existing: PersistenceSnapshot, replacement: PersistenceSnapshot) {
        if (existing.run.status == "COMPLETED" || existing.run.status == "ABORTED") {
            throw SnapshotConflictException("Terminal snapshots are immutable")
        }
        if (replacement.timeline.take(existing.timeline.size) != existing.timeline) {
            throw SnapshotConflictException("Timeline history cannot be rewritten")
        }
        val replacementEvents = replacement.events.associateBy { it.eventId }
        if (existing.events.any { replacementEvents[it.eventId] != it }) {
            throw SnapshotConflictException("Event history cannot be rewritten or removed")
        }
        val replacementReferences = replacement.references.groupBy { it.eventId }
        if (existing.references.groupBy { it.eventId }.any { (eventId, refs) ->
                replacementReferences[eventId] != refs
            }
        ) {
            throw SnapshotConflictException("Correlation history cannot be rewritten or removed")
        }
    }
}

@Database(
    entities = [
        ScenarioEntity::class,
        ScenarioStepEntity::class,
        TestRunEntity::class,
        TestEventEntity::class,
        CorrelationReferenceEntity::class,
        TimelineEntryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class TestDialerDatabase : RoomDatabase() {
    abstract fun testRunDao(): TestRunDao

    companion object {
        const val DATABASE_NAME = "test-dialer-history.db"

        fun create(context: Context): TestDialerDatabase =
            Room.databaseBuilder(context, TestDialerDatabase::class.java, DATABASE_NAME)
                .build()
    }
}

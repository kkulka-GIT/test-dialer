package com.example.testdialer.persistence

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import android.database.sqlite.SQLiteConstraintException
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

data class PersistenceSnapshot(
    val scenario: ScenarioEntity,
    val scenarioSteps: List<ScenarioStepEntity>,
    val run: TestRunEntity,
    val events: List<TestEventEntity>,
    val references: List<CorrelationReferenceEntity>,
    val timeline: List<TimelineEntryEntity>,
)

data class AnnotationSnapshot(
    val note: RunNoteEntity?,
    val reviews: List<BillingReviewEntity>,
    val interruption: RunInterruptionEntity?,
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

    @Query("SELECT DISTINCT actionKind FROM test_events WHERE runId = :runId")
    abstract fun serviceTypes(runId: String): List<String>

    @Query("SELECT COUNT(*) FROM test_events WHERE runId = :runId")
    abstract fun eventCount(runId: String): Int

    @Query("SELECT COUNT(*) FROM timeline_entries WHERE runId = :runId")
    abstract fun timelineCount(runId: String): Int

    @Query("SELECT * FROM run_notes WHERE runId = :runId")
    abstract fun findRunNote(runId: String): RunNoteEntity?

    @Query("SELECT r.* FROM billing_reviews r INNER JOIN test_events e ON e.eventId = r.eventId WHERE e.runId = :runId ORDER BY r.eventId")
    abstract fun findBillingReviews(runId: String): List<BillingReviewEntity>

    @Query("SELECT * FROM run_interruptions WHERE runId = :runId")
    abstract fun findRunInterruption(runId: String): RunInterruptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun upsertRunNote(entity: RunNoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun upsertBillingReviews(entities: List<BillingReviewEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun upsertRunInterruption(entity: RunInterruptionEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract fun insertScenario(entity: ScenarioEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract fun insertScenarioSteps(entities: List<ScenarioStepEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract fun insertRun(entity: TestRunEntity)

    @Query("""
        UPDATE test_runs
        SET status = :status,
            completedAtMillis = :completedAtMillis,
            revision = revision + 1
        WHERE runId = :runId
          AND revision = :expectedRevision
          AND scenarioId = :scenarioId
          AND scenarioVersion = :scenarioVersion
          AND startedAtMillis = :startedAtMillis
    """)
    protected abstract fun compareAndSetRun(
        runId: String,
        expectedRevision: Long,
        scenarioId: String,
        scenarioVersion: Int,
        startedAtMillis: Long,
        status: String,
        completedAtMillis: Long?,
    ): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract fun insertEvents(entities: List<TestEventEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract fun insertReferences(entities: List<CorrelationReferenceEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract fun insertTimeline(entities: List<TimelineEntryEntity>)

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
    open fun loadAnnotations(runId: String): AnnotationSnapshot = AnnotationSnapshot(
        findRunNote(runId), findBillingReviews(runId), findRunInterruption(runId),
    )

    /** One transaction boundary for future history restore; callers validate values before entry. */
    @androidx.room.Transaction
    open fun storeAnnotations(snapshot: AnnotationSnapshot) {
        snapshot.note?.let(::upsertRunNote)
        upsertBillingReviews(snapshot.reviews)
        snapshot.interruption?.let(::upsertRunInterruption)
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
            try {
                insertRun(nextRun)
            } catch (_: SQLiteConstraintException) {
                throw SnapshotConflictException("Run was created concurrently")
            }
        } else if (compareAndSetRun(
                runId = nextRun.runId,
                expectedRevision = requireNotNull(expectedRevision),
                scenarioId = nextRun.scenarioId,
                scenarioVersion = nextRun.scenarioVersion,
                startedAtMillis = nextRun.startedAtMillis,
                status = nextRun.status,
                completedAtMillis = nextRun.completedAtMillis,
            ) != 1
        ) {
            throw SnapshotConflictException("Snapshot revision is stale")
        }

        // Existing evidence is immutable. Deleting and reinserting it would cascade-delete
        // billing reviews; append only after validating the complete extension above.
        val existingEventIds = existing?.events?.map { it.eventId }?.toSet().orEmpty()
        val existingReferences = existing?.references?.toSet().orEmpty()
        insertEvents(snapshot.events.filter { it.eventId !in existingEventIds })
        insertReferences(snapshot.references.filter { it !in existingReferences })
        insertTimeline(snapshot.timeline.drop(existing?.timeline?.size ?: 0))
        return nextRevision
    }

    private fun requireExtension(existing: PersistenceSnapshot, replacement: PersistenceSnapshot) {
        if (replacement.run.runId != existing.run.runId ||
            replacement.run.scenarioId != existing.run.scenarioId ||
            replacement.run.scenarioVersion != existing.run.scenarioVersion ||
            replacement.run.startedAtMillis != existing.run.startedAtMillis
        ) {
            throw SnapshotConflictException("Run identity metadata is immutable")
        }
        if (existing.run.status == "COMPLETED" || existing.run.status == "ABORTED") {
            throw SnapshotConflictException("Terminal snapshots are immutable")
        }
        val allowedStatuses = when (existing.run.status) {
            "CREATED" -> setOf("CREATED", "RUNNING")
            "RUNNING" -> setOf("RUNNING", "COMPLETED", "ABORTED")
            else -> throw SnapshotConflictException("Unknown stored run status")
        }
        if (replacement.run.status !in allowedStatuses) {
            throw SnapshotConflictException(
                "Run status transition ${existing.run.status} -> ${replacement.run.status} is not allowed",
            )
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
        RunNoteEntity::class,
        BillingReviewEntity::class,
        RunInterruptionEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class TestDialerDatabase : RoomDatabase() {
    abstract fun testRunDao(): TestRunDao

    companion object {
        const val DATABASE_NAME = "test-dialer-history.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `run_notes` (`runId` TEXT NOT NULL, `text` TEXT NOT NULL, PRIMARY KEY(`runId`), FOREIGN KEY(`runId`) REFERENCES `test_runs`(`runId`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_run_notes_runId` ON `run_notes` (`runId`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `billing_reviews` (`eventId` TEXT NOT NULL, `expected` TEXT NOT NULL, `actual` TEXT NOT NULL, `verdict` TEXT NOT NULL, `reviewedAtMillis` INTEGER NOT NULL, PRIMARY KEY(`eventId`), FOREIGN KEY(`eventId`) REFERENCES `test_events`(`eventId`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_billing_reviews_eventId` ON `billing_reviews` (`eventId`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `run_interruptions` (`runId` TEXT NOT NULL, `interruptedAtMillis` INTEGER NOT NULL, PRIMARY KEY(`runId`), FOREIGN KEY(`runId`) REFERENCES `test_runs`(`runId`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_run_interruptions_runId` ON `run_interruptions` (`runId`)")
            }
        }

        fun create(context: Context): TestDialerDatabase =
            Room.databaseBuilder(context, TestDialerDatabase::class.java, DATABASE_NAME)
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}

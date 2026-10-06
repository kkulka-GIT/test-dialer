package com.example.testdialer.persistence

import android.content.Context
import android.os.Looper

/** Shared process cache keeps Views free of Room queries. Writes publish only after commit. */
class RoomAnnotationStore(
    context: Context,
    private val dao: TestRunDao,
    private val migrateLegacy: () -> Unit = { LegacyAnnotationMigrator(context, dao).migrate(); Unit },
) {
    private val lock = Any()
    private data class Cache(
        val notes: Map<String, RunNoteEntity>,
        val reviews: Map<String, BillingReviewEntity>,
        val interruptions: Map<String, RunInterruptionEntity>,
    )
    @Volatile private var cache: Cache? = null
    @Volatile private var attempted = false

    /** Call from a worker. A failed startup retains the legacy read fallback for this process. */
    fun initialize(): Boolean {
        check(Looper.myLooper() != Looper.getMainLooper()) { "Annotation initialization requires a worker" }
        synchronized(lock) {
            if (!attempted) {
                attempted = true
                runCatching {
                    migrateLegacy()
                    cache = Cache(dao.allRunNotes().associateBy { it.runId },
                        dao.allBillingReviews().associateBy { it.eventId },
                        dao.allRunInterruptions().associateBy { it.runId })
                }
            }
            return cache != null
        }
    }

    fun ready(): Boolean = if (Looper.myLooper() == Looper.getMainLooper()) cache != null else initialize()
    fun note(id: String): RunNoteEntity? = cache?.notes?.get(id)
    fun review(id: String): BillingReviewEntity? = cache?.reviews?.get(id)
    fun interruption(id: String): RunInterruptionEntity? = cache?.interruptions?.get(id)

    /** Worker-only restore; validate the whole archive before the database transaction. */
    fun restoreHistory(entries: List<com.example.testdialer.report.HistoryArchiveEntry>): HistoryRestoreResult {
        check(Looper.myLooper() != Looper.getMainLooper()) { "History restore requires a worker" }
        // Reuse archive limits and cross-run validation, also for in-memory callers.
        val validated = com.example.testdialer.report.HistoryArchive.decode(
            com.example.testdialer.report.HistoryArchive.encode(entries),
        )
        val records = validated.map { entry ->
            HistoryRestoreRecord(
                TestRunPersistenceMapper.toPersistence(entry.stored.scenario, entry.stored.run, entry.stored.revision),
                AnnotationSnapshot(
                    RunNoteEntity(entry.stored.run.id.value, entry.testerNote),
                    entry.reviews.map { (id, review) -> BillingReviewEntity(
                        id.value, review.expected, review.actual, review.verdict.name, review.reviewedAtMillis,
                    ) },
                    entry.interruptedAtMillis.takeIf { it > 0 }?.let {
                        RunInterruptionEntity(entry.stored.run.id.value, it)
                    },
                ),
            )
        }
        check(initialize()) { "Annotation storage is unavailable" }
        return synchronized(lock) {
            // Reads inside the transaction: hydration failure rolls back the restore too.
            val restored = dao.restoreHistoryWithCache(records)
            cache = Cache(restored.notes.associateBy { it.runId }, restored.reviews.associateBy { it.eventId },
                restored.interruptions.associateBy { it.runId })
            restored.result
        }
    }

    fun save(snapshot: AnnotationSnapshot): Boolean {
        check(Looper.myLooper() != Looper.getMainLooper()) { "Annotation writes require a worker" }
        if (!initialize()) return false
        return synchronized(lock) {
            runCatching {
                dao.storeAnnotations(snapshot)
                val before = requireNotNull(cache)
                cache = Cache(
                    before.notes + listOfNotNull(snapshot.note).associateBy { it.runId },
                    before.reviews + snapshot.reviews.associateBy { it.eventId },
                    before.interruptions + listOfNotNull(snapshot.interruption).associateBy { it.runId },
                )
                true
            }.getOrDefault(false)
        }
    }
}

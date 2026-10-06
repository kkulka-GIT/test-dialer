package com.example.testdialer.persistence

import android.content.Context
import com.example.testdialer.notes.RunNotesStore
import com.example.testdialer.review.BillingReviewStore
import com.example.testdialer.review.BillingVerdict
import org.json.JSONObject

/** Copies validated v1 preferences into Room without modifying the legacy backup. */
class LegacyAnnotationMigrator(context: Context, private val dao: TestRunDao) {
    private val notes = context.getSharedPreferences(RunNotesStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val reviews = context.getSharedPreferences(BillingReviewStore.PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun migrate(): LegacyMigrationResult = dao.importLegacyAnnotations(
        LegacyAnnotationBatch(
            notes = notes.all.mapNotNull { (runId, value) ->
                (value as? String)?.takeIf { it.length <= RunNotesStore.MAX_LENGTH }
                    ?.let { RunNoteEntity(runId, it) }
            },
            reviews = reviews.all.mapNotNull { (key, value) ->
                if (!key.startsWith("event:") || value !is String) return@mapNotNull null
                runCatching {
                    val json = JSONObject(value)
                    val expected = json.optString("expected")
                    val actual = json.optString("actual")
                    val verdict = BillingVerdict.valueOf(json.optString("verdict", "NOT_CHECKED"))
                    val reviewedAt = json.optLong("reviewedAtMillis")
                    require(expected.length <= 2000 && actual.length <= 2000 && reviewedAt > 0)
                    require(verdict == BillingVerdict.NOT_CHECKED || (expected.isNotBlank() && actual.isNotBlank()))
                    BillingReviewEntity(key.removePrefix("event:"), expected, actual, verdict.name, reviewedAt)
                }.getOrNull()
            },
            interruptions = reviews.all.mapNotNull { (key, value) ->
                if (!key.startsWith("interrupted:") || value !is Long || value <= 0) null
                else RunInterruptionEntity(key.removePrefix("interrupted:"), value)
            },
        ),
    )
}

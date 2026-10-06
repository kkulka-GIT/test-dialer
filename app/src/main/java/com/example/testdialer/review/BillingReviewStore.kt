package com.example.testdialer.review

import android.content.Context
import com.example.testdialer.domain.EventId
import com.example.testdialer.domain.RunId
import org.json.JSONObject

enum class BillingVerdict { NOT_CHECKED, PASS, FAIL }
data class BillingReview(val expected: String = "", val actual: String = "", val verdict: BillingVerdict = BillingVerdict.NOT_CHECKED, val reviewedAtMillis: Long = 0)

/** Tester annotations are independent of immutable technical observations. */
class BillingReviewStore(context: Context, private val room: com.example.testdialer.persistence.RoomAnnotationStore? = null) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    fun get(id: EventId): BillingReview {
        if (room?.ready() == true) return room.review(id.value)?.let {
            BillingReview(it.expected, it.actual, BillingVerdict.valueOf(it.verdict), it.reviewedAtMillis)
        } ?: BillingReview()
        return runCatching {
        val data = JSONObject(preferences.getString("event:${id.value}", "{}"))
        BillingReview(data.optString("expected"), data.optString("actual"),
            BillingVerdict.valueOf(data.optString("verdict", "NOT_CHECKED")), data.optLong("reviewedAtMillis"))
    }.getOrDefault(BillingReview())
    }
    fun save(id: EventId, review: BillingReview): Boolean {
        require(review.expected.length <= 2000 && review.actual.length <= 2000)
        require(review.reviewedAtMillis > 0)
        require(review.verdict == BillingVerdict.NOT_CHECKED || (review.expected.isNotBlank() && review.actual.isNotBlank())) {
            "Dla oceny PASS/FAIL opisz oczekiwanie i rzeczywisty wynik rozliczenia."
        }
        if (room != null) return room.save(com.example.testdialer.persistence.AnnotationSnapshot(null,
            listOf(com.example.testdialer.persistence.BillingReviewEntity(id.value, review.expected,
                review.actual, review.verdict.name, review.reviewedAtMillis)), null))
        return preferences.edit().putString("event:${id.value}", JSONObject()
            .put("expected", review.expected).put("actual", review.actual)
            .put("verdict", review.verdict.name).put("reviewedAtMillis", review.reviewedAtMillis).toString()).commit()
    }
    fun interruptedAt(id: RunId): Long = if (room?.ready() == true) room.interruption(id.value)?.interruptedAtMillis ?: 0
        else preferences.getLong("interrupted:${id.value}", 0)
    fun markInterrupted(id: RunId, atMillis: Long): Boolean {
        require(atMillis > 0)
        if (room != null) return room.save(com.example.testdialer.persistence.AnnotationSnapshot(null,
            emptyList(), com.example.testdialer.persistence.RunInterruptionEntity(id.value, atMillis)))
        return preferences.edit().putLong("interrupted:${id.value}", atMillis).commit()
    }
    companion object { const val PREFERENCES_NAME = "billing-reviews-v1" }
}

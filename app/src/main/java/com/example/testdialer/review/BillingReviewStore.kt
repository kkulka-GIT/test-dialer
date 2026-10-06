package com.example.testdialer.review

import android.content.Context
import com.example.testdialer.domain.EventId
import com.example.testdialer.domain.RunId
import org.json.JSONObject

enum class BillingVerdict { NOT_CHECKED, PASS, FAIL }
data class BillingReview(val expected: String = "", val actual: String = "", val verdict: BillingVerdict = BillingVerdict.NOT_CHECKED, val reviewedAtMillis: Long = 0)

/** Tester annotations are independent of immutable technical observations. */
class BillingReviewStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    fun get(id: EventId): BillingReview = runCatching {
        val data = JSONObject(preferences.getString("event:${id.value}", "{}"))
        BillingReview(data.optString("expected"), data.optString("actual"),
            BillingVerdict.valueOf(data.optString("verdict", "NOT_CHECKED")), data.optLong("reviewedAtMillis"))
    }.getOrDefault(BillingReview())
    fun save(id: EventId, review: BillingReview): Boolean {
        require(review.expected.length <= 2000 && review.actual.length <= 2000)
        require(review.reviewedAtMillis > 0)
        require(review.verdict == BillingVerdict.NOT_CHECKED || (review.expected.isNotBlank() && review.actual.isNotBlank())) {
            "Dla oceny PASS/FAIL opisz oczekiwanie i rzeczywisty wynik rozliczenia."
        }
        return preferences.edit().putString("event:${id.value}", JSONObject()
            .put("expected", review.expected).put("actual", review.actual)
            .put("verdict", review.verdict.name).put("reviewedAtMillis", review.reviewedAtMillis).toString()).commit()
    }
    fun interruptedAt(id: RunId): Long = preferences.getLong("interrupted:${id.value}", 0)
    fun markInterrupted(id: RunId, atMillis: Long): Boolean {
        require(atMillis > 0)
        return preferences.edit().putLong("interrupted:${id.value}", atMillis).commit()
    }
    companion object { const val PREFERENCES_NAME = "billing-reviews-v1" }
}

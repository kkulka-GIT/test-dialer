package com.example.testdialer.review

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.testdialer.domain.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BillingReviewStoreTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    @Before fun clear() { context.getSharedPreferences("billing-reviews-v1", Context.MODE_PRIVATE).edit().clear().commit() }
    @Test fun `billing verdict persists independently from service outcome`() {
        val store = BillingReviewStore(context)
        val id = EventId("sms")
        assertEquals(BillingVerdict.NOT_CHECKED, store.get(id).verdict)
        val review = BillingReview("0,79 PLN", "CDR: 1,58 PLN", BillingVerdict.FAIL, 1234)
        assertTrue(store.save(id, review))
        assertEquals(review, BillingReviewStore(context).get(id))
        assertEquals(BillingVerdict.NOT_CHECKED, store.get(EventId("other")).verdict)
    }
    @Test fun `PASS without expected and actual evidence is rejected`() {
        val store = BillingReviewStore(context)
        assertThrows(IllegalArgumentException::class.java) { store.save(EventId("e"), BillingReview("0,79", "", BillingVerdict.PASS, 1)) }
        assertEquals(BillingReview(), store.get(EventId("e")))
    }
    @Test fun `interruption marker preserves review and has no fabricated service outcome`() {
        val store = BillingReviewStore(context)
        assertTrue(store.markInterrupted(RunId("r"), 4567))
        assertEquals(4567L, BillingReviewStore(context).interruptedAt(RunId("r")))
        assertEquals(0L, store.interruptedAt(RunId("other")))
        assertEquals(BillingVerdict.NOT_CHECKED, store.get(EventId("r")).verdict)
    }
}

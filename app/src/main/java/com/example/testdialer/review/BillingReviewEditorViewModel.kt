package com.example.testdialer.review

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.testdialer.domain.EventId
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

data class BillingReviewEditorUiState(
    val eventId: EventId? = null,
    val expected: String = "",
    val actual: String = "",
    val expectedSelection: Int = 0,
    val actualSelection: Int = 0,
    val verdict: BillingVerdict = BillingVerdict.NOT_CHECKED,
    val open: Boolean = false,
    val busy: Boolean = false,
    val error: Boolean = false,
    val saved: Boolean = false,
)

class BillingReviewEditorViewModel(
    private val readReview: (EventId) -> BillingReview,
    private val saveReview: (EventId, BillingReview) -> Boolean,
    private val nowMillis: () -> Long,
    private val executor: ExecutorService,
) : ViewModel() {
    private val mutableState = MutableLiveData(BillingReviewEditorUiState())
    val state: LiveData<BillingReviewEditorUiState> = mutableState

    fun open(eventId: EventId) {
        val current = current()
        if (current.open && current.eventId == eventId) return
        val review = readReview(eventId)
        mutableState.value = BillingReviewEditorUiState(
            eventId = eventId,
            expected = review.expected,
            actual = review.actual,
            expectedSelection = review.expected.length,
            actualSelection = review.actual.length,
            verdict = review.verdict,
            open = true,
        )
    }

    fun updateDraft(
        expected: String,
        actual: String,
        expectedSelection: Int,
        actualSelection: Int,
        verdict: BillingVerdict,
    ) {
        val current = current()
        if (!current.open || current.busy) return
        mutableState.value = current.copy(
            expected = expected,
            actual = actual,
            expectedSelection = expectedSelection.coerceIn(0, expected.length),
            actualSelection = actualSelection.coerceIn(0, actual.length),
            verdict = verdict,
            error = false,
        )
    }

    fun cancel() {
        if (!current().busy) mutableState.value = BillingReviewEditorUiState()
    }

    fun save() {
        val before = current()
        val eventId = before.eventId ?: return
        if (!before.open || before.busy) return
        mutableState.value = before.copy(busy = true, error = false)
        executor.execute {
            val review = BillingReview(before.expected, before.actual, before.verdict, nowMillis())
            val saved = runCatching { saveReview(eventId, review) }.getOrDefault(false)
            mutableState.postValue(
                if (saved) BillingReviewEditorUiState(saved = true)
                else before.copy(busy = false, error = true),
            )
        }
    }

    fun acknowledgeSaved() {
        if (current().saved) mutableState.value = BillingReviewEditorUiState()
    }

    private fun current() = mutableState.value ?: BillingReviewEditorUiState()

    override fun onCleared() {
        executor.shutdown()
    }

    class Factory(private val store: BillingReviewStore) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(BillingReviewEditorViewModel::class.java))
            return BillingReviewEditorViewModel(store::get, store::save, System::currentTimeMillis, Executors.newSingleThreadExecutor()) as T
        }
    }
}

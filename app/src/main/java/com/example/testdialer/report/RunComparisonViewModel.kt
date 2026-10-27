package com.example.testdialer.report

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.testdialer.TestDialerApplication
import com.example.testdialer.domain.RunId
import com.example.testdialer.review.BillingReviewStore
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

sealed interface RunComparisonState {
    data object Idle : RunComparisonState
    data class Loading(val baselineId: RunId, val comparedId: RunId) : RunComparisonState
    data class Ready(
        val comparison: RunComparison,
        val onlyDifferences: Boolean = false,
    ) : RunComparisonState
    data class Sharing(
        val comparison: RunComparison,
        val onlyDifferences: Boolean,
    ) : RunComparisonState
    data class ShareReady(
        val comparison: RunComparison,
        val onlyDifferences: Boolean,
        val intent: Intent,
    ) : RunComparisonState
    data class Failure(
        val comparison: RunComparison? = null,
        val onlyDifferences: Boolean = false,
    ) : RunComparisonState
}

/** Keeps comparison preparation and its result across Activity recreation. */
class RunComparisonViewModel @JvmOverloads constructor(
    application: Application,
    private val executor: ExecutorService = Executors.newSingleThreadExecutor(),
    comparisonLoader: ((RunId, RunId) -> RunComparison)? = null,
    private val shareIntentFactory: (Application, String, String) -> Intent =
        { app, content, extension -> RunReportFiles.shareIntent(app, content, extension) },
) : AndroidViewModel(application) {
    private val app = application as TestDialerApplication
    private val billingReviews = BillingReviewStore(application, app.annotationStore)
    private val loadComparison = comparisonLoader ?: { baselineId: RunId, comparedId: RunId ->
        val baseline = app.testRunRepository.get(baselineId)
            ?: error("Baseline run unavailable")
        val compared = app.testRunRepository.get(comparedId)
            ?: error("Compared run unavailable")
        val reviews = (baseline.run.events + compared.run.events).associate {
            it.id.value to billingReviews.get(it.id)
        }
        RunComparisonFormatter.compare(baseline, compared, reviews)
    }
    private val _state = MutableLiveData<RunComparisonState>(RunComparisonState.Idle)
    val state: LiveData<RunComparisonState> = _state
    private val lock = Any()
    private var running = false

    fun load(baselineId: RunId, comparedId: RunId) {
        synchronized(lock) {
            if (running || _state.value !is RunComparisonState.Idle) return
            running = true
        }
        _state.value = RunComparisonState.Loading(baselineId, comparedId)
        execute {
            runCatching { loadComparison(baselineId, comparedId) }
                .fold(
                    onSuccess = { RunComparisonState.Ready(it) },
                    onFailure = { RunComparisonState.Failure() },
                )
        }
    }

    fun toggleDifferences() {
        val ready = _state.value as? RunComparisonState.Ready ?: return
        _state.value = ready.copy(onlyDifferences = !ready.onlyDifferences)
    }

    fun prepareShare() {
        val ready = _state.value as? RunComparisonState.Ready ?: return
        synchronized(lock) {
            if (running) return
            running = true
        }
        _state.value = RunComparisonState.Sharing(ready.comparison, ready.onlyDifferences)
        execute {
            runCatching {
                shareIntentFactory(
                    getApplication(),
                    ready.comparison.text(ready.onlyDifferences),
                    "txt",
                )
            }.fold(
                onSuccess = {
                    RunComparisonState.ShareReady(
                        ready.comparison,
                        ready.onlyDifferences,
                        it,
                    )
                },
                onFailure = {
                    RunComparisonState.Failure(
                        ready.comparison,
                        ready.onlyDifferences,
                    )
                },
            )
        }
    }

    fun acknowledgeShare() {
        if (_state.value is RunComparisonState.ShareReady) {
            _state.value = RunComparisonState.Idle
        }
    }

    fun acknowledgeFailure() {
        val failure = _state.value as? RunComparisonState.Failure ?: return
        _state.value = failure.comparison?.let {
            RunComparisonState.Ready(it, failure.onlyDifferences)
        } ?: RunComparisonState.Idle
    }

    fun dismiss() {
        if (_state.value is RunComparisonState.Ready) {
            _state.value = RunComparisonState.Idle
        }
    }

    private fun execute(block: () -> RunComparisonState) {
        try {
            executor.execute {
                val result = block()
                synchronized(lock) { running = false }
                _state.postValue(result)
            }
        } catch (_: RejectedExecutionException) {
            synchronized(lock) { running = false }
            _state.value = RunComparisonState.Failure()
        }
    }

    override fun onCleared() {
        executor.shutdownNow()
        super.onCleared()
    }
}

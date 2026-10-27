package com.example.testdialer.report

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.testdialer.TestDialerApplication
import com.example.testdialer.notes.RunNotesStore
import com.example.testdialer.persistence.StoredTestRun
import com.example.testdialer.review.BillingReviewStore
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

enum class ReportExportOption { COPY, SHARE_TEXT, SHARE_JSON, SHARE_CSV }

sealed interface ReportExportState {
    data object Idle : ReportExportState
    data class Working(val option: ReportExportOption) : ReportExportState
    data class Ready(
        val option: ReportExportOption,
        val content: String,
        val shareIntent: Intent?,
    ) : ReportExportState
    data class Failure(val clipboardTooLarge: Boolean = false) : ReportExportState
}

/** Keeps report preparation and its result across Activity recreation. */
class ReportExportViewModel @JvmOverloads constructor(
    application: Application,
    private val executor: ExecutorService = Executors.newSingleThreadExecutor(),
) : AndroidViewModel(application) {
    private val app = application as TestDialerApplication
    private val notes = RunNotesStore(application, app.annotationStore)
    private val reviews = BillingReviewStore(application, app.annotationStore)
    private val _state = MutableLiveData<ReportExportState>(ReportExportState.Idle)
    val state: LiveData<ReportExportState> = _state
    private val lock = Any()
    private var running = false

    fun prepare(stored: StoredTestRun, option: ReportExportOption) {
        synchronized(lock) {
            if (running || _state.value !is ReportExportState.Idle) return
            running = true
        }
        _state.value = ReportExportState.Working(option)
        try {
            executor.execute {
                val result = try {
                    val note = notes.get(stored.run.id)
                    val billing = stored.run.events.associate {
                        it.id.value to reviews.get(it.id)
                    }.filterValues { it.reviewedAtMillis > 0 }
                    val interruptedAt = reviews.interruptedAt(stored.run.id)
                    val content = when (option) {
                        ReportExportOption.SHARE_JSON ->
                            RunReportFormatter.json(stored, note, billing, interruptedAt)
                        ReportExportOption.SHARE_CSV ->
                            RunReportFormatter.csv(stored, note, billing, interruptedAt)
                        ReportExportOption.COPY, ReportExportOption.SHARE_TEXT ->
                            RunReportFormatter.text(stored, note, billing, interruptedAt)
                    }
                    if (option == ReportExportOption.COPY &&
                        content.toByteArray(Charsets.UTF_8).size > CLIPBOARD_LIMIT_BYTES
                    ) {
                        ReportExportState.Failure(clipboardTooLarge = true)
                    } else {
                        val intent = when (option) {
                            ReportExportOption.COPY -> null
                            ReportExportOption.SHARE_TEXT ->
                                RunReportFiles.shareIntent(application, content, "txt")
                            ReportExportOption.SHARE_JSON ->
                                RunReportFiles.shareIntent(application, content, "json")
                            ReportExportOption.SHARE_CSV ->
                                RunReportFiles.shareIntent(application, content, "csv")
                        }
                        ReportExportState.Ready(option, content, intent)
                    }
                } catch (_: Throwable) {
                    ReportExportState.Failure()
                }
                synchronized(lock) { running = false }
                _state.postValue(result)
            }
        } catch (_: RejectedExecutionException) {
            synchronized(lock) { running = false }
            _state.value = ReportExportState.Failure()
        }
    }

    fun acknowledge() {
        if (_state.value is ReportExportState.Ready || _state.value is ReportExportState.Failure) {
            _state.value = ReportExportState.Idle
        }
    }

    override fun onCleared() {
        executor.shutdownNow()
        super.onCleared()
    }

    private companion object {
        const val CLIPBOARD_LIMIT_BYTES = 100_000
    }
}

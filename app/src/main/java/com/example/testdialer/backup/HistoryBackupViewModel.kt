package com.example.testdialer.backup

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.testdialer.TestDialerApplication
import com.example.testdialer.notes.RunNotesStore
import com.example.testdialer.report.HistoryArchive
import com.example.testdialer.report.HistoryArchiveEntry
import com.example.testdialer.review.BillingReviewStore
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

enum class HistoryBackupOperation { EXPORT, READ_IMPORT, RESTORE }

sealed interface HistoryBackupState {
    data object Idle : HistoryBackupState
    data class Working(val operation: HistoryBackupOperation) : HistoryBackupState
    data class Preview(val entries: List<HistoryArchiveEntry>) : HistoryBackupState
    data class Success(val message: String, val refreshRegister: Boolean = false) : HistoryBackupState
    data class Failure(val message: String) : HistoryBackupState
}

/**
 * Keeps full-history backup work and its user-visible result across Activity recreation.
 * Document selection remains an explicit Activity action; no import is started automatically.
 */
class HistoryBackupViewModel @JvmOverloads constructor(
    application: Application,
    private val executor: ExecutorService = Executors.newSingleThreadExecutor(),
) : AndroidViewModel(application) {
    private val _state = MutableLiveData<HistoryBackupState>(HistoryBackupState.Idle)
    val state: LiveData<HistoryBackupState> = _state
    private val lock = Any()
    private var running = false

    private val app get() = getApplication<TestDialerApplication>()

    fun export(uri: Uri) {
        begin(HistoryBackupOperation.EXPORT, "Nie udało się zapisać historii") {
            val repository = app.testRunRepository
            val annotationStore = app.annotationStore
            val notes = RunNotesStore(app, annotationStore)
            val reviews = BillingReviewStore(app, annotationStore)
            val entries = repository.listSummaries().map { summary ->
                val stored = repository.get(summary.runId)
                    ?: error("Sesja " + summary.runId.value + " jest niedostępna.")
                val eventReviews = stored.run.events.associate { it.id to reviews.get(it.id) }
                    .filterValues { it.reviewedAtMillis > 0 }
                HistoryArchiveEntry(
                    stored,
                    notes.get(stored.run.id),
                    eventReviews,
                    reviews.interruptedAt(stored.run.id),
                )
            }
            val archive = HistoryArchive.encode(entries)
            app.contentResolver.openOutputStream(uri, "wt")?.use {
                it.write(archive.toByteArray(Charsets.UTF_8))
            } ?: error("Nie można otworzyć pliku do zapisu.")
            HistoryBackupState.Success("Kopia historii zapisana. Sesje: " + entries.size + ".")
        }
    }

    fun readImport(uri: Uri) {
        begin(HistoryBackupOperation.READ_IMPORT, "Nie udało się odczytać kopii historii") {
            val entries = app.contentResolver.openInputStream(uri)?.use(HistoryArchive::read)
                ?: error("Nie można otworzyć pliku.")
            HistoryBackupState.Preview(entries)
        }
    }

    fun restorePreview() {
        val entries = (_state.value as? HistoryBackupState.Preview)?.entries ?: return
        begin(HistoryBackupOperation.RESTORE, "Nie udało się przywrócić historii") {
            val restored = app.annotationStore.restoreHistory(entries)
            HistoryBackupState.Success(
                "Historia przywrócona. Dodano sesje: " + restored.addedRuns + ". " +
                    "Już istniejące: " + restored.existingRuns + ". " +
                    "Uzupełniono notatki: " + restored.annotations.notes +
                    ", oceny: " + restored.annotations.reviews + ".",
                refreshRegister = true,
            )
        }
    }

    fun discardPreview() {
        if (_state.value is HistoryBackupState.Preview) _state.value = HistoryBackupState.Idle
    }

    fun acknowledge() {
        if (_state.value is HistoryBackupState.Success || _state.value is HistoryBackupState.Failure) {
            _state.value = HistoryBackupState.Idle
        }
    }

    private fun begin(
        operation: HistoryBackupOperation,
        failurePrefix: String,
        work: () -> HistoryBackupState,
    ) {
        synchronized(lock) {
            if (running) return
            running = true
        }
        _state.value = HistoryBackupState.Working(operation)
        try {
            executor.execute {
                val result = try {
                    work()
                } catch (error: Throwable) {
                    val detail = if (error is com.example.testdialer.persistence.SnapshotConflictException) {
                        "Kopia różni się od historii zapisanej w telefonie. Niczego nie zmieniono."
                    } else {
                        failurePrefix + ": " + (error.message ?: "nieznany błąd") + ". Niczego nie zmieniono."
                    }
                    HistoryBackupState.Failure(detail)
                }
                synchronized(lock) { running = false }
                _state.postValue(result)
            }
        } catch (_: RejectedExecutionException) {
            synchronized(lock) { running = false }
            _state.value = HistoryBackupState.Failure(failurePrefix + ": operacja nie mogła zostać uruchomiona.")
        }
    }

    override fun onCleared() {
        executor.shutdownNow()
        super.onCleared()
    }
}

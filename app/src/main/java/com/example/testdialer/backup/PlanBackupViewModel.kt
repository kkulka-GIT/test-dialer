package com.example.testdialer.backup

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.testdialer.active.SavedScenarioPlan
import com.example.testdialer.active.ScenarioPlanArchive
import com.example.testdialer.active.ScenarioPlanStore
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

enum class PlanBackupOperation { EXPORT, READ_IMPORT, IMPORT }

sealed interface PlanBackupState {
    data object Idle : PlanBackupState
    data class Working(val operation: PlanBackupOperation) : PlanBackupState
    data class Preview(
        val items: List<SavedScenarioPlan>,
        val added: Int,
        val skipped: Int,
    ) : PlanBackupState
    data class Success(val message: String) : PlanBackupState
    data class Failure(val message: String) : PlanBackupState
}

/**
 * Keeps plan backup work and its explicit import preview across Activity recreation.
 * The system document picker remains an Activity action and no plan is executed by this class.
 */
class PlanBackupViewModel @JvmOverloads constructor(
    application: Application,
    private val executor: ExecutorService = Executors.newSingleThreadExecutor(),
) : AndroidViewModel(application) {
    private val store = ScenarioPlanStore(application)
    private val _state = MutableLiveData<PlanBackupState>(PlanBackupState.Idle)
    val state: LiveData<PlanBackupState> = _state
    private val lock = Any()
    private var running = false

    fun export(uri: Uri) {
        begin(PlanBackupOperation.EXPORT, "Nie udało się zapisać kopii planów") {
            val items = store.list()
            val archive = ScenarioPlanArchive.encode(items)
            getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use {
                it.write(archive.toByteArray(Charsets.UTF_8))
            } ?: error("Nie można otworzyć pliku do zapisu.")
            PlanBackupState.Success("Kopia planów zapisana. Plany: " + items.size + ".")
        }
    }

    fun readImport(uri: Uri) {
        begin(PlanBackupOperation.READ_IMPORT, "Nie udało się wczytać planów") {
            val items = getApplication<Application>().contentResolver.openInputStream(uri)
                ?.use(ScenarioPlanArchive::read)
                ?: error("Nie można otworzyć pliku.")
            val preview = store.previewImport(items)
            PlanBackupState.Preview(items, preview.added, preview.skipped)
        }
    }

    fun importPreview() {
        val items = (_state.value as? PlanBackupState.Preview)?.items ?: return
        begin(PlanBackupOperation.IMPORT, "Import planów nie powiódł się") {
            val result = store.importItems(items)
            PlanBackupState.Success(
                "Dodano plany: " + result.added + ". Pominięto powtórzenia: " + result.skipped + ".",
            )
        }
    }

    fun discardPreview() {
        if (_state.value is PlanBackupState.Preview) _state.value = PlanBackupState.Idle
    }

    fun acknowledge() {
        if (_state.value is PlanBackupState.Success || _state.value is PlanBackupState.Failure) {
            _state.value = PlanBackupState.Idle
        }
    }

    private fun begin(
        operation: PlanBackupOperation,
        failurePrefix: String,
        work: () -> PlanBackupState,
    ) {
        synchronized(lock) {
            if (running) return
            running = true
        }
        _state.value = PlanBackupState.Working(operation)
        try {
            executor.execute {
                val result = try {
                    work()
                } catch (error: Throwable) {
                    PlanBackupState.Failure(
                        failurePrefix + ": " + (error.message ?: "nieznany błąd") + ".",
                    )
                }
                synchronized(lock) { running = false }
                _state.postValue(result)
            }
        } catch (_: RejectedExecutionException) {
            synchronized(lock) { running = false }
            _state.value = PlanBackupState.Failure(
                failurePrefix + ": operacja nie mogła zostać uruchomiona.",
            )
        }
    }

    override fun onCleared() {
        executor.shutdownNow()
        super.onCleared()
    }
}

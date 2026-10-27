package com.example.testdialer.backup

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.testdialer.templates.TestTemplate
import com.example.testdialer.templates.TestTemplateArchive
import com.example.testdialer.templates.TestTemplateStore
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

enum class TemplateBackupOperation { EXPORT, READ_IMPORT, IMPORT }

sealed interface TemplateBackupState {
    data object Idle : TemplateBackupState
    data class Working(val operation: TemplateBackupOperation) : TemplateBackupState
    data class Preview(
        val items: List<TestTemplate>,
        val added: Int,
        val skipped: Int,
    ) : TemplateBackupState
    data class Success(val message: String) : TemplateBackupState
    data class Failure(val message: String) : TemplateBackupState
}

/**
 * Keeps template backup work and its explicit import preview across Activity recreation.
 * The system document picker remains an Activity action and loading parameters never executes a service.
 */
class TemplateBackupViewModel @JvmOverloads constructor(
    application: Application,
    private val executor: ExecutorService = Executors.newSingleThreadExecutor(),
) : AndroidViewModel(application) {
    private val store = TestTemplateStore(application)
    private val _state = MutableLiveData<TemplateBackupState>(TemplateBackupState.Idle)
    val state: LiveData<TemplateBackupState> = _state
    private val lock = Any()
    private var running = false

    fun export(uri: Uri) {
        begin(TemplateBackupOperation.EXPORT, "Nie udało się zapisać kopii szablonów") {
            val items = store.list()
            val archive = TestTemplateArchive.encode(items)
            getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use {
                it.write(archive.toByteArray(Charsets.UTF_8))
            } ?: error("Nie można otworzyć pliku do zapisu.")
            TemplateBackupState.Success("Kopia szablonów zapisana. Szablony: " + items.size + ".")
        }
    }

    fun readImport(uri: Uri) {
        begin(TemplateBackupOperation.READ_IMPORT, "Nie udało się wczytać szablonów") {
            val items = getApplication<Application>().contentResolver.openInputStream(uri)
                ?.use(TestTemplateArchive::read)
                ?: error("Nie można otworzyć pliku.")
            val preview = store.previewImport(items)
            TemplateBackupState.Preview(items, preview.added, preview.skipped)
        }
    }

    fun importPreview() {
        val items = (_state.value as? TemplateBackupState.Preview)?.items ?: return
        begin(TemplateBackupOperation.IMPORT, "Import szablonów nie powiódł się") {
            val result = store.importItems(items)
            TemplateBackupState.Success(
                "Dodano szablony: " + result.added + ". Pominięto powtórzenia: " + result.skipped + ".",
            )
        }
    }

    fun discardPreview() {
        if (_state.value is TemplateBackupState.Preview) _state.value = TemplateBackupState.Idle
    }

    fun acknowledge() {
        if (_state.value is TemplateBackupState.Success || _state.value is TemplateBackupState.Failure) {
            _state.value = TemplateBackupState.Idle
        }
    }

    private fun begin(
        operation: TemplateBackupOperation,
        failurePrefix: String,
        work: () -> TemplateBackupState,
    ) {
        synchronized(lock) {
            if (running) return
            running = true
        }
        _state.value = TemplateBackupState.Working(operation)
        try {
            executor.execute {
                val result = try {
                    work()
                } catch (error: Throwable) {
                    TemplateBackupState.Failure(
                        failurePrefix + ": " + (error.message ?: "nieznany błąd") + ".",
                    )
                }
                synchronized(lock) { running = false }
                _state.postValue(result)
            }
        } catch (_: RejectedExecutionException) {
            synchronized(lock) { running = false }
            _state.value = TemplateBackupState.Failure(
                failurePrefix + ": operacja nie mogła zostać uruchomiona.",
            )
        }
    }

    override fun onCleared() {
        executor.shutdownNow()
        super.onCleared()
    }
}

package com.example.testdialer.register

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.testdialer.domain.EventId
import com.example.testdialer.domain.RunId
import com.example.testdialer.persistence.StoredTestRun
import com.example.testdialer.persistence.TestRunRepository
import com.example.testdialer.persistence.TestRunSummary
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

data class RegisterUiState(
    val busy: Boolean = false,
    val runs: List<TestRunSummary> = emptyList(),
    val selectedRun: StoredTestRun? = null,
    val selectedEventId: EventId? = null,
    val error: String? = null,
)

/** Read-only navigation state for the unified Room-backed Run/Event register. */
class RegisterViewModel(
    private val repository: TestRunRepository,
    private val executor: ExecutorService,
) : ViewModel() {
    private val mutableState = MutableLiveData(RegisterUiState())
    val state: LiveData<RegisterUiState> = mutableState

    fun load() = submit("Nie udało się wczytać Rejestru") {
        val selectedRunId = selectedRun?.run?.id
        copy(
            runs = repository.listSummaries(),
            selectedRun = selectedRunId?.let { repository.get(it) },
        )
    }

    fun selectRun(runId: RunId) = submit("Nie udało się wczytać szczegółów Runu") {
        copy(selectedRun = repository.get(runId), selectedEventId = null)
    }

    fun selectEvent(eventId: EventId) {
        val current = mutableState.value ?: RegisterUiState()
        if (current.selectedRun?.run?.events?.any { it.id == eventId } == true) {
            mutableState.value = current.copy(selectedEventId = eventId, error = null)
        }
    }

    fun clearEvent() {
        mutableState.value = (mutableState.value ?: RegisterUiState()).copy(
            selectedEventId = null,
            error = null,
        )
    }

    fun clearRun() {
        mutableState.value = (mutableState.value ?: RegisterUiState()).copy(
            selectedRun = null,
            selectedEventId = null,
            error = null,
        )
    }

    private fun submit(prefix: String, operation: RegisterUiState.() -> RegisterUiState) {
        val before = mutableState.value ?: RegisterUiState()
        if (before.busy) return
        mutableState.value = before.copy(busy = true, error = null)
        executor.execute {
            mutableState.postValue(runCatching { before.operation() }.fold(
                onSuccess = { it.copy(busy = false, error = null) },
                onFailure = { failure ->
                    before.copy(
                        busy = false,
                        error = "$prefix: ${failure.message ?: failure.javaClass.simpleName}",
                    )
                },
            ))
        }
    }

    override fun onCleared() {
        executor.shutdown()
    }

    class Factory(private val repository: TestRunRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(RegisterViewModel::class.java))
            return RegisterViewModel(repository, Executors.newSingleThreadExecutor()) as T
        }
    }
}

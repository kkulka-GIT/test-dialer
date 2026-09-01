package com.example.testdialer.session

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.testdialer.domain.RunId
import com.example.testdialer.domain.ServiceType
import com.example.testdialer.domain.TestRunStatus
import com.example.testdialer.persistence.StoredTestRun
import com.example.testdialer.persistence.TestRunRepository
import com.example.testdialer.persistence.TestRunSummary
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

data class ManualSessionUiState(
    val busy: Boolean = false,
    val active: ActiveManualSession? = null,
    val history: List<TestRunSummary> = emptyList(),
    val selected: StoredTestRun? = null,
    val message: String? = null,
    val error: String? = null,
)

class ManualSessionViewModel(
    private val repository: TestRunRepository,
    private val coordinator: ManualBillingSessionCoordinator,
    private val executor: ExecutorService,
) : ViewModel() {
    private val mutableState = MutableLiveData(ManualSessionUiState())
    val state: LiveData<ManualSessionUiState> = mutableState

    fun loadHistory() = submit("Could not load session history") {
        copy(history = repository.listSummaries(), selected = selected)
    }

    fun select(runId: RunId) = submit("Could not load session details") {
        copy(selected = repository.get(runId))
    }

    fun clearSelection() {
        mutableState.value = current().copy(selected = null, message = null, error = null)
    }

    fun start(name: String, type: ServiceType, target: String) =
        submit("Could not start the session") {
            val active = coordinator.start(ManualSessionInput(name, type, target))
            copy(active = active, message = "Session started", history = repository.listSummaries())
        }

    fun recordEvent() = submit("Could not record the event; the in-process session was closed") {
        val active = coordinator.recordEvent()
        copy(active = active, message = "Event timestamp recorded", history = repository.listSummaries())
    }

    fun complete() = submit("Could not complete the session; the in-process session was closed") {
        val completed = coordinator.complete()
        check(completed.stored.run.status == TestRunStatus.COMPLETED)
        copy(active = null, message = "Session completed", history = repository.listSummaries())
    }

    private fun submit(errorPrefix: String, operation: ManualSessionUiState.() -> ManualSessionUiState) {
        val before = current()
        if (before.busy) return
        mutableState.value = before.copy(busy = true, message = null, error = null)
        executor.execute {
            val result = runCatching { before.operation() }
            mutableState.postValue(
                result.fold(
                    onSuccess = { it.copy(busy = false, error = null) },
                    onFailure = { failure ->
                        before.copy(
                            busy = false,
                            active = coordinator.active(),
                            message = null,
                            error = "$errorPrefix: ${failure.message ?: failure.javaClass.simpleName}",
                        )
                    },
                ),
            )
        }
    }

    private fun current(): ManualSessionUiState = mutableState.value ?: ManualSessionUiState()

    override fun onCleared() {
        executor.shutdown()
    }

    class Factory(private val repository: TestRunRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ManualSessionViewModel::class.java))
            return ManualSessionViewModel(
                repository,
                ManualBillingSessionCoordinator(repository),
                Executors.newSingleThreadExecutor(),
            ) as T
        }
    }
}

package com.example.testdialer.sms

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.testdialer.domain.TestRunStatus
import com.example.testdialer.persistence.TestRunRepository
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

data class GuidedSmsUiState(
    val busy: Boolean = false,
    val composerRequested: Boolean = false,
    val awaitingObservation: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
)

class GuidedSmsViewModel(
    private val coordinator: GuidedSmsTestCoordinator,
    private val executor: ExecutorService,
) : ViewModel() {
    private val mutableState = MutableLiveData(GuidedSmsUiState())
    val state: LiveData<GuidedSmsUiState> = mutableState

    fun start(input: GuidedSmsInput) = submit("Nie udało się rozpocząć testu SMS") {
        coordinator.start(input)
        GuidedSmsUiState(composerRequested = true)
    }

    fun composerOpened() {
        mutableState.value = current().copy(composerRequested = false)
    }

    fun returnedFromComposer() {
        mutableState.value = current().copy(composerRequested = false, awaitingObservation = true)
    }

    fun record(outcome: GuidedSmsOutcome) = submit("Nie udało się zapisać wyniku SMS") {
        val stored = coordinator.recordAndComplete(outcome)
        check(stored.run.status == TestRunStatus.COMPLETED)
        GuidedSmsUiState(saved = true)
    }

    fun startAnother() {
        check(!coordinator.hasActiveSession())
        mutableState.value = GuidedSmsUiState()
    }

    private fun submit(errorPrefix: String, operation: () -> GuidedSmsUiState) {
        val before = current()
        if (before.busy) return
        mutableState.value = before.copy(busy = true, error = null)
        executor.execute {
            mutableState.postValue(
                runCatching(operation).fold(
                    onSuccess = { it },
                    onFailure = { failure ->
                        before.copy(
                            busy = false,
                            composerRequested = false,
                            error = "$errorPrefix: ${failure.message ?: failure.javaClass.simpleName}",
                        )
                    },
                ),
            )
        }
    }

    private fun current(): GuidedSmsUiState = mutableState.value ?: GuidedSmsUiState()

    override fun onCleared() {
        executor.shutdown()
    }

    class Factory(private val repository: TestRunRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(GuidedSmsViewModel::class.java))
            return GuidedSmsViewModel(
                GuidedSmsTestCoordinator(repository),
                Executors.newSingleThreadExecutor(),
            ) as T
        }
    }
}

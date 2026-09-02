package com.example.testdialer.sms

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.testdialer.domain.TestRunStatus
import com.example.testdialer.persistence.TestRunRepository
import com.example.testdialer.persistence.StoredTestRun
import com.example.testdialer.domain.execution.SystemTimeProvider
import com.example.testdialer.domain.execution.TimeProvider
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

data class GuidedSmsUiState(
    val busy: Boolean = false,
    val input: GuidedSmsInput? = null,
    val composerRequested: Boolean = false,
    val composerOpen: Boolean = false,
    val awaitingObservation: Boolean = false,
    val saved: Boolean = false,
    val completed: StoredTestRun? = null,
    val error: String? = null,
)

class GuidedSmsViewModel(
    private val coordinator: GuidedSmsTestCoordinator,
    private val executor: ExecutorService,
    private val timeProvider: TimeProvider = SystemTimeProvider,
) : ViewModel() {
    private val mutableState = MutableLiveData(GuidedSmsUiState())
    val state: LiveData<GuidedSmsUiState> = mutableState

    fun start(input: GuidedSmsInput) = submit("Nie udało się rozpocząć testu SMS") {
        coordinator.start(input)
        GuidedSmsUiState(input = input, composerRequested = true)
    }

    fun composerOpened() {
        mutableState.value = current().copy(composerRequested = false, composerOpen = true)
    }

    fun returnedFromComposer() {
        mutableState.value = current().copy(
            composerRequested = false,
            composerOpen = false,
            awaitingObservation = true,
        )
    }

    fun composerLaunchFailed() = returnedFromComposer()

    fun record(outcome: GuidedSmsOutcome) {
        if (current().busy) return
        val observedAt = timeProvider.capture()
        submit("Nie udało się zapisać wyniku SMS") {
            val stored = coordinator.recordAndComplete(outcome, observedAt)
            check(stored.run.status == TestRunStatus.COMPLETED)
            GuidedSmsUiState(saved = true, completed = stored)
        }
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
                        val error = "$errorPrefix: ${failure.message ?: failure.javaClass.simpleName}"
                        if (coordinator.hasActiveSession()) {
                            before.copy(busy = false, composerRequested = false, error = error)
                        } else {
                            GuidedSmsUiState(error = error)
                        }
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
                GuidedSmsTestCoordinator(repository, SystemTimeProvider),
                Executors.newSingleThreadExecutor(),
                SystemTimeProvider,
            ) as T
        }
    }
}

package com.example.testdialer.active

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.testdialer.domain.CorrelationMetadata
import com.example.testdialer.domain.Observation
import com.example.testdialer.domain.ObservationSource
import com.example.testdialer.domain.ObservationStatus
import com.example.testdialer.domain.StepId
import com.example.testdialer.domain.ServiceType
import com.example.testdialer.domain.TestAction
import com.example.testdialer.persistence.StoredTestRun
import com.example.testdialer.persistence.TestRunRepository
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.Collections

data class ActiveRunUiState(
    val busy: Boolean = false,
    val active: ActiveRun? = null,
    val message: String? = null,
    val error: String? = null,
)

class ActiveRunViewModel(
    private val coordinator: ActiveRunCoordinator,
    private val executor: ExecutorService,
) : ViewModel() {
    private val mutableState = MutableLiveData(ActiveRunUiState())
    val state: LiveData<ActiveRunUiState> = mutableState
    private val executions = Collections.synchronizedMap(mutableMapOf<ServiceType, ActiveExecutionContext>())

    fun beginExecution(stepId: StepId?, serviceType: ServiceType) {
        executions[serviceType] = coordinator.beginExecution(stepId, serviceType)
    }

    fun cancelExecution(serviceType: ServiceType) {
        executions.remove(serviceType)?.let(coordinator::cancelExecution)
    }

    fun executionInProgress() = coordinator.executionInProgress()

    fun startEmpty(name: String) = submit("Nie udało się rozpocząć Runu") {
        copy(active = coordinator.startEmpty(name), message = "Run rozpoczęty")
    }

    fun startScenario(scenario: LocalScenario) = submit("Nie udało się rozpocząć Scenario") {
        copy(active = coordinator.startScenario(scenario), message = "Scenario rozpoczęte")
    }

    fun recordVoice(number: String, outcome: com.example.testdialer.VoiceTestResult.Outcome) {
        val context = executions.remove(ServiceType.VOICE) ?: return
        submitCompletion(
            context = context,
            errorPrefix = "Wynik Voice zapisano w legacy, ale nie udało się dołączyć Eventu do aktywnego Runu",
        ) {
            copy(active = coordinator.record(
                context,
                TestAction.Voice(number),
                Observation(
                    status = when (outcome) {
                        com.example.testdialer.VoiceTestResult.Outcome.SUCCESS -> ObservationStatus.CONFIRMED
                        com.example.testdialer.VoiceTestResult.Outcome.FAILURE -> ObservationStatus.NOT_CONFIRMED
                        com.example.testdialer.VoiceTestResult.Outcome.NOT_CHECKED -> ObservationStatus.NOT_VERIFIED
                    },
                    source = ObservationSource.TESTER,
                    code = "USER_REPORTED_VOICE_${outcome.name}",
                ),
            ), message = "Event Voice dodany do aktywnego Runu")
        }
    }

    fun recordExternal(serviceType: ServiceType, source: StoredTestRun) {
        val context = executions.remove(serviceType) ?: return
        submitCompletion(
            context = context,
            errorPrefix = "Test zapisano, ale nie udało się dołączyć Eventu do aktywnego Runu",
        ) {
            copy(active = coordinator.recordExternal(context, source), message = "Event dodany do aktywnego Runu")
        }
    }

    fun skip(stepId: StepId) = submit("Nie udało się pominąć Tasku") {
        copy(active = coordinator.skip(stepId), message = "Task pominięty")
    }

    fun complete() = submit("Nie udało się zakończyć Runu") {
        coordinator.complete()
        copy(active = null, message = "Run zakończony")
    }

    private fun submitCompletion(
        context: ActiveExecutionContext,
        errorPrefix: String,
        operation: ActiveRunUiState.() -> ActiveRunUiState,
    ) {
        try {
            submit(errorPrefix = errorPrefix, queueWhenBusy = true, operation = operation)
        } catch (failure: java.util.concurrent.RejectedExecutionException) {
            coordinator.cancelExecution(context)
            val current = mutableState.value ?: ActiveRunUiState()
            mutableState.value = current.copy(
                busy = false,
                active = coordinator.active(),
                error = "$errorPrefix: executor odrzucił zapis wyniku",
            )
        }
    }

    private fun submit(
        errorPrefix: String,
        queueWhenBusy: Boolean = false,
        operation: ActiveRunUiState.() -> ActiveRunUiState,
    ) {
        val before = mutableState.value ?: ActiveRunUiState()
        if (before.busy && !queueWhenBusy) return
        if (!before.busy) mutableState.value = before.copy(busy = true, message = null, error = null)
        executor.execute {
            mutableState.postValue(runCatching { before.operation() }.fold(
                onSuccess = { it.copy(busy = false, error = null) },
                onFailure = { failure ->
                    before.copy(
                        busy = false,
                        active = coordinator.active(),
                        error = "$errorPrefix: ${failure.message ?: failure.javaClass.simpleName}",
                    )
                },
            ))
        }
    }

    override fun onCleared() { executor.shutdown() }

    class Factory(private val repository: TestRunRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ActiveRunViewModel::class.java))
            return ActiveRunViewModel(ActiveRunCoordinator(repository), Executors.newSingleThreadExecutor()) as T
        }
    }
}

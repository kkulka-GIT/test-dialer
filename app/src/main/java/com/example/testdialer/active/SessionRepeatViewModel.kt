package com.example.testdialer.active

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.testdialer.TestDialerApplication
import com.example.testdialer.domain.RunId
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

sealed interface SessionRepeatState {
    data object Idle : SessionRepeatState
    data class Loading(val sourceId: RunId) : SessionRepeatState
    data class Ready(val plan: LocalScenario) : SessionRepeatState
    data class Failure(val message: String) : SessionRepeatState
}

/** Keeps repeat-plan preparation and its explicit preview across Activity recreation. */
class SessionRepeatViewModel @JvmOverloads constructor(
    application: Application,
    private val executor: ExecutorService = Executors.newSingleThreadExecutor(),
    planLoader: ((RunId) -> LocalScenario)? = null,
) : AndroidViewModel(application) {
    private val loadPlan = planLoader ?: { sourceId: RunId ->
        val app = application as TestDialerApplication
        val stored = app.testRunRepository.get(sourceId)
            ?: error("Sesja jest niedostępna.")
        SessionRepeatPlan.from(stored)
    }
    private val _state = MutableLiveData<SessionRepeatState>(SessionRepeatState.Idle)
    val state: LiveData<SessionRepeatState> = _state
    private val lock = Any()
    private var running = false

    fun load(sourceId: RunId) {
        synchronized(lock) {
            if (running || _state.value !is SessionRepeatState.Idle) return
            running = true
        }
        _state.value = SessionRepeatState.Loading(sourceId)
        execute {
            runCatching { loadPlan(sourceId) }.fold(
                onSuccess = { SessionRepeatState.Ready(it) },
                onFailure = {
                    SessionRepeatState.Failure(
                        it.message ?: "Nie udało się przygotować powtórki",
                    )
                },
            )
        }
    }

    fun dismiss() {
        if (_state.value is SessionRepeatState.Ready) {
            _state.value = SessionRepeatState.Idle
        }
    }

    fun acknowledgeFailure() {
        if (_state.value is SessionRepeatState.Failure) {
            _state.value = SessionRepeatState.Idle
        }
    }

    private fun execute(block: () -> SessionRepeatState) {
        try {
            executor.execute {
                val result = block()
                synchronized(lock) { running = false }
                _state.postValue(result)
            }
        } catch (_: RejectedExecutionException) {
            synchronized(lock) { running = false }
            _state.value = SessionRepeatState.Failure(
                "Nie udało się przygotować powtórki",
            )
        }
    }

    override fun onCleared() {
        executor.shutdownNow()
        super.onCleared()
    }
}

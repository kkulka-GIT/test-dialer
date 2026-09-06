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

    private sealed interface Request {
        data object Refresh : Request
        data class SelectRun(val runId: RunId) : Request
    }

    private val lock = Any()
    private var currentState = RegisterUiState()
    private val pendingRequests = ArrayDeque<Request>()
    private var requestInFlight = false
    private var navigationRevision = 0L

    /**
     * Refresh requests are coalesced while a read is running. The request is
     * still queued, rather than discarded, so a burst of repository updates
     * cannot leave the register stale.
     */
    fun load() = enqueue(Request.Refresh)

    fun selectRun(runId: RunId) = enqueue(Request.SelectRun(runId))

    fun selectEvent(eventId: EventId) {
        synchronized(lock) {
            if (currentState.selectedRun?.run?.events?.any { it.id == eventId } != true) return
            navigationRevision += 1
            publishLocked(currentState.copy(selectedEventId = eventId, error = null))
        }
    }

    fun clearEvent() {
        synchronized(lock) {
            navigationRevision += 1
            publishLocked(currentState.copy(selectedEventId = null, error = null))
        }
    }

    fun clearRun() {
        synchronized(lock) {
            navigationRevision += 1
            pendingRequests.removeAll { it is Request.SelectRun }
            publishLocked(currentState.copy(
                selectedRun = null,
                selectedEventId = null,
                error = null,
            ))
        }
    }

    private fun enqueue(request: Request) {
        val shouldStart = synchronized(lock) {
            if (request is Request.Refresh && pendingRequests.any { it is Request.Refresh }) {
                // Keep one refresh after the current request, even when the
                // current request is already in flight. Further refreshes in
                // the same burst are coalesced into that pending request.
                false
            } else {
                if (request is Request.SelectRun) navigationRevision += 1
                pendingRequests.addLast(request)
                !requestInFlight
            }
        }
        if (shouldStart) startNext()
    }

    private fun startNext() {
        val work = synchronized(lock) {
            if (requestInFlight || pendingRequests.isEmpty()) return
            requestInFlight = true
            val request = pendingRequests.removeFirst()
            val before = currentState
            val revision = navigationRevision
            publishLocked(before.copy(busy = true, error = null))
            Triple(request, before, revision)
        }
        executor.execute {
            val result = runCatching {
                when (val request = work.first) {
                    Request.Refresh -> {
                        val selectedRunId = work.second.selectedRun?.run?.id
                        work.second.copy(
                            runs = repository.listSummaries(),
                            selectedRun = selectedRunId?.let(repository::get),
                        )
                    }
                    is Request.SelectRun -> work.second.copy(
                        selectedRun = repository.get(request.runId),
                        selectedEventId = null,
                    )
                }
            }
            val startFollowing = synchronized(lock) {
                val stale = work.third != navigationRevision
                val completed = if (stale) {
                    // Navigation state is newer than this read. Keep the
                    // refreshed list, but never replace the newer selection.
                    if (work.first is Request.Refresh) {
                        result.getOrNull()?.let { refreshed ->
                            currentState.copy(runs = refreshed.runs, busy = false)
                        } ?: currentState.copy(busy = false)
                    } else {
                        currentState.copy(busy = false)
                    }
                } else {
                    result.fold(
                        onSuccess = { it.copy(busy = false, error = null) },
                        onFailure = { failure ->
                            currentState.copy(
                                busy = false,
                                error = errorMessage(work.first, failure),
                            )
                        },
                    )
                }
                publishLocked(completed)
                requestInFlight = false
                pendingRequests.isNotEmpty()
            }
            if (startFollowing) startNext()
        }
    }

    private fun errorMessage(request: Request, failure: Throwable): String {
        val prefix = when (request) {
            Request.Refresh -> "Nie udało się wczytać Rejestru"
            is Request.SelectRun -> "Nie udało się wczytać szczegółów Runu"
        }
        return "$prefix: ${failure.message ?: failure.javaClass.simpleName}"
    }

    private fun publishLocked(next: RegisterUiState) {
        currentState = next
        mutableState.postValue(next)
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

package com.example.testdialer.data

import android.net.ConnectivityManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.testdialer.domain.execution.SystemTimeProvider
import com.example.testdialer.domain.execution.TimeProvider
import com.example.testdialer.persistence.TestRunRepository
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

data class CellularDataUiState(
    val busy: Boolean = false,
    val saved: Boolean = false,
    val cancelled: Boolean = false,
    val error: String? = null,
)

class CellularDataViewModel(
    private val coordinator: CellularDataTestCoordinator,
    private val executor: ExecutorService,
    private val timeProvider: TimeProvider = SystemTimeProvider,
) : ViewModel() {
    private val mutableState = MutableLiveData(CellularDataUiState())
    val state: LiveData<CellularDataUiState> = mutableState
    @Volatile private var currentCancellation: DownloadCancellation? = null
    @Volatile private var currentFuture: Future<*>? = null

    fun start(input: CellularDataInput) {
        if (mutableState.value?.busy == true) return
        val requestedAt = timeProvider.capture()
        val cancellation = DownloadCancellation()
        currentCancellation = cancellation
        mutableState.value = CellularDataUiState(busy = true)
        currentFuture = executor.submit {
            mutableState.postValue(
                runCatching { coordinator.run(input, requestedAt, cancellation) }.fold(
                    onSuccess = { stored ->
                        CellularDataUiState(
                            saved = true,
                            cancelled = stored.run.status.name == "ABORTED",
                        )
                    },
                    onFailure = { CellularDataUiState(error = it.message ?: "Nie udało się wykonać testu Data") },
                ),
            )
            currentCancellation = null
            currentFuture = null
        }
    }

    fun cancel() {
        if (mutableState.value?.busy == true) {
            currentCancellation?.cancel()
        }
    }

    fun startAnother() {
        check(mutableState.value?.busy != true)
        mutableState.value = CellularDataUiState()
    }

    override fun onCleared() {
        currentCancellation?.cancel()
        currentFuture?.cancel(true)
        executor.shutdownNow()
    }

    class Factory(
        private val repository: TestRunRepository,
        private val connectivityManager: ConnectivityManager,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(CellularDataViewModel::class.java))
            val gateway = AndroidCellularDownloadGateway(connectivityManager)
            return CellularDataViewModel(
                CellularDataTestCoordinator(repository, gateway),
                Executors.newSingleThreadExecutor(),
            ) as T
        }
    }
}

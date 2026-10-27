package com.example.testdialer.templates

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.testdialer.domain.EventId
import com.example.testdialer.domain.TestAction
import com.example.testdialer.domain.TestEvent
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

data class TemplateSaveUiState(
    val eventId: EventId? = null,
    val action: TestAction? = null,
    val requestedBytes: Long? = null,
    val name: String = "",
    val selection: Int = 0,
    val open: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
)

class TemplateSaveViewModel(
    private val saveTemplate: (String, TestAction, Long?) -> Boolean,
    private val executor: ExecutorService,
) : ViewModel() {
    private val mutableState = MutableLiveData(TemplateSaveUiState())
    val state: LiveData<TemplateSaveUiState> = mutableState

    fun open(event: TestEvent, defaultName: String) {
        val current = current()
        if (current.open && current.eventId == event.id) return
        val name = defaultName.take(80)
        val requestedBytes = event.correlation.references
            .firstOrNull { it.namespace == "requestedBytes" }
            ?.value
            ?.toLongOrNull()
        mutableState.value = TemplateSaveUiState(
            eventId = event.id,
            action = event.action,
            requestedBytes = requestedBytes,
            name = name,
            selection = name.length,
            open = true,
        )
    }

    fun updateDraft(name: String, selection: Int) {
        val current = current()
        if (!current.open || current.busy) return
        val bounded = name.take(80)
        mutableState.value = current.copy(
            name = bounded,
            selection = selection.coerceIn(0, bounded.length),
            error = null,
        )
    }

    fun cancel() {
        if (current().busy) return
        mutableState.value = TemplateSaveUiState()
    }

    fun save() {
        val before = current()
        val action = before.action ?: return
        if (!before.open || before.busy) return
        val name = before.name.trim()
        if (name.isBlank()) {
            mutableState.value = before.copy(error = "Podaj nazwę")
            return
        }
        val saving = before.copy(
            name = name,
            selection = before.selection.coerceIn(0, name.length),
            busy = true,
            error = null,
        )
        mutableState.value = saving
        executor.execute {
            val result = runCatching {
                check(saveTemplate(name, action, before.requestedBytes)) {
                    "Nie udało się zapisać szablonu"
                }
            }
            mutableState.postValue(
                if (result.isSuccess) TemplateSaveUiState(saved = true)
                else saving.copy(
                    busy = false,
                    error = result.exceptionOrNull()?.message
                        ?: "Nie udało się zapisać szablonu",
                ),
            )
        }
    }

    fun acknowledgeSaved() {
        if (current().saved) mutableState.value = TemplateSaveUiState()
    }

    private fun current(): TemplateSaveUiState =
        mutableState.value ?: TemplateSaveUiState()

    override fun onCleared() {
        executor.shutdown()
    }

    class Factory(private val store: TestTemplateStore) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(TemplateSaveViewModel::class.java))
            return TemplateSaveViewModel(store::save, Executors.newSingleThreadExecutor()) as T
        }
    }
}

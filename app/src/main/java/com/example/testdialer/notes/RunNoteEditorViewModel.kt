package com.example.testdialer.notes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.testdialer.domain.RunId
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

data class RunNoteEditorUiState(
    val runId: RunId? = null,
    val text: String = "",
    val selection: Int = 0,
    val open: Boolean = false,
    val busy: Boolean = false,
    val error: Boolean = false,
    val saved: Boolean = false,
)

class RunNoteEditorViewModel(
    private val readNote: (RunId) -> String,
    private val saveNote: (RunId, String) -> Boolean,
    private val executor: ExecutorService,
) : ViewModel() {
    private val mutableState = MutableLiveData(RunNoteEditorUiState())
    val state: LiveData<RunNoteEditorUiState> = mutableState

    fun open(runId: RunId) {
        val current = current()
        if (current.open && current.runId == runId) return
        val text = readNote(runId)
        mutableState.value = RunNoteEditorUiState(
            runId = runId,
            text = text,
            selection = text.length,
            open = true,
        )
    }

    fun updateDraft(text: String, selection: Int) {
        val current = current()
        if (!current.open || current.busy) return
        mutableState.value = current.copy(
            text = text,
            selection = selection.coerceIn(0, text.length),
            error = false,
        )
    }

    fun cancel() {
        if (current().busy) return
        mutableState.value = RunNoteEditorUiState()
    }

    fun save() {
        val before = current()
        val runId = before.runId ?: return
        if (!before.open || before.busy) return
        mutableState.value = before.copy(busy = true, error = false)
        executor.execute {
            val saved = runCatching { saveNote(runId, before.text) }.getOrDefault(false)
            mutableState.postValue(
                if (saved) RunNoteEditorUiState(saved = true)
                else before.copy(busy = false, error = true),
            )
        }
    }

    fun acknowledgeSaved() {
        if (current().saved) mutableState.value = RunNoteEditorUiState()
    }

    private fun current(): RunNoteEditorUiState = mutableState.value ?: RunNoteEditorUiState()

    override fun onCleared() {
        executor.shutdown()
    }

    class Factory(private val store: RunNotesStore) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(RunNoteEditorViewModel::class.java))
            return RunNoteEditorViewModel(store::get, store::save, Executors.newSingleThreadExecutor()) as T
        }
    }
}

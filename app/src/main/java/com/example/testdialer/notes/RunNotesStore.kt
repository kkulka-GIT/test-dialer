package com.example.testdialer.notes

import android.content.Context
import com.example.testdialer.domain.RunId

/** Editable annotations are separate from immutable Room event history. */
class RunNotesStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    fun get(runId: RunId): String = preferences.getString(runId.value, "").orEmpty()
    fun save(runId: RunId, text: String): Boolean {
        require(text.length <= MAX_LENGTH)
        return preferences.edit().putString(runId.value, text).commit()
    }
    companion object {
        const val PREFERENCES_NAME = "run-notes-v1"
        const val MAX_LENGTH = 4000
    }
}

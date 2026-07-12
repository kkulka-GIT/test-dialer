package com.example.testdialer

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class VoiceResultStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun save(result: VoiceTestResult) {
        val records = readRawRecords().toMutableList()
        records.add(toJson(result))
        preferences.edit().putString(KEY_RESULTS, JSONArray(records).toString()).apply()
    }

    fun loadAll(): List<VoiceTestResult> {
        return readRawRecords()
            .mapNotNull(::fromJson)
            .sortedByDescending(VoiceTestResult::timestampMillis)
    }

    private fun readRawRecords(): List<JSONObject> {
        val stored = preferences.getString(KEY_RESULTS, null) ?: return emptyList()
        val array = runCatching { JSONArray(stored) }.getOrNull() ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                array.optJSONObject(index)?.let(::add)
            }
        }
    }

    private fun toJson(result: VoiceTestResult): JSONObject {
        return JSONObject().apply {
            put(FIELD_ID, result.id)
            put(FIELD_OUTCOME, result.outcome.name)
            put(FIELD_TIMESTAMP, result.timestampMillis)
            put(FIELD_PHONE_NUMBER, result.phoneNumber)
            result.testName?.let { put(FIELD_TEST_NAME, it) }
        }
    }

    private fun fromJson(json: JSONObject): VoiceTestResult? {
        return runCatching {
            val id = json.getString(FIELD_ID).takeIf(String::isNotBlank) ?: return null
            val outcome = VoiceTestResult.Outcome.valueOf(json.getString(FIELD_OUTCOME))
            val timestamp = json.getLong(FIELD_TIMESTAMP).takeIf { it > 0L } ?: return null
            val phoneNumber = json.getString(FIELD_PHONE_NUMBER).takeIf(String::isNotBlank) ?: return null
            val testName = json.optString(FIELD_TEST_NAME).trim().takeIf(String::isNotEmpty)
            VoiceTestResult(id, outcome, timestamp, phoneNumber, testName)
        }.getOrNull()
    }

    private companion object {
        const val PREFERENCES_NAME = "voice_results"
        const val KEY_RESULTS = "results"
        const val FIELD_ID = "id"
        const val FIELD_OUTCOME = "outcome"
        const val FIELD_TIMESTAMP = "timestampMillis"
        const val FIELD_PHONE_NUMBER = "phoneNumber"
        const val FIELD_TEST_NAME = "testName"
    }
}

package com.example.testdialer

data class VoiceTestResult(
    val id: String,
    val outcome: Outcome,
    val timestampMillis: Long,
    val phoneNumber: String,
    val testName: String?,
) {
    enum class Outcome {
        SUCCESS,
        FAILURE,
        NOT_CHECKED,
    }
}

package com.example.testdialer.domain

enum class ObservationStatus {
    CONFIRMED,
    NOT_CONFIRMED,
    NOT_VERIFIED,
}

enum class ObservationSource {
    TESTER,
    APPLICATION,
    ANDROID,
    EXTERNAL_SYSTEM,
}

data class Observation(
    val status: ObservationStatus,
    val source: ObservationSource,
    val code: String,
    val description: String? = null,
) {
    init { require(code.isNotBlank()) { "Observation code must not be blank" } }
}

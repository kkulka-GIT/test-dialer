package com.example.testdialer.domain

@JvmInline
value class ScenarioId(val value: String) {
    init { require(value.isNotBlank()) { "ScenarioId must not be blank" } }
}

@JvmInline
value class StepId(val value: String) {
    init { require(value.isNotBlank()) { "StepId must not be blank" } }
}

@JvmInline
value class RunId(val value: String) {
    init { require(value.isNotBlank()) { "RunId must not be blank" } }
}

@JvmInline
value class EventId(val value: String) {
    init { require(value.isNotBlank()) { "EventId must not be blank" } }
}

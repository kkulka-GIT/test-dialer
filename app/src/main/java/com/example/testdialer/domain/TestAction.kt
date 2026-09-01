package com.example.testdialer.domain

sealed interface TestAction {
    val serviceType: ServiceType

    data class Voice(
        val destination: String,
    ) : TestAction {
        init { require(destination.isNotBlank()) { "Voice destination must not be blank" } }
        override val serviceType: ServiceType = ServiceType.VOICE
    }

    data class Sms(
        val destination: String,
        val message: String? = null,
    ) : TestAction {
        init { require(destination.isNotBlank()) { "SMS destination must not be blank" } }
        override val serviceType: ServiceType = ServiceType.SMS
    }

    data class Data(
        val target: String,
    ) : TestAction {
        init { require(target.isNotBlank()) { "Data target must not be blank" } }
        override val serviceType: ServiceType = ServiceType.DATA
    }
}

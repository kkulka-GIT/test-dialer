package com.example.testdialer.domain

data class CorrelationReference(
    val namespace: String,
    val value: String,
) {
    init {
        require(namespace.isNotBlank()) { "Correlation namespace must not be blank" }
        require(value.isNotBlank()) { "Correlation value must not be blank" }
    }
}

data class CorrelationMetadata(
    val sourceAddress: String? = null,
    val destinationAddress: String? = null,
    val subscriberAlias: String? = null,
    val references: List<CorrelationReference> = emptyList(),
) {
    init {
        require(sourceAddress == null || sourceAddress.isNotBlank()) {
            "Source address must be null or non-blank"
        }
        require(destinationAddress == null || destinationAddress.isNotBlank()) {
            "Destination address must be null or non-blank"
        }
        require(subscriberAlias == null || subscriberAlias.isNotBlank()) {
            "Subscriber alias must be null or non-blank"
        }
        require(references.distinct().size == references.size) {
            "Correlation references must not contain duplicates"
        }
    }
}

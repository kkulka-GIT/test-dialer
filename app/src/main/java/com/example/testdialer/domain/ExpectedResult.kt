package com.example.testdialer.domain

data class ExpectedResult(
    val code: String,
    val description: String,
) {
    init {
        require(code.isNotBlank()) { "Expected result code must not be blank" }
        require(description.isNotBlank()) { "Expected result description must not be blank" }
    }
}

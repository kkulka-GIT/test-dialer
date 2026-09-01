package com.example.testdialer.domain.execution

data class CapturedTime(
    val epochMillis: Long,
    val monotonicNanos: Long,
) {
    init {
        require(epochMillis > 0L) { "Epoch time must be positive" }
        require(monotonicNanos >= 0L) { "Monotonic time must be non-negative" }
    }
}

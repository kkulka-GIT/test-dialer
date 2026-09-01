package com.example.testdialer.domain.execution

data class CorrelationTimeWindow(
    val fromEpochMillis: Long,
    val toEpochMillis: Long,
) {
    init {
        require(fromEpochMillis <= toEpochMillis) {
            "Correlation window start must not follow its end"
        }
    }
}

object CorrelationTimeWindowCalculator {
    fun around(
        epochMillis: Long,
        beforeMillis: Long,
        afterMillis: Long,
    ): CorrelationTimeWindow {
        require(epochMillis > 0L) { "Correlation center time must be positive" }
        require(beforeMillis >= 0L) { "Correlation margin before must be non-negative" }
        require(afterMillis >= 0L) { "Correlation margin after must be non-negative" }

        return CorrelationTimeWindow(
            fromEpochMillis = saturatedSubtract(epochMillis, beforeMillis),
            toEpochMillis = saturatedAdd(epochMillis, afterMillis),
        )
    }

    private fun saturatedSubtract(value: Long, amount: Long): Long =
        try {
            Math.subtractExact(value, amount)
        } catch (_: ArithmeticException) {
            Long.MIN_VALUE
        }

    private fun saturatedAdd(value: Long, amount: Long): Long =
        try {
            Math.addExact(value, amount)
        } catch (_: ArithmeticException) {
            Long.MAX_VALUE
        }
}

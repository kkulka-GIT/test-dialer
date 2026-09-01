package com.example.testdialer.domain.execution

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CorrelationTimeWindowTest {
    @Test
    fun createsExplicitWindowAroundEventTime() {
        val window = CorrelationTimeWindowCalculator.around(
            epochMillis = 10_000L,
            beforeMillis = 2_000L,
            afterMillis = 5_000L,
        )

        assertEquals(8_000L, window.fromEpochMillis)
        assertEquals(15_000L, window.toEpochMillis)
    }

    @Test
    fun saturatesAtLongBoundaries() {
        val lower = CorrelationTimeWindowCalculator.around(
            epochMillis = 1L,
            beforeMillis = Long.MAX_VALUE,
            afterMillis = 0L,
        )
        val upper = CorrelationTimeWindowCalculator.around(
            epochMillis = Long.MAX_VALUE,
            beforeMillis = 0L,
            afterMillis = 1L,
        )

        assertEquals(1L - Long.MAX_VALUE, lower.fromEpochMillis)
        assertEquals(Long.MAX_VALUE, upper.toEpochMillis)
    }

    @Test
    fun rejectsNegativeMargins() {
        assertThrows(IllegalArgumentException::class.java) {
            CorrelationTimeWindowCalculator.around(
                epochMillis = 10_000L,
                beforeMillis = -1L,
                afterMillis = 0L,
            )
        }
    }
}

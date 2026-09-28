package com.example.testdialer.data

import org.junit.Assert.*
import org.junit.Test

class DataVolumeTest {
    @Test fun `decimal units convert exactly including Polish fractions`() {
        assertEquals(1L, DataVolume.parse("1", "B"))
        assertEquals(1000L, DataVolume.parse("1", "kB"))
        assertEquals(500_000_000L, DataVolume.parse("500", "MB"))
        assertEquals(1_000_000_000L, DataVolume.parse("1", "GB"))
        assertEquals(1_500_000L, DataVolume.parse(" 1,5 ", "MB"))
        assertEquals(1L, DataVolume.parse("0.000001", "MB"))
    }
    @Test fun `invalid zero fractional byte and excessive volume are rejected`() {
        listOf("", "abc", "0", "-1", "0.5", "1000000001", "999999999999999999999999999").forEach {
            assertThrows(IllegalArgumentException::class.java) { DataVolume.parse(it, "B") }
        }
    }
}

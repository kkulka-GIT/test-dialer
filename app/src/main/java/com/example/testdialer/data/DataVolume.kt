package com.example.testdialer.data

import java.math.BigDecimal

object DataVolume {
    const val MAX_BYTES = 1_000_000_000L
    const val DEFAULT_URL = "https://fsn1-speed.hetzner.com/1GB.bin"
    val units = listOf("B", "kB", "MB", "GB")
    fun parse(value: String, unit: String): Long {
        val multiplier = when (unit) { "B" -> 1L; "kB" -> 1_000L; "MB" -> 1_000_000L; "GB" -> 1_000_000_000L; else -> error("Nieznana jednostka") }
        val bytes = try {
            BigDecimal(value.trim().replace(',', '.')).multiply(BigDecimal.valueOf(multiplier)).longValueExact()
        } catch (_: Exception) { throw IllegalArgumentException("Wpisz ilość odpowiadającą pełnej liczbie bajtów") }
        require(bytes in 1..MAX_BYTES) { "Wpisz od 1 B do 1 GB (1 000 000 000 B)" }
        return bytes
    }
}

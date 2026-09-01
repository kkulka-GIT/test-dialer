package com.example.testdialer.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SafeDownloadUrlValidatorTest {
    @Test fun `accepts public https on default port`() {
        assertEquals("example.com", SafeDownloadUrlValidator.requireValid("https://example.com/file").host)
    }

    @Test fun `accepts explicit port 443`() {
        SafeDownloadUrlValidator.requireValid("https://example.com:443/file")
    }

    @Test fun `rejects unsafe URL forms`() {
        listOf(
            "http://example.com/file",
            "https://user@example.com/file",
            "https://example.com/file?q=1",
            "https://example.com/file#part",
            "https://example.com:8443/file",
            "https://localhost/file",
            "https://service.local/file",
            "https://127.0.0.1/file",
            "https://10.0.0.1/file",
            "https://[::1]/file",
        ).forEach { raw ->
            runCatching { SafeDownloadUrlValidator.requireValid(raw) }
                .onSuccess { throw AssertionError("Expected rejection: $raw") }
        }
    }
}

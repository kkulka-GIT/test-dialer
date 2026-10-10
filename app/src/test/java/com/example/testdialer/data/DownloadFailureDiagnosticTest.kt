package com.example.testdialer.data

import android.system.ErrnoException
import android.system.OsConstants
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DownloadFailureDiagnosticTest {
    @Test fun `wrapped exceptions preserve useful classification`() {
        val cases = listOf(
            UnknownHostException() to DownloadResultCode.DNS_FAILURE,
            SocketTimeoutException() to DownloadResultCode.TIMEOUT,
            ConnectException() to DownloadResultCode.CONNECTION_FAILURE,
            SocketException() to DownloadResultCode.CONNECTION_FAILURE,
            SecurityException() to DownloadResultCode.SECURITY_REJECTED,
        )
        cases.forEach { (cause, code) ->
            assertEquals(code, classifyDownloadFailure(IOException("private URL", cause)).code)
        }
    }

    @Test fun `TLS wrapper retains TLS context and safe socket errno`() {
        val socket = SocketException("secret address").apply {
            initCause(ErrnoException("private function", OsConstants.EACCES))
        }
        val tls = SSLHandshakeException("private certificate").apply { initCause(socket) }
        val diagnostic = classifyDownloadFailure(IOException("private URL", tls))
        assertEquals(DownloadResultCode.TLS_FAILURE, diagnostic.code)
        assertEquals(DownloadFailureCause.TLS, diagnostic.cause)
        assertEquals("EACCES", diagnostic.errno)
        assertFalse(diagnostic.toString().contains("private"))
        assertFalse(diagnostic.toString().contains("secret"))
    }

    @Test fun `errno is allowlisted and does not imply VPN cause`() {
        for ((number, name) in listOf(OsConstants.EPERM to "EPERM", OsConstants.ENETUNREACH to "ENETUNREACH", OsConstants.ENONET to "ENONET", OsConstants.ENODEV to "ENODEV", OsConstants.EADDRNOTAVAIL to "EADDRNOTAVAIL", 99999 to "OTHER")) {
            val result = classifyDownloadFailure(IOException("secret", ErrnoException("secret", number)))
            assertEquals(DownloadResultCode.CONNECTION_FAILURE, result.code)
            assertEquals(name, result.errno)
        }
    }

    @Test fun `cyclic causes terminate with generic IO`() {
        val first = IOException("private")
        val second = IOException("private", first)
        first.initCause(second)
        assertEquals(DownloadFailureCause.IO, classifyDownloadFailure(first).cause)
    }

    @Test fun `cause traversal is bounded`() {
        var error: Throwable = UnknownHostException("secret")
        repeat(40) { error = IOException("secret", error) }
        assertEquals(DownloadFailureCause.IO, classifyDownloadFailure(error).cause)
        assertNull(classifyDownloadFailure(error).errno)
    }
}

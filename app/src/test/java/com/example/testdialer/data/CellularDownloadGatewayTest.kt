package com.example.testdialer.data

import android.content.Context
import android.net.ConnectivityManager
import com.example.testdialer.domain.execution.CapturedTime
import com.example.testdialer.domain.execution.TimeProvider
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.InetAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CellularDownloadGatewayTest {
    @Test fun `cancel before execute does not resolve or open connection`() {
        var resolved = false
        var opened = false
        val gateway = gateway(
            resolver = HostResolver { _, _ -> resolved = true; publicAddress() },
            factory = DownloadConnectionFactory { _, _ -> opened = true; FakeConnection(byteArrayOf()) },
        )
        val cancellation = DownloadCancellation().also { it.cancel() }

        val result = gateway.execute(prepared(), cancellation)

        assertEquals(DownloadResultCode.CANCELLED, result.resultCode)
        assertEquals(null, result.networkStartedAt)
        assertFalse(resolved)
        assertFalse(opened)
    }

    @Test fun `exactly one MiB succeeds after EOF probe`() {
        val result = gateway(body = ByteArray(AndroidCellularDownloadGateway.MAX_BYTES.toInt()))
            .execute(prepared(), DownloadCancellation())
        assertEquals(DownloadResultCode.COMPLETED, result.resultCode)
        assertEquals(AndroidCellularDownloadGateway.MAX_BYTES, result.bytes)
    }

    @Test fun `one MiB plus one byte fails at bounded probe`() {
        val result = gateway(body = ByteArray(AndroidCellularDownloadGateway.MAX_BYTES.toInt() + 1))
            .execute(prepared(), DownloadCancellation())
        assertEquals(DownloadResultCode.LIMIT_EXCEEDED, result.resultCode)
        assertEquals(AndroidCellularDownloadGateway.MAX_BYTES, result.bytes)
    }

    @Test fun `short EOF succeeds with actual bytes`() {
        val result = gateway(body = ByteArray(37)).execute(prepared(), DownloadCancellation())
        assertEquals(DownloadResultCode.COMPLETED, result.resultCode)
        assertEquals(37, result.bytes)
    }

    @Test fun `known oversized response does not open body`() {
        var bodyRead = false
        val connection = FakeConnection(
            body = byteArrayOf(),
            declaredLength = AndroidCellularDownloadGateway.MAX_BYTES + 1,
            onBody = { bodyRead = true },
        )
        val result = gateway(factory = DownloadConnectionFactory { _, _ -> connection })
            .execute(prepared(), DownloadCancellation())
        assertEquals(DownloadResultCode.LIMIT_EXCEEDED, result.resultCode)
        assertFalse(bodyRead)
        assertTrue(connection.disconnected)
    }

    @Test fun `unsafe DNS answer prevents connection open`() {
        var opened = false
        val gateway = gateway(
            resolver = HostResolver { _, _ -> listOf(InetAddress.getByName("100.64.0.1")) },
            factory = DownloadConnectionFactory { _, _ -> opened = true; FakeConnection(byteArrayOf()) },
        )
        val result = gateway.execute(prepared(), DownloadCancellation())
        assertEquals(DownloadResultCode.SECURITY_REJECTED, result.resultCode)
        assertFalse(opened)
    }

    @Test fun `address policy rejects private loopback link local CGNAT ULA and multicast`() {
        listOf(
            "0.0.0.0", "127.0.0.1", "10.0.0.1", "172.16.0.1", "192.168.1.1",
            "169.254.1.1", "100.64.0.1", "224.0.0.1", "::", "::1", "fe80::1", "fc00::1", "ff02::1",
        ).forEach { assertTrue(it, AndroidCellularDownloadGateway.isUnsafeAddress(InetAddress.getByName(it))) }
        assertFalse(AndroidCellularDownloadGateway.isUnsafeAddress(InetAddress.getByName("93.184.216.34")))
        assertFalse(AndroidCellularDownloadGateway.isUnsafeAddress(InetAddress.getByName("2606:2800:220:1:248:1893:25c8:1946")))
    }

    @Test fun `HTTP status is preserved without reading body`() {
        val result = gateway(factory = DownloadConnectionFactory { _, _ ->
            FakeConnection(byteArrayOf(), response = 503)
        }).execute(prepared(), DownloadCancellation())
        assertEquals(DownloadResultCode.HTTP_ERROR, result.resultCode)
        assertEquals(503, result.httpStatus)
    }

    @Test fun `volume stops at target without an extra probe even if server ignores range`() {
        var readBytes = 0
        val connection = object : DownloadConnection {
            override val responseCode = 200
            override val contentLength = 1_000_000_000L
            override val contentEncoding: String? = null
            override val inputStream = object : InputStream() {
                override fun read(): Int { readBytes++; return 0 }
            }
            var requestedEnd = -1L
            override fun requestRange(lastByte: Long) { requestedEnd = lastByte }
            override fun disconnect() = Unit
        }
        val progress = mutableListOf<Long>()
        val result = gateway(factory = DownloadConnectionFactory { _, _ -> connection })
            .executeVolume(prepared(), DownloadCancellation(), 17) { bytes, _ -> progress += bytes }
        assertEquals(DownloadResultCode.COMPLETED, result.resultCode)
        assertEquals(17L, result.bytes)
        assertEquals(17, readBytes)
        assertEquals(16L, connection.requestedEnd)
        assertEquals(17L, progress.last())
    }

    @Test fun `volume early EOF records partial bytes as incomplete`() {
        val result = gateway(body = ByteArray(37)).executeVolume(prepared(), DownloadCancellation(), 100) { _, _ -> }
        assertEquals(DownloadResultCode.INCOMPLETE, result.resultCode)
        assertEquals(DownloadStatus.FAILED, result.status)
        assertEquals(37L, result.bytes)
    }

    @Test fun `valid range completes and incorrect range is rejected before reading`() {
        for (range in listOf("bytes 0-16/1000", "bytes 1-17/1000", null)) {
            val connection = FakeConnection(ByteArray(17), response = 206, range = range)
            val result = gateway(factory = DownloadConnectionFactory { _, _ -> connection })
                .executeVolume(prepared(), DownloadCancellation(), 17) { _, _ -> }
            assertEquals(if (range == "bytes 0-16/1000") DownloadResultCode.COMPLETED else DownloadResultCode.HTTP_ERROR, result.resultCode)
            assertTrue(connection.disconnected)
        }
    }

    @Test fun `cancel after first chunk retains partial bytes and disconnects`() {
        val cancellation = DownloadCancellation()
        val connection = FakeConnection(ByteArray(20_000))
        val result = gateway(factory = DownloadConnectionFactory { _, _ -> connection })
            .executeVolume(prepared(), cancellation, 20_000) { _, _ -> cancellation.cancel() }
        assertEquals(DownloadResultCode.CANCELLED, result.resultCode)
        assertEquals(8192L, result.bytes)
        assertTrue(connection.disconnected)
    }

    @Test fun `DNS TLS and connection failures keep precise category and stage`() {
        val dns = gateway(resolver = HostResolver { _, _ -> throw java.net.UnknownHostException("private diagnostic") })
            .execute(prepared(), DownloadCancellation())
        assertEquals(DownloadResultCode.DNS_FAILURE, dns.resultCode)
        assertEquals(DownloadFailureStage.DNS, dns.failureStage)
        for ((exception, code) in listOf(
            javax.net.ssl.SSLHandshakeException("private diagnostic") to DownloadResultCode.TLS_FAILURE,
            java.net.ConnectException("private diagnostic") to DownloadResultCode.CONNECTION_FAILURE,
            java.net.SocketTimeoutException("private diagnostic") to DownloadResultCode.TIMEOUT,
        )) {
            val result = gateway(factory = DownloadConnectionFactory { _, _ -> throw exception })
                .execute(prepared(), DownloadCancellation())
            assertEquals(code, result.resultCode)
            assertEquals(DownloadFailureStage.CONNECTION, result.failureStage)
            assertEquals(0L, result.bytes)
        }
    }

    @Test fun `response failure and partial body failure record the failing phase`() {
        for (bodyFailure in listOf(false, true)) {
            val connection = object : DownloadConnection {
                override val responseCode: Int get() = if (bodyFailure) 200 else throw java.io.IOException("private diagnostic")
                override val contentLength = -1L
                override val contentEncoding: String? = null
                override val inputStream = object : InputStream() {
                    var reads = 0
                    override fun read(): Int = error("unused")
                    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                        if (reads++ == 0) return 7
                        throw java.io.IOException("private diagnostic")
                    }
                }
                override fun disconnect() = Unit
            }
            val result = gateway(factory = DownloadConnectionFactory { _, _ -> connection })
                .executeVolume(prepared(), DownloadCancellation(), 100) { _, _ -> }
            assertEquals(DownloadResultCode.NETWORK_ERROR, result.resultCode)
            assertEquals(if (bodyFailure) DownloadFailureStage.BODY else DownloadFailureStage.RESPONSE, result.failureStage)
            assertEquals(if (bodyFailure) 7L else 0L, result.bytes)
        }
    }

    @Test fun `wrapped socket response error preserves stage without retry or fallback`() {
        var opens = 0
        var disconnects = 0
        val connection = object : DownloadConnection {
            override val responseCode: Int get() = throw java.io.IOException("private URL",
                java.net.SocketException("private address").apply {
                    initCause(android.system.ErrnoException("private function", android.system.OsConstants.EACCES))
                })
            override val contentLength = -1L
            override val contentEncoding: String? = null
            override val inputStream: InputStream get() = error("must not read")
            override fun disconnect() { disconnects++ }
        }
        val result = gateway(factory = DownloadConnectionFactory { _, _ -> opens++; connection })
            .executeVolume(prepared(), DownloadCancellation(), 100) { _, _ -> }
        assertEquals(DownloadResultCode.CONNECTION_FAILURE, result.resultCode)
        assertEquals(DownloadFailureCause.SOCKET, result.failureCause)
        assertEquals("EACCES", result.failureErrno)
        assertEquals(DownloadFailureStage.RESPONSE, result.failureStage)
        assertEquals(0L, result.bytes)
        assertEquals(1, opens)
        assertEquals(1, disconnects)
        assertEquals(null, result.httpStatus)
    }


    @Test fun `network is acquired before DNS and released after download`() {
        val order = mutableListOf<String>()
        val lease = object : CellularNetworkLease {
            override val networkToken: Any = "requested-cellular"
            override fun close() { order += "release" }
        }
        val result = gateway(
            resolver = HostResolver { token, _ ->
                assertEquals("requested-cellular", token)
                order += "dns"
                publicAddress()
            },
            factory = DownloadConnectionFactory { token, _ ->
                assertEquals("requested-cellular", token)
                order += "http"
                FakeConnection(byteArrayOf(1))
            },
            acquirer = CellularNetworkAcquirer { _, _ -> order += "acquire"; lease },
        ).execute(prepared(), DownloadCancellation())

        assertEquals(DownloadResultCode.COMPLETED, result.resultCode)
        assertEquals(listOf("acquire", "dns", "http", "release"), order)
    }

    @Test fun `unavailable cellular records controlled failure without DNS or HTTP`() {
        var resolved = false
        var opened = false
        val result = gateway(
            resolver = HostResolver { _, _ -> resolved = true; publicAddress() },
            factory = DownloadConnectionFactory { _, _ -> opened = true; FakeConnection(byteArrayOf()) },
            acquirer = CellularNetworkAcquirer { _, _ -> throw CellularNetworkUnavailableException() },
        ).execute(prepared(), DownloadCancellation())

        assertEquals(DownloadResultCode.NETWORK_UNAVAILABLE, result.resultCode)
        assertEquals(DownloadFailureCause.NETWORK_ACQUISITION, result.failureCause)
        assertEquals(DownloadFailureStage.CONNECTION, result.failureStage)
        assertFalse(resolved)
        assertFalse(opened)
    }

    @Test fun `cancel during acquisition does not continue to DNS or HTTP`() {
        var resolved = false
        var opened = false
        val cancellation = DownloadCancellation()
        val result = gateway(
            resolver = HostResolver { _, _ -> resolved = true; publicAddress() },
            factory = DownloadConnectionFactory { _, _ -> opened = true; FakeConnection(byteArrayOf()) },
            acquirer = CellularNetworkAcquirer { _, token ->
                token.cancel()
                throw CellularNetworkUnavailableException()
            },
        ).execute(prepared(), cancellation)

        assertEquals(DownloadResultCode.CANCELLED, result.resultCode)
        assertFalse(resolved)
        assertFalse(opened)
    }

    private fun gateway(
        body: ByteArray = byteArrayOf(),
        resolver: HostResolver = HostResolver { _, _ -> publicAddress() },
        factory: DownloadConnectionFactory = DownloadConnectionFactory { _, _ -> FakeConnection(body) },
        acquirer: CellularNetworkAcquirer = CellularNetworkAcquirer { token, _ ->
            object : CellularNetworkLease {
                override val networkToken: Any = token ?: Any()
                override fun close() = Unit
            }
        },
    ): AndroidCellularDownloadGateway {
        val app = RuntimeEnvironment.getApplication()
        return AndroidCellularDownloadGateway(
            app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager,
            IncrementingTime(),
            resolver,
            factory,
            acquirer,
        )
    }

    private fun prepared() = PreparedCellularDownload(SafeDownloadUrlValidator.requireValid("https://example.com/file"), Any())
    private fun publicAddress() = listOf(InetAddress.getByName("93.184.216.34"))

    private class IncrementingTime : TimeProvider {
        private var value = 100L
        override fun capture() = CapturedTime(value, value).also { value += 100 }
    }

    private class FakeConnection(
        private val body: ByteArray,
        private val response: Int = 200,
        private val declaredLength: Long = -1,
        private val onBody: () -> Unit = {},
        private val range: String? = null,
    ) : DownloadConnection {
        var disconnected = false
        override val contentRange get() = range
        override val responseCode get() = response
        override val contentLength get() = declaredLength
        override val contentEncoding: String? = null
        override val inputStream: InputStream get() = ByteArrayInputStream(body).also { onBody() }
        override fun disconnect() { disconnected = true }
    }
}

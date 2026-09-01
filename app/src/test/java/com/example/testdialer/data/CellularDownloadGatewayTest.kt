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

    private fun gateway(
        body: ByteArray = byteArrayOf(),
        resolver: HostResolver = HostResolver { _, _ -> publicAddress() },
        factory: DownloadConnectionFactory = DownloadConnectionFactory { _, _ -> FakeConnection(body) },
    ): AndroidCellularDownloadGateway {
        val app = RuntimeEnvironment.getApplication()
        return AndroidCellularDownloadGateway(
            app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager,
            IncrementingTime(),
            resolver,
            factory,
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
    ) : DownloadConnection {
        var disconnected = false
        override val responseCode get() = response
        override val contentLength get() = declaredLength
        override val contentEncoding: String? = null
        override val inputStream: InputStream get() = ByteArrayInputStream(body).also { onBody() }
        override fun disconnect() { disconnected = true }
    }
}

package com.example.testdialer.data

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.example.testdialer.domain.execution.CapturedTime
import com.example.testdialer.domain.execution.SystemTimeProvider
import com.example.testdialer.domain.execution.TimeProvider
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class DownloadCancellation {
    private val cancelled = AtomicBoolean(false)
    private val callbacks = CopyOnWriteArrayList<() -> Unit>()
    fun cancel() { if (cancelled.compareAndSet(false, true)) callbacks.forEach { runCatching(it) } }
    fun isCancelled(): Boolean = cancelled.get() || Thread.currentThread().isInterrupted
    fun onCancel(callback: () -> Unit) {
        if (cancelled.get()) callback() else {
            callbacks += callback
            if (cancelled.get() && callbacks.remove(callback)) callback()
        }
    }
    fun remove(callback: () -> Unit) { callbacks.remove(callback) }
}

data class PreparedCellularDownload(val url: SafeDownloadUrl, internal val networkToken: Any? = null)
enum class DownloadStatus { COMPLETED, FAILED, CANCELLED }
enum class DownloadResultCode {
    COMPLETED, CANCELLED, TIMEOUT, DNS_FAILURE, HTTP_ERROR, LIMIT_EXCEEDED, NETWORK_ERROR, SECURITY_REJECTED,
}
data class DownloadResult(
    val status: DownloadStatus,
    val resultCode: DownloadResultCode,
    val networkStartedAt: CapturedTime?,
    val endedAt: CapturedTime,
    val bytes: Long,
    val httpStatus: Int? = null,
)

interface CellularDownloadGateway {
    fun prepare(rawUrl: String): PreparedCellularDownload
    fun execute(prepared: PreparedCellularDownload, cancellation: DownloadCancellation): DownloadResult
}
fun interface HostResolver {
    @Throws(UnknownHostException::class)
    fun resolve(networkToken: Any?, host: String): List<InetAddress>
}
interface DownloadConnection {
    val responseCode: Int
    val contentLength: Long
    val contentEncoding: String?
    val inputStream: InputStream
    fun disconnect()
}
fun interface DownloadConnectionFactory {
    fun open(networkToken: Any?, url: URL): DownloadConnection
}

class AndroidCellularDownloadGateway(
    private val connectivityManager: ConnectivityManager,
    private val timeProvider: TimeProvider = SystemTimeProvider,
    private val resolver: HostResolver = HostResolver { token, host -> (token as Network).getAllByName(host).toList() },
    private val connectionFactory: DownloadConnectionFactory = DownloadConnectionFactory { token, url ->
        val connection = (token as Network).openConnection(url) as HttpURLConnection
        connection.instanceFollowRedirects = false
        connection.useCaches = false
        connection.doOutput = false
        connection.requestMethod = "GET"
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.setRequestProperty("Accept-Encoding", "identity")
        AndroidDownloadConnection(connection)
    },
) : CellularDownloadGateway {
    override fun prepare(rawUrl: String): PreparedCellularDownload {
        val url = SafeDownloadUrlValidator.requireValid(rawUrl)
        val network = requireNotNull(connectivityManager.activeNetwork) { "Brak aktywnej sieci" }
        val capabilities = requireNotNull(connectivityManager.getNetworkCapabilities(network)) {
            "Nie można odczytać parametrów aktywnej sieci"
        }
        require(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) { "Aktywna sieć nie jest siecią komórkową" }
        require(!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) { "Test danych nie działa przez VPN" }
        require(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            "Sieć komórkowa nie zgłasza dostępu do Internetu"
        }
        return PreparedCellularDownload(url, network)
    }

    override fun execute(prepared: PreparedCellularDownload, cancellation: DownloadCancellation): DownloadResult {
        if (cancellation.isCancelled()) return cancelledResult()
        val started = timeProvider.capture()
        var bytes = 0L
        var httpStatus: Int? = null
        var connection: DownloadConnection? = null
        var resultCode = DownloadResultCode.NETWORK_ERROR
        try {
            val addresses = resolver.resolve(prepared.networkToken, prepared.url.host)
            require(addresses.isNotEmpty() && addresses.none(::isUnsafeAddress)) { "Niepubliczny wynik DNS" }
            if (cancellation.isCancelled()) return cancelledResult(started)
            connection = connectionFactory.open(prepared.networkToken, prepared.url.uri.toURL())
            val active = requireNotNull(connection)
            val disconnect = { active.disconnect() }
            cancellation.onCancel(disconnect)
            try {
                if (cancellation.isCancelled()) return cancelledResult(started)
                httpStatus = active.responseCode
                if (httpStatus !in 200..299) return failedResult(DownloadResultCode.HTTP_ERROR, started, bytes, httpStatus)
                val encoding = active.contentEncoding
                if (!encoding.isNullOrBlank() && !encoding.equals("identity", true)) {
                    return failedResult(DownloadResultCode.SECURITY_REJECTED, started, bytes, httpStatus)
                }
                if (active.contentLength > MAX_BYTES) {
                    return failedResult(DownloadResultCode.LIMIT_EXCEEDED, started, bytes, httpStatus)
                }
                active.inputStream.use { input ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (bytes < MAX_BYTES) {
                        if (cancellation.isCancelled()) return cancelledResult(started, bytes, httpStatus)
                        val remaining = (MAX_BYTES - bytes).coerceAtMost(buffer.size.toLong()).toInt()
                        val count = input.read(buffer, 0, remaining)
                        if (count < 0) break
                        bytes += count
                    }
                    if (bytes == MAX_BYTES && input.read() >= 0) {
                        return failedResult(DownloadResultCode.LIMIT_EXCEEDED, started, bytes, httpStatus)
                    }
                }
                if (cancellation.isCancelled()) return cancelledResult(started, bytes, httpStatus)
                return DownloadResult(
                    DownloadStatus.COMPLETED, DownloadResultCode.COMPLETED, started,
                    timeProvider.capture(), bytes, httpStatus,
                )
            } finally {
                cancellation.remove(disconnect)
            }
        } catch (_: SocketTimeoutException) {
            resultCode = DownloadResultCode.TIMEOUT
        } catch (_: UnknownHostException) {
            resultCode = DownloadResultCode.DNS_FAILURE
        } catch (_: SecurityException) {
            resultCode = DownloadResultCode.SECURITY_REJECTED
        } catch (_: IllegalArgumentException) {
            resultCode = DownloadResultCode.SECURITY_REJECTED
        } catch (_: Throwable) {
            resultCode = if (cancellation.isCancelled()) DownloadResultCode.CANCELLED else DownloadResultCode.NETWORK_ERROR
        } finally {
            connection?.disconnect()
        }
        return if (cancellation.isCancelled() || resultCode == DownloadResultCode.CANCELLED) {
            cancelledResult(started, bytes, httpStatus)
        } else failedResult(resultCode, started, bytes, httpStatus)
    }

    private fun failedResult(code: DownloadResultCode, started: CapturedTime, bytes: Long, status: Int?) =
        DownloadResult(DownloadStatus.FAILED, code, started, timeProvider.capture(), bytes, status)
    private fun cancelledResult(started: CapturedTime? = null, bytes: Long = 0L, status: Int? = null) =
        DownloadResult(DownloadStatus.CANCELLED, DownloadResultCode.CANCELLED, started, timeProvider.capture(), bytes, status)

    companion object {
        const val MAX_BYTES = 1024L * 1024L
        private const val BUFFER_SIZE = 8192
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 10_000
        internal fun isUnsafeAddress(address: InetAddress): Boolean {
            if (address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress ||
                address.isSiteLocalAddress || address.isMulticastAddress) return true
            val raw = address.address
            if (address is Inet4Address) {
                val first = raw[0].toInt() and 0xff
                val second = raw[1].toInt() and 0xff
                if (first == 100 && second in 64..127) return true
            }
            if (address is Inet6Address && ((raw[0].toInt() and 0xfe) == 0xfc)) return true
            return false
        }
    }
}

private class AndroidDownloadConnection(private val delegate: HttpURLConnection) : DownloadConnection {
    override val responseCode: Int get() = delegate.responseCode
    override val contentLength: Long get() = delegate.contentLengthLong
    override val contentEncoding: String? get() = delegate.contentEncoding
    override val inputStream: InputStream get() = delegate.inputStream
    override fun disconnect() = delegate.disconnect()
}

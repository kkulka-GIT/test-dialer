package com.example.testdialer.data

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.example.testdialer.domain.execution.CapturedTime
import com.example.testdialer.domain.execution.SystemTimeProvider
import com.example.testdialer.domain.execution.TimeProvider
import java.net.HttpURLConnection
import java.util.concurrent.atomic.AtomicBoolean

data class PreparedCellularDownload(
    val url: SafeDownloadUrl,
    internal val token: Any? = null,
)

enum class DownloadStatus { COMPLETED, FAILED, CANCELLED }

data class DownloadResult(
    val status: DownloadStatus,
    val networkStartedAt: CapturedTime,
    val endedAt: CapturedTime,
    val bytes: Long,
    val code: String,
)

interface CellularDownloadGateway {
    fun prepare(rawUrl: String): PreparedCellularDownload
    fun execute(prepared: PreparedCellularDownload): DownloadResult
    fun cancel()
}

class AndroidCellularDownloadGateway(
    private val connectivityManager: ConnectivityManager,
    private val timeProvider: TimeProvider = SystemTimeProvider,
) : CellularDownloadGateway {
    private val cancelled = AtomicBoolean(false)
    @Volatile private var connection: HttpURLConnection? = null

    override fun prepare(rawUrl: String): PreparedCellularDownload {
        val url = SafeDownloadUrlValidator.requireValid(rawUrl)
        val network = requireNotNull(connectivityManager.activeNetwork) { "Brak aktywnej sieci" }
        val capabilities = requireNotNull(connectivityManager.getNetworkCapabilities(network)) {
            "Nie można odczytać parametrów aktywnej sieci"
        }
        require(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            "Aktywna sieć nie jest siecią komórkową"
        }
        require(!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
            "Test danych nie działa przez VPN"
        }
        require(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            "Sieć komórkowa nie zgłasza dostępu do Internetu"
        }
        return PreparedCellularDownload(url, network)
    }

    override fun execute(prepared: PreparedCellularDownload): DownloadResult {
        cancelled.set(false)
        val started = timeProvider.capture()
        var bytes = 0L
        var status = DownloadStatus.FAILED
        var code = "DOWNLOAD_FAILED"
        try {
            val network = prepared.token as? Network ?: error("Nieprawidłowy token sieci")
            val opened = network.openConnection(prepared.url.uri.toURL()) as? HttpURLConnection
                ?: error("Połączenie nie jest HTTP")
            connection = opened
            opened.instanceFollowRedirects = false
            opened.useCaches = false
            opened.doOutput = false
            opened.requestMethod = "GET"
            opened.connectTimeout = CONNECT_TIMEOUT_MS
            opened.readTimeout = READ_TIMEOUT_MS
            opened.setRequestProperty("Accept-Encoding", "identity")
            val response = opened.responseCode
            require(response in 200..299) {
                if (response in 300..399) "Przekierowania są niedozwolone" else "HTTP $response"
            }
            val contentEncoding = opened.contentEncoding
            require(contentEncoding.isNullOrBlank() || contentEncoding.equals("identity", ignoreCase = true)) {
                "Skompresowane odpowiedzi są niedozwolone"
            }
            opened.inputStream.use { input ->
                val buffer = ByteArray(BUFFER_SIZE)
                while (true) {
                    if (cancelled.get()) break
                    val count = input.read(buffer)
                    if (count < 0) break
                    bytes += count
                    require(bytes <= MAX_BYTES) { "Odpowiedź przekroczyła 1 MiB" }
                }
            }
            if (cancelled.get()) {
                status = DownloadStatus.CANCELLED
                code = "CANCELLED"
            } else {
                status = DownloadStatus.COMPLETED
                code = "COMPLETED"
            }
        } catch (error: Throwable) {
            if (cancelled.get()) {
                status = DownloadStatus.CANCELLED
                code = "CANCELLED"
            } else {
                status = DownloadStatus.FAILED
                code = "FAILED"
            }
        } finally {
            connection?.disconnect()
            connection = null
        }
        return DownloadResult(status, started, timeProvider.capture(), bytes, code)
    }

    override fun cancel() {
        cancelled.set(true)
        connection?.disconnect()
    }

    companion object {
        const val MAX_BYTES = 1024L * 1024L
        private const val BUFFER_SIZE = 8192
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 10_000
    }
}

package com.example.testdialer.data

import android.system.ErrnoException
import android.system.OsConstants
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Collections
import java.util.IdentityHashMap
import javax.net.ssl.SSLException

enum class DownloadFailureCause { NETWORK_ACQUISITION, TIMEOUT, DNS, TLS, CONNECT, SOCKET, SECURITY, INVALID_ARGUMENT, IO, OTHER }

internal data class DownloadFailureDiagnostic(
    val code: DownloadResultCode,
    val cause: DownloadFailureCause,
    val errno: String? = null,
)

/** Only allowlisted categories leave this boundary: never messages, stack traces or addresses.
 * RESPONSE is the API operation that failed, not proof that DNS/connect/TLS completed.
 */
internal fun classifyDownloadFailure(error: Throwable): DownloadFailureDiagnostic {
    val seen = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
    val chain = mutableListOf<Throwable>()
    var current: Throwable? = error
    while (current != null && chain.size < 32 && seen.add(current)) {
        chain += current
        current = current.cause
    }
    val errno = chain.filterIsInstance<ErrnoException>().firstOrNull()?.errno?.let {
        when (it) {
            OsConstants.EACCES -> "EACCES"
            OsConstants.EPERM -> "EPERM"
            OsConstants.ENONET -> "ENONET"
            OsConstants.ENODEV -> "ENODEV"
            OsConstants.EADDRNOTAVAIL -> "EADDRNOTAVAIL"
            OsConstants.ENETUNREACH -> "ENETUNREACH"
            OsConstants.EHOSTUNREACH -> "EHOSTUNREACH"
            OsConstants.ECONNREFUSED -> "ECONNREFUSED"
            OsConstants.ECONNRESET -> "ECONNRESET"
            OsConstants.ETIMEDOUT -> "ETIMEDOUT"
            OsConstants.EPIPE -> "EPIPE"
            else -> "OTHER"
        }
    }
    // Preserve TLS context even when it wraps a socket error.
    val cause = when {
        chain.any { it is CellularNetworkUnavailableException } -> DownloadFailureCause.NETWORK_ACQUISITION
        chain.any { it is SecurityException } -> DownloadFailureCause.SECURITY
        chain.any { it is SSLException } -> DownloadFailureCause.TLS
        chain.any { it is SocketTimeoutException } -> DownloadFailureCause.TIMEOUT
        chain.any { it is UnknownHostException } -> DownloadFailureCause.DNS
        chain.any { it is ConnectException } -> DownloadFailureCause.CONNECT
        chain.any { it is SocketException || it is ErrnoException } -> DownloadFailureCause.SOCKET
        chain.any { it is IllegalArgumentException } -> DownloadFailureCause.INVALID_ARGUMENT
        chain.any { it is IOException } -> DownloadFailureCause.IO
        else -> DownloadFailureCause.OTHER
    }
    val code = when (cause) {
        DownloadFailureCause.NETWORK_ACQUISITION -> DownloadResultCode.NETWORK_UNAVAILABLE
        DownloadFailureCause.SECURITY, DownloadFailureCause.INVALID_ARGUMENT -> DownloadResultCode.SECURITY_REJECTED
        DownloadFailureCause.TLS -> DownloadResultCode.TLS_FAILURE
        DownloadFailureCause.TIMEOUT -> DownloadResultCode.TIMEOUT
        DownloadFailureCause.DNS -> DownloadResultCode.DNS_FAILURE
        DownloadFailureCause.CONNECT, DownloadFailureCause.SOCKET -> DownloadResultCode.CONNECTION_FAILURE
        else -> DownloadResultCode.NETWORK_ERROR
    }
    return DownloadFailureDiagnostic(code, cause, errno)
}

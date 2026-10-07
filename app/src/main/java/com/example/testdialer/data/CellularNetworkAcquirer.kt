package com.example.testdialer.data

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal interface CellularNetworkLease : AutoCloseable {
    val networkToken: Any
}

internal fun interface CellularNetworkAcquirer {
    @Throws(IOException::class)
    fun acquire(preferredNetworkToken: Any?, cancellation: DownloadCancellation): CellularNetworkLease
}

internal class CellularNetworkUnavailableException : IOException()

internal interface CellularNetworkRequester {
    fun request(listener: Listener, timeoutMillis: Int): AutoCloseable

    interface Listener {
        fun onCapabilitiesChanged(networkToken: Any, directCellular: Boolean)
        fun onUnavailable()
        fun onLost(networkToken: Any)
    }
}

private class AndroidCellularNetworkRequester(
    private val connectivityManager: ConnectivityManager,
) : CellularNetworkRequester {
    override fun request(listener: CellularNetworkRequester.Listener, timeoutMillis: Int): AutoCloseable {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                listener.onCapabilitiesChanged(network, capabilities.isDirectCellular())
            }

            override fun onUnavailable() = listener.onUnavailable()
            override fun onLost(network: Network) = listener.onLost(network)
        }
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            .build()
        connectivityManager.requestNetwork(request, callback, timeoutMillis)
        val closed = AtomicBoolean(false)
        return AutoCloseable {
            if (closed.compareAndSet(false, true)) {
                connectivityManager.unregisterNetworkCallback(callback)
            }
        }
    }

    private fun NetworkCapabilities.isDirectCellular(): Boolean =
        hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) &&
            !hasTransport(NetworkCapabilities.TRANSPORT_VPN) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
}

/**
 * Requests and retains a direct cellular network for the whole transfer.
 * The process is never bound globally, so Wi-Fi and an active VPN remain untouched.
 */
internal class AndroidCellularNetworkAcquirer(
    private val requester: CellularNetworkRequester,
    private val timeoutMillis: Int = NETWORK_TIMEOUT_MS,
) : CellularNetworkAcquirer {
    constructor(connectivityManager: ConnectivityManager) :
        this(AndroidCellularNetworkRequester(connectivityManager))

    @Suppress("UNUSED_PARAMETER")
    override fun acquire(preferredNetworkToken: Any?, cancellation: DownloadCancellation): CellularNetworkLease {
        if (cancellation.isCancelled()) throw CellularNetworkUnavailableException()
        val ready = CountDownLatch(1)
        val selected = AtomicReference<Any?>(null)
        val registration = AtomicReference<AutoCloseable?>(null)
        val released = AtomicBoolean(false)

        fun release() {
            if (released.compareAndSet(false, true)) {
                runCatching { registration.getAndSet(null)?.close() }
                ready.countDown()
            }
        }

        val listener = object : CellularNetworkRequester.Listener {
            override fun onCapabilitiesChanged(networkToken: Any, directCellular: Boolean) {
                if (directCellular && selected.compareAndSet(null, networkToken)) ready.countDown()
            }

            override fun onUnavailable() = ready.countDown()
            override fun onLost(networkToken: Any) {
                if (selected.compareAndSet(networkToken, null)) ready.countDown()
            }
        }
        val cancelCallback = { release() }
        cancellation.onCancel(cancelCallback)
        try {
            val newRegistration = requester.request(listener, timeoutMillis)
            if (!registration.compareAndSet(null, newRegistration) || released.get()) {
                runCatching { newRegistration.close() }
            }
            if (!ready.await(timeoutMillis.toLong() + CALLBACK_GRACE_MS, TimeUnit.MILLISECONDS) ||
                cancellation.isCancelled() || released.get()
            ) {
                release()
                throw CellularNetworkUnavailableException()
            }
            val network = selected.get() ?: run {
                release()
                throw CellularNetworkUnavailableException()
            }
            return object : CellularNetworkLease {
                override val networkToken: Any = network
                override fun close() = release()
            }
        } catch (error: Throwable) {
            release()
            if (error is InterruptedException) Thread.currentThread().interrupt()
            throw error
        } finally {
            cancellation.remove(cancelCallback)
        }
    }

    companion object {
        internal const val NETWORK_TIMEOUT_MS = 10_000
        private const val CALLBACK_GRACE_MS = 500L
    }
}

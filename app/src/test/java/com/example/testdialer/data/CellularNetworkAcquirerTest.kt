package com.example.testdialer.data

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CellularNetworkAcquirerTest {
    @Test fun `retains requested cellular network until lease closes exactly once`() {
        val requester = FakeRequester()
        val outcome = AtomicReference<Any>()
        val finished = CountDownLatch(1)

        Thread {
            outcome.set(runCatching {
                AndroidCellularNetworkAcquirer(requester, 1_000)
                    .acquire(null, DownloadCancellation())
            })
            finished.countDown()
        }.start()

        val listener = requester.awaitListener()
        val token = Any()
        listener.onCapabilitiesChanged(token, directCellular = false)
        assertEquals(0, requester.closeCount.get())
        listener.onCapabilitiesChanged(token, directCellular = true)
        assertTrue(finished.await(1, TimeUnit.SECONDS))

        val lease = (outcome.get() as Result<*>).getOrThrow() as CellularNetworkLease
        assertTrue(lease.networkToken === token)
        assertEquals(0, requester.closeCount.get())
        lease.close()
        lease.close()
        assertEquals(1, requester.closeCount.get())
    }

    @Test fun `unavailable request fails and releases registration`() {
        val requester = FakeRequester()
        val outcome = AtomicReference<Throwable?>()
        val finished = CountDownLatch(1)

        Thread {
            outcome.set(runCatching {
                AndroidCellularNetworkAcquirer(requester, 1_000)
                    .acquire(null, DownloadCancellation())
            }.exceptionOrNull())
            finished.countDown()
        }.start()

        requester.awaitListener().onUnavailable()
        assertTrue(finished.await(1, TimeUnit.SECONDS))
        assertTrue(outcome.get() is CellularNetworkUnavailableException)
        assertEquals(1, requester.closeCount.get())
    }

    @Test fun `cancellation while waiting releases request and fails promptly`() {
        val requester = FakeRequester()
        val cancellation = DownloadCancellation()
        val outcome = AtomicReference<Throwable?>()
        val finished = CountDownLatch(1)

        Thread {
            outcome.set(runCatching {
                AndroidCellularNetworkAcquirer(requester, 5_000)
                    .acquire(null, cancellation)
            }.exceptionOrNull())
            finished.countDown()
        }.start()

        assertNotNull(requester.awaitListener())
        cancellation.cancel()
        assertTrue(finished.await(1, TimeUnit.SECONDS))
        assertTrue(outcome.get() is CellularNetworkUnavailableException)
        assertEquals(1, requester.closeCount.get())
    }

    private class FakeRequester : CellularNetworkRequester {
        private val listenerReady = CountDownLatch(1)
        private val listener = AtomicReference<CellularNetworkRequester.Listener>()
        val closeCount = AtomicInteger(0)

        override fun request(listener: CellularNetworkRequester.Listener, timeoutMillis: Int): AutoCloseable {
            this.listener.set(listener)
            listenerReady.countDown()
            return AutoCloseable { closeCount.incrementAndGet() }
        }

        fun awaitListener(): CellularNetworkRequester.Listener {
            assertTrue(listenerReady.await(1, TimeUnit.SECONDS))
            return listener.get()
        }
    }
}

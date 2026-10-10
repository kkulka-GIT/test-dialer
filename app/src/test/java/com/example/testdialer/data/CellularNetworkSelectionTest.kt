package com.example.testdialer.data

import android.net.Network
import android.net.NetworkCapabilities
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNetwork
import org.robolectric.shadows.ShadowNetworkCapabilities
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CellularNetworkSelectionTest {
    private val mobile = ShadowNetwork.newInstance(101)
    private val vpn = ShadowNetwork.newInstance(102)
    private val wifi = ShadowNetwork.newInstance(103)
    private fun caps(vararg transport: Int) = ShadowNetworkCapabilities.newInstance().apply {
        transport.forEach { shadowOf(this).addTransportType(it) }
        shadowOf(this).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    private fun select(active: Network?, available: Map<Network, NetworkCapabilities>) =
        selectCellularNetwork(active, available.keys.toList(), available::get)

    @Test fun `default VPN advertising cellular does not block physical mobile`() {
        val available = mapOf(vpn to caps(NetworkCapabilities.TRANSPORT_VPN, NetworkCapabilities.TRANSPORT_CELLULAR), mobile to caps(NetworkCapabilities.TRANSPORT_CELLULAR))
        assertEquals(mobile, select(vpn, available))
    }
    @Test fun `default VPN without cellular transport can still use available mobile`() {
        assertEquals(mobile, select(vpn, mapOf(vpn to caps(NetworkCapabilities.TRANSPORT_VPN), mobile to caps(NetworkCapabilities.TRANSPORT_CELLULAR))))
    }
    @Test fun `default cellular is preferred over another mobile network`() {
        val other = ShadowNetwork.newInstance(104)
        assertEquals(mobile, select(mobile, mapOf(other to caps(NetworkCapabilities.TRANSPORT_CELLULAR), mobile to caps(NetworkCapabilities.TRANSPORT_CELLULAR))))
    }
    @Test fun `wifi default does not steal the mobile data test`() {
        assertEquals(mobile, select(wifi, mapOf(wifi to caps(NetworkCapabilities.TRANSPORT_WIFI), mobile to caps(NetworkCapabilities.TRANSPORT_CELLULAR))))
    }
    @Test fun `no fallback to wifi or VPN if physical cellular is unavailable`() {
        assertNull(select(vpn, mapOf(vpn to caps(NetworkCapabilities.TRANSPORT_VPN, NetworkCapabilities.TRANSPORT_CELLULAR), wifi to caps(NetworkCapabilities.TRANSPORT_WIFI))))
        assertNull(select(null, emptyMap()))
    }
    @Test fun `cellular without internet capability is not selected`() {
        assertNull(select(mobile, mapOf(mobile to caps(NetworkCapabilities.TRANSPORT_CELLULAR).apply { shadowOf(this).removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) })))
    }

    @Test fun `VPN presence is reported independently from cellular selection`() {
        val available = mapOf(
            vpn to caps(NetworkCapabilities.TRANSPORT_VPN),
            mobile to caps(NetworkCapabilities.TRANSPORT_CELLULAR),
        )
        assertTrue(hasVpnTransport(available.keys.toList(), available::get))
        assertEquals(mobile, select(vpn, available))
        assertFalse(hasVpnTransport(listOf(mobile), available::get))
    }
}

package com.example.testdialer.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager

internal data class PhoneNetworkStatus(
    val sim: String, val cellular: String, val wifi: String, val vpn: String, val defaultNetwork: String,
) {
    val rows get() = listOf("SIM: $sim", "Cellular: $cellular", "Wi-Fi: $wifi", "VPN: $vpn", "Sieć domyślna aplikacji: $defaultNetwork")
}

internal class PhoneNetworkStatusReader(context: Context) {
    private val app = context.applicationContext
    private val connectivity = app.getSystemService(ConnectivityManager::class.java)
    private val telephony = app.getSystemService(TelephonyManager::class.java)
    private val wifi = app.getSystemService(WifiManager::class.java)

    fun read(): PhoneNetworkStatus {
        val networks = runCatching { connectivity.allNetworks.mapNotNull(connectivity::getNetworkCapabilities) }.getOrNull()
        // VPN capabilities can also contain underlying WIFI/CELLULAR transports.
        val physical = networks?.filterNot { it.hasTransport(NetworkCapabilities.TRANSPORT_VPN) }
        val cell = physical?.any { it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) }
        val wifiConnected = physical?.any { it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) }
        val data = runCatching { telephony.isDataEnabled }.getOrNull()
        val technology = if (Build.VERSION.SDK_INT >= 33) runCatching {
            when (telephony.dataNetworkType) {
                TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
                TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
                TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_HSDPA,
                TelephonyManager.NETWORK_TYPE_HSUPA, TelephonyManager.NETWORK_TYPE_HSPA,
                TelephonyManager.NETWORK_TYPE_HSPAP -> "3G"
                TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_EDGE -> "2G"
                else -> "technologia nieustalona"
            }
        }.getOrDefault("technologia niedostępna") else "technologia wymaga uprawnienia telefonu"
        val sim = runCatching {
            when (telephony.simState) {
                TelephonyManager.SIM_STATE_READY -> "gotowa (domyślna SIM)"
                TelephonyManager.SIM_STATE_ABSENT -> "brak"
                TelephonyManager.SIM_STATE_PIN_REQUIRED -> "wymagany PIN"
                TelephonyManager.SIM_STATE_PUK_REQUIRED -> "wymagany PUK"
                TelephonyManager.SIM_STATE_NOT_READY -> "niegotowa"
                else -> "nie ustalono"
            }
        }.getOrDefault("nie ustalono")
        val default = runCatching {
            val network = connectivity.activeNetwork ?: return@runCatching "brak"
            val caps = connectivity.getNetworkCapabilities(network) ?: return@runCatching "nie ustalono"
            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "inna"
            }
        }.getOrDefault("nie ustalono")
        return PhoneNetworkStatus(sim,
            "${state(data, "dane włączone", "dane wyłączone")} · $technology · ${state(cell, "sieć widoczna", "sieć niewidoczna")}",
            "${state(runCatching { wifi.isWifiEnabled }.getOrNull(), "włączone", "wyłączone")} · ${state(wifiConnected, "połączone", "niepołączone")}",
            state(networks?.any { it.hasTransport(NetworkCapabilities.TRANSPORT_VPN) }, "aktywny", "nieaktywny"), default)
    }
    private fun state(value: Boolean?, yes: String, no: String) = when (value) { true -> yes; false -> no; null -> "nie ustalono" }
}

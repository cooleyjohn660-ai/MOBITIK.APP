package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL
import java.util.Collections

object VpnDetector {

    private const val TAG = "VpnDetector"

    /**
     * Checks if a VPN or proxy tunnel is active on the device.
     */
    fun isVpnActive(context: Context): Boolean {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm != null) {
                val activeNetwork = cm.activeNetwork
                if (activeNetwork != null) {
                    val caps = cm.getNetworkCapabilities(activeNetwork)
                    if (caps != null) {
                        // Check TRANSPORT_VPN
                        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                            Log.w(TAG, "VPN detected via TRANSPORT_VPN")
                            return true
                        }
                        // Check NET_CAPABILITY_NOT_VPN (if false, it's a VPN)
                        if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)) {
                            Log.w(TAG, "VPN detected: NET_CAPABILITY_NOT_VPN is missing")
                            return true
                        }
                    }
                }
            }

            // Secondary check: inspect network interfaces for tun/ppp/tap/p2p
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (intf.isUp && intf.interfaceAddresses.isNotEmpty()) {
                    val name = intf.name.lowercase()
                    if (name.startsWith("tun") || name.startsWith("ppp") || name.startsWith("tap") || name.startsWith("p2p")) {
                        Log.w(TAG, "VPN detected via interface name: ${intf.name}")
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking VPN status", e)
        }
        return false
    }

    /**
     * Gets the public or local IPv4 address of the user.
     */
    suspend fun resolveUserIpAddress(): String = withContext(Dispatchers.IO) {
        // Attempt 1: Fast external IP lookup
        val endpoints = listOf("https://api.ipify.org", "https://icanhazip.com")
        for (endpoint in endpoints) {
            try {
                val url = URL(endpoint)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.setRequestProperty("User-Agent", "MOBITIK-Client/5.0")
                if (conn.responseCode == 200) {
                    val ip = conn.inputStream.bufferedReader().readText().trim()
                    if (ip.isNotBlank() && ip.length <= 45 && !ip.contains("<")) {
                        return@withContext ip
                    }
                }
            } catch (_: Exception) {}
        }

        // Fallback: local active IPv4
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (intf.isUp && !intf.isLoopback) {
                    for (addr in Collections.list(intf.inetAddresses)) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            return@withContext addr.hostAddress ?: "127.0.0.1"
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        "127.0.0.1"
    }
}

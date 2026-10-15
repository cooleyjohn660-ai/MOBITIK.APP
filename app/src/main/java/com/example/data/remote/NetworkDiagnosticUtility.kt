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
import java.net.Proxy
import java.net.ProxySelector
import java.net.URI
import java.net.URL
import java.util.Collections

/**
 * Diagnostics report data class capturing real-time connection telemetry,
 * VPN/Proxy indicators, latency, and mining eligibility.
 */
data class NetworkDiagnosticReport(
    val isVpnDetected: Boolean,
    val hasTransportVpn: Boolean,
    val isProxyDetected: Boolean,
    val proxyDetails: String? = null,
    val connectionType: String,
    val ipAddress: String,
    val latencyMs: Long,
    val isMiningAllowed: Boolean,
    val diagnosticSummary: String,
    val timestampMs: Long = System.currentTimeMillis()
)

sealed class MiningEligibilityResult {
    data object Allowed : MiningEligibilityResult()
    data class Blocked(val reason: String, val report: NetworkDiagnosticReport) : MiningEligibilityResult()
}

object NetworkDiagnosticUtility {

    private const val TAG = "NetworkDiagnostic"

    /**
     * Executes a comprehensive network scan inspecting TRANSPORT_VPN,
     * NET_CAPABILITY_NOT_VPN, system proxy properties, network interfaces,
     * public IP, and latency.
     */
    suspend fun runDiagnostic(context: Context): NetworkDiagnosticReport = withContext(Dispatchers.IO) {
        var hasTransportVpn = false
        var lacksNotVpnCapability = false
        var connectionType = "DISCONNECTED"

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm != null) {
            val activeNetwork = cm.activeNetwork
            if (activeNetwork != null) {
                val caps = cm.getNetworkCapabilities(activeNetwork)
                if (caps != null) {
                    // 1. Explicit check: NetworkCapabilities.TRANSPORT_VPN
                    hasTransportVpn = caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)

                    // 2. Explicit check: NET_CAPABILITY_NOT_VPN
                    lacksNotVpnCapability = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)

                    // Determine active connection type
                    connectionType = when {
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN Tunnel"
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular (Mobile Data)"
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                        else -> "Active Network"
                    }
                }
            }
        }

        // 3. Network interface inspection for virtual tun/tap/ppp adapters
        val hasVirtualInterface = inspectVirtualInterfaces()

        val isVpnActive = hasTransportVpn || lacksNotVpnCapability || hasVirtualInterface

        // 4. Proxy detection
        val (isProxyActive, proxyInfo) = detectProxyUsage()

        // 5. Measure latency and resolve public IP
        val (latency, ip) = measureLatencyAndResolveIp()

        // 6. Mining eligibility rule: Both VPN and Proxy strictly prohibit protocol mining
        val isMiningAllowed = !isVpnActive && !isProxyActive

        val summary = when {
            hasTransportVpn -> "CRITICAL: Active VPN detected via NetworkCapabilities.TRANSPORT_VPN. Mining is strictly prohibited."
            isVpnActive -> "CRITICAL: Virtual network tunnel/proxy detected. Mining is suspended."
            isProxyActive -> "WARNING: Active HTTP/SOCKS proxy detected ($proxyInfo). Mining blocked."
            else -> "SECURE: Direct connection verified on $connectionType ($latency ms). Mining authorized."
        }

        NetworkDiagnosticReport(
            isVpnDetected = isVpnActive,
            hasTransportVpn = hasTransportVpn,
            isProxyDetected = isProxyActive,
            proxyDetails = proxyInfo,
            connectionType = connectionType,
            ipAddress = ip,
            latencyMs = latency,
            isMiningAllowed = isMiningAllowed,
            diagnosticSummary = summary
        )
    }

    /**
     * Inspects network interfaces for virtual adapters like tun0, ppp0, tap0, wg0 (WireGuard).
     */
    private fun inspectVirtualInterfaces(): Boolean {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (intf.isUp && intf.interfaceAddresses.isNotEmpty()) {
                    val name = intf.name.lowercase()
                    if (name.startsWith("tun") || name.startsWith("ppp") ||
                        name.startsWith("tap") || name.startsWith("p2p") ||
                        name.startsWith("wg")
                    ) {
                        Log.w(TAG, "Virtual interface detected: ${intf.name}")
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking interfaces", e)
        }
        return false
    }

    /**
     * Checks standard JVM system properties and ProxySelector for active proxies.
     */
    private fun detectProxyUsage(): Pair<Boolean, String?> {
        try {
            val httpProxyHost = System.getProperty("http.proxyHost")
            val httpProxyPort = System.getProperty("http.proxyPort")
            if (!httpProxyHost.isNullOrBlank()) {
                return true to "HTTP Proxy: $httpProxyHost:$httpProxyPort"
            }

            val httpsProxyHost = System.getProperty("https.proxyHost")
            val httpsProxyPort = System.getProperty("https.proxyPort")
            if (!httpsProxyHost.isNullOrBlank()) {
                return true to "HTTPS Proxy: $httpsProxyHost:$httpsProxyPort"
            }

            // Check default ProxySelector for a target URI
            val selector = ProxySelector.getDefault()
            if (selector != null) {
                val proxies = selector.select(URI("https://bsc-dataseed.binance.org"))
                for (p in proxies) {
                    if (p.type() != Proxy.Type.DIRECT) {
                        return true to "Selector Proxy: ${p.type()} -> ${p.address()}"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking proxy", e)
        }
        return false to null
    }

    /**
     * Measures round-trip ping time to a high-availability endpoint and resolves IP.
     */
    private fun measureLatencyAndResolveIp(): Pair<Long, String> {
        val start = System.currentTimeMillis()
        var resolvedIp = "127.0.0.1"
        var latency = 0L

        val endpoints = listOf("https://api.ipify.org", "https://icanhazip.com")
        for (endpoint in endpoints) {
            try {
                val url = URL(endpoint)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "MOBITIK-Diagnostic/5.0")
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().readText().trim()
                    if (body.isNotBlank() && body.length <= 45 && !body.contains("<")) {
                        resolvedIp = body
                        latency = (System.currentTimeMillis() - start).coerceAtLeast(1L)
                        return latency to resolvedIp
                    }
                }
            } catch (_: Exception) {}
        }

        // Fallback local IP
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (intf.isUp && !intf.isLoopback) {
                    for (addr in Collections.list(intf.inetAddresses)) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            resolvedIp = addr.hostAddress ?: "127.0.0.1"
                            break
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        latency = (System.currentTimeMillis() - start).coerceAtLeast(1L)
        return latency to resolvedIp
    }

    /**
     * Quick verification check specifically for mining session gates.
     */
    suspend fun verifyMiningSessionAllowed(context: Context): MiningEligibilityResult {
        val report = runDiagnostic(context)
        return if (report.isMiningAllowed) {
            MiningEligibilityResult.Allowed
        } else {
            val reason = if (report.hasTransportVpn) {
                "Active VPN detected via NetworkCapabilities.TRANSPORT_VPN."
            } else if (report.isVpnDetected) {
                "VPN / Virtual tunnel detected."
            } else {
                "Proxy usage detected (${report.proxyDetails ?: "System Proxy"})."
            }
            MiningEligibilityResult.Blocked(reason, report)
        }
    }
}

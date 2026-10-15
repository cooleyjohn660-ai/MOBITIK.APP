package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.VpnKeyOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.NetworkDiagnosticReport
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MobitikErrorRed
import com.example.ui.theme.MobitikGold
import com.example.ui.theme.MobitikOrange
import com.example.ui.theme.MobitikSuccessGreen
import com.example.ui.theme.MobitikWarningAmber

@Composable
fun NetworkDiagnosticDialog(
    report: NetworkDiagnosticReport?,
    isRunning: Boolean,
    onRunDiagnostic: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MobitikOrange.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.NetworkCheck,
                            contentDescription = "Diagnostic",
                            tint = MobitikOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Network Diagnostic",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Anti-Cheat & VPN Detection",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isRunning) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                color = MobitikOrange,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Auditing network interfaces, TRANSPORT_VPN & proxy...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (report != null) {
                    // Overall Protocol Mining Status Banner
                    val isAllowed = report.isMiningAllowed
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAllowed) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (isAllowed) MobitikSuccessGreen else MobitikErrorRed,
                                shape = RoundedCornerShape(14.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isAllowed) Icons.Default.CheckCircle else Icons.Default.VpnKeyOff,
                                contentDescription = null,
                                tint = if (isAllowed) MobitikSuccessGreen else MobitikErrorRed,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = if (isAllowed) "MINING AUTHORIZED" else "MINING BLOCKED",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = if (isAllowed) Color(0xFF166534) else MobitikErrorRed
                                )
                                Text(
                                    text = if (isAllowed) "Connection verified clean. No VPN or Proxy."
                                    else "Active VPN or Proxy tunnel violates fair-mining policy.",
                                    fontSize = 11.sp,
                                    color = if (isAllowed) Color(0xFF15803D) else Color(0xFF991B1B)
                                )
                            }
                        }
                    }

                    // Diagnostic Telemetry Rows
                    DiagnosticMetricItem(
                        icon = Icons.Default.VpnKey,
                        title = "TRANSPORT_VPN Check",
                        value = if (report.hasTransportVpn) "DETECTED (Flagged)" else if (report.isVpnDetected) "TUNNEL ACTIVE" else "CLEAN (Direct)",
                        isError = report.isVpnDetected,
                        isWarning = false
                    )

                    DiagnosticMetricItem(
                        icon = Icons.Default.Security,
                        title = "Proxy Detection",
                        value = if (report.isProxyDetected) report.proxyDetails ?: "Proxy Active" else "NO PROXY (Direct)",
                        isError = report.isProxyDetected,
                        isWarning = false
                    )

                    DiagnosticMetricItem(
                        icon = Icons.Default.Wifi,
                        title = "Interface / Transport",
                        value = report.connectionType,
                        isError = report.connectionType.contains("VPN", ignoreCase = true),
                        isWarning = false
                    )

                    DiagnosticMetricItem(
                        icon = Icons.Default.Dns,
                        title = "Resolved IP Address",
                        value = report.ipAddress,
                        isError = false,
                        isWarning = false,
                        isMono = true
                    )

                    DiagnosticMetricItem(
                        icon = Icons.Default.Speed,
                        title = "Round-Trip Latency",
                        value = "${report.latencyMs} ms",
                        isError = false,
                        isWarning = report.latencyMs > 800
                    )

                    // Diagnostic Policy Note
                    Text(
                        text = report.diagnosticSummary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    Text(
                        text = "Run a live diagnostic scan to inspect your device's network capabilities, VPN tunnel status, and proxy configurations.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onRunDiagnostic,
                colors = ButtonDefaults.buttonColors(containerColor = MobitikOrange),
                shape = RoundedCornerShape(10.dp),
                enabled = !isRunning,
                modifier = Modifier.testTag("run_diagnostic_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (report == null) "Run Test" else "Re-Test Now", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFF64748B))
            }
        }
    )
}

@Composable
private fun DiagnosticMetricItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    isError: Boolean,
    isWarning: Boolean,
    isMono: Boolean = false
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isError -> Color(0xFFFFF1F2)
                isWarning -> Color(0xFFFFFBEB)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = when {
                        isError -> MobitikErrorRed
                        isWarning -> MobitikWarningAmber
                        else -> MobitikOrange
                    },
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = if (isMono) JetBrainsMonoFamily else null,
                color = when {
                    isError -> MobitikErrorRed
                    isWarning -> MobitikWarningAmber
                    else -> MobitikSuccessGreen
                }
            )
        }
    }
}

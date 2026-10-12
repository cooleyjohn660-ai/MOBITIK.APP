package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.AppNotification
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MobitikBlack
import com.example.ui.theme.MobitikCharcoal
import com.example.ui.theme.MobitikErrorRed
import com.example.ui.theme.MobitikGold
import com.example.ui.theme.MobitikOrange
import com.example.ui.theme.MobitikOrangeLight
import com.example.ui.theme.MobitikSuccessGreen
import com.example.ui.theme.MobitikSurfaceElevated
import com.example.ui.theme.MobitikTextSecondary
import com.example.ui.theme.MobitikWarningAmber
import com.example.ui.theme.MobitikWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CircularMiningReactor(
    isMiningActive: Boolean,
    isReadyToClaim: Boolean,
    formattedCountdown: String,
    progressFraction: Float,
    rewardAmount: Double,
    durationHours: Long,
    isBusy: Boolean,
    onStartMining: () -> Unit,
    onClaimReward: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "miningPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(276.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 14.dp.toPx()
                val glowStroke = 26.dp.toPx()
                val diameter = size.minDimension - glowStroke
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                val arcSize = Size(diameter, diameter)

                // Outer radial glow when active or ready
                if (isMiningActive || isReadyToClaim) {
                    val glowColor = if (isReadyToClaim) MobitikSuccessGreen else MobitikOrange
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glowColor.copy(alpha = pulseAlpha * 0.65f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension / 1.85f
                        ),
                        radius = size.minDimension / 1.85f,
                        center = center
                    )
                }

                // Track ring
                drawArc(
                    color = MobitikSurfaceElevated,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Active progress arc
                val sweep = when {
                    isReadyToClaim -> 360f
                    isMiningActive -> (progressFraction.coerceIn(0.03f, 1f)) * 360f
                    else -> 0f
                }

                if (sweep > 0f) {
                    val brush = if (isReadyToClaim) {
                        Brush.sweepGradient(listOf(MobitikSuccessGreen, MobitikGold, MobitikSuccessGreen))
                    } else {
                        Brush.sweepGradient(listOf(MobitikOrange, MobitikGold, MobitikOrangeLight, MobitikOrange))
                    }
                    drawArc(
                        brush = brush,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            // Inner Core Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_launcher_icon_1791779383277),
                    contentDescription = "MOBITIK Core Logo",
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.5.dp,
                            color = if (isReadyToClaim) MobitikSuccessGreen else MobitikOrange,
                            shape = CircleShape
                        ),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(8.dp))

                StatusPillBadge(
                    label = when {
                        isReadyToClaim -> "SESSION COMPLETE"
                        isMiningActive -> "MINING ACTIVE"
                        else -> "IDLE • ${durationHours}H SESSION"
                    },
                    color = when {
                        isReadyToClaim -> MobitikSuccessGreen
                        isMiningActive -> MobitikOrange
                        else -> MobitikTextSecondary
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = formattedCountdown,
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("mining_countdown_text")
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "+${"%.2f".format(rewardAmount)} MOBITIK / ${durationHours}h",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isReadyToClaim) MobitikSuccessGreen else MobitikOrangeLight
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (isReadyToClaim) {
                    onClaimReward()
                } else if (!isMiningActive) {
                    onStartMining()
                }
            },
            enabled = !isBusy && (!isMiningActive || isReadyToClaim),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = when {
                    isReadyToClaim -> MobitikSuccessGreen
                    isMiningActive -> MobitikSurfaceElevated
                    else -> MobitikOrange
                },
                contentColor = MobitikBlack,
                disabledContainerColor = MobitikSurfaceElevated,
                disabledContentColor = MobitikTextSecondary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("primary_mining_action_button")
        ) {
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MobitikOrange,
                    strokeWidth = 2.5.dp
                )
            } else {
                Icon(
                    imageVector = if (isReadyToClaim) Icons.Default.CheckCircle else Icons.Default.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        isReadyToClaim -> "Claim ${"%.2f".format(rewardAmount)} MOBITIK & Restart"
                        isMiningActive -> "12-Hour Server Mining in Progress..."
                        else -> "Start ${durationHours}-Hour Mining (+${"%.2f".format(rewardAmount)} MBTK)"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StatusPillBadge(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.16f),
        shape = RoundedCornerShape(50),
        modifier = modifier.border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(50))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun StatMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color = MobitikOrange,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Deterministic 21x21 QR-style visual matrix rendered in Compose Canvas from the user's unique wallet address.
 * Includes standard finder patterns on the 3 corners and deterministic bit encoding of the address payload.
 */
@Composable
fun WalletAddressQrCode(
    address: String,
    qrSize: Dp = 180.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(qrSize)
            .clip(RoundedCornerShape(16.dp))
            .background(MobitikWhite)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val grid = 21
            val cellSize = size.minDimension / grid.toFloat()
            val bytes = address.ifBlank { "MBTK-DEFAULT" }.toByteArray()

            fun isFinderPattern(r: Int, c: Int): Boolean {
                val topLeft = r in 0..6 && c in 0..6
                val topRight = r in 0..6 && c in (grid - 7) until grid
                val bottomLeft = r in (grid - 7) until grid && c in 0..6
                return topLeft || topRight || bottomLeft
            }

            fun drawFinderSquare(startR: Int, startC: Int) {
                for (r in 0..6) {
                    for (c in 0..6) {
                        val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                        val isCenter = r in 2..4 && c in 2..4
                        if (isBorder || isCenter) {
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset((startC + c) * cellSize, (startR + r) * cellSize),
                                size = Size(cellSize * 1.02f, cellSize * 1.02f)
                            )
                        }
                    }
                }
            }

            drawFinderSquare(0, 0)
            drawFinderSquare(0, grid - 7)
            drawFinderSquare(grid - 7, 0)

            var bitIndex = 0
            for (r in 0 until grid) {
                for (c in 0 until grid) {
                    if (isFinderPattern(r, c)) continue
                    // Quiet separator around finders
                    if ((r == 7 && (c <= 7 || c >= grid - 8)) ||
                        (c == 7 && (r <= 7 || r >= grid - 8)) ||
                        (r == grid - 8 && c <= 7) ||
                        (c == grid - 8 && r <= 7)
                    ) {
                        continue
                    }
                    val byteVal = bytes[bitIndex % bytes.size].toInt()
                    val shift = (r * 3 + c * 5 + bitIndex) % 7
                    val isDark = ((byteVal shr shift) and 1) == 1 || ((r + c + byteVal) % 3 == 0)
                    bitIndex++
                    if (isDark) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize * 1.02f, cellSize * 1.02f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsBottomSheet(
    notifications: List<AppNotification>,
    onMarkRead: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MobitikOrange
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Notifications (${notifications.count { !it.isRead }} unread)",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close notifications")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (notifications.isEmpty()) {
                Text(
                    text = "No notifications yet. Start mining or invite friends to see activity alerts here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 28.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                ) {
                    items(notifications, key = { it.id }) { item ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (item.isRead) {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                } else {
                                    MobitikOrange.copy(alpha = 0.14f)
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!item.isRead) onMarkRead(item.id)
                                }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StatusPillBadge(
                                        label = item.category,
                                        color = when (item.category) {
                                            "MINING" -> MobitikOrange
                                            "SWAP", "WITHDRAWAL" -> MobitikSuccessGreen
                                            "REFERRAL" -> MobitikGold
                                            "SECURITY" -> MobitikWarningAmber
                                            else -> MobitikOrangeLight
                                        }
                                    )
                                    Text(
                                        text = formatTimestamp(item.createdAt?.toDate()),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (item.isRead) FontWeight.Normal else FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

fun formatTimestamp(date: Date?): String {
    if (date == null) return "Just now"
    val sdf = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
    return sdf.format(date)
}

fun formatEpochMs(epochMs: Long): String {
    if (epochMs <= 0L) return "--"
    val sdf = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(epochMs))
}

@Composable
fun TermsOfServiceDialog(
    onAccept: () -> Unit,
    isFirstLaunch: Boolean = true,
    onDismissRequest: () -> Unit = {}
) {
    var agreedToTerms by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = {
            if (!isFirstLaunch) {
                onDismissRequest()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isFirstLaunch,
            dismissOnClickOutside = !isFirstLaunch,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .border(
                    width = 1.dp,
                    color = MobitikGold.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(24.dp)
                )
                .testTag("terms_of_service_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MobitikOrange.copy(alpha = 0.16f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Gavel,
                                    contentDescription = stringResource(R.string.tos_dialog_title),
                                    tint = MobitikGold,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.tos_dialog_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.tos_dialog_subtitle),
                                style = MaterialTheme.typography.labelMedium,
                                color = MobitikGold
                            )
                        }
                    }

                    if (!isFirstLaunch) {
                        IconButton(
                            onClick = onDismissRequest,
                            modifier = Modifier.testTag("tos_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Terms of Service"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable rules content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(310.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TermsRuleSectionCard(
                        icon = Icons.Default.Schedule,
                        iconTint = MobitikOrange,
                        title = stringResource(R.string.tos_section_cycle_title),
                        description = stringResource(R.string.tos_section_cycle_desc)
                    )

                    TermsRuleSectionCard(
                        icon = Icons.Default.Bolt,
                        iconTint = MobitikGold,
                        title = stringResource(R.string.tos_section_rewards_title),
                        description = stringResource(R.string.tos_section_rewards_desc)
                    )

                    TermsRuleSectionCard(
                        icon = Icons.Default.VerifiedUser,
                        iconTint = MobitikSuccessGreen,
                        title = stringResource(R.string.tos_section_swap_title),
                        description = stringResource(R.string.tos_section_swap_desc)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Agreement Checkbox Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { agreedToTerms = !agreedToTerms }
                        .padding(vertical = 4.dp)
                        .testTag("tos_agreement_row"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = agreedToTerms,
                        onCheckedChange = { agreedToTerms = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MobitikOrange,
                            checkmarkColor = MobitikBlack
                        ),
                        modifier = Modifier.testTag("tos_agreement_checkbox")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.tos_agreement_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onAccept,
                    enabled = agreedToTerms,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MobitikOrange,
                        contentColor = MobitikBlack,
                        disabledContainerColor = MobitikSurfaceElevated,
                        disabledContentColor = MobitikTextSecondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("tos_accept_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.tos_accept_button),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun TermsRuleSectionCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}


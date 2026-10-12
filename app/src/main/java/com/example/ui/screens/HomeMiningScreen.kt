package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AppConfig
import com.example.data.model.MiningSession
import com.example.data.model.UserProfile
import com.example.data.model.WalletTransaction
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MobitikBlack
import com.example.ui.theme.MobitikGold
import com.example.ui.theme.MobitikOrange
import com.example.ui.theme.MobitikSuccessGreen
import com.example.ui.viewmodel.MiningTimerUiState
import kotlinx.coroutines.delay

const val MOBITIK_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

@Composable
fun HomeMiningScreen(
    user: UserProfile,
    config: AppConfig,
    timerState: MiningTimerUiState,
    recentSessions: List<MiningSession>,
    recentTransactions: List<WalletTransaction>,
    unreadNotificationCount: Int,
    isBusy: Boolean,
    onStartMining: () -> Unit,
    onClaimReward: () -> Unit,
    onOpenNotifications: () -> Unit,
    onNavigateToWalletTab: (subTab: Int) -> Unit,
    onNavigateToReferrals: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToWhitepaper: () -> Unit
) {
    val context = LocalContext.current
    var pendingRewardedAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val triggerMiningWithAd = {
        if (timerState.isReadyToClaim) {
            pendingRewardedAction = onClaimReward
        } else if (!timerState.isMiningActive) {
            pendingRewardedAction = onStartMining
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Area (Matching Screenshot 1: Center Circle + Right Floating Icon Column)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MobitikOrange.copy(alpha = 0.92f),
                            Color(0xFFD84315),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // Center Pioneer Globe Circle
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(CircleShape)
                            .clickable(enabled = !isBusy && (!timerState.isMiningActive || timerState.isReadyToClaim)) {
                                triggerMiningWithAd()
                            }
                            .testTag("mining_primary_action_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.35f),
                                style = Stroke(width = 6.dp.toPx())
                            )
                            val sweep = if (timerState.isMiningActive) {
                                360f * timerState.progressFraction.coerceIn(0.05f, 1f)
                            } else {
                                360f
                            }
                            drawArc(
                                color = Color.White,
                                startAngle = -90f,
                                sweepAngle = sweep,
                                useCenter = false,
                                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = "Global Pioneers",
                                tint = Color.White,
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "55M+ Engaged\nPioneers",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 19.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when {
                                    timerState.isReadyToClaim -> "CLAIM +${config.miningReward} MBTK"
                                    timerState.isMiningActive -> timerState.formattedCountdown
                                    else -> "TAP TO MINE"
                                },
                                color = MobitikGold,
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Right Vertical Icon Column (Chat, User, Mining Rate ⚡, Share)
                    Column(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        RightSideIconItem(
                            icon = Icons.Default.ChatBubble,
                            label = "Wallet",
                            onClick = { onNavigateToWalletTab(0) }
                        )
                        RightSideIconItem(
                            icon = Icons.Default.Group,
                            label = "${user.totalReferrals} Team",
                            onClick = onNavigateToReferrals
                        )
                        // Mining Bolt Button (triggers Rewarded Ad + Mining)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable(enabled = !isBusy) {
                                    triggerMiningWithAd()
                                }
                                .testTag("mining_bolt_button")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (timerState.isMiningActive) MobitikSuccessGreen else Color.White,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = "Mine",
                                        tint = MobitikBlack,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${"%.2f".format(config.miningReward)}/12h",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        RightSideIconItem(
                            icon = Icons.Default.Share,
                            label = "Share",
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Join MOBITIK with my code: ${user.referralCode}"
                                    )
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share MOBITIK"))
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Pill Action Button below Circle (Like "Zai Day 2025" in Screenshot 1)
                Button(
                    onClick = {
                        if (!timerState.isMiningActive || timerState.isReadyToClaim) {
                            triggerMiningWithAd()
                        } else {
                            onNavigateToWalletTab(1) // Open Swap
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = MobitikOrange
                    ),
                    modifier = Modifier
                        .height(44.dp)
                        .padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = when {
                            timerState.isReadyToClaim -> "Claim +${config.miningReward} MBTK"
                            timerState.isMiningActive -> "Mining Active • ${timerState.formattedCountdown}"
                            else -> "Start 12h Mining"
                        },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Short @MobitikCoreTeam note
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = "@MobitikCoreTeam",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Mine ${config.miningReward} MBTK every 12h. Swap to USDT at 1 MBTK = ${config.swapRateUsdt} USDT or withdraw from $${"%.2f".format(config.minWithdrawalUsdt)}.",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Pioneer Posts Visual Card (Matching bottom of Screenshot 1)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Pioneer Posts",
                        color = MobitikGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MobitikOrange,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "M",
                                                color = MobitikBlack,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "@${user.username}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "${"%.2f".format(user.usdtBalance)} USDT",
                                    color = MobitikGold,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Image(
                                painter = painterResource(id = R.drawable.img_mobitik_hero_1791779396147),
                                contentDescription = "MOBITIK Network",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }
                }
            }
        }
    }

    if (pendingRewardedAction != null) {
        RewardedMiningAdDialog(
            adUnitId = MOBITIK_REWARDED_AD_UNIT_ID,
            onAdRewardEarned = {
                val action = pendingRewardedAction
                pendingRewardedAction = null
                action?.invoke()
            },
            onDismiss = {
                pendingRewardedAction = null
            }
        )
    }
}

@Composable
private fun RightSideIconItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun RewardedMiningAdDialog(
    adUnitId: String,
    onAdRewardEarned: () -> Unit,
    onDismiss: () -> Unit
) {
    var remainingSeconds by remember { mutableIntStateOf(3) }

    LaunchedEffect(Unit) {
        while (remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds -= 1
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (remainingSeconds == 0) onDismiss()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (remainingSeconds == 0) Icons.Default.CheckCircle else Icons.Default.PlayCircleFilled,
                    contentDescription = null,
                    tint = if (remainingSeconds == 0) MobitikSuccessGreen else MobitikOrange
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (remainingSeconds == 0) "Reward Ready" else "Ad Playing (${remainingSeconds}s)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rewarded_ad_dialog")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .background(MobitikBlack, RoundedCornerShape(14.dp))
                        .border(1.dp, MobitikOrange.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "REWARDED VIDEO AD",
                            style = MaterialTheme.typography.titleSmall,
                            color = MobitikOrange,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = adUnitId,
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = MobitikGold
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { (3 - remainingSeconds) / 3f },
                    color = MobitikOrange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAdRewardEarned,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (remainingSeconds == 0) MobitikSuccessGreen else MobitikOrange,
                    contentColor = MobitikBlack
                ),
                modifier = Modifier.testTag("rewarded_ad_confirm_button")
            ) {
                Text(
                    text = if (remainingSeconds == 0) "Start Mining" else "Skip & Mine",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

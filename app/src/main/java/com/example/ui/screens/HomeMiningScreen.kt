package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.util.Log
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import android.widget.Toast
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.NetworkCheck
import com.example.data.remote.NetworkDiagnosticReport
import com.example.ui.components.NetworkDiagnosticDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextDecoration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import com.example.data.remote.AdManager
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.net.Uri
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.OutlinedButton
import com.example.R
import com.example.data.model.AppConfig
import com.example.data.model.MiningSession
import com.example.data.model.MobitikTask
import com.example.data.model.UserProfile
import com.example.data.model.WalletTransaction
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MobitikBlack
import com.example.ui.theme.MobitikCharcoal
import com.example.ui.theme.MobitikGold
import com.example.ui.theme.MobitikOrange
import com.example.ui.theme.MobitikSuccessGreen
import com.example.ui.theme.MobitikErrorRed
import com.example.ui.viewmodel.MiningTimerUiState
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.delay

// Real AdMob Interstitial Ad Unit from User's AdMob Console
const val MOBITIK_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-7750958842878506/5000750002"
// Standard Google AdMob Test Ad Units (Guaranteed fill for testing / emulator / unverified ad serving)
const val MOBITIK_TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

private fun Context.findActivity(): Activity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

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
    onNavigateToWhitepaper: () -> Unit,
    onRewardedAdBoostClaimed: () -> Unit = {},
    tasks: List<MobitikTask> = emptyList(),
    completedTaskIds: Set<String> = emptySet(),
    onClaimTaskReward: (MobitikTask) -> Unit = {},
    networkDiagnosticReport: NetworkDiagnosticReport? = null,
    isDiagnosticRunning: Boolean = false,
    onRunDiagnostic: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isAdReady by AdManager.isAdReady.collectAsState()
    val isAdLoading by AdManager.isAdLoading.collectAsState()
    val adLoadError by AdManager.loadError.collectAsState()
    var pendingRewardedAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingRewardLabel by remember { mutableStateOf("+0.22 MBTK") }
    var showDiagnosticDialog by remember { mutableStateOf(false) }

    val isMiningCycleIdle = !timerState.isMiningActive && !timerState.isReadyToClaim

    LaunchedEffect(Unit) {
        // Pre-load interstitial ad immediately so the mining button is enabled as soon as the ad is ready
        AdManager.preloadAd(context)
    }

    val showAdAndExecute: (action: () -> Unit, rewardLabel: String) -> Unit = { targetAction, rewardLabel ->
        val activity = context.findActivity() ?: AdManager.findActivity(context)
        if (activity != null && isAdReady) {
            AdManager.showAd(
                activity = activity,
                onAdDismissed = {
                    targetAction.invoke()
                },
                onAdFailed = { errMsg ->
                    Log.w("AdMob", "Ad failed to display: $errMsg")
                    targetAction.invoke()
                }
            )
        } else {
            AdManager.preloadAd(context)
            if (isAdLoading) {
                Toast.makeText(context, "Ad is loading. Please wait a moment until it's ready!", Toast.LENGTH_SHORT).show()
            } else {
                pendingRewardLabel = rewardLabel
                pendingRewardedAction = targetAction
            }
        }
    }

    val triggerMiningWithAd: () -> Unit = {
        if (!timerState.isMiningActive || timerState.isReadyToClaim) {
            if (timerState.isReadyToClaim) {
                onClaimReward.invoke()
            } else {
                if (isAdReady) {
                    showAdAndExecute(onStartMining, "Watch Full Ad to Start 12h Mining (+${config.miningReward} MBTK)")
                } else {
                    AdManager.preloadAd(context)
                    if (isAdLoading) {
                        Toast.makeText(context, "Ad is loading. Mining button will unlock automatically once loaded!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Connecting to ad network... retrying now.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Area (Center Circle + Right Floating Icon Column)
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
                    val isGlobeClickable = !isBusy && when {
                        timerState.isReadyToClaim -> true
                        timerState.isMiningActive -> false
                        else -> isAdReady
                    }

                    // Center Pioneer Globe Circle
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(CircleShape)
                            .clickable(enabled = isGlobeClickable) {
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
                                    isAdReady -> "AD READY • TAP TO MINE"
                                    isAdLoading -> "LOADING AD..."
                                    else -> "WAITING FOR AD..."
                                },
                                color = if (isAdReady || timerState.isReadyToClaim) MobitikGold else Color.White.copy(alpha = 0.85f),
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Right Vertical Icon Column (Wallet, Team, Mining Rate ⚡, Share)
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
                        // Mining Bolt Indicator
                        val isBoltClickable = !isBusy && when {
                            timerState.isReadyToClaim -> true
                            timerState.isMiningActive -> false
                            else -> isAdReady
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable(enabled = isBoltClickable) {
                                    triggerMiningWithAd()
                                }
                                .testTag("mining_bolt_button")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = when {
                                    timerState.isMiningActive -> MobitikSuccessGreen
                                    isAdReady -> Color.White
                                    else -> Color.White.copy(alpha = 0.45f)
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = "Mine",
                                        tint = if (isAdReady || timerState.isMiningActive) MobitikBlack else MobitikCharcoal.copy(alpha = 0.4f),
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

                // Pill Action Button below Circle — starts mining on full ad watch
                val isPillEnabled = !isBusy && when {
                    timerState.isReadyToClaim -> true
                    timerState.isMiningActive -> false
                    else -> isAdReady
                }

                Button(
                    onClick = {
                        triggerMiningWithAd()
                    },
                    enabled = isPillEnabled,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPillEnabled) Color.White else Color.White.copy(alpha = 0.6f),
                        contentColor = MobitikOrange,
                        disabledContainerColor = Color.White.copy(alpha = 0.5f),
                        disabledContentColor = MobitikCharcoal.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .height(46.dp)
                        .padding(horizontal = 20.dp)
                        .testTag("mining_pill_action_button")
                ) {
                    if (isMiningCycleIdle && !isAdReady) {
                        if (isAdLoading) {
                            CircularProgressIndicator(
                                color = MobitikOrange,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Loading Ad...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MobitikCharcoal.copy(alpha = 0.7f)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = MobitikCharcoal.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Waiting for Ad...",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MobitikCharcoal.copy(alpha = 0.7f)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = when {
                                timerState.isMiningActive -> Icons.Default.Bolt
                                timerState.isReadyToClaim -> Icons.Default.CheckCircle
                                isAdReady -> Icons.Default.PlayCircleFilled
                                else -> Icons.Default.HourglassTop
                            },
                            contentDescription = null,
                            tint = when {
                                timerState.isMiningActive -> MobitikSuccessGreen
                                timerState.isReadyToClaim -> MobitikGold
                                isAdReady -> MobitikOrange
                                else -> MobitikCharcoal.copy(alpha = 0.6f)
                            },
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                timerState.isReadyToClaim -> "Claim +${config.miningReward} MBTK"
                                timerState.isMiningActive -> "Mining Active (${timerState.formattedCountdown})"
                                isAdReady -> "Watch Ad to Start Mining"
                                else -> "Waiting for Ad..."
                            },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                    }
                }

                if (isMiningCycleIdle) {
                    Spacer(modifier = Modifier.height(6.dp))
                    if (!isAdReady) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    AdManager.preloadAd(context)
                                    Toast.makeText(context, "Reloading ad...", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry",
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isAdLoading) "Ad is loading, please wait • Tap to refresh" else "Ad not ready • Tap here to reload ad",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                textDecoration = TextDecoration.Underline
                            )
                        }
                    } else {
                        Text(
                            text = "✓ Ad Loaded • Click above to start 12h mining session",
                            color = MobitikGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )
                    }
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
                        text = "Watch the full sponsored ad to start your 12-hour mining session. Mine ${config.miningReward} MBTK per session.",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Network Diagnostic Status Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = 0.28f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .clickable {
                            if (networkDiagnosticReport == null && !isDiagnosticRunning) {
                                onRunDiagnostic()
                            }
                            showDiagnosticDialog = true
                        }
                        .testTag("network_diagnostic_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = CircleShape,
                                color = if (networkDiagnosticReport?.isMiningAllowed == false) MobitikErrorRed.copy(alpha = 0.2f)
                                else MobitikSuccessGreen.copy(alpha = 0.2f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.NetworkCheck,
                                        contentDescription = null,
                                        tint = if (networkDiagnosticReport?.isMiningAllowed == false) MobitikErrorRed else MobitikSuccessGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Network & Anti-Cheat Diagnostic",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = if (isDiagnosticRunning) "Auditing TRANSPORT_VPN & proxy..."
                                    else if (networkDiagnosticReport == null) "Tap to inspect TRANSPORT_VPN & proxy status"
                                    else if (networkDiagnosticReport.isMiningAllowed) "✓ Clean Direct Connection (${networkDiagnosticReport.latencyMs}ms)"
                                    else "⛔ VPN/Proxy Detected • Mining Suspended",
                                    color = if (networkDiagnosticReport?.isMiningAllowed == false) MobitikErrorRed else Color.White.copy(alpha = 0.85f),
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Text(
                            text = "Audit ➔",
                            color = MobitikGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                if (showDiagnosticDialog) {
                    NetworkDiagnosticDialog(
                        report = networkDiagnosticReport,
                        isRunning = isDiagnosticRunning,
                        onRunDiagnostic = onRunDiagnostic,
                        onDismiss = { showDiagnosticDialog = false }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                CommunityTasksSection(
                    tasks = tasks,
                    completedTaskIds = completedTaskIds,
                    onClaimTaskReward = onClaimTaskReward,
                    isBusy = isBusy,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Pioneer Posts Visual Card
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
                            containerColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
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
            adUnitId = MOBITIK_INTERSTITIAL_AD_UNIT_ID,
            rewardLabel = pendingRewardLabel,
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
    rewardLabel: String,
    onAdRewardEarned: () -> Unit,
    onDismiss: () -> Unit
) {
    val totalSeconds = 5
    var remainingSeconds by remember { mutableIntStateOf(totalSeconds) }

    LaunchedEffect(Unit) {
        while (remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds -= 1
        }
    }

    Dialog(
        onDismissRequest = {
            if (remainingSeconds == 0) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = remainingSeconds == 0,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 460.dp)
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(22.dp))
                .testTag("rewarded_ad_dialog"),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top AdMob Test Ad Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MobitikGold,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Ad",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Google AdMob Sponsored",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = if (remainingSeconds == 0) MobitikSuccessGreen.copy(alpha = 0.15f) else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.border(
                                1.dp,
                                if (remainingSeconds == 0) MobitikSuccessGreen else Color(0xFFCBD5E1),
                                RoundedCornerShape(50)
                            )
                        ) {
                            Text(
                                text = if (remainingSeconds == 0) "Reward Granted ✓" else "Reward in " + remainingSeconds + "s",
                                color = if (remainingSeconds == 0) MobitikSuccessGreen else MobitikOrange,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        if (remainingSeconds == 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = onAdRewardEarned,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Ad",
                                    tint = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }

                // Video Stage
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_mobitik_hero_1791779396147),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alpha = 0.2f,
                        modifier = Modifier.fillMaxSize()
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (remainingSeconds == 0) MobitikSuccessGreen else MobitikOrange,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (remainingSeconds == 0) Icons.Default.CheckCircle else Icons.Default.PlayCircleFilled,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (remainingSeconds == 0) "AD COMPLETED — REWARD UNLOCKED" else "PLAYING REWARDED VIDEO AD...",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = rewardLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MobitikOrange,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Ad Unit: " + adUnitId,
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                // Video Progress Bar
                LinearProgressIndicator(
                    progress = { (totalSeconds - remainingSeconds) / totalSeconds.toFloat() },
                    color = if (remainingSeconds == 0) MobitikSuccessGreen else MobitikOrange,
                    trackColor = Color(0xFFE2E8F0),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(0.38f)
                    ) {
                        Text("Close", color = Color(0xFF64748B))
                    }

                    Button(
                        onClick = onAdRewardEarned,
                        enabled = remainingSeconds == 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MobitikSuccessGreen,
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFE2E8F0),
                            disabledContentColor = Color(0xFF94A3B8)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(0.62f)
                            .height(48.dp)
                            .testTag("rewarded_ad_confirm_button")
                    ) {
                        Text(
                            text = if (remainingSeconds == 0) "Start 12h Mining Now" else "Watch Ad (" + remainingSeconds + "s)",
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityTasksSection(
    tasks: List<MobitikTask>,
    completedTaskIds: Set<String>,
    onClaimTaskReward: (MobitikTask) -> Unit,
    isBusy: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
            .testTag("community_tasks_section")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MobitikOrange.copy(alpha = 0.15f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Assignment,
                                contentDescription = null,
                                tint = MobitikOrange,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Community & Social Tasks",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Earn 1.00 MBTK per task",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MobitikGold.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${completedTaskIds.size}/${tasks.size} Done",
                        color = MobitikBlack,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (tasks.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No active tasks at the moment. Official tasks added by Admin will appear here.",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                tasks.forEach { task ->
                    val isCompleted = completedTaskIds.contains(task.id)
                    TaskItemCard(
                        task = task,
                        isCompleted = isCompleted,
                        isBusy = isBusy,
                        onOpenUrl = {
                            if (task.actionUrl.isNotBlank()) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(task.actionUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open link: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onClaim = { onClaimTaskReward(task) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskItemCard(
    task: MobitikTask,
    isCompleted: Boolean,
    isBusy: Boolean,
    onOpenUrl: () -> Unit,
    onClaim: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) Color(0xFFF8FAFC) else Color(0xFFFAFAFA)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isCompleted) Color(0xFFE2E8F0) else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(14.dp)
            )
            .testTag("task_item_${task.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val platformColor = when (task.platform.uppercase()) {
                        "YOUTUBE" -> Color(0xFFFF0000)
                        "TIKTOK" -> Color(0xFF000000)
                        "TELEGRAM" -> Color(0xFF0088CC)
                        "TWITTER" -> Color(0xFF1DA1F2)
                        else -> MobitikOrange
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = platformColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = task.platform.uppercase(),
                            color = platformColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = task.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF0F172A),
                        maxLines = 1
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isCompleted) Color(0xFFDCFCE7) else MobitikGold.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = if (isCompleted) "✓ 1 MBTK" else "+1.00 MBTK",
                        color = if (isCompleted) Color(0xFF15803D) else Color(0xFFB45309),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            if (task.description.isNotBlank()) {
                Text(
                    text = task.description,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Color(0xFF64748B)
                )
            }

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isCompleted) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFDCFCE7)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Completed (+1 MBTK Claimed)",
                                color = Color(0xFF15803D),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    if (task.actionUrl.isNotBlank()) {
                        OutlinedButton(
                            onClick = onOpenUrl,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open Link", fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Button(
                        onClick = onClaim,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MobitikOrange,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Claim 1 MBTK", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


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
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.AppConfig
import com.example.data.model.MiningSession
import com.example.data.model.UserProfile
import com.example.data.model.WalletTransaction
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MobitikBlack
import com.example.ui.theme.MobitikCharcoal
import com.example.ui.theme.MobitikGold
import com.example.ui.theme.MobitikOrange
import com.example.ui.theme.MobitikSuccessGreen
import com.example.ui.viewmodel.MiningTimerUiState
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import kotlinx.coroutines.delay

// Real AdMob Rewarded Interstitial Ad Unit from User's AdMob Console
const val MOBITIK_REWARDED_AD_UNIT_ID = "ca-app-pub-7750958842878506/4746452763"
const val MOBITIK_TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5354046379"

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
    onNavigateToWhitepaper: () -> Unit
) {
    val context = LocalContext.current
    var loadedRewardedInterstitialAd by remember { mutableStateOf<RewardedInterstitialAd?>(null) }
    var loadedRewardedAd by remember { mutableStateOf<RewardedAd?>(null) }
    var isAdLoading by remember { mutableStateOf(false) }
    var pendingRewardedAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingRewardLabel by remember { mutableStateOf("+0.22 MBTK") }

    fun loadAdMobAds() {
        if (isAdLoading || loadedRewardedInterstitialAd != null || loadedRewardedAd != null) return
        isAdLoading = true
        val adRequest = AdRequest.Builder().build()

        // 1. Load Rewarded Interstitial Ad (primary real ad unit)
        RewardedInterstitialAd.load(
            context,
            MOBITIK_REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    isAdLoading = false
                    loadedRewardedInterstitialAd = ad
                    Log.d("AdMob", "Real RewardedInterstitialAd loaded: $MOBITIK_REWARDED_AD_UNIT_ID")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.w("AdMob", "Real RewardedInterstitialAd failed: ${loadAdError.message}. Trying RewardedAd format...")
                    // 2. Fallback to RewardedAd format with real ID or test ID
                    RewardedAd.load(
                        context,
                        MOBITIK_REWARDED_AD_UNIT_ID,
                        adRequest,
                        object : RewardedAdLoadCallback() {
                            override fun onAdLoaded(ad: RewardedAd) {
                                isAdLoading = false
                                loadedRewardedAd = ad
                                Log.d("AdMob", "RewardedAd loaded: $MOBITIK_REWARDED_AD_UNIT_ID")
                            }

                            override fun onAdFailedToLoad(err: LoadAdError) {
                                Log.w("AdMob", "RewardedAd also failed: ${err.message}. Loading test fallback...")
                                // 3. Fallback to test unit for seamless local testing
                                RewardedAd.load(
                                    context,
                                    MOBITIK_TEST_REWARDED_AD_UNIT_ID,
                                    adRequest,
                                    object : RewardedAdLoadCallback() {
                                        override fun onAdLoaded(testAd: RewardedAd) {
                                            isAdLoading = false
                                            loadedRewardedAd = testAd
                                        }

                                        override fun onAdFailedToLoad(testErr: LoadAdError) {
                                            isAdLoading = false
                                        }
                                    }
                                )
                            }
                        }
                    )
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        loadAdMobAds()
    }

    val triggerMiningWithAd: () -> Unit = {
        if (!timerState.isMiningActive || timerState.isReadyToClaim) {
            val targetAction: () -> Unit
            val rewardLabel: String
            if (timerState.isReadyToClaim) {
                targetAction = onClaimReward
                rewardLabel = "Claim Mining Reward (+${config.miningReward} MBTK)"
            } else {
                targetAction = onStartMining
                rewardLabel = "Watch Full Ad to Start 12h Mining (+${config.miningReward} MBTK)"
            }

            val activity = context.findActivity()
            val interstitial = loadedRewardedInterstitialAd
            val rewarded = loadedRewardedAd

            if (interstitial != null && activity != null) {
                var rewardEarned = false
                interstitial.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        loadedRewardedInterstitialAd = null
                        loadAdMobAds()
                        if (rewardEarned) {
                            targetAction.invoke()
                        }
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        loadedRewardedInterstitialAd = null
                        loadAdMobAds()
                        pendingRewardLabel = rewardLabel
                        pendingRewardedAction = targetAction
                    }
                }
                interstitial.show(activity) {
                    rewardEarned = true
                }
            } else if (rewarded != null && activity != null) {
                var rewardEarned = false
                rewarded.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        loadedRewardedAd = null
                        loadAdMobAds()
                        if (rewardEarned) {
                            targetAction.invoke()
                        }
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        loadedRewardedAd = null
                        loadAdMobAds()
                        pendingRewardLabel = rewardLabel
                        pendingRewardedAction = targetAction
                    }
                }
                rewarded.show(activity) {
                    rewardEarned = true
                }
            } else {
                loadAdMobAds()
                pendingRewardLabel = rewardLabel
                pendingRewardedAction = targetAction
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
                    // Center Pioneer Globe Circle
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(CircleShape)
                            .clickable(enabled = !isBusy && !timerState.isMiningActive) {
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
                                    else -> "WATCH AD TO MINE"
                                },
                                color = MobitikGold,
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
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable(enabled = !isBusy && !timerState.isMiningActive) {
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

                // Pill Action Button below Circle — starts mining on full ad watch
                Button(
                    onClick = {
                        triggerMiningWithAd()
                    },
                    enabled = !isBusy && (!timerState.isMiningActive || timerState.isReadyToClaim),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = MobitikOrange
                    ),
                    modifier = Modifier
                        .height(44.dp)
                        .padding(horizontal = 24.dp)
                        .testTag("mining_pill_action_button")
                ) {
                    Icon(
                        imageVector = if (timerState.isMiningActive) Icons.Default.Bolt else Icons.Default.PlayCircleFilled,
                        contentDescription = null,
                        tint = if (timerState.isMiningActive) MobitikSuccessGreen else MobitikOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            timerState.isReadyToClaim -> "Claim +${config.miningReward} MBTK"
                            timerState.isMiningActive -> "Mining Active (${timerState.formattedCountdown})"
                            else -> "Watch Full Ad to Start Mining"
                        },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 6.dp)
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
                        text = "Watch the full sponsored ad to start your 12-hour mining session. Mine ${config.miningReward} MBTK per session.",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }

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
            adUnitId = MOBITIK_REWARDED_AD_UNIT_ID,
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
                                text = "Test Ad",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Google AdMob Rewarded",
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

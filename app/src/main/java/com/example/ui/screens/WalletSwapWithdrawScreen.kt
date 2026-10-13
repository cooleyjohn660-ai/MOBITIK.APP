package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Outbox
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppConfig
import com.example.data.model.OnChainWalletBalance
import com.example.data.model.UsdtWithdrawal
import com.example.data.model.UserProfile
import com.example.data.model.WalletTransaction
import com.example.ui.components.StatusPillBadge
import android.graphics.Bitmap
import com.example.ui.screens.KycVerificationDialog
import com.example.ui.components.WalletAddressQrCode
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MobitikBlack
import com.example.ui.theme.MobitikErrorRed
import com.example.ui.theme.MobitikGold
import com.example.ui.theme.MobitikOrange
import com.example.ui.theme.MobitikSuccessGreen
import com.example.ui.theme.MobitikUsdtTeal
import com.example.ui.theme.MobitikWarningAmber
import java.util.UUID

private enum class WalletQuickModal {
    NONE, RECEIVE, SEND, SWAP, WITHDRAW, SETTING
}

@Composable
fun WalletSwapWithdrawScreen(
    user: UserProfile,
    config: AppConfig,
    transactions: List<WalletTransaction>,
    withdrawals: List<UsdtWithdrawal>,
    onChainBalance: OnChainWalletBalance?,
    initialSubTab: Int,
    isBusy: Boolean,
    onSendMobitik: (recipient: String, amount: Double, note: String, pin: String, onSuccess: (String) -> Unit) -> Unit,
    onSwapMbtkToUsdt: (mbtkAmount: Double, pin: String, onSuccess: (String) -> Unit) -> Unit,
    onConnectTrustWallet: (bep20Address: String, network: String) -> Unit,
    onDisconnectTrustWallet: () -> Unit,
    onRefreshOnChainBalance: () -> Unit,
    onRequestWithdrawal: (dest: String, network: String, amountUsdt: Double, pin: String, idempotencyKey: String, onSuccess: (UsdtWithdrawal) -> Unit) -> Unit,
    onCancelWithdrawal: (UsdtWithdrawal) -> Unit,
    onShowMessage: (String) -> Unit,
    onSubmitKyc: (String, String, String, Bitmap, (Boolean, String) -> Unit) -> Unit = { _, _, _, _, _ -> }
) {
    val clipboard = LocalClipboardManager.current
    var hideBalance by remember { mutableStateOf(false) }
    var selectedBottomTab by remember { mutableIntStateOf(0) } // 0 = History, 1 = Assets, 2 = Migrated
    var activeModal by remember { mutableStateOf(WalletQuickModal.NONE) }
    var showAllTransactions by remember { mutableStateOf(false) }
    var showKycDialog by remember { mutableStateOf(false) }

    LaunchedEffect(initialSubTab) {
        when (initialSubTab) {
            1 -> activeModal = WalletQuickModal.SWAP
            2 -> activeModal = WalletQuickModal.WITHDRAW
        }
    }

    val shortAddress = remember(user.walletAddress) {
        if (user.walletAddress.length > 11) {
            "${user.walletAddress.take(5)}...${user.walletAddress.takeLast(5)}"
        } else {
            user.walletAddress
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
        ) {
            // 1. Clean Top Wallet Bar (Matches Screenshot 3: "Wallet 🪙" on left, "GD765...266CO 📋" on right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Wallet",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = MobitikGold,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "M",
                                color = MobitikBlack,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier
                            .clickable {
                                clipboard.setText(AnnotatedString(user.walletAddress))
                                onShowMessage("Address copied!")
                            }
                            .testTag("copy_wallet_address_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = shortAddress,
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Address",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            onRefreshOnChainBalance()
                            onShowMessage("Balances refreshed")
                        },
                        enabled = !isBusy,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("refresh_wallet_balances_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MobitikOrange,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Light Balance Card (Crisp Modern Light Theme)
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
                    .testTag("wallet_dashboard_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mobitik Balance",
                            color = Color(0xFF64748B),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        IconButton(
                            onClick = { hideBalance = !hideBalance },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (hideBalance) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Balance",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (hideBalance) "•••• MBTK" else "${"%.2f".format(user.availableBalance)} MBTK",
                            color = Color(0xFF0F172A),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.testTag("dashboard_mbtk_balance_box")
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (hideBalance) {
                                "•••• USDT"
                            } else {
                                "${"%.2f".format(user.usdtBalance)} USDT  •  ≈ $${"%.2f".format(user.availableBalance * config.swapRateUsdt + user.usdtBalance)}"
                            },
                            color = MobitikOrange,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("dashboard_usdt_balance_box")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                            .clickable { selectedBottomTab = 2 }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "1 MBTK = ${"%.2f".format(config.swapRateUsdt)} USDT",
                                color = Color(0xFF64748B),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Details",
                                    color = Color(0xFF334155),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Circular Action Buttons Row (Receive, Send, Swap, Withdraw, Setting - Matches Screenshot 3)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularWalletActionItem(
                    icon = Icons.Default.ArrowDownward,
                    label = "Receive",
                    isSelected = activeModal == WalletQuickModal.RECEIVE,
                    tag = "action_receive_button",
                    onClick = {
                        activeModal = if (activeModal == WalletQuickModal.RECEIVE) WalletQuickModal.NONE else WalletQuickModal.RECEIVE
                    }
                )
                CircularWalletActionItem(
                    icon = Icons.Default.ArrowOutward,
                    label = "Send",
                    isSelected = activeModal == WalletQuickModal.SEND,
                    tag = "wallet_subtab_0",
                    onClick = {
                        activeModal = if (activeModal == WalletQuickModal.SEND) WalletQuickModal.NONE else WalletQuickModal.SEND
                    }
                )
                CircularWalletActionItem(
                    icon = Icons.Default.SwapHoriz,
                    label = "Swap",
                    isSelected = activeModal == WalletQuickModal.SWAP,
                    tag = "wallet_subtab_1",
                    onClick = {
                        activeModal = if (activeModal == WalletQuickModal.SWAP) WalletQuickModal.NONE else WalletQuickModal.SWAP
                    }
                )
                CircularWalletActionItem(
                    icon = Icons.Default.Outbox,
                    label = "Withdraw",
                    isSelected = activeModal == WalletQuickModal.WITHDRAW,
                    tag = "wallet_subtab_2",
                    onClick = {
                        activeModal = if (activeModal == WalletQuickModal.WITHDRAW) WalletQuickModal.NONE else WalletQuickModal.WITHDRAW
                    }
                )
                CircularWalletActionItem(
                    icon = Icons.Default.Tune,
                    label = "Setting",
                    isSelected = activeModal == WalletQuickModal.SETTING,
                    tag = "action_setting_button",
                    onClick = {
                        activeModal = if (activeModal == WalletQuickModal.SETTING) WalletQuickModal.NONE else WalletQuickModal.SETTING
                    }
                )
            }

            // Expandable Clean Action Panel when Receive / Send / Swap / Withdraw / Setting is tapped
            if (activeModal != WalletQuickModal.NONE) {
                Spacer(modifier = Modifier.height(16.dp))
                when (activeModal) {
                    WalletQuickModal.RECEIVE -> CompactReceivePanel(
                        user = user,
                        onCopy = {
                            clipboard.setText(AnnotatedString(user.walletAddress))
                            onShowMessage("Address copied!")
                        },
                        onClose = { activeModal = WalletQuickModal.NONE }
                    )

                    WalletQuickModal.SEND -> CompactSendPanel(
                        user = user,
                        isBusy = isBusy,
                        onSendMobitik = onSendMobitik,
                        onRequestWithdrawal = onRequestWithdrawal,
                        onClose = { activeModal = WalletQuickModal.NONE }
                    )

                    WalletQuickModal.SWAP -> CompactSwapPanel(
                        user = user,
                        config = config,
                        isBusy = isBusy,
                        onSwapMbtkToUsdt = onSwapMbtkToUsdt,
                        onClose = { activeModal = WalletQuickModal.NONE }
                    )

                    WalletQuickModal.WITHDRAW -> CompactWithdrawPanel(
                        user = user,
                        config = config,
                        isBusy = isBusy,
                        onRequestWithdrawal = onRequestWithdrawal,
                        onOpenKycDialog = { showKycDialog = true },
                        onClose = { activeModal = WalletQuickModal.NONE }
                    )

                    WalletQuickModal.SETTING -> CompactWalletSettingPanel(
                        user = user,
                        onChainBalance = onChainBalance,
                        isBusy = isBusy,
                        onConnectTrustWallet = onConnectTrustWallet,
                        onDisconnectTrustWallet = onDisconnectTrustWallet,
                        onRefreshOnChainBalance = onRefreshOnChainBalance,
                        onClose = { activeModal = WalletQuickModal.NONE }
                    )

                    WalletQuickModal.NONE -> Unit
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Clean 3-Tab Row: History | Assets | Migrated (Exact match to Screenshot 3)
            val tabs = listOf("History", "Assets", "Migrated")
            TabRow(
                selectedTabIndex = selectedBottomTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
                indicator = { tabPositions ->
                    if (selectedBottomTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedBottomTab]),
                            color = MobitikOrange,
                            height = 3.dp
                        )
                    }
                },
                divider = {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedBottomTab == index,
                        onClick = { selectedBottomTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedBottomTab == index) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedBottomTab == index) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (selectedBottomTab) {
                0 -> CleanHistorySection(
                    transactions = transactions,
                    withdrawals = withdrawals,
                    showAll = showAllTransactions,
                    onToggleShowAll = { showAllTransactions = !showAllTransactions },
                    onCancelWithdrawal = onCancelWithdrawal
                )

                1 -> CleanAssetsSection(
                    user = user,
                    config = config,
                    onChainBalance = onChainBalance,
                    onOpenSwap = { activeModal = WalletQuickModal.SWAP },
                    onOpenWithdraw = { activeModal = WalletQuickModal.WITHDRAW },
                    onOpenSetting = { activeModal = WalletQuickModal.SETTING }
                )

                2 -> CleanMigratedSection(
                    user = user,
                    config = config
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showKycDialog) {
        KycVerificationDialog(
            user = user,
            isBusy = isBusy,
            onSubmitKyc = onSubmitKyc,
            onDismiss = { showKycDialog = false }
        )
    }
}

@Composable
private fun CircularWalletActionItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp)
            .testTag(tag)
    ) {
        Surface(
            shape = CircleShape,
            color = if (isSelected) MobitikOrange else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSelected) MobitikBlack else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) MobitikOrange else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun CleanHistorySection(
    transactions: List<WalletTransaction>,
    withdrawals: List<UsdtWithdrawal>,
    showAll: Boolean,
    onToggleShowAll: () -> Unit,
    onCancelWithdrawal: (UsdtWithdrawal) -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Transactions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        TextButton(onClick = onToggleShowAll) {
            Text(
                text = if (showAll) "Show less" else "See all",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }

    val pendingWithdrawals = withdrawals.filter { it.status == "PENDING" }
    val displayTxs = if (showAll) transactions else transactions.take(6)

    if (pendingWithdrawals.isEmpty() && displayTxs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Nothing To Show!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            pendingWithdrawals.forEach { wd ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Withdraw ${"%.2f".format(wd.amountUsdt)} USDT",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                StatusPillBadge(label = wd.status, color = MobitikWarningAmber)
                            }
                            Text(
                                text = wd.destinationAddress,
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(onClick = { onCancelWithdrawal(wd) }) {
                            Text("Cancel", color = MobitikErrorRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            displayTxs.forEach { tx ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tx.type,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatTimestamp(tx.createdAt?.toDate()),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (tx.txHash.startsWith("0x") && tx.txHash.length == 66) {
                                Text(
                                    text = "BscScan ↗",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MobitikOrange,
                                    modifier = Modifier.clickable {
                                        context.startActivity(
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse("https://bscscan.com/tx/${tx.txHash}")
                                            )
                                        )
                                    }
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${"%.2f".format(tx.amount)} ${tx.tokenSymbol}",
                                fontWeight = FontWeight.Bold,
                                color = if (tx.tokenSymbol == "USDT") MobitikUsdtTeal else MobitikGold
                            )
                            StatusPillBadge(
                                label = tx.status,
                                color = if (tx.status == "CONFIRMED" || tx.status == "COMPLETED") MobitikSuccessGreen else MobitikWarningAmber
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CleanAssetsSection(
    user: UserProfile,
    config: AppConfig,
    onChainBalance: OnChainWalletBalance?,
    onOpenSwap: () -> Unit,
    onOpenWithdraw: () -> Unit,
    onOpenSetting: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // MBTK Row
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Mobitik (MBTK)", fontWeight = FontWeight.Bold)
                    Text(
                        text = "${"%.4f".format(user.availableBalance)} MBTK • ≈ $${"%.2f".format(user.availableBalance * config.swapRateUsdt)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onOpenSwap,
                    colors = ButtonDefaults.buttonColors(containerColor = MobitikOrange, contentColor = MobitikBlack),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Swap", fontWeight = FontWeight.Bold)
                }
            }
        }

        // USDT Row
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Tether (USDT)", fontWeight = FontWeight.Bold)
                    Text(
                        text = "${"%.2f".format(user.usdtBalance)} USDT",
                        style = MaterialTheme.typography.labelMedium,
                        color = MobitikUsdtTeal
                    )
                }
                Button(
                    onClick = onOpenWithdraw,
                    colors = ButtonDefaults.buttonColors(containerColor = MobitikUsdtTeal, contentColor = MobitikBlack),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Withdraw", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Trust Wallet Row
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Trust Wallet (BEP-20)", fontWeight = FontWeight.Bold)
                    Text(
                        text = if (user.externalWalletAddress.isNotBlank()) {
                            if (onChainBalance != null && onChainBalance.isReachable) {
                                "On-Chain: ${"%.2f".format(onChainBalance.usdtBep20Balance)} USDT"
                            } else {
                                user.externalWalletAddress
                            }
                        } else {
                            "Not connected"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(
                    onClick = onOpenSetting,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (user.externalWalletAddress.isNotBlank()) "Manage" else "Connect")
                }
            }
        }
    }
}

@Composable
private fun CleanMigratedSection(
    user: UserProfile,
    config: AppConfig
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CompactMetricRow("Available Balance", "${"%.4f".format(user.availableBalance)} MBTK")
            CompactMetricRow("USDT Balance", "${"%.2f".format(user.usdtBalance)} USDT")
            CompactMetricRow("Pending Mining", "${"%.4f".format(user.pendingRewards)} MBTK")
            CompactMetricRow("Referral Bonus", "${"%.4f".format(user.referralRewards)} MBTK")
            CompactMetricRow("Reference Rate", "1 MBTK = ${"%.2f".format(config.swapRateUsdt)} USDT")
        }
    }
}

@Composable
private fun CompactMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CompactReceivePanel(
    user: UserProfile,
    onCopy: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MobitikOrange.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Receive", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onClose) { Text("Close") }
            }
            WalletAddressQrCode(address = user.walletAddress, qrSize = 160.dp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = user.walletAddress,
                fontFamily = JetBrainsMonoFamily,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onCopy,
                    colors = ButtonDefaults.buttonColors(containerColor = MobitikOrange, contentColor = MobitikBlack)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, user.walletAddress)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Address"))
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share")
                }
            }
        }
    }
}

@Composable
private fun CompactSendPanel(
    user: UserProfile,
    isBusy: Boolean,
    onSendMobitik: (String, Double, String, String, (String) -> Unit) -> Unit,
    onRequestWithdrawal: (String, String, Double, String, String, (UsdtWithdrawal) -> Unit) -> Unit,
    onClose: () -> Unit
) {
    var recipient by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    val amount = amountStr.toDoubleOrNull() ?: 0.0
    val trimmedRecipient = recipient.trim()
    val isMbtkFormat = trimmedRecipient.uppercase().startsWith("MBTK-") && trimmedRecipient.length >= 8
    val isUsernameFormat = trimmedRecipient.removePrefix("@").matches(Regex("^[a-zA-Z0-9_]{3,30}$"))
    val validRecipient = isMbtkFormat || isUsernameFormat

    val hasSufficientBalance = amount > 0.0 && amount <= user.availableBalance

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Send MOBITIK (App-to-App Instant)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Text(
                        text = "Instant transfer to any pioneer via @username or MBTK- address",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                }
                TextButton(onClick = onClose) { Text("Close") }
            }

            OutlinedTextField(
                value = recipient,
                onValueChange = { recipient = it.trim() },
                label = { Text("Recipient (MBTK-... or @username)") },
                placeholder = { Text("MBTK-XXXX-XXXX or @alice") },
                supportingText = {
                    if (recipient.isNotBlank() && !validRecipient) {
                        Text("Enter a valid MBTK- address or pioneer @username", color = MobitikErrorRed)
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("send_recipient_input")
            )

            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = { Text("Amount (MBTK) • Available: " + "%.4f".format(user.availableBalance) + " MBTK") },
                trailingIcon = {
                    TextButton(onClick = {
                        amountStr = "%.4f".format(user.availableBalance)
                    }) {
                        Text("MAX", color = MobitikOrange, fontWeight = FontWeight.Bold)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("send_amount_input")
            )

            if (user.pinHash.isNotBlank()) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6) pin = it.filter { c -> c.isDigit() } },
                    label = { Text("Security PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = {
                    val formattedRecipient = if (trimmedRecipient.startsWith("@")) {
                        trimmedRecipient.lowercase()
                    } else if (trimmedRecipient.uppercase().startsWith("MBTK-")) {
                        trimmedRecipient.uppercase()
                    } else {
                        trimmedRecipient.lowercase()
                    }

                    onSendMobitik(formattedRecipient, amount, "App-to-App Instant MBTK Transfer", pin) {
                        recipient = ""
                        amountStr = ""
                        pin = ""
                        onClose()
                    }
                },
                enabled = !isBusy && validRecipient && hasSufficientBalance,
                colors = ButtonDefaults.buttonColors(containerColor = MobitikOrange, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("send_mbtk_submit_button")
            ) {
                Text("Send MOBITIK Instantly", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CompactSwapPanel(
    user: UserProfile,
    config: AppConfig,
    isBusy: Boolean,
    onSwapMbtkToUsdt: (Double, String, (String) -> Unit) -> Unit,
    onClose: () -> Unit
) {
    var mbtkStr by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var showConfirm by remember { mutableStateOf(false) }

    val mbtkAmount = mbtkStr.toDoubleOrNull() ?: 0.0
    val grossUsdt = mbtkAmount * config.swapRateUsdt
    val feeUsdt = grossUsdt * (config.swapFeePercent / 100.0)
    val netUsdt = (grossUsdt - feeUsdt).coerceAtLeast(0.0)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MobitikOrange.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Swap MBTK → USDT", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "1 MBTK = ${"%.2f".format(config.swapRateUsdt)} USDT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MobitikGold
                    )
                }
                TextButton(onClick = onClose) { Text("Close") }
            }

            // Quick Preset Chips: 1, 10, 100, 1000 MBTK
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(1, 10, 100, 1000).forEach { amt ->
                    FilterChip(
                        selected = mbtkAmount == amt.toDouble(),
                        onClick = { mbtkStr = amt.toString() },
                        label = { Text("$amt", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            OutlinedTextField(
                value = mbtkStr,
                onValueChange = { mbtkStr = it },
                label = { Text("MBTK Amount (Max ${"%.2f".format(user.availableBalance)})") },
                trailingIcon = {
                    TextButton(onClick = { mbtkStr = "%.4f".format(user.availableBalance) }) {
                        Text("MAX", color = MobitikOrange, fontWeight = FontWeight.Bold)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("swap_mbtk_amount_input")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Receive USDT", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "${"%.4f".format(netUsdt)} USDT",
                    fontWeight = FontWeight.ExtraBold,
                    color = MobitikSuccessGreen
                )
            }

            Button(
                onClick = { showConfirm = true },
                enabled = !isBusy && config.swapEnabled && mbtkAmount > 0.0 && mbtkAmount <= user.availableBalance,
                colors = ButtonDefaults.buttonColors(containerColor = MobitikOrange, contentColor = MobitikBlack),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("swap_mbtk_button")
            ) {
                Icon(Icons.Default.CurrencyExchange, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Swap to USDT", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Confirm Swap") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Swap ${"%.4f".format(mbtkAmount)} MBTK → ${"%.4f".format(netUsdt)} USDT")
                    if (user.pinHash.isNotBlank()) {
                        OutlinedTextField(
                            value = pin,
                            onValueChange = { if (it.length <= 6) pin = it.filter { c -> c.isDigit() } },
                            label = { Text("Security PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirm = false
                        onSwapMbtkToUsdt(mbtkAmount, pin) {
                            mbtkStr = ""
                            pin = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MobitikSuccessGreen, contentColor = MobitikBlack),
                    modifier = Modifier.testTag("confirm_swap_dialog_button")
                ) {
                    Text("Confirm", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CompactWithdrawPanel(
    user: UserProfile,
    config: AppConfig,
    isBusy: Boolean,
    onRequestWithdrawal: (String, String, Double, String, String, (UsdtWithdrawal) -> Unit) -> Unit,
    onOpenKycDialog: () -> Unit,
    onClose: () -> Unit
) {
    var address by remember { mutableStateOf(user.externalWalletAddress) }
    var amountStr by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var showConfirm by remember { mutableStateOf(false) }

    val amount = amountStr.toDoubleOrNull() ?: 0.0
    val totalFee = config.withdrawalNetworkFeeUsdt + config.withdrawalPlatformFeeUsdt
    val finalUsdt = (amount - totalFee).coerceAtLeast(0.0)
    val isValidDest = address.trim().matches(Regex("^0x[a-fA-F0-9]{40}$"))
    val isKycApproved = user.kycStatus == "APPROVED"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Withdraw USDT (Admin Approval Required)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Text(
                        text = "BNB Smart Chain (BEP-20) • Min $" + "%.2f".format(config.minWithdrawalUsdt) + " • Fee $" + "%.2f".format(totalFee),
                        style = MaterialTheme.typography.labelSmall,
                        color = MobitikUsdtTeal
                    )
                }
                TextButton(onClick = onClose) { Text("Close") }
            }

            // KYC Status Banner
            if (!isKycApproved) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Face KYC Verification Required",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E),
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (user.kycStatus) {
                                "PENDING" -> "Your face scan and identity are currently under Admin review. Withdrawals will unlock once approved."
                                "REJECTED" -> "Your previous KYC submission was rejected: " + user.kycRejectionReason + ". Please resubmit."
                                else -> "To withdraw USDT, complete your 1-person-1-account biometric face scan and legal identity verification."
                            },
                            fontSize = 12.sp,
                            color = Color(0xFF78350F),
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (user.kycStatus != "PENDING") {
                            Button(
                                onClick = onOpenKycDialog,
                                colors = ButtonDefaults.buttonColors(containerColor = MobitikOrange, contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Verify Face KYC Now", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFDE68A),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "⏳ KYC Pending Admin Approval",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF92400E),
                                    modifier = Modifier.padding(8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFECFDF5),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MobitikSuccessGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("KYC Verified Pioneer (1 Person 1 Account Approved)", color = Color(0xFF065F46), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }

            OutlinedTextField(
                value = address,
                onValueChange = { address = it.trim() },
                enabled = isKycApproved,
                label = { Text("BEP-20 Wallet Address (0x...)") },
                placeholder = { Text("0x...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("withdraw_destination_input")
            )

            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = it },
                enabled = isKycApproved,
                label = { Text("USDT Amount • Available: " + "%.2f".format(user.usdtBalance) + " USDT") },
                trailingIcon = {
                    TextButton(onClick = { amountStr = "%.2f".format(user.usdtBalance) }, enabled = isKycApproved) {
                        Text("MAX", color = MobitikUsdtTeal, fontWeight = FontWeight.Bold)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("withdraw_usdt_amount_input")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Net Payout (Fee $" + "%.2f".format(totalFee) + ")", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                Text(
                    text = "%.2f".format(finalUsdt) + " USDT",
                    fontWeight = FontWeight.ExtraBold,
                    color = MobitikSuccessGreen
                )
            }

            Button(
                onClick = { showConfirm = true },
                enabled = isKycApproved && !isBusy && isValidDest && amount >= config.minWithdrawalUsdt && amount <= user.usdtBalance && finalUsdt > 0.0,
                colors = ButtonDefaults.buttonColors(containerColor = MobitikUsdtTeal, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("request_usdt_withdrawal_button")
            ) {
                Text("Submit USDT Withdrawal for Approval", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Confirm USDT Withdrawal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Amount: " + "%.2f".format(amount) + " USDT (Fee: " + "%.2f".format(totalFee) + " USDT)")
                    Text("Receive: " + "%.2f".format(finalUsdt) + " USDT")
                    Text("To: " + address.trim(), fontFamily = JetBrainsMonoFamily, style = MaterialTheme.typography.labelSmall)
                    Text("Note: USDT withdrawals require manual treasury approval in Admin section before blockchain transfer.", fontSize = 11.sp, color = Color(0xFF64748B))
                    if (user.pinHash.isNotBlank()) {
                        OutlinedTextField(
                            value = pin,
                            onValueChange = { if (it.length <= 6) pin = it.filter { c -> c.isDigit() } },
                            label = { Text("Security PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val key = UUID.randomUUID().toString().take(16)
                        onRequestWithdrawal(
                            address.trim(),
                            "BNB Smart Chain (BEP-20)",
                            amount,
                            pin,
                            key
                        ) {
                            showConfirm = false
                            onClose()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MobitikSuccessGreen, contentColor = Color.White),
                    modifier = Modifier.testTag("confirm_withdrawal_dialog_button")
                ) {
                    Text("Confirm & Request Approval")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CompactWalletSettingPanel(
    user: UserProfile,
    onChainBalance: OnChainWalletBalance?,
    isBusy: Boolean,
    onConnectTrustWallet: (bep20Address: String, network: String) -> Unit,
    onDisconnectTrustWallet: () -> Unit,
    onRefreshOnChainBalance: () -> Unit,
    onClose: () -> Unit
) {
    var inputAddress by remember(user.externalWalletAddress) { mutableStateOf(user.externalWalletAddress) }
    val isConnected = user.externalWalletAddress.isNotBlank()
    val isValidAddress = remember(inputAddress) {
        inputAddress.trim().startsWith("0x") && inputAddress.trim().length == 42
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Web3 & Trust Wallet Settings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF0F172A)
                )
                TextButton(onClick = onClose) {
                    Text("Close", color = Color(0xFF64748B))
                }
            }

            if (isConnected) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MobitikSuccessGreen.copy(alpha = 0.15f),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MobitikSuccessGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Connected to ${user.externalWalletNetwork}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF065F46)
                            )
                        }

                        Text(
                            text = user.externalWalletAddress,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 12.sp,
                            color = Color(0xFF334155),
                            lineHeight = 16.sp
                        )

                        if (onChainBalance != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "On-Chain: " + "%.4f".format(onChainBalance.bnbBalance) + " BNB • " + "%.2f".format(onChainBalance.usdtBep20Balance) + " USDT",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A)
                                )
                                IconButton(
                                    onClick = onRefreshOnChainBalance,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "Refresh On-Chain Balance",
                                        tint = MobitikOrange,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onDisconnectTrustWallet,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MobitikErrorRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Disconnect External Wallet", fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    text = "Link your external BEP-20 wallet (Trust Wallet, MetaMask, or BitKeep) to verify on-chain balances and receive future token airdrops.",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 16.sp
                )

                OutlinedTextField(
                    value = inputAddress,
                    onValueChange = { inputAddress = it.trim() },
                    label = { Text("BEP-20 Address (0x...)") },
                    placeholder = { Text("0x1234...abcd") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        onConnectTrustWallet(inputAddress.trim(), "BNB Smart Chain (BEP-20)")
                    },
                    enabled = !isBusy && isValidAddress,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MobitikOrange, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Connect BEP-20 Wallet", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


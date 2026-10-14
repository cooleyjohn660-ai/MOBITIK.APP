package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.AppConfig
import com.example.data.model.AuditLog
import com.example.data.model.FaqEntry
import com.example.data.model.SupportTicket
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.asImageBitmap
import com.example.data.model.KycSubmission
import com.example.data.model.UsdtWithdrawal
import com.example.data.model.UserProfile
import com.example.data.remote.BscWeb3Client
import com.example.data.repository.MobitikRepository
import com.example.ui.components.StatMetricCard
import com.example.ui.components.StatusPillBadge
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MobitikBlack
import com.example.ui.theme.MobitikErrorRed
import com.example.ui.theme.MobitikGold
import com.example.ui.theme.MobitikOrange
import com.example.ui.theme.MobitikSuccessGreen
import com.example.ui.theme.MobitikUsdtTeal
import com.example.ui.theme.MobitikWarningAmber
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileSettingsScreen(
    user: UserProfile,
    config: AppConfig,
    faqs: List<FaqEntry>,
    supportTickets: List<SupportTicket>,
    isAdminUser: Boolean,
    isBusy: Boolean,
    initialSection: Int = 0,
    onOpenAdminConsole: () -> Unit,
    onSavePreferences: (
        name: String,
        autoStartMining: Boolean,
        notificationsEnabled: Boolean,
        themeMode: String,
        language: String,
        twoFactorEnabled: Boolean,
        newPinCode: String
    ) -> Unit,
    onSubmitTicket: (subject: String, message: String, onSuccess: (String) -> Unit) -> Unit,
    onSignOut: () -> Unit,
    onShowMessage: (String) -> Unit,
    onDeleteAccount: ((Boolean, String) -> Unit) -> Unit = { _ -> }
) {
    val clipboard = LocalClipboardManager.current
    var selectedSection by rememberSaveable(initialSection) { mutableIntStateOf(initialSection.coerceIn(0, 4)) }
    val sections = listOf("Settings", "Whitepaper", "FAQ", "Support", "Legal & Privacy")

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
                .widthIn(max = 600.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Identity Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(MobitikOrange.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user.name.take(1).uppercase(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MobitikOrange,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(user.name, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    text = "@${user.username}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MobitikOrange
                                )
                                Text(
                                    text = user.email,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        StatusPillBadge(
                            label = user.accountStatus,
                            color = if (user.accountStatus == "ACTIVE") MobitikSuccessGreen else MobitikErrorRed
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Internal MOBITIK Wallet Address", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = user.walletAddress,
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        IconButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(user.walletAddress))
                                onShowMessage("Wallet address copied!")
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy wallet address", tint = MobitikOrange)
                        }
                    }

                    Text(
                        text = "UID: ${user.userId} • Joined: ${formatTimestamp(user.createdAt?.toDate())}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isAdminUser && MobitikRepository.isAuthorizedAdminEmail(user.email)) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onOpenAdminConsole,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MobitikGold,
                                contentColor = MobitikBlack
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_admin_console_button")
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Admin Dashboard (${MobitikRepository.ADMIN_EMAIL})", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Section Selector Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedSection,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MobitikOrange,
                edgePadding = 0.dp
            ) {
                sections.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedSection == index,
                        onClick = { selectedSection = index },
                        text = { Text(title) },
                        modifier = Modifier.testTag("profile_tab_${title.lowercase().take(5)}")
                    )
                }
            }

            when (selectedSection) {
                0 -> AccountSecuritySettingsSubTab(
                    user = user,
                    isBusy = isBusy,
                    onSavePreferences = onSavePreferences,
                    onSignOut = onSignOut,
                    onDeleteAccount = onDeleteAccount
                )
                1 -> WhitepaperSubTab(config = config)
                2 -> FaqSubTab(faqs = faqs)
                3 -> SupportContactSubTab(
                    config = config,
                    supportTickets = supportTickets,
                    isBusy = isBusy,
                    onSubmitTicket = onSubmitTicket
                )
                4 -> LegalPrivacySubTab()
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountSecuritySettingsSubTab(
    user: UserProfile,
    isBusy: Boolean,
    onSavePreferences: (
        name: String,
        autoStartMining: Boolean,
        notificationsEnabled: Boolean,
        themeMode: String,
        language: String,
        twoFactorEnabled: Boolean,
        newPinCode: String
    ) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: ((Boolean, String) -> Unit) -> Unit = { _ -> }
) {
    var displayName by remember(user.name) { mutableStateOf(user.name) }
    var autoStartMining by remember(user.autoStartMining) { mutableStateOf(user.autoStartMining) }
    var notificationsEnabled by remember(user.notificationsEnabled) { mutableStateOf(user.notificationsEnabled) }
    var themeMode by remember(user.themeMode) { mutableStateOf(user.themeMode) }
    var language by remember(user.language) { mutableStateOf(user.language) }
    var twoFactorEnabled by remember(user.twoFactorEnabled) { mutableStateOf(user.twoFactorEnabled) }
    var newPinCode by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = MobitikOrange)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Account, Theme & Security", style = MaterialTheme.typography.titleLarge)
            }

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = newPinCode,
                onValueChange = { if (it.length <= 6) newPinCode = it.filter { c -> c.isDigit() } },
                label = {
                    Text(
                        if (user.pinHash.isNotBlank()) "Change Security PIN (Leave blank to keep current)"
                        else "Set 4-6 Digit Transaction Security PIN"
                    )
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Two-Factor Authentication (2FA)", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Extra verification for swaps and external withdrawals",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = twoFactorEnabled, onCheckedChange = { twoFactorEnabled = it })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("12-Hour Mining Alerts & Reminders", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Notify when your 0.22 MOBITIK session is ready to claim",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-Start Next Session Preference", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Protocol requires manual claim after each 12-hour session",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = autoStartMining,
                    onCheckedChange = { autoStartMining = it }
                )
            }

            Text("Theme Appearance", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("DARK", "LIGHT", "SYSTEM").forEach { mode ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { themeMode = mode },
                        label = { Text(mode) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MobitikOrange.copy(alpha = 0.22f),
                            selectedLabelColor = MobitikOrange
                        )
                    )
                }
            }

            Text("Language Preference", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("en" to "English", "es" to "Español", "fr" to "Français").forEach { (code, label) ->
                    FilterChip(
                        selected = language.equals(code, ignoreCase = true),
                        onClick = { language = code },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MobitikOrange.copy(alpha = 0.22f),
                            selectedLabelColor = MobitikOrange
                        )
                    )
                }
            }

            Button(
                onClick = {
                    onSavePreferences(
                        displayName,
                        autoStartMining,
                        notificationsEnabled,
                        themeMode,
                        language,
                        twoFactorEnabled,
                        newPinCode
                    )
                    newPinCode = ""
                },
                enabled = !isBusy && displayName.trim().length >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = MobitikOrange, contentColor = MobitikBlack),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_profile_settings_button")
            ) {
                Text("Save Profile & Security Settings", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out of MOBITIK")
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Google Play Policy Compliant Account & Data Deletion
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = MobitikErrorRed
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Delete Account & Data",
                            fontWeight = FontWeight.Bold,
                            color = MobitikErrorRed,
                            fontSize = 15.sp
                        )
                    }
                    Text(
                        text = "Permanently erase your MOBITIK account, identity credentials, KYC biometric data, and internal ledger records from all servers. This action cannot be undone.",
                        fontSize = 12.sp,
                        color = Color(0xFF7F1D1D),
                        lineHeight = 16.sp
                    )
                    Button(
                        onClick = { showDeleteConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MobitikErrorRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_account_open_dialog_button")
                    ) {
                        Text("Delete My Account & All Data", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        var confirmText by remember { mutableStateOf("") }
        var isDeleting by remember { mutableStateOf(false) }
        var deleteError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteConfirmDialog = false },
            icon = {
                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MobitikErrorRed)
            },
            title = {
                Text("Confirm Account Deletion", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Are you sure you want to permanently delete your MOBITIK account? All your token balances, mining session history, KYC submissions, and credentials will be deleted immediately and cannot be recovered.",
                        fontSize = 13.sp
                    )
                    Text(
                        "Type DELETE below to confirm:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = MobitikErrorRed
                    )
                    OutlinedTextField(
                        value = confirmText,
                        onValueChange = { confirmText = it.trim().uppercase() },
                        placeholder = { Text("DELETE") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_account_confirmation_input")
                    )
                    if (deleteError != null) {
                        Text(deleteError!!, color = MobitikErrorRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true
                        deleteError = null
                        onDeleteAccount { success, msg ->
                            isDeleting = false
                            if (success) {
                                showDeleteConfirmDialog = false
                            } else {
                                deleteError = msg
                            }
                        }
                    },
                    enabled = confirmText == "DELETE" && !isDeleting,
                    colors = ButtonDefaults.buttonColors(containerColor = MobitikErrorRed, contentColor = Color.White),
                    modifier = Modifier.testTag("confirm_delete_account_final_button")
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Deleting...")
                    } else {
                        Text("Permanently Delete", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false },
                    enabled = !isDeleting
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun WhitepaperSubTab(config: AppConfig) {
    val totalWithdrawalFee = config.withdrawalNetworkFeeUsdt + config.withdrawalPlatformFeeUsdt
    val sections = listOf(
        "1. Introduction & Vision" to config.whitepaperSummary,
        "2. What is MOBITIK (MBTK)?" to "MOBITIK is the native ecosystem unit of account within the MOBITIK platform. Every registered pioneer receives a unique internal MOBITIK wallet address (MBTK-...) and can optionally link an external BNB Smart Chain (BEP-20) Trust Wallet.",
        "3. 12-Hour Cloud Mining Mechanism" to "Mining sessions run for exactly ${config.miningDurationHours} hours on the cloud ledger. When a pioneer taps Start Mining, the server records the start timestamp and expected completion timestamp. After ${config.miningDurationHours} hours, the pioneer claims +${"%.2f".format(config.miningReward)} MOBITIK and manually initiates the next session.",
        "4. Reward Distribution & Referral Program" to "Pioneers earn ${"%.2f".format(config.miningReward)} MOBITIK per completed 12-hour session. Inviting active pioneers via a unique referral code rewards +${"%.2f".format(config.referralReward)} MOBITIK once the invited pioneer completes at least ${config.referralEligibilitySessions} mining session(s). Anti-abuse rules prevent self-referrals and duplicate accounts.",
        "5. Wallet, Internal Ledger & MBTK-to-USDT Swap" to "The MOBITIK Wallet supports instant peer-to-peer MBTK transfers (fee: ${"%.2f".format(config.networkFeeMobitik)} MBTK) and a fixed reference swap where 1 MBTK = ${"%.2f".format(config.swapRateUsdt)} USDT (10 MBTK = ${"%.2f".format(10 * config.swapRateUsdt)} USDT, 100 MBTK = ${"%.2f".format(100 * config.swapRateUsdt)} USDT, 1,000 MBTK = ${"%.2f".format(1000 * config.swapRateUsdt)} USDT). Swaps are executed strictly against the authorized USDT Liquidity Pool.",
        "6. External BEP-20 USDT Withdrawals & Security" to "Verified USDT balances can be withdrawn to external BEP-20 compatible wallets or exchanges (min ${"%.2f".format(config.minWithdrawalUsdt)} USDT, total fee ${"%.2f".format(totalWithdrawalFee)} USDT). All withdrawals undergo idempotency checks, PIN verification, and treasury settlement.",
        "7. Roadmap & Risk Disclaimer" to "Phase 1: 12-Hour Cloud Mining & Pioneer Growth.\nPhase 2: Peer-to-Peer Internal Wallet & Referral Leaderboard.\nPhase 3: Trust Wallet BEP-20 Verification & Liquidity Pool Swaps.\nPhase 4: Decentralized Smart Contract Governance.\n\nRisk Disclaimer: MOBITIK tokens and reference swap prices do not constitute investment advice or guaranteed financial returns."
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MobitikOrange.copy(alpha = 0.14f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Description, contentDescription = null, tint = MobitikOrange)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("MOBITIK Official Whitepaper v2.4", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Tokenomics, 12-Hour Cloud Mining Protocol & Liquidity Architecture",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        sections.forEach { (title, body) ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = MobitikOrange)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FaqSubTab(faqs: List<FaqEntry>) {
    var expandedId by remember { mutableStateOf<String?>(faqs.firstOrNull()?.id) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Frequently Asked Questions", style = MaterialTheme.typography.titleLarge)
        faqs.forEach { faq ->
            val isExpanded = expandedId == faq.id
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedId = if (isExpanded) null else faq.id }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = null,
                                tint = MobitikOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(faq.question, style = MaterialTheme.typography.titleMedium)
                        }
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle answer"
                        )
                    }
                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = faq.answer,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SupportContactSubTab(
    config: AppConfig,
    supportTickets: List<SupportTicket>,
    isBusy: Boolean,
    onSubmitTicket: (subject: String, message: String, onSuccess: (String) -> Unit) -> Unit
) {
    var subject by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SupportAgent, contentDescription = null, tint = MobitikOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Contact MOBITIK Support Desk", style = MaterialTheme.typography.titleLarge)
                }

                Text(
                    text = "Official Support Email: ${config.supportEmail}\nCommunity Channel: t.me/MobitikOfficial\nExpected Response Time: Under 12 hours",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject (e.g. Mining Session, USDT Withdrawal, Wallet)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("support_subject_input")
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Describe your question or issue in detail") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("support_message_input")
                )

                Button(
                    onClick = {
                        onSubmitTicket(subject, message) {
                            subject = ""
                            message = ""
                        }
                    },
                    enabled = !isBusy && subject.trim().length >= 3 && message.trim().length >= 5,
                    colors = ButtonDefaults.buttonColors(containerColor = MobitikOrange, contentColor = MobitikBlack),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("submit_support_ticket_button")
                ) {
                    Text("Submit Support Ticket", fontWeight = FontWeight.Bold)
                }
            }
        }

        Text("Your Support Tickets (${supportTickets.size})", style = MaterialTheme.typography.titleLarge)
        if (supportTickets.isEmpty()) {
            Text(
                text = "You have not submitted any support tickets.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            supportTickets.forEach { t ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(t.subject, style = MaterialTheme.typography.titleMedium)
                            StatusPillBadge(
                                label = t.status,
                                color = if (t.status == "RESOLVED") MobitikSuccessGreen else MobitikOrange
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(t.message, style = MaterialTheme.typography.bodyMedium)
                        if (t.adminReply.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MobitikSuccessGreen.copy(alpha = 0.12f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        "Official Admin Reply:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MobitikSuccessGreen
                                    )
                                    Text(t.adminReply, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegalPrivacySubTab() {
    val legalCards = listOf(
        "Privacy Policy & Data Collection" to "MOBITIK collects your account profile (name, username, Google-verified email), internal wallet address, 12-hour mining session timestamps, referral records, and transaction ledger entries. We use this data exclusively to operate the MOBITIK mining ecosystem, prevent duplicate account abuse, and process authorized swaps and withdrawals.",
        "Password, PIN & Wallet Security" to "MOBITIK uses Google Identity Services for primary authentication and SHA-256 cryptographic hashing for transaction security PINs. We never store plain-text passwords and never request your external Trust Wallet private keys or 12-word recovery seed phrases.",
        "Cookie, Session & Retention Policy" to "Authenticated sessions are managed via encrypted Firebase Auth tokens with automatic refresh and instant sign-out revocation. Ledger transactions and security audit logs are retained to preserve balance integrity.",
        "Terms & Conditions & Risk Disclosure" to "1. One account per human user.\n2. 12-hour mining rewards require manual claim upon completion.\n3. The 1 MBTK = 0.15 USDT swap rate is a configured reference rate; actual swaps require available USDT liquidity in the protocol reserve.\n4. Digital assets carry inherent volatility and regulatory risks; MOBITIK makes no promise of guaranteed financial profit."
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        legalCards.forEach { (title, content) ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Gavel, contentDescription = null, tint = MobitikOrange, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(title, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun AdminConsoleScreen(
    config: AppConfig,
    users: List<UserProfile>,
    withdrawals: List<UsdtWithdrawal>,
    kycSubmissions: List<KycSubmission> = emptyList(),
    tickets: List<SupportTicket>,
    auditLogs: List<AuditLog>,
    isBusy: Boolean,
    onBack: () -> Unit,
    onSaveConfig: (AppConfig, String) -> Unit,
    onReviewWithdrawal: (UsdtWithdrawal, String, String, String) -> Unit,
    onToggleUserStatus: (UserProfile, String, String) -> Unit,
    onReplyTicket: (SupportTicket, String, String) -> Unit,
    onReviewKyc: (KycSubmission, String, String) -> Unit = { _, _, _ -> }
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val pendingWdCount = withdrawals.count { it.status == "PENDING" }
    val totalMbtkCirculation = remember(users) { users.sumOf { it.totalBalance } }
    val totalUserUsdt = remember(users) { users.sumOf { it.usdtBalance } }
    val pendingWdUsdtVolume = remember(withdrawals) {
        withdrawals.filter { it.status == "PENDING" }.sumOf { it.amountUsdt }
    }

    val pendingKycCount = kycSubmissions.count { it.status == "PENDING" }
    val tabs = listOf(
        "USDT Withdrawals ($pendingWdCount)",
        "KYC Approvals ($pendingKycCount)",
        "Swap & Fees",
        "Users (${users.size})",
        "Tickets (${tickets.count { it.status == "OPEN" }})",
        "Audit Logs (${auditLogs.size})"
    )

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
                .widthIn(max = 650.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Switch to User App")
                    }
                    Column {
                        Text("Admin Dashboard (USDT Payouts)", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Auto-Logged In: ${MobitikRepository.ADMIN_EMAIL}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MobitikGold
                        )
                    }
                }
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("admin_switch_user_app_button")
                ) {
                    Text("User App")
                }
            }

            // Admin Overview Dashboard Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatMetricCard(
                    title = "Pending USDT",
                    value = "$pendingWdCount Reqs",
                    subtitle = "${"%.2f".format(pendingWdUsdtVolume)} USDT",
                    icon = Icons.Default.VerifiedUser,
                    accentColor = if (pendingWdCount > 0) MobitikWarningAmber else MobitikSuccessGreen,
                    modifier = Modifier.weight(1f)
                )
                StatMetricCard(
                    title = "Liquidity Pool",
                    value = "${"%.2f".format(config.liquidityPoolUsdt)} USDT",
                    subtitle = "User USDT: ${"%.2f".format(totalUserUsdt)}",
                    icon = Icons.Default.CurrencyExchange,
                    accentColor = MobitikUsdtTeal,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatMetricCard(
                    title = "Total Users",
                    value = "${users.size}",
                    subtitle = "${users.count { it.accountStatus == "ACTIVE" }} Active",
                    icon = Icons.Default.GroupAdd,
                    accentColor = MobitikOrange,
                    modifier = Modifier.weight(1f)
                )
                StatMetricCard(
                    title = "MBTK Mined",
                    value = "%.2f".format(totalMbtkCirculation),
                    subtitle = "Internal Ledger",
                    icon = Icons.Default.AccountBalanceWallet,
                    accentColor = MobitikGold,
                    modifier = Modifier.weight(1f)
                )
            }

            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MobitikGold,
                edgePadding = 0.dp
            ) {
                tabs.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> AdminWithdrawalsQueueTab(
                    withdrawals = withdrawals,
                    isBusy = isBusy,
                    onReviewWithdrawal = onReviewWithdrawal
                )
                1 -> AdminKycManagementTab(
                    kycSubmissions = kycSubmissions,
                    isBusy = isBusy,
                    onReviewKyc = onReviewKyc
                )
                2 -> AdminProtocolLiquidityTab(config = config, isBusy = isBusy, onSaveConfig = onSaveConfig)
                3 -> AdminUsersManagementTab(
                    users = users,
                    isBusy = isBusy,
                    onToggleUserStatus = onToggleUserStatus
                )
                4 -> AdminSupportTicketsTab(
                    tickets = tickets,
                    isBusy = isBusy,
                    onReplyTicket = onReplyTicket
                )
                5 -> AdminAuditLogsTab(auditLogs = auditLogs)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}


@Composable
private fun AdminKycManagementTab(
    kycSubmissions: List<KycSubmission>,
    isBusy: Boolean,
    onReviewKyc: (KycSubmission, String, String) -> Unit
) {
    var statusFilter by remember { mutableStateOf("ALL") }
    var selectedForReject by remember { mutableStateOf<KycSubmission?>(null) }
    var rejectReasonInput by remember { mutableStateOf("") }

    val filtered = remember(kycSubmissions, statusFilter) {
        if (statusFilter == "ALL") kycSubmissions
        else kycSubmissions.filter { it.status == statusFilter }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("KYC Verification Queue (${filtered.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("ALL", "PENDING", "APPROVED", "REJECTED", "BANNED").forEach { item ->
                    FilterChip(
                        selected = statusFilter == item,
                        onClick = { statusFilter = item },
                        label = { Text(item, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
            ) {
                Text(
                    text = "No KYC submissions found matching filter ($statusFilter).",
                    modifier = Modifier.padding(24.dp),
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            filtered.forEach { sub ->
                val faceBitmap = remember(sub.faceScanBase64) {
                    try {
                        if (sub.faceScanBase64.isNotBlank()) {
                            val bytes = android.util.Base64.decode(sub.faceScanBase64, android.util.Base64.DEFAULT)
                            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                        } else null
                    } catch (_: Exception) {
                        null
                    }
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "@${sub.username} (${sub.fullName})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = sub.userEmail,
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = when (sub.status) {
                                    "APPROVED" -> MobitikSuccessGreen.copy(alpha = 0.15f)
                                    "PENDING" -> MobitikWarningAmber.copy(alpha = 0.15f)
                                    "BANNED" -> MobitikErrorRed.copy(alpha = 0.15f)
                                    else -> Color(0xFFF1F5F9)
                                }
                            ) {
                                Text(
                                    text = sub.status,
                                    color = when (sub.status) {
                                        "APPROVED" -> MobitikSuccessGreen
                                        "PENDING" -> Color(0xFFB45309)
                                        "BANNED" -> MobitikErrorRed
                                        else -> Color(0xFF64748B)
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Face photo and Doc info row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (faceBitmap != null) {
                                Image(
                                    bitmap = faceBitmap,
                                    contentDescription = "Face Scan",
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                                )
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF1F5F9),
                                    modifier = Modifier.size(80.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF94A3B8))
                                    }
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Doc: ${sub.documentType} • ${sub.documentNumber}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = "Face Hash: ${sub.faceHash.take(16)}...",
                                    fontFamily = JetBrainsMonoFamily,
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                                if (sub.rejectionReason.isNotBlank()) {
                                    Text(
                                        text = "Note: ${sub.rejectionReason}",
                                        fontSize = 11.sp,
                                        color = MobitikErrorRed
                                    )
                                }
                            }
                        }

                        // Action Buttons for PENDING submissions
                        if (sub.status == "PENDING") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onReviewKyc(sub, "APPROVED", "") },
                                    enabled = !isBusy,
                                    colors = ButtonDefaults.buttonColors(containerColor = MobitikSuccessGreen, contentColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Approve KYC", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        selectedForReject = sub
                                        rejectReasonInput = "Document details mismatch or unclear face photo"
                                    },
                                    enabled = !isBusy,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Reject", color = MobitikOrange, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        onReviewKyc(sub, "BANNED", "Multi-account fraud violation")
                                    },
                                    enabled = !isBusy,
                                    colors = ButtonDefaults.buttonColors(containerColor = MobitikErrorRed, contentColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(0.9f)
                                ) {
                                    Text("Ban User", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedForReject != null) {
        val targetSub = selectedForReject!!
        AlertDialog(
            onDismissRequest = { selectedForReject = null },
            title = { Text("Reject KYC for @${targetSub.username}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Specify reason for rejecting verification:")
                    OutlinedTextField(
                        value = rejectReasonInput,
                        onValueChange = { rejectReasonInput = it },
                        label = { Text("Rejection Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReviewKyc(targetSub, "REJECTED", rejectReasonInput.trim())
                        selectedForReject = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MobitikOrange, contentColor = Color.White)
                ) {
                    Text("Confirm Rejection")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedForReject = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
@Composable
private fun AdminProtocolLiquidityTab(
    config: AppConfig,
    isBusy: Boolean,
    onSaveConfig: (AppConfig, String) -> Unit
) {
    var miningReward by remember(config) { mutableStateOf(config.miningReward.toString()) }
    var miningDurationHours by remember(config) { mutableStateOf(config.miningDurationHours.toString()) }
    var referralReward by remember(config) { mutableStateOf(config.referralReward.toString()) }
    var swapRateUsdt by remember(config) { mutableStateOf(config.swapRateUsdt.toString()) }
    var swapFeePercent by remember(config) { mutableStateOf(config.swapFeePercent.toString()) }
    var liquidityPoolUsdt by remember(config) { mutableStateOf(config.liquidityPoolUsdt.toString()) }
    var minWithdrawalUsdt by remember(config) { mutableStateOf(config.minWithdrawalUsdt.toString()) }
    var maxWithdrawalUsdt by remember(config) { mutableStateOf(config.maxWithdrawalUsdt.toString()) }
    var withdrawalNetworkFeeUsdt by remember(config) { mutableStateOf(config.withdrawalNetworkFeeUsdt.toString()) }
    var withdrawalPlatformFeeUsdt by remember(config) { mutableStateOf(config.withdrawalPlatformFeeUsdt.toString()) }
    var bscMbtkContractAddress by remember(config) { mutableStateOf(config.bscMbtkContractAddress) }
    var announcementText by remember(config) { mutableStateOf(config.announcementText) }

    var miningEnabled by remember(config) { mutableStateOf(config.miningEnabled) }
    var sendEnabled by remember(config) { mutableStateOf(config.sendEnabled) }
    var receiveEnabled by remember(config) { mutableStateOf(config.receiveEnabled) }
    var swapEnabled by remember(config) { mutableStateOf(config.swapEnabled) }
    var withdrawalsEnabled by remember(config) { mutableStateOf(config.withdrawalsEnabled) }
    var maintenanceMode by remember(config) { mutableStateOf(config.maintenanceMode) }
    var auditReason by remember { mutableStateOf("Routine treasury & protocol parameter update") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Protocol Parameters & USDT Liquidity Pool", style = MaterialTheme.typography.titleLarge)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = miningReward,
                    onValueChange = { miningReward = it },
                    label = { Text("Mining Reward (MBTK)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = miningDurationHours,
                    onValueChange = { miningDurationHours = it },
                    label = { Text("Duration (Hours)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = referralReward,
                    onValueChange = { referralReward = it },
                    label = { Text("Referral Bonus (MBTK)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = swapRateUsdt,
                    onValueChange = { swapRateUsdt = it },
                    label = { Text("Swap Rate (USDT/MBTK)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = liquidityPoolUsdt,
                    onValueChange = { liquidityPoolUsdt = it },
                    label = { Text("Liquidity Pool Reserve (USDT)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = swapFeePercent,
                    onValueChange = { swapFeePercent = it },
                    label = { Text("Swap Fee (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = minWithdrawalUsdt,
                    onValueChange = { minWithdrawalUsdt = it },
                    label = { Text("Min Withdraw (USDT)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = maxWithdrawalUsdt,
                    onValueChange = { maxWithdrawalUsdt = it },
                    label = { Text("Max Withdraw (USDT)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = withdrawalNetworkFeeUsdt,
                    onValueChange = { withdrawalNetworkFeeUsdt = it },
                    label = { Text("Network Fee (USDT)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = withdrawalPlatformFeeUsdt,
                    onValueChange = { withdrawalPlatformFeeUsdt = it },
                    label = { Text("Platform Fee (USDT)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = bscMbtkContractAddress,
                onValueChange = { bscMbtkContractAddress = it },
                label = { Text("BSC MBTK Token Contract Address (Optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = announcementText,
                onValueChange = { announcementText = it },
                label = { Text("System Announcement Banner") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()
            Text("Emergency Kill-Switches & Protocol Controls", style = MaterialTheme.typography.titleMedium)

            AdminSwitchRow("12-Hour Mining Sessions Enabled", miningEnabled) { miningEnabled = it }
            AdminSwitchRow("Peer-to-Peer MBTK Send Enabled", sendEnabled) { sendEnabled = it }
            AdminSwitchRow("Peer-to-Peer MBTK Receive Enabled", receiveEnabled) { receiveEnabled = it }
            AdminSwitchRow("MBTK-to-USDT Liquidity Swaps Enabled", swapEnabled) { swapEnabled = it }
            AdminSwitchRow("External BEP-20 USDT Withdrawals Enabled", withdrawalsEnabled) { withdrawalsEnabled = it }
            AdminSwitchRow("Global Maintenance Mode", maintenanceMode) { maintenanceMode = it }

            OutlinedTextField(
                value = auditReason,
                onValueChange = { auditReason = it },
                label = { Text("Mandatory Audit Log Reason") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { showConfirmDialog = true },
                enabled = !isBusy && auditReason.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MobitikGold, contentColor = MobitikBlack),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_save_config_button")
            ) {
                Text("Review & Save Protocol Configuration", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Sensitive Admin Update") },
            text = {
                Text(
                    "You are updating global protocol parameters (Reference Swap Rate: $swapRateUsdt USDT, Liquidity Pool: $liquidityPoolUsdt USDT, Swaps Enabled: $swapEnabled, Withdrawals Enabled: $withdrawalsEnabled).\n\nAudit Reason: $auditReason"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = config.copy(
                            miningReward = miningReward.toDoubleOrNull() ?: config.miningReward,
                            miningDurationHours = miningDurationHours.toLongOrNull() ?: config.miningDurationHours,
                            referralReward = referralReward.toDoubleOrNull() ?: config.referralReward,
                            swapRateUsdt = swapRateUsdt.toDoubleOrNull() ?: config.swapRateUsdt,
                            swapFeePercent = swapFeePercent.toDoubleOrNull() ?: config.swapFeePercent,
                            liquidityPoolUsdt = liquidityPoolUsdt.toDoubleOrNull() ?: config.liquidityPoolUsdt,
                            minWithdrawalUsdt = minWithdrawalUsdt.toDoubleOrNull() ?: config.minWithdrawalUsdt,
                            maxWithdrawalUsdt = maxWithdrawalUsdt.toDoubleOrNull() ?: config.maxWithdrawalUsdt,
                            withdrawalNetworkFeeUsdt = withdrawalNetworkFeeUsdt.toDoubleOrNull() ?: config.withdrawalNetworkFeeUsdt,
                            withdrawalPlatformFeeUsdt = withdrawalPlatformFeeUsdt.toDoubleOrNull() ?: config.withdrawalPlatformFeeUsdt,
                            bscMbtkContractAddress = bscMbtkContractAddress.trim(),
                            announcementText = announcementText.trim(),
                            miningEnabled = miningEnabled,
                            sendEnabled = sendEnabled,
                            receiveEnabled = receiveEnabled,
                            swapEnabled = swapEnabled,
                            withdrawalsEnabled = withdrawalsEnabled,
                            maintenanceMode = maintenanceMode
                        )
                        onSaveConfig(updated, auditReason)
                        showConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MobitikGold, contentColor = MobitikBlack)
                ) {
                    Text("Confirm & Log Audit", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AdminSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun AdminWithdrawalsQueueTab(
    withdrawals: List<UsdtWithdrawal>,
    isBusy: Boolean,
    onReviewWithdrawal: (UsdtWithdrawal, String, String, String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val web3Client = remember { BscWeb3Client() }
    var selectedForAction by remember { mutableStateOf<UsdtWithdrawal?>(null) }
    var txHashInput by remember { mutableStateOf("") }
    var adminNoteInput by remember { mutableStateOf("") }
    var onChainVerifyStatus by remember { mutableStateOf<String?>(null) }
    var isVerifyingHash by remember { mutableStateOf(false) }

    val clipboard = LocalClipboardManager.current
    var copiedAddressId by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("External BEP-20 USDT Withdrawal Queue", style = MaterialTheme.typography.titleLarge)
        if (withdrawals.isEmpty()) {
            Text(
                "No withdrawal requests found.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            withdrawals.forEach { wd ->
                val totalFee = wd.networkFeeUsdt + wd.platformFeeUsdt
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "@${wd.username} • ${"%.2f".format(wd.finalAmountUsdt)} USDT Net",
                                style = MaterialTheme.typography.titleMedium
                            )
                            StatusPillBadge(
                                label = wd.status,
                                color = when (wd.status) {
                                    "CONFIRMED" -> MobitikSuccessGreen
                                    "PENDING", "PROCESSING" -> MobitikWarningAmber
                                    else -> MobitikErrorRed
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "BEP-20: ${wd.destinationAddress}",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (copiedAddressId == wd.id) MobitikSuccessGreen.copy(alpha = 0.2f) else MobitikUsdtTeal.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .clickable {
                                        clipboard.setText(AnnotatedString(wd.destinationAddress))
                                        copiedAddressId = wd.id
                                    }
                                    .testTag("admin_copy_withdrawal_address_${wd.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Withdrawal Address",
                                        tint = if (copiedAddressId == wd.id) MobitikSuccessGreen else MobitikUsdtTeal,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (copiedAddressId == wd.id) "Copied ✓" else "Copy Address",
                                        color = if (copiedAddressId == wd.id) MobitikSuccessGreen else MobitikUsdtTeal,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Gross: ${"%.2f".format(wd.amountUsdt)} USDT • Total Fee: ${"%.2f".format(totalFee)} USDT • Key: ${wd.idempotencyKey.take(12)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (wd.txHash.isNotBlank()) {
                            Text(
                                text = "BSC TxHash: ${wd.txHash}",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = MobitikUsdtTeal
                            )
                        }
                        if (wd.status in listOf("PENDING", "PROCESSING")) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val autoHash = wd.txHash.ifBlank {
                                            "0x" + wd.id.replace("_", "").padEnd(64, 'a').take(64)
                                        }
                                        onReviewWithdrawal(
                                            wd,
                                            "CONFIRMED",
                                            autoHash,
                                            "Approved USDT payout by Admin (${MobitikRepository.ADMIN_EMAIL})"
                                        )
                                    },
                                    enabled = !isBusy,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MobitikSuccessGreen,
                                        contentColor = MobitikBlack
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("admin_approve_usdt_${wd.id}")
                                ) {
                                    Text("Approve USDT", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        onReviewWithdrawal(
                                            wd,
                                            "REJECTED",
                                            "",
                                            "Rejected & refunded USDT to user balance by Admin"
                                        )
                                    },
                                    enabled = !isBusy,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("admin_reject_usdt_${wd.id}")
                                ) {
                                    Text("Reject & Refund", color = MobitikErrorRed, fontWeight = FontWeight.Bold)
                                }
                            }
                            TextButton(
                                onClick = {
                                    selectedForAction = wd
                                    txHashInput = wd.txHash
                                    adminNoteInput = "Verified BEP-20 payout on BNB Smart Chain"
                                    onChainVerifyStatus = null
                                }
                            ) {
                                Text("Custom BSC TxHash / On-Chain Verify", color = MobitikOrange)
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedForAction != null) {
        val wd = selectedForAction!!
        AlertDialog(
            onDismissRequest = { selectedForAction = null },
            title = { Text("Settle Withdrawal #${wd.id.takeLast(6)}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("User: @${wd.username} (${"%.2f".format(wd.finalAmountUsdt)} USDT Net to ${wd.destinationAddress})")
                    OutlinedTextField(
                        value = txHashInput,
                        onValueChange = {
                            txHashInput = it
                            onChainVerifyStatus = null
                        },
                        label = { Text("BSC On-Chain TxHash (0x + 64 hex chars)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isVerifyingHash = true
                                val res = web3Client.verifyTransactionOnChain(txHashInput)
                                isVerifyingHash = false
                                onChainVerifyStatus = res.message
                            }
                        },
                        enabled = !isVerifyingHash && txHashInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isVerifyingHash) "Querying BSC RPC Receipt..." else "Verify TxHash Receipt on BNB Smart Chain")
                    }
                    if (!onChainVerifyStatus.isNullOrBlank()) {
                        Text(
                            text = onChainVerifyStatus!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = MobitikUsdtTeal
                        )
                    }
                    OutlinedTextField(
                        value = adminNoteInput,
                        onValueChange = { adminNoteInput = it },
                        label = { Text("Admin Settlement / Rejection Note") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReviewWithdrawal(wd, "CONFIRMED", txHashInput.trim(), adminNoteInput.trim())
                        selectedForAction = null
                    },
                    enabled = txHashInput.trim().startsWith("0x") && txHashInput.trim().length >= 16,
                    colors = ButtonDefaults.buttonColors(containerColor = MobitikSuccessGreen, contentColor = MobitikBlack)
                ) {
                    Text("Confirm Payout", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            onReviewWithdrawal(wd, "REJECTED", "", adminNoteInput.ifBlank { "Rejected & Refunded by Admin" })
                            selectedForAction = null
                        }
                    ) {
                        Text("Reject & Refund", color = MobitikErrorRed)
                    }
                    TextButton(onClick = { selectedForAction = null }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

@Composable
private fun AdminUsersManagementTab(
    users: List<UserProfile>,
    isBusy: Boolean,
    onToggleUserStatus: (UserProfile, String, String) -> Unit
) {
    var search by remember { mutableStateOf("") }
    val filtered = remember(users, search) {
        val q = search.trim().lowercase()
        if (q.isBlank()) users else users.filter {
            it.username.lowercase().contains(q) ||
                it.email.lowercase().contains(q) ||
                it.walletAddress.lowercase().contains(q)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text("Search users by username, email, or MBTK address") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        filtered.forEach { u ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${u.name} (@${u.username})", style = MaterialTheme.typography.titleMedium)
                            Text(u.email, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        StatusPillBadge(
                            label = u.accountStatus,
                            color = if (u.accountStatus == "ACTIVE") MobitikSuccessGreen else MobitikErrorRed
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Balance: ${"%.2f".format(u.availableBalance)} MBTK • ${"%.2f".format(u.usdtBalance)} USDT • Sessions: ${u.completedSessions} • Referrals: ${u.totalReferrals}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val targetStatus = if (u.accountStatus == "ACTIVE") "SUSPENDED" else "ACTIVE"
                    OutlinedButton(
                        onClick = {
                            onToggleUserStatus(u, targetStatus, "Admin status change to $targetStatus")
                        },
                        enabled = !isBusy
                    ) {
                        Text(if (u.accountStatus == "ACTIVE") "Suspend Account" else "Reactivate Account")
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminSupportTicketsTab(
    tickets: List<SupportTicket>,
    isBusy: Boolean,
    onReplyTicket: (SupportTicket, String, String) -> Unit
) {
    var activeTicket by remember { mutableStateOf<SupportTicket?>(null) }
    var replyText by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tickets.forEach { t ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${t.name} (${t.email}): ${t.subject}", style = MaterialTheme.typography.titleMedium)
                        StatusPillBadge(label = t.status, color = MobitikOrange)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(t.message, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            activeTicket = t
                            replyText = t.adminReply
                        },
                        enabled = !isBusy
                    ) {
                        Text("Reply & Resolve")
                    }
                }
            }
        }
    }

    if (activeTicket != null) {
        val t = activeTicket!!
        AlertDialog(
            onDismissRequest = { activeTicket = null },
            title = { Text("Reply to ${t.name}") },
            text = {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    label = { Text("Official Support Reply") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReplyTicket(t, replyText.trim(), "RESOLVED")
                        activeTicket = null
                    },
                    enabled = replyText.isNotBlank()
                ) {
                    Text("Send Reply & Mark Resolved")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeTicket = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AdminAuditLogsTab(auditLogs: List<AuditLog>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Immutable Security & Treasury Audit Trail", style = MaterialTheme.typography.titleLarge)
        if (auditLogs.isEmpty()) {
            Text(
                "No admin actions logged yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            auditLogs.forEach { log ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(log.actionType, style = MaterialTheme.typography.titleMedium, color = MobitikGold)
                            Text(
                                formatTimestamp(log.createdAt?.toDate()),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "Admin: ${log.adminEmail} • Target: ${log.targetId}",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            log.details,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

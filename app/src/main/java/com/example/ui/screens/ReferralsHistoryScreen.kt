package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppConfig
import com.example.data.model.MiningSession
import com.example.data.model.PublicProfile
import com.example.data.model.ReferralRecord
import com.example.data.model.UserProfile
import com.example.data.model.WalletTransaction
import com.example.ui.components.StatMetricCard
import com.example.ui.components.StatusPillBadge
import com.example.ui.components.formatEpochMs
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MobitikBlack
import com.example.ui.theme.MobitikErrorRed
import com.example.ui.theme.MobitikGold
import com.example.ui.theme.MobitikOrange
import com.example.ui.theme.MobitikOrangeLight
import com.example.ui.theme.MobitikSuccessGreen
import com.example.ui.theme.MobitikUsdtTeal
import com.example.ui.theme.MobitikWarningAmber

@Composable
fun ReferralsScreen(
    user: UserProfile,
    config: AppConfig,
    referrals: List<ReferralRecord>,
    leaderboard: List<PublicProfile>,
    isBusy: Boolean,
    onClaimReferralBonus: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val inviteLink = "https://mobitik.network/invite/${user.referralCode}"
    val eligibleCount = referrals.count { it.status == "ELIGIBLE" }
    val rewardedCount = referrals.count { it.status == "REWARDED" }

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
            Text("Pioneer Referral Program", style = MaterialTheme.typography.headlineMedium)

            // Invite Code & Share Card
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
                        Column {
                            Text("Your Personal Referral Code", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = user.referralCode,
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.headlineLarge,
                                color = MobitikOrange
                            )
                        }
                        StatusPillBadge(
                            label = "+${"%.2f".format(config.referralReward)} MBTK / ELIGIBLE",
                            color = MobitikGold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Invite Link: $inviteLink",
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Eligibility Rule: Invited pioneers become ELIGIBLE after completing at least ${config.referralEligibilitySessions} 12-hour mining session(s). Duplicate accounts and self-referrals are automatically blocked.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(user.referralCode))
                                onShowMessage("Referral code ${user.referralCode} copied!")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("copy_referral_code_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Code")
                        }

                        Button(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Join me on MOBITIK! Mine 0.22 MOBITIK every 12 hours. Use my referral code ${user.referralCode} or join at $inviteLink"
                                    )
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Invite Friends to MOBITIK"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MobitikOrange, contentColor = MobitikBlack),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("share_referral_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share Invite", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (eligibleCount > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onClaimReferralBonus,
                            enabled = !isBusy,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MobitikSuccessGreen,
                                contentColor = MobitikBlack
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CardGiftcard, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Claim Bonus for $eligibleCount Eligible Referral(s)",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Referral Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatMetricCard(
                    title = "Registered",
                    value = "${referrals.size}",
                    subtitle = "Total Invited",
                    icon = Icons.Default.GroupAdd,
                    accentColor = MobitikOrange,
                    modifier = Modifier.weight(1f)
                )
                StatMetricCard(
                    title = "Eligible / Active",
                    value = "${eligibleCount + rewardedCount}",
                    subtitle = "$eligibleCount unclaimed",
                    icon = Icons.Default.VerifiedUser,
                    accentColor = MobitikSuccessGreen,
                    modifier = Modifier.weight(1f)
                )
                StatMetricCard(
                    title = "Bonus Earned",
                    value = "${"%.2f".format(user.referralRewards)}",
                    subtitle = "MOBITIK",
                    icon = Icons.Default.CardGiftcard,
                    accentColor = MobitikGold,
                    modifier = Modifier.weight(1f)
                )
            }

            // Referral History
            Text("Your Invited Pioneers", style = MaterialTheme.typography.titleLarge)
            if (referrals.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "You haven't referred any pioneers yet. Share your code ${user.referralCode} to start earning referral rewards!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                referrals.forEach { ref ->
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
                                Text("@${ref.referredUsername}", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Joined: ${formatTimestamp(ref.createdAt?.toDate())}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                StatusPillBadge(
                                    label = ref.status,
                                    color = when (ref.status) {
                                        "REWARDED" -> MobitikSuccessGreen
                                        "ELIGIBLE" -> MobitikGold
                                        "REGISTERED" -> MobitikOrangeLight
                                        else -> MobitikErrorRed
                                    }
                                )
                                if (ref.rewardAmount > 0.0) {
                                    Text(
                                        text = "+${"%.2f".format(ref.rewardAmount)} MBTK",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MobitikSuccessGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Referral Leaderboard
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MobitikGold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Top Pioneer Referral Leaderboard", style = MaterialTheme.typography.titleLarge)
            }

            if (leaderboard.isEmpty()) {
                Text(
                    text = "Leaderboard loading...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                leaderboard.take(15).forEachIndexed { idx, entry ->
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (idx) {
                                                0 -> MobitikGold.copy(alpha = 0.25f)
                                                1 -> MobitikOrange.copy(alpha = 0.2f)
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "#${idx + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (idx == 0) MobitikGold else MobitikOrange
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("@${entry.username}", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "${entry.completedSessions} mining sessions",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            StatusPillBadge(
                                label = "${entry.totalReferrals} Referrals",
                                color = MobitikOrange
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(
    transactions: List<WalletTransaction>,
    miningSessions: List<MiningSession>
) {
    var selectedSection by remember { mutableIntStateOf(0) } // 0 = Wallet Ledger, 1 = Mining Sessions
    var selectedFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTxDetail by remember { mutableStateOf<WalletTransaction?>(null) }

    val filterOptions = listOf(
        "ALL" to "All",
        "MINING_REWARD" to "Mining",
        "REFERRAL_REWARD" to "Referrals",
        "TRANSFER_IN" to "Received",
        "TRANSFER_OUT" to "Sent",
        "SWAP" to "Swaps",
        "WITHDRAWAL" to "Withdrawals"
    )

    val filteredTransactions = remember(transactions, selectedFilter, searchQuery) {
        transactions.filter { tx ->
            val matchesFilter = selectedFilter == "ALL" || tx.type == selectedFilter
            val q = searchQuery.trim().lowercase()
            val matchesSearch = q.isBlank() ||
                tx.txHash.lowercase().contains(q) ||
                tx.recipientAddress.lowercase().contains(q) ||
                tx.senderAddress.lowercase().contains(q) ||
                tx.note.lowercase().contains(q)
            matchesFilter && matchesSearch
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
                .widthIn(max = 600.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Ledger & Mining History", style = MaterialTheme.typography.headlineMedium)

            TabRow(
                selectedTabIndex = selectedSection,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MobitikOrange
            ) {
                Tab(
                    selected = selectedSection == 0,
                    onClick = { selectedSection = 0 },
                    text = { Text("Wallet Transactions (${transactions.size})") },
                    modifier = Modifier.testTag("history_tab_transactions")
                )
                Tab(
                    selected = selectedSection == 1,
                    onClick = { selectedSection = 1 },
                    text = { Text("12h Mining Sessions (${miningSessions.size})") },
                    modifier = Modifier.testTag("history_tab_mining")
                )
            }

            if (selectedSection == 0) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search by TxHash, Address, or Note") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_search_input")
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    filterOptions.forEach { (key, label) ->
                        FilterChip(
                            selected = selectedFilter == key,
                            onClick = { selectedFilter = key },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MobitikOrange.copy(alpha = 0.22f),
                                selectedLabelColor = MobitikOrange
                            )
                        )
                    }
                }

                if (filteredTransactions.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "No matching wallet transactions found.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    filteredTransactions.forEach { tx ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTxDetail = tx }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tx.type.replace("_", " "),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = tx.txHash,
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = formatTimestamp(tx.createdAt?.toDate()),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    val isCompleted = tx.status in listOf("CONFIRMED", "COMPLETED", "APPROVED")
                                    val isOut = tx.type in listOf("TRANSFER_OUT", "WITHDRAWAL")
                                    val color = when {
                                        isCompleted -> MobitikSuccessGreen
                                        tx.type == "SWAP" -> MobitikUsdtTeal
                                        isOut -> MobitikWarningAmber
                                        else -> MobitikSuccessGreen
                                    }
                                    val displayAmount = if (tx.type == "SWAP") {
                                        "${"%.2f".format(tx.amount)} MBTK → ${"%.2f".format(tx.swapReceivedAmount)} USDT"
                                    } else {
                                        "${if (isOut) "-" else "+"}${"%.2f".format(tx.amount)} ${tx.tokenSymbol}"
                                    }
                                    Text(
                                        text = displayAmount,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = color
                                    )
                                    StatusPillBadge(
                                        label = if (isCompleted) "COMPLETED" else tx.status,
                                        color = if (isCompleted) MobitikSuccessGreen else color
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Separate Mining Session History
                if (miningSessions.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No 12-hour mining sessions recorded yet. Start your first session on the Home tab!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    miningSessions.forEach { session ->
                        val nowMs = System.currentTimeMillis()
                        val displayStatus = when {
                            session.claimed || session.status == "CLAIMED" -> "CLAIMED"
                            nowMs >= session.endTimeMs -> "COMPLETED"
                            else -> "ACTIVE"
                        }
                        val statusColor = when (displayStatus) {
                            "CLAIMED" -> MobitikSuccessGreen
                            "COMPLETED" -> MobitikGold
                            else -> MobitikOrange
                        }

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
                                        text = "Session #${session.sessionNumber} (${session.durationHours} Hours)",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    StatusPillBadge(label = displayStatus, color = statusColor)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Start: ${formatEpochMs(session.startTimeMs)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Completion: ${formatEpochMs(session.endTimeMs)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Reward: +${"%.2f".format(session.rewardAmount)} MOBITIK • Claimed: ${if (session.claimed) "Yes" else "No"}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = statusColor
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (selectedTxDetail != null) {
        val tx = selectedTxDetail!!
        AlertDialog(
            onDismissRequest = { selectedTxDetail = null },
            title = { Text("Transaction Details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Type: ${tx.type}", fontWeight = FontWeight.Bold)
                    Text("Status: ${tx.status}", color = MobitikOrange)
                    Text("Amount: ${"%.4f".format(tx.amount)} ${tx.tokenSymbol}")
                    if (tx.fee > 0.0) Text("Fee: ${"%.4f".format(tx.fee)}")
                    if (tx.swapToToken.isNotBlank()) {
                        Text("Swapped To: ${"%.4f".format(tx.swapReceivedAmount)} ${tx.swapToToken}")
                    }
                    Text("Sender: ${tx.senderAddress}", fontFamily = JetBrainsMonoFamily, style = MaterialTheme.typography.labelSmall)
                    Text("Recipient: ${tx.recipientAddress}", fontFamily = JetBrainsMonoFamily, style = MaterialTheme.typography.labelSmall)
                    Text("Reference Hash: ${tx.txHash}", fontFamily = JetBrainsMonoFamily, style = MaterialTheme.typography.labelSmall)
                    if (tx.note.isNotBlank()) Text("Memo: ${tx.note}")
                    Text("Date: ${formatTimestamp(tx.createdAt?.toDate())}")
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedTxDetail = null }) {
                    Text("Close", color = MobitikOrange)
                }
            }
        )
    }
}

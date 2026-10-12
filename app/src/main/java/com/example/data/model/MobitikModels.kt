package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val username: String = "",
    val email: String = "",
    val walletAddress: String = "",
    val externalWalletAddress: String = "",
    val externalWalletNetwork: String = "",
    val referralCode: String = "",
    val referredByCode: String = "",
    val referredByUserId: String = "",
    val totalBalance: Double = 0.0,
    val availableBalance: Double = 0.0,
    val usdtBalance: Double = 0.0,
    val pendingUsdtWithdrawal: Double = 0.0,
    val pendingRewards: Double = 0.0,
    val lockedBalance: Double = 0.0,
    val totalMiningRewards: Double = 0.0,
    val completedSessions: Long = 0L,
    val totalReferrals: Long = 0L,
    val activeReferrals: Long = 0L,
    val referralRewards: Double = 0.0,
    val autoStartMining: Boolean = false,
    val twoFactorEnabled: Boolean = false,
    val pinHash: String = "",
    val accountStatus: String = "ACTIVE",
    val role: String = "USER",
    val notificationsEnabled: Boolean = true,
    val themeMode: String = "DARK",
    val language: String = "en",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "name" to name,
        "username" to username,
        "email" to email,
        "walletAddress" to walletAddress,
        "externalWalletAddress" to externalWalletAddress,
        "externalWalletNetwork" to externalWalletNetwork,
        "referralCode" to referralCode,
        "referredByCode" to referredByCode,
        "referredByUserId" to referredByUserId,
        "totalBalance" to totalBalance,
        "availableBalance" to availableBalance,
        "usdtBalance" to usdtBalance,
        "pendingUsdtWithdrawal" to pendingUsdtWithdrawal,
        "pendingRewards" to pendingRewards,
        "lockedBalance" to lockedBalance,
        "totalMiningRewards" to totalMiningRewards,
        "completedSessions" to completedSessions,
        "totalReferrals" to totalReferrals,
        "activeReferrals" to activeReferrals,
        "referralRewards" to referralRewards,
        "autoStartMining" to autoStartMining,
        "twoFactorEnabled" to twoFactorEnabled,
        "pinHash" to pinHash,
        "accountStatus" to accountStatus,
        "role" to role,
        "notificationsEnabled" to notificationsEnabled,
        "themeMode" to themeMode,
        "language" to language,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class PublicProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val walletAddress: String = "",
    val referralCode: String = "",
    val totalReferrals: Long = 0L,
    val activeReferrals: Long = 0L,
    val completedSessions: Long = 0L,
    val accountStatus: String = "ACTIVE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "username" to username,
        "displayName" to displayName,
        "walletAddress" to walletAddress,
        "referralCode" to referralCode,
        "totalReferrals" to totalReferrals,
        "activeReferrals" to activeReferrals,
        "completedSessions" to completedSessions,
        "accountStatus" to accountStatus,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class MiningSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sessionNumber: Long = 1L,
    val startTimeMs: Long = 0L,
    val endTimeMs: Long = 0L,
    val durationHours: Long = 12L,
    val rewardAmount: Double = 0.22,
    val status: String = "ACTIVE",
    val claimed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "sessionNumber" to sessionNumber,
        "startTimeMs" to startTimeMs,
        "endTimeMs" to endTimeMs,
        "durationHours" to durationHours,
        "rewardAmount" to rewardAmount,
        "status" to status,
        "claimed" to claimed,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class WalletTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val counterpartyUserId: String = "",
    val type: String = "MINING_REWARD",
    val senderAddress: String = "",
    val recipientAddress: String = "",
    val amount: Double = 0.0,
    val fee: Double = 0.0,
    val tokenSymbol: String = "MOBITIK",
    val swapToToken: String = "",
    val swapReceivedAmount: Double = 0.0,
    val note: String = "",
    val status: String = "COMPLETED",
    val txHash: String = "",
    val sessionNumber: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "counterpartyUserId" to counterpartyUserId,
        "type" to type,
        "senderAddress" to senderAddress,
        "recipientAddress" to recipientAddress,
        "amount" to amount,
        "fee" to fee,
        "tokenSymbol" to tokenSymbol,
        "swapToToken" to swapToToken,
        "swapReceivedAmount" to swapReceivedAmount,
        "note" to note,
        "status" to status,
        "txHash" to txHash,
        "sessionNumber" to sessionNumber,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class UsdtWithdrawal(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val destinationAddress: String = "",
    val network: String = "BNB Smart Chain (BEP-20)",
    val amountUsdt: Double = 0.0,
    val networkFeeUsdt: Double = 0.25,
    val platformFeeUsdt: Double = 0.0,
    val finalAmountUsdt: Double = 0.0,
    val status: String = "PENDING",
    val txHash: String = "",
    val adminNote: String = "",
    val idempotencyKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "username" to username,
        "destinationAddress" to destinationAddress,
        "network" to network,
        "amountUsdt" to amountUsdt,
        "networkFeeUsdt" to networkFeeUsdt,
        "platformFeeUsdt" to platformFeeUsdt,
        "finalAmountUsdt" to finalAmountUsdt,
        "status" to status,
        "txHash" to txHash,
        "adminNote" to adminNote,
        "idempotencyKey" to idempotencyKey,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class ReferralRecord(
    @DocumentId val id: String = "",
    val referrerUserId: String = "",
    val referredUserId: String = "",
    val referredUsername: String = "",
    val referralCode: String = "",
    val status: String = "REGISTERED",
    val rewardAmount: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "referrerUserId" to referrerUserId,
        "referredUserId" to referredUserId,
        "referredUsername" to referredUsername,
        "referralCode" to referralCode,
        "status" to status,
        "rewardAmount" to rewardAmount,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val senderUid: String = "",
    val title: String = "",
    val message: String = "",
    val category: String = "MINING",
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "senderUid" to senderUid,
        "title" to title,
        "message" to message,
        "category" to category,
        "isRead" to isRead,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class SupportTicket(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val subject: String = "",
    val message: String = "",
    val status: String = "OPEN",
    val adminReply: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "name" to name,
        "email" to email,
        "subject" to subject,
        "message" to message,
        "status" to status,
        "adminReply" to adminReply,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class AppConfig(
    @DocumentId val id: String = "global",
    val configKey: String = "global",
    val miningDurationHours: Long = 12L,
    val miningReward: Double = 0.22,
    val miningEnabled: Boolean = true,
    val referralReward: Double = 0.10,
    val referralEligibilitySessions: Long = 1L,
    val sendEnabled: Boolean = true,
    val receiveEnabled: Boolean = true,
    val swapEnabled: Boolean = true,
    val swapRateUsdt: Double = 0.15,
    val swapFeePercent: Double = 0.0,
    val liquidityPoolUsdt: Double = 50000.0,
    val withdrawalsEnabled: Boolean = true,
    val minWithdrawalUsdt: Double = 2.0,
    val maxWithdrawalUsdt: Double = 5000.0,
    val withdrawalNetworkFeeUsdt: Double = 0.25,
    val withdrawalPlatformFeeUsdt: Double = 0.0,
    val bscUsdtContractAddress: String = "0x55d398326f99059fF775485246999027B3197955",
    val bscMbtkContractAddress: String = "",
    val networkFeeMobitik: Double = 0.0,
    val supportEmail: String = "support@mobitik.network",
    val maintenanceMode: Boolean = false,
    val announcementText: String = "Earn 0.22 MBTK every 12h • Swap Open (0.15 USDT) • Min Withdrawal 2 USDT",
    val whitepaperSummary: String = "MOBITIK is a 12-hour cloud mining and USDT swap ecosystem.",
    val updatedByUid: String = "system",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "configKey" to configKey,
        "miningDurationHours" to miningDurationHours,
        "miningReward" to miningReward,
        "miningEnabled" to miningEnabled,
        "referralReward" to referralReward,
        "referralEligibilitySessions" to referralEligibilitySessions,
        "sendEnabled" to sendEnabled,
        "receiveEnabled" to receiveEnabled,
        "swapEnabled" to swapEnabled,
        "swapRateUsdt" to swapRateUsdt,
        "swapFeePercent" to swapFeePercent,
        "liquidityPoolUsdt" to liquidityPoolUsdt,
        "withdrawalsEnabled" to withdrawalsEnabled,
        "minWithdrawalUsdt" to minWithdrawalUsdt,
        "maxWithdrawalUsdt" to maxWithdrawalUsdt,
        "withdrawalNetworkFeeUsdt" to withdrawalNetworkFeeUsdt,
        "withdrawalPlatformFeeUsdt" to withdrawalPlatformFeeUsdt,
        "bscUsdtContractAddress" to bscUsdtContractAddress,
        "bscMbtkContractAddress" to bscMbtkContractAddress,
        "networkFeeMobitik" to networkFeeMobitik,
        "supportEmail" to supportEmail,
        "maintenanceMode" to maintenanceMode,
        "announcementText" to announcementText,
        "whitepaperSummary" to whitepaperSummary,
        "updatedByUid" to updatedByUid,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class FaqEntry(
    @DocumentId val id: String = "",
    val question: String = "",
    val answer: String = "",
    val category: String = "General",
    val sortOrder: Long = 0L,
    val isPublished: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "question" to question,
        "answer" to answer,
        "category" to category,
        "sortOrder" to sortOrder,
        "isPublished" to isPublished,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val adminUid: String = "",
    val adminEmail: String = "",
    val actionType: String = "",
    val targetId: String = "",
    val details: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "adminUid" to adminUid,
        "adminEmail" to adminEmail,
        "actionType" to actionType,
        "targetId" to targetId,
        "details" to details,
        "createdAt" to FieldValue.serverTimestamp()
    )
}

data class OnChainWalletBalance(
    val address: String,
    val network: String = "BNB Smart Chain (BEP-20)",
    val bnbBalance: Double = 0.0,
    val usdtBep20Balance: Double = 0.0,
    val isReachable: Boolean = false,
    val lastCheckedMs: Long = System.currentTimeMillis()
)

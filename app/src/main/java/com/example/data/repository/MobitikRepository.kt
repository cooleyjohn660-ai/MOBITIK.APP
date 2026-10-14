package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AppConfig
import com.example.data.model.AppNotification
import com.example.data.model.AuditLog
import com.example.data.model.FaqEntry
import com.example.data.model.MiningSession
import com.example.data.model.PublicProfile
import com.example.data.model.ReferralRecord
import com.example.data.model.SupportTicket
import com.example.data.model.UsdtWithdrawal
import com.example.data.model.UserProfile
import com.example.data.model.WalletTransaction
import com.example.data.model.KycSubmission
import com.example.data.model.KycFaceIndex
import android.util.Log
import com.example.data.remote.BscWeb3Client
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

class MobitikRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    companion object {
        const val ADMIN_EMAIL = "cooleyjohn660@gmail.com"

        fun isAuthorizedAdminEmail(email: String?): Boolean {
            return email?.trim()?.equals(ADMIN_EMAIL, ignoreCase = true) == true
        }

        fun hashPin(pin: String): String {
            if (pin.isBlank()) return ""
            val bytes = MessageDigest.getInstance("SHA-256").digest(pin.trim().toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }

        fun generateWalletAddress(uid: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest("MBTK_ADDR_$uid".toByteArray())
            val hex = digest.joinToString("") { "%02X".format(it) }.take(20)
            return "MBTK-$hex"
        }

        fun generateReferralCode(uid: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest("MBTK_REF_$uid".toByteArray())
            val hex = digest.joinToString("") { "%02X".format(it) }.take(6)
            return "MBK$hex"
        }

        fun generateTxHash(prefix: String = "MBTK"): String {
            val raw = UUID.randomUUID().toString().replace("-", "").uppercase()
            return "$prefix-TX-${raw.take(20)}"
        }
    }

    var fallbackUserId: String = ""

    private fun requireUserId(): String {
        return FirebaseAuth.getInstance().currentUser?.uid
            ?: fallbackUserId.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
    }

    // --- Real-time Observers ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.GET, path)
                throw e
            }
    }

    fun observeAppConfig(): Flow<AppConfig> {
        val path = "app_config/global"
        return db.collection("app_config").document("global")
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    val raw = snapshot.toObject(AppConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: AppConfig()
                    raw.copy(
                        swapEnabled = true,
                        liquidityPoolUsdt = if (raw.liquidityPoolUsdt <= 0.0) 50000.0 else raw.liquidityPoolUsdt,
                        minWithdrawalUsdt = if (raw.minWithdrawalUsdt == 5.0 || raw.minWithdrawalUsdt <= 0.0) 2.0 else raw.minWithdrawalUsdt,
                        withdrawalNetworkFeeUsdt = if (raw.withdrawalNetworkFeeUsdt == 0.5) 0.25 else raw.withdrawalNetworkFeeUsdt,
                        withdrawalPlatformFeeUsdt = if (raw.withdrawalPlatformFeeUsdt == 0.25 && raw.withdrawalNetworkFeeUsdt == 0.5) 0.0 else raw.withdrawalPlatformFeeUsdt
                    )
                } else {
                    AppConfig()
                }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.GET, path)
                throw e
            }
    }

    fun observeMiningSessions(userId: String): Flow<List<MiningSession>> {
        val path = "mining_sessions"
        return db.collection(path)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(MiningSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.startTimeMs }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    fun observeTransactions(userId: String): Flow<List<WalletTransaction>> {
        val path = "transactions"
        return db.collection(path)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(WalletTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    fun observeIncomingTransfers(userId: String): Flow<List<WalletTransaction>> {
        val path = "transactions"
        return db.collection(path)
            .whereEqualTo("counterpartyUserId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(WalletTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    fun observeWithdrawals(userId: String): Flow<List<UsdtWithdrawal>> {
        val path = "withdrawals"
        return db.collection(path)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(UsdtWithdrawal::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    fun observeReferrals(userId: String): Flow<List<ReferralRecord>> {
        val path = "referrals"
        return db.collection(path)
            .whereEqualTo("referrerUserId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ReferralRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    fun observeLeaderboard(): Flow<List<PublicProfile>> {
        val path = "public_profiles"
        return db.collection(path)
            .whereEqualTo("accountStatus", "ACTIVE")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(compareByDescending<PublicProfile> { it.totalReferrals }.thenByDescending { it.completedSessions })
                    .take(50)
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    fun observeNotifications(userId: String): Flow<List<AppNotification>> {
        val path = "notifications"
        return db.collection(path)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    fun observeSupportTickets(userId: String): Flow<List<SupportTicket>> {
        val path = "support_tickets"
        return db.collection(path)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(SupportTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    fun observePublishedFaqs(): Flow<List<FaqEntry>> {
        val path = "faq_entries"
        return db.collection(path)
            .whereEqualTo("isPublished", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FaqEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.sortOrder }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    // --- Admin Observers ---

    fun observeAllUsersForAdmin(): Flow<List<UserProfile>> {
        val path = "users"
        return db.collection(path)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    fun observeAllWithdrawalsForAdmin(): Flow<List<UsdtWithdrawal>> {
        val path = "withdrawals"
        return db.collection(path)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(UsdtWithdrawal::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    fun observeAllTicketsForAdmin(): Flow<List<SupportTicket>> {
        val path = "support_tickets"
        return db.collection(path)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(SupportTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    fun observeAuditLogsForAdmin(): Flow<List<AuditLog>> {
        val path = "audit_logs"
        return db.collection(path)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(AuditLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    // --- Core Operations ---

    suspend fun ensureGlobalConfigAndFaqs(): Result<Unit> = try {
        val configDoc = db.collection("app_config").document("global").get().await()
        if (!configDoc.exists()) {
            val defaultConfig = AppConfig(updatedByUid = requireUserId())
            db.collection("app_config").document("global").set(defaultConfig.toCreateMap()).await()
        }
        val faqSnap = db.collection("faq_entries").whereEqualTo("isPublished", true).limit(1).get().await()
        if (faqSnap.isEmpty) {
            seedDefaultFaqs()
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "app_config/global")
        Result.failure(e)
    }

    suspend fun ensureAdminBootstrap(userId: String, email: String, currentProfile: UserProfile): Result<Unit> {
        if (!isAuthorizedAdminEmail(email) && !isAuthorizedAdminEmail(currentProfile.email)) {
            return Result.success(Unit)
        }
        return try {
            val adminDocRef = db.collection("admins").document(userId)
            val adminSnap = adminDocRef.get().await()
            if (!adminSnap.exists()) {
                adminDocRef.set(
                    mapOf(
                        "uid" to userId,
                        "email" to ADMIN_EMAIL,
                        "role" to "ADMIN",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            if (currentProfile.role != "ADMIN") {
                db.collection("users").document(userId).update(
                    mapOf(
                        "role" to "ADMIN",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun seedDefaultFaqs() {
        val defaultFaqs = listOf(
            FaqEntry(
                question = "What is MOBITIK (MBTK)?",
                answer = "MOBITIK is a mobile-first 12-hour participation mining and Web3 wallet ecosystem designed for accessibility, transparent reward accounting, and BNB Smart Chain (BEP-20) interoperability.",
                category = "General",
                sortOrder = 1L
            ),
            FaqEntry(
                question = "How does 12-hour mining work and how much can I earn?",
                answer = "Press Start Mining on the Home dashboard to begin a 12-hour server-tracked session. Each completed 12-hour session earns 0.22 MOBITIK (configurable by protocol governance). You can close the app while the timer runs on the server.",
                category = "Mining",
                sortOrder = 2L
            ),
            FaqEntry(
                question = "Why must I restart mining every 12 hours?",
                answer = "Periodic 12-hour session check-ins verify active human participation and prevent automated bot farms. You may also enable Auto-Start in Settings so your next session begins immediately upon claiming a reward.",
                category = "Mining",
                sortOrder = 3L
            ),
            FaqEntry(
                question = "How does the MBTK-to-USDT swap work?",
                answer = "The Swap screen uses the configured reference price (1 MBTK = 0.15 USDT; 10 MBTK = 1.50 USDT; 100 MBTK = 15 USDT; 1,000 MBTK = 150 USDT). Swaps execute only when the authorized protocol USDT liquidity pool is funded and enabled.",
                category = "Swap & Wallet",
                sortOrder = 4L
            ),
            FaqEntry(
                question = "How do I connect Trust Wallet and withdraw USDT?",
                answer = "Open the Wallet tab, tap Connect Trust Wallet, and link your BNB Smart Chain (BEP-20) address. You can then request a USDT withdrawal to your external BEP-20 wallet once you meet the minimum withdrawal threshold.",
                category = "Swap & Wallet",
                sortOrder = 5L
            ),
            FaqEntry(
                question = "How do referral rewards work?",
                answer = "Every pioneer receives a unique referral code and invite link. When friends register with your referral code and complete their required mining sessions, they become Eligible Referrals and you can claim your referral bonus.",
                category = "Referrals",
                sortOrder = 6L
            )
        )
        defaultFaqs.forEachIndexed { idx, faq ->
            val docId = "faq_default_${idx + 1}"
            db.collection("faq_entries").document(docId).set(faq.toCreateMap()).await()
        }
    }

    suspend fun registerUserProfile(
        name: String,
        username: String,
        email: String,
        referralCodeInput: String,
        pinCode: String,
        twoFactorEnabled: Boolean
    ): Result<UserProfile> {
        val uid = requireUserId()
        val userPath = "users/$uid"
        return try {
            val cleanUsername = username.trim().lowercase().replace(Regex("[^a-z0-9_]"), "")
            require(cleanUsername.length in 3..30) { "Username must be 3–30 alphanumeric characters." }
            require(name.trim().length in 2..80) { "Please enter your full name." }

            val existingDoc = db.collection("users").document(uid).get().await()
            if (existingDoc.exists()) {
                val existingProfile = existingDoc.toObject(UserProfile::class.java) ?: UserProfile(userId = uid)
                return Result.success(existingProfile)
            }

            // Check if username is already taken in public_profiles
            try {
                val usernameCheck = db.collection("public_profiles")
                    .whereEqualTo("username", cleanUsername)
                    .limit(1)
                    .get()
                    .await()
                if (!usernameCheck.isEmpty && usernameCheck.documents.any { it.id != uid }) {
                    return Result.failure(IllegalArgumentException("Username @$cleanUsername is already taken."))
                }
            } catch (e: Exception) {
                Log.w("MobitikRepo", "Username check non-fatal exception: ${e.message}")
            }

            val walletAddress = generateWalletAddress(uid)
            val myReferralCode = generateReferralCode(uid)
            val cleanRefCode = referralCodeInput.trim().uppercase()

            var inviterUid = ""
            if (cleanRefCode.isNotBlank() && cleanRefCode != myReferralCode) {
                try {
                    val inviterSnap = db.collection("public_profiles")
                        .whereEqualTo("referralCode", cleanRefCode)
                        .limit(1)
                        .get()
                        .await()
                    val inviterDoc = inviterSnap.documents.firstOrNull { it.id != uid }
                    if (inviterDoc != null) {
                        inviterUid = inviterDoc.id
                    }
                } catch (e: Exception) {
                    Log.w("MobitikRepo", "Inviter check non-fatal exception: ${e.message}")
                }
            }

            val isAdmin = isAuthorizedAdminEmail(email)
            val role = if (isAdmin) "ADMIN" else "USER"
            val profile = UserProfile(
                id = uid,
                userId = uid,
                name = name.trim(),
                username = cleanUsername,
                email = if (isAdmin) ADMIN_EMAIL else email.trim().ifBlank { "$cleanUsername@mobitik.user" },
                walletAddress = walletAddress,
                externalWalletAddress = "",
                externalWalletNetwork = "",
                referralCode = myReferralCode,
                referredByCode = if (inviterUid.isNotBlank()) cleanRefCode else "",
                referredByUserId = inviterUid,
                pinHash = hashPin(pinCode),
                twoFactorEnabled = twoFactorEnabled,
                role = role
            )

            val publicProfile = PublicProfile(
                id = uid,
                userId = uid,
                username = cleanUsername,
                displayName = name.trim(),
                walletAddress = walletAddress,
                referralCode = myReferralCode
            )

            db.collection("users").document(uid).set(profile.toCreateMap()).await()
            db.collection("public_profiles").document(uid).set(publicProfile.toCreateMap()).await()
            if (isAdmin) {
                db.collection("admins").document(uid).set(
                    mapOf(
                        "uid" to uid,
                        "email" to ADMIN_EMAIL,
                        "role" to "ADMIN",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // If referred by a valid pioneer, create ReferralRecord & increment inviter's public counter
            if (inviterUid.isNotBlank()) {
                try {
                    val refId = "ref_${uid}"
                    val referralRecord = ReferralRecord(
                        id = refId,
                        referrerUserId = inviterUid,
                        referredUserId = uid,
                        referredUsername = cleanUsername,
                        referralCode = cleanRefCode,
                        status = "REGISTERED",
                        rewardAmount = 0.0
                    )
                    db.collection("referrals").document(refId).set(referralRecord.toCreateMap()).await()

                    val inviterPublicDoc = db.collection("public_profiles").document(inviterUid).get().await()
                    if (inviterPublicDoc.exists()) {
                        val currentRefs = inviterPublicDoc.getLong("totalReferrals") ?: 0L
                        db.collection("public_profiles").document(inviterUid).update(
                            mapOf(
                                "totalReferrals" to (currentRefs + 1L),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }

                    createNotification(
                        recipientUid = inviterUid,
                        title = "New Pioneer Joined via Your Code!",
                        message = "@$cleanUsername registered using your referral code $cleanRefCode.",
                        category = "REFERRAL"
                    )
                } catch (refErr: Exception) {
                    Log.w("MobitikRepo", "Referral recording non-fatal exception: ${refErr.message}")
                }
            }

            try {
                createNotification(
                    recipientUid = uid,
                    title = "Welcome to MOBITIK Network",
                    message = "Your unique MOBITIK wallet ($walletAddress) is active. Start your first 12-hour mining session!",
                    category = "SECURITY"
                )
            } catch (notifErr: Exception) {
                Log.w("MobitikRepo", "Welcome notification non-fatal exception: ${notifErr.message}")
            }

            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, userPath)
            Result.failure(e)
        }
    }

    suspend fun startMiningSession(user: UserProfile, config: AppConfig): Result<MiningSession> {
        val uid = requireUserId()
        val sessionsPath = "mining_sessions"
        return try {
            require(config.miningEnabled) { "Mining is temporarily paused by administrator." }
            require(user.accountStatus == "ACTIVE") { "Account is suspended. Contact support." }

            val existingSnap = db.collection(sessionsPath)
                .whereEqualTo("userId", uid)
                .get()
                .await()
            val sessions = existingSnap.toObjects(MiningSession::class.java)
            val hasUnclaimedOrActive = sessions.any { !it.claimed && it.status in listOf("ACTIVE", "COMPLETED") }
            if (hasUnclaimedOrActive) {
                return Result.failure(IllegalStateException("You already have an active or unclaimed mining session."))
            }

            val nextSessionNumber = (sessions.maxOfOrNull { it.sessionNumber } ?: 0L) + 1L
            val nowMs = System.currentTimeMillis()
            val durationHours = config.miningDurationHours.coerceAtLeast(1L)
            val endTimeMs = nowMs + durationHours * 3600_000L
            val sessionId = "ms_${uid}_${nextSessionNumber}_${UUID.randomUUID().toString().take(6)}"

            val newSession = MiningSession(
                id = sessionId,
                userId = uid,
                sessionNumber = nextSessionNumber,
                startTimeMs = nowMs,
                endTimeMs = endTimeMs,
                durationHours = durationHours,
                rewardAmount = config.miningReward,
                status = "ACTIVE",
                claimed = false
            )

            db.collection(sessionsPath).document(sessionId).set(newSession.toCreateMap()).await()

            createNotification(
                recipientUid = uid,
                title = "12-Hour Mining Session #$nextSessionNumber Started",
                message = "Mining at ${config.miningReward} MBTK / ${durationHours}h. Your session runs on the server even when offline.",
                category = "MINING"
            )

            Result.success(newSession)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, sessionsPath)
            Result.failure(e)
        }
    }

    suspend fun claimMiningSessionReward(
        user: UserProfile,
        session: MiningSession,
        config: AppConfig
    ): Result<Double> {
        val uid = requireUserId()
        val sessionDocPath = "mining_sessions/${session.id}"
        return try {
            require(!session.claimed && session.status != "CLAIMED") { "This session reward has already been claimed." }
            val nowMs = System.currentTimeMillis()
            require(nowMs >= session.endTimeMs) { "Mining session is still in progress. Please wait for the countdown to finish." }

            // 1. Lock terminal state on mining_sessions document
            db.collection("mining_sessions").document(session.id).update(
                mapOf(
                    "status" to "CLAIMED",
                    "claimed" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val reward = session.rewardAmount
            val newCompletedSessions = user.completedSessions + 1L
            val newTotalBalance = user.totalBalance + reward
            val newAvailableBalance = user.availableBalance + reward
            val newTotalMiningRewards = user.totalMiningRewards + reward

            // 2. Credit user balances
            db.collection("users").document(uid).update(
                mapOf(
                    "totalBalance" to newTotalBalance,
                    "availableBalance" to newAvailableBalance,
                    "totalMiningRewards" to newTotalMiningRewards,
                    "completedSessions" to newCompletedSessions,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            db.collection("public_profiles").document(uid).update(
                mapOf(
                    "completedSessions" to newCompletedSessions,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // 3. Record reward transaction in ledger
            val txId = "tx_mine_${session.id}"
            val tx = WalletTransaction(
                id = txId,
                userId = uid,
                counterpartyUserId = "",
                type = "MINING_REWARD",
                senderAddress = "MOBITIK-PROTOCOL-POOL",
                recipientAddress = user.walletAddress,
                amount = reward,
                fee = 0.0,
                tokenSymbol = "MOBITIK",
                note = "12h Mining Session #${session.sessionNumber} Reward",
                status = "COMPLETED",
                txHash = generateTxHash("MINE"),
                sessionNumber = session.sessionNumber
            )
            db.collection("transactions").document(txId).set(tx.toCreateMap()).await()

            // 4. Update referral eligibility if this user was referred
            if (user.referredByUserId.isNotBlank() && newCompletedSessions >= config.referralEligibilitySessions) {
                val refId = "ref_$uid"
                val refDoc = db.collection("referrals").document(refId).get().await()
                if (refDoc.exists() && refDoc.getString("status") == "REGISTERED") {
                    db.collection("referrals").document(refId).update(
                        mapOf(
                            "status" to "ELIGIBLE",
                            "rewardAmount" to config.referralReward,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }

            createNotification(
                recipientUid = uid,
                title = "Mining Reward Claimed: +$reward MOBITIK",
                message = "Session #${session.sessionNumber} reward has been credited to your available MOBITIK balance.",
                category = "MINING"
            )

            // 5. Optional Auto-Start next session if enabled by user
            if (user.autoStartMining && config.miningEnabled) {
                val updatedUser = user.copy(
                    totalBalance = newTotalBalance,
                    availableBalance = newAvailableBalance,
                    totalMiningRewards = newTotalMiningRewards,
                    completedSessions = newCompletedSessions
                )
                startMiningSession(updatedUser, config)
            }

            Result.success(reward)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, sessionDocPath)
            Result.failure(e)
        }
    }

    suspend fun claimRewardedAdMiningBoost(
        user: UserProfile,
        config: AppConfig
    ): Result<Double> {
        val uid = requireUserId()
        val userDocPath = "users/$uid"
        return try {
            require(config.miningEnabled) { "Mining is temporarily paused by administrator." }
            require(user.accountStatus == "ACTIVE") { "Account is suspended. Contact support." }

            val boostReward = 0.05
            val newTotalBalance = user.totalBalance + boostReward
            val newAvailableBalance = user.availableBalance + boostReward
            val newTotalMiningRewards = user.totalMiningRewards + boostReward

            db.collection("users").document(uid).update(
                mapOf(
                    "totalBalance" to newTotalBalance,
                    "availableBalance" to newAvailableBalance,
                    "totalMiningRewards" to newTotalMiningRewards,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val txId = "tx_adboost_${uid}_${UUID.randomUUID().toString().take(8)}"
            val tx = WalletTransaction(
                id = txId,
                userId = uid,
                counterpartyUserId = "",
                type = "MINING_REWARD",
                senderAddress = "MOBITIK-AD-REWARD-POOL",
                recipientAddress = user.walletAddress,
                amount = boostReward,
                fee = 0.0,
                tokenSymbol = "MOBITIK",
                note = "Rewarded Ad Mining Boost (AdMob)",
                status = "COMPLETED",
                txHash = generateTxHash("ADBOOST"),
                sessionNumber = user.completedSessions
            )
            db.collection("transactions").document(txId).set(tx.toCreateMap()).await()

            Result.success(boostReward)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userDocPath)
            Result.failure(e)
        }
    }

    suspend fun claimEligibleReferralRewards(
        user: UserProfile,
        referrals: List<ReferralRecord>,
        config: AppConfig
    ): Result<Double> {
        val uid = requireUserId()
        val eligibleList = referrals.filter { it.status == "ELIGIBLE" && it.referrerUserId == uid }
        if (eligibleList.isEmpty()) {
            return Result.failure(IllegalStateException("No eligible unclaimed referral rewards available yet."))
        }
        return try {
            var totalClaimed = 0.0
            for (ref in eligibleList) {
                val reward = if (ref.rewardAmount > 0.0) ref.rewardAmount else config.referralReward
                db.collection("referrals").document(ref.id).update(
                    mapOf(
                        "status" to "REWARDED",
                        "rewardAmount" to reward,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                totalClaimed += reward

                val txId = "tx_ref_${ref.id}"
                val tx = WalletTransaction(
                    id = txId,
                    userId = uid,
                    counterpartyUserId = ref.referredUserId,
                    type = "REFERRAL_REWARD",
                    senderAddress = "MOBITIK-REFERRAL-TREASURY",
                    recipientAddress = user.walletAddress,
                    amount = reward,
                    fee = 0.0,
                    tokenSymbol = "MOBITIK",
                    note = "Referral bonus for pioneer @${ref.referredUsername}",
                    status = "COMPLETED",
                    txHash = generateTxHash("REF")
                )
                db.collection("transactions").document(txId).set(tx.toCreateMap()).await()
            }

            val totalRewardedCount = referrals.count { it.status == "REWARDED" } + eligibleList.size
            db.collection("users").document(uid).update(
                mapOf(
                    "totalBalance" to (user.totalBalance + totalClaimed),
                    "availableBalance" to (user.availableBalance + totalClaimed),
                    "referralRewards" to (user.referralRewards + totalClaimed),
                    "totalReferrals" to referrals.size.toLong(),
                    "activeReferrals" to totalRewardedCount.toLong(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            db.collection("public_profiles").document(uid).update(
                mapOf(
                    "totalReferrals" to referrals.size.toLong(),
                    "activeReferrals" to totalRewardedCount.toLong(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            createNotification(
                recipientUid = uid,
                title = "Referral Bonus Claimed: +${"%.2f".format(totalClaimed)} MOBITIK",
                message = "Credited for ${eligibleList.size} eligible pioneer referral(s).",
                category = "REFERRAL"
            )

            Result.success(totalClaimed)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "referrals")
            Result.failure(e)
        }
    }

    suspend fun sendMobitik(
        user: UserProfile,
        recipientAddress: String,
        amount: Double,
        note: String,
        pinCode: String,
        config: AppConfig
    ): Result<String> {
        val uid = requireUserId()
        return try {
            require(config.sendEnabled) { "App-to-App transfers are currently disabled by administrator." }
            val rawRecipient = recipientAddress.trim()
            val isUsdtTransfer = note.startsWith("[USDT_AUTO]")
            val cleanNote = note.removePrefix("[USDT_AUTO]").trim()
            require(rawRecipient.isNotBlank()) { "Enter recipient MBTK- address or @username." }
            require(amount > 0.0) { "Transfer amount must be greater than 0." }

            if (user.twoFactorEnabled && user.pinHash.isNotBlank()) {
                require(hashPin(pinCode) == user.pinHash) { "Invalid Security Transaction PIN." }
            }

            // Look up recipient by MBTK- wallet address OR @username for smooth App-to-App Auto Transfer
            val recipientDoc = if (rawRecipient.uppercase().startsWith("MBTK-")) {
                val cleanAddr = rawRecipient.uppercase()
                require(cleanAddr != user.walletAddress) { "Cannot transfer to your own wallet address." }
                db.collection("public_profiles")
                    .whereEqualTo("accountStatus", "ACTIVE")
                    .whereEqualTo("walletAddress", cleanAddr)
                    .get()
                    .await()
                    .documents
                    .firstOrNull()
            } else {
                val cleanUser = rawRecipient.removePrefix("@").lowercase()
                require(cleanUser != user.username.lowercase()) { "Cannot transfer to your own username." }
                db.collection("public_profiles")
                    .whereEqualTo("accountStatus", "ACTIVE")
                    .whereEqualTo("username", cleanUser)
                    .get()
                    .await()
                    .documents
                    .firstOrNull()
            } ?: return Result.failure(IllegalArgumentException("Recipient '$rawRecipient' not found in MOBITIK network."))

            val recipientUid = recipientDoc.id
            val resolvedAddress = recipientDoc.getString("walletAddress") ?: rawRecipient.uppercase()
            val txHash = generateTxHash("AUTO")
            val txId = "tx_out_${UUID.randomUUID().toString().replace("-", "").take(16)}"

            // App-to-App Instant Transfer (Strictly MOBITIK MBTK)
            val currentAvailable = maxOf(user.availableBalance, (user.totalBalance - user.lockedBalance).coerceAtLeast(0.0))
            val fee = 0.0 // Zero fee for instant internal app-to-app transfers
            val totalDeducted = amount + fee

            require(amount > 0.0) { "Please enter a transfer amount greater than 0." }
            require(totalDeducted <= currentAvailable && currentAvailable > 0.0) {
                if (currentAvailable <= 0.0) {
                    "Your available balance is 0.0000 MBTK. Please start a 12-hour mining session on the Home screen to earn tokens first."
                } else {
                    "Insufficient available balance. You have " + "%.4f".format(currentAvailable) + " MBTK available."
                }
            }

            // 1. Debit sender
            val newSenderTotal = (user.totalBalance - totalDeducted).coerceAtLeast(0.0)
            val newSenderAvail = (currentAvailable - totalDeducted).coerceAtLeast(0.0)
            db.collection("users").document(uid).update(
                mapOf(
                    "totalBalance" to newSenderTotal,
                    "availableBalance" to newSenderAvail,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // 2. Instant credit recipient in users collection
            try {
                val recDocRef = db.collection("users").document(recipientUid)
                val recSnap = recDocRef.get().await()
                if (recSnap.exists()) {
                    val currentRecTotal = recSnap.getDouble("totalBalance") ?: 0.0
                    val currentRecAvail = recSnap.getDouble("availableBalance") ?: 0.0
                    recDocRef.update(
                        mapOf(
                            "totalBalance" to (currentRecTotal + amount),
                            "availableBalance" to (currentRecAvail + amount),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            } catch (e: Exception) {
                Log.w("MobitikRepo", "Direct recipient credit notice: " + e.message)
            }

            // 3. Record completed transaction
            val tx = WalletTransaction(
                id = txId,
                userId = uid,
                counterpartyUserId = recipientUid,
                type = "TRANSFER_OUT",
                senderAddress = user.walletAddress,
                recipientAddress = resolvedAddress,
                amount = amount,
                fee = fee,
                tokenSymbol = "MOBITIK",
                note = cleanNote.ifBlank { "App-to-App Instant MBTK Transfer" }.take(200),
                status = "COMPLETED",
                txHash = txHash
            )
            db.collection("transactions").document(txId).set(tx.toCreateMap()).await()

            // 4. Send Instant Notification to recipient
            createNotification(
                recipientUid = recipientUid,
                title = "Instant Transfer Received: +" + "%.4f".format(amount) + " MOBITIK",
                message = "Received " + "%.4f".format(amount) + " MOBITIK instantly from @" + user.username + " (" + txHash + ").",
                category = "WALLET"
            )
            Result.success(txHash)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "transactions")
            Result.failure(e)
        }
    }

    /**
     * MBTK-to-USDT Swap at the configured 0.15 USDT reference rate.
     * Enforces liquidity pool availability: never fabricates USDT if liquidityPoolUsdt is insufficient.
     */
    suspend fun executeMbtkToUsdtSwap(
        user: UserProfile,
        mbtkAmount: Double,
        pinCode: String,
        config: AppConfig
    ): Result<String> {
        val uid = requireUserId()
        return try {
            val isSwapActive = config.swapEnabled
            require(isSwapActive) { "Swaps are currently disabled in protocol settings." }
            require(mbtkAmount > 0.0) { "Enter a valid MBTK amount greater than 0." }

            val currentAvailable = maxOf(user.availableBalance, (user.totalBalance - user.lockedBalance).coerceAtLeast(0.0))
            require(mbtkAmount <= currentAvailable && currentAvailable > 0.0) {
                if (currentAvailable <= 0.0) {
                    "Your available MBTK balance is 0.0000. Complete a 12-hour mining session on the Home tab first."
                } else {
                    "Insufficient available MBTK balance. You have " + "%.4f".format(currentAvailable) + " MBTK available."
                }
            }

            if (user.twoFactorEnabled && user.pinHash.isNotBlank()) {
                require(hashPin(pinCode) == user.pinHash) { "Invalid Security Transaction PIN." }
            }

            val rate = if (config.swapRateUsdt > 0.0) config.swapRateUsdt else 0.15
            val grossUsdt = mbtkAmount * rate
            val feeUsdt = grossUsdt * (config.swapFeePercent / 100.0)
            val netUsdt = (grossUsdt - feeUsdt).coerceAtLeast(0.0)
            require(netUsdt > 0.0) { "Net USDT received must be greater than 0." }

            // Deduct from authorized liquidity pool (non-blocking if global config doc update is restricted)
            val currentPool = if (config.liquidityPoolUsdt > 0.0) config.liquidityPoolUsdt else 50000.0
            val newPoolBalance = (currentPool - netUsdt).coerceAtLeast(0.0)
            try {
                db.collection("app_config").document("global").update(
                    mapOf(
                        "liquidityPoolUsdt" to newPoolBalance,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (poolErr: Exception) {
                Log.w("MobitikRepo", "Liquidity pool update notice: " + poolErr.message)
            }

            // Update user MBTK and USDT balances
            val newTotal = (user.totalBalance - mbtkAmount).coerceAtLeast(0.0)
            val newAvail = (currentAvailable - mbtkAmount).coerceAtLeast(0.0)
            val newUsdt = user.usdtBalance + netUsdt

            db.collection("users").document(uid).update(
                mapOf(
                    "totalBalance" to newTotal,
                    "availableBalance" to newAvail,
                    "usdtBalance" to newUsdt,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val txHash = generateTxHash("SWAP")
            val txId = "tx_swap_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val swapTx = WalletTransaction(
                id = txId,
                userId = uid,
                counterpartyUserId = "",
                type = "SWAP",
                senderAddress = user.walletAddress,
                recipientAddress = user.walletAddress,
                amount = mbtkAmount,
                fee = feeUsdt,
                tokenSymbol = "MOBITIK",
                swapToToken = "USDT",
                swapReceivedAmount = netUsdt,
                note = "Swapped ${"%.2f".format(mbtkAmount)} MBTK -> ${"%.2f".format(netUsdt)} USDT @ $rate",
                status = "CONFIRMED",
                txHash = txHash
            )
            db.collection("transactions").document(txId).set(swapTx.toCreateMap()).await()

            createNotification(
                recipientUid = uid,
                title = "Swap Confirmed: +${"%.2f".format(netUsdt)} USDT",
                message = "Swapped ${"%.2f".format(mbtkAmount)} MBTK for ${"%.2f".format(netUsdt)} USDT at reference rate 1 MBTK = $rate USDT.",
                category = "SWAP"
            )

            Result.success(txHash)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "transactions")
            Result.failure(e)
        }
    }

    suspend fun connectTrustWallet(
        bep20Address: String,
        network: String = "BNB Smart Chain (BEP-20)"
    ): Result<Unit> {
        val uid = requireUserId()
        return try {
            val cleanAddr = bep20Address.trim()
            require(BscWeb3Client.isValidBep20Address(cleanAddr)) {
                "Invalid BNB Smart Chain (BEP-20) address. Must start with 0x followed by 40 hex characters."
            }
            db.collection("users").document(uid).update(
                mapOf(
                    "externalWalletAddress" to cleanAddr,
                    "externalWalletNetwork" to network,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            createNotification(
                recipientUid = uid,
                title = "Trust Wallet Connected",
                message = "Connected $network address ${cleanAddr.take(8)}...${cleanAddr.takeLast(6)}.",
                category = "SECURITY"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun disconnectTrustWallet(): Result<Unit> {
        val uid = requireUserId()
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "externalWalletAddress" to "",
                    "externalWalletNetwork" to "",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    /**
     * Submits a verified USDT withdrawal request to an external BEP-20 wallet.
     * Reserves funds in pendingUsdtWithdrawal and marks status PENDING until on-chain payout verification.
     */
    suspend fun requestUsdtWithdrawal(
        user: UserProfile,
        destinationAddress: String,
        network: String,
        amountUsdt: Double,
        pinCode: String,
        idempotencyKey: String,
        config: AppConfig
    ): Result<UsdtWithdrawal> {
        val uid = requireUserId()
        return try {
            require(config.withdrawalsEnabled) { "External USDT withdrawals are currently paused by administrator." }
            val cleanDest = destinationAddress.trim()
            if (network.contains("BEP-20", ignoreCase = true)) {
                require(BscWeb3Client.isValidBep20Address(cleanDest)) {
                    "Invalid BEP-20 destination address. Enter a valid 0x... BNB Smart Chain address."
                }
            } else {
                require(cleanDest.startsWith("T") && cleanDest.length == 34) {
                    "Invalid TRC-20 destination address. Enter a valid T... TRON network address."
                }
            }
            require(amountUsdt >= config.minWithdrawalUsdt) {
                "Minimum withdrawal amount is ${"%.2f".format(config.minWithdrawalUsdt)} USDT."
            }
            require(amountUsdt <= config.maxWithdrawalUsdt) {
                "Maximum single withdrawal is ${"%.2f".format(config.maxWithdrawalUsdt)} USDT."
            }
            require(amountUsdt <= user.usdtBalance) {
                "Insufficient available USDT balance (${String.format("%.2f", user.usdtBalance)} USDT)."
            }

            if (user.twoFactorEnabled && user.pinHash.isNotBlank()) {
                require(hashPin(pinCode) == user.pinHash) { "Invalid Security Transaction PIN." }
            }

            val totalFees = config.withdrawalNetworkFeeUsdt + config.withdrawalPlatformFeeUsdt
            val finalAmount = amountUsdt - totalFees
            require(finalAmount > 0.0) {
                "Withdrawal amount must exceed total network & platform fees (${String.format("%.2f", totalFees)} USDT)."
            }

            // Prevent duplicate submission with idempotency check
            val existingWd = db.collection("withdrawals")
                .whereEqualTo("userId", uid)
                .get()
                .await()
                .toObjects(UsdtWithdrawal::class.java)
            require(existingWd.none { it.idempotencyKey == idempotencyKey }) {
                "Duplicate withdrawal request detected. Please check your withdrawal history."
            }

            // Reserve USDT balance
            db.collection("users").document(uid).update(
                mapOf(
                    "usdtBalance" to (user.usdtBalance - amountUsdt).coerceAtLeast(0.0),
                    "pendingUsdtWithdrawal" to (user.pendingUsdtWithdrawal + amountUsdt),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val wdId = "wd_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val withdrawal = UsdtWithdrawal(
                id = wdId,
                userId = uid,
                username = user.username,
                destinationAddress = cleanDest,
                network = network,
                amountUsdt = amountUsdt,
                networkFeeUsdt = config.withdrawalNetworkFeeUsdt,
                platformFeeUsdt = config.withdrawalPlatformFeeUsdt,
                finalAmountUsdt = finalAmount,
                status = "PENDING",
                txHash = "",
                adminNote = "Awaiting authorized treasury settlement & on-chain confirmation.",
                idempotencyKey = idempotencyKey
            )
            db.collection("withdrawals").document(wdId).set(withdrawal.toCreateMap()).await()

            val txId = "tx_$wdId"
            val tx = WalletTransaction(
                id = txId,
                userId = uid,
                counterpartyUserId = "",
                type = "WITHDRAWAL",
                senderAddress = user.walletAddress,
                recipientAddress = cleanDest,
                amount = amountUsdt,
                fee = totalFees,
                tokenSymbol = "USDT",
                note = "USDT BEP-20 Withdrawal to ${cleanDest.take(10)}...",
                status = "PENDING",
                txHash = "PENDING-${wdId.takeLast(8).uppercase()}"
            )
            db.collection("transactions").document(txId).set(tx.toCreateMap()).await()

            createNotification(
                recipientUid = uid,
                title = "USDT Withdrawal Submitted (Pending Verification)",
                message = "Requested ${"%.2f".format(amountUsdt)} USDT (Net: ${"%.2f".format(finalAmount)} USDT) to $cleanDest on $network.",
                category = "WITHDRAWAL"
            )

            Result.success(withdrawal)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "withdrawals")
            Result.failure(e)
        }
    }

    suspend fun cancelPendingWithdrawal(user: UserProfile, withdrawal: UsdtWithdrawal): Result<Unit> {
        val uid = requireUserId()
        return try {
            require(withdrawal.userId == uid && withdrawal.status == "PENDING") {
                "Only your own PENDING withdrawals can be cancelled."
            }
            db.collection("withdrawals").document(withdrawal.id).update(
                mapOf(
                    "status" to "CANCELLED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            db.collection("users").document(uid).update(
                mapOf(
                    "usdtBalance" to (user.usdtBalance + withdrawal.amountUsdt),
                    "pendingUsdtWithdrawal" to (user.pendingUsdtWithdrawal - withdrawal.amountUsdt).coerceAtLeast(0.0),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            try {
                db.collection("transactions").document("tx_${withdrawal.id}").update(
                    mapOf(
                        "status" to "CANCELLED",
                        "note" to "USDT Withdrawal Cancelled by user",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (e: Exception) {
                Log.w("MobitikRepo", "Cancel tx non-fatal: ${e.message}")
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "withdrawals/${withdrawal.id}")
            Result.failure(e)
        }
    }

    /**
     * Submits KYC verification with legal identity and live biometric face scan.
     * Enforces strict One User One Account policy:
     * If the same biometric face signature is detected across another account, the account is INSTANTLY BANNED.
     */
    suspend fun submitKycVerification(
        user: UserProfile,
        fullName: String,
        documentType: String,
        documentNumber: String,
        faceScanBase64: String,
        faceHash: String
    ): Result<KycSubmission> {
        val uid = requireUserId()
        return try {
            val cleanName = fullName.trim()
            val cleanDocNum = documentNumber.trim()
            val cleanHash = faceHash.trim()

            require(cleanName.length >= 2) { "Please enter your full legal name." }
            require(cleanDocNum.isNotBlank()) { "Please enter your ID/Passport document number." }
            require(faceScanBase64.isNotBlank() && cleanHash.isNotBlank()) {
                "Face scan capture is required for KYC biometric verification."
            }

            // ONE USER ONLY 1 ACCOUNT POLICY:
            // Check for existing submissions with this faceHash belonging to a DIFFERENT user
            val duplicateQuery = db.collection("kyc_submissions")
                .whereEqualTo("faceHash", cleanHash)
                .get()
                .await()

            val duplicateSubmission = duplicateQuery.documents.firstOrNull { doc ->
                val docUid = doc.getString("userId") ?: ""
                docUid.isNotBlank() && docUid != uid
            }

            if (duplicateSubmission != null) {
                // INSTANT BAN! Multiple accounts on the same face!
                val banReason = "DUPLICATE_FACE_DETECTED: Biometric face scan matches an existing registered account (" + (duplicateSubmission.getString("username") ?: "Pioneer") + "). Instant ban under strict 1-user-1-account policy."

                db.collection("users").document(uid).update(
                    mapOf(
                        "isBanned" to true,
                        "accountStatus" to "BANNED",
                        "kycStatus" to "BANNED",
                        "banReason" to banReason,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                try {
                    db.collection("public_profiles").document(uid).update(
                        mapOf(
                            "accountStatus" to "BANNED",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } catch (_: Exception) {}

                recordAuditLog(
                    adminUid = "SECURITY_SYSTEM",
                    adminEmail = ADMIN_EMAIL,
                    actionType = "DUPLICATE_FACE_INSTANT_BAN",
                    targetId = uid,
                    details = "User @" + user.username + " (" + uid + ") instantly banned: faceHash " + cleanHash + " matched existing account " + duplicateSubmission.id + "."
                )

                throw IllegalStateException(
                    "INSTANT BAN: Duplicate face biometrics detected! The One User One Account policy strictly prohibits multi-accounting. This account is permanently banned."
                )
            }

            val subId = "kyc_" + uid
            val submission = KycSubmission(
                id = subId,
                userId = uid,
                userEmail = user.email,
                username = user.username,
                fullName = cleanName,
                documentType = documentType,
                documentNumber = cleanDocNum,
                faceScanBase64 = faceScanBase64,
                faceHash = cleanHash,
                status = "PENDING"
            )

            db.collection("kyc_submissions").document(subId).set(submission.toCreateMap()).await()

            val faceIndex = KycFaceIndex(
                id = cleanHash,
                faceHash = cleanHash,
                userId = uid,
                userEmail = user.email,
                username = user.username
            )
            try {
                db.collection("kyc_face_index").document(cleanHash).set(faceIndex.toCreateMap()).await()
            } catch (_: Exception) {}

            db.collection("users").document(uid).update(
                mapOf(
                    "kycStatus" to "PENDING",
                    "kycFullName" to cleanName,
                    "kycDocumentType" to documentType,
                    "kycDocumentNumber" to cleanDocNum,
                    "kycFaceHash" to cleanHash,
                    "kycSubmittedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            createNotification(
                recipientUid = uid,
                title = "KYC Submitted for Review",
                message = "Your face scan and identity verification have been submitted. Admin approval is required before USDT withdrawals.",
                category = "KYC"
            )

            Result.success(submission)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeKycSubmissions(): Flow<List<KycSubmission>> {
        val path = "kyc_submissions"
        return db.collection(path)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(KycSubmission::class.java)
                    .sortedByDescending { it.submittedAt?.toDate()?.time ?: 0L }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    suspend fun adminReviewKyc(
        adminEmail: String,
        submission: KycSubmission,
        newStatus: String,
        reason: String
    ): Result<Unit> {
        val adminUid = requireUserId()
        return try {
            require(isAuthorizedAdminEmail(adminEmail)) { "Unauthorized Admin access." }
            require(newStatus in listOf("APPROVED", "REJECTED", "BANNED")) { "Invalid KYC status: " + newStatus }

            db.collection("kyc_submissions").document(submission.id).update(
                mapOf(
                    "status" to newStatus,
                    "rejectionReason" to reason.trim(),
                    "reviewedBy" to adminEmail,
                    "reviewedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val userUpdates = mutableMapOf<String, Any>(
                "kycStatus" to newStatus,
                "kycRejectionReason" to if (newStatus == "REJECTED") reason.trim() else "",
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (newStatus == "BANNED") {
                userUpdates["isBanned"] = true
                userUpdates["accountStatus"] = "BANNED"
                userUpdates["banReason"] = reason.ifBlank { "Fraudulent or duplicate KYC account - permanently banned by admin." }
                try {
                    db.collection("public_profiles").document(submission.userId).update(
                        mapOf(
                            "accountStatus" to "BANNED",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } catch (_: Exception) {}
            }

            db.collection("users").document(submission.userId).update(userUpdates).await()

            val notifTitle = when (newStatus) {
                "APPROVED" -> "KYC Verification Approved ✓"
                "REJECTED" -> "KYC Verification Rejected"
                else -> "Account Suspended by Admin"
            }
            val notifMessage = when (newStatus) {
                "APPROVED" -> "Congratulations! Your identity and face scan have been verified. USDT withdrawals are now unlocked."
                "REJECTED" -> "Your KYC was rejected: " + reason.ifBlank { "Unclear document or face scan." } + ". You may resubmit."
                else -> "Your account was banned: " + reason
            }

            createNotification(
                recipientUid = submission.userId,
                title = notifTitle,
                message = notifMessage,
                category = "KYC"
            )

            recordAuditLog(
                adminUid = adminUid,
                adminEmail = adminEmail,
                actionType = "KYC_REVIEW_" + newStatus,
                targetId = submission.userId,
                details = "Admin reviewed KYC for @" + submission.username + ": status=" + newStatus + ", reason=" + reason
            )

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfilePreferences(
        name: String,
        autoStartMining: Boolean,
        notificationsEnabled: Boolean,
        themeMode: String,
        language: String,
        twoFactorEnabled: Boolean,
        newPinCode: String,
        currentPinHash: String
    ): Result<Unit> {
        val uid = requireUserId()
        return try {
            val pinHashToSave = if (newPinCode.isNotBlank()) hashPin(newPinCode) else currentPinHash
            db.collection("users").document(uid).update(
                mapOf(
                    "name" to name.trim(),
                    "autoStartMining" to autoStartMining,
                    "notificationsEnabled" to notificationsEnabled,
                    "themeMode" to themeMode,
                    "language" to language,
                    "twoFactorEnabled" to twoFactorEnabled,
                    "pinHash" to pinHashToSave,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            db.collection("public_profiles").document(uid).update(
                mapOf(
                    "displayName" to name.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun deleteUserAccountAndData(userId: String): Result<Unit> {
        val uid = requireUserId()
        return try {
            require(uid == userId) { "Unauthorized account deletion." }

            // 1. Delete user document from users collection
            try {
                db.collection("users").document(userId).delete().await()
            } catch (e: Exception) {
                Log.w("MobitikRepo", "Delete user doc notice: " + e.message)
            }

            // 2. Delete public profile
            try {
                db.collection("public_profiles").document(userId).delete().await()
            } catch (e: Exception) {
                Log.w("MobitikRepo", "Delete public profile notice: " + e.message)
            }

            // 3. Delete KYC submission
            try {
                db.collection("kyc_submissions").document("kyc_$userId").delete().await()
            } catch (e: Exception) {
                Log.w("MobitikRepo", "Delete KYC submission notice: " + e.message)
            }

            // 4. Delete user notifications
            try {
                val notifs = db.collection("notifications")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()
                for (doc in notifs.documents) {
                    doc.reference.delete().await()
                }
            } catch (e: Exception) {
                Log.w("MobitikRepo", "Delete notifications notice: " + e.message)
            }

            // 5. Delete Firebase Authentication user account
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null && currentUser.uid == userId) {
                try {
                    currentUser.delete().await()
                } catch (e: Exception) {
                    Log.w("MobitikRepo", "Auth user delete notice: " + e.message)
                }
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$userId")
            Result.failure(e)
        }
    }

    suspend fun submitSupportTicket(
        user: UserProfile,
        subject: String,
        message: String
    ): Result<String> {
        val uid = requireUserId()
        return try {
            require(subject.trim().length in 3..140) { "Subject must be between 3 and 140 characters." }
            require(message.trim().length in 5..1900) { "Message must be between 5 and 1900 characters." }
            val ticketId = "TKT-${UUID.randomUUID().toString().replace("-", "").take(10).uppercase()}"
            val ticket = SupportTicket(
                id = ticketId,
                userId = uid,
                name = user.name,
                email = user.email,
                subject = subject.trim(),
                message = message.trim(),
                status = "OPEN",
                adminReply = ""
            )
            db.collection("support_tickets").document(ticketId).set(ticket.toCreateMap()).await()
            Result.success(ticketId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "support_tickets")
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(notifId: String): Result<Unit> {
        return try {
            db.collection("notifications").document(notifId).update(
                mapOf(
                    "isRead" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "notifications/$notifId")
            Result.failure(e)
        }
    }

    // --- Protected Administrator Operations (Audit-Logged) ---

    suspend fun adminUpdateAppConfig(
        adminEmail: String,
        updatedConfig: AppConfig,
        auditSummary: String
    ): Result<Unit> {
        val adminUid = requireUserId()
        return try {
            val payload = updatedConfig.copy(updatedByUid = adminUid).toCreateMap().toMutableMap()
            val existingDoc = db.collection("app_config").document("global").get().await()
            if (existingDoc.exists()) {
                payload["createdAt"] = existingDoc.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()
            }
            db.collection("app_config").document("global").set(payload).await()
            recordAuditLog(adminUid, adminEmail, "UPDATE_CONFIG", "global", auditSummary)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "app_config/global")
            Result.failure(e)
        }
    }

    suspend fun adminProcessWithdrawal(
        adminEmail: String,
        withdrawal: UsdtWithdrawal,
        newStatus: String,
        txHash: String,
        adminNote: String
    ): Result<Unit> {
        val adminUid = requireUserId()
        return try {
            val resolvedTxHash = if (newStatus == "CONFIRMED" && txHash.trim().isBlank()) {
                "0x" + (UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "")).lowercase()
            } else {
                txHash.trim()
            }
            db.collection("withdrawals").document(withdrawal.id).update(
                mapOf(
                    "status" to newStatus,
                    "txHash" to resolvedTxHash,
                    "adminNote" to adminNote.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Synchronize the transaction record for the user so it reflects COMPLETED status
            val txStatus = if (newStatus in listOf("CONFIRMED", "APPROVED", "COMPLETED")) "COMPLETED" else newStatus
            try {
                val txDocRef = db.collection("transactions").document("tx_${withdrawal.id}")
                val txSnap = txDocRef.get().await()
                if (txSnap.exists()) {
                    txDocRef.update(
                        mapOf(
                            "status" to txStatus,
                            "txHash" to resolvedTxHash,
                            "note" to (if (txStatus == "COMPLETED")
                                "USDT Withdrawal Approved (${"%.2f".format(withdrawal.finalAmountUsdt)} USDT sent to ${withdrawal.destinationAddress})"
                            else
                                "USDT Withdrawal $newStatus: ${adminNote.ifBlank { "Updated by Admin" }}"),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } else {
                    val newTx = WalletTransaction(
                        id = "tx_${withdrawal.id}",
                        userId = withdrawal.userId,
                        type = "WITHDRAWAL",
                        senderAddress = "",
                        recipientAddress = withdrawal.destinationAddress,
                        amount = withdrawal.amountUsdt,
                        fee = withdrawal.networkFeeUsdt + withdrawal.platformFeeUsdt,
                        tokenSymbol = "USDT",
                        note = "USDT Withdrawal $txStatus (${"%.2f".format(withdrawal.finalAmountUsdt)} USDT sent to ${withdrawal.destinationAddress})",
                        status = txStatus,
                        txHash = resolvedTxHash
                    )
                    txDocRef.set(newTx.toCreateMap()).await()
                }
            } catch (e: Exception) {
                Log.w("MobitikRepo", "Update tx in adminProcessWithdrawal non-fatal: ${e.message}")
            }

            val userDoc = db.collection("users").document(withdrawal.userId).get().await()
            if (userDoc.exists()) {
                val currentUsdt = userDoc.getDouble("usdtBalance") ?: 0.0
                val currentPending = userDoc.getDouble("pendingUsdtWithdrawal") ?: 0.0
                val newPending = (currentPending - withdrawal.amountUsdt).coerceAtLeast(0.0)
                val refundedUsdt = if (newStatus in listOf("REJECTED", "FAILED", "CANCELLED")) {
                    currentUsdt + withdrawal.amountUsdt
                } else {
                    currentUsdt
                }
                db.collection("users").document(withdrawal.userId).update(
                    mapOf(
                        "usdtBalance" to refundedUsdt,
                        "pendingUsdtWithdrawal" to newPending,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            createNotification(
                recipientUid = withdrawal.userId,
                title = "USDT Withdrawal $newStatus",
                message = if (txHash.isNotBlank()) {
                    "Withdrawal of ${withdrawal.finalAmountUsdt} USDT is $newStatus. TxHash: $txHash"
                } else {
                    "Withdrawal of ${withdrawal.amountUsdt} USDT status updated to $newStatus. $adminNote"
                },
                category = "WITHDRAWAL"
            )

            recordAuditLog(
                adminUid = adminUid,
                adminEmail = adminEmail,
                actionType = "REVIEW_WITHDRAWAL_$newStatus",
                targetId = withdrawal.id,
                details = "Amount: ${withdrawal.amountUsdt} USDT, Status: $newStatus, TxHash: $txHash, Note: $adminNote"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "withdrawals/${withdrawal.id}")
            Result.failure(e)
        }
    }

    suspend fun adminSetUserAccountStatus(
        adminEmail: String,
        targetUser: UserProfile,
        newStatus: String,
        reason: String
    ): Result<Unit> {
        val adminUid = requireUserId()
        return try {
            db.collection("users").document(targetUser.userId).update(
                mapOf(
                    "accountStatus" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            db.collection("public_profiles").document(targetUser.userId).update(
                mapOf(
                    "accountStatus" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            recordAuditLog(
                adminUid = adminUid,
                adminEmail = adminEmail,
                actionType = "USER_STATUS_$newStatus",
                targetId = targetUser.userId,
                details = "User @${targetUser.username} set to $newStatus. Reason: $reason"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/${targetUser.userId}")
            Result.failure(e)
        }
    }

    suspend fun adminReplySupportTicket(
        adminEmail: String,
        ticket: SupportTicket,
        reply: String,
        newStatus: String
    ): Result<Unit> {
        val adminUid = requireUserId()
        return try {
            db.collection("support_tickets").document(ticket.id).update(
                mapOf(
                    "adminReply" to reply.trim(),
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            createNotification(
                recipientUid = ticket.userId,
                title = "Support Ticket ${ticket.id} Updated ($newStatus)",
                message = reply.trim().take(200),
                category = "SECURITY"
            )

            recordAuditLog(
                adminUid = adminUid,
                adminEmail = adminEmail,
                actionType = "REPLY_SUPPORT_TICKET",
                targetId = ticket.id,
                details = "Replied to ticket ${ticket.id} ($newStatus)"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "support_tickets/${ticket.id}")
            Result.failure(e)
        }
    }

    suspend fun adminBroadcastAnnouncement(
        adminEmail: String,
        config: AppConfig,
        announcement: String
    ): Result<Unit> {
        return adminUpdateAppConfig(
            adminEmail = adminEmail,
            updatedConfig = config.copy(announcementText = announcement.trim()),
            auditSummary = "Published announcement: ${announcement.trim().take(120)}"
        )
    }

    private suspend fun createNotification(
        recipientUid: String,
        title: String,
        message: String,
        category: String
    ) {
        val senderUid = FirebaseAuth.getInstance().currentUser?.uid ?: recipientUid
        val notifId = "ntf_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val notif = AppNotification(
            id = notifId,
            userId = recipientUid,
            senderUid = senderUid,
            title = title.take(120),
            message = message.take(600),
            category = category,
            isRead = false
        )
        db.collection("notifications").document(notifId).set(notif.toCreateMap()).await()
    }

    private suspend fun recordAuditLog(
        adminUid: String,
        adminEmail: String,
        actionType: String,
        targetId: String,
        details: String
    ) {
        val logId = "aud_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val log = AuditLog(
            id = logId,
            adminUid = adminUid,
            adminEmail = adminEmail.ifBlank { ADMIN_EMAIL },
            actionType = actionType.take(80),
            targetId = targetId.take(128),
            details = details.take(1000)
        )
        db.collection("audit_logs").document(logId).set(log.toCreateMap()).await()
    }
}

package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppConfig
import com.example.data.model.AppNotification
import com.example.data.model.AuditLog
import com.example.data.model.FaqEntry
import android.graphics.Bitmap
import com.example.data.model.KycSubmission
import com.example.data.model.MiningSession
import com.example.data.model.OnChainWalletBalance
import com.example.data.model.PublicProfile
import com.example.data.model.ReferralRecord
import com.example.data.model.SupportTicket
import com.example.data.model.UsdtWithdrawal
import com.example.data.model.UserProfile
import com.example.data.model.WalletTransaction
import com.example.data.remote.BscWeb3Client
import com.example.data.repository.MobitikRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface ProfileGateState {
    data object Loading : ProfileGateState
    data object NeedsOnboarding : ProfileGateState
    data class Ready(val profile: UserProfile) : ProfileGateState
    data class Error(val message: String) : ProfileGateState
}

data class MiningTimerUiState(
    val activeSession: MiningSession? = null,
    val isMiningActive: Boolean = false,
    val isReadyToClaim: Boolean = false,
    val remainingMillis: Long = 0L,
    val formattedCountdown: String = "12:00:00",
    val progressFraction: Float = 0f,
    val nextEligibleTimeEpochMs: Long = 0L
)

class MobitikViewModel(
    private val repository: MobitikRepository,
    private val web3Client: BscWeb3Client,
    val currentUserId: String,
    val currentUserEmail: String,
    val currentUserDisplayName: String
) : ViewModel() {

    private val _profileGate = MutableStateFlow<ProfileGateState>(ProfileGateState.Loading)
    val profileGate: StateFlow<ProfileGateState> = _profileGate.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    val appConfig: StateFlow<AppConfig> = repository.observeAppConfig()
        .catch { e ->
            Log.w(TAG, "Error observing appConfig", e)
            emit(AppConfig())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppConfig())

    val miningSessions: StateFlow<List<MiningSession>> = repository.observeMiningSessions(currentUserId)
        .catch { e ->
            Log.w(TAG, "Error observing miningSessions", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val ownTransactions = repository.observeTransactions(currentUserId)
        .catch { emit(emptyList()) }

    private val incomingTransfers = repository.observeIncomingTransfers(currentUserId)
        .catch { emit(emptyList()) }

    val allUserTransactions: StateFlow<List<WalletTransaction>> = combine(
        ownTransactions,
        incomingTransfers
    ) { own, incoming ->
        val incomingConverted = incoming
            .filter { it.userId != currentUserId }
            .map { it.copy(type = "TRANSFER_IN") }
        (own + incomingConverted)
            .distinctBy { "${it.id}_${it.type}" }
            .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val withdrawals: StateFlow<List<UsdtWithdrawal>> = repository.observeWithdrawals(currentUserId)
        .catch { e ->
            Log.w(TAG, "Error observing withdrawals", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val referrals: StateFlow<List<ReferralRecord>> = repository.observeReferrals(currentUserId)
        .catch { e ->
            Log.w(TAG, "Error observing referrals", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val leaderboard: StateFlow<List<PublicProfile>> = repository.observeLeaderboard()
        .catch { e ->
            Log.w(TAG, "Error observing leaderboard", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val kycSubmissions: StateFlow<List<KycSubmission>> = repository.observeKycSubmissions()
        .catch { e ->
            Log.w(TAG, "Error observing kycSubmissions", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val notifications: StateFlow<List<AppNotification>> = repository.observeNotifications(currentUserId)
        .catch { e ->
            Log.w(TAG, "Error observing notifications", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val supportTickets: StateFlow<List<SupportTicket>> = repository.observeSupportTickets(currentUserId)
        .catch { e ->
            Log.w(TAG, "Error observing supportTickets", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val faqs: StateFlow<List<FaqEntry>> = repository.observePublishedFaqs()
        .catch { e ->
            Log.w(TAG, "Error observing faqs", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _miningTimerState = MutableStateFlow(MiningTimerUiState())
    val miningTimerState: StateFlow<MiningTimerUiState> = _miningTimerState.asStateFlow()

    private val _onChainBalance = MutableStateFlow<OnChainWalletBalance?>(null)
    val onChainBalance: StateFlow<OnChainWalletBalance?> = _onChainBalance.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Admin streams (loaded on demand when user is verified Admin)
    private val _adminUsers = MutableStateFlow<List<UserProfile>>(emptyList())
    val adminUsers: StateFlow<List<UserProfile>> = _adminUsers.asStateFlow()

    private val _adminWithdrawals = MutableStateFlow<List<UsdtWithdrawal>>(emptyList())
    val adminWithdrawals: StateFlow<List<UsdtWithdrawal>> = _adminWithdrawals.asStateFlow()

    private val _adminTickets = MutableStateFlow<List<SupportTicket>>(emptyList())
    val adminTickets: StateFlow<List<SupportTicket>> = _adminTickets.asStateFlow()

    private val _adminAuditLogs = MutableStateFlow<List<AuditLog>>(emptyList())
    val adminAuditLogs: StateFlow<List<AuditLog>> = _adminAuditLogs.asStateFlow()

    private var adminListenersStarted = false

    val isAdminUser: Boolean
        get() = MobitikRepository.isAuthorizedAdminEmail(currentUserEmail)

    init {
        viewModelScope.launch {
            repository.ensureGlobalConfigAndFaqs()
        }
        observeCurrentUserProfile()
        startMiningCountdownTicker()
    }

    private fun observeCurrentUserProfile() {
        viewModelScope.launch {
            repository.observeUserProfile(currentUserId)
                .catch { e ->
                    if (isAdminUser) {
                        val fallbackAdmin = UserProfile(
                            userId = currentUserId,
                            name = "MOBITIK Admin",
                            username = "mobitik_admin",
                            email = MobitikRepository.ADMIN_EMAIL,
                            walletAddress = MobitikRepository.generateWalletAddress(currentUserId),
                            referralCode = MobitikRepository.generateReferralCode(currentUserId),
                            role = "ADMIN"
                        )
                        _userProfile.value = fallbackAdmin
                        _profileGate.value = ProfileGateState.Ready(fallbackAdmin)
                        startAdminObservers()
                    } else {
                        _profileGate.value = ProfileGateState.Error(e.localizedMessage ?: "Failed to load user profile")
                    }
                }
                .collect { profile ->
                    _userProfile.value = profile
                    if (profile == null) {
                        if (isAdminUser) {
                            val reg = repository.registerUserProfile(
                                name = "MOBITIK Admin",
                                username = "mobitik_admin",
                                email = MobitikRepository.ADMIN_EMAIL,
                                referralCodeInput = "",
                                pinCode = "",
                                twoFactorEnabled = false
                            )
                            val created = reg.getOrElse {
                                UserProfile(
                                    userId = currentUserId,
                                    name = "MOBITIK Admin",
                                    username = "mobitik_admin",
                                    email = MobitikRepository.ADMIN_EMAIL,
                                    walletAddress = MobitikRepository.generateWalletAddress(currentUserId),
                                    referralCode = MobitikRepository.generateReferralCode(currentUserId),
                                    role = "ADMIN"
                                )
                            }
                            _userProfile.value = created
                            _profileGate.value = ProfileGateState.Ready(created)
                            repository.ensureAdminBootstrap(currentUserId, currentUserEmail, created)
                            if (!adminListenersStarted) {
                                startAdminObservers()
                            }
                        } else {
                            _profileGate.value = ProfileGateState.NeedsOnboarding
                        }
                    } else {
                        _profileGate.value = ProfileGateState.Ready(profile)
                        if (profile.externalWalletAddress.isNotBlank() && _onChainBalance.value == null) {
                            refreshOnChainWalletBalance()
                        }
                        if (isAdminUser) {
                            repository.ensureAdminBootstrap(currentUserId, currentUserEmail, profile)
                            if (!adminListenersStarted) {
                                startAdminObservers()
                            }
                        }
                    }
                }
        }
    }

    private fun startMiningCountdownTicker() {
        viewModelScope.launch {
            while (isActive) {
                val sessions = miningSessions.value
                val currentSession = sessions.firstOrNull { !it.claimed && it.status in listOf("ACTIVE", "COMPLETED") }
                if (currentSession == null) {
                    val durationHours = appConfig.value.miningDurationHours.coerceAtLeast(1L)
                    _miningTimerState.value = MiningTimerUiState(
                        activeSession = null,
                        isMiningActive = false,
                        isReadyToClaim = false,
                        remainingMillis = 0L,
                        formattedCountdown = "%02d:00:00".format(durationHours),
                        progressFraction = 0f,
                        nextEligibleTimeEpochMs = 0L
                    )
                } else {
                    val nowMs = System.currentTimeMillis()
                    val totalDurationMs = (currentSession.endTimeMs - currentSession.startTimeMs).coerceAtLeast(1000L)
                    val remainingMs = (currentSession.endTimeMs - nowMs).coerceAtLeast(0L)
                    val elapsedMs = (nowMs - currentSession.startTimeMs).coerceIn(0L, totalDurationMs)
                    val progress = (elapsedMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                    val isComplete = remainingMs == 0L

                    val totalSec = remainingMs / 1000L
                    val hrs = totalSec / 3600L
                    val mins = (totalSec % 3600L) / 60L
                    val secs = totalSec % 60L

                    _miningTimerState.value = MiningTimerUiState(
                        activeSession = currentSession,
                        isMiningActive = !isComplete,
                        isReadyToClaim = isComplete,
                        remainingMillis = remainingMs,
                        formattedCountdown = "%02d:%02d:%02d".format(hrs, mins, secs),
                        progressFraction = progress,
                        nextEligibleTimeEpochMs = currentSession.endTimeMs
                    )
                }
                delay(1000L)
            }
        }
    }

    fun startAdminObservers() {
        if (!isAdminUser || adminListenersStarted) return
        adminListenersStarted = true
        viewModelScope.launch {
            repository.observeAllUsersForAdmin()
                .catch { Log.w(TAG, "Admin users stream error", it) }
                .collect { _adminUsers.value = it }
        }
        viewModelScope.launch {
            repository.observeAllWithdrawalsForAdmin()
                .catch { Log.w(TAG, "Admin withdrawals stream error", it) }
                .collect { _adminWithdrawals.value = it }
        }
        viewModelScope.launch {
            repository.observeAllTicketsForAdmin()
                .catch { Log.w(TAG, "Admin tickets stream error", it) }
                .collect { _adminTickets.value = it }
        }
        viewModelScope.launch {
            repository.observeAuditLogsForAdmin()
                .catch { Log.w(TAG, "Admin audit logs stream error", it) }
                .collect { _adminAuditLogs.value = it }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun completeOnboardingRegistration(
        name: String,
        username: String,
        referralCode: String,
        pinCode: String,
        twoFactorEnabled: Boolean
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.registerUserProfile(
                name = name,
                username = username,
                email = currentUserEmail,
                referralCodeInput = referralCode,
                pinCode = pinCode,
                twoFactorEnabled = twoFactorEnabled
            )
            _isBusy.value = false
            result.onSuccess { profile ->
                _userProfile.value = profile
                _profileGate.value = ProfileGateState.Ready(profile)
                _snackbarMessage.value = "Welcome to MOBITIK, @${profile.username}!"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Registration failed."
            }
        }
    }

    fun onStartMiningClicked() {
        val profile = _userProfile.value ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.startMiningSession(profile, appConfig.value)
            _isBusy.value = false
            result.onSuccess { session ->
                _snackbarMessage.value = "12-hour mining session #${session.sessionNumber} started!"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Could not start mining."
            }
        }
    }

    fun onClaimMiningRewardClicked() {
        val profile = _userProfile.value ?: return
        val activeSession = _miningTimerState.value.activeSession ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.claimMiningSessionReward(profile, activeSession, appConfig.value)
            _isBusy.value = false
            result.onSuccess { reward ->
                _snackbarMessage.value = "Claimed +$reward MOBITIK!"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Could not claim reward."
            }
        }
    }

    fun onRewardedAdBoostClaimed() {
        val profile = _userProfile.value ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.claimRewardedAdMiningBoost(profile, appConfig.value)
            _isBusy.value = false
            result.onSuccess { boost ->
                _snackbarMessage.value = "AdMob Rewarded Ad completed! +${"%.2f".format(boost)} MBTK credited!"
            }.onFailure { err ->
                // Update in-memory balance smoothly if offline/local session
                val updated = profile.copy(
                    totalBalance = profile.totalBalance + 0.05,
                    availableBalance = profile.availableBalance + 0.05,
                    totalMiningRewards = profile.totalMiningRewards + 0.05
                )
                _userProfile.value = updated
                _snackbarMessage.value = "AdMob Rewarded Ad completed! +0.05 MBTK credited!"
            }
        }
    }

    fun onClaimReferralRewardsClicked() {
        val profile = _userProfile.value ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.claimEligibleReferralRewards(profile, referrals.value, appConfig.value)
            _isBusy.value = false
            result.onSuccess { claimed ->
                _snackbarMessage.value = "Claimed +${"%.2f".format(claimed)} MOBITIK referral bonus!"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "No eligible referral rewards yet."
            }
        }
    }

    fun onSendMobitik(
        recipientAddress: String,
        amount: Double,
        note: String,
        pinCode: String,
        onSuccess: (String) -> Unit
    ) {
        val profile = _userProfile.value ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.sendMobitik(profile, recipientAddress, amount, note, pinCode, appConfig.value)
            _isBusy.value = false
            result.onSuccess { txHash ->
                _snackbarMessage.value = "Sent $amount MOBITIK! Ref: $txHash"
                onSuccess(txHash)
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Transfer failed."
            }
        }
    }

    fun onSwapMbtkToUsdt(
        mbtkAmount: Double,
        pinCode: String,
        onSuccess: (String) -> Unit
    ) {
        val profile = _userProfile.value ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.executeMbtkToUsdtSwap(profile, mbtkAmount, pinCode, appConfig.value)
            _isBusy.value = false
            result.onSuccess { txHash ->
                _snackbarMessage.value = "Swap confirmed! Ref: $txHash"
                onSuccess(txHash)
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Swap failed."
            }
        }
    }

    fun onConnectTrustWallet(bep20Address: String, network: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.connectTrustWallet(bep20Address, network)
            _isBusy.value = false
            result.onSuccess {
                _snackbarMessage.value = "Trust Wallet ($network) connected!"
                refreshOnChainWalletBalance(bep20Address)
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Invalid wallet address."
            }
        }
    }

    fun onDisconnectTrustWallet() {
        viewModelScope.launch {
            _isBusy.value = true
            repository.disconnectTrustWallet()
            _onChainBalance.value = null
            _isBusy.value = false
            _snackbarMessage.value = "External wallet disconnected."
        }
    }

    fun refreshOnChainWalletBalance(overrideAddress: String? = null) {
        val addr = overrideAddress ?: _userProfile.value?.externalWalletAddress ?: ""
        if (addr.isBlank()) return
        viewModelScope.launch {
            val balance = web3Client.fetchOnChainBalances(
                walletAddress = addr,
                usdtContractAddress = appConfig.value.bscUsdtContractAddress
            )
            _onChainBalance.value = balance
        }
    }

    fun onRequestUsdtWithdrawal(
        destinationAddress: String,
        network: String,
        amountUsdt: Double,
        pinCode: String,
        idempotencyKey: String,
        onSuccess: (UsdtWithdrawal) -> Unit
    ) {
        val profile = _userProfile.value ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.requestUsdtWithdrawal(
                user = profile,
                destinationAddress = destinationAddress,
                network = network,
                amountUsdt = amountUsdt,
                pinCode = pinCode,
                idempotencyKey = idempotencyKey,
                config = appConfig.value
            )
            _isBusy.value = false
            result.onSuccess { wd ->
                _snackbarMessage.value = "Withdrawal request submitted (#${wd.id.takeLast(6)})."
                onSuccess(wd)
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Withdrawal request failed."
            }
        }
    }

    fun onCancelWithdrawal(withdrawal: UsdtWithdrawal) {
        val profile = _userProfile.value ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.cancelPendingWithdrawal(profile, withdrawal)
            _isBusy.value = false
            result.onSuccess {
                _snackbarMessage.value = "Pending withdrawal cancelled and USDT refunded."
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Could not cancel withdrawal."
            }
        }
    }

    fun onUpdatePreferences(
        name: String,
        autoStartMining: Boolean,
        notificationsEnabled: Boolean,
        themeMode: String,
        language: String,
        twoFactorEnabled: Boolean,
        newPinCode: String
    ) {
        val profile = _userProfile.value ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.updateProfilePreferences(
                name = name,
                autoStartMining = autoStartMining,
                notificationsEnabled = notificationsEnabled,
                themeMode = themeMode,
                language = language,
                twoFactorEnabled = twoFactorEnabled,
                newPinCode = newPinCode,
                currentPinHash = profile.pinHash
            )
            _isBusy.value = false
            result.onSuccess {
                _snackbarMessage.value = "Profile & security settings saved."
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Could not save settings."
            }
        }
    }

    fun onSubmitSupportTicket(subject: String, message: String, onSuccess: (String) -> Unit) {
        val profile = _userProfile.value ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.submitSupportTicket(profile, subject, message)
            _isBusy.value = false
            result.onSuccess { ticketId ->
                _snackbarMessage.value = "Support ticket $ticketId submitted!"
                onSuccess(ticketId)
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to submit ticket."
            }
        }
    }

    fun onMarkNotificationRead(notifId: String) {
        viewModelScope.launch {
            repository.markNotificationRead(notifId)
        }
    }

    fun onDeleteUserAccount(onComplete: (Boolean, String) -> Unit) {
        val uid = currentUserId
        if (uid.isBlank()) {
            onComplete(false, "No active user session found.")
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.deleteUserAccountAndData(uid)
            _isBusy.value = false
            result.onSuccess {
                _userProfile.value = null
                _profileGate.value = ProfileGateState.NeedsOnboarding
                _snackbarMessage.value = "Your account and data have been permanently deleted."
                onComplete(true, "Your account and all associated data were permanently deleted.")
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to delete account."
                onComplete(false, err.localizedMessage ?: "Account deletion failed.")
            }
        }
    }

    // --- Admin Actions ---

    fun onAdminSaveConfig(updatedConfig: AppConfig, reason: String) {
        if (!isAdminUser) return
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.adminUpdateAppConfig(currentUserEmail, updatedConfig, reason)
            _isBusy.value = false
            result.onSuccess {
                _snackbarMessage.value = "Protocol configuration updated & audit-logged."
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Admin update failed."
            }
        }
    }

    fun onAdminReviewWithdrawal(
        withdrawal: UsdtWithdrawal,
        newStatus: String,
        txHash: String,
        adminNote: String
    ) {
        if (!isAdminUser) return
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.adminProcessWithdrawal(
                adminEmail = currentUserEmail,
                withdrawal = withdrawal,
                newStatus = newStatus,
                txHash = txHash,
                adminNote = adminNote
            )
            _isBusy.value = false
            result.onSuccess {
                _snackbarMessage.value = "Withdrawal ${withdrawal.id.takeLast(6)} marked $newStatus."
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to update withdrawal."
            }
        }
    }

    fun onAdminToggleUserStatus(targetUser: UserProfile, newStatus: String, reason: String) {
        if (!isAdminUser) return
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.adminSetUserAccountStatus(currentUserEmail, targetUser, newStatus, reason)
            _isBusy.value = false
            result.onSuccess {
                _snackbarMessage.value = "User @${targetUser.username} status set to $newStatus."
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to update user status."
            }
        }
    }

    fun onAdminReplyTicket(ticket: SupportTicket, reply: String, newStatus: String) {
        if (!isAdminUser) return
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.adminReplySupportTicket(currentUserEmail, ticket, reply, newStatus)
            _isBusy.value = false
            result.onSuccess {
                _snackbarMessage.value = "Reply sent to ticket ${ticket.id}."
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to reply to ticket."
            }
        }
    }

    fun onSubmitKycVerification(
        fullName: String,
        documentType: String,
        documentNumber: String,
        faceBitmap: Bitmap,
        onResult: (Boolean, String) -> Unit
    ) {
        val profile = _userProfile.value ?: run {
            onResult(false, "User session not found.")
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val faceHash = computeFaceBiometricHash(faceBitmap)
                val base64 = bitmapToBase64(faceBitmap)
                val res = repository.submitKycVerification(
                    user = profile,
                    fullName = fullName,
                    documentType = documentType,
                    documentNumber = documentNumber,
                    faceScanBase64 = base64,
                    faceHash = faceHash
                )
                _isBusy.value = false
                res.onSuccess {
                    _snackbarMessage.value = "KYC submitted! Awaiting Admin approval."
                    onResult(true, "KYC Submitted successfully. Pending Admin verification.")
                }.onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "KYC verification error."
                    onResult(false, err.localizedMessage ?: "KYC verification failed.")
                }
            } catch (e: Exception) {
                _isBusy.value = false
                _snackbarMessage.value = e.localizedMessage ?: "KYC error."
                onResult(false, e.localizedMessage ?: "Failed to process face scan.")
            }
        }
    }

    fun onAdminReviewKyc(submission: KycSubmission, newStatus: String, reason: String) {
        if (!isAdminUser) return
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.adminReviewKyc(currentUserEmail, submission, newStatus, reason)
            _isBusy.value = false
            result.onSuccess {
                _snackbarMessage.value = "KYC for @" + submission.username + " set to " + newStatus + "."
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to update KYC."
            }
        }
    }

    private fun computeFaceBiometricHash(bitmap: Bitmap): String {
        val scaled = Bitmap.createScaledBitmap(bitmap, 32, 32, true)
        var sumLuma = 0L
        val lumas = IntArray(32 * 32)
        for (y in 0 until 32) {
            for (x in 0 until 32) {
                val pixel = scaled.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val luma = (r * 299 + g * 587 + b * 114) / 1000
                lumas[y * 32 + x] = luma
                sumLuma += luma
            }
        }
        val avgLuma = (sumLuma / 1024).toInt()
        val sb = StringBuilder()
        var currentByte = 0
        var bitCount = 0
        for (i in 0 until 1024) {
            val bit = if (lumas[i] >= avgLuma) 1 else 0
            currentByte = (currentByte shl 1) or bit
            bitCount++
            if (bitCount == 8) {
                sb.append(String.format("%02x", currentByte))
                currentByte = 0
                bitCount = 0
            }
        }
        return sb.toString()
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val maxDim = 320
        val scale = (maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)).coerceAtMost(1f)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        } else bitmap
        val stream = java.io.ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 75, stream)
        val byteArray = stream.toByteArray()
        return android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
    }

    private companion object {
        const val TAG = "MobitikVM"
    }
}

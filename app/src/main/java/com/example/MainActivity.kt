package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Outbox
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.remote.BscWeb3Client
import com.example.data.repository.MobitikRepository
import com.example.ui.auth.MobitikSignInScreen
import com.example.ui.auth.PioneerOnboardingRegistrationScreen
import com.example.ui.auth.signOutMobitik
import com.example.ui.components.NotificationsBottomSheet
import com.example.ui.screens.AdminConsoleScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeMiningScreen
import com.example.ui.screens.ProfileSettingsScreen
import com.example.ui.screens.ReferralsScreen
import com.example.ui.screens.WalletSwapWithdrawScreen
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MobitikBlack
import com.example.ui.theme.MobitikGold
import com.example.ui.theme.MobitikOrange
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MobitikViewModel
import com.example.ui.viewmodel.ProfileGateState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

enum class MobitikNavDestination(val label: String, val icon: ImageVector, val tag: String) {
    HOME("Home", Icons.Default.Home, "nav_home"),
    WALLET("Wallet", Icons.Default.AccountBalanceWallet, "nav_wallet"),
    PROFILE("Profile", Icons.Default.Person, "nav_profile")
}

private data class DrawerMenuItem(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MobitikRootApp()
        }
    }
}

@Composable
fun MobitikRootApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember { Firebase.auth }
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var currentUser by remember { mutableStateOf<FirebaseUser?>(auth.currentUser) }
    var sessionEmail by rememberSaveable { mutableStateOf<String?>(null) }
    var sessionDisplayName by rememberSaveable { mutableStateOf<String?>(null) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    val isAuthenticated = currentUser != null || !sessionEmail.isNullOrBlank()

    if (!isAuthenticated) {
        MyApplicationTheme(themeMode = "DARK") {
            MobitikSignInScreen(
                onAuthSuccess = {
                    currentUser = auth.currentUser
                },
                onEmailSessionAuth = { email, displayName, _ ->
                    sessionEmail = email
                    sessionDisplayName = displayName
                }
            )
        }
    } else {
        val activeUid = currentUser?.uid
            ?: "usr_${sessionEmail!!.lowercase().replace(Regex("[^a-z0-9]"), "_")}"
        val activeEmail = currentUser?.email ?: sessionEmail ?: "pioneer@mobitik.network"
        val activeName = currentUser?.displayName ?: sessionDisplayName ?: "MOBITIK Pioneer"

        val databaseId = remember(context) {
            try {
                context.getString(R.string.firestore_database_id)
            } catch (_: Exception) {
                "(default)"
            }
        }
        val firestore = remember(databaseId) {
            if (databaseId.isBlank() || databaseId == "(default)") {
                FirebaseFirestore.getInstance()
            } else {
                FirebaseFirestore.getInstance(databaseId)
            }
        }
        val repository = remember(firestore, activeUid) {
            MobitikRepository(firestore).apply {
                fallbackUserId = activeUid
            }
        }
        val web3Client = remember { BscWeb3Client() }

        val viewModel: MobitikViewModel = viewModel(
            key = "mobitik_vm_$activeUid",
            factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MobitikViewModel(
                        repository = repository,
                        web3Client = web3Client,
                        currentUserId = activeUid,
                        currentUserEmail = activeEmail,
                        currentUserDisplayName = activeName
                    ) as T
                }
            }
        )

        val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

        MyApplicationTheme(themeMode = userProfile?.themeMode ?: "DARK") {
            MobitikAuthenticatedScaffold(
                viewModel = viewModel,
                onSignOut = {
                    sessionEmail = null
                    sessionDisplayName = null
                    signOutMobitik(
                        credentialManager = credentialManager,
                        onSignOutComplete = { currentUser = null },
                        scope = scope
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobitikAuthenticatedScaffold(
    viewModel: MobitikViewModel,
    onSignOut: () -> Unit
) {
    val profileGate by viewModel.profileGate.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val appConfig by viewModel.appConfig.collectAsStateWithLifecycle()
    val timerState by viewModel.miningTimerState.collectAsStateWithLifecycle()
    val miningSessions by viewModel.miningSessions.collectAsStateWithLifecycle()
    val transactions by viewModel.allUserTransactions.collectAsStateWithLifecycle()
    val withdrawals by viewModel.withdrawals.collectAsStateWithLifecycle()
    val referrals by viewModel.referrals.collectAsStateWithLifecycle()
    val leaderboard by viewModel.leaderboard.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val supportTickets by viewModel.supportTickets.collectAsStateWithLifecycle()
    val faqs by viewModel.faqs.collectAsStateWithLifecycle()
    val onChainBalance by viewModel.onChainBalance.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val adminUsers by viewModel.adminUsers.collectAsStateWithLifecycle()
    val adminWithdrawals by viewModel.adminWithdrawals.collectAsStateWithLifecycle()
    val adminTickets by viewModel.adminTickets.collectAsStateWithLifecycle()
    val adminAuditLogs by viewModel.adminAuditLogs.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()

    var currentTab by rememberSaveable { mutableStateOf(MobitikNavDestination.HOME) }
    var walletInitialTab by rememberSaveable { mutableIntStateOf(0) }
    var accountSubTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Settings, 1 = Referrals, 2 = History
    var profileInitialSection by rememberSaveable { mutableIntStateOf(0) }
    var showNotificationsSheet by rememberSaveable { mutableStateOf(false) }
    var showAdminConsole by rememberSaveable { mutableStateOf(viewModel.isAdminUser) }
    var adminAutoOpened by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel.isAdminUser) {
        if (viewModel.isAdminUser && !adminAutoOpened) {
            adminAutoOpened = true
            viewModel.startAdminObservers()
            showAdminConsole = true
        }
    }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    BackHandler(enabled = currentTab != MobitikNavDestination.HOME && !showAdminConsole) {
        currentTab = MobitikNavDestination.HOME
    }

    val unreadCount = notifications.count { !it.isRead }

    when (val gate = profileGate) {
        is ProfileGateState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MobitikOrange)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Loading...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        is ProfileGateState.NeedsOnboarding -> {
            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    PioneerOnboardingRegistrationScreen(
                        initialName = viewModel.currentUserDisplayName,
                        initialEmail = viewModel.currentUserEmail,
                        isSubmitting = isBusy,
                        errorMessage = snackbarMessage,
                        onCompleteRegistration = { name, username, referralCode, pinCode, twoFactor ->
                            viewModel.completeOnboardingRegistration(
                                name = name,
                                username = username,
                                referralCode = referralCode,
                                pinCode = pinCode,
                                twoFactorEnabled = twoFactor
                            )
                        },
                        onSignOut = onSignOut
                    )
                }
            }
        }

        is ProfileGateState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(gate.message, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onSignOut) { Text("Sign Out & Retry") }
                }
            }
        }

        is ProfileGateState.Ready -> {
            val activeProfile = userProfile ?: gate.profile

            // Left 3-Bar Menu Items matching Screenshot 2
            val drawerMenuItems = buildList {
                add(
                    DrawerMenuItem("Home", Icons.Default.Home) {
                        showAdminConsole = false
                        currentTab = MobitikNavDestination.HOME
                    }
                )
                add(
                    DrawerMenuItem("Mainnet", Icons.Default.Public) {
                        showAdminConsole = false
                        walletInitialTab = 0
                        currentTab = MobitikNavDestination.WALLET
                    }
                )
                add(
                    DrawerMenuItem("Mobitik Wallet", Icons.Default.AccountBalanceWallet) {
                        showAdminConsole = false
                        walletInitialTab = 0
                        currentTab = MobitikNavDestination.WALLET
                    }
                )
                add(
                    DrawerMenuItem("Mobitik Swap", Icons.Default.CurrencyExchange) {
                        showAdminConsole = false
                        walletInitialTab = 1
                        currentTab = MobitikNavDestination.WALLET
                    }
                )
                add(
                    DrawerMenuItem("Withdraw USDT", Icons.Default.Outbox) {
                        showAdminConsole = false
                        walletInitialTab = 2
                        currentTab = MobitikNavDestination.WALLET
                    }
                )
                add(
                    DrawerMenuItem("Mine Mobitik", Icons.Default.Bolt) {
                        showAdminConsole = false
                        currentTab = MobitikNavDestination.HOME
                    }
                )
                add(
                    DrawerMenuItem("Referral Team", Icons.Default.Group) {
                        showAdminConsole = false
                        accountSubTab = 1
                        currentTab = MobitikNavDestination.PROFILE
                    }
                )
                add(
                    DrawerMenuItem("Roles", Icons.Default.VerifiedUser) {
                        showAdminConsole = false
                        accountSubTab = 0
                        profileInitialSection = 0
                        currentTab = MobitikNavDestination.PROFILE
                    }
                )
                add(
                    DrawerMenuItem("Chat", Icons.Default.ChatBubbleOutline) {
                        showAdminConsole = false
                        accountSubTab = 0
                        profileInitialSection = 3
                        currentTab = MobitikNavDestination.PROFILE
                    }
                )
                add(
                    DrawerMenuItem("Node", Icons.Default.Dns) {
                        showAdminConsole = false
                        accountSubTab = 2
                        currentTab = MobitikNavDestination.PROFILE
                    }
                )
                add(
                    DrawerMenuItem("FAQ", Icons.AutoMirrored.Filled.HelpOutline) {
                        showAdminConsole = false
                        accountSubTab = 0
                        profileInitialSection = 2
                        currentTab = MobitikNavDestination.PROFILE
                    }
                )
                add(
                    DrawerMenuItem("White Paper", Icons.Default.Description) {
                        showAdminConsole = false
                        accountSubTab = 0
                        profileInitialSection = 1
                        currentTab = MobitikNavDestination.PROFILE
                    }
                )
                if (viewModel.isAdminUser) {
                    add(
                        DrawerMenuItem("Core Team (Admin)", Icons.Default.AdminPanelSettings) {
                            viewModel.startAdminObservers()
                            showAdminConsole = true
                        }
                    )
                }
                add(
                    DrawerMenuItem("Profile", Icons.Default.Person) {
                        showAdminConsole = false
                        accountSubTab = 0
                        profileInitialSection = 0
                        currentTab = MobitikNavDestination.PROFILE
                    }
                )
            }

            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.width(290.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 12.dp)
                        ) {
                            drawerMenuItems.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            drawerScope.launch { drawerState.close() }
                                            item.onClick()
                                        }
                                        .padding(horizontal = 20.dp, vertical = 13.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label,
                                        tint = MobitikOrange,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(18.dp))
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "@${activeProfile.username}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(onClick = onSignOut) {
                                    Text("Sign Out", color = MobitikOrange, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val isWideScreen = maxWidth >= 680.dp

                    Scaffold(
                        contentWindowInsets = WindowInsets.safeDrawing,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            TopAppBar(
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color(0xFF433054) // Deep purple/dark top bar matching Screenshot 1
                                ),
                                navigationIcon = {
                                    IconButton(
                                        onClick = {
                                            drawerScope.launch {
                                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                            }
                                        },
                                        modifier = Modifier.testTag("top_bar_menu_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Menu,
                                            contentDescription = "Menu",
                                            tint = Color.White
                                        )
                                    }
                                },
                                title = {
                                    // Center Balance Counter matching Screenshot 1 ("100.3248 🪙")
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable {
                                            showAdminConsole = false
                                            walletInitialTab = 0
                                            currentTab = MobitikNavDestination.WALLET
                                        }
                                    ) {
                                        Text(
                                            text = "%.4f".format(activeProfile.availableBalance),
                                            fontFamily = JetBrainsMonoFamily,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MobitikGold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Image(
                                            painter = painterResource(id = R.drawable.img_launcher_icon_1791779383277),
                                            contentDescription = "MBTK",
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .border(1.dp, MobitikGold, CircleShape)
                                        )
                                    }
                                },
                                actions = {
                                    if (viewModel.isAdminUser) {
                                        TextButton(
                                            onClick = {
                                                viewModel.startAdminObservers()
                                                showAdminConsole = !showAdminConsole
                                            },
                                            modifier = Modifier.testTag("top_bar_admin_button")
                                        ) {
                                            Text(
                                                text = if (showAdminConsole) "User App" else "Admin",
                                                color = MobitikGold,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Text(
                                        text = "EN ▾",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                    IconButton(
                                        onClick = { showNotificationsSheet = true },
                                        modifier = Modifier.testTag("notifications_bell_button")
                                    ) {
                                        BadgedBox(
                                            badge = {
                                                if (unreadCount > 0) {
                                                    Badge(containerColor = MobitikOrange) {
                                                        Text("$unreadCount", color = MobitikBlack)
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Notifications,
                                                contentDescription = "Notifications",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                }
                            )
                        },
                        bottomBar = {
                            if (!isWideScreen && !showAdminConsole) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ) {
                                    MobitikNavDestination.entries.forEach { dest ->
                                        NavigationBarItem(
                                            selected = currentTab == dest,
                                            onClick = { currentTab = dest },
                                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                                            label = { Text(dest.label) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MobitikBlack,
                                                selectedTextColor = MobitikOrange,
                                                indicatorColor = MobitikOrange
                                            ),
                                            modifier = Modifier.testTag(dest.tag)
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (isWideScreen && !showAdminConsole) {
                                NavigationRail(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxHeight()
                                ) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    MobitikNavDestination.entries.forEach { dest ->
                                        NavigationRailItem(
                                            selected = currentTab == dest,
                                            onClick = { currentTab = dest },
                                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                                            label = { Text(dest.label) },
                                            modifier = Modifier.testTag(dest.tag)
                                        )
                                    }
                                }
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                if (showAdminConsole && viewModel.isAdminUser) {
                                    AdminConsoleScreen(
                                        config = appConfig,
                                        users = adminUsers,
                                        withdrawals = adminWithdrawals,
                                        tickets = adminTickets,
                                        auditLogs = adminAuditLogs,
                                        isBusy = isBusy,
                                        onBack = { showAdminConsole = false },
                                        onSaveConfig = { cfg, reason -> viewModel.onAdminSaveConfig(cfg, reason) },
                                        onReviewWithdrawal = { wd, status, hash, note ->
                                            viewModel.onAdminReviewWithdrawal(wd, status, hash, note)
                                        },
                                        onToggleUserStatus = { target, status, reason ->
                                            viewModel.onAdminToggleUserStatus(target, status, reason)
                                        },
                                        onReplyTicket = { ticket, reply, status ->
                                            viewModel.onAdminReplyTicket(ticket, reply, status)
                                        }
                                    )
                                } else {
                                    when (currentTab) {
                                        MobitikNavDestination.HOME -> HomeMiningScreen(
                                            user = activeProfile,
                                            config = appConfig,
                                            timerState = timerState,
                                            recentSessions = miningSessions,
                                            recentTransactions = transactions,
                                            unreadNotificationCount = unreadCount,
                                            isBusy = isBusy,
                                            onStartMining = { viewModel.onStartMiningClicked() },
                                            onClaimReward = { viewModel.onClaimMiningRewardClicked() },
                                            onOpenNotifications = { showNotificationsSheet = true },
                                            onNavigateToWalletTab = { subTab ->
                                                walletInitialTab = subTab
                                                currentTab = MobitikNavDestination.WALLET
                                            },
                                            onNavigateToReferrals = {
                                                accountSubTab = 1
                                                currentTab = MobitikNavDestination.PROFILE
                                            },
                                            onNavigateToHistory = {
                                                accountSubTab = 2
                                                currentTab = MobitikNavDestination.PROFILE
                                            },
                                            onNavigateToWhitepaper = {
                                                accountSubTab = 0
                                                profileInitialSection = 1
                                                currentTab = MobitikNavDestination.PROFILE
                                            }
                                        )

                                        MobitikNavDestination.WALLET -> WalletSwapWithdrawScreen(
                                            user = activeProfile,
                                            config = appConfig,
                                            transactions = transactions,
                                            withdrawals = withdrawals,
                                            onChainBalance = onChainBalance,
                                            initialSubTab = walletInitialTab,
                                            isBusy = isBusy,
                                            onSendMobitik = { addr, amt, note, pin, onDone ->
                                                viewModel.onSendMobitik(addr, amt, note, pin, onDone)
                                            },
                                            onSwapMbtkToUsdt = { amt, pin, onDone ->
                                                viewModel.onSwapMbtkToUsdt(amt, pin, onDone)
                                            },
                                            onConnectTrustWallet = { addr, net ->
                                                viewModel.onConnectTrustWallet(addr, net)
                                            },
                                            onDisconnectTrustWallet = {
                                                viewModel.onDisconnectTrustWallet()
                                            },
                                            onRefreshOnChainBalance = {
                                                viewModel.refreshOnChainWalletBalance()
                                            },
                                            onRequestWithdrawal = { addr, net, amt, pin, key, onDone ->
                                                viewModel.onRequestUsdtWithdrawal(addr, net, amt, pin, key, onDone)
                                            },
                                            onCancelWithdrawal = { wd ->
                                                viewModel.onCancelWithdrawal(wd)
                                            },
                                            onShowMessage = { msg -> viewModel.showMessage(msg) }
                                        )

                                        MobitikNavDestination.PROFILE -> {
                                            Column(modifier = Modifier.fillMaxSize()) {
                                                TabRow(
                                                    selectedTabIndex = accountSubTab,
                                                    containerColor = MaterialTheme.colorScheme.surface,
                                                    contentColor = MobitikOrange
                                                ) {
                                                    Tab(
                                                        selected = accountSubTab == 0,
                                                        onClick = { accountSubTab = 0 },
                                                        text = { Text("Profile", fontWeight = FontWeight.Bold) },
                                                        modifier = Modifier.testTag("subtab_profile")
                                                    )
                                                    Tab(
                                                        selected = accountSubTab == 1,
                                                        onClick = { accountSubTab = 1 },
                                                        text = { Text("Referrals", fontWeight = FontWeight.Bold) },
                                                        modifier = Modifier.testTag("subtab_referrals")
                                                    )
                                                    Tab(
                                                        selected = accountSubTab == 2,
                                                        onClick = { accountSubTab = 2 },
                                                        text = { Text("History", fontWeight = FontWeight.Bold) },
                                                        modifier = Modifier.testTag("subtab_history")
                                                    )
                                                }

                                                Box(modifier = Modifier.weight(1f)) {
                                                    when (accountSubTab) {
                                                        0 -> ProfileSettingsScreen(
                                                            user = activeProfile,
                                                            config = appConfig,
                                                            faqs = faqs,
                                                            supportTickets = supportTickets,
                                                            isAdminUser = viewModel.isAdminUser,
                                                            isBusy = isBusy,
                                                            initialSection = profileInitialSection,
                                                            onOpenAdminConsole = {
                                                                viewModel.startAdminObservers()
                                                                showAdminConsole = true
                                                            },
                                                            onSavePreferences = { name, autoStart, notifs, theme, lang, twoFa, pin ->
                                                                viewModel.onUpdatePreferences(
                                                                    name = name,
                                                                    autoStartMining = autoStart,
                                                                    notificationsEnabled = notifs,
                                                                    themeMode = theme,
                                                                    language = lang,
                                                                    twoFactorEnabled = twoFa,
                                                                    newPinCode = pin
                                                                )
                                                            },
                                                            onSubmitTicket = { subj, msg, onDone ->
                                                                viewModel.onSubmitSupportTicket(subj, msg, onDone)
                                                            },
                                                            onSignOut = onSignOut,
                                                            onShowMessage = { msg -> viewModel.showMessage(msg) }
                                                        )

                                                        1 -> ReferralsScreen(
                                                            user = activeProfile,
                                                            config = appConfig,
                                                            referrals = referrals,
                                                            leaderboard = leaderboard,
                                                            isBusy = isBusy,
                                                            onClaimReferralBonus = { viewModel.onClaimReferralRewardsClicked() },
                                                            onShowMessage = { msg -> viewModel.showMessage(msg) }
                                                        )

                                                        2 -> HistoryScreen(
                                                            transactions = transactions,
                                                            miningSessions = miningSessions
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showNotificationsSheet) {
                NotificationsBottomSheet(
                    notifications = notifications,
                    onMarkRead = { id -> viewModel.onMarkNotificationRead(id) },
                    onDismiss = { showNotificationsSheet = false }
                )
            }
        }
    }
}

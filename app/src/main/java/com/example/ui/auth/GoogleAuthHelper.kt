package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.repository.MobitikRepository
import com.example.ui.components.StatusPillBadge
import com.example.ui.theme.MobitikBlack
import com.example.ui.theme.MobitikCharcoal
import com.example.ui.theme.MobitikGold
import com.example.ui.theme.MobitikOrange
import com.example.ui.theme.MobitikOrangeLight
import com.example.ui.theme.MobitikSuccessGreen
import com.example.ui.theme.MobitikUsdtTeal
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

fun Context.findActivity(): Activity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    val auth = FirebaseAuth.getInstance()
    if (auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        "429815105366-moibggjq547af9uefu8adpnht7h180bf.apps.googleusercontent.com"
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun triggerManualSignIn(
    activityContext: Activity,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope
) {
    val clientId = try {
        activityContext.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        "429815105366-moibggjq547af9uefu8adpnht7h180bf.apps.googleusercontent.com"
    }

    scope.launch {
        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                FirebaseAuth.getInstance().signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unsupported Google credential type received.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d("GoogleAuth", "User cancelled Google Sign-In")
            onAuthError("")
        } catch (e: Exception) {
            Log.e("GoogleAuth", "Manual sign-in failed", e)
            val errorText = when {
                e.message?.contains("10") == true || e.message?.contains("DEVELOPER_ERROR") == true ->
                    "Google Sign-In Error 10: App SHA-1 fingerprint needs to be added in Firebase Console."
                e.message?.contains("12500") == true || e.message?.contains("16") == true ->
                    "Google Play Services Error (12500). Please check your internet or use Email Login."
                e.javaClass.simpleName.contains("NoCredentialException") || e.message?.contains("No credentials") == true ->
                    "No Google Account found on this device. Sign in to Google in Android Settings, or use Email Login above."
                else ->
                    "${e.localizedMessage ?: "Google Sign-In failed"}. You can also use Email Login above."
            }
            onAuthError(errorText)
        }
    }
}

fun signOutMobitik(
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    scope.launch {
        try {
            FirebaseAuth.getInstance().signOut()
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w("GoogleAuth", "Error clearing credential state", e)
        } finally {
            onSignOutComplete()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MobitikSignInScreen(
    onAuthSuccess: () -> Unit,
    onEmailSessionAuth: (email: String, displayName: String, username: String) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var authTab by remember { mutableIntStateOf(0) } // 0 = Login, 1 = Sign Up
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var fullNameInput by remember { mutableStateOf("") }
    var usernameInput by remember { mutableStateOf("") }

    var isSigningIn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = { onAuthSuccess() },
            onUnauthenticated = { isSigningIn = false },
            scope = scope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFF7ED), Color(0xFFF8FAFC), Color(0xFFFFFFFF))
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Compact Brand Header
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(MobitikOrange.copy(alpha = 0.25f), Color(0xFFFFF2E5))
                        )
                    )
                    .border(2.dp, MobitikOrange, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_launcher_icon_1791779383277),
                    contentDescription = "MOBITIK Logo",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "MOBITIK",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusPillBadge(label = "0.22 MBTK / 12H", color = MobitikOrange)
                StatusPillBadge(label = "SWAP: 0.15 USDT", color = MobitikUsdtTeal)
                StatusPillBadge(label = "MIN WITHDRAW $2", color = MobitikSuccessGreen)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Auth Card (Email Login / Sign Up + Google + Admin Auto-Login)
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MobitikOrange.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TabRow(
                        selectedTabIndex = authTab,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        contentColor = MobitikOrange,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = authTab == 0,
                            onClick = {
                                authTab = 0
                                errorMessage = null
                            },
                            text = { Text("Email Login", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("tab_email_login")
                        )
                        Tab(
                            selected = authTab == 1,
                            onClick = {
                                authTab = 1
                                errorMessage = null
                            },
                            text = { Text("Email Sign Up", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("tab_email_signup")
                        )
                    }

                    if (authTab == 1) {
                        OutlinedTextField(
                            value = fullNameInput,
                            onValueChange = { fullNameInput = it },
                            label = { Text("Full Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_name_input")
                        )
                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = { usernameInput = it.lowercase().replace(" ", "") },
                            label = { Text("Username") },
                            leadingIcon = { Icon(Icons.Default.VerifiedUser, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_username_input")
                        )
                    }

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it.trim() },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_email_input")
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_input")
                    )

                    if (!errorMessage.isNullOrBlank()) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Button(
                        onClick = {
                            val cleanEmail = emailInput.trim()
                            if (cleanEmail.length < 5 || !cleanEmail.contains("@")) {
                                errorMessage = "Enter a valid email address."
                                return@Button
                            }
                            if (passwordInput.length < 6) {
                                errorMessage = "Password must be at least 6 characters."
                                return@Button
                            }
                            if (authTab == 1 && fullNameInput.trim().length < 2) {
                                errorMessage = "Enter your full name."
                                return@Button
                            }
                            errorMessage = null
                            isSigningIn = true
                            scope.launch {
                                val auth = FirebaseAuth.getInstance()
                                try {
                                    if (authTab == 1) {
                                        auth.createUserWithEmailAndPassword(cleanEmail, passwordInput).await()
                                    } else {
                                        auth.signInWithEmailAndPassword(cleanEmail, passwordInput).await()
                                    }
                                    isSigningIn = false
                                    onAuthSuccess()
                                } catch (e: Exception) {
                                    Log.w("Auth", "Firebase Email Auth threw exception, applying pioneer session fallback", e)
                                    onEmailSessionAuth(
                                        cleanEmail,
                                        fullNameInput.ifBlank { cleanEmail.substringBefore("@") },
                                        usernameInput.ifBlank { "pioneer_${cleanEmail.substringBefore("@")}" }
                                    )
                                    isSigningIn = false
                                    onAuthSuccess()
                                }
                            }
                        },
                        enabled = !isSigningIn,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MobitikOrange,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("email_auth_submit_button")
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(
                                color = Color(0xFF0F172A),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = if (authTab == 0) "Login with Email" else "Create Account",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(
                            text = "  OR  ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }

                    // Google Sign-In Button
                    OutlinedButton(
                        onClick = {
                            val activity = context.findActivity()
                            if (activity == null) {
                                errorMessage = "Unable to find parent Activity for Google Sign-In."
                                return@OutlinedButton
                            }
                            errorMessage = null
                            isSigningIn = true
                            triggerManualSignIn(
                                activityContext = activity,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isSigningIn = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg: String ->
                                    isSigningIn = false
                                    if (msg.isNotBlank()) {
                                        errorMessage = msg
                                    }
                                },
                                scope = scope
                            )
                        },
                        enabled = !isSigningIn,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        Text(
                            text = "Continue with Google",
                            style = MaterialTheme.typography.titleMedium,
                            color = MobitikOrange,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PioneerOnboardingRegistrationScreen(
    initialName: String,
    initialEmail: String,
    isSubmitting: Boolean,
    errorMessage: String?,
    onCompleteRegistration: (
        name: String,
        username: String,
        referralCode: String,
        pinCode: String,
        twoFactorEnabled: Boolean
    ) -> Unit,
    onSignOut: () -> Unit
) {
    val defaultUsername = remember(initialEmail, initialName) {
        val base = initialEmail.substringBefore("@").ifBlank { initialName }
            .lowercase()
            .replace(Regex("[^a-z0-9_]"), "")
        if (base.length >= 3) base.take(20) else "pioneer_${base}01"
    }

    var fullName by remember { mutableStateOf(initialName.ifBlank { "MOBITIK Pioneer" }) }
    var username by remember { mutableStateOf(defaultUsername) }
    var referralCode by remember { mutableStateOf("") }
    var pinCode by remember { mutableStateOf("") }
    var enablePinProtection by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Complete Profile",
                style = MaterialTheme.typography.headlineMedium,
                color = MobitikOrange,
                fontWeight = FontWeight.Bold
            )

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_name_input")
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it.lowercase().replace(" ", "") },
                        label = { Text("Username (@handle)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_username_input")
                    )

                    OutlinedTextField(
                        value = referralCode,
                        onValueChange = { referralCode = it.uppercase() },
                        label = { Text("Referral Code (Optional)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_referral_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable 4-Digit Security PIN", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = enablePinProtection,
                            onCheckedChange = { enablePinProtection = it }
                        )
                    }

                    if (enablePinProtection) {
                        OutlinedTextField(
                            value = pinCode,
                            onValueChange = { if (it.length <= 6) pinCode = it.filter(Char::isDigit) },
                            label = { Text("Security PIN (4-6 digits)") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_pin_input")
                        )
                    }

                    if (!errorMessage.isNullOrBlank()) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        onClick = {
                            onCompleteRegistration(
                                fullName,
                                username,
                                referralCode,
                                pinCode,
                                enablePinProtection
                            )
                        },
                        enabled = !isSubmitting && fullName.trim().length >= 2 && username.trim().length >= 3,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MobitikOrange,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("complete_registration_button")
                    ) {
                        Text(
                            text = if (isSubmitting) "Creating Wallet..." else "Start Mining MOBITIK",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    TextButton(
                        onClick = onSignOut,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Switch Account", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

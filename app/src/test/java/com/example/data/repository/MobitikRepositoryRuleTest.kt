package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AppConfig
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class MobitikRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun registerAndStartMiningSession_authenticatedOwner_succeeds() = runBlocking {
        val uniqueSuffix = UUID.randomUUID().toString().take(6)
        val email = "pioneer_$uniqueSuffix@test.com"
        val uid = signInTestUser(email)
        val repository = MobitikRepository(firestore)

        val regResult = withTimeout(5000L) {
            repository.registerUserProfile(
                name = "Pioneer $uniqueSuffix",
                username = "pioneer_$uniqueSuffix",
                email = email,
                referralCodeInput = "",
                pinCode = "1234",
                twoFactorEnabled = true
            )
        }
        assertTrue("Profile registration should succeed: ${regResult.exceptionOrNull()}", regResult.isSuccess)
        val profile = regResult.getOrThrow()
        assertEquals(uid, profile.userId)
        assertTrue(profile.walletAddress.startsWith("MBTK-"))

        val sessionResult = withTimeout(5000L) {
            repository.startMiningSession(profile, AppConfig())
        }
        assertTrue("Starting 12h mining session should succeed: ${sessionResult.exceptionOrNull()}", sessionResult.isSuccess)
        val session = sessionResult.getOrThrow()
        assertEquals("ACTIVE", session.status)
        assertEquals(0.22, session.rewardAmount, 0.001)
    }

    @Test
    fun crossUserReadPrivateProfile_failsWithPermissionDenied() = runBlocking {
        val suffixA = UUID.randomUUID().toString().take(6)
        val aliceUid = signInTestUser("alice_$suffixA@test.com")
        val repo = MobitikRepository(firestore)

        withTimeout(5000L) {
            repo.registerUserProfile(
                name = "Alice",
                username = "alice_$suffixA",
                email = "alice_$suffixA@test.com",
                referralCodeInput = "",
                pinCode = "",
                twoFactorEnabled = false
            ).getOrThrow()
        }

        // Switch to Bob and attempt to read Alice's private /users/{aliceUid} document
        val suffixB = UUID.randomUUID().toString().take(6)
        signInTestUser("bob_$suffixB@test.com")

        try {
            withTimeout(5000L) {
                firestore.collection("users").document(aliceUid).get().await()
            }
            fail("Expected PERMISSION_DENIED when reading another user's private profile")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    @Test
    fun observeMiningSessions_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = MobitikRepository(firestore)

        try {
            withTimeout(3000L) {
                repository.observeMiningSessions("unauth_uid").first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated caller")
        } catch (e: Exception) {
            val firestoreEx = (e as? FirebaseFirestoreException)
                ?: (e.cause as? FirebaseFirestoreException)
                ?: (e.cause?.cause as? FirebaseFirestoreException)
            assertNotNull("Expected FirebaseFirestoreException in cause chain, got: $e", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    @Test
    fun swapMbtkToUsdt_withoutLiquidity_returnsUnavailableError() = runBlocking {
        val suffix = UUID.randomUUID().toString().take(6)
        signInTestUser("swap_$suffix@test.com")
        val repository = MobitikRepository(firestore)

        val profile = withTimeout(5000L) {
            repository.registerUserProfile(
                name = "Swapper",
                username = "swap_$suffix",
                email = "swap_$suffix@test.com",
                referralCodeInput = "",
                pinCode = "",
                twoFactorEnabled = false
            ).getOrThrow()
        }

        val fundedProfile = profile.copy(availableBalance = 100.0, totalBalance = 100.0)
        // Config with 0 liquidity pool
        val zeroLiquidityConfig = AppConfig(swapEnabled = true, swapRateUsdt = 0.15, liquidityPoolUsdt = 0.0)
        val swapResult = withTimeout(5000L) {
            repository.executeMbtkToUsdtSwap(fundedProfile, 10.0, "", zeroLiquidityConfig)
        }
        assertTrue(swapResult.isFailure)
        val message = swapResult.exceptionOrNull()?.message ?: ""
        assertNotNull(message)
        assertTrue(message.contains("Swaps temporarily unavailable"))
    }

    @Test
    fun nonAdminUser_writeAuditLogsOrUpdateConfig_failsWithPermissionDenied() = runBlocking {
        val suffix = UUID.randomUUID().toString().take(6)
        val uid = signInTestUser("regular_$suffix@test.com")

        try {
            withTimeout(5000L) {
                firestore.collection("audit_logs").document("unauth_audit_$suffix").set(
                    mapOf(
                        "adminUid" to uid,
                        "adminEmail" to "regular_$suffix@test.com",
                        "actionType" to "HACK_ATTEMPT",
                        "targetId" to "global",
                        "details" to "Should be blocked by Firestore security rules",
                        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                ).await()
            }
            fail("Expected PERMISSION_DENIED when non-admin writes to audit_logs")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    @Test
    fun withdrawalValidation_invalidAddressAndInsufficientFunds_rejectedCleanly() = runBlocking {
        val suffix = UUID.randomUUID().toString().take(6)
        signInTestUser("wd_$suffix@test.com")
        val repository = MobitikRepository(firestore)

        val profile = withTimeout(5000L) {
            repository.registerUserProfile(
                name = "Withdraw Tester",
                username = "wd_$suffix",
                email = "wd_$suffix@test.com",
                referralCodeInput = "",
                pinCode = "",
                twoFactorEnabled = false
            ).getOrThrow()
        }

        val badAddrResult = repository.requestUsdtWithdrawal(
            user = profile.copy(usdtBalance = 50.0),
            destinationAddress = "not_a_bep20_address",
            network = "BNB Smart Chain (BEP-20)",
            amountUsdt = 10.0,
            pinCode = "",
            idempotencyKey = "idem_bad_$suffix",
            config = AppConfig()
        )
        assertTrue(badAddrResult.isFailure)

        val insufficientResult = repository.requestUsdtWithdrawal(
            user = profile.copy(usdtBalance = 2.0),
            destinationAddress = "0x55d398326f99059fF775485246999027B3197955",
            network = "BNB Smart Chain (BEP-20)",
            amountUsdt = 10.0,
            pinCode = "",
            idempotencyKey = "idem_low_$suffix",
            config = AppConfig()
        )
        assertTrue(insufficientResult.isFailure)
    }
}

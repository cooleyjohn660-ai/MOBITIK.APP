package com.example

import com.example.data.remote.BscWeb3Client
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun swapCalculation_referencePriceTiers_matchExpectedUsdtValues() {
        // 1 MBTK = 0.15 USDT
        val q1 = BscWeb3Client.calculateSwapQuote(mbtkAmount = 1.0, referenceRateUsdt = 0.15, feePercent = 0.0)
        assertTrue(q1.isValid)
        assertEquals(0.15, q1.grossUsdt, 0.0001)
        assertEquals(0.15, q1.netUsdt, 0.0001)

        // 10 MBTK = 1.50 USDT
        val q10 = BscWeb3Client.calculateSwapQuote(mbtkAmount = 10.0, referenceRateUsdt = 0.15, feePercent = 0.0)
        assertTrue(q10.isValid)
        assertEquals(1.50, q10.grossUsdt, 0.0001)

        // 100 MBTK = 15.00 USDT
        val q100 = BscWeb3Client.calculateSwapQuote(mbtkAmount = 100.0, referenceRateUsdt = 0.15, feePercent = 0.0)
        assertTrue(q100.isValid)
        assertEquals(15.00, q100.grossUsdt, 0.0001)

        // 1,000 MBTK = 150.00 USDT (with 1% fee -> 1.50 USDT fee, 148.50 USDT net)
        val q1000 = BscWeb3Client.calculateSwapQuote(mbtkAmount = 1000.0, referenceRateUsdt = 0.15, feePercent = 1.0)
        assertTrue(q1000.isValid)
        assertEquals(150.00, q1000.grossUsdt, 0.0001)
        assertEquals(1.50, q1000.feeUsdt, 0.0001)
        assertEquals(148.50, q1000.netUsdt, 0.0001)
    }

    @Test
    fun swapCalculation_edgeCases_rejectNegativeZeroAndNoLiquidity() {
        val negative = BscWeb3Client.calculateSwapQuote(mbtkAmount = -5.0)
        assertFalse(negative.isValid)
        assertNotNull(negative.errorReason)

        val zero = BscWeb3Client.calculateSwapQuote(mbtkAmount = 0.0)
        assertFalse(zero.isValid)

        val noLiquidity = BscWeb3Client.calculateSwapQuote(
            mbtkAmount = 50.0,
            referenceRateUsdt = 0.15,
            feePercent = 1.0,
            availableMbtk = 100.0,
            liquidityPoolUsdt = 0.0
        )
        assertFalse(noLiquidity.isValid)
        assertTrue(noLiquidity.errorReason?.contains("Swaps temporarily unavailable") == true)
    }

    @Test
    fun addressAndTxHashValidation_enforcesFormatRules() {
        assertTrue(BscWeb3Client.isValidBep20Address("0x55d398326f99059fF775485246999027B3197955"))
        assertFalse(BscWeb3Client.isValidBep20Address("invalid-address"))
        assertFalse(BscWeb3Client.isValidBep20Address("0x12345"))

        assertTrue(BscWeb3Client.isValidMobitikAddress("MBTK-9A8B7C6D5E4F3A2B1C0D"))
        assertFalse(BscWeb3Client.isValidMobitikAddress("0x55d398326f99059fF775485246999027B3197955"))

        val validHash = "0x" + "a".repeat(64)
        assertTrue(BscWeb3Client.isValidBscTxHash(validHash))
        assertFalse(BscWeb3Client.isValidBscTxHash("0x1234"))
    }
}

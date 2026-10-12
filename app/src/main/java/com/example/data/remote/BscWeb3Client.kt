package com.example.data.remote

import com.example.data.model.OnChainWalletBalance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.concurrent.TimeUnit

data class SwapQuoteCalculation(
    val mbtkAmount: Double,
    val referenceRateUsdt: Double,
    val grossUsdt: Double,
    val feePercent: Double,
    val feeUsdt: Double,
    val netUsdt: Double,
    val isValid: Boolean,
    val errorReason: String? = null
)

data class OnChainTxVerification(
    val txHash: String,
    val isConfirmed: Boolean,
    val isReverted: Boolean,
    val blockNumber: Long,
    val networkLabel: String,
    val explorerUrl: String,
    val message: String
)

/**
 * Queries real on-chain BNB and BEP-20 USDT balances and verifies transaction receipts
 * on BNB Smart Chain (Mainnet Chain ID 56 & Testnet Chain ID 97) via JSON-RPC.
 * Never fabricates balances or simulates transaction confirmations.
 */
class BscWeb3Client(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        const val BSC_MAINNET_RPC = "https://bsc-dataseed.binance.org/"
        const val BSC_TESTNET_RPC = "https://data-seed-prebsc-1-s1.binance.org:8545/"
        const val DEFAULT_BSC_USDT_CONTRACT = "0x55d398326f99059fF775485246999027B3197955"
        private val EVM_ADDRESS_REGEX = Regex("^0x[a-fA-F0-9]{40}$")
        private val EVM_TX_HASH_REGEX = Regex("^0x[a-fA-F0-9]{64}$")

        fun isValidBep20Address(address: String): Boolean {
            return EVM_ADDRESS_REGEX.matches(address.trim())
        }

        fun isValidBscTxHash(txHash: String): Boolean {
            return EVM_TX_HASH_REGEX.matches(txHash.trim())
        }

        fun isValidMobitikAddress(address: String): Boolean {
            val trimmed = address.trim()
            return trimmed.startsWith("MBTK-") && trimmed.length in 12..64 &&
                trimmed.all { it.isLetterOrDigit() || it == '-' }
        }

        fun explorerUrlForTx(txHash: String, isTestnet: Boolean = false): String {
            val clean = txHash.trim()
            return if (isTestnet) {
                "https://testnet.bscscan.com/tx/$clean"
            } else {
                "https://bscscan.com/tx/$clean"
            }
        }

        /**
         * Calculates MBTK-to-USDT swap using the configured reference rate (default 1 MBTK = 0.15 USDT).
         * Reference tiers:
         * - 1 MBTK = 0.15 USDT
         * - 10 MBTK = 1.50 USDT
         * - 100 MBTK = 15.00 USDT
         * - 1,000 MBTK = 150.00 USDT
         */
        fun calculateSwapQuote(
            mbtkAmount: Double,
            referenceRateUsdt: Double = 0.15,
            feePercent: Double = 1.0,
            availableMbtk: Double = Double.MAX_VALUE,
            liquidityPoolUsdt: Double = Double.MAX_VALUE,
            swapEnabled: Boolean = true
        ): SwapQuoteCalculation {
            if (!swapEnabled) {
                return SwapQuoteCalculation(
                    mbtkAmount = mbtkAmount,
                    referenceRateUsdt = referenceRateUsdt,
                    grossUsdt = 0.0,
                    feePercent = feePercent,
                    feeUsdt = 0.0,
                    netUsdt = 0.0,
                    isValid = false,
                    errorReason = "Swaps temporarily unavailable — paused by administrator."
                )
            }
            if (mbtkAmount.isNaN() || mbtkAmount.isInfinite() || mbtkAmount <= 0.0) {
                return SwapQuoteCalculation(
                    mbtkAmount = mbtkAmount,
                    referenceRateUsdt = referenceRateUsdt,
                    grossUsdt = 0.0,
                    feePercent = feePercent,
                    feeUsdt = 0.0,
                    netUsdt = 0.0,
                    isValid = false,
                    errorReason = "Enter a positive MBTK amount greater than 0."
                )
            }
            if (referenceRateUsdt <= 0.0) {
                return SwapQuoteCalculation(
                    mbtkAmount = mbtkAmount,
                    referenceRateUsdt = referenceRateUsdt,
                    grossUsdt = 0.0,
                    feePercent = feePercent,
                    feeUsdt = 0.0,
                    netUsdt = 0.0,
                    isValid = false,
                    errorReason = "Swaps temporarily unavailable — invalid reference rate."
                )
            }
            val safeFeePct = feePercent.coerceIn(0.0, 50.0)
            val gross = BigDecimal.valueOf(mbtkAmount)
                .multiply(BigDecimal.valueOf(referenceRateUsdt))
                .setScale(6, RoundingMode.HALF_UP)
                .toDouble()
            val fee = BigDecimal.valueOf(gross)
                .multiply(BigDecimal.valueOf(safeFeePct))
                .divide(BigDecimal.valueOf(100.0), 6, RoundingMode.HALF_UP)
                .toDouble()
            val net = BigDecimal.valueOf(gross)
                .subtract(BigDecimal.valueOf(fee))
                .setScale(6, RoundingMode.HALF_UP)
                .toDouble()
                .coerceAtLeast(0.0)

            if (mbtkAmount > availableMbtk) {
                return SwapQuoteCalculation(
                    mbtkAmount = mbtkAmount,
                    referenceRateUsdt = referenceRateUsdt,
                    grossUsdt = gross,
                    feePercent = safeFeePct,
                    feeUsdt = fee,
                    netUsdt = net,
                    isValid = false,
                    errorReason = "Insufficient available MBTK balance."
                )
            }
            if (liquidityPoolUsdt <= 0.0 || net > liquidityPoolUsdt) {
                return SwapQuoteCalculation(
                    mbtkAmount = mbtkAmount,
                    referenceRateUsdt = referenceRateUsdt,
                    grossUsdt = gross,
                    feePercent = safeFeePct,
                    feeUsdt = fee,
                    netUsdt = net,
                    isValid = false,
                    errorReason = "Swaps temporarily unavailable — insufficient USDT liquidity in pool."
                )
            }
            return SwapQuoteCalculation(
                mbtkAmount = mbtkAmount,
                referenceRateUsdt = referenceRateUsdt,
                grossUsdt = gross,
                feePercent = safeFeePct,
                feeUsdt = fee,
                netUsdt = net,
                isValid = true,
                errorReason = null
            )
        }
    }

    suspend fun fetchOnChainBalances(
        walletAddress: String,
        usdtContractAddress: String = DEFAULT_BSC_USDT_CONTRACT,
        isTestnet: Boolean = false
    ): OnChainWalletBalance = withContext(Dispatchers.IO) {
        val cleanAddr = walletAddress.trim()
        val networkName = if (isTestnet) "BNB Smart Chain Testnet (Chapel)" else "BNB Smart Chain (BEP-20)"
        if (!isValidBep20Address(cleanAddr)) {
            return@withContext OnChainWalletBalance(
                address = cleanAddr,
                network = networkName,
                isReachable = false
            )
        }

        val rpcUrl = if (isTestnet) BSC_TESTNET_RPC else BSC_MAINNET_RPC
        try {
            val bnbWeiHex = rpcCall(
                rpcUrl = rpcUrl,
                method = "eth_getBalance",
                params = JSONArray().apply {
                    put(cleanAddr)
                    put("latest")
                }
            )
            // ERC-20 / BEP-20 balanceOf(address) selector: 0x70a08231 + 32-byte padded address
            val paddedAddress = cleanAddr.removePrefix("0x").removePrefix("0X").lowercase().padStart(64, '0')
            val callData = "0x70a08231$paddedAddress"
            val callObj = JSONObject().apply {
                put("to", usdtContractAddress.ifBlank { DEFAULT_BSC_USDT_CONTRACT })
                put("data", callData)
            }
            val usdtWeiHex = rpcCall(
                rpcUrl = rpcUrl,
                method = "eth_call",
                params = JSONArray().apply {
                    put(callObj)
                    put("latest")
                }
            )

            val bnbBalance = parseWeiHexToDouble(bnbWeiHex, 18)
            val usdtBalance = parseWeiHexToDouble(usdtWeiHex, 18)

            OnChainWalletBalance(
                address = cleanAddr,
                network = networkName,
                bnbBalance = bnbBalance,
                usdtBep20Balance = usdtBalance,
                isReachable = bnbWeiHex != null
            )
        } catch (e: Exception) {
            OnChainWalletBalance(
                address = cleanAddr,
                network = networkName,
                isReachable = false
            )
        }
    }

    /**
     * Queries eth_getTransactionReceipt on BNB Smart Chain to verify whether a withdrawal transaction hash
     * is confirmed on-chain before or after settlement.
     */
    suspend fun verifyTransactionOnChain(
        txHash: String,
        isTestnet: Boolean = false
    ): OnChainTxVerification = withContext(Dispatchers.IO) {
        val cleanHash = txHash.trim()
        val networkLabel = if (isTestnet) "BSC Testnet (Chain 97)" else "BSC Mainnet (Chain 56)"
        val explorer = explorerUrlForTx(cleanHash, isTestnet)

        if (!isValidBscTxHash(cleanHash)) {
            return@withContext OnChainTxVerification(
                txHash = cleanHash,
                isConfirmed = false,
                isReverted = false,
                blockNumber = 0L,
                networkLabel = networkLabel,
                explorerUrl = explorer,
                message = "Invalid BSC transaction hash format (expected 0x followed by 64 hex chars)."
            )
        }

        val rpcUrl = if (isTestnet) BSC_TESTNET_RPC else BSC_MAINNET_RPC
        try {
            val receiptJson = rpcCallObject(
                rpcUrl = rpcUrl,
                method = "eth_getTransactionReceipt",
                params = JSONArray().apply { put(cleanHash) }
            ) ?: return@withContext OnChainTxVerification(
                txHash = cleanHash,
                isConfirmed = false,
                isReverted = false,
                blockNumber = 0L,
                networkLabel = networkLabel,
                explorerUrl = explorer,
                message = "Transaction receipt not found on $networkLabel (pending broadcast or indexing)."
            )

            val statusHex = receiptJson.optString("status", "")
            val blockHex = receiptJson.optString("blockNumber", "0x0").removePrefix("0x")
            val blockNum = blockHex.toLongOrNull(16) ?: 0L
            val isConfirmed = statusHex.equals("0x1", ignoreCase = true)
            val isReverted = statusHex.equals("0x0", ignoreCase = true)

            OnChainTxVerification(
                txHash = cleanHash,
                isConfirmed = isConfirmed,
                isReverted = isReverted,
                blockNumber = blockNum,
                networkLabel = networkLabel,
                explorerUrl = explorer,
                message = when {
                    isConfirmed -> "Verified CONFIRMED on $networkLabel at Block #$blockNum."
                    isReverted -> "Transaction REVERTED on $networkLabel at Block #$blockNum."
                    else -> "Transaction found on $networkLabel (status: $statusHex)."
                }
            )
        } catch (e: Exception) {
            OnChainTxVerification(
                txHash = cleanHash,
                isConfirmed = false,
                isReverted = false,
                blockNumber = 0L,
                networkLabel = networkLabel,
                explorerUrl = explorer,
                message = "RPC verification error: ${e.localizedMessage ?: "Network unreachable"}"
            )
        }
    }

    private fun rpcCall(rpcUrl: String, method: String, params: JSONArray): String? {
        val payload = JSONObject().apply {
            put("jsonrpc", "2.0")
            put("id", 1)
            put("method", method)
            put("params", params)
        }.toString()

        val request = Request.Builder()
            .url(rpcUrl)
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val bodyStr = response.body?.string() ?: return null
            val json = JSONObject(bodyStr)
            if (json.has("error")) return null
            return json.optString("result").takeIf { it.isNotBlank() && it != "null" }
        }
    }

    private fun rpcCallObject(rpcUrl: String, method: String, params: JSONArray): JSONObject? {
        val payload = JSONObject().apply {
            put("jsonrpc", "2.0")
            put("id", 1)
            put("method", method)
            put("params", params)
        }.toString()

        val request = Request.Builder()
            .url(rpcUrl)
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val bodyStr = response.body?.string() ?: return null
            val json = JSONObject(bodyStr)
            if (json.has("error")) return null
            return json.optJSONObject("result")
        }
    }

    private fun parseWeiHexToDouble(hexStr: String?, decimals: Int): Double {
        if (hexStr.isNullOrBlank()) return 0.0
        val cleanHex = hexStr.removePrefix("0x").removePrefix("0X").ifBlank { "0" }
        return try {
            val wei = BigInteger(cleanHex, 16)
            BigDecimal(wei)
                .divide(BigDecimal.TEN.pow(decimals), 6, RoundingMode.HALF_UP)
                .toDouble()
        } catch (e: Exception) {
            0.0
        }
    }
}

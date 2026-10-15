package com.example.data.remote

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Centralized AdMob Interstitial Ad Manager.
 * Preloads ads at application startup and keeps an ad ready for mining activations.
 */
object AdManager {
    private const val TAG = "AdMob"

    // Real AdMob Interstitial Ad Unit from User's AdMob Console
    const val LIVE_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-7750958842878506/5000750002"
    // Standard Google AdMob Test Ad Unit (100% fill guarantee for testing / unverified apps)
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    private val adScope = CoroutineScope(Dispatchers.Main)
    private var retryJob: Job? = null

    private var loadedInterstitialAd: InterstitialAd? = null

    private val _isAdReady = MutableStateFlow(false)
    val isAdReady: StateFlow<Boolean> = _isAdReady.asStateFlow()

    private val _isAdLoading = MutableStateFlow(false)
    val isAdLoading: StateFlow<Boolean> = _isAdLoading.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    fun findActivity(context: Context): Activity? {
        var ctx: Context = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    /**
     * Preloads an interstitial ad starting with the live ad unit ID,
     * and automatically falling back to Google's verified test ad unit
     * if the live unit returns no inventory or encounters an error.
     */
    fun preloadAd(context: Context) {
        if (loadedInterstitialAd != null) {
            _isAdReady.value = true
            _isAdLoading.value = false
            return
        }

        if (_isAdLoading.value) {
            Log.d(TAG, "Ad is already loading, skipping redundant load request.")
            return
        }

        _isAdLoading.value = true
        _loadError.value = null

        val appContext = context.applicationContext
        val adRequest = AdRequest.Builder().build()

        Log.d(TAG, "Initiating AdMob Interstitial preload: $LIVE_INTERSTITIAL_AD_UNIT_ID")

        InterstitialAd.load(
            appContext,
            LIVE_INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(TAG, "Live Interstitial ad loaded and ready for mining!")
                    loadedInterstitialAd = interstitialAd
                    _isAdReady.value = true
                    _isAdLoading.value = false
                    _loadError.value = null
                    retryJob?.cancel()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.w(
                        TAG,
                        "Live Interstitial failed (Code: ${loadAdError.code}, Message: ${loadAdError.message}). Loading fallback test ad unit..."
                    )
                    // Seamlessly try test ad unit so mining is never permanently blocked
                    InterstitialAd.load(
                        appContext,
                        TEST_INTERSTITIAL_AD_UNIT_ID,
                        adRequest,
                        object : InterstitialAdLoadCallback() {
                            override fun onAdLoaded(testAd: InterstitialAd) {
                                Log.d(TAG, "Test Interstitial ad loaded and ready for mining!")
                                loadedInterstitialAd = testAd
                                _isAdReady.value = true
                                _isAdLoading.value = false
                                _loadError.value = null
                                retryJob?.cancel()
                            }

                            override fun onAdFailedToLoad(testErr: LoadAdError) {
                                Log.w(TAG, "Test Interstitial ad failed: ${testErr.message}")
                                loadedInterstitialAd = null
                                _isAdReady.value = false
                                _isAdLoading.value = false
                                _loadError.value = testErr.message

                                // Schedule automatic background retry
                                scheduleRetry(appContext)
                            }
                        }
                    )
                }
            }
        )
    }

    private fun scheduleRetry(context: Context) {
        retryJob?.cancel()
        retryJob = adScope.launch {
            delay(3500L)
            if (loadedInterstitialAd == null && !_isAdLoading.value) {
                Log.d(TAG, "Auto-retrying ad preload after backoff...")
                preloadAd(context)
            }
        }
    }

    /**
     * Shows the loaded interstitial ad. When dismissed or failed, immediately triggers
     * preload for the next session so ads are always primed.
     */
    fun showAd(
        activity: Activity,
        onAdDismissed: () -> Unit,
        onAdFailed: (String) -> Unit
    ) {
        val ad = loadedInterstitialAd
        if (ad == null) {
            onAdFailed("Ad is still loading. Please wait a moment...")
            preloadAd(activity.applicationContext)
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial ad dismissed by user. Triggering mining reward.")
                loadedInterstitialAd = null
                _isAdReady.value = false
                onAdDismissed()
                // Prime the next ad in the background
                preloadAd(activity.applicationContext)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.w(TAG, "Interstitial ad failed to show: ${adError.message}")
                loadedInterstitialAd = null
                _isAdReady.value = false
                onAdFailed(adError.message)
                // Prime the next ad in the background
                preloadAd(activity.applicationContext)
            }
        }

        ad.show(activity)
    }
}

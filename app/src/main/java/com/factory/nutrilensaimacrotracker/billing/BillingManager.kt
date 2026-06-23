package com.factory.nutrilensaimacrotracker.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class BillingManager(
    context: Context,
    private val onPurchaseVerified: suspend (Purchase) -> Unit
) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "BillingManager"

        const val PRODUCT_WEEKLY = "com.factory.nutrilensaimacrotracker.subscription.weekly"
        const val PRODUCT_MONTHLY = "com.factory.nutrilensaimacrotracker.subscription.monthly"
        const val PRODUCT_YEARLY = "com.factory.nutrilensaimacrotracker.subscription.yearly"
        // Lifetime is a one-time purchase (INAPP) even though the product ID has "subscription" in it
        const val PRODUCT_LIFETIME = "com.factory.nutrilensaimacrotracker.subscription.lifetime"
        const val PRODUCT_SMALL_IAP = "com.factory.nutrilensaimacrotracker.small_iap"

        val SUBSCRIPTION_IDS = listOf(PRODUCT_WEEKLY, PRODUCT_MONTHLY, PRODUCT_YEARLY)
        val INAPP_IDS = listOf(PRODUCT_LIFETIME, PRODUCT_SMALL_IAP)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionState = MutableStateFlow<BillingConnectionState>(BillingConnectionState.Disconnected)
    val connectionState: StateFlow<BillingConnectionState> = _connectionState.asStateFlow()

    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _products.asStateFlow()

    private val _billingError = MutableStateFlow<String?>(null)
    val billingError: StateFlow<String?> = _billingError.asStateFlow()

    val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    fun connect() {
        if (billingClient.isReady) {
            scope.launch { queryProducts() }
            scope.launch { checkExistingPurchases() }
            return
        }
        _connectionState.value = BillingConnectionState.Connecting
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _connectionState.value = BillingConnectionState.Connected
                    scope.launch { queryProducts() }
                    scope.launch { checkExistingPurchases() }
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                    _connectionState.value = BillingConnectionState.Disconnected
                    _billingError.value = "Billing unavailable. Check your connection and try again."
                }
            }

            override fun onBillingServiceDisconnected() {
                _connectionState.value = BillingConnectionState.Disconnected
            }
        })
    }

    private suspend fun queryProducts() {
        val allProducts = mutableListOf<ProductDetails>()

        // Query subscriptions (weekly, monthly, yearly)
        if (SUBSCRIPTION_IDS.isNotEmpty()) {
            val subParams = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    SUBSCRIPTION_IDS.map { productId ->
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(productId)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                    }
                )
                .build()
            val subResult = billingClient.queryProductDetails(subParams)
            if (subResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                subResult.productDetailsList?.let { allProducts.addAll(it) }
            } else {
                Log.w(TAG, "Subscription query failed: ${subResult.billingResult.debugMessage}")
            }
        }

        // Query one-time purchases (lifetime, small_iap)
        if (INAPP_IDS.isNotEmpty()) {
            val iapParams = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    INAPP_IDS.map { productId ->
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(productId)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    }
                )
                .build()
            val iapResult = billingClient.queryProductDetails(iapParams)
            if (iapResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                iapResult.productDetailsList?.let { allProducts.addAll(it) }
            } else {
                Log.w(TAG, "IAP query failed: ${iapResult.billingResult.debugMessage}")
            }
        }

        _products.value = allProducts
    }

    suspend fun checkExistingPurchases() {
        // Check active subscriptions
        val subResult = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        subResult.purchasesList.forEach { purchase ->
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                onPurchaseVerified(purchase)
                acknowledgePurchase(purchase)
            }
        }

        // Check one-time purchases (lifetime)
        val iapResult = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        iapResult.purchasesList.forEach { purchase ->
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                onPurchaseVerified(purchase)
                acknowledgePurchase(purchase)
            }
        }
    }

    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails): BillingResult {
        val isSubscription = SUBSCRIPTION_IDS.contains(productDetails.productId)

        val productDetailsParams = if (isSubscription) {
            val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: ""
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        } else {
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        }

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        return billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    scope.launch {
                        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                            onPurchaseVerified(purchase)
                            acknowledgePurchase(purchase)
                        }
                        // PENDING state: purchase will be verified later via checkExistingPurchases
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User cancelled the purchase flow")
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                scope.launch { checkExistingPurchases() }
            }
            BillingClient.BillingResponseCode.NETWORK_ERROR -> {
                _billingError.value = "No network connection. Please try again."
            }
            else -> {
                Log.e(TAG, "Purchase failed: [${billingResult.responseCode}] ${billingResult.debugMessage}")
                _billingError.value = "Purchase failed. Please try again."
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        scope.launch {
            val result = billingClient.acknowledgePurchase(params)
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.e(TAG, "Failed to acknowledge purchase: ${result.debugMessage}")
            }
        }
    }

    fun restorePurchases() {
        scope.launch { checkExistingPurchases() }
    }

    fun clearError() {
        _billingError.value = null
    }

    fun getProductDetails(productId: String): ProductDetails? =
        _products.value.find { it.productId == productId }

    fun disconnect() {
        scope.cancel()
        billingClient.endConnection()
    }
}

sealed class BillingConnectionState {
    data object Disconnected : BillingConnectionState()
    data object Connecting : BillingConnectionState()
    data object Connected : BillingConnectionState()
}

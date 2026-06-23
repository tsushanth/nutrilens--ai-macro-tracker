package com.factory.nutrilensaimacrotracker.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.factory.nutrilensaimacrotracker.NutriLensApp
import com.factory.nutrilensaimacrotracker.billing.BillingConnectionState
import com.factory.nutrilensaimacrotracker.billing.BillingManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PricingPlan(
    val productId: String,
    val label: String,
    val price: String,        // Fallback price shown before billing loads
    val period: String,
    val badge: String? = null // e.g. "Best Value"
)

class PaywallViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as NutriLensApp
    private val billingManager get() = app.billingManager
    private val premiumManager get() = app.premiumManager

    val isPremium: StateFlow<Boolean> = premiumManager.isPremium
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val connectionState: StateFlow<BillingConnectionState> = billingManager.connectionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BillingConnectionState.Disconnected)

    val billingError: StateFlow<String?> = billingManager.billingError
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _restoreSuccess = MutableStateFlow(false)
    val restoreSuccess: StateFlow<Boolean> = _restoreSuccess.asStateFlow()

    // Hardcoded fallback plans — billing library will supplement with real prices when available
    val plans = listOf(
        PricingPlan(BillingManager.PRODUCT_WEEKLY,  "Weekly",  "$2.87", "/ week"),
        PricingPlan(BillingManager.PRODUCT_MONTHLY, "Monthly", "$7.99", "/ month"),
        PricingPlan(BillingManager.PRODUCT_YEARLY,  "Yearly",  "$31.99", "/ year", badge = "Best Value"),
        PricingPlan(BillingManager.PRODUCT_LIFETIME,"Lifetime","$63.98", "one-time")
    )

    private val _selectedPlanId = MutableStateFlow(BillingManager.PRODUCT_YEARLY)
    val selectedPlanId: StateFlow<String> = _selectedPlanId.asStateFlow()

    fun selectPlan(productId: String) {
        _selectedPlanId.value = productId
    }

    fun purchaseSelectedPlan(activity: Activity) {
        val productId = _selectedPlanId.value
        val productDetails = billingManager.getProductDetails(productId)
        if (productDetails != null) {
            billingManager.launchBillingFlow(activity, productDetails)
        } else {
            // Billing products not loaded yet — connect and retry
            billingManager.connect()
        }
    }

    fun purchasePlan(activity: Activity, productId: String) {
        _selectedPlanId.value = productId
        val productDetails = billingManager.getProductDetails(productId)
        if (productDetails != null) {
            billingManager.launchBillingFlow(activity, productDetails)
        } else {
            billingManager.connect()
        }
    }

    fun getProductDetails(productId: String): ProductDetails? =
        billingManager.getProductDetails(productId)

    fun getPriceForPlan(plan: PricingPlan): String {
        val details = billingManager.getProductDetails(plan.productId)
        return when {
            details == null -> plan.price
            details.subscriptionOfferDetails != null -> {
                details.subscriptionOfferDetails
                    ?.firstOrNull()
                    ?.pricingPhases
                    ?.pricingPhaseList
                    ?.firstOrNull()
                    ?.formattedPrice
                    ?: plan.price
            }
            else -> details.oneTimePurchaseOfferDetails?.formattedPrice ?: plan.price
        }
    }

    fun restorePurchases() {
        viewModelScope.launch {
            billingManager.restorePurchases()
            // Give the billing client a moment to process, then signal UI
            kotlinx.coroutines.delay(2_000)
            _restoreSuccess.value = true
            kotlinx.coroutines.delay(3_000)
            _restoreSuccess.value = false
        }
    }

    fun clearError() {
        billingManager.clearError()
    }
}

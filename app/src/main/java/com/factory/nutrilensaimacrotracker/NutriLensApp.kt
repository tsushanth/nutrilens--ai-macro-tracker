package com.factory.nutrilensaimacrotracker

import android.app.Application
import com.factory.nutrilensaimacrotracker.billing.BillingManager
import com.factory.nutrilensaimacrotracker.billing.PremiumManager
import com.factory.nutrilensaimacrotracker.data.database.NutriLensDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NutriLensApp : Application() {

    val database: NutriLensDatabase by lazy {
        NutriLensDatabase.getDatabase(this)
    }

    val premiumManager: PremiumManager by lazy {
        PremiumManager(this)
    }

    val billingManager: BillingManager by lazy {
        BillingManager(this) { purchase ->
            // Called when a purchase is verified — persist premium status
            premiumManager.grantPremium(purchase)
        }
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Kick off billing connection early so product details are ready when paywall opens
        appScope.launch {
            billingManager.connect()
        }
    }
}

package com.factory.nutrilensaimacrotracker.billing

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.premiumDataStore: DataStore<Preferences> by preferencesDataStore(name = "premium_prefs")

class PremiumManager(private val context: Context) {

    companion object {
        private val KEY_IS_PREMIUM = booleanPreferencesKey("is_premium")
        private val KEY_ACTIVE_PRODUCT_ID = stringPreferencesKey("active_product_id")
        private val KEY_PURCHASE_TOKEN = stringPreferencesKey("purchase_token")
    }

    val isPremium: Flow<Boolean> = context.premiumDataStore.data.map { prefs ->
        prefs[KEY_IS_PREMIUM] ?: false
    }

    val activeProductId: Flow<String?> = context.premiumDataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_PRODUCT_ID]?.takeIf { it.isNotEmpty() }
    }

    suspend fun grantPremium(purchase: Purchase) {
        context.premiumDataStore.edit { prefs ->
            prefs[KEY_IS_PREMIUM] = true
            prefs[KEY_ACTIVE_PRODUCT_ID] = purchase.products.firstOrNull() ?: ""
            prefs[KEY_PURCHASE_TOKEN] = purchase.purchaseToken
        }
    }

    suspend fun revokePremium() {
        context.premiumDataStore.edit { prefs ->
            prefs[KEY_IS_PREMIUM] = false
            prefs[KEY_ACTIVE_PRODUCT_ID] = ""
            prefs[KEY_PURCHASE_TOKEN] = ""
        }
    }
}

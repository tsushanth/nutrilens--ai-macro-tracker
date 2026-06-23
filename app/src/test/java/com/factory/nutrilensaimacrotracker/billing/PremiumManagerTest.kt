package com.factory.nutrilensaimacrotracker.billing

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.billingclient.api.Purchase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PremiumManagerTest {

    private lateinit var context: Context
    private lateinit var premiumManager: PremiumManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        premiumManager = PremiumManager(context)
        // Ensure a clean slate before every test — DataStore is a singleton in Robolectric
        // so revokePremium() resets all persisted keys to their defaults
        runBlocking { premiumManager.revokePremium() }
    }

    // --- Default State Tests ---

    @Test
    fun `isPremium is false by default`() = runTest {
        val result = premiumManager.isPremium.first()
        assertFalse(result)
    }

    @Test
    fun `activeProductId is null by default`() = runTest {
        val result = premiumManager.activeProductId.first()
        assertNull(result)
    }

    // --- grantPremium Tests ---

    @Test
    fun `grantPremium sets isPremium to true`() = runTest {
        val mockPurchase = buildMockPurchase(
            products = listOf("com.factory.nutrilensaimacrotracker.subscription.monthly"),
            token = "test_token_123"
        )

        premiumManager.grantPremium(mockPurchase)

        assertTrue(premiumManager.isPremium.first())
    }

    @Test
    fun `grantPremium stores the active product ID`() = runTest {
        val productId = "com.factory.nutrilensaimacrotracker.subscription.yearly"
        val mockPurchase = buildMockPurchase(
            products = listOf(productId),
            token = "yearly_token_456"
        )

        premiumManager.grantPremium(mockPurchase)

        assertEquals(productId, premiumManager.activeProductId.first())
    }

    @Test
    fun `grantPremium with multiple products stores first product ID`() = runTest {
        val firstProduct = "com.factory.nutrilensaimacrotracker.subscription.monthly"
        val mockPurchase = buildMockPurchase(
            products = listOf(firstProduct, "com.factory.nutrilensaimacrotracker.subscription.yearly"),
            token = "multi_token"
        )

        premiumManager.grantPremium(mockPurchase)

        assertEquals(firstProduct, premiumManager.activeProductId.first())
    }

    @Test
    fun `grantPremium with empty products list stores empty product ID`() = runTest {
        val mockPurchase = buildMockPurchase(
            products = emptyList(),
            token = "empty_token"
        )

        premiumManager.grantPremium(mockPurchase)

        // Empty products list means firstOrNull() returns null → stored as ""
        // activeProductId uses takeIf { it.isNotEmpty() } so returns null
        val result = premiumManager.activeProductId.first()
        assertNull(result)
    }

    // --- revokePremium Tests ---

    @Test
    fun `revokePremium sets isPremium to false`() = runTest {
        val mockPurchase = buildMockPurchase(
            products = listOf("com.factory.nutrilensaimacrotracker.subscription.weekly"),
            token = "weekly_token"
        )
        premiumManager.grantPremium(mockPurchase)
        assertTrue(premiumManager.isPremium.first())

        premiumManager.revokePremium()

        assertFalse(premiumManager.isPremium.first())
    }

    @Test
    fun `revokePremium clears active product ID`() = runTest {
        val mockPurchase = buildMockPurchase(
            products = listOf("com.factory.nutrilensaimacrotracker.subscription.monthly"),
            token = "revoke_token"
        )
        premiumManager.grantPremium(mockPurchase)

        premiumManager.revokePremium()

        assertNull(premiumManager.activeProductId.first())
    }

    @Test
    fun `revokePremium on non-premium user does not throw`() = runTest {
        // Should complete without error
        premiumManager.revokePremium()

        assertFalse(premiumManager.isPremium.first())
    }

    // --- State Persistence Tests ---

    @Test
    fun `grant then revoke leaves isPremium as false`() = runTest {
        val mockPurchase = buildMockPurchase(
            products = listOf("com.factory.nutrilensaimacrotracker.subscription.yearly"),
            token = "persistence_token"
        )

        premiumManager.grantPremium(mockPurchase)
        premiumManager.revokePremium()

        assertFalse(premiumManager.isPremium.first())
        assertNull(premiumManager.activeProductId.first())
    }

    @Test
    fun `multiple grantPremium calls update to latest product`() = runTest {
        val firstPurchase = buildMockPurchase(
            products = listOf("com.factory.nutrilensaimacrotracker.subscription.monthly"),
            token = "first_token"
        )
        val secondPurchase = buildMockPurchase(
            products = listOf("com.factory.nutrilensaimacrotracker.subscription.yearly"),
            token = "second_token"
        )

        premiumManager.grantPremium(firstPurchase)
        premiumManager.grantPremium(secondPurchase)

        assertTrue(premiumManager.isPremium.first())
        assertEquals(
            "com.factory.nutrilensaimacrotracker.subscription.yearly",
            premiumManager.activeProductId.first()
        )
    }

    // --- Lifetime purchase test ---

    @Test
    fun `grantPremium works with lifetime inapp purchase`() = runTest {
        val mockPurchase = buildMockPurchase(
            products = listOf("com.factory.nutrilensaimacrotracker.subscription.lifetime"),
            token = "lifetime_token"
        )

        premiumManager.grantPremium(mockPurchase)

        assertTrue(premiumManager.isPremium.first())
        assertEquals(
            "com.factory.nutrilensaimacrotracker.subscription.lifetime",
            premiumManager.activeProductId.first()
        )
    }

    // --- Helper ---

    private fun buildMockPurchase(products: List<String>, token: String): Purchase {
        return mockk<Purchase> {
            every { this@mockk.products } returns products
            every { purchaseToken } returns token
        }
    }
}

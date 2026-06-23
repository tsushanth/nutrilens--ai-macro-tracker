package com.factory.nutrilensaimacrotracker.billing

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.factory.nutrilensaimacrotracker.utils.MainDispatcherRule
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class BillingManagerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var billingManager: BillingManager
    private lateinit var mockBillingClient: BillingClient
    private lateinit var mockBuilder: BillingClient.Builder
    private val mockContext = mockk<Context>(relaxed = true)

    @Before
    fun setup() {
        mockkStatic(BillingClient::class)
        mockBillingClient = mockk(relaxed = true)
        mockBuilder = mockk(relaxed = true)

        every { BillingClient.newBuilder(any()) } returns mockBuilder
        every { mockBuilder.setListener(any()) } returns mockBuilder
        every { mockBuilder.enablePendingPurchases() } returns mockBuilder
        every { mockBuilder.build() } returns mockBillingClient
        every { mockBillingClient.isReady } returns false

        billingManager = BillingManager(mockContext) { }
    }

    @After
    fun teardown() {
        unmockkStatic(BillingClient::class)
    }

    // --- Initial State Tests ---

    @Test
    fun `initial connection state is Disconnected`() {
        assertEquals(BillingConnectionState.Disconnected, billingManager.connectionState.value)
    }

    @Test
    fun `initial billing error is null`() {
        assertNull(billingManager.billingError.value)
    }

    @Test
    fun `initial products list is empty`() {
        assertTrue(billingManager.products.value.isEmpty())
    }

    // --- Product ID Constants Tests ---

    @Test
    fun `product IDs are correctly defined`() {
        assertEquals(
            "com.factory.nutrilensaimacrotracker.subscription.weekly",
            BillingManager.PRODUCT_WEEKLY
        )
        assertEquals(
            "com.factory.nutrilensaimacrotracker.subscription.monthly",
            BillingManager.PRODUCT_MONTHLY
        )
        assertEquals(
            "com.factory.nutrilensaimacrotracker.subscription.yearly",
            BillingManager.PRODUCT_YEARLY
        )
        assertEquals(
            "com.factory.nutrilensaimacrotracker.subscription.lifetime",
            BillingManager.PRODUCT_LIFETIME
        )
        assertEquals(
            "com.factory.nutrilensaimacrotracker.small_iap",
            BillingManager.PRODUCT_SMALL_IAP
        )
    }

    @Test
    fun `subscription IDs list contains weekly monthly and yearly`() {
        assertTrue(BillingManager.SUBSCRIPTION_IDS.contains(BillingManager.PRODUCT_WEEKLY))
        assertTrue(BillingManager.SUBSCRIPTION_IDS.contains(BillingManager.PRODUCT_MONTHLY))
        assertTrue(BillingManager.SUBSCRIPTION_IDS.contains(BillingManager.PRODUCT_YEARLY))
    }

    @Test
    fun `subscription IDs list does not contain lifetime or small iap`() {
        assertFalse(BillingManager.SUBSCRIPTION_IDS.contains(BillingManager.PRODUCT_LIFETIME))
        assertFalse(BillingManager.SUBSCRIPTION_IDS.contains(BillingManager.PRODUCT_SMALL_IAP))
    }

    @Test
    fun `inapp IDs list contains lifetime and small iap`() {
        assertTrue(BillingManager.INAPP_IDS.contains(BillingManager.PRODUCT_LIFETIME))
        assertTrue(BillingManager.INAPP_IDS.contains(BillingManager.PRODUCT_SMALL_IAP))
    }

    @Test
    fun `inapp IDs list does not contain subscription products`() {
        assertFalse(BillingManager.INAPP_IDS.contains(BillingManager.PRODUCT_WEEKLY))
        assertFalse(BillingManager.INAPP_IDS.contains(BillingManager.PRODUCT_MONTHLY))
        assertFalse(BillingManager.INAPP_IDS.contains(BillingManager.PRODUCT_YEARLY))
    }

    // --- getProductDetails Tests ---

    @Test
    fun `getProductDetails returns null when products list is empty`() {
        assertNull(billingManager.getProductDetails(BillingManager.PRODUCT_MONTHLY))
    }

    @Test
    fun `getProductDetails returns null for unknown product ID`() {
        assertNull(billingManager.getProductDetails("unknown.product.id"))
    }

    // --- clearError Tests ---

    @Test
    fun `clearError sets billing error to null when no error present`() {
        billingManager.clearError()
        assertNull(billingManager.billingError.value)
    }

    @Test
    fun `clearError clears billing error after network error`() {
        val networkErrorResult = mockk<BillingResult> {
            every { responseCode } returns BillingClient.BillingResponseCode.NETWORK_ERROR
        }
        billingManager.onPurchasesUpdated(networkErrorResult, null)
        assertEquals("No network connection. Please try again.", billingManager.billingError.value)

        billingManager.clearError()

        assertNull(billingManager.billingError.value)
    }

    // --- onPurchasesUpdated Tests ---

    @Test
    fun `onPurchasesUpdated with network error sets appropriate billing error`() {
        val result = mockk<BillingResult> {
            every { responseCode } returns BillingClient.BillingResponseCode.NETWORK_ERROR
        }

        billingManager.onPurchasesUpdated(result, null)

        assertEquals("No network connection. Please try again.", billingManager.billingError.value)
    }

    @Test
    fun `onPurchasesUpdated with generic error sets billing error`() {
        val result = mockk<BillingResult> {
            every { responseCode } returns BillingClient.BillingResponseCode.ERROR
            every { debugMessage } returns "Internal error"
        }

        billingManager.onPurchasesUpdated(result, null)

        assertEquals("Purchase failed. Please try again.", billingManager.billingError.value)
    }

    @Test
    fun `onPurchasesUpdated with user cancelled does not set error`() {
        val result = mockk<BillingResult> {
            every { responseCode } returns BillingClient.BillingResponseCode.USER_CANCELED
        }

        billingManager.onPurchasesUpdated(result, null)

        assertNull(billingManager.billingError.value)
    }

    @Test
    fun `onPurchasesUpdated with null purchases and OK response does not crash`() {
        val result = mockk<BillingResult> {
            every { responseCode } returns BillingClient.BillingResponseCode.OK
        }

        billingManager.onPurchasesUpdated(result, null)

        // No crash, no error set
        assertNull(billingManager.billingError.value)
    }

    @Test
    fun `onPurchasesUpdated with developer error sets billing error`() {
        val result = mockk<BillingResult> {
            every { responseCode } returns BillingClient.BillingResponseCode.DEVELOPER_ERROR
            every { debugMessage } returns "Bad request"
        }

        billingManager.onPurchasesUpdated(result, null)

        assertEquals("Purchase failed. Please try again.", billingManager.billingError.value)
    }

    // --- connect() Tests ---

    @Test
    fun `connect sets connection state to Connecting when client not ready`() = runTest {
        val listenerSlot = slot<BillingClientStateListener>()
        every { mockBillingClient.startConnection(capture(listenerSlot)) } just runs

        billingManager.connect()

        assertEquals(BillingConnectionState.Connecting, billingManager.connectionState.value)
    }

    @Test
    fun `connect does not change state when client is already ready`() = runTest {
        every { mockBillingClient.isReady } returns true

        billingManager.connect()

        // Should not change to Connecting since client is already ready
        assertEquals(BillingConnectionState.Disconnected, billingManager.connectionState.value)
    }

    @Test
    fun `connect calls startConnection when client not ready`() {
        every { mockBillingClient.startConnection(any()) } just runs

        billingManager.connect()

        verify { mockBillingClient.startConnection(any()) }
    }

    // --- BillingConnectionState Tests ---

    @Test
    fun `BillingConnectionState sealed class has three states`() {
        val disconnected: BillingConnectionState = BillingConnectionState.Disconnected
        val connecting: BillingConnectionState = BillingConnectionState.Connecting
        val connected: BillingConnectionState = BillingConnectionState.Connected

        assertTrue(disconnected is BillingConnectionState.Disconnected)
        assertTrue(connecting is BillingConnectionState.Connecting)
        assertTrue(connected is BillingConnectionState.Connected)
    }

    // --- Purchase flow interaction test ---

    @Test
    fun `onPurchasesUpdated with OK and pending purchase does not invoke callback immediately`() = runTest {
        var callbackInvoked = false
        val managerWithCallback = BillingManager(mockContext) { callbackInvoked = true }

        val pendingPurchase = mockk<Purchase> {
            every { purchaseState } returns Purchase.PurchaseState.PENDING
            every { isAcknowledged } returns false
        }
        val okResult = mockk<BillingResult> {
            every { responseCode } returns BillingClient.BillingResponseCode.OK
        }

        managerWithCallback.onPurchasesUpdated(okResult, listOf(pendingPurchase))

        // PENDING purchase should not trigger callback
        assertFalse(callbackInvoked)
    }
}

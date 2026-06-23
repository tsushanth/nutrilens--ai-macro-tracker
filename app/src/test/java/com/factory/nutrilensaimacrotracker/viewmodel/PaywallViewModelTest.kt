package com.factory.nutrilensaimacrotracker.viewmodel

import app.cash.turbine.test
import com.android.billingclient.api.ProductDetails
import com.factory.nutrilensaimacrotracker.NutriLensApp
import com.factory.nutrilensaimacrotracker.billing.BillingConnectionState
import com.factory.nutrilensaimacrotracker.billing.BillingManager
import com.factory.nutrilensaimacrotracker.billing.PremiumManager
import com.factory.nutrilensaimacrotracker.utils.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PaywallViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: PaywallViewModel
    private val mockBillingManager = mockk<BillingManager>(relaxed = true)
    private val mockPremiumManager = mockk<PremiumManager>(relaxed = true)
    private val mockApp = mockk<NutriLensApp>(relaxed = true)

    private val connectionStateFlow = MutableStateFlow<BillingConnectionState>(BillingConnectionState.Disconnected)
    private val billingErrorFlow = MutableStateFlow<String?>(null)

    @Before
    fun setup() {
        every { mockApp.billingManager } returns mockBillingManager
        every { mockApp.premiumManager } returns mockPremiumManager
        every { mockBillingManager.connectionState } returns connectionStateFlow
        every { mockBillingManager.billingError } returns billingErrorFlow
        every { mockPremiumManager.isPremium } returns flowOf(false)
        every { mockBillingManager.getProductDetails(any()) } returns null

        viewModel = PaywallViewModel(mockApp)
    }

    // --- Initial State Tests ---

    @Test
    fun `isPremium initial value is false`() = runTest {
        viewModel.isPremium.test {
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `connectionState initial value is Disconnected`() = runTest {
        viewModel.connectionState.test {
            assertEquals(BillingConnectionState.Disconnected, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `billingError initial value is null`() = runTest {
        viewModel.billingError.test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `restoreSuccess initial value is false`() = runTest {
        viewModel.restoreSuccess.test {
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selectedPlanId initial value is yearly plan`() = runTest {
        viewModel.selectedPlanId.test {
            assertEquals(BillingManager.PRODUCT_YEARLY, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- isPremium State Tests ---

    @Test
    fun `isPremium reflects premium status from PremiumManager`() = runTest {
        every { mockPremiumManager.isPremium } returns flowOf(true)
        val vm = PaywallViewModel(mockApp)

        vm.isPremium.test {
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- connectionState Tests ---

    @Test
    fun `connectionState reflects billing connection changes`() = runTest {
        viewModel.connectionState.test {
            assertEquals(BillingConnectionState.Disconnected, awaitItem())

            connectionStateFlow.value = BillingConnectionState.Connected
            assertEquals(BillingConnectionState.Connected, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `connectionState reflects connecting state`() = runTest {
        connectionStateFlow.value = BillingConnectionState.Connecting
        val vm = PaywallViewModel(mockApp)

        vm.connectionState.test {
            assertEquals(BillingConnectionState.Connecting, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- billingError Tests ---

    @Test
    fun `billingError reflects error from BillingManager`() = runTest {
        viewModel.billingError.test {
            assertNull(awaitItem())

            billingErrorFlow.value = "No network connection. Please try again."
            assertEquals("No network connection. Please try again.", awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- plans Tests ---

    @Test
    fun `plans list contains four pricing plans`() {
        assertEquals(4, viewModel.plans.size)
    }

    @Test
    fun `plans list contains weekly monthly yearly and lifetime`() {
        val planIds = viewModel.plans.map { it.productId }
        assertTrue(planIds.contains(BillingManager.PRODUCT_WEEKLY))
        assertTrue(planIds.contains(BillingManager.PRODUCT_MONTHLY))
        assertTrue(planIds.contains(BillingManager.PRODUCT_YEARLY))
        assertTrue(planIds.contains(BillingManager.PRODUCT_LIFETIME))
    }

    @Test
    fun `yearly plan has Best Value badge`() {
        val yearlyPlan = viewModel.plans.find { it.productId == BillingManager.PRODUCT_YEARLY }
        assertNotNull(yearlyPlan)
        assertEquals("Best Value", yearlyPlan?.badge)
    }

    @Test
    fun `weekly plan has no badge`() {
        val weeklyPlan = viewModel.plans.find { it.productId == BillingManager.PRODUCT_WEEKLY }
        assertNotNull(weeklyPlan)
        assertNull(weeklyPlan?.badge)
    }

    // --- selectPlan Tests ---

    @Test
    fun `selectPlan updates selectedPlanId`() = runTest {
        viewModel.selectPlan(BillingManager.PRODUCT_MONTHLY)

        viewModel.selectedPlanId.test {
            assertEquals(BillingManager.PRODUCT_MONTHLY, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selectPlan can switch between different plans`() = runTest {
        viewModel.selectPlan(BillingManager.PRODUCT_WEEKLY)
        viewModel.selectPlan(BillingManager.PRODUCT_LIFETIME)

        viewModel.selectedPlanId.test {
            assertEquals(BillingManager.PRODUCT_LIFETIME, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- purchaseSelectedPlan Tests ---

    @Test
    fun `purchaseSelectedPlan calls connect when product details not loaded`() {
        val mockActivity = mockk<android.app.Activity>(relaxed = true)
        every { mockBillingManager.getProductDetails(any()) } returns null

        viewModel.purchaseSelectedPlan(mockActivity)

        verify { mockBillingManager.connect() }
    }

    @Test
    fun `purchaseSelectedPlan launches billing flow when product details available`() {
        val mockActivity = mockk<android.app.Activity>(relaxed = true)
        val mockProductDetails = mockk<ProductDetails>(relaxed = true)
        every { mockBillingManager.getProductDetails(BillingManager.PRODUCT_YEARLY) } returns mockProductDetails

        viewModel.purchaseSelectedPlan(mockActivity)

        verify { mockBillingManager.launchBillingFlow(mockActivity, mockProductDetails) }
    }

    // --- purchasePlan Tests ---

    @Test
    fun `purchasePlan updates selectedPlanId then launches flow`() {
        val mockActivity = mockk<android.app.Activity>(relaxed = true)
        val mockProductDetails = mockk<ProductDetails>(relaxed = true)
        every { mockBillingManager.getProductDetails(BillingManager.PRODUCT_MONTHLY) } returns mockProductDetails

        viewModel.purchasePlan(mockActivity, BillingManager.PRODUCT_MONTHLY)

        viewModel.selectedPlanId.value.also {
            assertEquals(BillingManager.PRODUCT_MONTHLY, it)
        }
        verify { mockBillingManager.launchBillingFlow(mockActivity, mockProductDetails) }
    }

    @Test
    fun `purchasePlan calls connect when product details unavailable`() {
        val mockActivity = mockk<android.app.Activity>(relaxed = true)
        every { mockBillingManager.getProductDetails(any()) } returns null

        viewModel.purchasePlan(mockActivity, BillingManager.PRODUCT_WEEKLY)

        verify { mockBillingManager.connect() }
    }

    // --- getProductDetails Tests ---

    @Test
    fun `getProductDetails returns null when billing not loaded`() {
        every { mockBillingManager.getProductDetails(BillingManager.PRODUCT_MONTHLY) } returns null

        val result = viewModel.getProductDetails(BillingManager.PRODUCT_MONTHLY)

        assertNull(result)
    }

    @Test
    fun `getProductDetails returns ProductDetails when available`() {
        val mockProductDetails = mockk<ProductDetails>(relaxed = true)
        every { mockBillingManager.getProductDetails(BillingManager.PRODUCT_YEARLY) } returns mockProductDetails

        val result = viewModel.getProductDetails(BillingManager.PRODUCT_YEARLY)

        assertEquals(mockProductDetails, result)
    }

    // --- getPriceForPlan Tests ---

    @Test
    fun `getPriceForPlan returns fallback price when billing not loaded`() {
        val weeklyPlan = viewModel.plans.find { it.productId == BillingManager.PRODUCT_WEEKLY }!!
        every { mockBillingManager.getProductDetails(BillingManager.PRODUCT_WEEKLY) } returns null

        val price = viewModel.getPriceForPlan(weeklyPlan)

        assertEquals(weeklyPlan.price, price)
    }

    // --- clearError Tests ---

    @Test
    fun `clearError delegates to BillingManager`() {
        viewModel.clearError()

        verify { mockBillingManager.clearError() }
    }
}

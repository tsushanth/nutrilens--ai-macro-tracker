package com.factory.nutrilensaimacrotracker.viewmodel

import app.cash.turbine.test
import com.factory.nutrilensaimacrotracker.NutriLensApp
import com.factory.nutrilensaimacrotracker.data.database.DailyTotals
import com.factory.nutrilensaimacrotracker.data.database.NutriLensDatabase
import com.factory.nutrilensaimacrotracker.data.database.FoodEntryDao
import com.factory.nutrilensaimacrotracker.data.database.DailyGoalDao
import com.factory.nutrilensaimacrotracker.data.model.DailyGoal
import com.factory.nutrilensaimacrotracker.data.model.FoodEntry
import com.factory.nutrilensaimacrotracker.utils.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: HomeViewModel
    private val mockFoodEntryDao = mockk<FoodEntryDao>(relaxed = true)
    private val mockDailyGoalDao = mockk<DailyGoalDao>(relaxed = true)
    private val mockDatabase = mockk<NutriLensDatabase>(relaxed = true)
    private val mockApp = mockk<NutriLensApp>(relaxed = true)

    private val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    @Before
    fun setup() {
        every { mockDatabase.foodEntryDao() } returns mockFoodEntryDao
        every { mockDatabase.dailyGoalDao() } returns mockDailyGoalDao
        every { mockApp.database } returns mockDatabase
        every { mockFoodEntryDao.getFoodEntriesForDate(any()) } returns flowOf(emptyList())
        every { mockFoodEntryDao.getDailyTotalsForDate(any()) } returns flowOf(
            DailyTotals(0f, 0f, 0f, 0f)
        )
        every { mockDailyGoalDao.getGoal() } returns flowOf(DailyGoal())

        viewModel = HomeViewModel(mockApp)
    }

    // --- Initial State Tests ---

    @Test
    fun `todayEntries initial state is empty list`() = runTest {
        viewModel.todayEntries.test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `todayTotals initial state has zero macros`() = runTest {
        viewModel.todayTotals.test {
            val totals = awaitItem()
            assertEquals(0f, totals.calories)
            assertEquals(0f, totals.protein)
            assertEquals(0f, totals.carbs)
            assertEquals(0f, totals.fat)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dailyGoal initial state has default values`() = runTest {
        viewModel.dailyGoal.test {
            val goal = awaitItem()
            assertEquals(2000f, goal.targetCalories)
            assertEquals(150f, goal.targetProtein)
            assertEquals(250f, goal.targetCarbs)
            assertEquals(65f, goal.targetFat)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- Data Loading Tests ---

    @Test
    fun `todayEntries reflects food entries from repository`() = runTest {
        val entries = listOf(
            buildFoodEntry(name = "Apple"),
            buildFoodEntry(name = "Banana")
        )
        every { mockFoodEntryDao.getFoodEntriesForDate(today) } returns flowOf(entries)

        val vm = HomeViewModel(mockApp)

        vm.todayEntries.test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals("Apple", result[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `todayTotals reflects aggregated macros from repository`() = runTest {
        val totals = DailyTotals(calories = 1800f, protein = 140f, carbs = 220f, fat = 58f)
        every { mockFoodEntryDao.getDailyTotalsForDate(today) } returns flowOf(totals)

        val vm = HomeViewModel(mockApp)

        vm.todayTotals.test {
            val result = awaitItem()
            assertEquals(1800f, result.calories)
            assertEquals(140f, result.protein)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dailyGoal reflects goal from repository`() = runTest {
        val customGoal = DailyGoal(
            targetCalories = 1600f,
            targetProtein = 120f,
            targetCarbs = 200f,
            targetFat = 50f
        )
        every { mockDailyGoalDao.getGoal() } returns flowOf(customGoal)

        val vm = HomeViewModel(mockApp)

        vm.dailyGoal.test {
            val result = awaitItem()
            assertEquals(1600f, result.targetCalories)
            assertEquals(120f, result.targetProtein)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- deleteFoodEntry Tests ---

    @Test
    fun `deleteFoodEntry calls repository delete with correct entry`() = runTest {
        val entry = buildFoodEntry(name = "Pizza")

        viewModel.deleteFoodEntry(entry)

        coVerify { mockFoodEntryDao.delete(entry) }
    }

    @Test
    fun `deleteFoodEntry does not throw for valid entry`() = runTest {
        val entry = buildFoodEntry(name = "Sushi")

        // Should not throw
        viewModel.deleteFoodEntry(entry)
    }

    // --- Helper ---

    private fun buildFoodEntry(
        name: String = "Test Food",
        calories: Float = 200f,
        protein: Float = 20f,
        carbs: Float = 25f,
        fat: Float = 8f
    ) = FoodEntry(
        name = name,
        calories = calories,
        protein = protein,
        carbs = carbs,
        fat = fat,
        dateLogged = today,
        timestampMs = System.currentTimeMillis()
    )
}

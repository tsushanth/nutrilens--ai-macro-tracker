package com.factory.nutrilensaimacrotracker.viewmodel

import app.cash.turbine.test
import com.factory.nutrilensaimacrotracker.NutriLensApp
import com.factory.nutrilensaimacrotracker.data.database.DailyTotals
import com.factory.nutrilensaimacrotracker.data.database.DailyGoalDao
import com.factory.nutrilensaimacrotracker.data.database.FoodEntryDao
import com.factory.nutrilensaimacrotracker.data.database.NutriLensDatabase
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
class FoodLogViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: FoodLogViewModel
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
        every { mockFoodEntryDao.getAllLoggedDates() } returns flowOf(emptyList())
        every { mockDailyGoalDao.getGoal() } returns flowOf(DailyGoal())

        viewModel = FoodLogViewModel(mockApp)
    }

    // --- Initial State Tests ---

    @Test
    fun `selectedDate initial value is today`() = runTest {
        viewModel.selectedDate.test {
            assertEquals(today, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loggedDates initial state is empty`() = runTest {
        viewModel.loggedDates.test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selectedDateEntries initial state is empty`() = runTest {
        viewModel.selectedDateEntries.test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selectedDateTotals initial state has zero macros`() = runTest {
        viewModel.selectedDateTotals.test {
            val totals = awaitItem()
            assertEquals(0f, totals.calories)
            assertEquals(0f, totals.protein)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- selectDate Tests ---

    @Test
    fun `selectDate updates selectedDate state`() = runTest {
        val newDate = "2024-03-15"

        viewModel.selectDate(newDate)

        viewModel.selectedDate.test {
            assertEquals(newDate, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selectDate triggers re-fetch of entries for new date`() = runTest {
        val newDate = "2024-03-15"
        val newEntries = listOf(buildFoodEntry(name = "Rice", date = newDate))
        every { mockFoodEntryDao.getFoodEntriesForDate(newDate) } returns flowOf(newEntries)

        viewModel.selectDate(newDate)

        viewModel.selectedDateEntries.test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Rice", result[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selectDate triggers re-fetch of totals for new date`() = runTest {
        val newDate = "2024-03-15"
        val newTotals = DailyTotals(calories = 1500f, protein = 100f, carbs = 200f, fat = 50f)
        every { mockFoodEntryDao.getDailyTotalsForDate(newDate) } returns flowOf(newTotals)

        viewModel.selectDate(newDate)

        viewModel.selectedDateTotals.test {
            val result = awaitItem()
            assertEquals(1500f, result.calories)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- loggedDates Data Tests ---

    @Test
    fun `loggedDates reflects dates from repository`() = runTest {
        val dates = listOf("2024-03-15", "2024-03-14", "2024-03-10")
        every { mockFoodEntryDao.getAllLoggedDates() } returns flowOf(dates)

        val vm = FoodLogViewModel(mockApp)

        vm.loggedDates.test {
            val result = awaitItem()
            assertEquals(3, result.size)
            assertEquals("2024-03-15", result[0])
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- deleteFoodEntry Tests ---

    @Test
    fun `deleteFoodEntry calls dao delete with correct entry`() = runTest {
        val entry = buildFoodEntry(name = "Tacos", date = today)

        viewModel.deleteFoodEntry(entry)

        coVerify { mockFoodEntryDao.delete(entry) }
    }

    @Test
    fun `deleteFoodEntry on different date triggers correct dao call`() = runTest {
        val pastDate = "2024-03-10"
        val entry = buildFoodEntry(name = "Soup", date = pastDate)

        viewModel.deleteFoodEntry(entry)

        coVerify { mockFoodEntryDao.delete(entry) }
    }

    // --- Multiple date selection tests ---

    @Test
    fun `selectDate can be called multiple times`() = runTest {
        viewModel.selectDate("2024-01-01")
        viewModel.selectDate("2024-02-15")
        viewModel.selectDate("2024-03-20")

        viewModel.selectedDate.test {
            assertEquals("2024-03-20", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- Helper ---

    private fun buildFoodEntry(
        name: String = "Test Food",
        calories: Float = 300f,
        protein: Float = 25f,
        carbs: Float = 35f,
        fat: Float = 10f,
        date: String = today
    ) = FoodEntry(
        name = name,
        calories = calories,
        protein = protein,
        carbs = carbs,
        fat = fat,
        dateLogged = date,
        timestampMs = System.currentTimeMillis()
    )
}

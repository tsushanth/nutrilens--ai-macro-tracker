package com.factory.nutrilensaimacrotracker.viewmodel

import app.cash.turbine.test
import com.factory.nutrilensaimacrotracker.NutriLensApp
import com.factory.nutrilensaimacrotracker.data.database.DailyGoalDao
import com.factory.nutrilensaimacrotracker.data.database.NutriLensDatabase
import com.factory.nutrilensaimacrotracker.data.model.DailyGoal
import com.factory.nutrilensaimacrotracker.utils.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GoalsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: GoalsViewModel
    private val mockDailyGoalDao = mockk<DailyGoalDao>(relaxed = true)
    private val mockDatabase = mockk<NutriLensDatabase>(relaxed = true)
    private val mockApp = mockk<NutriLensApp>(relaxed = true)

    @Before
    fun setup() {
        every { mockDatabase.dailyGoalDao() } returns mockDailyGoalDao
        every { mockApp.database } returns mockDatabase
        every { mockDailyGoalDao.getGoal() } returns flowOf(DailyGoal())

        viewModel = GoalsViewModel(mockApp)
    }

    // --- Initial State Tests ---

    @Test
    fun `goal initial state has default macro targets`() = runTest {
        viewModel.goal.test {
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
    fun `goal reflects stored goal from dao`() = runTest {
        val storedGoal = DailyGoal(
            targetCalories = 1800f,
            targetProtein = 130f,
            targetCarbs = 210f,
            targetFat = 55f
        )
        every { mockDailyGoalDao.getGoal() } returns flowOf(storedGoal)

        val vm = GoalsViewModel(mockApp)

        vm.goal.test {
            val result = awaitItem()
            assertEquals(1800f, result.targetCalories)
            assertEquals(130f, result.targetProtein)
            assertEquals(210f, result.targetCarbs)
            assertEquals(55f, result.targetFat)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `goal emits default when dao returns null`() = runTest {
        every { mockDailyGoalDao.getGoal() } returns flowOf(null)

        val vm = GoalsViewModel(mockApp)

        vm.goal.test {
            val result = awaitItem()
            assertEquals(DailyGoal().targetCalories, result.targetCalories)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- updateGoal Tests ---

    @Test
    fun `updateGoal persists new macro targets to dao`() = runTest {
        viewModel.updateGoal(
            calories = 2200f,
            protein = 160f,
            carbs = 275f,
            fat = 70f
        )

        coVerify {
            mockDailyGoalDao.upsert(
                match { goal ->
                    goal.targetCalories == 2200f &&
                    goal.targetProtein == 160f &&
                    goal.targetCarbs == 275f &&
                    goal.targetFat == 70f
                }
            )
        }
    }

    @Test
    fun `updateGoal with minimum values persists correctly`() = runTest {
        viewModel.updateGoal(
            calories = 1200f,
            protein = 80f,
            carbs = 130f,
            fat = 35f
        )

        coVerify {
            mockDailyGoalDao.upsert(
                match { goal ->
                    goal.targetCalories == 1200f &&
                    goal.targetProtein == 80f
                }
            )
        }
    }

    @Test
    fun `updateGoal with zero values persists correctly`() = runTest {
        viewModel.updateGoal(
            calories = 0f,
            protein = 0f,
            carbs = 0f,
            fat = 0f
        )

        coVerify {
            mockDailyGoalDao.upsert(
                match { goal ->
                    goal.targetCalories == 0f &&
                    goal.targetProtein == 0f &&
                    goal.targetCarbs == 0f &&
                    goal.targetFat == 0f
                }
            )
        }
    }

    @Test
    fun `updateGoal calls dao exactly once`() = runTest {
        viewModel.updateGoal(2000f, 150f, 250f, 65f)

        coVerify(exactly = 1) { mockDailyGoalDao.upsert(any()) }
    }

    @Test
    fun `updateGoal called multiple times persists all updates`() = runTest {
        viewModel.updateGoal(2000f, 150f, 250f, 65f)
        viewModel.updateGoal(1800f, 130f, 220f, 55f)

        coVerify(exactly = 2) { mockDailyGoalDao.upsert(any()) }
    }

    // --- Goal value range tests ---

    @Test
    fun `updateGoal with large macro values persists correctly`() = runTest {
        viewModel.updateGoal(
            calories = 5000f,
            protein = 400f,
            carbs = 600f,
            fat = 200f
        )

        coVerify {
            mockDailyGoalDao.upsert(
                match { goal ->
                    goal.targetCalories == 5000f &&
                    goal.targetProtein == 400f
                }
            )
        }
    }

    @Test
    fun `updateGoal with fractional values persists correctly`() = runTest {
        viewModel.updateGoal(
            calories = 1750.5f,
            protein = 137.3f,
            carbs = 218.7f,
            fat = 58.2f
        )

        coVerify {
            mockDailyGoalDao.upsert(
                match { goal ->
                    goal.targetCalories == 1750.5f &&
                    goal.targetProtein == 137.3f
                }
            )
        }
    }
}

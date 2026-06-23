package com.factory.nutrilensaimacrotracker.data.repository

import app.cash.turbine.test
import com.factory.nutrilensaimacrotracker.data.database.DailyGoalDao
import com.factory.nutrilensaimacrotracker.data.model.DailyGoal
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class GoalRepositoryTest {

    private lateinit var dailyGoalDao: DailyGoalDao
    private lateinit var repository: GoalRepository

    private val defaultGoal = DailyGoal(
        id = 1,
        targetCalories = 2000f,
        targetProtein = 150f,
        targetCarbs = 250f,
        targetFat = 65f
    )

    @Before
    fun setup() {
        dailyGoalDao = mockk(relaxed = true)
        repository = GoalRepository(dailyGoalDao)
    }

    // --- getGoal Flow Tests ---

    @Test
    fun `getGoal emits DailyGoal when dao has a stored goal`() = runTest {
        every { dailyGoalDao.getGoal() } returns flowOf(defaultGoal)

        repository.getGoal().test {
            val result = awaitItem()
            assertEquals(2000f, result.targetCalories)
            assertEquals(150f, result.targetProtein)
            assertEquals(250f, result.targetCarbs)
            assertEquals(65f, result.targetFat)
            awaitComplete()
        }
    }

    @Test
    fun `getGoal emits default DailyGoal when dao returns null`() = runTest {
        every { dailyGoalDao.getGoal() } returns flowOf(null)

        repository.getGoal().test {
            val result = awaitItem()
            // DailyGoal() defaults: 2000 cal, 150 protein, 250 carbs, 65 fat
            assertEquals(2000f, result.targetCalories)
            assertEquals(150f, result.targetProtein)
            assertEquals(250f, result.targetCarbs)
            assertEquals(65f, result.targetFat)
            awaitComplete()
        }
    }

    @Test
    fun `getGoal delegates to dao getGoal`() = runTest {
        every { dailyGoalDao.getGoal() } returns flowOf(defaultGoal)

        repository.getGoal().test {
            awaitItem()
            awaitComplete()
        }

        verify { dailyGoalDao.getGoal() }
    }

    @Test
    fun `getGoal emits updated goal when dao emits new value`() = runTest {
        val updatedGoal = DailyGoal(
            targetCalories = 2500f,
            targetProtein = 180f,
            targetCarbs = 300f,
            targetFat = 80f
        )
        every { dailyGoalDao.getGoal() } returns kotlinx.coroutines.flow.flow {
            emit(defaultGoal)
            emit(updatedGoal)
        }

        repository.getGoal().test {
            val first = awaitItem()
            assertEquals(2000f, first.targetCalories)

            val second = awaitItem()
            assertEquals(2500f, second.targetCalories)
            assertEquals(180f, second.targetProtein)
            awaitComplete()
        }
    }

    // --- updateGoal Tests ---

    @Test
    fun `updateGoal calls dao upsert with provided goal`() = runTest {
        val newGoal = DailyGoal(
            targetCalories = 1800f,
            targetProtein = 130f,
            targetCarbs = 220f,
            targetFat = 55f
        )

        repository.updateGoal(newGoal)

        coVerify { dailyGoalDao.upsert(newGoal) }
    }

    @Test
    fun `updateGoal delegates to dao exactly once`() = runTest {
        repository.updateGoal(defaultGoal)

        coVerify(exactly = 1) { dailyGoalDao.upsert(defaultGoal) }
    }

    // --- getGoalOnce Tests ---

    @Test
    fun `getGoalOnce returns stored goal when dao has a value`() = runTest {
        coEvery { dailyGoalDao.getGoalOnce() } returns defaultGoal

        val result = repository.getGoalOnce()

        assertEquals(2000f, result.targetCalories)
        assertEquals(150f, result.targetProtein)
        coVerify { dailyGoalDao.getGoalOnce() }
    }

    @Test
    fun `getGoalOnce returns default DailyGoal when dao returns null`() = runTest {
        coEvery { dailyGoalDao.getGoalOnce() } returns null

        val result = repository.getGoalOnce()

        // Should return default DailyGoal()
        assertEquals(DailyGoal().targetCalories, result.targetCalories)
        assertEquals(DailyGoal().targetProtein, result.targetProtein)
        assertEquals(DailyGoal().targetCarbs, result.targetCarbs)
        assertEquals(DailyGoal().targetFat, result.targetFat)
    }

    @Test
    fun `getGoalOnce delegates to dao getGoalOnce`() = runTest {
        coEvery { dailyGoalDao.getGoalOnce() } returns defaultGoal

        repository.getGoalOnce()

        coVerify { dailyGoalDao.getGoalOnce() }
    }

    // --- Edge case: custom goal values ---

    @Test
    fun `getGoal correctly propagates custom macro targets`() = runTest {
        val customGoal = DailyGoal(
            id = 1,
            targetCalories = 1500f,
            targetProtein = 100f,
            targetCarbs = 175f,
            targetFat = 45f
        )
        every { dailyGoalDao.getGoal() } returns flowOf(customGoal)

        repository.getGoal().test {
            val result = awaitItem()
            assertEquals(1500f, result.targetCalories)
            assertEquals(100f, result.targetProtein)
            assertEquals(175f, result.targetCarbs)
            assertEquals(45f, result.targetFat)
            awaitComplete()
        }
    }
}

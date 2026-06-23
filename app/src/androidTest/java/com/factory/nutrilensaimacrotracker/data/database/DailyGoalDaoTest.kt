package com.factory.nutrilensaimacrotracker.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.factory.nutrilensaimacrotracker.data.model.DailyGoal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class DailyGoalDaoTest {

    private lateinit var database: NutriLensDatabase
    private lateinit var dao: DailyGoalDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NutriLensDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.dailyGoalDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    // --- getGoal (Flow) Tests ---

    @Test
    fun getGoalReturnsNullFlowWhenNoGoalStored() = runTest {
        dao.getGoal().test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getGoalReturnsStoredGoalAfterUpsert() = runTest {
        val goal = buildGoal(calories = 2000f, protein = 150f, carbs = 250f, fat = 65f)
        dao.upsert(goal)

        dao.getGoal().test {
            val result = awaitItem()
            assertNotNull(result)
            assertEquals(2000f, result!!.targetCalories)
            assertEquals(150f, result.targetProtein)
            assertEquals(250f, result.targetCarbs)
            assertEquals(65f, result.targetFat)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getGoalEmitsUpdatedValueAfterUpsert() = runTest {
        val initialGoal = buildGoal(calories = 2000f, protein = 150f, carbs = 250f, fat = 65f)
        dao.upsert(initialGoal)

        dao.getGoal().test {
            val first = awaitItem()
            assertEquals(2000f, first!!.targetCalories)

            val updatedGoal = buildGoal(calories = 1800f, protein = 130f, carbs = 220f, fat = 55f)
            dao.upsert(updatedGoal)

            val second = awaitItem()
            assertEquals(1800f, second!!.targetCalories)
            assertEquals(130f, second.targetProtein)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- getGoalOnce (Suspend) Tests ---

    @Test
    fun getGoalOnceReturnsNullWhenNoGoalStored() = runTest {
        val result = dao.getGoalOnce()
        assertNull(result)
    }

    @Test
    fun getGoalOnceReturnsStoredGoal() = runTest {
        val goal = buildGoal(calories = 2200f, protein = 165f, carbs = 275f, fat = 72f)
        dao.upsert(goal)

        val result = dao.getGoalOnce()
        assertNotNull(result)
        assertEquals(2200f, result!!.targetCalories)
        assertEquals(165f, result.targetProtein)
        assertEquals(275f, result.targetCarbs)
        assertEquals(72f, result.targetFat)
    }

    // --- upsert Tests ---

    @Test
    fun upsertInsertsNewGoalWhenNoneExists() = runTest {
        val goal = buildGoal(calories = 1800f, protein = 130f, carbs = 220f, fat = 55f)

        dao.upsert(goal)

        val result = dao.getGoalOnce()
        assertNotNull(result)
        assertEquals(1800f, result!!.targetCalories)
    }

    @Test
    fun upsertReplacesExistingGoalWithSameId() = runTest {
        val initialGoal = buildGoal(calories = 2000f, protein = 150f, carbs = 250f, fat = 65f)
        dao.upsert(initialGoal)

        val updatedGoal = buildGoal(calories = 2500f, protein = 180f, carbs = 300f, fat = 80f)
        dao.upsert(updatedGoal)

        val result = dao.getGoalOnce()
        assertNotNull(result)
        assertEquals(2500f, result!!.targetCalories)
        assertEquals(180f, result.targetProtein)
    }

    @Test
    fun upsertMultipleTimesKeepsSingleRecord() = runTest {
        dao.upsert(buildGoal(calories = 1600f, protein = 120f, carbs = 200f, fat = 50f))
        dao.upsert(buildGoal(calories = 1800f, protein = 135f, carbs = 225f, fat = 58f))
        dao.upsert(buildGoal(calories = 2000f, protein = 150f, carbs = 250f, fat = 65f))

        // Should still be a single record with id=1
        val result = dao.getGoalOnce()
        assertNotNull(result)
        assertEquals(1, result!!.id)
        assertEquals(2000f, result.targetCalories)
    }

    // --- Specific goal value tests ---

    @Test
    fun upsertWithZeroMacrosPersistedCorrectly() = runTest {
        val zeroGoal = buildGoal(calories = 0f, protein = 0f, carbs = 0f, fat = 0f)
        dao.upsert(zeroGoal)

        val result = dao.getGoalOnce()
        assertNotNull(result)
        assertEquals(0f, result!!.targetCalories)
        assertEquals(0f, result.targetProtein)
        assertEquals(0f, result.targetCarbs)
        assertEquals(0f, result.targetFat)
    }

    @Test
    fun upsertWithLargeMacroValuesPersistedCorrectly() = runTest {
        val largeGoal = buildGoal(calories = 9999f, protein = 999f, carbs = 999f, fat = 999f)
        dao.upsert(largeGoal)

        val result = dao.getGoalOnce()
        assertNotNull(result)
        assertEquals(9999f, result!!.targetCalories)
        assertEquals(999f, result.targetProtein)
    }

    @Test
    fun upsertWithFractionalValuesPersistedCorrectly() = runTest {
        val fractionalGoal = buildGoal(
            calories = 1876.5f,
            protein = 143.7f,
            carbs = 234.2f,
            fat = 61.8f
        )
        dao.upsert(fractionalGoal)

        val result = dao.getGoalOnce()
        assertNotNull(result)
        assertEquals(1876.5f, result!!.targetCalories)
        assertEquals(143.7f, result.targetProtein, 0.001f)
        assertEquals(234.2f, result.targetCarbs, 0.001f)
        assertEquals(61.8f, result.targetFat, 0.001f)
    }

    // --- Default goal values Tests ---

    @Test
    fun defaultGoalHasExpectedMacroTargets() = runTest {
        val defaultGoal = DailyGoal()
        assertEquals(1, defaultGoal.id)
        assertEquals(2000f, defaultGoal.targetCalories)
        assertEquals(150f, defaultGoal.targetProtein)
        assertEquals(250f, defaultGoal.targetCarbs)
        assertEquals(65f, defaultGoal.targetFat)
    }

    @Test
    fun upsertDefaultGoalThenRetrieveHasCorrectDefaults() = runTest {
        dao.upsert(DailyGoal())

        val result = dao.getGoalOnce()
        assertNotNull(result)
        assertEquals(2000f, result!!.targetCalories)
        assertEquals(150f, result.targetProtein)
        assertEquals(250f, result.targetCarbs)
        assertEquals(65f, result.targetFat)
    }

    // --- Reactive Flow update Tests ---

    @Test
    fun getGoalFlowReactsToDeletion() = runTest {
        dao.upsert(buildGoal(calories = 2000f, protein = 150f, carbs = 250f, fat = 65f))

        dao.getGoal().test {
            val first = awaitItem()
            assertNotNull(first)

            // Simulate "revoking" by upserting zero values
            dao.upsert(buildGoal(calories = 0f, protein = 0f, carbs = 0f, fat = 0f))

            val updated = awaitItem()
            assertEquals(0f, updated!!.targetCalories)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- Helper ---

    private fun buildGoal(
        calories: Float,
        protein: Float,
        carbs: Float,
        fat: Float
    ) = DailyGoal(
        id = 1,
        targetCalories = calories,
        targetProtein = protein,
        targetCarbs = carbs,
        targetFat = fat
    )
}

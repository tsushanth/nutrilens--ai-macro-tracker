package com.factory.nutrilensaimacrotracker.data.repository

import app.cash.turbine.test
import com.factory.nutrilensaimacrotracker.data.database.DailyTotals
import com.factory.nutrilensaimacrotracker.data.database.FoodEntryDao
import com.factory.nutrilensaimacrotracker.data.model.FoodEntry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class FoodRepositoryTest {

    private lateinit var foodEntryDao: FoodEntryDao
    private lateinit var repository: FoodRepository

    private val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    @Before
    fun setup() {
        foodEntryDao = mockk(relaxed = true)
        repository = FoodRepository(foodEntryDao)
    }

    // --- getFoodEntriesForToday ---

    @Test
    fun `getFoodEntriesForToday delegates to dao with today's date`() = runTest {
        val entries = listOf(buildFoodEntry(name = "Apple", date = today))
        every { foodEntryDao.getFoodEntriesForDate(today) } returns flowOf(entries)

        repository.getFoodEntriesForToday().test {
            assertEquals(entries, awaitItem())
            awaitComplete()
        }

        verify { foodEntryDao.getFoodEntriesForDate(today) }
    }

    @Test
    fun `getFoodEntriesForToday returns empty list when no entries today`() = runTest {
        every { foodEntryDao.getFoodEntriesForDate(today) } returns flowOf(emptyList())

        repository.getFoodEntriesForToday().test {
            assertTrue(awaitItem().isEmpty())
            awaitComplete()
        }
    }

    // --- getFoodEntriesForDate ---

    @Test
    fun `getFoodEntriesForDate returns entries for specified date`() = runTest {
        val date = "2024-03-15"
        val entries = listOf(
            buildFoodEntry(name = "Banana", date = date),
            buildFoodEntry(name = "Chicken", date = date)
        )
        every { foodEntryDao.getFoodEntriesForDate(date) } returns flowOf(entries)

        repository.getFoodEntriesForDate(date).test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals("Banana", result[0].name)
            awaitComplete()
        }
    }

    @Test
    fun `getFoodEntriesForDate passes correct date string to dao`() = runTest {
        val date = "2024-06-01"
        every { foodEntryDao.getFoodEntriesForDate(date) } returns flowOf(emptyList())

        repository.getFoodEntriesForDate(date).test {
            awaitItem()
            awaitComplete()
        }

        verify { foodEntryDao.getFoodEntriesForDate(date) }
    }

    // --- getAllFoodEntries ---

    @Test
    fun `getAllFoodEntries returns all entries across dates`() = runTest {
        val allEntries = listOf(
            buildFoodEntry(name = "Apple", date = "2024-03-10"),
            buildFoodEntry(name = "Rice", date = "2024-03-11"),
            buildFoodEntry(name = "Salmon", date = "2024-03-12")
        )
        every { foodEntryDao.getAllFoodEntries() } returns flowOf(allEntries)

        repository.getAllFoodEntries().test {
            val result = awaitItem()
            assertEquals(3, result.size)
            awaitComplete()
        }
    }

    // --- getDailyTotalsForToday ---

    @Test
    fun `getDailyTotalsForToday delegates to dao with today's date`() = runTest {
        val totals = DailyTotals(calories = 2000f, protein = 150f, carbs = 250f, fat = 65f)
        every { foodEntryDao.getDailyTotalsForDate(today) } returns flowOf(totals)

        repository.getDailyTotalsForToday().test {
            assertEquals(totals, awaitItem())
            awaitComplete()
        }

        verify { foodEntryDao.getDailyTotalsForDate(today) }
    }

    // --- getDailyTotalsForDate ---

    @Test
    fun `getDailyTotalsForDate returns correct aggregated totals`() = runTest {
        val date = "2024-03-15"
        val totals = DailyTotals(calories = 500f, protein = 40f, carbs = 60f, fat = 15f)
        every { foodEntryDao.getDailyTotalsForDate(date) } returns flowOf(totals)

        repository.getDailyTotalsForDate(date).test {
            val result = awaitItem()
            assertEquals(500f, result.calories)
            assertEquals(40f, result.protein)
            assertEquals(60f, result.carbs)
            assertEquals(15f, result.fat)
            awaitComplete()
        }
    }

    // --- getAllLoggedDates ---

    @Test
    fun `getAllLoggedDates returns distinct dates`() = runTest {
        val dates = listOf("2024-03-15", "2024-03-14", "2024-03-10")
        every { foodEntryDao.getAllLoggedDates() } returns flowOf(dates)

        repository.getAllLoggedDates().test {
            val result = awaitItem()
            assertEquals(3, result.size)
            assertEquals("2024-03-15", result[0])
            awaitComplete()
        }
    }

    @Test
    fun `getAllLoggedDates returns empty list when no entries`() = runTest {
        every { foodEntryDao.getAllLoggedDates() } returns flowOf(emptyList())

        repository.getAllLoggedDates().test {
            assertTrue(awaitItem().isEmpty())
            awaitComplete()
        }
    }

    // --- addFoodEntry ---

    @Test
    fun `addFoodEntry inserts entry and returns generated ID`() = runTest {
        val entry = buildFoodEntry(name = "Steak", date = today)
        coEvery { foodEntryDao.insert(entry) } returns 42L

        val result = repository.addFoodEntry(entry)

        assertEquals(42L, result)
        coVerify { foodEntryDao.insert(entry) }
    }

    @Test
    fun `addFoodEntry delegates to dao insert`() = runTest {
        val entry = buildFoodEntry(name = "Salad", date = today)
        coEvery { foodEntryDao.insert(any()) } returns 1L

        repository.addFoodEntry(entry)

        coVerify(exactly = 1) { foodEntryDao.insert(entry) }
    }

    // --- deleteFoodEntry ---

    @Test
    fun `deleteFoodEntry calls dao delete with correct entry`() = runTest {
        val entry = buildFoodEntry(name = "Pizza", date = today)

        repository.deleteFoodEntry(entry)

        coVerify { foodEntryDao.delete(entry) }
    }

    // --- deleteFoodEntryById ---

    @Test
    fun `deleteFoodEntryById calls dao deleteById with correct id`() = runTest {
        val entryId = 99L

        repository.deleteFoodEntryById(entryId)

        coVerify { foodEntryDao.deleteById(entryId) }
    }

    // --- Flow emission tests ---

    @Test
    fun `getFoodEntriesForDate emits multiple updates`() = runTest {
        val date = "2024-04-01"
        val firstEmission = listOf(buildFoodEntry(name = "Egg", date = date))
        val secondEmission = listOf(
            buildFoodEntry(name = "Egg", date = date),
            buildFoodEntry(name = "Toast", date = date)
        )
        every { foodEntryDao.getFoodEntriesForDate(date) } returns
                kotlinx.coroutines.flow.flow {
                    emit(firstEmission)
                    emit(secondEmission)
                }

        repository.getFoodEntriesForDate(date).test {
            assertEquals(1, awaitItem().size)
            assertEquals(2, awaitItem().size)
            awaitComplete()
        }
    }

    // --- Helper ---

    private fun buildFoodEntry(
        name: String = "Test Food",
        calories: Float = 200f,
        protein: Float = 20f,
        carbs: Float = 25f,
        fat: Float = 8f,
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

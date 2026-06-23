package com.factory.nutrilensaimacrotracker.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.factory.nutrilensaimacrotracker.data.model.FoodEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class FoodEntryDaoTest {

    private lateinit var database: NutriLensDatabase
    private lateinit var dao: FoodEntryDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NutriLensDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.foodEntryDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    // --- insert & getFoodEntriesForDate Tests ---

    @Test
    fun insertSingleEntryAndRetrieveByDate() = runTest {
        val entry = buildFoodEntry(name = "Apple", date = "2024-03-15")
        dao.insert(entry)

        dao.getFoodEntriesForDate("2024-03-15").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Apple", result[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun insertMultipleEntriesSameDateReturnsAllSorted() = runTest {
        dao.insert(buildFoodEntry(name = "Breakfast", date = "2024-03-15", timestampMs = 1000L))
        dao.insert(buildFoodEntry(name = "Lunch", date = "2024-03-15", timestampMs = 3000L))
        dao.insert(buildFoodEntry(name = "Dinner", date = "2024-03-15", timestampMs = 2000L))

        dao.getFoodEntriesForDate("2024-03-15").test {
            val result = awaitItem()
            assertEquals(3, result.size)
            // Ordered by timestampMs DESC: Lunch (3000), Dinner (2000), Breakfast (1000)
            assertEquals("Lunch", result[0].name)
            assertEquals("Dinner", result[1].name)
            assertEquals("Breakfast", result[2].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun insertEntriesDifferentDatesReturnsOnlyRequestedDate() = runTest {
        dao.insert(buildFoodEntry(name = "Monday Meal", date = "2024-03-11"))
        dao.insert(buildFoodEntry(name = "Tuesday Meal", date = "2024-03-12"))
        dao.insert(buildFoodEntry(name = "Wednesday Meal", date = "2024-03-13"))

        dao.getFoodEntriesForDate("2024-03-12").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Tuesday Meal", result[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getFoodEntriesForDateReturnsEmptyListWhenNoneExist() = runTest {
        dao.getFoodEntriesForDate("2024-03-15").test {
            val result = awaitItem()
            assertTrue(result.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun insertReturnsGeneratedId() = runTest {
        val id = dao.insert(buildFoodEntry(name = "Test"))
        assertNotEquals(0L, id)
    }

    // --- getAllFoodEntries Tests ---

    @Test
    fun getAllFoodEntriesReturnsAllEntriesAcrossDates() = runTest {
        dao.insert(buildFoodEntry(name = "Entry1", date = "2024-03-10"))
        dao.insert(buildFoodEntry(name = "Entry2", date = "2024-03-11"))
        dao.insert(buildFoodEntry(name = "Entry3", date = "2024-03-12"))

        dao.getAllFoodEntries().test {
            val result = awaitItem()
            assertEquals(3, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getAllFoodEntriesReturnsEmptyWhenNoEntries() = runTest {
        dao.getAllFoodEntries().test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getAllFoodEntriesOrderedByTimestampDesc() = runTest {
        dao.insert(buildFoodEntry(name = "First", date = "2024-03-10", timestampMs = 1000L))
        dao.insert(buildFoodEntry(name = "Second", date = "2024-03-11", timestampMs = 3000L))
        dao.insert(buildFoodEntry(name = "Third", date = "2024-03-12", timestampMs = 2000L))

        dao.getAllFoodEntries().test {
            val result = awaitItem()
            assertEquals("Second", result[0].name)
            assertEquals("Third", result[1].name)
            assertEquals("First", result[2].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- delete Tests ---

    @Test
    fun deleteRemovesEntryFromDatabase() = runTest {
        val id = dao.insert(buildFoodEntry(name = "ToDelete", date = "2024-03-15"))

        // Retrieve inserted entry by id to get the correct object
        dao.getAllFoodEntries().test {
            val entries = awaitItem()
            val inserted = entries.first { it.id == id }
            cancelAndIgnoreRemainingEvents()

            dao.delete(inserted)
        }

        dao.getFoodEntriesForDate("2024-03-15").test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteOnlyRemovesTargetEntry() = runTest {
        val id1 = dao.insert(buildFoodEntry(name = "Keep", date = "2024-03-15", timestampMs = 1000L))
        val id2 = dao.insert(buildFoodEntry(name = "Delete", date = "2024-03-15", timestampMs = 2000L))

        dao.getAllFoodEntries().test {
            val allEntries = awaitItem()
            val entryToDelete = allEntries.first { it.id == id2 }
            cancelAndIgnoreRemainingEvents()

            dao.delete(entryToDelete)
        }

        dao.getFoodEntriesForDate("2024-03-15").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Keep", result[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- deleteById Tests ---

    @Test
    fun deleteByIdRemovesCorrectEntry() = runTest {
        val id = dao.insert(buildFoodEntry(name = "ById", date = "2024-03-15"))

        dao.deleteById(id)

        dao.getFoodEntriesForDate("2024-03-15").test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteByIdWithNonExistentIdDoesNotThrow() = runTest {
        // Should not throw
        dao.deleteById(99999L)
    }

    // --- getDailyTotalsForDate Tests ---

    @Test
    fun getDailyTotalsForDateSumsAllMacros() = runTest {
        dao.insert(buildFoodEntry(
            name = "Food1", date = "2024-03-15",
            calories = 300f, protein = 25f, carbs = 40f, fat = 10f
        ))
        dao.insert(buildFoodEntry(
            name = "Food2", date = "2024-03-15",
            calories = 200f, protein = 15f, carbs = 30f, fat = 8f
        ))

        dao.getDailyTotalsForDate("2024-03-15").test {
            val totals = awaitItem()
            assertEquals(500f, totals.calories)
            assertEquals(40f, totals.protein)
            assertEquals(70f, totals.carbs)
            assertEquals(18f, totals.fat)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getDailyTotalsForDateReturnsZerosWhenNoEntries() = runTest {
        dao.getDailyTotalsForDate("2024-03-15").test {
            val totals = awaitItem()
            assertEquals(0f, totals.calories)
            assertEquals(0f, totals.protein)
            assertEquals(0f, totals.carbs)
            assertEquals(0f, totals.fat)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getDailyTotalsOnlyIncludesEntriesForSpecifiedDate() = runTest {
        dao.insert(buildFoodEntry(
            name = "Today", date = "2024-03-15",
            calories = 400f, protein = 30f, carbs = 50f, fat = 12f
        ))
        dao.insert(buildFoodEntry(
            name = "Yesterday", date = "2024-03-14",
            calories = 1000f, protein = 80f, carbs = 120f, fat = 40f
        ))

        dao.getDailyTotalsForDate("2024-03-15").test {
            val totals = awaitItem()
            assertEquals(400f, totals.calories)
            assertEquals(30f, totals.protein)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getDailyTotalsUpdateAfterInsert() = runTest {
        dao.insert(buildFoodEntry(
            name = "Meal1", date = "2024-03-15",
            calories = 500f, protein = 40f, carbs = 60f, fat = 15f
        ))

        dao.getDailyTotalsForDate("2024-03-15").test {
            val first = awaitItem()
            assertEquals(500f, first.calories)

            dao.insert(buildFoodEntry(
                name = "Meal2", date = "2024-03-15",
                calories = 300f, protein = 20f, carbs = 40f, fat = 10f
            ))

            val updated = awaitItem()
            assertEquals(800f, updated.calories)
            assertEquals(60f, updated.protein)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- getAllLoggedDates Tests ---

    @Test
    fun getAllLoggedDatesReturnsDistinctDates() = runTest {
        dao.insert(buildFoodEntry(name = "A", date = "2024-03-15"))
        dao.insert(buildFoodEntry(name = "B", date = "2024-03-15")) // Same date
        dao.insert(buildFoodEntry(name = "C", date = "2024-03-14"))

        dao.getAllLoggedDates().test {
            val dates = awaitItem()
            assertEquals(2, dates.size)
            assertTrue(dates.contains("2024-03-15"))
            assertTrue(dates.contains("2024-03-14"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getAllLoggedDatesOrderedDescending() = runTest {
        dao.insert(buildFoodEntry(name = "Old", date = "2024-01-10"))
        dao.insert(buildFoodEntry(name = "New", date = "2024-03-20"))
        dao.insert(buildFoodEntry(name = "Mid", date = "2024-02-15"))

        dao.getAllLoggedDates().test {
            val dates = awaitItem()
            assertEquals("2024-03-20", dates[0])
            assertEquals("2024-02-15", dates[1])
            assertEquals("2024-01-10", dates[2])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getAllLoggedDatesReturnsEmptyWhenNoEntries() = runTest {
        dao.getAllLoggedDates().test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getAllLoggedDatesUpdatesWhenEntryAdded() = runTest {
        dao.getAllLoggedDates().test {
            assertTrue(awaitItem().isEmpty())

            dao.insert(buildFoodEntry(name = "New Entry", date = "2024-03-15"))

            val updated = awaitItem()
            assertEquals(1, updated.size)
            assertEquals("2024-03-15", updated[0])
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- REPLACE conflict strategy Tests ---

    @Test
    fun insertWithSameIdReplacesExistingEntry() = runTest {
        val entry = buildFoodEntry(name = "Original", date = "2024-03-15")
        val id = dao.insert(entry)

        // Insert an entry with the same auto-generated ID by updating through replace
        val updatedEntry = FoodEntry(
            id = id,
            name = "Updated",
            calories = 999f,
            protein = 99f,
            carbs = 88f,
            fat = 77f,
            dateLogged = "2024-03-15",
            timestampMs = System.currentTimeMillis()
        )
        dao.insert(updatedEntry)

        dao.getFoodEntriesForDate("2024-03-15").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Updated", result[0].name)
            assertEquals(999f, result[0].calories)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- All macro fields persisted correctly ---

    @Test
    fun allFoodEntryFieldsPersistedCorrectly() = runTest {
        val entry = FoodEntry(
            name = "Complete Food",
            calories = 450f,
            protein = 35f,
            carbs = 55f,
            fat = 12f,
            servingSize = "1 cup (240g)",
            imageUri = "content://media/external/images/1234",
            notes = "Delicious meal",
            dateLogged = "2024-03-15",
            timestampMs = 1710547200000L
        )
        dao.insert(entry)

        dao.getFoodEntriesForDate("2024-03-15").test {
            val result = awaitItem().first()
            assertEquals("Complete Food", result.name)
            assertEquals(450f, result.calories)
            assertEquals(35f, result.protein)
            assertEquals(55f, result.carbs)
            assertEquals(12f, result.fat)
            assertEquals("1 cup (240g)", result.servingSize)
            assertEquals("content://media/external/images/1234", result.imageUri)
            assertEquals("Delicious meal", result.notes)
            assertEquals("2024-03-15", result.dateLogged)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- Helper ---

    private fun buildFoodEntry(
        name: String = "Test Food",
        calories: Float = 200f,
        protein: Float = 20f,
        carbs: Float = 25f,
        fat: Float = 8f,
        date: String = "2024-03-15",
        timestampMs: Long = System.currentTimeMillis()
    ) = FoodEntry(
        name = name,
        calories = calories,
        protein = protein,
        carbs = carbs,
        fat = fat,
        dateLogged = date,
        timestampMs = timestampMs
    )
}

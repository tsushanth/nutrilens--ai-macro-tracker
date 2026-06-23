package com.factory.nutrilensaimacrotracker.data.database

import androidx.room.*
import com.factory.nutrilensaimacrotracker.data.model.FoodEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(foodEntry: FoodEntry): Long

    @Delete
    suspend fun delete(foodEntry: FoodEntry)

    @Query("DELETE FROM food_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM food_entries WHERE dateLogged = :date ORDER BY timestampMs DESC")
    fun getFoodEntriesForDate(date: String): Flow<List<FoodEntry>>

    @Query("SELECT * FROM food_entries ORDER BY timestampMs DESC")
    fun getAllFoodEntries(): Flow<List<FoodEntry>>

    @Query("""
        SELECT COALESCE(SUM(calories), 0) as calories,
               COALESCE(SUM(protein), 0) as protein,
               COALESCE(SUM(carbs), 0) as carbs,
               COALESCE(SUM(fat), 0) as fat
        FROM food_entries
        WHERE dateLogged = :date
    """)
    fun getDailyTotalsForDate(date: String): Flow<DailyTotals>

    @Query("SELECT DISTINCT dateLogged FROM food_entries ORDER BY dateLogged DESC")
    fun getAllLoggedDates(): Flow<List<String>>
}

data class DailyTotals(
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float
)

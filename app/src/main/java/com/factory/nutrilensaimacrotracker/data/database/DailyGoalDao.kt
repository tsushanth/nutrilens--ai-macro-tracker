package com.factory.nutrilensaimacrotracker.data.database

import androidx.room.*
import com.factory.nutrilensaimacrotracker.data.model.DailyGoal
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyGoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: DailyGoal)

    @Query("SELECT * FROM daily_goals WHERE id = 1")
    fun getGoal(): Flow<DailyGoal?>

    @Query("SELECT * FROM daily_goals WHERE id = 1")
    suspend fun getGoalOnce(): DailyGoal?
}

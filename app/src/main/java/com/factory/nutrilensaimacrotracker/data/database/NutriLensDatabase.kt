package com.factory.nutrilensaimacrotracker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.factory.nutrilensaimacrotracker.data.model.DailyGoal
import com.factory.nutrilensaimacrotracker.data.model.FoodEntry

@Database(
    entities = [FoodEntry::class, DailyGoal::class],
    version = 1,
    exportSchema = false
)
abstract class NutriLensDatabase : RoomDatabase() {
    abstract fun foodEntryDao(): FoodEntryDao
    abstract fun dailyGoalDao(): DailyGoalDao

    companion object {
        @Volatile
        private var INSTANCE: NutriLensDatabase? = null

        fun getDatabase(context: Context): NutriLensDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NutriLensDatabase::class.java,
                    "nutrilens_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

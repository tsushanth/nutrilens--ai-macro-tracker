package com.factory.nutrilensaimacrotracker.data.repository

import com.factory.nutrilensaimacrotracker.data.database.DailyTotals
import com.factory.nutrilensaimacrotracker.data.database.FoodEntryDao
import com.factory.nutrilensaimacrotracker.data.model.FoodEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class FoodRepository(private val foodEntryDao: FoodEntryDao) {

    fun getFoodEntriesForToday(): Flow<List<FoodEntry>> {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return foodEntryDao.getFoodEntriesForDate(today)
    }

    fun getFoodEntriesForDate(date: String): Flow<List<FoodEntry>> {
        return foodEntryDao.getFoodEntriesForDate(date)
    }

    fun getAllFoodEntries(): Flow<List<FoodEntry>> {
        return foodEntryDao.getAllFoodEntries()
    }

    fun getDailyTotalsForToday(): Flow<DailyTotals> {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return foodEntryDao.getDailyTotalsForDate(today)
    }

    fun getDailyTotalsForDate(date: String): Flow<DailyTotals> {
        return foodEntryDao.getDailyTotalsForDate(date)
    }

    fun getAllLoggedDates(): Flow<List<String>> {
        return foodEntryDao.getAllLoggedDates()
    }

    suspend fun addFoodEntry(foodEntry: FoodEntry): Long {
        return foodEntryDao.insert(foodEntry)
    }

    suspend fun deleteFoodEntry(foodEntry: FoodEntry) {
        foodEntryDao.delete(foodEntry)
    }

    suspend fun deleteFoodEntryById(id: Long) {
        foodEntryDao.deleteById(id)
    }
}

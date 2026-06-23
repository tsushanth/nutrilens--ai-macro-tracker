package com.factory.nutrilensaimacrotracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.factory.nutrilensaimacrotracker.NutriLensApp
import com.factory.nutrilensaimacrotracker.data.database.DailyTotals
import com.factory.nutrilensaimacrotracker.data.model.DailyGoal
import com.factory.nutrilensaimacrotracker.data.model.FoodEntry
import com.factory.nutrilensaimacrotracker.data.repository.FoodRepository
import com.factory.nutrilensaimacrotracker.data.repository.GoalRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val database = (application as NutriLensApp).database
    private val foodRepository = FoodRepository(database.foodEntryDao())
    private val goalRepository = GoalRepository(database.dailyGoalDao())

    val todayEntries: StateFlow<List<FoodEntry>> = foodRepository
        .getFoodEntriesForToday()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayTotals: StateFlow<DailyTotals> = foodRepository
        .getDailyTotalsForToday()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyTotals(0f, 0f, 0f, 0f))

    val dailyGoal: StateFlow<DailyGoal> = goalRepository
        .getGoal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyGoal())

    fun deleteFoodEntry(entry: FoodEntry) {
        viewModelScope.launch {
            foodRepository.deleteFoodEntry(entry)
        }
    }
}

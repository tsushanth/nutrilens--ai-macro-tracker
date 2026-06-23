package com.factory.nutrilensaimacrotracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.factory.nutrilensaimacrotracker.NutriLensApp
import com.factory.nutrilensaimacrotracker.data.model.DailyGoal
import com.factory.nutrilensaimacrotracker.data.repository.GoalRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GoalsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = (application as NutriLensApp).database
    private val goalRepository = GoalRepository(database.dailyGoalDao())

    val goal: StateFlow<DailyGoal> = goalRepository
        .getGoal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyGoal())

    fun updateGoal(calories: Float, protein: Float, carbs: Float, fat: Float) {
        viewModelScope.launch {
            goalRepository.updateGoal(
                DailyGoal(
                    targetCalories = calories,
                    targetProtein = protein,
                    targetCarbs = carbs,
                    targetFat = fat
                )
            )
        }
    }
}

package com.factory.nutrilensaimacrotracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_goals")
data class DailyGoal(
    @PrimaryKey
    val id: Int = 1,
    val targetCalories: Float = 2000f,
    val targetProtein: Float = 150f,
    val targetCarbs: Float = 250f,
    val targetFat: Float = 65f
)

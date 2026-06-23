package com.factory.nutrilensaimacrotracker.data.repository

import com.factory.nutrilensaimacrotracker.data.database.DailyGoalDao
import com.factory.nutrilensaimacrotracker.data.model.DailyGoal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GoalRepository(private val dailyGoalDao: DailyGoalDao) {

    fun getGoal(): Flow<DailyGoal> {
        return dailyGoalDao.getGoal().map { it ?: DailyGoal() }
    }

    suspend fun updateGoal(goal: DailyGoal) {
        dailyGoalDao.upsert(goal)
    }

    suspend fun getGoalOnce(): DailyGoal {
        return dailyGoalDao.getGoalOnce() ?: DailyGoal()
    }
}

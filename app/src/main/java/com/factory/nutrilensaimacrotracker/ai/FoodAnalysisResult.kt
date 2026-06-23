package com.factory.nutrilensaimacrotracker.ai

data class FoodAnalysisResult(
    val foodName: String,
    val servingSize: String,
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val fiber: Float = 0f,
    val sugar: Float = 0f,
    val notes: String = "",
    val confidence: Float = 0.9f
)

sealed class AnalysisState {
    data object Idle : AnalysisState()
    data object Loading : AnalysisState()
    data class Success(val result: FoodAnalysisResult) : AnalysisState()
    data class Error(val message: String) : AnalysisState()
}

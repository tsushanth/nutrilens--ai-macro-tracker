package com.factory.nutrilensaimacrotracker.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.factory.nutrilensaimacrotracker.NutriLensApp
import com.factory.nutrilensaimacrotracker.ai.AnalysisState
import com.factory.nutrilensaimacrotracker.ai.ClaudeAIService
import com.factory.nutrilensaimacrotracker.ai.FoodAnalysisResult
import com.factory.nutrilensaimacrotracker.data.model.FoodEntry
import com.factory.nutrilensaimacrotracker.data.repository.FoodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ScanViewModel(application: Application) : AndroidViewModel(application) {
    private val database = (application as NutriLensApp).database
    private val foodRepository = FoodRepository(database.foodEntryDao())
    private val claudeService = ClaudeAIService()

    private val _analysisState = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val analysisState: StateFlow<AnalysisState> = _analysisState.asStateFlow()

    private val _savedSuccess = MutableStateFlow(false)
    val savedSuccess: StateFlow<Boolean> = _savedSuccess.asStateFlow()

    fun analyzeFromText(foodDescription: String) {
        if (foodDescription.isBlank()) return
        viewModelScope.launch {
            _analysisState.value = AnalysisState.Loading
            val result = claudeService.analyzeFoodFromText(foodDescription)
            _analysisState.value = result.fold(
                onSuccess = { AnalysisState.Success(it) },
                onFailure = { AnalysisState.Error(it.message ?: "Analysis failed") }
            )
        }
    }

    fun analyzeFromBitmap(bitmap: Bitmap, context: String = "") {
        viewModelScope.launch {
            _analysisState.value = AnalysisState.Loading
            val result = claudeService.analyzeFoodFromImage(bitmap, context)
            _analysisState.value = result.fold(
                onSuccess = { AnalysisState.Success(it) },
                onFailure = { AnalysisState.Error(it.message ?: "Image analysis failed") }
            )
        }
    }

    fun saveFoodEntry(result: FoodAnalysisResult, imageUri: String? = null) {
        viewModelScope.launch {
            val entry = FoodEntry(
                name = result.foodName,
                calories = result.calories,
                protein = result.protein,
                carbs = result.carbs,
                fat = result.fat,
                servingSize = result.servingSize,
                imageUri = imageUri,
                notes = result.notes,
                dateLogged = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                timestampMs = System.currentTimeMillis()
            )
            foodRepository.addFoodEntry(entry)
            _savedSuccess.value = true
        }
    }

    fun resetState() {
        _analysisState.value = AnalysisState.Idle
        _savedSuccess.value = false
    }
}

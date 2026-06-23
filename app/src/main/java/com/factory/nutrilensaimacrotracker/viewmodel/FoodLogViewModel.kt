package com.factory.nutrilensaimacrotracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.factory.nutrilensaimacrotracker.NutriLensApp
import com.factory.nutrilensaimacrotracker.data.database.DailyTotals
import com.factory.nutrilensaimacrotracker.data.model.FoodEntry
import com.factory.nutrilensaimacrotracker.data.repository.FoodRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class FoodLogViewModel(application: Application) : AndroidViewModel(application) {
    private val database = (application as NutriLensApp).database
    private val foodRepository = FoodRepository(database.foodEntryDao())

    private val _selectedDate = MutableStateFlow(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    val loggedDates: StateFlow<List<String>> = foodRepository
        .getAllLoggedDates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedDateEntries: StateFlow<List<FoodEntry>> = _selectedDate
        .flatMapLatest { date -> foodRepository.getFoodEntriesForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedDateTotals: StateFlow<DailyTotals> = _selectedDate
        .flatMapLatest { date -> foodRepository.getDailyTotalsForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyTotals(0f, 0f, 0f, 0f))

    fun selectDate(date: String) {
        _selectedDate.value = date
    }

    fun deleteFoodEntry(entry: FoodEntry) {
        viewModelScope.launch {
            foodRepository.deleteFoodEntry(entry)
        }
    }
}

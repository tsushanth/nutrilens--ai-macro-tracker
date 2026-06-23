package com.factory.nutrilensaimacrotracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Entity(tableName = "food_entries")
data class FoodEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val servingSize: String = "1 serving",
    val imageUri: String? = null,
    val notes: String? = null,
    val dateLogged: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val timestampMs: Long = System.currentTimeMillis()
)

package com.factory.nutrilensaimacrotracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.factory.nutrilensaimacrotracker.NutriLensApp
import com.factory.nutrilensaimacrotracker.data.model.FoodEntry
import com.factory.nutrilensaimacrotracker.data.repository.FoodRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodScreen(
    onNavigateBack: () -> Unit,
    prefillName: String = "",
    prefillCalories: String = "",
    prefillProtein: String = "",
    prefillCarbs: String = "",
    prefillFat: String = "",
    prefillServing: String = ""
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val database = (context.applicationContext as NutriLensApp).database
    val foodRepository = FoodRepository(database.foodEntryDao())

    var foodName by remember { mutableStateOf(prefillName) }
    var calories by remember { mutableStateOf(prefillCalories) }
    var protein by remember { mutableStateOf(prefillProtein) }
    var carbs by remember { mutableStateOf(prefillCarbs) }
    var fat by remember { mutableStateOf(prefillFat) }
    var servingSize by remember { mutableStateOf(prefillServing.ifBlank { "1 serving" }) }
    var notes by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf(false) }
    var caloriesError by remember { mutableStateOf(false) }

    val servingFocusRequester = remember { FocusRequester() }
    val caloriesFocusRequester = remember { FocusRequester() }
    val proteinFocusRequester = remember { FocusRequester() }
    val carbsFocusRequester = remember { FocusRequester() }
    val fatFocusRequester = remember { FocusRequester() }
    val notesFocusRequester = remember { FocusRequester() }

    fun validate(): Boolean {
        nameError = foodName.isBlank()
        caloriesError = calories.isBlank() || calories.toFloatOrNull() == null
        return !nameError && !caloriesError
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Food") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = foodName,
                onValueChange = {
                    foodName = it
                    nameError = false
                },
                label = { Text("Food Name *") },
                modifier = Modifier.fillMaxWidth(),
                isError = nameError,
                supportingText = if (nameError) ({ Text("Food name is required") }) else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { servingFocusRequester.requestFocus() })
            )

            OutlinedTextField(
                value = servingSize,
                onValueChange = { servingSize = it },
                label = { Text("Serving Size") },
                placeholder = { Text("e.g., 100g, 1 cup, 1 slice") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(servingFocusRequester),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { caloriesFocusRequester.requestFocus() })
            )

            HorizontalDivider()
            Text(
                text = "Nutrition per serving",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = calories,
                onValueChange = {
                    calories = it
                    caloriesError = false
                },
                label = { Text("Calories (kcal) *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(caloriesFocusRequester),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { proteinFocusRequester.requestFocus() }),
                isError = caloriesError,
                supportingText = if (caloriesError) ({ Text("Valid calorie amount required") }) else null,
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = protein,
                    onValueChange = { protein = it },
                    label = { Text("Protein (g)") },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(proteinFocusRequester),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { carbsFocusRequester.requestFocus() }),
                    singleLine = true
                )
                OutlinedTextField(
                    value = carbs,
                    onValueChange = { carbs = it },
                    label = { Text("Carbs (g)") },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(carbsFocusRequester),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { fatFocusRequester.requestFocus() }),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = fat,
                onValueChange = { fat = it },
                label = { Text("Fat (g)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(fatFocusRequester),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { notesFocusRequester.requestFocus() }),
                singleLine = true
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(notesFocusRequester),
                minLines = 2,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (validate()) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        focusManager.clearFocus()
                        scope.launch {
                            val entry = FoodEntry(
                                name = foodName.trim(),
                                calories = calories.toFloatOrNull() ?: 0f,
                                protein = protein.toFloatOrNull() ?: 0f,
                                carbs = carbs.toFloatOrNull() ?: 0f,
                                fat = fat.toFloatOrNull() ?: 0f,
                                servingSize = servingSize.trim(),
                                notes = notes.trim().ifBlank { null },
                                dateLogged = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                                timestampMs = System.currentTimeMillis()
                            )
                            foodRepository.addFoodEntry(entry)
                            onNavigateBack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text("Save to Log")
            }
        }
    }
}

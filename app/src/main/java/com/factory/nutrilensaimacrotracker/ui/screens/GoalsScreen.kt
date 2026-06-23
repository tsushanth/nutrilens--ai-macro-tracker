package com.factory.nutrilensaimacrotracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.factory.nutrilensaimacrotracker.ui.theme.CalorieColor
import com.factory.nutrilensaimacrotracker.ui.theme.CarbsColor
import com.factory.nutrilensaimacrotracker.ui.theme.FatColorDark
import com.factory.nutrilensaimacrotracker.ui.theme.ProteinColor
import com.factory.nutrilensaimacrotracker.viewmodel.GoalsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    isPremium: Boolean,
    onNavigateToPaywall: () -> Unit,
    viewModel: GoalsViewModel = viewModel()
) {
    val goal by viewModel.goal.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current

    var calories by remember(goal) { mutableStateOf(goal.targetCalories.toInt().toString()) }
    var protein by remember(goal) { mutableStateOf(goal.targetProtein.toInt().toString()) }
    var carbs by remember(goal) { mutableStateOf(goal.targetCarbs.toInt().toString()) }
    var fat by remember(goal) { mutableStateOf(goal.targetFat.toInt().toString()) }
    var saved by remember { mutableStateOf(false) }

    var caloriesError by remember { mutableStateOf(false) }
    var proteinError by remember { mutableStateOf(false) }
    var carbsError by remember { mutableStateOf(false) }
    var fatError by remember { mutableStateOf(false) }

    val proteinFocusRequester = remember { FocusRequester() }
    val carbsFocusRequester = remember { FocusRequester() }
    val fatFocusRequester = remember { FocusRequester() }

    LaunchedEffect(saved) {
        if (saved) {
            kotlinx.coroutines.delay(2000)
            saved = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Daily Goals", fontWeight = FontWeight.Bold)
                        if (!isPremium) ProBadge()
                    }
                },
                actions = {
                    if (!isPremium) {
                        TextButton(
                            onClick = onNavigateToPaywall,
                            modifier = Modifier.semantics { contentDescription = "Upgrade to Pro to set custom goals" }
                        ) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Upgrade",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Info / upgrade card
            if (isPremium) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Set your daily macro targets",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "These goals help track your progress on the home screen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                Card(
                    onClick = onNavigateToPaywall,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Custom goals require Pro",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Tap to unlock precision macro targeting",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Goal fields — enabled only for premium users
            GoalTextField(
                value = calories,
                onValueChange = { calories = it; caloriesError = false },
                label = "Daily Calories",
                unit = "kcal",
                color = CalorieColor,
                isError = caloriesError,
                enabled = isPremium,
                imeAction = ImeAction.Next,
                keyboardActions = KeyboardActions(onNext = { proteinFocusRequester.requestFocus() })
            )

            GoalTextField(
                value = protein,
                onValueChange = { protein = it; proteinError = false },
                label = "Protein",
                unit = "grams",
                color = ProteinColor,
                isError = proteinError,
                enabled = isPremium,
                focusRequester = proteinFocusRequester,
                imeAction = ImeAction.Next,
                keyboardActions = KeyboardActions(onNext = { carbsFocusRequester.requestFocus() })
            )

            GoalTextField(
                value = carbs,
                onValueChange = { carbs = it; carbsError = false },
                label = "Carbohydrates",
                unit = "grams",
                color = CarbsColor,
                isError = carbsError,
                enabled = isPremium,
                focusRequester = carbsFocusRequester,
                imeAction = ImeAction.Next,
                keyboardActions = KeyboardActions(onNext = { fatFocusRequester.requestFocus() })
            )

            GoalTextField(
                value = fat,
                onValueChange = { fat = it; fatError = false },
                label = "Fat",
                unit = "grams",
                color = FatColorDark,
                isError = fatError,
                enabled = isPremium,
                focusRequester = fatFocusRequester,
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (saved) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Goals saved successfully!",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Save button — opens paywall for free users
            Button(
                onClick = {
                    if (!isPremium) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToPaywall()
                        return@Button
                    }
                    caloriesError = calories.toFloatOrNull() == null
                    proteinError = protein.toFloatOrNull() == null
                    carbsError = carbs.toFloatOrNull() == null
                    fatError = fat.toFloatOrNull() == null
                    if (!caloriesError && !proteinError && !carbsError && !fatError) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        focusManager.clearFocus()
                        viewModel.updateGoal(
                            calories.toFloat(),
                            protein.toFloat(),
                            carbs.toFloat(),
                            fat.toFloat()
                        )
                        saved = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                if (!isPremium) {
                    Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isPremium) "Save Goals" else "Unlock to Save Goals")
            }

            // Quick presets — available to all users as teasers, but saving requires premium
            Text(
                text = "Quick Presets",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            if (!isPremium) {
                Text(
                    text = "Upgrade to Pro to apply and save presets",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PresetButton(
                    label = "Cut",
                    description = "1500 kcal",
                    isPremium = isPremium,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (isPremium) {
                            calories = "1500"; protein = "150"; carbs = "130"; fat = "50"
                        } else {
                            onNavigateToPaywall()
                        }
                    }
                )
                PresetButton(
                    label = "Maintain",
                    description = "2000 kcal",
                    isPremium = isPremium,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (isPremium) {
                            calories = "2000"; protein = "150"; carbs = "225"; fat = "65"
                        } else {
                            onNavigateToPaywall()
                        }
                    }
                )
                PresetButton(
                    label = "Bulk",
                    description = "2500 kcal",
                    isPremium = isPremium,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (isPremium) {
                            calories = "2500"; protein = "180"; carbs = "300"; fat = "80"
                        } else {
                            onNavigateToPaywall()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun GoalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    unit: String,
    color: androidx.compose.ui.graphics.Color,
    isError: Boolean = false,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        suffix = { Text(unit, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier = if (focusRequester != null)
            Modifier.fillMaxWidth().focusRequester(focusRequester)
        else
            Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = imeAction
        ),
        keyboardActions = keyboardActions,
        singleLine = true,
        isError = isError,
        enabled = enabled,
        supportingText = if (isError) ({ Text("Enter a valid number") }) else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = color,
            focusedLabelColor = color
        )
    )
}

@Composable
private fun PresetButton(
    label: String,
    description: String,
    isPremium: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    OutlinedButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier.semantics {
            contentDescription = "$label preset: $description${if (!isPremium) " — Pro required" else ""}"
        }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge)
                if (!isPremium) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

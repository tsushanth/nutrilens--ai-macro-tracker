package com.factory.nutrilensaimacrotracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.factory.nutrilensaimacrotracker.ui.components.FoodEntryCard
import com.factory.nutrilensaimacrotracker.ui.theme.CalorieColor
import com.factory.nutrilensaimacrotracker.ui.theme.CarbsColor
import com.factory.nutrilensaimacrotracker.ui.theme.FatColorDark
import com.factory.nutrilensaimacrotracker.ui.theme.ProteinColor
import com.factory.nutrilensaimacrotracker.viewmodel.FoodLogViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodLogScreen(
    isPremium: Boolean,
    onNavigateToPaywall: () -> Unit,
    viewModel: FoodLogViewModel = viewModel()
) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val loggedDates by viewModel.loggedDates.collectAsStateWithLifecycle()
    val entries by viewModel.selectedDateEntries.collectAsStateWithLifecycle()
    val totals by viewModel.selectedDateTotals.collectAsStateWithLifecycle()

    val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    // Free users only see today; premium users see full history
    val displayDates = remember(loggedDates, isPremium) {
        if (isPremium) {
            val dates = loggedDates.toMutableList()
            if (!dates.contains(today)) dates.add(0, today)
            dates.sortedDescending()
        } else {
            listOf(today)
        }
    }

    // Keep free users on today always
    LaunchedEffect(isPremium) {
        if (!isPremium && selectedDate != today) {
            viewModel.selectDate(today)
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
                        Text("Food Log", fontWeight = FontWeight.Bold)
                        if (!isPremium) {
                            ProBadge()
                        }
                    }
                },
                actions = {
                    if (!isPremium) {
                        TextButton(
                            onClick = onNavigateToPaywall,
                            modifier = Modifier.semantics { contentDescription = "Unlock full food log history with Pro" }
                        ) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Unlock History",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Date picker row
            item {
                Column {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayDates) { date ->
                            DateChip(
                                date = date,
                                isSelected = date == selectedDate,
                                onClick = { viewModel.selectDate(date) }
                            )
                        }
                        // History locked teaser chip for free users
                        if (!isPremium) {
                            item {
                                FilterChip(
                                    selected = false,
                                    onClick = onNavigateToPaywall,
                                    modifier = Modifier.semantics {
                                        contentDescription = "Unlock history — Pro feature"
                                    },
                                    label = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.Lock,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text("History")
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Paywall banner for free users
                    if (!isPremium) {
                        Card(
                            onClick = onNavigateToPaywall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
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
                                        text = "Viewing today only",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Upgrade to Pro to browse your full history",
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
                }
            }

            // Daily totals summary
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MacroStat(label = "Cal", value = totals.calories.toInt().toString(), color = CalorieColor)
                        VerticalDivider(modifier = Modifier.height(40.dp))
                        MacroStat(label = "Protein", value = "${totals.protein.toInt()}g", color = ProteinColor)
                        VerticalDivider(modifier = Modifier.height(40.dp))
                        MacroStat(label = "Carbs", value = "${totals.carbs.toInt()}g", color = CarbsColor)
                        VerticalDivider(modifier = Modifier.height(40.dp))
                        MacroStat(label = "Fat", value = "${totals.fat.toInt()}g", color = FatColorDark)
                    }
                }
            }

            if (entries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.RestaurantMenu,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Text(
                                text = "No food logged for this date",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Scan or add food to start tracking",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                item {
                    Text(
                        text = "${entries.size} item${if (entries.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                items(entries, key = { it.id }) { entry ->
                    FoodEntryCard(
                        entry = entry,
                        onDelete = { viewModel.deleteFoodEntry(entry) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DateChip(date: String, isSelected: Boolean, onClick: () -> Unit) {
    val localDate = runCatching { LocalDate.parse(date) }.getOrNull()
    val today = LocalDate.now()
    val label = when (localDate) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> localDate?.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())) ?: date
    }

    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.semantics {
            contentDescription = "View food log for $label${if (isSelected) ", currently selected" else ""}"
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
private fun MacroStat(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

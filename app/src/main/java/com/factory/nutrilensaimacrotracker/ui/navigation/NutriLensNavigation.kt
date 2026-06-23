package com.factory.nutrilensaimacrotracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Scan : Screen("scan")
    data object FoodLog : Screen("food_log")
    data object AddFood : Screen("add_food")
    data object Goals : Screen("goals")
    data object Paywall : Screen("paywall")
}

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val screen: Screen
)

val bottomNavItems = listOf(
    BottomNavItem("Home", Icons.Filled.Home, Screen.Home),
    BottomNavItem("Scan", Icons.Outlined.CameraAlt, Screen.Scan),
    BottomNavItem("Log", Icons.AutoMirrored.Filled.List, Screen.FoodLog),
    BottomNavItem("Goals", Icons.Filled.Settings, Screen.Goals)
)

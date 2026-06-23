package com.factory.nutrilensaimacrotracker

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.factory.nutrilensaimacrotracker.ui.navigation.Screen
import com.factory.nutrilensaimacrotracker.ui.navigation.bottomNavItems
import com.factory.nutrilensaimacrotracker.ui.screens.*
import com.factory.nutrilensaimacrotracker.ui.theme.NutriLensTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NutriLensTheme {
                NutriLensAppContent()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check purchases each time the activity comes to foreground
        // (handles pending purchases that completed while app was backgrounded)
        val app = application as NutriLensApp
        app.billingManager.restorePurchases()
    }

    override fun onDestroy() {
        super.onDestroy()
        val app = application as NutriLensApp
        app.billingManager.disconnect()
    }
}

@Composable
fun NutriLensAppContent() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // Observe premium state app-wide
    val app = (androidx.compose.ui.platform.LocalContext.current.applicationContext as NutriLensApp)
    val isPremium by app.premiumManager.isPremium.collectAsState(false)

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Scan.route,
        Screen.FoodLog.route,
        Screen.Goals.route
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.screen.route,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues),
            enterTransition = {
                fadeIn(animationSpec = tween(200)) + slideInHorizontally(
                    initialOffsetX = { it / 4 },
                    animationSpec = tween(200)
                )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(200)) + slideOutHorizontally(
                    targetOffsetX = { -it / 4 },
                    animationSpec = tween(200)
                )
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(200)) + slideInHorizontally(
                    initialOffsetX = { -it / 4 },
                    animationSpec = tween(200)
                )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(200)) + slideOutHorizontally(
                    targetOffsetX = { it / 4 },
                    animationSpec = tween(200)
                )
            }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    isPremium = isPremium,
                    onNavigateToScan = {
                        if (isPremium) {
                            navController.navigate(Screen.Scan.route)
                        } else {
                            navController.navigate(Screen.Paywall.route)
                        }
                    },
                    onNavigateToAddFood = {
                        navController.navigate(Screen.AddFood.route)
                    },
                    onNavigateToPaywall = {
                        navController.navigate(Screen.Paywall.route)
                    }
                )
            }

            composable(Screen.Scan.route) {
                if (!isPremium) {
                    // Redirect non-premium users who somehow reach this route
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.Paywall.route) {
                            popUpTo(Screen.Scan.route) { inclusive = true }
                        }
                    }
                } else {
                    ScanScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onNavigateToAddFood = { name, cal, prot, carbs, fat, serving ->
                            val encodedName = Uri.encode(name)
                            val encodedServing = Uri.encode(serving)
                            navController.navigate(
                                "${Screen.AddFood.route}?name=$encodedName&cal=$cal&prot=$prot&carbs=$carbs&fat=$fat&serving=$encodedServing"
                            )
                        }
                    )
                }
            }

            composable(Screen.FoodLog.route) {
                FoodLogScreen(
                    isPremium = isPremium,
                    onNavigateToPaywall = { navController.navigate(Screen.Paywall.route) }
                )
            }

            composable(
                route = "${Screen.AddFood.route}?name={name}&cal={cal}&prot={prot}&carbs={carbs}&fat={fat}&serving={serving}",
                arguments = listOf(
                    navArgument("name") { type = NavType.StringType; defaultValue = "" },
                    navArgument("cal") { type = NavType.StringType; defaultValue = "" },
                    navArgument("prot") { type = NavType.StringType; defaultValue = "" },
                    navArgument("carbs") { type = NavType.StringType; defaultValue = "" },
                    navArgument("fat") { type = NavType.StringType; defaultValue = "" },
                    navArgument("serving") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                val name = backStackEntry.arguments?.getString("name") ?: ""
                val cal = backStackEntry.arguments?.getString("cal") ?: ""
                val prot = backStackEntry.arguments?.getString("prot") ?: ""
                val carbs = backStackEntry.arguments?.getString("carbs") ?: ""
                val fat = backStackEntry.arguments?.getString("fat") ?: ""
                val serving = backStackEntry.arguments?.getString("serving") ?: ""
                AddFoodScreen(
                    onNavigateBack = { navController.popBackStack() },
                    prefillName = name,
                    prefillCalories = cal,
                    prefillProtein = prot,
                    prefillCarbs = carbs,
                    prefillFat = fat,
                    prefillServing = serving
                )
            }

            composable(Screen.Goals.route) {
                GoalsScreen(
                    isPremium = isPremium,
                    onNavigateToPaywall = { navController.navigate(Screen.Paywall.route) }
                )
            }

            composable(Screen.Paywall.route) {
                PaywallScreen(
                    onClose = { navController.popBackStack() }
                )
            }
        }
    }
}

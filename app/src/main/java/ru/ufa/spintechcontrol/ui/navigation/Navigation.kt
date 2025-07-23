package ru.ufa.spintechcontrol.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ru.ufa.spintechcontrol.ui.screens.InfoScreen
import ru.ufa.spintechcontrol.ui.screens.MainScreen

// Навигационные экраны
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Main : Screen("main", "Управление", Icons.Default.Dashboard)
    object Info : Screen("info", "О проекте", Icons.Default.Info)
}

@Composable
fun SpinTechNavigation(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Screen.Main.route
    ) {
        composable(Screen.Main.route) {
            MainScreen()
        }
        composable(Screen.Info.route) {
            InfoScreen()
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        Screen.Main,
        Screen.Info
    )
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    NavigationBar {
        items.forEach { screen ->
            NavigationBarItem(
                icon = { 
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.title
                    ) 
                },
                label = { Text(screen.title) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        // Избегаем множественных копий одного экрана в стеке
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
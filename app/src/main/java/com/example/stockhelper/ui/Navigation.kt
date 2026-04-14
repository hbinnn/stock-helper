package com.example.stockhelper.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.stockhelper.ui.screens.add.AddStockScreen
import com.example.stockhelper.ui.screens.detail.DetailScreen
import com.example.stockhelper.ui.screens.home.HomeScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object AddStock : Screen("add_stock")
    object Detail : Screen("detail/{stockCode}") {
        fun createRoute(stockCode: String) = "detail/$stockCode"
    }
}

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onAddStock = { navController.navigate(Screen.AddStock.route) },
                onStockClick = { code ->
                    navController.navigate(Screen.Detail.createRoute(code))
                }
            )
        }

        composable(Screen.AddStock.route) {
            AddStockScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Detail.route,
            arguments = listOf(navArgument("stockCode") { type = NavType.StringType })
        ) { backStackEntry ->
            val stockCode = backStackEntry.arguments?.getString("stockCode") ?: ""
            DetailScreen(
                stockCode = stockCode,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

package com.example.stockhelper

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.stockhelper.ui.screens.add.AddStockScreen
import com.example.stockhelper.ui.screens.detail.DetailScreen
import com.example.stockhelper.ui.screens.edit.EditStockScreen
import com.example.stockhelper.ui.screens.home.HomeScreen
import com.example.stockhelper.ui.theme.RichHelperTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // 权限处理
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermission()

        setContent {
            RichHelperTheme {
                MainScreen()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

sealed class BottomNavItem(val route: String, val title: String, val icon: @Composable () -> Unit) {
    object StockHelper : BottomNavItem(
        route = "stock_helper",
        title = "做T助手",
        icon = { Icon(Icons.Default.Home, contentDescription = "做T助手") }
    )
    object More : BottomNavItem(
        route = "more",
        title = "更多",
        icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "更多") }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val items = listOf(BottomNavItem.StockHelper, BottomNavItem.More)

    // 判断是否显示底部导航（只在顶层页面显示）
    val showBottomBar = currentDestination?.route in listOf("stock_helper", "more")

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    items.forEach { item ->
                        NavigationBarItem(
                            icon = item.icon,
                            label = { Text(item.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
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
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.StockHelper.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.StockHelper.route) {
                StockHelperNavHost()
            }
            composable(BottomNavItem.More.route) {
                MoreScreen()
            }
        }
    }
}

@Composable
fun StockHelperNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                onAddStock = { navController.navigate("add_stock") },
                onStockClick = { code ->
                    navController.navigate("detail/$code")
                }
            )
        }

        composable("add_stock") {
            AddStockScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("detail/{stockCode}") { backStackEntry ->
            val stockCode = backStackEntry.arguments?.getString("stockCode") ?: ""
            DetailScreen(
                stockCode = stockCode,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { code ->
                    navController.navigate("edit_stock/$code")
                }
            )
        }

        composable("edit_stock/{stockCode}") { backStackEntry ->
            val stockCode = backStackEntry.arguments?.getString("stockCode") ?: ""
            EditStockScreen(
                stockCode = stockCode,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun MoreScreen() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "more_home"
    ) {
        composable("more_home") {
            MoreHomeScreen()
        }
    }
}

@Composable
fun MoreHomeScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "更多功能",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "功能开发中...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

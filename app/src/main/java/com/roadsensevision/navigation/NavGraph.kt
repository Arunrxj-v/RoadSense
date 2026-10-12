package com.roadsensevision.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.roadsensevision.monitoring.MonitoringViewModel
import com.roadsensevision.ui.AppViewModel
import com.roadsensevision.ui.components.NavTab
import com.roadsensevision.ui.components.RoadSenseBottomBar
import com.roadsensevision.ui.screens.DashboardScreen
import com.roadsensevision.ui.screens.DefectDetailScreen
import com.roadsensevision.ui.screens.HistoryScreen
import com.roadsensevision.ui.screens.LiveMonitoringScreen
import com.roadsensevision.ui.screens.MapScreen
import com.roadsensevision.ui.screens.SessionSummaryScreen
import com.roadsensevision.ui.screens.SettingsScreen

object Routes {
    const val HOME = "home"
    const val MONITOR = "monitor"
    const val MAP = "map"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val SESSION_SUMMARY = "session_summary"
    const val DEFECT = "defect"
    fun defect(id: String) = "$DEFECT/$id"
}

@Composable
fun RoadSenseNavGraph(
    appViewModel: AppViewModel,
    monitoringViewModel: MonitoringViewModel
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route
            val showBar = currentRoute in setOf(Routes.HOME, Routes.MAP, Routes.HISTORY)
            if (showBar) {
                RoadSenseBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { tab ->
                        navController.navigate(tab.route) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Routes.HOME) {
                DashboardScreen(
                    appViewModel = appViewModel,
                    monitoringViewModel = monitoringViewModel,
                    onStartMonitoring = { navController.navigate(Routes.MONITOR) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenHistory = { navController.navigate(Routes.HISTORY) },
                    onOpenDefect = { id -> navController.navigate(Routes.defect(id)) },
                    onOpenSummary = { navController.navigate(Routes.SESSION_SUMMARY) }
                )
            }
            composable(Routes.MONITOR) {
                LiveMonitoringScreen(
                    monitoringViewModel = monitoringViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.MAP) {
                MapScreen(
                    appViewModel = appViewModel,
                    onOpenDefect = { id -> navController.navigate(Routes.defect(id)) }
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    appViewModel = appViewModel,
                    onOpenDefect = { id -> navController.navigate(Routes.defect(id)) }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    appViewModel = appViewModel,
                    monitoringViewModel = monitoringViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.SESSION_SUMMARY) {
                SessionSummaryScreen(
                    appViewModel = appViewModel,
                    onOpenMap = {
                        navController.navigate(Routes.MAP) {
                            popUpTo(Routes.HOME)
                            launchSingleTop = true
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("${Routes.DEFECT}/{defectId}") { backStackEntry ->
                val defectId = backStackEntry.arguments?.getString("defectId") ?: return@composable
                DefectDetailScreen(
                    appViewModel = appViewModel,
                    defectId = defectId,
                    onBack = { navController.popBackStack() },
                    onOpenMap = {
                        navController.navigate(Routes.MAP) {
                            popUpTo(Routes.HOME)
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}

package com.photo.plan.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.photo.plan.ui.create.CreatePlanScreen
import com.photo.plan.ui.detail.DetailScreen
import com.photo.plan.ui.home.HomeScreen
import com.photo.plan.ui.viewer.ImageViewerScreen

object Routes {
    const val HOME = "home"
    const val CREATE_PLAN = "create_plan"
    const val EDIT_PLAN = "edit_plan/{planId}"
    const val DETAIL = "detail/{planId}"
    const val VIEWER = "viewer/{planId}/{sampleIndex}"

    fun editPlan(planId: Long) = "edit_plan/$planId"
    fun detail(planId: Long) = "detail/$planId"
    fun viewer(planId: Long, sampleIndex: Int) = "viewer/$planId/$sampleIndex"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToCreate = { navController.navigate(Routes.CREATE_PLAN) },
                onNavigateToDetail = { planId -> navController.navigate(Routes.detail(planId)) },
                onNavigateToEdit = { planId -> navController.navigate(Routes.editPlan(planId)) }
            )
        }

        composable(Routes.CREATE_PLAN) {
            CreatePlanScreen(
                planId = null,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { planId ->
                    navController.navigate(Routes.detail(planId)) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = Routes.EDIT_PLAN,
            arguments = listOf(navArgument("planId") { type = NavType.LongType })
        ) { backStackEntry ->
            val planId = backStackEntry.arguments?.getLong("planId") ?: return@composable
            CreatePlanScreen(
                planId = planId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { _ ->
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("planId") { type = NavType.LongType })
        ) { backStackEntry ->
            val planId = backStackEntry.arguments?.getLong("planId") ?: return@composable
            DetailScreen(
                planId = planId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToViewer = { pid, idx -> navController.navigate(Routes.viewer(pid, idx)) },
                onNavigateToEdit = { pid -> navController.navigate(Routes.editPlan(pid)) }
            )
        }

        composable(
            route = Routes.VIEWER,
            arguments = listOf(
                navArgument("planId") { type = NavType.LongType },
                navArgument("sampleIndex") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val planId = backStackEntry.arguments?.getLong("planId") ?: return@composable
            val sampleIndex = backStackEntry.arguments?.getInt("sampleIndex") ?: return@composable
            ImageViewerScreen(
                planId = planId,
                initialIndex = sampleIndex,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

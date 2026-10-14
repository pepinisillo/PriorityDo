package com.prioritydo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.prioritydo.ui.screens.TaskEditScreen
import com.prioritydo.ui.screens.TaskListScreen
import com.prioritydo.viewmodel.TaskViewModel

sealed class Screen(val route: String) {
    object TaskList : Screen("task_list")
    object TaskCreate : Screen("task_create")
    object TaskEdit : Screen("task_edit/{taskId}") {
        fun createRoute(taskId: Long) = "task_edit/$taskId"
    }
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    viewModel: TaskViewModel = hiltViewModel(),
    startDestination: String = Screen.TaskList.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.TaskList.route) {
            TaskListScreen(
                viewModel = viewModel,
                onNavigateToCreate = {
                    navController.navigate(Screen.TaskCreate.route)
                },
                onNavigateToEdit = { taskId ->
                    navController.navigate(Screen.TaskEdit.createRoute(taskId))
                }
            )
        }

        composable(Screen.TaskCreate.route) {
            TaskEditScreen(
                taskId = null,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.TaskEdit.route,
            arguments = listOf(
                navArgument("taskId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getLong("taskId")
            TaskEditScreen(
                taskId = taskId,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

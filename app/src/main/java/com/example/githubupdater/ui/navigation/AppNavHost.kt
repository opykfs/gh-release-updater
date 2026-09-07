package com.example.githubupdater.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.githubupdater.di.AppContainer
import com.example.githubupdater.ui.addtracking.AddTrackingScreen
import com.example.githubupdater.ui.addtracking.AddTrackingViewModel
import com.example.githubupdater.ui.trackinglist.TrackingListScreen
import com.example.githubupdater.ui.trackinglist.TrackingListViewModel

/** 导航路由常量。 */
object Routes {
    const val TRACKING_LIST = "tracking_list"
    const val ADD_TRACKING = "add_tracking"
}

/**
 * App 导航宿主：仅两个路由（追踪列表 / 添加追踪）。
 */
@Composable
fun AppNavHost(
    container: AppContainer,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Routes.TRACKING_LIST,
    ) {
        composable(Routes.TRACKING_LIST) {
            val viewModel: TrackingListViewModel =
                viewModel(factory = TrackingListViewModel.factory(container))
            TrackingListScreen(
                onNavigateToAdd = { navController.navigate(Routes.ADD_TRACKING) },
                viewModel = viewModel,
            )
        }

        composable(Routes.ADD_TRACKING) {
            val viewModel: AddTrackingViewModel =
                viewModel(factory = AddTrackingViewModel.factory(container))
            AddTrackingScreen(
                onBack = { navController.popBackStack() },
                viewModel = viewModel,
            )
        }
    }
}

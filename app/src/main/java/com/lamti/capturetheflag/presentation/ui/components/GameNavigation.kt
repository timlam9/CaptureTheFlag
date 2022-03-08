package com.lamti.capturetheflag.presentation.ui.components

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lamti.capturetheflag.presentation.ui.components.map.MapScreen
import com.lamti.capturetheflag.presentation.ui.fragments.maps.MapViewModel
import com.lamti.capturetheflag.utils.EMPTY

@Composable
fun GameNavigation(
    viewModel: MapViewModel,
    enteredGeofenceId: String,
    onSettingFlagsButtonClicked: () -> Unit,
    onArScannerButtonClicked: () -> Unit,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = viewModel.initialScreen.value.route,
    ) {
        composable(route = Screen.Menu.route) {
            MenuScreen(
                onNewGameClicked = { navController.navigate(Screen.CreateGame.route) },
                onAvailableGamesClicked = {}
            )
        }
        composable(route = Screen.CreateGame.route) {
            CreateGameScreen(
                viewModel = viewModel
            ) {
                navController.popNavigate(Screen.Map.route)
            }
        }
        composable(route = Screen.Map.route) {
            MapScreen(
                viewModel = viewModel,
                enteredGeofenceId = enteredGeofenceId,
                onSettingFlagsButtonClicked = onSettingFlagsButtonClicked,
                onArScannerButtonClicked = onArScannerButtonClicked
            )
        }
    }
}

fun NavHostController.popNavigate(to: String) {
    navigate(to) {
        popUpTo(0) {
            inclusive = true
        }
    }
}

sealed class Screen(open val route: String = EMPTY) {

    object Map : Screen("map")

    object Menu : Screen("menu")

    object CreateGame : Screen("create_game")

}

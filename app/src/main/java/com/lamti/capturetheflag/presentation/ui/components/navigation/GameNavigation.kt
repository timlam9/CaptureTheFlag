package com.lamti.capturetheflag.presentation.ui.components.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.presentation.ui.components.screens.ConnectingToGameScreen
import com.lamti.capturetheflag.presentation.ui.components.screens.CreateGameScreen
import com.lamti.capturetheflag.presentation.ui.components.screens.JoinGameScreen
import com.lamti.capturetheflag.presentation.ui.components.screens.MapScreen
import com.lamti.capturetheflag.presentation.ui.components.screens.MenuScreen
import com.lamti.capturetheflag.presentation.ui.fragments.maps.MapViewModel
import com.lamti.capturetheflag.presentation.ui.popNavigate
import kotlinx.coroutines.launch

@Composable
fun GameNavigation(
    viewModel: MapViewModel,
    enteredGeofenceId: String,
    onSettingFlagsButtonClicked: () -> Unit,
    onArScannerButtonClicked: () -> Unit
) {
    val navController = rememberNavController()
    val coroutine = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = viewModel.initialScreen.value.route,
    ) {
        composable(route = Screen.Menu.route) {
            MenuScreen(
                onNewGameClicked = { navController.navigate(Screen.CreateGame.route) },
                onJoinGameClicked = { navController.navigate(Screen.JoinGame.route) }
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
                onArScannerButtonClicked = onArScannerButtonClicked,
                onQuitButtonClicked = {
                    navController.popNavigate(Screen.Menu.route)
                }
            )
        }
        composable(route = Screen.JoinGame.route) {
            JoinGameScreen { qrCode ->
                coroutine.launch {
                    val game = viewModel.getGame(qrCode)
                    if (game != null) {
                        viewModel.onJoinButtonClicked(game.gameID)
                        navController.navigate(Screen.ConnectingToGame.route)
                    }
                }
            }
        }
        composable(route = Screen.ConnectingToGame.route) {
            ConnectingToGameScreen(
                hasChosenTeam = viewModel.player.value.gameDetails?.team != null && viewModel.player.value.gameDetails?.team != Team.Unknown,
                onRedButtonClicked = {
                    viewModel.onTeamButtonClicked(Team.Red)
                }
            ) {
                viewModel.onTeamButtonClicked(Team.Green)
            }
        }
    }
}
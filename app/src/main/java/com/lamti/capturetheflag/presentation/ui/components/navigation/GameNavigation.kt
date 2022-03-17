package com.lamti.capturetheflag.presentation.ui.components.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.asImageBitmap
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lamti.capturetheflag.domain.player.GameDetails
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.presentation.ui.components.screens.ConnectingToGameScreen
import com.lamti.capturetheflag.presentation.ui.components.screens.CreateGameScreen
import com.lamti.capturetheflag.presentation.ui.components.screens.JoinGameScreen
import com.lamti.capturetheflag.presentation.ui.components.screens.MapScreen
import com.lamti.capturetheflag.presentation.ui.components.screens.MenuScreen
import com.lamti.capturetheflag.presentation.ui.fragments.maps.MapViewModel
import com.lamti.capturetheflag.presentation.ui.popNavigate
import com.lamti.capturetheflag.utils.EMPTY
import kotlinx.coroutines.launch

@Composable
fun GameNavigation(
    viewModel: MapViewModel,
    enteredGeofenceId: String,
    onSettingFlagsButtonClicked: () -> Unit,
    onArScannerButtonClicked: () -> Unit,
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
                gameID = viewModel.player.value.gameDetails?.gameID ?: EMPTY,
                qrCodeImage = viewModel.qrCodeBitmap.value?.asImageBitmap(),
                gameState = viewModel.game.value.gameState.state,
                redPlayers = viewModel.game.value.redPlayers.size,
                greenPlayers = viewModel.game.value.greenPlayers.size,
                onSetGameClicked = {
                    viewModel.onSetGameClicked()
                    navController.popNavigate(Screen.Map.route)
                },
                onCreateGameClicked = { viewModel.onCreateGameClicked(it) }
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
        composable(route = Screen.Map.route) {
            MapScreen(
                userID = viewModel.player.value.userID,
                gameDetails = viewModel.player.value.gameDetails ?: GameDetails.initialGameDetails(),
                gameState = viewModel.game.value.gameState,
                canPlaceFlag = viewModel.canPlaceFlag.value,
                enteredGeofenceId = enteredGeofenceId,
                currentPosition = viewModel.currentPosition.value,
                isSafehouseDraggable = viewModel.isSafehouseDraggable.value,
                otherPlayers = viewModel.otherPlayers.value,
                onSafehouseMarkerClicked = { viewModel.updateSafeHousePosition(it) },
                onArScannerButtonClicked = onArScannerButtonClicked,
                onSettingFlagsButtonClicked = onSettingFlagsButtonClicked,
                onSetFlagsClicked = { viewModel.onSetFlagsClicked() },
            ) {
                viewModel.onQuitButtonClicked {
                    if (it) navController.popNavigate(Screen.Menu.route)
                }
            }
        }
    }
}

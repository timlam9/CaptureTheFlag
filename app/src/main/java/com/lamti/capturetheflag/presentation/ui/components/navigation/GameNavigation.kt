package com.lamti.capturetheflag.presentation.ui.components.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.asImageBitmap
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lamti.capturetheflag.domain.player.GameDetails
import com.lamti.capturetheflag.domain.player.Player
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.presentation.ui.components.screens.BattleScreen
import com.lamti.capturetheflag.presentation.ui.components.screens.ConnectingToGameScreen
import com.lamti.capturetheflag.presentation.ui.components.screens.CreateGameScreen
import com.lamti.capturetheflag.presentation.ui.components.screens.GameOverScreen
import com.lamti.capturetheflag.presentation.ui.components.screens.JoinGameScreen
import com.lamti.capturetheflag.presentation.ui.components.screens.MapScreen
import com.lamti.capturetheflag.presentation.ui.components.screens.MenuScreen
import com.lamti.capturetheflag.presentation.ui.components.screens.StartingGameScreen
import com.lamti.capturetheflag.presentation.ui.fragments.maps.MapViewModel
import com.lamti.capturetheflag.presentation.ui.popNavigate
import com.lamti.capturetheflag.utils.EMPTY
import kotlinx.coroutines.launch

@Composable
fun GameNavigation(
    viewModel: MapViewModel,
    enteredGeofenceId: String,
    onLogoutClicked: () -> Unit,
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
                onLogoutClicked = {
                    viewModel.logout()
                    onLogoutClicked()
                },
                onNewGameClicked = { navController.navigate(Screen.CreateGame.route) },
                onJoinGameClicked = { navController.navigate(Screen.JoinGame.route) }
            )
        }
        composable(route = Screen.CreateGame.route) {
            CreateGameScreen {
                viewModel.onCreateGameClicked(it)
                navController.navigate(Screen.StartingGame.route)
            }
        }
        composable(route = Screen.StartingGame.route) {
            StartingGameScreen(
                gameID = viewModel.player.value.gameDetails?.gameID ?: EMPTY,
                qrCodeImage = viewModel.qrCodeBitmap.value?.asImageBitmap(),
                gameTitle = viewModel.game.value.title,
                redPlayers = viewModel.game.value.redPlayers.size,
                greenPlayers = viewModel.game.value.greenPlayers.size,
                onStartGameClicked = {
                    viewModel.onSetGameClicked()
                    navController.popNavigate(Screen.Map.route)
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
                onRedButtonClicked = { viewModel.onTeamButtonClicked(Team.Red) },
                onGreenButtonClicked = { viewModel.onTeamButtonClicked(Team.Green) },
                onOkButtonClicked = { viewModel.onTeamOkButtonClicked() }
            )
        }
        composable(route = Screen.Map.route) {
            MapScreen(
                userID = viewModel.player.value.userID,
                gameDetails = viewModel.player.value.gameDetails ?: GameDetails.initialGameDetails(),
                gameState = viewModel.game.value.gameState,
                canPlaceFlag = viewModel.canPlaceFlag.value,
                enteredGeofenceId = enteredGeofenceId,
                initialPosition = viewModel.initialPosition.value,
                livePosition = viewModel.livePosition.value,
                isSafehouseDraggable = viewModel.isSafehouseDraggable.value,
                otherPlayers = viewModel.otherPlayers.value,
                battleID = viewModel.battleID.value,
                lost = viewModel.player.value.status == Player.Status.Lost,
                redPlayersCount = viewModel.game.value.redPlayers.size,
                greenPlayersCount = viewModel.game.value.greenPlayers.size,
                enterBattleScreen = viewModel.enterBattleScreen.value,
                enterGameOverScreen = viewModel.enterGameOverScreen.value,
                onEnterBattleScreen = { navController.popNavigate(Screen.Battle.route) },
                onEnterGameOverScreen = { navController.popNavigate(Screen.GameOver.route) },
                onSafehouseMarkerClicked = { viewModel.updateSafeHousePosition(it) },
                onArScannerButtonClicked = onArScannerButtonClicked,
                onSettingFlagsButtonClicked = onSettingFlagsButtonClicked,
                onReadyButtonClicked = { viewModel.onSetFlagsClicked() },
                onBattleButtonClicked = { viewModel.onBattleButtonClicked() }
            )
        }
        composable(route = Screen.Battle.route) {
            BattleScreen(
                team = viewModel.player.value.gameDetails?.team ?: Team.Unknown,
                enterBattleScreen = viewModel.enterBattleScreen.value,
                onEnterBattleScreen = { navController.popNavigate(Screen.Map.route) },
                onLostButtonClicked = { viewModel.onLostBattleButtonClicked() }
            )
        }
        composable(route = Screen.GameOver.route) {
            GameOverScreen(
                winners = viewModel.game.value.gameState.winners,
                onOkButtonClicked = {
                    viewModel.onGameOverOkClicked {
                        if (it) navController.popNavigate(Screen.Menu.route)
                    }
                }
            )
        }
    }
}

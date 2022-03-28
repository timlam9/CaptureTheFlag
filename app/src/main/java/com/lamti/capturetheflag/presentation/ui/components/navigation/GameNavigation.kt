package com.lamti.capturetheflag.presentation.ui.components.navigation

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.domain.player.GameDetails
import com.lamti.capturetheflag.domain.player.Player
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.presentation.ui.components.composables.DefaultButton
import com.lamti.capturetheflag.presentation.ui.components.screens.ConnectingToGameScreen
import com.lamti.capturetheflag.presentation.ui.components.screens.CreateGameScreen
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
                currentPosition = viewModel.initialPosition.value,
                isSafehouseDraggable = viewModel.isSafehouseDraggable.value,
                otherPlayers = viewModel.otherPlayers.value,
                battleID = viewModel.battleID.value,
                lost = viewModel.player.value.status == Player.Status.Lost,
                enterBattleScreen = viewModel.enterBattleScreen.value,
                onEnterBattleScreen = { navController.popNavigate(Screen.Battle.route) },
                onSafehouseMarkerClicked = { viewModel.updateSafeHousePosition(it) },
                onArScannerButtonClicked = onArScannerButtonClicked,
                onSettingFlagsButtonClicked = onSettingFlagsButtonClicked,
                onSetFlagsClicked = { viewModel.onSetFlagsClicked() },
                onBattleButtonClicked = { viewModel.onBattleButtonClicked() }
            ) {
                viewModel.onQuitButtonClicked {
                    if (it) navController.popNavigate(Screen.Menu.route)
                }
            }
        }
        composable(route = Screen.Battle.route) {
            BattleScreen(
                enterBattleScreen = viewModel.enterBattleScreen.value,
                onEnterBattleScreen = { navController.popNavigate(Screen.Map.route) },
                onLostButtonClicked = { viewModel.onLostBattleButtonClicked() }
            )
        }
    }
}

@Composable
fun BattleScreen(
    enterBattleScreen: Boolean,
    onEnterBattleScreen: () -> Unit,
    onLostButtonClicked: () -> Unit
) {
    if (!enterBattleScreen) {
        LaunchedEffect(key1 = enterBattleScreen) {
            Log.d("TAGARA", "Enter Battle Battle")
            onEnterBattleScreen()
        }
    }
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            modifier = Modifier.padding(32.dp),
            text = "Battle"
        )
        DefaultButton(
            modifier = Modifier
                .padding(20.dp)
                .wrapContentHeight(align = Alignment.Bottom)
                .fillMaxWidth(),
            text = stringResource(R.string.i_lost),
            color = MaterialTheme.colors.secondary
        ) {
            onLostButtonClicked()
        }
    }
}

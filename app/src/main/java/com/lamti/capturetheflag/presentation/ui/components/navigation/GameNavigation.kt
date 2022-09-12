package com.lamti.capturetheflag.presentation.ui.components.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.domain.game.BattleMiniGame
import com.lamti.capturetheflag.domain.game.BattleState
import com.lamti.capturetheflag.domain.player.GameDetails
import com.lamti.capturetheflag.domain.player.Player
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.presentation.ui.DatastoreHelper
import com.lamti.capturetheflag.presentation.ui.components.screens.BattleGameScreen
import com.lamti.capturetheflag.presentation.ui.components.screens.BattleWonScreen
import com.lamti.capturetheflag.presentation.ui.components.screens.ChooseTeamScreen
import com.lamti.capturetheflag.presentation.ui.components.screens.CreateGameScreen
import com.lamti.capturetheflag.presentation.ui.components.screens.GameOverScreen
import com.lamti.capturetheflag.presentation.ui.components.screens.JoinGameScreen
import com.lamti.capturetheflag.presentation.ui.components.screens.MapScreen
import com.lamti.capturetheflag.presentation.ui.components.screens.MenuScreen
import com.lamti.capturetheflag.presentation.ui.components.screens.StartingGameScreen
import com.lamti.capturetheflag.presentation.ui.fragments.maps.MapViewModel
import com.lamti.capturetheflag.presentation.ui.playSound
import com.lamti.capturetheflag.presentation.ui.popNavigate
import com.lamti.capturetheflag.presentation.ui.style.Green
import com.lamti.capturetheflag.presentation.ui.style.Red
import com.lamti.capturetheflag.utils.EMPTY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val battleFoundSound: Uri = Uri.parse("android.resource://com.lamti.capturetheflag/" + R.raw.battle_found)

@Composable
fun GameNavigation(
    viewModel: MapViewModel,
    navController: NavHostController,
    coroutineScope: CoroutineScope,
    dataStore: DatastoreHelper,
    onLogoutClicked: () -> Unit,
    onSettingFlagsButtonClicked: () -> Unit,
    onArScannerButtonClicked: () -> Unit,
    onSettingsClicked: () -> Unit
) {
    val qrCodeImage by viewModel.qrCodeBitmap.collectAsState()
    val initialScreen by viewModel.initialScreen.collectAsState()
    val player by viewModel.player.collectAsState()
    val canPlaceFlag by viewModel.canPlaceFlag.collectAsState()
    val initialPosition by viewModel.initialPosition.collectAsState()
    val livePosition by viewModel.livePosition.collectAsState()
    val isSafehouseDraggable by viewModel.isSafehouseDraggable.collectAsState()
    val otherPlayers by viewModel.otherPlayers.collectAsState()
    val showBattleButton by viewModel.showBattleButton.collectAsState()
    val showArFlagButton by viewModel.showArFlagButton.collectAsState()
    val enterBattleScreen by viewModel.enterBattleScreen.collectAsState()
    val battleWinner by viewModel.battleWinner.collectAsState()
    val battleState by viewModel.battleState.collectAsState()
    val isPlayerReadyToBattle by viewModel.isPlayerReadyToBattle.collectAsState()
    val enterGameOverScreen by viewModel.enterGameOverScreen.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(key1 = true) { dataStore.saveHasGameFound(false) }
    LaunchedEffect(key1 = showBattleButton) {
        if (showBattleButton.isNotEmpty()) {
            context.playSound(battleFoundSound)
        }
    }
    val hasGameFound by dataStore.hasGameFound.collectAsState(initial = false)

    NavHost(
        navController = navController,
        startDestination = initialScreen.route,
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
            CreateGameScreen { title, miniGame ->
                viewModel.onCreateGameClicked(title, miniGame)
                navController.navigate(Screen.StartingGame.route)
            }
        }
        composable(route = Screen.StartingGame.route) {
            StartingGameScreen(
                game = viewModel.game.value,
                qrCodeImage = qrCodeImage?.asImageBitmap(),
                onStartGameClicked = {
                    viewModel.onStartGameClicked()
                    navController.popNavigate(Screen.Map.route)
                }
            )
        }
        composable(route = Screen.JoinGame.route) {
            JoinGameScreen { qrCode ->
                coroutineScope.launch {
                    val gameToJoin = viewModel.getGame(qrCode)
                    if (gameToJoin != null && !hasGameFound) {
                        dataStore.saveHasGameFound(true)
                        viewModel.onGameCodeScanned(qrCode)
                        navController.navigate(Screen.ChooseTeam.route)
                    }
                }
            }
        }
        composable(route = Screen.ChooseTeam.route) {
            ChooseTeamScreen(
                dataStore = dataStore,
                onRedButtonClicked = { viewModel.onTeamButtonClicked(Team.Red) },
                onGreenButtonClicked = { viewModel.onTeamButtonClicked(Team.Green) },
                onOkButtonClicked = { viewModel.onTeamOkButtonClicked() }
            )
        }
        composable(route = Screen.Map.route) {
            MapScreen(
                userID = player.userID,
                gameDetails = player.gameDetails ?: GameDetails.initialGameDetails(),
                gameState = viewModel.game.value.gameState,
                flagRadius = viewModel.game.value.flagRadius,
                gameRadius = viewModel.game.value.gameRadius,
                canPlaceFlag = canPlaceFlag,
                safehousePosition = viewModel.game.value.gameState.safehouse.position,
                initialPosition = initialPosition,
                livePosition = livePosition,
                isSafehouseDraggable = isSafehouseDraggable,
                otherPlayers = otherPlayers,
                showBattleButton = showBattleButton,
                showArFlagButton = showArFlagButton,
                lost = player.status == Player.Status.Lost,
                redPlayersCount = viewModel.game.value.redPlayers.filterNot { it.hasLost }.size,
                greenPlayersCount = viewModel.game.value.greenPlayers.filterNot { it.hasLost }.size,
                enterBattleScreen = enterBattleScreen,
                enterGameOverScreen = enterGameOverScreen,
                onEnterBattleScreen = {
                    when (viewModel.game.value.battleMiniGame) {
                        BattleMiniGame.None -> navController.popNavigate(Screen.BattleWon.route)
                        BattleMiniGame.TapTheFlag -> navController.popNavigate(Screen.Battle.route)
                    }
                },
                onEnterGameOverScreen = { navController.popNavigate(Screen.GameOver.route) },
                onArScannerButtonClicked = onArScannerButtonClicked,
                onSettingFlagsButtonClicked = onSettingFlagsButtonClicked,
                onReadyButtonClicked = { safehousePosition, gameRadius, flagRadius ->
                    viewModel.onReadyButtonClicked(safehousePosition, gameRadius, flagRadius)
                },
                onBattleButtonClicked = { viewModel.onBattleButtonClicked() },
                onSettingsClicked = onSettingsClicked
            )
        }
        composable(route = Screen.Battle.route) {
            BattleGameScreen(
                color = if (player.gameDetails?.team == Team.Green) Green else Red,
                winner = battleWinner,
                isPlayerReady = isPlayerReadyToBattle,
                battleStarted = battleState == BattleState.Started,
                onReadyClicked = { viewModel.readyToBattle() },
                onWinnerFound = {
                    if (battleWinner == EMPTY) viewModel.onBattleWinnerFound()
                    navController.navigate(Screen.BattleWon.route)
                }
            )
        }
        composable(route = Screen.BattleWon.route) {
            BattleWonScreen(
                team = player.gameDetails?.team ?: Team.Unknown,
                winner = battleWinner,
                playerName = player.details.username,
                enterBattleScreen = enterBattleScreen,
                onEnterBattleScreen = { navController.popNavigate(Screen.Map.route) }
            ) { viewModel.onLostBattleButtonClicked() }
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

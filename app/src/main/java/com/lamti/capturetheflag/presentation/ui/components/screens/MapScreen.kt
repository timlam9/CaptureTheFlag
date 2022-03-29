package com.lamti.capturetheflag.presentation.ui.components.screens

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.LatLng
import com.lamti.capturetheflag.domain.game.GamePlayer
import com.lamti.capturetheflag.domain.game.GameState
import com.lamti.capturetheflag.domain.player.GameDetails
import com.lamti.capturetheflag.presentation.ui.components.composables.InstructionsCard
import com.lamti.capturetheflag.presentation.ui.components.composables.map.ActionButtons
import com.lamti.capturetheflag.presentation.ui.components.composables.map.GoogleMapsView
import com.lamti.capturetheflag.presentation.ui.components.composables.map.QuitButton
import com.lamti.capturetheflag.presentation.ui.components.composables.map.ReadyButton
import com.lamti.capturetheflag.presentation.ui.components.composables.map.SettingFlags

@Composable
fun MapScreen(
    userID: String,
    gameDetails: GameDetails,
    gameState: GameState,
    canPlaceFlag: Boolean,
    enteredGeofenceId: String,
    currentPosition: LatLng,
    isSafehouseDraggable: Boolean,
    otherPlayers: List<GamePlayer>,
    battleID: String,
    lost: Boolean,
    enterBattleScreen: Boolean,
    onEnterBattleScreen: () -> Unit,
    onSafehouseMarkerClicked: (LatLng) -> Unit,
    onArScannerButtonClicked: () -> Unit,
    onSettingFlagsButtonClicked: () -> Unit,
    onSetFlagsClicked: () -> Unit,
    onBattleButtonClicked: () -> Unit,
    onQuitButtonClicked: () -> Unit,
) {
    RedirectToBattleScreenIfNeeded(enterBattleScreen, onEnterBattleScreen)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        GoogleMapsView(
            currentPosition = currentPosition,
            safehousePosition = gameState.safehouse.position,
            isSafeHouseDraggable = isSafehouseDraggable,
            team = gameDetails.team,
            userID = userID,
            redFlag = gameState.redFlag,
            greenFlag = gameState.greenFlag,
            redFlagPlayer = gameState.redFlagCaptured,
            greenFlagPlayer = gameState.greenFlagCaptured,
            otherPlayers = otherPlayers,
            onSafehouseMarkerClicked = onSafehouseMarkerClicked
        )
        SettingFlags(
            modifier = Modifier.align(Alignment.BottomCenter),
            gameState = gameState.state,
            playerGameDetails = gameDetails,
            redFlagIsPlaced = gameState.redFlag.isPlaced,
            greenFlagIsPlaced = gameState.greenFlag.isPlaced,
            canPlaceFlag = canPlaceFlag,
            onSettingFlagsButtonClicked = onSettingFlagsButtonClicked
        )
        QuitButton(
            modifier = Modifier.align(Alignment.BottomCenter),
            gameState = gameState.state,
            onQuitButtonClicked = onQuitButtonClicked
        )
        ReadyButton(
            modifier = Modifier.align(Alignment.BottomCenter),
            gameState = gameState.state,
            playerGameDetails = gameDetails,
            onReadyButtonClicked = onSetFlagsClicked
        )
        InstructionsCard(
            team = gameDetails.team,
            rank = gameDetails.rank,
            state = gameState.state,
            canPlaceFlag = canPlaceFlag,
            isRedFlagPlaced = gameState.redFlag.isPlaced,
            isGreenFlagPlaced = gameState.greenFlag.isPlaced,
            redFlagCaptured = gameState.redFlagCaptured,
            greenFlagCaptured = gameState.greenFlagCaptured,
        )
        ActionButtons(
            modifier = Modifier.align(Alignment.BottomCenter),
            battleModifier = Modifier.align(Alignment.Center),
            lost = lost,
            team = gameDetails.team,
            battleID = battleID,
            enteredGeofenceId = enteredGeofenceId,
            redFlagCaptured = gameState.redFlagCaptured,
            greenFlagCaptured = gameState.greenFlagCaptured,
            onArScannerButtonClicked = onArScannerButtonClicked,
            onBattleButtonClicked = onBattleButtonClicked
        )
    }
}

@Composable
private fun RedirectToBattleScreenIfNeeded(enterBattleScreen: Boolean, onEnterBattleScreen: () -> Unit) {
    if (enterBattleScreen) {
        LaunchedEffect(key1 = enterBattleScreen) {
            onEnterBattleScreen()
        }
    }
}

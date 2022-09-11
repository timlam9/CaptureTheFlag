package com.lamti.capturetheflag.presentation.ui.components.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.rememberCameraPositionState
import com.lamti.capturetheflag.domain.game.GamePlayer
import com.lamti.capturetheflag.domain.game.GameState
import com.lamti.capturetheflag.domain.player.GameDetails
import com.lamti.capturetheflag.presentation.ui.DEFAULT_ZOOM_LEVEL
import com.lamti.capturetheflag.presentation.ui.components.composables.common.InstructionsCard
import com.lamti.capturetheflag.presentation.ui.components.composables.map.ActionButtons
import com.lamti.capturetheflag.presentation.ui.components.composables.map.GoogleMapsView
import com.lamti.capturetheflag.presentation.ui.components.composables.map.InfoBar
import com.lamti.capturetheflag.presentation.ui.components.composables.map.SettingFlags
import kotlinx.coroutines.launch

@Composable
fun MapScreen(
    userID: String,
    gameDetails: GameDetails,
    gameState: GameState,
    flagRadius: Float,
    gameRadius: Float,
    canPlaceFlag: Boolean,
    safehousePosition: LatLng,
    initialPosition: LatLng,
    livePosition: LatLng,
    isSafehouseDraggable: Boolean,
    otherPlayers: List<GamePlayer>,
    showBattleButton: String,
    showArFlagButton: Boolean,
    lost: Boolean,
    greenPlayersCount: Int,
    redPlayersCount: Int,
    enterBattleScreen: Boolean,
    enterGameOverScreen: Boolean,
    onEnterBattleScreen: () -> Unit,
    onEnterGameOverScreen: () -> Unit,
    onArScannerButtonClicked: () -> Unit,
    onSettingFlagsButtonClicked: () -> Unit,
    onReadyButtonClicked: (LatLng, Float, Float) -> Unit,
    onBattleButtonClicked: () -> Unit,
    onSettingsClicked: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val zoom by remember { mutableStateOf(DEFAULT_ZOOM_LEVEL) }
    val cameraPositionState: CameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, zoom)
    }

    LaunchedEffect(initialPosition) {
        snapshotFlow { initialPosition }.collect {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(initialPosition, zoom)
        }
    }
    RedirectToBattleScreenIfNeeded(
        enterBattleScreen = enterBattleScreen,
        onEnterBattleScreen = onEnterBattleScreen
    )
    RedirectToGameOverScreenIfNeeded(
        enterGameOverScreen = enterGameOverScreen,
        onEnterGameOverScreen = onEnterGameOverScreen
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        GoogleMapsView(
            cameraPositionState = cameraPositionState,
            safehousePosition = safehousePosition,
            isSafeHouseDraggable = isSafehouseDraggable,
            team = gameDetails.team,
            flagRadius = flagRadius,
            gameRadius = gameRadius,
            userID = userID,
            redFlag = gameState.redFlag,
            gameState = gameState.state,
            gameDetails = gameDetails,
            greenFlag = gameState.greenFlag,
            redFlagPlayer = gameState.redFlagCaptured,
            greenFlagPlayer = gameState.greenFlagCaptured,
            otherPlayers = otherPlayers,
            onReadyButtonClicked = onReadyButtonClicked
        )
        TopBar(
            gameDetails = gameDetails,
            redPlayersCount = redPlayersCount,
            greenPlayersCount = greenPlayersCount,
            gameState = gameState,
            canPlaceFlag = canPlaceFlag,
            onSettingsClicked = onSettingsClicked,
            onGpsClicked = {
                coroutineScope.launch {
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(livePosition, DEFAULT_ZOOM_LEVEL))
                }
            }
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
        ActionButtons(
            modifier = Modifier.align(Alignment.Center),
            buttonModifier = Modifier.align(Alignment.BottomCenter),
            lost = lost,
            team = gameDetails.team,
            showBattleButton = showBattleButton,
            showArFlagButton = showArFlagButton,
            onArScannerButtonClicked = onArScannerButtonClicked,
            onBattleButtonClicked = onBattleButtonClicked
        )
    }
}

@Composable
private fun TopBar(
    gameDetails: GameDetails,
    redPlayersCount: Int,
    greenPlayersCount: Int,
    gameState: GameState,
    canPlaceFlag: Boolean,
    onSettingsClicked: () -> Unit,
    onGpsClicked: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        InfoBar(
            team = gameDetails.team,
            redPlayersCount = redPlayersCount,
            greenPlayersCount = greenPlayersCount,
            onSettingsClicked = onSettingsClicked,
            onGpsClicked = onGpsClicked
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

@Composable
private fun RedirectToGameOverScreenIfNeeded(enterGameOverScreen: Boolean, onEnterGameOverScreen: () -> Unit) {
    if (enterGameOverScreen) {
        LaunchedEffect(key1 = enterGameOverScreen) {
            onEnterGameOverScreen()
        }
    }
}

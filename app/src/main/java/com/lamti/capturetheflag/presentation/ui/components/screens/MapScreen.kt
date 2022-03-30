package com.lamti.capturetheflag.presentation.ui.components.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.rememberCameraPositionState
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.domain.game.GamePlayer
import com.lamti.capturetheflag.domain.game.GameState
import com.lamti.capturetheflag.domain.player.GameDetails
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.presentation.ui.components.composables.DefaultButton
import com.lamti.capturetheflag.presentation.ui.components.composables.IconButton
import com.lamti.capturetheflag.presentation.ui.components.composables.InstructionsCard
import com.lamti.capturetheflag.presentation.ui.components.composables.map.ActionButtons
import com.lamti.capturetheflag.presentation.ui.components.composables.map.GoogleMapsView
import com.lamti.capturetheflag.presentation.ui.components.composables.map.ReadyButton
import com.lamti.capturetheflag.presentation.ui.components.composables.map.SettingFlags
import com.lamti.capturetheflag.presentation.ui.style.Black
import com.lamti.capturetheflag.presentation.ui.style.Green
import com.lamti.capturetheflag.presentation.ui.style.Red
import kotlinx.coroutines.launch

@Composable
fun MapScreen(
    userID: String,
    gameDetails: GameDetails,
    gameState: GameState,
    canPlaceFlag: Boolean,
    enteredGeofenceId: String,
    initialPosition: LatLng,
    livePosition: LatLng,
    isSafehouseDraggable: Boolean,
    otherPlayers: List<GamePlayer>,
    battleID: String,
    lost: Boolean,
    greenPlayersCount: Int,
    redPlayersCount: Int,
    enterBattleScreen: Boolean,
    enterGameOverScreen: Boolean,
    onEnterBattleScreen: () -> Unit,
    onEnterGameOverScreen: () -> Unit,
    onSafehouseMarkerClicked: (LatLng) -> Unit,
    onArScannerButtonClicked: () -> Unit,
    onSettingFlagsButtonClicked: () -> Unit,
    onSetFlagsClicked: () -> Unit,
    onBattleButtonClicked: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var zoom by remember { mutableStateOf(15f) }
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
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            InfoBar(
                team = gameDetails.team,
                redPlayersCount = redPlayersCount,
                greenPlayersCount = greenPlayersCount,
                onCompassClicked = {
                    zoom = if (zoom == 15f) 18f else 15f
                    coroutineScope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(livePosition, zoom))
                    }
                },
                onGpsClicked = {
                    coroutineScope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.newLatLng(livePosition))
                    }
                }
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
        SettingFlags(
            modifier = Modifier.align(Alignment.BottomCenter),
            gameState = gameState.state,
            playerGameDetails = gameDetails,
            redFlagIsPlaced = gameState.redFlag.isPlaced,
            greenFlagIsPlaced = gameState.greenFlag.isPlaced,
            canPlaceFlag = canPlaceFlag,
            onSettingFlagsButtonClicked = onSettingFlagsButtonClicked
        )
        ReadyButton(
            modifier = Modifier.align(Alignment.BottomCenter),
            gameState = gameState.state,
            playerGameDetails = gameDetails,
            onReadyButtonClicked = onSetFlagsClicked
        )
        ActionButtons(
            modifier = Modifier.align(Alignment.Center),
            buttonModifier = Modifier.align(Alignment.BottomCenter),
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
fun InfoBar(
    modifier: Modifier = Modifier,
    team: Team,
    redPlayersCount: Int,
    greenPlayersCount: Int,
    onCompassClicked: () -> Unit,
    onGpsClicked: () -> Unit
) {
    val teamColor: Color = remember(team) {
        when (team) {
            Team.Red -> Red
            Team.Green -> Green
            Team.Unknown -> Black
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DefaultButton(
            modifier = Modifier.height(42.dp),
            text = team.name,
            fontSize = 16.sp,
            color = teamColor,
            cornerSize = CornerSize(20),
            onclick = {}
        )
        DefaultButton(
            modifier = Modifier.size(42.dp),
            text = redPlayersCount.toString(),
            color = MaterialTheme.colors.background,
            textColor = Red,
            cornerSize = CornerSize(20),
            onclick = {}
        )
        DefaultButton(
            modifier = Modifier.size(42.dp),
            text = greenPlayersCount.toString(),
            color = MaterialTheme.colors.background,
            textColor = Green,
            cornerSize = CornerSize(20),
            onclick = {}
        )
        IconButton(
            icon = R.drawable.ic_compass,
            onclick = onCompassClicked
        )
        IconButton(
            icon = R.drawable.ic_gps,
            onclick = onGpsClicked
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

package com.lamti.capturetheflag.presentation.ui.components.screens

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.google.android.gms.maps.model.LatLng
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.domain.game.GamePlayer
import com.lamti.capturetheflag.domain.game.GameState
import com.lamti.capturetheflag.domain.game.ProgressState
import com.lamti.capturetheflag.domain.player.GameDetails
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.presentation.ui.components.composables.InstructionsCard
import com.lamti.capturetheflag.presentation.ui.components.composables.map.ArFlagButton
import com.lamti.capturetheflag.presentation.ui.components.composables.map.GoogleMapsView
import com.lamti.capturetheflag.presentation.ui.components.composables.map.QuitButton
import com.lamti.capturetheflag.presentation.ui.components.composables.map.ReadyButton
import com.lamti.capturetheflag.presentation.ui.components.composables.map.SettingFlags
import com.lamti.capturetheflag.utils.EMPTY

@Composable
fun MapScreen(
    userID: String,
    gameDetails: GameDetails,
    gameState: GameState,
    isInsideSafehouse: Boolean,
    enteredGeofenceId: String,
    currentPosition: LatLng,
    isSafehouseDraggable: Boolean,
    otherPlayers: List<GamePlayer>,
    onSafehouseMarkerClicked: (LatLng) -> Unit,
    onArScannerButtonClicked: () -> Unit,
    onSettingFlagsButtonClicked: () -> Unit,
    onSetFlagsClicked: () -> Unit,
    onQuitButtonClicked: () -> Unit,
) {

    val instructions = setInstructions(
        team = gameDetails.team,
        rank = gameDetails.rank,
        gameState = gameState.state,
        isInsideSafehouse = isInsideSafehouse,
        isRedFlagPlaced = gameState.redFlag.isPlaced,
        isGreenFlagPlaced = gameState.greenFlag.isPlaced
    )

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        GoogleMapsView(
            currentPosition = currentPosition,
            safehousePosition = gameState.safehouse.position,
            isSafeHouseDraggable = isSafehouseDraggable,
            team = gameDetails.team,
            userID = userID,
            redFlag = gameState.redFlag,
            greenFlag = gameState.greenFlag,
            otherPlayers = otherPlayers,
            onSafehouseMarkerClicked = onSafehouseMarkerClicked
        )
        SettingFlags(
            modifier = Modifier.align(Alignment.BottomCenter),
            gameState = gameState.state,
            playerGameDetails = gameDetails,
            redFlagIsPlaced = gameState.redFlag.isPlaced,
            greenFlagIsPlaced = gameState.greenFlag.isPlaced,
            isInsideSafehouse = isInsideSafehouse,
            onSettingFlagsButtonClicked = onSettingFlagsButtonClicked
        )
        if (gameState.state != ProgressState.Started) {
            InstructionsCard(instructions)
        }
        ReadyButton(
            modifier = Modifier.align(Alignment.BottomCenter),
            gameState = gameState.state,
            playerGameDetails = gameDetails,
            onReadyButtonClicked = onSetFlagsClicked
        )
        ArFlagButton(
            modifier = Modifier.align(Alignment.BottomCenter),
            team = gameDetails.team,
            enteredGeofenceId = enteredGeofenceId,
            onArScannerButtonClicked = onArScannerButtonClicked
        )
        QuitButton(
            modifier = Modifier.align(Alignment.BottomCenter),
            gameState = gameState.state,
            onQuitButtonClicked = onQuitButtonClicked
        )
    }
}


@Composable
private fun setInstructions(
    team: Team,
    rank: GameDetails.Rank,
    gameState: ProgressState,
    isInsideSafehouse: Boolean,
    isRedFlagPlaced: Boolean,
    isGreenFlagPlaced: Boolean,
): String = when (gameState) {
    ProgressState.Created -> {
        if (rank == GameDetails.Rank.Captain)
            stringResource(R.string.instructions_set_safehouse)
        else
            stringResource(R.string.wait_the_captain)
    }
    ProgressState.SettingFlags -> {
        when {
            team == Team.Red && isRedFlagPlaced && !isGreenFlagPlaced -> stringResource(R.string.wait_for_green_flag)
            team == Team.Green && isGreenFlagPlaced && !isRedFlagPlaced -> stringResource(R.string.wait_for_red_flag)
            else -> {
                if (isInsideSafehouse) stringResource(R.string.place_flag_outside_safehouse)
                else stringResource(R.string.instructions_set_flags)
            }
        }
    }
    ProgressState.Ended -> stringResource(id = R.string.game_over)
    else -> EMPTY
}


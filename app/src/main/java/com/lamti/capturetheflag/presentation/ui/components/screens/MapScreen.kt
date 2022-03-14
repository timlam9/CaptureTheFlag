package com.lamti.capturetheflag.presentation.ui.components.screens

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.domain.game.ProgressState
import com.lamti.capturetheflag.domain.player.GameDetails
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.presentation.ui.components.composables.InstructionsCard
import com.lamti.capturetheflag.presentation.ui.components.composables.map.ArFlagButton
import com.lamti.capturetheflag.presentation.ui.components.composables.map.GoogleMapsView
import com.lamti.capturetheflag.presentation.ui.components.composables.map.QuitButton
import com.lamti.capturetheflag.presentation.ui.components.composables.map.ReadyButton
import com.lamti.capturetheflag.presentation.ui.components.composables.map.SettingFlags
import com.lamti.capturetheflag.presentation.ui.fragments.maps.MapViewModel
import com.lamti.capturetheflag.utils.EMPTY

@Composable
fun MapScreen(
    viewModel: MapViewModel,
    enteredGeofenceId: String,
    onArScannerButtonClicked: () -> Unit,
    onSettingFlagsButtonClicked: () -> Unit,
    onQuitButtonClicked: () -> Unit,
) {
    val playerGameDetails = viewModel.player.value.gameDetails
    val gameState = viewModel.game.value.gameState.state
    val isInsideSafehouse = viewModel.isInsideSafehouse.value
    val instructions = setInstructions(
        team = playerGameDetails?.team ?: Team.Unknown,
        rank = playerGameDetails?.rank ?: GameDetails.Rank.Soldier,
        gameState = gameState,
        isInsideSafehouse = isInsideSafehouse,
        isRedFlagPlaced = viewModel.game.value.gameState.redFlag.isPlaced,
        isGreenFlagPlaced = viewModel.game.value.gameState.greenFlag.isPlaced
    )

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        GoogleMapsView(viewModel = viewModel)
        SettingFlags(
            modifier = Modifier.align(Alignment.BottomCenter),
            gameState = gameState,
            playerGameDetails = playerGameDetails,
            redFlagIsPlaced = viewModel.game.value.gameState.redFlag.isPlaced,
            greenFlagIsPlaced = viewModel.game.value.gameState.greenFlag.isPlaced,
            isInsideSafehouse = isInsideSafehouse,
            onSettingFlagsButtonClicked = onSettingFlagsButtonClicked
        )
        if (gameState != ProgressState.Started) {
            InstructionsCard(instructions)
        }
        ReadyButton(
            modifier = Modifier.align(Alignment.BottomCenter),
            gameState = gameState,
            playerGameDetails = playerGameDetails,
            onReadyButtonClicked = { viewModel.onSetFlagsClicked() }
        )
        ArFlagButton(
            modifier = Modifier.align(Alignment.BottomCenter),
            team = playerGameDetails?.team ?: Team.Unknown,
            enteredGeofenceId = enteredGeofenceId,
            onArScannerButtonClicked = onArScannerButtonClicked
        )
        QuitButton(
            modifier = Modifier.align(Alignment.BottomCenter),
            gameState = gameState,
            onQuitButtonClicked = {
                viewModel.onQuitButtonClicked {
                    if (it) onQuitButtonClicked()
                }
            }
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


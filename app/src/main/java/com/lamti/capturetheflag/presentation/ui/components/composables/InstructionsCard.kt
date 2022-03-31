package com.lamti.capturetheflag.presentation.ui.components.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.domain.game.ProgressState
import com.lamti.capturetheflag.domain.player.GameDetails
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.utils.EMPTY

@Composable
fun InstructionsCard(
    team: Team,
    rank: GameDetails.Rank,
    state: ProgressState,
    canPlaceFlag: Boolean,
    isRedFlagPlaced: Boolean,
    isGreenFlagPlaced: Boolean,
    redFlagCaptured: String?,
    greenFlagCaptured: String?
) {
    val instructions = setInstructions(
        team = team,
        rank = rank,
        gameState = state,
        canPlaceFlag = canPlaceFlag,
        isRedFlagPlaced = isRedFlagPlaced,
        isGreenFlagPlaced = isGreenFlagPlaced,
        isRedFlagCaptured = !redFlagCaptured.isNullOrEmpty(),
        isGreenFlagCaptured = !greenFlagCaptured.isNullOrEmpty(),
    ).also {
        if (it.isEmpty()) return
    }
    InstructionsCard(text = instructions)
}

@Composable
fun InstructionsCard(modifier: Modifier = Modifier, text: String) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        elevation = 10.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = text)
        }
    }
}

@Composable
private fun setInstructions(
    team: Team,
    rank: GameDetails.Rank,
    gameState: ProgressState,
    canPlaceFlag: Boolean,
    isRedFlagPlaced: Boolean,
    isGreenFlagPlaced: Boolean,
    isRedFlagCaptured: Boolean,
    isGreenFlagCaptured: Boolean,
): String = when (gameState) {
    ProgressState.Created -> {
        if (rank == GameDetails.Rank.Captain)
            stringResource(R.string.instructions_set_safehouse)
        else
            stringResource(R.string.wait_the_captain)
    }
    ProgressState.SettingFlags -> {
        when {
            team == Team.Red && isRedFlagPlaced && !isGreenFlagPlaced -> EMPTY
            team == Team.Green && isGreenFlagPlaced && !isRedFlagPlaced -> EMPTY
            else -> {
                if (canPlaceFlag) stringResource(R.string.instructions_set_flags)
                else stringResource(R.string.place_flag_outside_safehouse)
            }
        }
    }
    ProgressState.Started -> {
        when {
            isGreenFlagCaptured && isRedFlagCaptured -> stringResource(id = R.string.both_flags_captured)
            isRedFlagCaptured -> when (team) {
                Team.Green -> stringResource(id = R.string.red_flag_captured)
                else -> stringResource(id = R.string.your_flag_captured)
            }
            isGreenFlagCaptured -> when (team) {
                Team.Red -> stringResource(id = R.string.green_flag_captured)
                else -> stringResource(id = R.string.your_flag_captured)
            }
            else -> EMPTY
        }
    }
    ProgressState.Ended -> stringResource(id = R.string.game_over)
    else -> EMPTY
}

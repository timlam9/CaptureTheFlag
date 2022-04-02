package com.lamti.capturetheflag.presentation.ui.components.composables.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.domain.game.ProgressState
import com.lamti.capturetheflag.domain.player.GameDetails
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.presentation.ui.components.composables.common.DefaultButton
import com.lamti.capturetheflag.presentation.ui.style.DarkBlueOpacity
import com.lamti.capturetheflag.presentation.ui.style.White

@Composable
fun SettingFlags(
    modifier: Modifier = Modifier,
    gameState: ProgressState,
    playerGameDetails: GameDetails?,
    redFlagIsPlaced: Boolean,
    greenFlagIsPlaced: Boolean,
    canPlaceFlag: Boolean,
    onSettingFlagsButtonClicked: () -> Unit
) {
    if (gameState == ProgressState.SettingFlags) {
        if (playerGameDetails?.rank == GameDetails.Rank.Captain ||
            playerGameDetails?.rank == GameDetails.Rank.Leader
        ) {
            val showArButton = when (playerGameDetails.team) {
                Team.Red -> !redFlagIsPlaced
                Team.Green -> !greenFlagIsPlaced
                Team.Unknown -> false
            }
            when (showArButton) {
                true -> if (canPlaceFlag) {
                    DefaultButton(
                        modifier = modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        text = stringResource(id = R.string.place_flag),
                        onclick = onSettingFlagsButtonClicked
                    )
                }
                false -> WaitingLeaders()
            }
        } else WaitingLeaders()
    }
}

@Composable
private fun WaitingLeaders(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = DarkBlueOpacity)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = Modifier.align(Alignment.Center),
            text = stringResource(id = R.string.wait_leaders),
            style = MaterialTheme.typography.h4.copy(
                color = White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )
    }
}

package com.lamti.capturetheflag.presentation.ui.components.composables.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.domain.game.ProgressState
import com.lamti.capturetheflag.domain.player.GameDetails
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.presentation.ui.style.DarkBlueOpacity
import com.lamti.capturetheflag.utils.EMPTY

@Composable
 fun SettingFlags(
    modifier: Modifier,
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
            if (showArButton) {
                if (canPlaceFlag) {
                    FloatingActionButton(
                        modifier = modifier.padding(bottom = 64.dp),
                        onClick = onSettingFlagsButtonClicked,
                        backgroundColor = MaterialTheme.colors.secondary,
                        contentColor = Color.White
                    ) {
                        Icon(painterResource(id = R.drawable.ic_flag), EMPTY)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = DarkBlueOpacity)
                        .fillMaxSize()
                )
            }
        }
    }
}

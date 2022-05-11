package com.lamti.capturetheflag.presentation.ui.components.composables.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.domain.game.ProgressState
import com.lamti.capturetheflag.domain.player.GameDetails
import com.lamti.capturetheflag.presentation.ui.DEFAULT_GAME_RADIUS
import com.lamti.capturetheflag.presentation.ui.components.composables.common.DefaultButton

@Composable
fun GameConfiguration(
    modifier: Modifier,
    gameState: ProgressState,
    playerGameDetails: GameDetails?,
    onReadyButtonClicked: () -> Unit,
    onValueChange: (Float) -> Unit,
) {
    if (gameState == ProgressState.Created && playerGameDetails?.rank == GameDetails.Rank.Captain) {
        Column(modifier = modifier.fillMaxWidth()) {
            RadiusSlider(onValueChange = onValueChange)
            DefaultButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.ready)
            ) {
                onReadyButtonClicked()
            }
        }
    }
}

@Composable
fun RadiusSlider(title: String = "Configure game's boundaries", onValueChange: (Float) -> Unit) {
    var sliderPosition by remember { mutableStateOf(DEFAULT_GAME_RADIUS) }
    Text(text = title)
    Slider(
        value = sliderPosition,
        valueRange = 250f..750f,
        onValueChange = {
            sliderPosition = it
            onValueChange(it)
        }
    )
}

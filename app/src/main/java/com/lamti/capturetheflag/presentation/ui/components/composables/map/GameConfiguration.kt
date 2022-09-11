package com.lamti.capturetheflag.presentation.ui.components.composables.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.tooling.preview.Preview
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.domain.game.ProgressState
import com.lamti.capturetheflag.domain.player.GameDetails
import com.lamti.capturetheflag.presentation.ui.DEFAULT_FLAG_RADIUS
import com.lamti.capturetheflag.presentation.ui.DEFAULT_GAME_RADIUS
import com.lamti.capturetheflag.presentation.ui.components.composables.common.DefaultButton

@Composable
fun GameConfiguration(
    modifier: Modifier,
    gameState: ProgressState,
    playerRank: GameDetails.Rank?,
    onReadyButtonClicked: () -> Unit,
    onFlagRadiusValueChange: (Float) -> Unit,
    onGameRadiusValueChange: (Float) -> Unit,
) {
    if (gameState == ProgressState.Created && playerRank == GameDetails.Rank.Captain) {
        Column(modifier = modifier.fillMaxWidth()) {
            RadiusSlider(onValueChange = onGameRadiusValueChange)
            RadiusSlider(
                title = "Configure safehouse size",
                valueRange = 10f..50f,
                initialValue = DEFAULT_FLAG_RADIUS,
                onValueChange = onFlagRadiusValueChange
            )
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
private fun RadiusSlider(
    title: String = "Configure game's boundaries",
    valueRange: ClosedFloatingPointRange<Float> = 250f..750f,
    initialValue: Float = DEFAULT_GAME_RADIUS,
    onValueChange: (Float) -> Unit
) {
    var sliderPosition by remember { mutableStateOf(initialValue) }
    Text(text = title)
    Slider(
        value = sliderPosition,
        valueRange = valueRange,
        onValueChange = {
            sliderPosition = it
            onValueChange(it)
        }
    )
}

@Preview
@Composable
fun MenuScreenPreview() {
    GameConfiguration(
        modifier = Modifier.fillMaxSize(),
        gameState = ProgressState.Created,
        playerRank = GameDetails.Rank.Captain,
        onReadyButtonClicked = {},
        onGameRadiusValueChange = {},
        onFlagRadiusValueChange = {}
    )
}

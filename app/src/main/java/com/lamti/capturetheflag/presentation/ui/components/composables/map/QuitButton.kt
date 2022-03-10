package com.lamti.capturetheflag.presentation.ui.components.composables.map

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.domain.game.ProgressState
import com.lamti.capturetheflag.presentation.ui.components.composables.DefaultButton

@Composable
fun QuitButton(
    modifier: Modifier,
    gameState: ProgressState,
    onQuitButtonClicked: () -> Unit,
) {
    if (gameState == ProgressState.Ended) {
        DefaultButton(
            modifier = modifier.fillMaxWidth(),
            text = stringResource(id = R.string.quit_game)
        ) {
            onQuitButtonClicked()
        }
    }
}

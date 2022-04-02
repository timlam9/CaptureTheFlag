package com.lamti.capturetheflag.presentation.ui.components.composables.map

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.domain.game.ProgressState
import com.lamti.capturetheflag.domain.player.GameDetails
import com.lamti.capturetheflag.presentation.ui.components.composables.common.DefaultButton

@Composable
 fun ReadyButton(
    modifier: Modifier,
    gameState: ProgressState,
    playerGameDetails: GameDetails?,
    onReadyButtonClicked: () -> Unit
) {
    if (gameState == ProgressState.Created && playerGameDetails?.rank == GameDetails.Rank.Captain) {
        DefaultButton(
            modifier = modifier.fillMaxWidth(),
            text = stringResource(id = R.string.ready)
        ) {
            onReadyButtonClicked()
        }
    }
}

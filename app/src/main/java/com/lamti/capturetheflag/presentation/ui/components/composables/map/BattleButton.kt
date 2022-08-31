package com.lamti.capturetheflag.presentation.ui.components.composables.map

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.presentation.ui.components.composables.common.DefaultButton
import com.lamti.capturetheflag.presentation.ui.components.composables.common.PulseAnimation
import com.lamti.capturetheflag.presentation.ui.style.Black
import com.lamti.capturetheflag.presentation.ui.style.Green
import com.lamti.capturetheflag.presentation.ui.style.Red
import com.lamti.capturetheflag.utils.EMPTY

@Composable
fun BattleButton(
    modifier: Modifier = Modifier,
    team: Team,
    opponentName: String,
    onBattleButtonClicked: () -> Unit
) {
    if (opponentName != EMPTY) {
        val teamColor: Color = remember(team) {
            when (team) {
                Team.Red -> Red
                Team.Green -> Green
                Team.Unknown -> Black
            }
        }
        PulseAnimation(color = teamColor)
        DefaultButton(
            modifier = modifier.padding(bottom = 64.dp),
            text = "${stringResource(id = R.string.battle)}: $opponentName",
            color = teamColor,
            onclick = onBattleButtonClicked
        )
    }
}

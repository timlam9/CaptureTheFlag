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

@Composable
fun BattleButton(
    modifier: Modifier = Modifier,
    battleID: String,
    enteredGeofenceId: String,
    team: Team,
    onBattleButtonClicked: () -> Unit
) {
    if (battleID.isNotEmpty() && isInBattleableGameZone(enteredGeofenceId)) {
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
            text = stringResource(id = R.string.battle),
            color = teamColor,
            onclick = onBattleButtonClicked
        )
    }
}

@Composable
private fun isInBattleableGameZone(enteredGeofenceId: String) =
    !enteredGeofenceId.contains("safehouse") &&
            !enteredGeofenceId.contains(Team.Green.name) &&
            !enteredGeofenceId.contains(Team.Red.name)

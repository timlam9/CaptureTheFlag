package com.lamti.capturetheflag.presentation.ui.components.composables.map

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lamti.capturetheflag.domain.player.Team

@Composable
fun ActionButtons(
    modifier: Modifier = Modifier,
    battleModifier: Modifier = Modifier,
    lost: Boolean,
    team: Team,
    battleID: String,
    enteredGeofenceId: String,
    redFlagCaptured: String?,
    greenFlagCaptured: String?,
    onArScannerButtonClicked: () -> Unit,
    onBattleButtonClicked: () -> Unit
) {
    when (lost) {
        true -> Text(text = "You Lost!", modifier = battleModifier)
        false -> {
            ArFlagButton(
                modifier = modifier,
                team = team,
                enteredGeofenceId = enteredGeofenceId,
                redFlagPlayer = redFlagCaptured,
                greenFlagPlayer = greenFlagCaptured,
                onArScannerButtonClicked = onArScannerButtonClicked
            )
            BattleButton(
                modifier = battleModifier,
                battleID = battleID,
                enteredGeofenceId = enteredGeofenceId,
                team = team,
                onBattleButtonClicked = onBattleButtonClicked
            )
        }
    }
}

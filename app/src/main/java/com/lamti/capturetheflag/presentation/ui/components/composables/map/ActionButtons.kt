package com.lamti.capturetheflag.presentation.ui.components.composables.map

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lamti.capturetheflag.domain.player.Team

@Composable
fun ActionButtons(
    modifier: Modifier = Modifier,
    buttonModifier: Modifier = Modifier,
    lost: Boolean,
    team: Team,
    showBattleButton: Boolean,
    showArFlagButton: Boolean,
    onArScannerButtonClicked: () -> Unit,
    onBattleButtonClicked: () -> Unit
) {
    when (lost) {
        true -> Text(text = "You Lost!", modifier = modifier)
        false -> {
            ArFlagButton(
                modifier = buttonModifier,
                team = team,
                show = showArFlagButton,
                onArScannerButtonClicked = onArScannerButtonClicked
            )
            BattleButton(
                modifier = buttonModifier,
                show = showBattleButton,
                team = team,
                onBattleButtonClicked = onBattleButtonClicked
            )
        }
    }
}

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
import com.lamti.capturetheflag.presentation.ui.style.Blue
import com.lamti.capturetheflag.presentation.ui.style.Green
import com.lamti.capturetheflag.presentation.ui.style.Red

@Composable
fun ArFlagButton(
    modifier: Modifier = Modifier,
    team: Team,
    show: Boolean,
    onArScannerButtonClicked: () -> Unit
) {
    if (show) {
        val opponentColor: Color = remember(team) {
            when (team) {
                Team.Red -> Green
                Team.Green -> Red
                Team.Unknown -> Blue
            }
        }
        PulseAnimation(color = opponentColor)
        DefaultButton(
            modifier = modifier.padding(bottom = 64.dp),
            text = stringResource(id = R.string.capture_flag),
            color = opponentColor,
            onclick = onArScannerButtonClicked
        )
    }
}

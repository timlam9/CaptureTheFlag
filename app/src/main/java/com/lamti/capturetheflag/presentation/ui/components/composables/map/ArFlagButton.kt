package com.lamti.capturetheflag.presentation.ui.components.composables.map

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
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.utils.EMPTY

@Composable
fun ArFlagButton(
    modifier: Modifier,
    team: Team,
    enteredGeofenceId: String,
    onArScannerButtonClicked: () -> Unit
) {
    if (team == Team.Red && enteredGeofenceId.contains(Team.Green.name) ||
        team == Team.Green && enteredGeofenceId.contains(Team.Red.name)
    ) {
        FloatingActionButton(
            modifier = modifier.padding(bottom = 64.dp),
            onClick = onArScannerButtonClicked,
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = Color.White
        ) {
            Icon(painterResource(id = R.drawable.ic_flag), EMPTY)
        }
    }
}

package com.lamti.capturetheflag.presentation.ui.components.composables.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.presentation.ui.components.composables.common.DefaultButton
import com.lamti.capturetheflag.presentation.ui.components.composables.common.IconButton
import com.lamti.capturetheflag.presentation.ui.style.Black
import com.lamti.capturetheflag.presentation.ui.style.Green
import com.lamti.capturetheflag.presentation.ui.style.Red

@Composable
fun InfoBar(
    modifier: Modifier = Modifier,
    team: Team,
    redPlayersCount: Int,
    greenPlayersCount: Int,
    onCompassClicked: () -> Unit,
    onGpsClicked: () -> Unit
) {
    val teamColor: Color = remember(team) {
        when (team) {
            Team.Red -> Red
            Team.Green -> Green
            Team.Unknown -> Black
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DefaultButton(
            modifier = Modifier.height(42.dp),
            text = team.name,
            fontSize = 16.sp,
            color = teamColor,
            cornerSize = CornerSize(20),
            onclick = {}
        )
        DefaultButton(
            modifier = Modifier.size(42.dp),
            text = redPlayersCount.toString(),
            color = MaterialTheme.colors.background,
            textColor = Red,
            cornerSize = CornerSize(20),
            onclick = {}
        )
        DefaultButton(
            modifier = Modifier.size(42.dp),
            text = greenPlayersCount.toString(),
            color = MaterialTheme.colors.background,
            textColor = Green,
            cornerSize = CornerSize(20),
            onclick = {}
        )
        IconButton(
            icon = R.drawable.ic_compass,
            onclick = onCompassClicked
        )
        IconButton(
            icon = R.drawable.ic_gps,
            onclick = onGpsClicked
        )
    }
}

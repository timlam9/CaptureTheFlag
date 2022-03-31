package com.lamti.capturetheflag.presentation.ui.components.screens


import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.presentation.ui.components.composables.DefaultButton
import com.lamti.capturetheflag.presentation.ui.style.Black
import com.lamti.capturetheflag.presentation.ui.style.Green
import com.lamti.capturetheflag.presentation.ui.style.Red

@Composable
fun GameOverScreen(
    winners: Team,
    onOkButtonClicked: () -> Unit
) {
    val teamColor: Color = remember(winners) {
        when (winners) {
            Team.Red -> Red
            Team.Green -> Green
            Team.Unknown -> Black
        }
    }
    val teamImage = remember(winners) {
        when (winners) {
            Team.Red -> R.drawable.game_over_red
            Team.Green -> R.drawable.game_over_green
            Team.Unknown -> R.drawable.game_over_red
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.game_over),
            style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            modifier = Modifier.offset(y = (-60).dp),
            text = "$winners team won the game!",
            style = MaterialTheme.typography.h5.copy(
                color = teamColor,
                fontWeight = FontWeight.Bold
            )
        )
        Image(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-20).dp),
            painter = painterResource(id = teamImage),
            contentDescription = stringResource(R.string.gr_code)
        )
        DefaultButton(
            modifier = Modifier
                .height(60.dp)
                .fillMaxWidth(),
            text = stringResource(R.string.ok),
        ) {
            onOkButtonClicked()
        }
    }
}

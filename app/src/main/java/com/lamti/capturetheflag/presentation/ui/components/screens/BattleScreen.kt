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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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
fun BattleScreen(
    team: Team,
    enterBattleScreen: Boolean,
    onEnterBattleScreen: () -> Unit,
    onLostButtonClicked: () -> Unit
) {
    if (!enterBattleScreen) {
        LaunchedEffect(key1 = enterBattleScreen) {
            onEnterBattleScreen()
        }
    }
    val teamColor: Color = remember(team) {
        when (team) {
            Team.Red -> Red
            Team.Green -> Green
            Team.Unknown -> Black
        }
    }
    val teamImage = remember(team) {
        when (team) {
            Team.Red -> R.drawable.red_battle_image
            Team.Green -> R.drawable.green_battle_image
            Team.Unknown -> R.drawable.red_battle_image
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
            text = stringResource(id = R.string.battle),
            style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            modifier = Modifier.offset(y = (-120).dp),
            text = stringResource(R.string.fight_for_your_team),
            style = MaterialTheme.typography.h5.copy(
                color = teamColor,
                fontWeight = FontWeight.Bold
            )
        )
        Image(
            modifier = Modifier
                .fillMaxWidth()
                .scale(1.4f)
                .offset(y = (-30).dp),
            painter = painterResource(id = teamImage),
            contentDescription = stringResource(R.string.gr_code)
        )
        DefaultButton(
            modifier = Modifier
                .height(60.dp)
                .fillMaxWidth(),
            text = stringResource(R.string.i_lost),
            color = MaterialTheme.colors.onBackground,
            textColor = MaterialTheme.colors.background
        ) {
            onLostButtonClicked()
        }
    }
}

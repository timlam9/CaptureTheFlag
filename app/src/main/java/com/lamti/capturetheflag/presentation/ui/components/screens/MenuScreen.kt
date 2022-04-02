package com.lamti.capturetheflag.presentation.ui.components.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.presentation.ui.components.composables.common.DefaultButton
import com.lamti.capturetheflag.presentation.ui.components.composables.common.OutlinedButton
import com.lamti.capturetheflag.presentation.ui.style.White

@Composable
fun MenuScreen(
    onLogoutClicked: () -> Unit,
    onNewGameClicked: () -> Unit,
    onJoinGameClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        Box(
            modifier = Modifier
                .weight(2f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8))
                .background(color = MaterialTheme.colors.primary),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                modifier = Modifier
                    .size(52.dp)
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                onClick = onLogoutClicked
            ) {
                Icon(
                    Icons.Filled.ExitToApp,
                    contentDescription = null,
                    tint = White
                )
            }
            Image(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(30.dp),
                painter = painterResource(id = R.drawable.intro_logo),
                contentDescription = "intro image"
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.capture_the_flag),
                style = MaterialTheme.typography.h4.copy(
                    color = MaterialTheme.colors.onBackground,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = stringResource(R.string.play_ultimate_game),
                style = MaterialTheme.typography.body2
            )
        }
        JoinGameButton {
            onJoinGameClicked()
        }
        NewGameButton(
            modifier = Modifier
                .padding(top = 16.dp, bottom = 16.dp)
        ) {
            onNewGameClicked()
        }
    }
}

@Composable
private fun JoinGameButton(modifier: Modifier = Modifier, onAvailableGamesClicked: () -> Unit) {
    DefaultButton(
        modifier = modifier.fillMaxWidth(),
        text = stringResource(R.string.join_game),
        color = MaterialTheme.colors.primary
    ) {
        onAvailableGamesClicked()
    }
}

@Composable
private fun NewGameButton(modifier: Modifier = Modifier, onNewGameClicked: () -> Unit) {
    OutlinedButton(
        modifier = modifier.fillMaxWidth(),
        text = stringResource(R.string.new_game)
    ) {
        onNewGameClicked()
    }
}


@Preview
@Composable
fun MenuScreenPreview() {
    MenuScreen({}, {}, {})
}

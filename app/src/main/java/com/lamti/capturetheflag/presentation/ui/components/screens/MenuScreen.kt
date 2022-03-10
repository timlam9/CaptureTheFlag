package com.lamti.capturetheflag.presentation.ui.components.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.presentation.ui.components.composables.DefaultButton

@Composable
fun MenuScreen(
    onNewGameClicked: () -> Unit,
    onJoinGameClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Spacer(modifier = Modifier.weight(0.25f))
        Text(
            text = stringResource(R.string.capture_the_flag),
            style = MaterialTheme.typography.h4.copy(
                color = MaterialTheme.colors.primaryVariant,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = stringResource(R.string.are_you_ready_to_play),
            style = MaterialTheme.typography.h5.copy(color = MaterialTheme.colors.onBackground)
        )
        Spacer(modifier = Modifier.weight(2f))
        JoinGameButton(modifier = Modifier.padding(20.dp)) {
            onJoinGameClicked()
        }
        NewGameButton(modifier = Modifier.padding(20.dp)) {
            onNewGameClicked()
        }
    }
}

@Composable
fun NewGameButton(modifier: Modifier = Modifier, onNewGameClicked: () -> Unit) {
    DefaultButton(
        modifier = modifier.fillMaxWidth(),
        text = stringResource(R.string.new_game),
        color = MaterialTheme.colors.primaryVariant
    ) {
        onNewGameClicked()
    }
}

@Composable
fun JoinGameButton(modifier: Modifier = Modifier, onAvailableGamesClicked: () -> Unit) {
    DefaultButton(
        modifier = modifier.fillMaxWidth(),
        text = stringResource(R.string.join_game),
        color = MaterialTheme.colors.secondary
    ) {
        onAvailableGamesClicked()
    }
}




@Preview
@Composable
fun MenuScreenPreview() {
    MenuScreen({}, {})
}

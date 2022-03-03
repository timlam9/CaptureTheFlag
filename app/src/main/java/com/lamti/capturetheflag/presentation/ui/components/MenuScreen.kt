package com.lamti.capturetheflag.presentation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.Button
import androidx.compose.material.ButtonColors
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
import com.lamti.capturetheflag.utils.EMPTY

@Composable
fun MenuScreen(onNewGameClicked: () -> Unit, onAvailableGamesClicked: () -> Unit) {
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
        AvailableGamesButton(modifier = Modifier.padding(20.dp)) {
            onAvailableGamesClicked()
        }
        NewGameButton(modifier = Modifier.padding(20.dp)) {
            onNewGameClicked()
        }
    }
}

@Composable
fun NewGameButton(modifier: Modifier = Modifier, onNewGameClicked: () -> Unit) {
    DefaultButton(
        modifier = modifier,
        text = stringResource(R.string.new_game),
        color = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primaryVariant)
    ) {
        onNewGameClicked()
    }
}

@Composable
fun AvailableGamesButton(modifier: Modifier = Modifier, onAvailableGamesClicked: () -> Unit) {
    DefaultButton(
        modifier = modifier,
        text = stringResource(R.string.available_games),
        color = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary)
    ) {
        onAvailableGamesClicked()
    }
}

@Composable
fun DefaultButton(
    modifier: Modifier = Modifier,
    text: String = EMPTY,
    textColor: Color = Color.White,
    color: ButtonColors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primaryVariant),
    shape: Shape = MaterialTheme.shapes.small.copy(CornerSize(20)),
    onclick: () -> Unit
) {
    Button(
        onClick = onclick,
        modifier = modifier
            .height(60.dp)
            .fillMaxWidth(),
        shape = shape,
        colors = color
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.button.copy(
                color = textColor,
                fontWeight = FontWeight.Bold
            )
        )
    }
}


@Preview
@Composable
fun MenuScreenPreview() {
    MenuScreen({}, {})
}

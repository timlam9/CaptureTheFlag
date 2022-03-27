package com.lamti.capturetheflag.presentation.ui.components.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.domain.game.ProgressState
import com.lamti.capturetheflag.presentation.ui.components.composables.DefaultButton
import com.lamti.capturetheflag.presentation.ui.components.composables.InfoTextField
import com.lamti.capturetheflag.utils.EMPTY

@Composable
fun CreateGameScreen(
    gameID: String,
    qrCodeImage: ImageBitmap?,
    gameState: ProgressState,
    redPlayers: Int,
    greenPlayers: Int,
    onSetGameClicked: () -> Unit,
    onCreateGameClicked: (String) -> Unit
) {
    var isGameCreated by remember { mutableStateOf(false) }
    var buttonName by remember { mutableStateOf(EMPTY) }
    var gameName by remember { mutableStateOf(EMPTY) }
    var title by remember { mutableStateOf(EMPTY) }

    when (gameState) {
        ProgressState.Created -> {
            isGameCreated = true
            title = stringResource(R.string.game_created)
            buttonName = stringResource(R.string.set_game)
        }
        else -> {
            title = stringResource(R.string.create_game)
            buttonName = stringResource(R.string.create_game)
        }
    }

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
            text = title,
            style = MaterialTheme.typography.h5.copy(color = MaterialTheme.colors.onBackground)
        )
        if (gameState == ProgressState.Created) {
            Text(
                text = "Green Players: $greenPlayers",
                style = MaterialTheme.typography.h5.copy(color = Color.Green)
            )
            Text(
                text = "Red Players: $redPlayers",
                style = MaterialTheme.typography.h5.copy(color = Color.Red)
            )
        }
        Spacer(modifier = Modifier.weight(0.1f))
        if (isGameCreated) {
            GameCreated(
                modifier = Modifier.weight(3f),
                gameID = gameID,
                bitmap = qrCodeImage,
                spacerModifier = Modifier.weight(0.2f)
            )
        } else {
            CreateGame(
                gameName = gameName,
                onTextFieldUpdate = { gameName = it },
                modifier = Modifier.weight(3f)
            )
        }
        Image(
            modifier = Modifier
                .fillMaxSize()
                .padding(30.dp),
            painter = painterResource(id = R.drawable.rocket),
            contentDescription = "create game image"
        )
        DefaultButton(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            text = buttonName,
            color = MaterialTheme.colors.primaryVariant
        ) {
            when {
                isGameCreated -> onSetGameClicked()
                else -> onCreateGameClicked(gameName)
            }
        }
    }
}

@Composable
fun CreateGame(
    gameName: String,
    onTextFieldUpdate: (String) -> Unit,
    modifier: Modifier
) {
    InfoTextField(
        text = gameName,
        label = stringResource(id = R.string.type_title),
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Done,
            keyboardType = KeyboardType.Text,
            capitalization = KeyboardCapitalization.Words
        )
    ) {
        onTextFieldUpdate(it)
    }
    Spacer(modifier = modifier)
}

@Composable
fun GameCreated(
    modifier: Modifier,
    gameID: String,
    bitmap: ImageBitmap?,
    spacerModifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (bitmap != null) {
            Image(
                modifier = Modifier.size(200.dp),
                bitmap = bitmap,
                contentDescription = stringResource(R.string.gr_code)
            )
        }
        Text(text = "Or share code: $gameID")
    }
    Spacer(modifier = spacerModifier)
}
